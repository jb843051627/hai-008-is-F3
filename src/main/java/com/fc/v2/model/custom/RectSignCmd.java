package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 隐患整改申请签收指令。
 *
 * 动作只许四样；转交必带 targetLevelNo；代办必带 agentFor/agentReason 三要素。
 *
 * @author fuce
 * @date 2026-10-02
 */
@ApiModel(value = "RectSignCmd", description = "签收指令（通过/驳回/补充材料/转交他级）")
public class RectSignCmd {

    @ApiModelProperty(value = "动作 PASS/REJECT/SUPPLEMENT/TRANSFER", required = true)
    private String action;

    @ApiModelProperty(value = "本级处理意见（每一级签自己的意见；转交也要记全）", required = true)
    private String opinion;

    @ApiModelProperty(value = "代办时的原签署人（非代办留空）")
    private String agentFor;

    @ApiModelProperty(value = "代办原因（非代办留空）")
    private String agentReason;

    @ApiModelProperty(value = "转交目标层级 level_no（仅 TRANSFER）")
    private Integer targetLevelNo;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }

    public String getAgentFor() {
        return agentFor;
    }

    public void setAgentFor(String agentFor) {
        this.agentFor = agentFor;
    }

    public String getAgentReason() {
        return agentReason;
    }

    public void setAgentReason(String agentReason) {
        this.agentReason = agentReason;
    }

    public Integer getTargetLevelNo() {
        return targetLevelNo;
    }

    public void setTargetLevelNo(Integer targetLevelNo) {
        this.targetLevelNo = targetLevelNo;
    }
}
