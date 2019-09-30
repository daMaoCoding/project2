package dc.pay.config.annotation;/**
 * Created by admin on 2017/6/20.
 */

import java.lang.annotation.*;

/**
 * ************************
 *  @author tony 3556239829
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponsePayHandler {
    String value();  // default ""
}
