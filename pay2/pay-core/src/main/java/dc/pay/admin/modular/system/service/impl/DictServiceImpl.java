package dc.pay.admin.modular.system.service.impl;

import dc.pay.admin.common.exception.BizExceptionEnum;
import dc.pay.admin.common.exception.BussinessException;
import dc.pay.mapper.admin.DictMapper;
import dc.pay.entity.admin.Dict;
import dc.pay.admin.modular.system.service.IDictService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

import static dc.pay.admin.common.constant.factory.MutiStrFactory.*;

@Service
@Transactional
public class DictServiceImpl implements IDictService {

    @Resource
    DictMapper dictMapper;

    @Override
    public void addDict(String dictName, String dictValues) {
        //判断有没有该字典
        Example example = new Example(Dict.class);
        example.createCriteria().andEqualTo("name", dictName).andEqualTo("pid", 0);
        List<Dict> dicts = dictMapper.selectByExample(example);
        if(dicts != null && dicts.size() > 0){
            throw new BussinessException(BizExceptionEnum.DICT_EXISTED);
        }

        //解析dictValues
        List<Map<String, String>> items = parseKeyValue(dictValues);

        //添加字典
        Dict dict = new Dict();
        dict.setName(dictName);
        dict.setNum(0);
        dict.setPid(0);
        this.dictMapper.insert(dict);

        //添加字典条目
        for (Map<String, String> item : items) {
            String num = item.get(MUTI_STR_KEY);
            String name = item.get(MUTI_STR_VALUE);
            Dict itemDict = new Dict();
            itemDict.setPid(dict.getId());
            itemDict.setName(name);
            try {
                itemDict.setNum(Integer.valueOf(num));
            }catch (NumberFormatException e){
                throw new BussinessException(BizExceptionEnum.DICT_MUST_BE_NUMBER);
            }
            this.dictMapper.insert(itemDict);
        }
    }

    @Override
    public void editDict(Integer dictId, String dictName, String dicts) {
        //删除之前的字典
        this.delteDict(dictId);

        //重新添加新的字典
        this.addDict(dictName,dicts);
    }

    @Override
    public void delteDict(Integer dictId) {
        //删除这个字典的子词典
        Dict query = new Dict();
        query.setPid(dictId);
        dictMapper.delete(query);
        //删除这个词典
        dictMapper.deleteByPrimaryKey(dictId);
    }
}
