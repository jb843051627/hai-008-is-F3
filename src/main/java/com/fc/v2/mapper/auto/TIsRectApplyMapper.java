package com.fc.v2.mapper.auto;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fc.v2.model.auto.TIsRectApply;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/**
 * 隐患整改申请审批单数据层。
 *
 * 每一次流转都是"带当前状态/当前层级条件"的原子更新：条件不符（已被别人先动、
 * 已结案、不在这一级）更新 0 行，调用方按 0 行拒绝。当前待签层级全库只有一个值，
 * 不允许一张单同时挂两个人手上。
 *
 * @author fuce
 * @date 2026-10-02
 */
public interface TIsRectApplyMapper extends BaseMapper<TIsRectApply> {

    /**
     * 提交 / 补材料后重新提交：未提交(0) → 审批中(1)，永远落在第一级(level_no=0)。
     * round_no 由调用方带死：首提 1，补材料重提 +1，绝不从原处接着走。
     */
    @Update("UPDATE t_is_rect_apply SET "
            + " status = 1, cur_level_no = 0, transfer_from_no = NULL, "
            + " arrive_at = #{at}, submit_time = #{at}, round_no = #{roundNo}, update_time = #{at} "
            + " WHERE id = #{id} AND status = 0 AND del_flag = 0")
    int submit(@Param("id") Long id,
               @Param("roundNo") int roundNo,
               @Param("at") Date at);

    /**
     * 通过走到下一级：仍在审批中，只挪当前层级。本级若是被转交级(toTransferFrom 非空)，
     * 办完回正主的下一级；正常流转 toTransferFrom 传 null。
     */
    @Update("UPDATE t_is_rect_apply SET "
            + " cur_level_no = #{toLevel}, transfer_from_no = #{toTransferFrom}, "
            + " arrive_at = #{at}, update_time = #{at} "
            + " WHERE id = #{id} AND status = 1 AND cur_level_no = #{expectLevel} AND del_flag = 0")
    int moveLevel(@Param("id") Long id,
                  @Param("expectLevel") int expectLevel,
                  @Param("toLevel") int toLevel,
                  @Param("toTransferFrom") Integer toTransferFrom,
                  @Param("at") Date at);

    /**
     * 结案：末级通过(2) 或任一级驳回(3)。当前层级清空、结论与结案时刻同一刻写死，
     * 此后任何更新入口都不许再碰这张终态单（状态条件挡死）。
     */
    @Update("UPDATE t_is_rect_apply SET "
            + " status = #{toStatus}, cur_level_no = NULL, transfer_from_no = NULL, arrive_at = NULL, "
            + " conclusion = #{conclusion}, closed_at = #{at}, update_time = #{at} "
            + " WHERE id = #{id} AND status = 1 AND cur_level_no = #{expectLevel} AND del_flag = 0")
    int closeBill(@Param("id") Long id,
                  @Param("expectLevel") int expectLevel,
                  @Param("toStatus") int toStatus,
                  @Param("conclusion") String conclusion,
                  @Param("at") Date at);

    /**
     * 补充材料：退回申请人(0)，清空当前层级。补齐后只能走 submit 从第一级重新走起。
     */
    @Update("UPDATE t_is_rect_apply SET "
            + " status = 0, cur_level_no = NULL, transfer_from_no = NULL, arrive_at = NULL, update_time = #{at} "
            + " WHERE id = #{id} AND status = 1 AND cur_level_no = #{expectLevel} AND del_flag = 0")
    int backToApplicant(@Param("id") Long id,
                        @Param("expectLevel") int expectLevel,
                        @Param("at") Date at);

    /**
     * 转交他级：审批中不结案，当前层级挪到目标级。holdFrom 记录本棒正主级
     * （层层转时由调用方保留最早转出的那一级，不被后到转手覆盖），目标级办完通过回正主的下一级。
     */
    @Update("UPDATE t_is_rect_apply SET "
            + " cur_level_no = #{targetLevel}, transfer_from_no = #{holdFrom}, "
            + " arrive_at = #{at}, update_time = #{at} "
            + " WHERE id = #{id} AND status = 1 AND cur_level_no = #{expectLevel} AND del_flag = 0")
    int transferLevel(@Param("id") Long id,
                      @Param("expectLevel") int expectLevel,
                      @Param("targetLevel") int targetLevel,
                      @Param("holdFrom") int holdFrom,
                      @Param("at") Date at);

    /**
     * 申请人撤回（第一种作废）：只许在未提交(0) 时作废——进入任何一级签署后连这条件都进不来。
     * 不写 closed_at：撤回不是结案。
     */
    @Update("UPDATE t_is_rect_apply SET "
            + " status = 4, conclusion = #{conclusion}, update_time = #{at} "
            + " WHERE id = #{id} AND status = 0 AND del_flag = 0")
    int withdraw(@Param("id") Long id,
                 @Param("conclusion") String conclusion,
                 @Param("at") Date at);

    /**
     * 管理员归档（第二种作废）：只许对已结案(2/3) 的单翻旗；结论、结案时刻原样保留，一个字都不覆盖。
     */
    @Update("UPDATE t_is_rect_apply SET status = 4, update_time = #{at} "
            + " WHERE id = #{id} AND status IN (2, 3) AND del_flag = 0")
    int archive(@Param("id") Long id, @Param("at") Date at);

    /** 催办计数原子加一（只认审批中的单；作废/结案单扫不到也加不上）。 */
    @Update("UPDATE t_is_rect_apply SET urge_rounds = IFNULL(urge_rounds, 0) + 1, update_time = #{at} "
            + " WHERE id = #{id} AND status = 1 AND del_flag = 0")
    int incUrge(@Param("id") Long id, @Param("at") Date at);

    /**
     * 我的待签：能看见的严格等于"当前层级法定签署人是我"的在途单。
     * 层级配置是唯一口径——不是自己这层的，连这查询都进不来。
     */
    @Select("SELECT a.* FROM t_is_rect_apply a "
            + " INNER JOIN t_is_rect_level l ON l.level_no = a.cur_level_no "
            + "     AND l.del_flag = 0 AND l.status = 0 "
            + " WHERE a.del_flag = 0 AND a.status = 1 AND l.signer = #{loginName} "
            + " ORDER BY a.arrive_at ASC, a.id ASC")
    List<TIsRectApply> selectMyTodo(@Param("loginName") String loginName);
}
