package dc.pay.admin.modular.system.warpper;

import dc.pay.admin.common.constant.factory.ConstantFactory;
import dc.pay.admin.common.warpper.BaseControllerWarpper;

import java.util.Map;

/**
 * 部门列表的包装
 */
public class NoticeWrapper extends BaseControllerWarpper {

    public NoticeWrapper(Object list) {
        super(list);
    }

    @Override
    public void warpTheMap(Map<String, Object> map) {
        Integer creater = (Integer) map.get("creater");
        map.put("createrName", ConstantFactory.me().getUserNameById(creater));
    }

}
