package dc.pay.service.daifu;/**
 * Created by admin on 2017/6/5.
 */

import com.github.pagehelper.PageHelper;
import dc.pay.entity.daifu.ReqDaiFuList;
import dc.pay.entity.pay.ReqPayList;
import dc.pay.mapper.daifu.ReqDaifuListMapper;
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
public class ReqDaiFuListService {


    @Autowired
    ReqDaifuListMapper reqDaifuListMapper;


    /**
     * 订单号查询
     */
    public ReqDaiFuList getByOrderId(String orderId) {
        if(StringUtils.isNotBlank(orderId)){
            Example example = new Example(ReqDaiFuList.class);
            Example.Criteria criteria = example.createCriteria();
            if( StringUtils.isNotBlank(orderId)) {
                criteria.andEqualTo("orderId", orderId);
            }
            // example.setOrderByClause("id desc"); //,time_stmp desc
            example.setDistinct(false);
            List<ReqDaiFuList>  reqDaiFuList = reqDaifuListMapper.selectByExample(example);
            if(null!=reqDaiFuList && reqDaiFuList.size()==1) return reqDaiFuList.get(0);
        }
        return null;
    }



    public void save(ReqDaiFuList reqDaiFuList) {
        if (reqDaiFuList.getId() != null) {
            reqDaifuListMapper.updateByPrimaryKey(reqDaiFuList);
        } else {
            reqDaifuListMapper.insert(reqDaiFuList);
        }
    }



    public List<ReqDaiFuList> getAll(ReqDaiFuList reqDaiFuList) {
        if (null!=reqDaiFuList && reqDaiFuList.getPage() != null && reqDaiFuList.getRows() != null) {
            PageHelper.startPage(reqDaiFuList.getPage(), reqDaiFuList.getRows());
        }
        Example example = new Example(ReqPayList.class);
        Example.Criteria criteria = example.createCriteria();

        if(null!=reqDaiFuList && StringUtils.isNotBlank(reqDaiFuList.getOrderId())) {
            criteria.andEqualTo("orderId", reqDaiFuList.getOrderId().trim());
        }

        if(null!=reqDaiFuList && StringUtils.isNotBlank(reqDaiFuList.getOid()) && !"ALL".equalsIgnoreCase(reqDaiFuList.getOid()))
            criteria.andEqualTo("oid", reqDaiFuList.getOid().trim());


        if(null!=reqDaiFuList && StringUtils.isNotBlank(reqDaiFuList.getChannel()) ){
            if(reqDaiFuList.getChannel().trim().split("_").length==5 ) criteria.andEqualTo("channel", reqDaiFuList.getChannel().trim());
            if(reqDaiFuList.getChannel().trim().split("_").length!=5 ) criteria.andLike("channel", reqDaiFuList.getChannel().trim()+"%");
        }


        if(null!=reqDaiFuList && StringUtils.isNotBlank(reqDaiFuList.getResult()) && !"ALL".equalsIgnoreCase(reqDaiFuList.getResult()))
            criteria.andEqualTo("result", reqDaiFuList.getResult().trim());
        if(null!=reqDaiFuList && StringUtils.isNotBlank(reqDaiFuList.getClientIp()))
            criteria.andEqualTo("clientIp", reqDaiFuList.getClientIp().trim());


        if(null!=reqDaiFuList && StringUtils.isNotBlank(reqDaiFuList.getChannelMemberId()))
            criteria.andEqualTo("channelMemberId", reqDaiFuList.getChannelMemberId().trim());
        if(null!=reqDaiFuList && StringUtils.isNotBlank(reqDaiFuList.getServerId()))
            criteria.andEqualTo("serverId", reqDaiFuList.getServerId().trim());

        example.setOrderByClause("id desc"); //,time_stmp desc
        example.setDistinct(false);
       List<ReqDaiFuList>  reqDaiFuLists = reqDaifuListMapper.selectByExample(example);
        return reqDaiFuLists;
    }




    public String getAllAmount(ReqDaiFuList reqDaiFuList){
        return reqDaifuListMapper.getAllAmount(reqDaiFuList);
    }


    public ReqDaiFuList getById(Long id) {
        return reqDaifuListMapper.selectByPrimaryKey(id);
    }

    public void deleteById(Long id) {
        reqDaifuListMapper.deleteByPrimaryKey(id);
    }



    /**
     * 通过商户号获取流水表
     */
    public ReqDaiFuList getReqDaifuListByMemberId(String memberId){
        ReqDaiFuList reqDaiFuList = null;
        if(StringUtils.isNotBlank(memberId)) {
            reqDaiFuList = reqDaifuListMapper.getReqDaiFuListByMemberId(memberId);
            if (null == reqDaiFuList) reqDaiFuList = reqDaifuListMapper.getReqDaiFuListByMemberIdIncludeTestOrder(memberId);
        }
        return reqDaiFuList;
    }



}
