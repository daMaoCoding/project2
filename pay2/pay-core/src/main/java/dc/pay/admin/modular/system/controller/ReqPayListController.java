package dc.pay.admin.modular.system.controller;/**
 * Created by admin on 2017/6/6.
 */

import com.github.pagehelper.PageInfo;
import dc.pay.admin.common.annotion.Permission;
import dc.pay.base.BaseController;
import dc.pay.entity.pay.ReqPayList;
import dc.pay.service.pay.ReqPayListService;
import dc.pay.utils.HandlerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * ************************
 *  请求支付流水信息
 * @author tony 3556239829
 */

@Controller
@RequestMapping("/reqPayList")
public class ReqPayListController extends BaseController {

    @Autowired
    private ReqPayListService reqPayListService;

    @RequestMapping
    @Permission
    public ModelAndView getAll(ReqPayList reqPayList) {
        ModelAndView result = new ModelAndView("payList/reqdb/index");
        List<ReqPayList> reqPayListList = reqPayListService.getAll(reqPayList);
        reqPayListList.forEach(list-> list.setChannelCName(HandlerUtil.getChannelCNameByChannelName(list.getChannel())));
       // String allAmount = reqPayListService.getAllAmount(reqPayList);
       // result.addObject("allAmount", StringUtils.isBlank(allAmount)?"0":allAmount);
        result.addObject("pageInfo", new PageInfo<ReqPayList>(reqPayListList));
        result.addObject("queryParam", reqPayList);
        result.addObject("page", reqPayList.getPage());
        result.addObject("rows", reqPayList.getRows());
        result.addObject("oidMaps",HandlerUtil.getAllOid());//业主们
        result.addObject("payTypeMaps", HandlerUtil.getAllPayType());//统计类型
        result.addObject("searchResultMaps", HandlerUtil.getAllSearchResult());//查询结果，成功/失败
        return result;
    }

    //@RequestMapping(value = "/add")
    public ModelAndView add() {
        ModelAndView result = new ModelAndView("payList/reqdb/view");
        result.addObject("reqPayList", new ReqPayList());
        return result;
    }

    @RequestMapping(value = "/view/{id}")
    public ModelAndView view(@PathVariable Long id) {
        ModelAndView result = new ModelAndView("payList/reqdb/view");
        ReqPayList reqPayList = reqPayListService.getById(id);
        result.addObject("reqPayList", reqPayList);
        result.getModel().put("IpHelperCZ", useStaticPacker("dc.pay.utils.ipUtil.qqwry.qqwry3.IpHelperCZ"));//freemark静态方法ip地址映射
        return result;
    }





     @RequestMapping(value = "/delete/{id}")
     @Permission
    public ModelAndView delete(@PathVariable Long id, RedirectAttributes ra) {
        ModelAndView result = new ModelAndView("redirect:/reqPayList");
        reqPayListService.deleteById(id);
        ra.addFlashAttribute("msg", "删除成功!");
        return result;
    }

    //@RequestMapping(value = "/save", method = RequestMethod.POST)
    public ModelAndView save(ReqPayList reqPayList) {
        ModelAndView result = new ModelAndView("payList/reqdb/view");
        String msg = reqPayList.getId() == null ? "新增成功!" : "更新成功!";
        reqPayListService.save(reqPayList);
        result.addObject("reqPayList", reqPayList);
        result.addObject("msg", msg);
        return result;
    }


}
