package dc.pay.service.daifu;/**
 * Created by admin on 2017/6/5.
 */

import com.github.pagehelper.PageHelper;
import dc.pay.entity.daifu.ResDaiFuList;
import dc.pay.entity.pay.ResPayList;
import dc.pay.mapper.daifu.ResDaifuListMapper;
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
public class ResDaiFuListService {

    @Autowired
    ResDaifuListMapper resDaifuListMapper;


    public List<ResDaiFuList> getAll(ResDaiFuList resDaifu) {
        if (null!=resDaifu && resDaifu.getPage() != null && resDaifu.getRows() != null) {
            PageHelper.startPage(resDaifu.getPage(), resDaifu.getRows());
        } // return respayMapper.selectAll();

        Example example = new Example(ResDaiFuList.class);
        Example.Criteria criteria = example.createCriteria();
        if(null!=resDaifu && StringUtils.isNotBlank(resDaifu.getOrderId())) {
            criteria.andEqualTo("orderId", resDaifu.getOrderId().trim());
        }
        if(null!=resDaifu && StringUtils.isNotBlank(resDaifu.getOid()) && !"ALL".equalsIgnoreCase(resDaifu.getOid()))
            criteria.andEqualTo("oid", resDaifu.getOid().trim());
        if(null!=resDaifu && StringUtils.isNotBlank(resDaifu.getResult())  && !"ALL".equalsIgnoreCase(resDaifu.getResult()))
            criteria.andEqualTo("result", resDaifu.getResult().trim());
        if(null!=resDaifu && StringUtils.isNotBlank(resDaifu.getResDbResult())  && !"ALL".equalsIgnoreCase(resDaifu.getResDbResult()) )
            criteria.andEqualTo("resDbResult", resDaifu.getResDbResult().trim());
        if(null!=resDaifu && StringUtils.isNotBlank(resDaifu.getResDbCount()+""))
            criteria.andGreaterThanOrEqualTo("resDbCount", resDaifu.getResDbCount());

        if(null!=resDaifu && StringUtils.isNotBlank(resDaifu.getChannel()) ){
            if(resDaifu.getChannel().trim().split("_").length==5 ) criteria.andEqualTo("channel", resDaifu.getChannel().trim());
            if(resDaifu.getChannel().trim().split("_").length!=5 ) criteria.andLike("channel", resDaifu.getChannel().trim()+"%");
        }


        if(null!=resDaifu && StringUtils.isNotBlank(resDaifu.getResDaifuRemoteIp()))
            criteria.andEqualTo("resDaifuRemoteIp", resDaifu.getResDaifuRemoteIp().trim());

        if(null!=resDaifu && StringUtils.isNotBlank(resDaifu.getChannelMemberId()))
            criteria.andEqualTo("channelMemberId", resDaifu.getChannelMemberId());
        example.setOrderByClause("id desc"); //,time_stmp desc
      //  example.setDistinct(true);
        List<ResDaiFuList>  resPayLists = resDaifuListMapper.selectByExample(example);
        return resPayLists;
    }

    /**
     * 查找通道中文名称
     */
    public static String  getChannelCNameByChannelName(String channelName){
        return  HandlerUtil.getChannelCNameByChannelName(channelName);
    }

    public ResDaiFuList getById(Long id) {
        return resDaifuListMapper.selectByPrimaryKey(id);
    }

    public void deleteById(Long id) {
        resDaifuListMapper.deleteByPrimaryKey(id);
    }

    public void save(ResDaiFuList resDaiFuList) {
        if (resDaiFuList.getId() != null) {
            resDaifuListMapper.updateByPrimaryKey(resDaiFuList);
        } else {
            resDaifuListMapper.insert(resDaiFuList);
        }
    }

    public String getAllAmount(ResDaiFuList resDaiFuList) {
        return resDaifuListMapper.getAllAmount(resDaiFuList);
    }
}
