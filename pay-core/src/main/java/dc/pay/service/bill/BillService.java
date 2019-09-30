package dc.pay.service.bill;/**
 * Created by admin on 2017/6/5.
 */

import com.github.pagehelper.PageHelper;
import dc.pay.entity.bill.Bill;
import dc.pay.mapper.bill.BillMapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * ************************
 * @author tony 3556239829
 */
@Service
public class BillService {

    @Autowired
    BillMapper billMapper;

    public List<Bill> getAll(Bill bill) {
        if (null!=bill && bill.getPage() != null && bill.getRows() != null) {
            PageHelper.startPage(bill.getPage(), bill.getRows());
        }

        Example example = new Example(Bill.class);
        Example.Criteria criteria = example.createCriteria();

        if(null!=bill && StringUtils.isNotBlank(bill.getAPI_ORDER_ID())) {
            criteria.andEqualTo("API_ORDER_ID", bill.getAPI_ORDER_ID());
        }
        if(null!=bill && StringUtils.isNotBlank(bill.getAPI_CHANNEL_BANK_NAME()))
            criteria.andLike("API_CHANNEL_BANK_NAME", "%"+bill.getAPI_CHANNEL_BANK_NAME()+"%");

        example.setOrderByClause("API_ORDER_TIME desc");
        example.setDistinct(false);
       List<Bill>  billLists = billMapper.selectByExample(example);
        return billLists;
    }

    public Bill getById(Long id) {
        return billMapper.selectByPrimaryKey(id);
    }

    public Bill getByAPI_ORDER_ID(String API_ORDER_ID) {
        return billMapper.getByAPI_ORDER_ID(API_ORDER_ID);
    }

    public void deleteById(Long id) {
        billMapper.deleteByPrimaryKey(id);
    }

    public void save(Bill bill) {
        if (bill.getId() != null) {
            billMapper.updateByPrimaryKey(bill);
        } else {
            billMapper.insert(bill);
        }
    }




}
