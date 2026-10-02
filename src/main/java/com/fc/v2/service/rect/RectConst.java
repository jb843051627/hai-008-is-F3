package com.fc.v2.service.rect;

/**
 * 隐患整改申请审批流转领域常量。
 *
 * 状态只许五档，动作只许四样（外加提交留痕），全系统认这一处，不许自造。
 *
 * @author fuce
 * @date 2026-10-02
 */
public final class RectConst {

    private RectConst() {
    }

    /** 申请单状态：只许这五档 */
    public static final int ST_DRAFT = 0;
    public static final int ST_RUNNING = 1;
    public static final int ST_PASS = 2;
    public static final int ST_REJECT = 3;
    public static final int ST_VOID = 4;

    /** 签收动作（签署留痕 action） */
    public static final String ACT_SUBMIT = "SUBMIT";
    public static final String ACT_PASS = "PASS";
    public static final String ACT_REJECT = "REJECT";
    public static final String ACT_SUPPLEMENT = "SUPPLEMENT";
    public static final String ACT_TRANSFER = "TRANSFER";

    /** 隐患级别 */
    public static final int GRADE_NORMAL = 1;
    public static final int GRADE_MAJOR = 2;

    /** 同一停留段两次催办之间至少间隔的钟点（不到间隔不重复刷催办条） */
    public static final int URGE_AGAIN_HOURS = 24;

    public static String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case ST_DRAFT: return "未提交";
            case ST_RUNNING: return "审批中";
            case ST_PASS: return "已通过";
            case ST_REJECT: return "已驳回";
            case ST_VOID: return "已作废";
            default: return "未知";
        }
    }

    public static String actionText(String action) {
        if (action == null) {
            return "未知";
        }
        switch (action) {
            case ACT_SUBMIT: return "提交";
            case ACT_PASS: return "通过";
            case ACT_REJECT: return "驳回";
            case ACT_SUPPLEMENT: return "补充材料";
            case ACT_TRANSFER: return "转交他级";
            default: return action;
        }
    }

    public static String gradeText(Integer grade) {
        if (grade == null) {
            return "未分级";
        }
        switch (grade) {
            case GRADE_NORMAL: return "一般隐患";
            case GRADE_MAJOR: return "重大隐患";
            default: return "未知";
        }
    }
}
