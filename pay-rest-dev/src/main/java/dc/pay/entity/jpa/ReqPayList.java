package dc.pay.entity.jpa;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "req_pay_list")
public class ReqPayList implements Serializable {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private  String orderId;
    private String requestPayResult;



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getRequestPayResult() {
        return requestPayResult;
    }

    public void setRequestPayResult(String requestPayResult) {
        this.requestPayResult = requestPayResult;
    }
}
