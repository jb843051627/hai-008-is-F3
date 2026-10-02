package com.fc.v2.service;

import com.fc.v2.model.auto.TIsRectApply;
import com.fc.v2.model.auto.TIsRectLevel;
import com.fc.v2.model.auto.TIsRectUrge;
import com.fc.v2.model.custom.RectSignCmd;

import java.util.List;
import java.util.Map;

/**
 * 隐患整改申请审批 Service 接口（approval-chain 形状：逐级签收 + 催办）。
 *
 * 当前登录人由 Shiro 会话取；待办严格按"当前层级法定签署人=我"过滤，
 * 不是自己这层的单连看都看不到。已作废单不进待办、不进统计。
 *
 * @author fuce
 * @date 2026-10-02
 */
public interface ITIsRectApplyService {

    /** 建草稿（未提交），关联隐患必填 */
    TIsRectApply createDraft(Long hazardId, Integer hazardGrade, String remark);

    /** 草稿/补材料退回后补录（仅本人、仅未提交态） */
    TIsRectApply saveDraft(Long id, Integer hazardGrade, String remark);

    /** 提交或补材料后重新提交（永远从第一级起步；轮次自动累加） */
    TIsRectApply submit(Long id, String opinion);

    /** 签收：通过/驳回/补充材料/转交他级；代办走 cmd 三要素 */
    TIsRectApply sign(Long id, RectSignCmd cmd);

    /** 申请人撤回（只许未进任何一级签署前） */
    TIsRectApply withdraw(Long id, String reason);

    /** 管理员归档（只许已结案单；结论/结案时刻原样保留） */
    TIsRectApply archive(Long id);

    /** 催办扫描（定时任务调；超时即催、留痕，不重复刷） */
    List<TIsRectUrge> urgeScan();

    /** 我的待签：当前层级法定签署人是我的在途单 */
    List<TIsRectApply> myTodo();

    /** 台账（status 为空取除已作废外全部；显式传 4 只用于翻查作废档） */
    List<TIsRectApply> listApplies(Integer status, String applicant);

    /** 单条回查（带当前层级名/签署人/停留钟点） */
    TIsRectApply selectById(Long id);

    /** 单条全貌：申请单 + 逐级签署留痕 + 催办留痕 + 层级配置次序 */
    Map<String, Object> detail(Long id);

    /** 层级配置（次序平台写死，台账页亮明"下一级是谁"） */
    List<TIsRectLevel> listLevels();

    /** 统计台账（已作废单一律不参与） */
    Map<String, Object> stats();
}
