/**
 * Created by admin on 2017/6/24.
 */

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@SpringBootApplication
@EnableEurekaClient
@ComponentScan(basePackages = {"dc.pay"})
@EnableCircuitBreaker
public class PayRestMain {
    public static void main(String[] args) {
        SpringApplication.run(PayRestMain.class, args);
    }

}
