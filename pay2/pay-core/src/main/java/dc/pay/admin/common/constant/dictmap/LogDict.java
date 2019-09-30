package dc.pay.admin.common.constant.dictmap;

import dc.pay.admin.common.constant.dictmap.base.AbstractDictMap;
import dc.pay.admin.common.constant.dictmap.base.AbstractDictMap;

/**
 * 日志的字典
 */
public class LogDict extends AbstractDictMap {

    @Override
    public void init() {
        put("tips","备注");
    }

    @Override
    protected void initBeWrapped() {

    }
}
