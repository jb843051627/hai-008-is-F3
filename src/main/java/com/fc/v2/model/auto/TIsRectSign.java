package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 隐患整改申请逐级签署留痕对象 t_is_rect_sign
 *
 * 一张单可有多条，补材料往返几轮各留各的，谁也不覆盖谁。要查当时的单子就查本表，
 * 不许拿现在的单子顶替。
 *
 * 代办：signer 落代办人，agentFor 落原签署人（该是谁还是谁），agentReason 落代办原因。
 *
 * @author fuce
 * @date 2026-10-02
 */
@TableName("t_is_rect_sign")
@ApiModel(value = "TIsRectSign", description = "隐患整改申请逐级签署留痕")
public class TIsRectSign implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所属申请单 */
    @TableField("apply_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所属申请单")
    private Long applyId;

    /** 申请单号（抄存） */
    @TableField("apply_no")
    @ApiModelProperty(value = "申请单号")
    private String applyNo;

    /** 第几轮（首次提交=1，补材料重提累加） */
    @TableField("round_no")
    @ApiModelProperty(value = "第几轮")
    private Integer roundNo;

    /** 本级层级次序 */
    @TableField("level_no")
    @ApiModelProperty(value = "层级次序")
    private Integer levelNo;

    /** 层级名（落笔时抄存） */
    @TableField("level_name")
    @ApiModelProperty(value = "层级名")
    private String levelName;

    /** 动作 SUBMIT/PASS/REJECT/SUPPLEMENT/TRANSFER */
    @TableField("action")
    @ApiModelProperty(value = "动作 SUBMIT/PASS/REJECT/SUPPLEMENT/TRANSFER")
    private String action;

    /** 本级处理意见（转交也必须记全） */
    @TableField("opinion")
    @ApiModelProperty(value = "处理意见")
    private String opinion;

    /** 实际签署/操作人（代办时为代办人） */
    @TableField("signer")
    @ApiModelProperty(value = "实际签署人")
    private String signer;

    /** 原签署人（代办留痕；非代办为空） */
    @TableField("agent_for")
    @ApiModelProperty(value = "原签署人（代办时）")
    private String agentFor;

    /** 代办原因（代办留痕；非代办为空） */
    @TableField("agent_reason")
    @ApiModelProperty(value = "代办原因")
    private String agentReason;

    /** 转交目标层级（仅 TRANSFER） */
    @TableField("target_level_no")
    @ApiModelProperty(value = "转交目标层级")
    private Integer targetLevelNo;

    /** 签署/操作时刻 */
    @TableField("sign_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "签署时刻")
    private Date signTime;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getApplyId() {
        return applyId;
    }

    public void setApplyId(Long applyId) {
        this.applyId = applyId;
    }

    public String getApplyNo() {
        return applyNo;
    }

    public void setApplyNo(String applyNo) {
        this.applyNo = applyNo;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public Integer getLevelNo() {
        return levelNo;
    }

    public void setLevelNo(Integer levelNo) {
        this.levelNo = levelNo;
    }

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

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

    public String getSigner() {
        return signer;
    }

    public void setSigner(String signer) {
        this.signer = signer;
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

    public Date getSignTime() {
        return signTime;
    }

    public void setSignTime(Date signTime) {
        this.signTime = signTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
