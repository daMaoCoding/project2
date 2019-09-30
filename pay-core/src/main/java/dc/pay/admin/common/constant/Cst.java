package dc.pay.admin.common.constant;

/**
 * 一些服务的快捷获取
 */
public class Cst {

    private Cst() {
    }

    private static Cst cst = new Cst();

    public static Cst me() {
        return cst;
    }

}
