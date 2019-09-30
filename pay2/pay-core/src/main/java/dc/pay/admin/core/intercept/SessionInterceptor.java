package dc.pay.admin.core.intercept;

import dc.pay.admin.common.controller.BaseController;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import dc.pay.admin.core.util.HttpSessionHolder;
import org.springframework.stereotype.Component;

/**
 * 静态调用session的拦截器
 */
@Aspect
@Component
public class SessionInterceptor extends BaseController {

//  @Pointcut("execution(* dc.pay.*..controller.*.*(..))")
// @Pointcut("execution(* dc.pay..controller.*.*(..))")
  @Pointcut("execution(* dc.pay.admin.*..controller.*.*(..))")
    public void cutService() {
    }

    @Around("cutService()")
    public Object sessionKit(ProceedingJoinPoint point) throws Throwable {

        HttpSessionHolder.put(super.getHttpServletRequest().getSession());
        try {
            return point.proceed();
        } finally {
            HttpSessionHolder.remove();
        }
    }
}
