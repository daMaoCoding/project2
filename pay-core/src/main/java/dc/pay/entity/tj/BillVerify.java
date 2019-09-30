package dc.pay.entity.tj;

/**
 * ************************
 * 入款统计核对
 * @author tony 3556239829
 */
public class BillVerify {

    String orderCount;
    String orderAmount;
    String oid;

    public String getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(String orderCount) {
        this.orderCount = orderCount;
    }

    public String getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(String orderAmount) {
        this.orderAmount = orderAmount;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }
}
