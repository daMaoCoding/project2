/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2016 abel533@gmail.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package dc.pay.controller.bill;

import com.github.pagehelper.PageInfo;
import dc.pay.entity.bill.Bill;
import dc.pay.service.bill.BillService;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;


@Controller
@RequestMapping("/bills")
public class BillController {
    private static final BeansWrapper beansWrapper = new BeansWrapperBuilder(Configuration.VERSION_2_3_21).build();
    @Autowired
    private BillService billService;

    @RequestMapping
    public ModelAndView getAll(Bill bill) {
        ModelAndView result = new ModelAndView("bill/index");
        List<Bill> countryList = billService.getAll(bill);
        result.addObject("pageInfo", new PageInfo<Bill>(countryList));
        result.addObject("queryParam", bill);
        result.addObject("page", bill.getPage());
        result.addObject("rows", bill.getRows());
        return result;
    }

    @RequestMapping(value = "/add")
    public ModelAndView add() {
        ModelAndView result = new ModelAndView("bill/view");
        result.addObject("bill", new Bill());
        result.addObject("statics", beansWrapper.getStaticModels());
        result.addObject("test", "123456789");
        return result;
    }

    @RequestMapping(value = "/view/{id}")
    public ModelAndView view(@PathVariable Long id) {
        ModelAndView result = new ModelAndView("bill/view");
        Bill bill = billService.getById(id);
        result.addObject("bill", bill);
        return result;
    }

    @RequestMapping(value = "/delete/{id}")
    public ModelAndView delete(@PathVariable Long id, RedirectAttributes ra) {
        ModelAndView result = new ModelAndView("redirect:/bills");
        billService.deleteById(id);
        ra.addFlashAttribute("msg", "删除成功!");
        return result;
    }

    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public ModelAndView save(Bill bill) {
        ModelAndView result = new ModelAndView("bill/view");
        String msg = bill.getId() == null ? "新增成功!" : "更新成功!";
        billService.save(bill);
        result.addObject("bill", bill);
        result.addObject("msg", msg);
        return result;
    }
}