package dc.pay.base.processor;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public class DaifuException extends Exception {
    private static final long serialVersionUID = 1L;
    protected DaifuException() { }
    protected DaifuException(String message) {
        super(message);
    }
    public DaifuException(String message, Throwable cause) {
        super(message, cause);
    }
    public DaifuException(Throwable cause) {
        super(cause);
    }
}
