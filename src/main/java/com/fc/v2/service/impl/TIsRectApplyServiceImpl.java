package com.fc.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TIsRectApplyMapper;
import com.fc.v2.mapper.auto.TIsRectLevelMapper;
import com.fc.v2.mapper.auto.TIsRectSignMapper;
import com.fc.v2.mapper.auto.TIsRectUrgeMapper;
import com.fc.v2.model.auto.TIsRectApply;
import com.fc.v2.model.auto.TIsRectLevel;
import com.fc.v2.model.auto.TIsRectSign;
import com.fc.v2.model.auto.TIsRectUrge;
import com.fc.v2.model.custom.RectSignCmd;
import com.fc.v2.service.ITIsRectApplyService;
import com.fc.v2.service.rect.LevelRuler;
import com.fc.v2.service.rect.RectConst;
import com.fc.v2.service.rect.RectFlowTx;
import com.fc.v2.shiro.util.ShiroUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 隐患整改申请审批 Service 实现。
 *
 * 本类只做"谁在操作、翻条、统计"；真正翻状态的落库过程全在 {@link RectFlowTx} 单一事务里跑，
 * 保证状态翻档、签署留痕同成同败。当前登录人一律取 Shiro 会话，不由前端代填。
 *
 * @author fuce
 * @date 2026-10-02
 */
@Service
public class TIsRectApplyServiceImpl implements ITIsRectApplyService {

    @Resource
    private TIsRectApplyMapper applyMapper;
    @Resource
    private TIsRectSignMapper signMapper;
    @Resource
    private TIsRectUrgeMapper urgeMapper;
    @Resource
    private TIsRectLevelMapper levelMapper;
    @Resource
    private LevelRuler levelRuler;
    @Resource
    private RectFlowTx flowTx;

    @Override
    public TIsRectApply createDraft(Long hazardId, Integer hazardGrade, String remark) {
        return flowTx.createDraft(hazardId, hazardGrade, loginName(), remark, new Date());
    }

    @Override
    public TIsRectApply saveDraft(Long id, Integer hazardGrade, String remark) {
        return flowTx.saveDraft(id, loginName(), hazardGrade, remark);
    }

    @Override
    public TIsRectApply submit(Long id, String opinion) {
        TIsRectApply a = flowTx.submit(id, loginName(), opinion, new Date());
        // 回填当前层级名/签署人，供前端话术直接亮"下一个谁签"。
        fillView(a, levelRuler.enabledMap(), new Date());
        return a;
    }

    @Override
    public TIsRectApply sign(Long id, RectSignCmd cmd) {
        if (cmd == null) {
            throw new IllegalArgumentException("签收指令不能为空");
        }
        TIsRectApply a = flowTx.sign(id, cmd.getAction(), loginName(), cmd.getOpinion(),
                cmd.getAgentFor(), cmd.getAgentReason(), cmd.getTargetLevelNo(), new Date());
        fillView(a, levelRuler.enabledMap(), new Date());
        return a;
    }

    @Override
    public TIsRectApply withdraw(Long id, String reason) {
        return flowTx.withdraw(id, loginName(), reason, new Date());
    }

    @Override
    public TIsRectApply archive(Long id) {
        return flowTx.archive(id, new Date());
    }

    @Override
    public List<TIsRectUrge> urgeScan() {
        return flowTx.urgeScan(new Date());
    }

    @Override
    public List<TIsRectApply> myTodo() {
        List<TIsRectApply> list = flowTx.myTodo(loginName());
        Date now = new Date();
        for (TIsRectApply a : list) {
            fillView(a, levelRuler.enabledMap(), now);
        }
        return list;
    }

    @Override
    public List<TIsRectApply> listApplies(Integer status, String applicant) {
        QueryWrapper<TIsRectApply> qw = new QueryWrapper<TIsRectApply>().eq("del_flag", 0);
        if (status != null) {
            qw.eq("status", status);
        } else {
            // 台账默认不亮已作废档：作废单不参与任何统计和待办，需翻查显式传 status=4。
            qw.ne("status", RectConst.ST_VOID);
        }
        if (applicant != null && !applicant.trim().isEmpty()) {
            qw.eq("applicant", applicant.trim());
        }
        qw.orderByDesc("create_time", "id");
        List<TIsRectApply> list = applyMapper.selectList(qw);
        Map<Integer, TIsRectLevel> map = levelRuler.enabledMap();
        Date now = new Date();
        for (TIsRectApply a : list) {
            fillView(a, map, now);
        }
        return list;
    }

    @Override
    public TIsRectApply selectById(Long id) {
        TIsRectApply a = applyMapper.selectById(id);
        if (a == null || nz(a.getDelFlag()) == 1) {
            return null;
        }
        fillView(a, levelRuler.enabledMap(), new Date());
        return a;
    }

    @Override
    public Map<String, Object> detail(Long id) {
        TIsRectApply a = applyMapper.selectById(id);
        if (a == null || nz(a.getDelFlag()) == 1) {
            return null;
        }
        fillView(a, levelRuler.enabledMap(), new Date());
        List<TIsRectSign> signs = signMapper.selectList(new QueryWrapper<TIsRectSign>()
                .eq("apply_id", id)
                .orderByAsc("round_no", "id"));
        List<TIsRectUrge> urges = urgeMapper.selectList(new QueryWrapper<TIsRectUrge>()
                .eq("apply_id", id)
                .orderByAsc("urged_at", "id"));
        Map<String, Object> data = new HashMap<>();
        data.put("apply", a);
        data.put("signs", signs);
        data.put("urges", urges);
        data.put("levels", levelRuler.enabledLevels());
        return data;
    }

    @Override
    public List<TIsRectLevel> listLevels() {
        return levelMapper.selectList(new QueryWrapper<TIsRectLevel>()
                .eq("del_flag", 0)
                .orderByAsc("level_no", "id"));
    }

    @Override
    public Map<String, Object> stats() {
        // 已作废(status=4) 一律不参与任何统计。
        List<TIsRectApply> all = applyMapper.selectList(new QueryWrapper<TIsRectApply>()
                .eq("del_flag", 0)
                .ne("status", RectConst.ST_VOID));
        Map<String, Object> m = new HashMap<>();
        int draft = 0, running = 0, pass = 0, reject = 0;
        for (TIsRectApply a : all) {
            switch (nz(a.getStatus())) {
                case RectConst.ST_DRAFT: draft++; break;
                case RectConst.ST_RUNNING: running++; break;
                case RectConst.ST_PASS: pass++; break;
                case RectConst.ST_REJECT: reject++; break;
                default: break;
            }
        }
        m.put("total", all.size());
        m.put("draft", draft);
        m.put("running", running);
        m.put("pass", pass);
        m.put("reject", reject);
        m.put("myTodo", flowTx.myTodo(loginName()).size());
        return m;
    }

    /** 翻条补当前层级名/法定签署人/停留钟点：只从配置现取，不存单子。 */
    private void fillView(TIsRectApply a, Map<Integer, TIsRectLevel> map, Date now) {
        if (a.getCurLevelNo() != null) {
            TIsRectLevel l = map.get(a.getCurLevelNo());
            if (l != null) {
                a.setCurLevelName(l.getLevelName());
                a.setCurSigner(l.getSigner());
            }
        }
        if (a.getArriveAt() != null) {
            a.setStayHours((now.getTime() - a.getArriveAt().getTime()) / 3600000L);
        }
    }

    private String loginName() {
        return ShiroUtils.getLoginName();
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
