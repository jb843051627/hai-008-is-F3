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
 * 隐患整改审批催办留痕对象 t_is_rect_urge
 *
 * 单子在某一级卡住超过配置钟点就催，催一次一条；同一级两次催办间隔不足不重复刷。
 * 已作废单不扫不催，也不参与任何统计和待办。
 *
 * @author fuce
 * @date 2026-10-02
 */
@TableName("t_is_rect_urge")
@ApiModel(value = "TIsRectUrge", description = "隐患整改审批催办留痕")
public class TIsRectUrge implements Serializable {
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

    /** 卡住的层级 */
    @TableField("level_no")
    @ApiModelProperty(value = "卡住的层级")
    private Integer levelNo;

    /** 层级名（催办时抄存） */
    @TableField("level_name")
    @ApiModelProperty(value = "层级名")
    private String levelName;

    /** 被催办人（该级法定签署人；代办不等于换人，仍催正主） */
    @TableField("signer")
    @ApiModelProperty(value = "被催办人")
    private String signer;

    /** 开催时已在本级停留钟点 */
    @TableField("stay_hours")
    @ApiModelProperty(value = "已停留钟点")
    private Integer stayHours;

    /** 催办时刻 */
    @TableField("urged_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "催办时刻")
    private Date urgedAt;

    /** 催办事由（超约定时限多少） */
    @TableField("detail")
    @ApiModelProperty(value = "催办事由")
    private String detail;

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

    public String getSigner() {
        return signer;
    }

    public void setSigner(String signer) {
        this.signer = signer;
    }

    public Integer getStayHours() {
        return stayHours;
    }

    public void setStayHours(Integer stayHours) {
        this.stayHours = stayHours;
    }

    public Date getUrgedAt() {
        return urgedAt;
    }

    public void setUrgedAt(Date urgedAt) {
        this.urgedAt = urgedAt;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
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
