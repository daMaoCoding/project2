package dc.pay.admin.core.intercept;

import dc.pay.admin.common.controller.BaseController;
import dc.pay.admin.core.shiro.ShiroKit;
import dc.pay.admin.core.support.HttpKit;
import org.apache.shiro.session.InvalidSessionException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 验证session超时的拦截器
 */
@Aspect
@Component
@ConditionalOnProperty(prefix = "payAdmin", name = "session-open", havingValue = "true")
public class SessionTimeoutInterceptor extends BaseController {

    //@Pointcut("execution(* dc.pay..controller.*.*(..))")
    @Pointcut("execution(* dc.pay.admin.*..controller.*.*(..))")
    public void cutService() {
    }

    @Around("cutService()")
    public Object sessionTimeoutValidate(ProceedingJoinPoint point) throws Throwable {

        String servletPath = HttpKit.getRequest().getServletPath();

        if (servletPath.equals("/") || servletPath.equals("/kaptcha") || servletPath.equals("/login") || servletPath.equals("/global/sessionError") || servletPath.contains("/tongji/cgl")) {
            return point.proceed();
        }else{
            if(ShiroKit.getSession().getAttribute("sessionFlag") == null){
                ShiroKit.getSubject().logout();
                throw new InvalidSessionException();
            }else{
                return point.proceed();
            }
        }
    }
}
