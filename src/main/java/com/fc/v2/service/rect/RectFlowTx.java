package com.fc.v2.service.rect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TIsHazardMapper;
import com.fc.v2.mapper.auto.TIsRectApplyMapper;
import com.fc.v2.mapper.auto.TIsRectSignMapper;
import com.fc.v2.mapper.auto.TIsRectUrgeMapper;
import com.fc.v2.model.auto.TIsHazard;
import com.fc.v2.model.auto.TIsRectApply;
import com.fc.v2.model.auto.TIsRectLevel;
import com.fc.v2.model.auto.TIsRectSign;
import com.fc.v2.model.auto.TIsRectUrge;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 隐患整改申请逐级审批的事务执行器。
 *
 * 单开一个 Bean：让 {@link Transactional} 经代理生效（同类自调不生效）。
 * 一次签收里的全部落库——翻单、原子翻状态、签署留痕——在同一事务内，
 * 翻状态 0 行（被别人先动/已结案/不在本级）整笔回滚，绝不允许一张单挂两个人。
 *
 * 死口径：
 * 1) 层级次序只认 {@link LevelRuler}（平台配置），单上只有"停在哪一级"；
 * 2) 驳回直接结案为"已驳回"，不停留在流程里；
 * 3) 补材料退回申请人，重提从第一级起步，绝不从原处接着走；
 * 4) 结案时结论/结案时刻写死，终态单没有任何改写入口；
 * 5) 作废只两条路——未签署前申请人撤回、结案后管理员归档，签收中途一律拒。
 *
 * @author fuce
 * @date 2026-10-02
 */
@Component
public class RectFlowTx {

    @Resource
    private TIsRectApplyMapper applyMapper;
    @Resource
    private TIsRectSignMapper signMapper;
    @Resource
    private TIsRectUrgeMapper urgeMapper;
    @Resource
    private TIsHazardMapper hazardMapper;
    @Resource
    private LevelRuler levelRuler;

    /** 建草稿（未提交）。提交前申请人可改；提交后再没有改写单子的入口。 */
    @Transactional(rollbackFor = Exception.class)
    public TIsRectApply createDraft(Long hazardId, Integer hazardGrade, String applicant, String remark, Date at) {
        if (blank(applicant)) {
            throw new IllegalArgumentException("申请人不能为空");
        }
        if (hazardGrade == null
                || (hazardGrade != RectConst.GRADE_NORMAL && hazardGrade != RectConst.GRADE_MAJOR)) {
            throw new IllegalArgumentException("隐患级别只许 1一般隐患 2重大隐患");
        }
        TIsHazard hazard = hazardId == null ? null : hazardMapper.selectById(hazardId);
        if (hazard == null || nz(hazard.getDelFlag()) == 1) {
            throw new IllegalArgumentException("关联隐患不存在：" + hazardId);
        }
        TIsRectApply a = new TIsRectApply();
        a.setApplyNo(nextApplyNo(at));
        a.setHazardId(hazard.getId());
        a.setHazardNo(hazard.getHazardNo());
        a.setHazardGrade(hazardGrade);
        a.setHazardSummary(hazard.getSummary());
        a.setApplicant(applicant);
        a.setRoundNo(1);
        a.setUrgeRounds(0);
        a.setStatus(RectConst.ST_DRAFT);
        a.setRemark(remark);
        a.setDelFlag(0);
        a.setCreateTime(at);
        a.setUpdateTime(at);
        applyMapper.insert(a);
        return a;
    }

    /** 草稿补录（仅未提交态；补材料退回后也走这里补齐材料）。 */
    @Transactional(rollbackFor = Exception.class)
    public TIsRectApply saveDraft(Long id, String applicant, Integer hazardGrade, String remark) {
        TIsRectApply a = mustGet(id);
        if (nz(a.getStatus()) != RectConst.ST_DRAFT) {
            throw new IllegalStateException("只有未提交的单子能补录，当前状态：" + RectConst.statusText(a.getStatus()));
        }
        if (!a.getApplicant().equals(applicant)) {
            throw new IllegalStateException("只有申请人本人能补录这张单");
        }
        if (hazardGrade != null) {
            if (hazardGrade != RectConst.GRADE_NORMAL && hazardGrade != RectConst.GRADE_MAJOR) {
                throw new IllegalArgumentException("隐患级别只许 1一般隐患 2重大隐患");
            }
            a.setHazardGrade(hazardGrade);
        }
        a.setRemark(remark);
        applyMapper.updateById(a);
        return a;
    }

    /**
     * 提交 / 补材料后重新提交：永远落第一级。轮次 = 历史补材料次数 + 1，
     * 因此补材料后重提天然从第一级、新一轮走起，不可能从原处接着走。
     */
    @Transactional(rollbackFor = Exception.class)
    public TIsRectApply submit(Long id, String applicant, String opinion, Date at) {
        TIsRectApply a = mustGet(id);
        if (nz(a.getStatus()) != RectConst.ST_DRAFT) {
            throw new IllegalStateException("只有未提交的单子能提交，当前状态：" + RectConst.statusText(a.getStatus()));
        }
        if (!a.getApplicant().equals(applicant)) {
            throw new IllegalStateException("只有申请人本人能提交这张单");
        }
        TIsRectLevel first = levelRuler.first();
        int roundNo = 1 + countAction(a.getId(), RectConst.ACT_SUPPLEMENT);
        int rows = applyMapper.submit(id, roundNo, at);
        if (rows == 0) {
            throw new IllegalStateException("提交未生效：单子已被别的动作先动过");
        }
        writeSign(a, roundNo, first.getLevelNo(), first.getLevelName(),
                RectConst.ACT_SUBMIT, opinion, applicant, null, null, null, at);
        return mustGet(id);
    }

    /**
     * 签收：通过 / 驳回 / 补充材料 / 转交他级。
     *
     * 权限口径：操作人必须是当前层级的法定签署人；不是的，只有凭"代办三要素"
     * （代办人=操作人、原签署人=法定签署人、代办原因）代办才放——代办不换人，
     * 单上法定签署人该是谁还是谁。
     */
    @Transactional(rollbackFor = Exception.class)
    public TIsRectApply sign(Long id, String action, String operator, String opinion,
                             String agentFor, String agentReason, Integer targetLevelNo, Date at) {
        TIsRectApply a = mustGet(id);
        if (nz(a.getStatus()) != RectConst.ST_RUNNING) {
            throw new IllegalStateException("单子不在审批中（" + RectConst.statusText(a.getStatus()) + "），不能签收");
        }
        if (blank(operator)) {
            throw new IllegalArgumentException("签署人不能为空");
        }
        int curNo = a.getCurLevelNo() == null ? -1 : a.getCurLevelNo();
        TIsRectLevel level = levelRuler.byNo(curNo);
        if (level == null) {
            throw new IllegalStateException("当前待签层级配置已停用或缺失，流转无法继续，请联系平台管理员");
        }
        if (blank(level.getSigner())) {
            throw new IllegalStateException("第" + (curNo + 1) + "级[" + level.getLevelName()
                    + "]未配置法定签署人，无法签收，请联系平台管理员补配置");
        }
        String actualSigner;
        if (operator.equals(level.getSigner())) {
            actualSigner = operator;
            agentFor = null;
            agentReason = null;
        } else {
            // 代办留痕：代办人、原签署人、代办原因三样写齐，缺一不算代办。
            if (blank(agentFor) || blank(agentReason)) {
                throw new IllegalStateException("这一级该由[" + level.getSigner() + "]签，不是你这一层；"
                        + "确需代办必须写明代办人、原签署人和代办原因");
            }
            if (!agentFor.equals(level.getSigner())) {
                throw new IllegalStateException("代办的原签署人必须是本級法定签署人[" + level.getSigner() + "]");
            }
            actualSigner = operator;
        }
        if (blank(opinion)) {
            throw new IllegalArgumentException("每一级签的是自己的意见，处理意见不能为空");
        }

        int roundNo = a.getRoundNo() == null ? 1 : a.getRoundNo();
        switch (action == null ? "" : action) {
            case RectConst.ACT_PASS:
                doPass(a, level, roundNo, opinion, actualSigner, agentFor, agentReason, at);
                break;
            case RectConst.ACT_REJECT:
                doReject(a, level, roundNo, opinion, actualSigner, agentFor, agentReason, at);
                break;
            case RectConst.ACT_SUPPLEMENT:
                doSupplement(a, level, roundNo, opinion, actualSigner, agentFor, agentReason, at);
                break;
            case RectConst.ACT_TRANSFER:
                doTransfer(a, level, roundNo, opinion, actualSigner, agentFor, agentReason, targetLevelNo, at);
                break;
            default:
                throw new IllegalArgumentException(
                        "签收只许四种动作：通过/驳回/补充材料/转交他级，收到：" + action);
        }
        return mustGet(id);
    }

    /** 通过：进下一级；若是被转交级，则把棒送回转出本级并清掉绕行标记，由本级按主线放行。 */
    private void doPass(TIsRectApply a, TIsRectLevel level, int roundNo, String opinion,
                        String signer, String agentFor, String agentReason, Date at) {
        int curNo = level.getLevelNo();
        Integer holdFrom = a.getTransferFromNo();
        TIsRectLevel next;
        Integer keepHold;
        if (holdFrom != null) {
            // 转交是"请别级核办"的绕行：被转交级办完，棒送回转出本级、恢复成普通在途单，
            // 本级之后可放行、可驳回、可补材料、也可再转——与通常这一级没有两样。
            next = levelRuler.byNo(holdFrom);
            if (next == null) {
                throw new IllegalStateException("转出本级配置已停用或缺失，棒送不回去，请联系平台管理员");
            }
            keepHold = null;
        } else {
            next = levelRuler.nextAfter(curNo);
            keepHold = null;
        }
        int rows;
        if (next == null) {
            rows = applyMapper.closeBill(a.getId(), curNo, RectConst.ST_PASS, "全部层级审批通过", at);
        } else {
            rows = applyMapper.moveLevel(a.getId(), curNo, next.getLevelNo(), keepHold, at);
        }
        if (rows == 0) {
            throw new IllegalStateException("通过未生效：单子已被别的动作先动过");
        }
        writeSign(a, roundNo, curNo, level.getLevelName(), RectConst.ACT_PASS, opinion,
                signer, agentFor, agentReason, null, at);
    }

    /** 驳回：直接落"已驳回"结案，不许停留在流程里。 */
    private void doReject(TIsRectApply a, TIsRectLevel level, int roundNo, String opinion,
                          String signer, String agentFor, String agentReason, Date at) {
        int rows = applyMapper.closeBill(a.getId(), level.getLevelNo(),
                RectConst.ST_REJECT, clip("第" + (level.getLevelNo() + 1) + "级[" + level.getLevelName()
                        + "]驳回：" + opinion, 255), at);
        if (rows == 0) {
            throw new IllegalStateException("驳回未生效：单子已被别的动作先动过");
        }
        writeSign(a, roundNo, level.getLevelNo(), level.getLevelName(), RectConst.ACT_REJECT, opinion,
                signer, agentFor, agentReason, null, at);
    }

    /** 补充材料：退回申请人；补齐后只能 submit，从第一级、新一轮走起。 */
    private void doSupplement(TIsRectApply a, TIsRectLevel level, int roundNo, String opinion,
                              String signer, String agentFor, String agentReason, Date at) {
        int rows = applyMapper.backToApplicant(a.getId(), level.getLevelNo(), at);
        if (rows == 0) {
            throw new IllegalStateException("退回补材料未生效：单子已被别的动作先动过");
        }
        writeSign(a, roundNo, level.getLevelNo(), level.getLevelName(), RectConst.ACT_SUPPLEMENT, opinion,
                signer, agentFor, agentReason, null, at);
    }

    /** 转交他级：本級处理意见必须记全；棒的正主级留住，目标级办完通过后回正主。 */
    private void doTransfer(TIsRectApply a, TIsRectLevel level, int roundNo, String opinion,
                            String signer, String agentFor, String agentReason, Integer targetLevelNo, Date at) {
        TIsRectLevel target = levelRuler.transferTarget(level.getLevelNo(), targetLevelNo);
        if (a.getTransferFromNo() != null && a.getTransferFromNo().equals(target.getLevelNo())) {
            // 棒本来就是从正主那级转出来的：回正主该走"通过"放行，不许用转交把棒弹回去。
            throw new IllegalStateException("把棒送回正主本级请用“通过”，不能转交回去");
        }
        // 正主只记最早把棒转出来的那一级；层层转时不被后到的转手覆盖。
        int holdLevel = a.getTransferFromNo() == null ? level.getLevelNo() : a.getTransferFromNo();
        int rows = applyMapper.transferLevel(a.getId(), level.getLevelNo(),
                target.getLevelNo(), holdLevel, at);
        if (rows == 0) {
            throw new IllegalStateException("转交未生效：单子已被别的动作先动过");
        }
        String fullOpinion = "转交第" + (target.getLevelNo() + 1) + "级[" + target.getLevelName()
                + "]核办。本级意见：" + opinion;
        writeSign(a, roundNo, level.getLevelNo(), level.getLevelName(), RectConst.ACT_TRANSFER, fullOpinion,
                signer, agentFor, agentReason, target.getLevelNo(), at);
    }

    /**
     * 作废第一条路：申请人撤回。只许单子还没进任何一级签署——即从未提交过
     * （连一条提交/签署留痕都没有）的草稿。补材料退回后状态虽也是未提交，但已经
     * 进过签署，此路不成立：中途有异议走驳回，不走作废。
     */
    @Transactional(rollbackFor = Exception.class)
    public TIsRectApply withdraw(Long id, String operator, String reason, Date at) {
        TIsRectApply a = mustGet(id);
        if (!a.getApplicant().equals(operator)) {
            throw new IllegalStateException("只有申请人本人能撤回");
        }
        if (signMapper.selectCount(new QueryWrapper<TIsRectSign>().eq("apply_id", id)) > 0) {
            throw new IllegalStateException("撤回不成立：单子已经提交并进入过签署，中途有异议请走驳回，不许作废");
        }
        int rows = applyMapper.withdraw(id, clip("申请人撤回" + (blank(reason) ? "" : "：" + reason), 255), at);
        if (rows == 0) {
            throw new IllegalStateException("撤回不成立：单子已进入签署或已结案，中途有异议请走驳回，不许作废");
        }
        return mustGet(id);
    }

    /**
     * 作废第二条路：已结案单由管理员归档。结论、结案时刻原样保留，一个字都不覆盖；
     * 归档后不参与任何统计和待办。
     */
    @Transactional(rollbackFor = Exception.class)
    public TIsRectApply archive(Long id, Date at) {
        TIsRectApply a = mustGet(id);
        int rows = applyMapper.archive(id, at);
        if (rows == 0) {
            throw new IllegalStateException("归档不成立：只有已通过/已驳回的结案单能归档，当前状态："
                    + RectConst.statusText(a.getStatus()));
        }
        return mustGet(id);
    }

    /**
     * 催办扫描：审批中且在本级停留超过配置钟点的单，催一次留一条痕。
     * 同一停留段不足 {@link RectConst#URGE_AGAIN_HOURS} 钟点不重复刷；已作废/结案单不扫不催。
     *
     * @return 本次实际催办的留痕
     */
    @Transactional(rollbackFor = Exception.class)
    public List<TIsRectUrge> urgeScan(Date at) {
        List<TIsRectUrge> urged = new ArrayList<>();
        Map<Integer, TIsRectLevel> levelMap = levelRuler.enabledMap();
        List<TIsRectApply> running = applyMapper.selectList(new QueryWrapper<TIsRectApply>()
                .eq("del_flag", 0)
                .eq("status", RectConst.ST_RUNNING)
                .orderByAsc("arrive_at", "id"));
        for (TIsRectApply a : running) {
            if (a.getCurLevelNo() == null || a.getArriveAt() == null) {
                continue;
            }
            TIsRectLevel level = levelMap.get(a.getCurLevelNo());
            if (level == null || level.getTimeLimitHours() == null) {
                continue;
            }
            long stay = (at.getTime() - a.getArriveAt().getTime()) / 3600000L;
            if (stay < level.getTimeLimitHours()) {
                continue;
            }
            TIsRectUrge last = latestUrge(a.getId(), a.getCurLevelNo(), a.getArriveAt());
            if (last != null && last.getUrgedAt() != null
                    && at.getTime() - last.getUrgedAt().getTime()
                    < (long) RectConst.URGE_AGAIN_HOURS * 3600000L) {
                continue;
            }
            int rows = applyMapper.incUrge(a.getId(), at);
            if (rows == 0) {
                // 扫描期间单子被签收/作废：跳过，不留痕。
                continue;
            }
            TIsRectUrge u = new TIsRectUrge();
            u.setApplyId(a.getId());
            u.setApplyNo(a.getApplyNo());
            u.setLevelNo(level.getLevelNo());
            u.setLevelName(level.getLevelName());
            u.setSigner(level.getSigner());
            u.setStayHours((int) stay);
            u.setUrgedAt(at);
            u.setDetail("申请单[" + a.getApplyNo() + "]在第" + (level.getLevelNo() + 1) + "级[" + level.getLevelName()
                    + "]停留已 " + stay + " 钟点，超过约定 " + level.getTimeLimitHours()
                    + " 钟点，催请[" + level.getSigner() + "]尽快签收");
            u.setCreateBy("Task");
            u.setCreateTime(at);
            urgeMapper.insert(u);
            urged.add(u);
        }
        return urged;
    }

    private TIsRectApply mustGet(Long id) {
        TIsRectApply a = applyMapper.selectById(id);
        if (a == null || nz(a.getDelFlag()) == 1) {
            throw new IllegalArgumentException("申请单不存在：" + id);
        }
        return a;
    }

    private int countAction(Long applyId, String action) {
        Integer c = signMapper.selectCount(new QueryWrapper<TIsRectSign>()
                .eq("apply_id", applyId)
                .eq("action", action));
        return c == null ? 0 : c;
    }

    /** 最近一次催办：只认"当前停留段"（arriveAt 之后）的记录，绕行回到同一级时旧段催办不算数。 */
    private TIsRectUrge latestUrge(Long applyId, int levelNo, Date arriveAt) {
        QueryWrapper<TIsRectUrge> qw = new QueryWrapper<TIsRectUrge>()
                .eq("apply_id", applyId)
                .eq("level_no", levelNo);
        if (arriveAt != null) {
            qw.ge("urged_at", arriveAt);
        }
        qw.orderByDesc("urged_at", "id").last("limit 1");
        List<TIsRectUrge> list = urgeMapper.selectList(qw);
        return list.isEmpty() ? null : list.get(0);
    }

    private void writeSign(TIsRectApply a, int roundNo, int levelNo, String levelName, String action,
                           String opinion, String signer, String agentFor, String agentReason,
                           Integer targetLevelNo, Date at) {
        TIsRectSign s = new TIsRectSign();
        s.setApplyId(a.getId());
        s.setApplyNo(a.getApplyNo());
        s.setRoundNo(roundNo);
        s.setLevelNo(levelNo);
        s.setLevelName(levelName);
        s.setAction(action);
        s.setOpinion(opinion);
        s.setSigner(signer);
        s.setAgentFor(agentFor);
        s.setAgentReason(agentReason);
        s.setTargetLevelNo(targetLevelNo);
        s.setSignTime(at);
        s.setCreateBy(signer);
        s.setCreateTime(at);
        signMapper.insert(s);
    }

    private String nextApplyNo(Date at) {
        int seq = (int) applyMapper.selectCount(new QueryWrapper<TIsRectApply>()
                .likeRight("apply_no", "RA" + new SimpleDateFormat("yyyyMMdd").format(at))) + 1;
        return "RA" + new SimpleDateFormat("yyyyMMdd").format(at) + "-" + String.format("%04d", seq);
    }

    /** 我的待签（严格按当前层级法定签署人=登录人）。 */
    public List<TIsRectApply> myTodo(String loginName) {
        if (blank(loginName)) {
            return new ArrayList<>();
        }
        return applyMapper.selectMyTodo(loginName);
    }

    /** 台账翻条辅助：供服务层填层级名/签署人/停留钟点。 */
    public Map<Integer, TIsRectLevel> levelMap() {
        return levelRuler.enabledMap();
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** 结论列宽 255：拼接超长时截断，完整意见在签署留痕里（1000 格）一字不少。 */
    private static String clip(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
