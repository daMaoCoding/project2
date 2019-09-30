package dc.pay.service.pay;/**
 * Created by admin on 2017/6/5.
 */

import com.github.pagehelper.PageHelper;
import dc.pay.entity.pay.ReqPayList;
import dc.pay.mapper.pay.ReqpayListMapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@Service
public class ReqPayListService {

    @Autowired
    ReqpayListMapper reqpayMapper;

    public List<ReqPayList> getAll(ReqPayList reqPay) {
        if (null!=reqPay && reqPay.getPage() != null && reqPay.getRows() != null) {
            PageHelper.startPage(reqPay.getPage(), reqPay.getRows());
        }
        Example example = new Example(ReqPayList.class);
        Example.Criteria criteria = example.createCriteria();

        if(null!=reqPay && StringUtils.isNotBlank(reqPay.getOrderId())) {
            criteria.andEqualTo("orderId", reqPay.getOrderId().trim());
        }

        if(null!=reqPay && StringUtils.isNotBlank(reqPay.getOid()) && !"ALL".equalsIgnoreCase(reqPay.getOid()))
            criteria.andEqualTo("oid", reqPay.getOid().trim());


        if(null!=reqPay && StringUtils.isNotBlank(reqPay.getChannel()) ){
            if(reqPay.getChannel().trim().split("_").length==5 ) criteria.andEqualTo("channel", reqPay.getChannel().trim());
            if(reqPay.getChannel().trim().split("_").length!=5 ) criteria.andLike("channel", reqPay.getChannel().trim()+"%");
        }



        if(null!=reqPay && StringUtils.isNotBlank(reqPay.getResult()) && !"ALL".equalsIgnoreCase(reqPay.getResult()))
            criteria.andEqualTo("result", reqPay.getResult().trim());
        if(null!=reqPay && StringUtils.isNotBlank(reqPay.getClientIp()))
            criteria.andEqualTo("clientIp", reqPay.getClientIp().trim());


        if(null!=reqPay && StringUtils.isNotBlank(reqPay.getChannelMemberId()))
            criteria.andEqualTo("channelMemberId", reqPay.getChannelMemberId().trim());
        if(null!=reqPay && StringUtils.isNotBlank(reqPay.getServerId()))
            criteria.andEqualTo("serverId", reqPay.getServerId().trim());


        example.setOrderByClause("id desc"); //,time_stmp desc
        example.setDistinct(false);
       List<ReqPayList>  reqPayLists = reqpayMapper.selectByExample(example);
        return reqPayLists;
    }


    /**
     * 订单号查询
     * @param orderId
     * @return
     */
    public ReqPayList getByOrderId(String orderId) {
        if(StringUtils.isNotBlank(orderId)){
            Example example = new Example(ReqPayList.class);
            Example.Criteria criteria = example.createCriteria();
            if( StringUtils.isNotBlank(orderId)) {
                criteria.andEqualTo("orderId", orderId);
            }
            // example.setOrderByClause("id desc"); //,time_stmp desc
            example.setDistinct(false);
            List<ReqPayList>  reqPayLists = reqpayMapper.selectByExample(example);
            if(null!=reqPayLists && reqPayLists.size()==1) return reqPayLists.get(0);
        }
        return null;
    }


    //更新浏览次数
    public int updataRestView(String orderId){
        return reqpayMapper.updataRestView(orderId);
    }



    public String getAllAmount(ReqPayList reqPayList){
        return reqpayMapper.getAllAmount(reqPayList);
    }


    public ReqPayList getById(Long id) {
        return reqpayMapper.selectByPrimaryKey(id);
    }

    public void deleteById(Long id) {
        reqpayMapper.deleteByPrimaryKey(id);
    }

    public void save(ReqPayList reqPay) {
        if (reqPay.getId() != null) {
            reqpayMapper.updateByPrimaryKey(reqPay);
        } else {
            reqpayMapper.insert(reqPay);
        }
    }



    /**
     * 跳转
     * @param response
     * @param result
     */
    public  void writeResponse(HttpServletResponse response, String result){
        response.setContentType("text/html; charset=utf-8");
        PrintWriter out = null;
        try {
            out = response.getWriter();
            if(null!=result && !"".equalsIgnoreCase(result)){
                out.println("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
                out.print(result);
                out.println("</html>");
                out.flush();
            }else{
                out.print("ERROR");
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(null!=out){
                out.close();
            }
        }

    }


    /**
     * 通过商户号获取流水表
     */
    public ReqPayList getReqpayListByMemberId(String memberId){
        if(StringUtils.isNotBlank(memberId)){
            return reqpayMapper.getReqpayListByMemberId(memberId);
        }
        return null;
    }


}
