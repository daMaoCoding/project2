package dc.pay.admin.modular.biz.service.impl;

import dc.pay.admin.common.annotion.DataSource;
import dc.pay.admin.common.constant.DSEnum;
import dc.pay.mapper.admin.TestMapper;
import dc.pay.entity.admin.Test;
import dc.pay.admin.modular.biz.service.ITestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试服务
 */
@Service
public class TestServiceImpl implements ITestService {


    @Autowired
    TestMapper testMapper;

    @Override
    @DataSource(name = DSEnum.DATA_SOURCE_BIZ)
    public void testBiz() {
        Test test = testMapper.selectByPrimaryKey(1);
        test.setId(22);
        testMapper.insert(test);
    }


    @Override
    @DataSource(name = DSEnum.DATA_SOURCE_GUNS)
    public void testGuns() {
        Test test = testMapper.selectByPrimaryKey(1);
        test.setId(33);
        testMapper.insert(test);
    }

    @Override
    @Transactional
    public void testAll() {
        testBiz();
        testGuns();
        //int i = 1 / 0;
    }

}
