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
 * 隐患整改申请审批单对象 t_is_rect_apply
 *
 * 一张申请单一行，字段固定：申请单号、关联隐患（哪条/级别）、申请人、提交时间、
 * 当前待签层级、状态、结论、结案时间。状态只许 0未提交 1审批中 2已通过 3已驳回 4已作废。
 *
 * 当前待签层级同一时刻只有一个值；补材料重提从第一级（level_no=0）重新走起。
 * 结案（2/3）后 conclusion/closed_at 写死，单上任何一栏都不许再改。
 *
 * @author fuce
 * @date 2026-10-02
 */
@TableName("t_is_rect_apply")
@ApiModel(value = "TIsRectApply", description = "隐患整改申请审批单")
public class TIsRectApply implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 申请单号 */
    @TableField("apply_no")
    @ApiModelProperty(value = "申请单号")
    private String applyNo;

    /** 关联隐患（哪条） */
    @TableField("hazard_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "关联隐患ID")
    private Long hazardId;

    /** 隐患号（提交时抄存） */
    @TableField("hazard_no")
    @ApiModelProperty(value = "隐患号")
    private String hazardNo;

    /** 隐患级别 1一般隐患 2重大隐患 */
    @TableField("hazard_grade")
    @ApiModelProperty(value = "隐患级别 1一般隐患 2重大隐患")
    private Integer hazardGrade;

    /** 隐患概况（提交时抄存） */
    @TableField("hazard_summary")
    @ApiModelProperty(value = "隐患概况")
    private String hazardSummary;

    /** 申请人（登录账号，提交时写定） */
    @TableField("applicant")
    @ApiModelProperty(value = "申请人")
    private String applicant;

    /** 提交时间（最近一次提交；历次重提查签署留痕） */
    @TableField("submit_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "提交时间")
    private Date submitTime;

    /** 当前待签层级（唯一值；未提交/结案为空） */
    @TableField("cur_level_no")
    @ApiModelProperty(value = "当前待签层级")
    private Integer curLevelNo;

    /** 本棒正主级：被转交级办完后回正主的下一级（正常流转为空） */
    @TableField("transfer_from_no")
    @ApiModelProperty(value = "转交回正主级；正常流转为空")
    private Integer transferFromNo;

    /** 进入当前待签层级的时刻（催办超时起算点） */
    @TableField("arrive_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "进入当前层级时刻")
    private Date arriveAt;

    /** 本轮次：首次提交为1，每补材料重提一次加1 */
    @TableField("round_no")
    @ApiModelProperty(value = "本轮次")
    private Integer roundNo;

    /** 已催办次数 */
    @TableField("urge_rounds")
    @ApiModelProperty(value = "已催办次数")
    private Integer urgeRounds;

    /** 状态 0未提交 1审批中 2已通过 3已驳回 4已作废 */
    @TableField("status")
    @ApiModelProperty(value = "状态 0未提交 1审批中 2已通过 3已驳回 4已作废")
    private Integer status;

    /** 结论（结案时写死） */
    @TableField("conclusion")
    @ApiModelProperty(value = "结论（结案写死）")
    private String conclusion;

    /** 结案时间（写死） */
    @TableField("closed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "结案时间")
    private Date closedAt;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    /** 当前待签层级名（非表字段，服务层按当前配置填） */
    @TableField(exist = false)
    @ApiModelProperty(value = "当前待签层级名")
    private String curLevelName;

    /** 当前待签法定签署人（非表字段） */
    @TableField(exist = false)
    @ApiModelProperty(value = "当前待签法定签署人")
    private String curSigner;

    /** 已在当前层级停留钟点（非表字段，翻条时按 arriveAt 现算） */
    @TableField(exist = false)
    @ApiModelProperty(value = "已停留钟点")
    private Long stayHours;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApplyNo() {
        return applyNo;
    }

    public void setApplyNo(String applyNo) {
        this.applyNo = applyNo;
    }

    public Long getHazardId() {
        return hazardId;
    }

    public void setHazardId(Long hazardId) {
        this.hazardId = hazardId;
    }

    public String getHazardNo() {
        return hazardNo;
    }

    public void setHazardNo(String hazardNo) {
        this.hazardNo = hazardNo;
    }

    public Integer getHazardGrade() {
        return hazardGrade;
    }

    public void setHazardGrade(Integer hazardGrade) {
        this.hazardGrade = hazardGrade;
    }

    public String getHazardSummary() {
        return hazardSummary;
    }

    public void setHazardSummary(String hazardSummary) {
        this.hazardSummary = hazardSummary;
    }

    public String getApplicant() {
        return applicant;
    }

    public void setApplicant(String applicant) {
        this.applicant = applicant;
    }

    public Date getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime) {
        this.submitTime = submitTime;
    }

    public Integer getCurLevelNo() {
        return curLevelNo;
    }

    public void setCurLevelNo(Integer curLevelNo) {
        this.curLevelNo = curLevelNo;
    }

    public Integer getTransferFromNo() {
        return transferFromNo;
    }

    public void setTransferFromNo(Integer transferFromNo) {
        this.transferFromNo = transferFromNo;
    }

    public Date getArriveAt() {
        return arriveAt;
    }

    public void setArriveAt(Date arriveAt) {
        this.arriveAt = arriveAt;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public Integer getUrgeRounds() {
        return urgeRounds;
    }

    public void setUrgeRounds(Integer urgeRounds) {
        this.urgeRounds = urgeRounds;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public Date getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Date closedAt) {
        this.closedAt = closedAt;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
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

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getCurLevelName() {
        return curLevelName;
    }

    public void setCurLevelName(String curLevelName) {
        this.curLevelName = curLevelName;
    }

    public String getCurSigner() {
        return curSigner;
    }

    public void setCurSigner(String curSigner) {
        this.curSigner = curSigner;
    }

    public Long getStayHours() {
        return stayHours;
    }

    public void setStayHours(Long stayHours) {
        this.stayHours = stayHours;
    }
}
