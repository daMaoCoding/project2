package dc.pay.admin.common.constant.dictmap.factory;

import dc.pay.admin.common.constant.dictmap.base.AbstractDictMap;
import dc.pay.admin.common.constant.dictmap.base.SystemDict;
import dc.pay.admin.common.exception.BizExceptionEnum;
import dc.pay.admin.common.exception.BussinessException;

/**
 * 字典的创建工厂
 */
public class DictMapFactory {

    private static final String basePath = "dc.pay.admin.common.constant.dictmap.";

    /**
     * 通过类名创建具体的字典类
     */
    public static AbstractDictMap createDictMap(String className) {
        if("SystemDict".equals(className)){
            return new SystemDict();
        }else{
            try {
                Class<AbstractDictMap> clazz = (Class<AbstractDictMap>) Class.forName(basePath + className);
                return clazz.newInstance();
            } catch (Exception e) {
                throw new BussinessException(BizExceptionEnum.ERROR_CREATE_DICT);
            }
        }
    }
}
