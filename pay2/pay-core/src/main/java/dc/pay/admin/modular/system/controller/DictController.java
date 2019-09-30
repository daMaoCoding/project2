package dc.pay.admin.modular.system.controller;

import dc.pay.admin.common.annotion.Permission;
import dc.pay.admin.common.annotion.log.BussinessLog;
import dc.pay.admin.common.constant.Const;
import dc.pay.admin.common.constant.factory.ConstantFactory;
import dc.pay.admin.common.controller.BaseController;
import dc.pay.admin.common.exception.BizExceptionEnum;
import dc.pay.admin.common.exception.BussinessException;
import dc.pay.mapper.admin.DictMapper;
import dc.pay.entity.admin.Dict;
import dc.pay.admin.core.log.LogObjectHolder;
import dc.pay.admin.core.util.ToolUtil;
import dc.pay.admin.modular.system.service.IDictService;
import dc.pay.admin.modular.system.warpper.DictWarpper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 字典控制器
 */
@Controller
@RequestMapping("/dict")
public class DictController extends BaseController {

    @Resource
    DictMapper dictMapper;
    @Resource
    IDictService dictService;
    private String PREFIX = "system/dict/";

    /**
     * 跳转到字典管理首页
     */
    @RequestMapping("")
    public String index() {
        return PREFIX + "dict.html";
    }

    /**
     * 跳转到添加字典
     */
    @RequestMapping("/dict_add")
    public String deptAdd() {
        return PREFIX + "dict_add.html";
    }

    /**
     * 跳转到修改字典
     */
    @Permission(Const.ADMIN_NAME)
    @RequestMapping("/dict_edit/{dictId}")
    public String deptUpdate(@PathVariable Integer dictId, Model model) {
        Dict dict = dictMapper.selectByPrimaryKey(dictId);
        model.addAttribute("dict", dict);
        Dict queryModel = new Dict();
        dict.setPid(dictId);
        List<Dict> subDicts = dictMapper.select(queryModel);
        model.addAttribute("subDicts", subDicts);
        LogObjectHolder.me().set(dict);
        return PREFIX + "dict_edit.html";
    }

    /**
     * 新增字典
     * @param dictValues 格式例如   "1:启用;2:禁用;3:冻结"
     */
    @BussinessLog(value = "添加字典记录", key = "dictName,dictValues", dict = dc.pay.admin.common.constant.Dict.DictMap)
    @RequestMapping(value = "/add")
    @Permission(Const.ADMIN_NAME)
    @ResponseBody
    public Object add(String dictName, String dictValues) {
        if (ToolUtil.isOneEmpty(dictName, dictValues)) {
            throw new BussinessException(BizExceptionEnum.REQUEST_NULL);
        }
        dictService.addDict(dictName, dictValues);
        return SUCCESS_TIP;
    }

    /**
     * 获取所有字典列表
     */
    @RequestMapping(value = "/list")
    @Permission(Const.ADMIN_NAME)
    @ResponseBody
    public Object list(String condition) {
        List<Map<String, Object>> list = dictMapper.list(condition);
        return super.warpObject(new DictWarpper(list));
    }

    /**
     * 字典详情
     */
    @RequestMapping(value = "/detail/{dictId}")
    @Permission(Const.ADMIN_NAME)
    @ResponseBody
    public Object detail(@PathVariable("dictId") Integer dictId) {
        return dictMapper.selectByPrimaryKey(dictId);
    }

    /**
     * 修改字典
     */
    @BussinessLog(value = "修改字典", key = "dictName,dictValues", dict = dc.pay.admin.common.constant.Dict.DictMap)
    @RequestMapping(value = "/update")
    @Permission(Const.ADMIN_NAME)
    @ResponseBody
    public Object update(Integer dictId, String dictName, String dictValues) {
        if (ToolUtil.isOneEmpty(dictId, dictName, dictValues)) {
            throw new BussinessException(BizExceptionEnum.REQUEST_NULL);
        }
        dictService.editDict(dictId, dictName, dictValues);
        return super.SUCCESS_TIP;
    }

    /**
     * 删除字典记录
     */
    @BussinessLog(value = "删除字典记录", key = "dictId", dict = dc.pay.admin.common.constant.Dict.DeleteDict)
    @RequestMapping(value = "/delete")
    @Permission(Const.ADMIN_NAME)
    @ResponseBody
    public Object delete(@RequestParam Integer dictId) {

        //缓存被删除的名称
        LogObjectHolder.me().set(ConstantFactory.me().getDictName(dictId));

        dictService.delteDict(dictId);
        return SUCCESS_TIP;
    }

}
