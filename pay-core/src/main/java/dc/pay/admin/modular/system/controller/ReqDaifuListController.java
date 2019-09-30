package dc.pay.admin.modular.system.controller;/**
 * Created by admin on 2017/6/6.
 */

import com.github.pagehelper.PageInfo;
import dc.pay.admin.common.annotion.Permission;
import dc.pay.base.BaseController;
import dc.pay.entity.daifu.ReqDaiFuList;
import dc.pay.entity.pay.ReqPayList;
import dc.pay.service.daifu.ReqDaiFuListService;
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
 * @author tony 3556239829
 */

@Controller
@RequestMapping("/reqDaifuList")
public class ReqDaifuListController extends BaseController {

    @Autowired
    private ReqDaiFuListService reqDaiFuListService;

    @RequestMapping
    @Permission
    public ModelAndView getAll(ReqDaiFuList reqDaiFuList) {
        ModelAndView result = new ModelAndView("daifuList/reqdb/index");
        List<ReqDaiFuList> reqDaifuListList = reqDaiFuListService.getAll(reqDaiFuList);
        reqDaifuListList.forEach(list-> list.setChannelCName(HandlerUtil.getChannelCNameByChannelName(list.getChannel())));
       // String allAmount = reqPayListService.getAllAmount(reqPayList);
       // result.addObject("allAmount", StringUtils.isBlank(allAmount)?"0":allAmount);
        result.addObject("pageInfo", new PageInfo<ReqDaiFuList>(reqDaifuListList));
        result.addObject("queryParam", reqDaiFuList);
        result.addObject("page", reqDaiFuList.getPage());
        result.addObject("rows", reqDaiFuList.getRows());
        result.addObject("oidMaps",HandlerUtil.getAllOid());//业主们
        result.addObject("payTypeMaps", HandlerUtil.getAllPayType());//统计类型
        result.addObject("searchResultMaps", HandlerUtil.getAllSearchResult());//查询结果，成功/失败
        return result;
    }

    //@RequestMapping(value = "/add")
    public ModelAndView add() {
        ModelAndView result = new ModelAndView("daifuList/reqdb/view");
        result.addObject("reqPayList", new ReqPayList());
        return result;
    }

    @RequestMapping(value = "/view/{id}")
    public ModelAndView view(@PathVariable Long id) {
        ModelAndView result = new ModelAndView("daifuList/reqdb/view");
        ReqDaiFuList reqDaiFuList = reqDaiFuListService.getById(id);
        result.addObject("reqDaifuList", reqDaiFuList);
        result.getModel().put("IpHelperCZ", useStaticPacker("dc.pay.utils.ipUtil.qqwry.qqwry3.IpHelperCZ"));//freemark静态方法ip地址映射
        return result;
    }





     @RequestMapping(value = "/delete/{id}")
     @Permission
    public ModelAndView delete(@PathVariable Long id, RedirectAttributes ra) {
        ModelAndView result = new ModelAndView("redirect:/reqDaifuList");
         reqDaiFuListService.deleteById(id);
        ra.addFlashAttribute("msg", "删除成功!");
        return result;
    }

    //@RequestMapping(value = "/save", method = RequestMethod.POST)
    public ModelAndView save(ReqDaiFuList reqDaiFuList) {
        ModelAndView result = new ModelAndView("daifuList/reqdb/view");
        String msg = reqDaiFuList.getId() == null ? "新增成功!" : "更新成功!";
        reqDaiFuListService.save(reqDaiFuList);
        result.addObject("reqDaifuList", reqDaiFuList);
        result.addObject("msg", msg);
        return result;
    }


}
