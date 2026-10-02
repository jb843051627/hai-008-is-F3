package com.fc.v2.service.rect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TIsRectLevelMapper;
import com.fc.v2.model.auto.TIsRectLevel;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批层级解析：层级次序、签署人、允停钟点都只认平台配置 t_is_rect_level，
 * 申请单上只存"停在哪一级"的 level_no，下一级是谁一律问本类。
 *
 * @author fuce
 * @date 2026-10-02
 */
@Component
public class LevelRuler {

    @Resource
    private TIsRectLevelMapper levelMapper;

    /** 启用层级，按 level_no 从小到大——这就是逐级签的次序。 */
    public List<TIsRectLevel> enabledLevels() {
        List<TIsRectLevel> list = levelMapper.selectList(new QueryWrapper<TIsRectLevel>()
                .eq("del_flag", 0)
                .eq("status", 0)
                .orderByAsc("level_no", "id"));
        return list == null ? new ArrayList<TIsRectLevel>() : list;
    }

    /** level_no → 启用层级；找不到或已停用返回 null。 */
    public TIsRectLevel byNo(int levelNo) {
        List<TIsRectLevel> levels = enabledLevels();
        for (TIsRectLevel l : levels) {
            if (l.getLevelNo() != null && l.getLevelNo() == levelNo) {
                return l;
            }
        }
        return null;
    }

    /** 第一级（提交、补材料重提都落这里）。无启用层级抛错。 */
    public TIsRectLevel first() {
        List<TIsRectLevel> levels = enabledLevels();
        if (levels.isEmpty()) {
            throw new IllegalStateException("平台未配置启用的审批层级，申请无法进入流转");
        }
        return levels.get(0);
    }

    /**
     * 正常流转的下一级：按配置次序走，不认 level_no 数字加减。
     * 已在最后一级返回 null（调用方据此结案为"通过"）。
     */
    public TIsRectLevel nextAfter(int levelNo) {
        List<TIsRectLevel> levels = enabledLevels();
        for (int i = 0; i < levels.size(); i++) {
            Integer no = levels.get(i).getLevelNo();
            if (no != null && no == levelNo) {
                return i + 1 < levels.size() ? levels.get(i + 1) : null;
            }
        }
        return null;
    }

    /** 转交目标必须是启用中、且不是本级自己的另一级。 */
    public TIsRectLevel transferTarget(int curLevelNo, Integer targetNo) {
        if (targetNo == null) {
            throw new IllegalArgumentException("转交他级必须指明目标层级");
        }
        if (targetNo.equals(curLevelNo)) {
            throw new IllegalArgumentException("转交目标不能是本级自己");
        }
        TIsRectLevel target = byNo(targetNo);
        if (target == null) {
            throw new IllegalArgumentException("目标层级不存在或已停用：level_no=" + targetNo);
        }
        return target;
    }

    /** 配置速览（台账页展示次序用）。 */
    public Map<Integer, TIsRectLevel> enabledMap() {
        Map<Integer, TIsRectLevel> map = new HashMap<>();
        for (TIsRectLevel l : enabledLevels()) {
            map.put(l.getLevelNo(), l);
        }
        return map;
    }
}
