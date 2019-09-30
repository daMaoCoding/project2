package dc.pay.service.pay;/**
 * Created by admin on 2017/6/5.
 */

import com.github.pagehelper.PageHelper;
import dc.pay.entity.pay.ResPayList;
import dc.pay.mapper.pay.RespayListMapper;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@Service
public class ResPayListService {

    @Autowired
    RespayListMapper respayMapper;


    public List<ResPayList> getAll(ResPayList resPay) {
        if (null!=resPay && resPay.getPage() != null && resPay.getRows() != null) {
            PageHelper.startPage(resPay.getPage(), resPay.getRows());
        } // return respayMapper.selectAll();

        Example example = new Example(ResPayList.class);
        Example.Criteria criteria = example.createCriteria();

        if(null!=resPay && StringUtils.isNotBlank(resPay.getOrderId())) {
            criteria.andEqualTo("orderId", resPay.getOrderId().trim());
        }

        if(null!=resPay && StringUtils.isNotBlank(resPay.getOid()) && !"ALL".equalsIgnoreCase(resPay.getOid()))
            criteria.andEqualTo("oid", resPay.getOid().trim());
        if(null!=resPay && StringUtils.isNotBlank(resPay.getResult())  && !"ALL".equalsIgnoreCase(resPay.getResult()))
            criteria.andEqualTo("result", resPay.getResult().trim());
        if(null!=resPay && StringUtils.isNotBlank(resPay.getResDbResult())  && !"ALL".equalsIgnoreCase(resPay.getResDbResult()) )
            criteria.andEqualTo("resDbResult", resPay.getResDbResult().trim());
        if(null!=resPay && StringUtils.isNotBlank(resPay.getResDbCount()+""))
            criteria.andGreaterThanOrEqualTo("resDbCount", resPay.getResDbCount());

        if(null!=resPay && StringUtils.isNotBlank(resPay.getChannel()) ){
            if(resPay.getChannel().trim().split("_").length==5 ) criteria.andEqualTo("channel", resPay.getChannel().trim());
            if(resPay.getChannel().trim().split("_").length!=5 ) criteria.andLike("channel", resPay.getChannel().trim()+"%");
        }


        if(null!=resPay && StringUtils.isNotBlank(resPay.getResPayRemoteIp()))
            criteria.andEqualTo("resPayRemoteIp", resPay.getResPayRemoteIp().trim());

        if(null!=resPay && StringUtils.isNotBlank(resPay.getChannelMemberId()))
            criteria.andEqualTo("channelMemberId", resPay.getChannelMemberId());
        example.setOrderByClause("id desc"); //,time_stmp desc
      //  example.setDistinct(true);
        List<ResPayList>  resPayLists = respayMapper.selectByExample(example);
        return resPayLists;
    }

    /**
     * 查找通道中文名称
     */
    public static String  getChannelCNameByChannelName(String channelName){
        return  HandlerUtil.getChannelCNameByChannelName(channelName);
    }

    public ResPayList getById(Long id) {
        return respayMapper.selectByPrimaryKey(id);
    }

    public void deleteById(Long id) {
        respayMapper.deleteByPrimaryKey(id);
    }

    public void save(ResPayList resPay) {
        if(null==resPay || StringUtils.isBlank(resPay.getOrderId())) return;
        if (resPay.getId() != null) {
            respayMapper.updateByPrimaryKey(resPay);
        } else {
            respayMapper.insert(resPay);
        }
    }

    public String getAllAmount(ResPayList resPayList) {
        return respayMapper.getAllAmount(resPayList);
    }
}
