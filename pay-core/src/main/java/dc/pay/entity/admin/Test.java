package dc.pay.entity.admin;

/**
 * <p>
 * <p>
 * </p>
 *
 */
public class Test extends Base {

    private static final long serialVersionUID = 1L;

    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }


    @Override
    public String toString() {
        return "Test{" +
                "id=" + id +
                ", value=" + value +
                "}";
    }
}
