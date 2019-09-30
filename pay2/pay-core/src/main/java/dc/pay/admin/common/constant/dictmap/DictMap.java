package dc.pay.admin.common.constant.dictmap;

import dc.pay.admin.common.constant.dictmap.base.AbstractDictMap;
import dc.pay.admin.common.constant.dictmap.base.AbstractDictMap;

/**
 * 字典map
 */
public class DictMap extends AbstractDictMap {

    @Override
    public void init() {
        put("dictId","字典名称");
        put("dictName","字典名称");
        put("dictValues","字典内容");
    }

    @Override
    protected void initBeWrapped() {

    }
}
