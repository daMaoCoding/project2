package dc.pay.admin.modular.system.controller;/**
 * Created by admin on 2017/6/6.
 */

import com.github.pagehelper.PageInfo;
import dc.pay.admin.common.annotion.Permission;
import dc.pay.base.BaseController;
import dc.pay.entity.pay.ResPayList;
import dc.pay.service.pay.ResPayListService;
import dc.pay.service.pay.ResponsePayService;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * ************************
 * @author tony 3556239829
 */

@Controller
@RequestMapping("/resPayList")
public class ResPayListController  extends BaseController {

    @Autowired
    private ResPayListService resPayListService;

    @Autowired
    private ResponsePayService responsePayService;

    @RequestMapping
    @Permission
    public ModelAndView getAll(ResPayList resPayList) {
        ModelAndView result = new ModelAndView("payList/resdb/index");
        List<ResPayList> resPayListList = resPayListService.getAll(resPayList);
        resPayListList.forEach(list-> list.setChannelCName(HandlerUtil.getChannelCNameByChannelName(list.getChannel())));
       // String allAmount = resPayListService.getAllAmount(resPayList);
       // result.addObject("allAmount", StringUtils.isBlank(allAmount)?"0":allAmount);
        result.addObject("pageInfo", new PageInfo<ResPayList>(resPayListList));
        result.addObject("queryParam", resPayList);
        result.addObject("page", resPayList.getPage());
        result.addObject("rows", resPayList.getRows());
        result.addObject("oidMaps",HandlerUtil.getAllOid());//业主们
        result.addObject("payTypeMaps", HandlerUtil.getAllPayType());//统计类型
        result.addObject("searchResultMaps", HandlerUtil.getAllSearchResult());//查询结果，成功/失败
        return result;
    }

    //@RequestMapping(value = "/add")
    public ModelAndView add() {
        ModelAndView result = new ModelAndView("payList/resdb/view");
        result.addObject("resPayList", new ResPayList());
        return result;
    }


    @RequestMapping(value = "/bufa/{id}")
    @ResponseBody
    public Object bufa(@PathVariable(required = true) Long id) {
        if(StringUtils.isNotBlank(id.toString())){
             ResPayList resPayList = resPayListService.getById(id);
             if(resPayList!=null){
                 String dbMsg = responsePayService.saveAndResDbMsgNextTime(resPayList,true);
                 return responsePayService.getResDbResult(dbMsg,"result","1")?"SUCCESS":"ERROR"; // TODO: 2018/1/10 db上线后打开
             }
        }
        return "ERROR";
    }


    @RequestMapping(value = "/view/{id}")
    public ModelAndView view(@PathVariable Long id) {
        ModelAndView result = new ModelAndView("payList/resdb/view");
        ResPayList resPayList = resPayListService.getById(id);
        result.addObject("resPayList", resPayList);
        result.getModel().put("IpHelperCZ", useStaticPacker("dc.pay.utils.ipUtil.qqwry.qqwry3.IpHelperCZ"));//freemark静态方法ip地址映射
        return result;
    }


   @RequestMapping(value = "/delete/{id}")
   @Permission
    public ModelAndView delete(@PathVariable Long id, RedirectAttributes ra) {
        ModelAndView result = new ModelAndView("redirect:/resPayList");
        resPayListService.deleteById(id);
        ra.addFlashAttribute("msg", "删除成功!");
        return result;
    }

    //@RequestMapping(value = "/save", method = RequestMethod.POST)
    public ModelAndView save(ResPayList resPayList) {
        ModelAndView result = new ModelAndView("payList/resdb/view");
        String msg = resPayList.getId() == null ? "新增成功!" : "更新成功!";
        resPayListService.save(resPayList);
        result.addObject("resPayList", resPayList);
        result.addObject("msg", msg);
        return result;
    }


}
