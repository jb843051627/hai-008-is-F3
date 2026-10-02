package com.fc.v2.controller.admin;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TIsRectApply;
import com.fc.v2.model.custom.RectSignCmd;
import com.fc.v2.service.ITIsRectApplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 隐患整改申请审批 Controller（approval-chain 形状：逐级签收 + 催办）。
 *
 * 一张单看得见停在哪一级、谁该签、已有哪些意见；待办只对当前层级法定签署人露面。
 *
 * @author fuce
 * @date 2026-10-02
 */
@Api(value = "隐患整改申请审批")
@Controller
@RequestMapping("/isRectApply")
public class IsRectApplyController extends BaseController {

    private final String prefix = "admin/isRectApply";

    @Autowired
    private ITIsRectApplyService isRectApplyService;

    @ApiOperation(value = "审批台账跳转", notes = "当前停在哪一级、谁该签、已有意见一屏可见")
    @GetMapping("/view")
    @RequiresPermissions("isRectApply:view")
    public String view(ModelMap model) {
        model.put("levels", isRectApplyService.listLevels());
        return prefix + "/list";
    }

    @Log(title = "隐患整改申请审批台账", action = "list")
    @ApiOperation(value = "台账分页", notes = "默认不含已作废；显式 status=4 翻作废档")
    @GetMapping("/list")
    @RequiresPermissions("isRectApply:list")
    @ResponseBody
    public ResultTable list(@RequestParam(required = false) Integer status,
                            @RequestParam(required = false) String applicant) {
        startPage();
        List<TIsRectApply> list = isRectApplyService.listApplies(status, applicant);
        com.github.pagehelper.PageInfo<TIsRectApply> page =
                new com.github.pagehelper.PageInfo<>(list);
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "我的待签", action = "todo")
    @ApiOperation(value = "我的待签", notes = "只亮当前层级法定签署人是我的单；不是自己这层的看不到")
    @GetMapping("/todo")
    @RequiresPermissions("isRectApply:list")
    @ResponseBody
    public ResultTable todo() {
        startPage();
        List<TIsRectApply> list = isRectApplyService.myTodo();
        com.github.pagehelper.PageInfo<TIsRectApply> page =
                new com.github.pagehelper.PageInfo<>(list);
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "隐患整改申请单", action = "detail")
    @ApiOperation(value = "单条全貌", notes = "单子现状 + 逐级签署留痕 + 催办留痕；查变化查签署留痕")
    @GetMapping("/detail")
    @RequiresPermissions("isRectApply:list")
    @ResponseBody
    public AjaxResult detail(Long id) {
        java.util.Map<String, Object> data = isRectApplyService.detail(id);
        if (data == null) {
            return AjaxResult.error("申请单不存在");
        }
        return AjaxResult.successData(200, data);
    }

    @ApiOperation(value = "台账统计", notes = "已作废单不参与")
    @GetMapping("/stats")
    @RequiresPermissions("isRectApply:list")
    @ResponseBody
    public AjaxResult stats() {
        return AjaxResult.successData(200, isRectApplyService.stats());
    }

    @ApiOperation(value = "审批层级配置", notes = "次序平台写死，不在单子上")
    @GetMapping("/levels")
    @RequiresPermissions("isRectApply:list")
    @ResponseBody
    public AjaxResult levels() {
        return AjaxResult.successData(200, isRectApplyService.listLevels());
    }

    @Log(title = "隐患整改申请建草稿", action = "create")
    @ApiOperation(value = "建草稿（未提交）", notes = "关联隐患与级别；未进任何签署前申请人可撤回")
    @PostMapping("/create")
    @RequiresPermissions("isRectApply:create")
    @ResponseBody
    public AjaxResult create(Long hazardId, Integer hazardGrade, String remark) {
        try {
            TIsRectApply a = isRectApplyService.createDraft(hazardId, hazardGrade, remark);
            return AjaxResult.success("草稿已建：" + a.getApplyNo()).put("data", a);
        } catch (RuntimeException ex) {
            return AjaxResult.error(ex.getMessage());
        }
    }

    @Log(title = "隐患整改申请补录", action = "saveDraft")
    @ApiOperation(value = "草稿/补材料补齐", notes = "只有申请人本人、未提交态可补；补齐后提交从第一级重走")
    @PostMapping("/saveDraft")
    @RequiresPermissions("isRectApply:create")
    @ResponseBody
    public AjaxResult saveDraft(Long id, Integer hazardGrade, String remark) {
        try {
            return AjaxResult.success("已补齐，待提交").put("data", isRectApplyService.saveDraft(id, hazardGrade, remark));
        } catch (RuntimeException ex) {
            return AjaxResult.error(ex.getMessage());
        }
    }

    @Log(title = "隐患整改申请提交", action = "submit")
    @ApiOperation(value = "提交/补材料后重新提交", notes = "永远进第一级；补材料重提轮次加一，不从原处接着走")
    @PostMapping("/submit")
    @RequiresPermissions("isRectApply:submit")
    @ResponseBody
    public AjaxResult submit(Long id, String opinion) {
        try {
            TIsRectApply a = isRectApplyService.submit(id, opinion);
            return AjaxResult.success("已提交，进第一级[" + a.getCurLevelName() + "]待签").put("data", a);
        } catch (RuntimeException ex) {
            return AjaxResult.error(ex.getMessage());
        }
    }

    @Log(title = "隐患整改申请签收", action = "sign")
    @ApiOperation(value = "签收", notes = "四动作：PASS通过/REJECT驳回/SUPPLEMENT补充材料/TRANSFER转交他级；"
            + "代办带 agentFor/agentReason；转交带 targetLevelNo 且意见记全")
    @PostMapping("/sign")
    @RequiresPermissions("isRectApply:sign")
    @ResponseBody
    public AjaxResult sign(Long id, RectSignCmd cmd) {
        try {
            TIsRectApply a = isRectApplyService.sign(id, cmd);
            String msg;
            switch (a.getStatus()) {
                case 2: msg = "末级通过，申请已结案（结论已写死）"; break;
                case 3: msg = "已驳回，流程结案（不再停留在流程里）"; break;
                case 0: msg = "已退回申请人补材料；补齐后重新提交，从第一级重新走起，轮次加一"; break;
                default:
                    if ("TRANSFER".equals(cmd.getAction())) {
                        msg = "已转交他级；当前待签：第" + (a.getCurLevelNo() + 1) + "级[" + a.getCurLevelName()
                                + "] " + a.getCurSigner() + "（本棒办完回正主的下一级）";
                    } else {
                        msg = "通过，当前待签：第" + (a.getCurLevelNo() + 1) + "级[" + a.getCurLevelName()
                                + "] " + a.getCurSigner();
                    }
            }
            return AjaxResult.success(msg).put("data", a);
        } catch (RuntimeException ex) {
            return AjaxResult.error(ex.getMessage());
        }
    }

    @Log(title = "隐患整改申请撤回", action = "withdraw")
    @ApiOperation(value = "申请人撤回", notes = "只许未进任何一级签署前；签收中途有异议走驳回")
    @PostMapping("/withdraw")
    @RequiresPermissions("isRectApply:submit")
    @ResponseBody
    public AjaxResult withdraw(Long id, String reason) {
        try {
            isRectApplyService.withdraw(id, reason);
            return AjaxResult.success("已撤回作废，不再参与统计和待办");
        } catch (RuntimeException ex) {
            return AjaxResult.error(ex.getMessage());
        }
    }

    @Log(title = "隐患整改申请归档", action = "archive")
    @ApiOperation(value = "管理员归档", notes = "只许已结案单；结论/结案时刻原样保留；归档后不入统计待办")
    @PostMapping("/archive")
    @RequiresPermissions("isRectApply:archive")
    @ResponseBody
    public AjaxResult archive(Long id) {
        try {
            isRectApplyService.archive(id);
            return AjaxResult.success("已归档，结论与结案时间原样保留");
        } catch (RuntimeException ex) {
            return AjaxResult.error(ex.getMessage());
        }
    }

    @Log(title = "隐患整改审批催办", action = "urge")
    @ApiOperation(value = "立即催办扫描", notes = "超约定时限即催并留痕；同一级不重复刷；作废单不扫")
    @PostMapping("/urge")
    @RequiresPermissions("isRectApply:urge")
    @ResponseBody
    public AjaxResult urge() {
        List<?> urged = isRectApplyService.urgeScan();
        return AjaxResult.success("本次催办 " + urged.size() + " 张超时单").put("data", urged);
    }
}
