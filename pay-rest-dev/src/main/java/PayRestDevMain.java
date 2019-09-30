/**
 * Created by admin on 2017/6/24.
 */

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@Controller
@SpringBootApplication  //(scanBasePackages={"dc.pay"})
@ComponentScan(basePackages = {"dc.pay"})
@EnableAutoConfiguration
@EntityScan("dc.pay.entity.jpa")
@EnableJpaRepositories("dc.pay.services.jpaRepository")
public class PayRestDevMain {
    public static void main(String[] args) {
        new SpringApplicationBuilder(PayRestDevMain.class).web(true).properties("server.port:61036").run(args);
        //SpringApplication.run(PayRestMain.class, args);
        System.out.println("=======================================================================");
        System.out.println("===============================-Start Finish-==========================");
        System.out.println("=======================================================================");
    }


    @RequestMapping("")
    public String index(Map<String,Object> map, HttpServletRequest request, HttpServletResponse response){
        return "index";
    }

    @RequestMapping("addBlackIp")
    public String addBlackIp(HttpServletRequest request, HttpServletResponse response){
        dc.pay.payrest.PayRestDevConFig.getInstance().addBlackIp(request.getParameter("blackIp"));
        return "index";
    }

    @RequestMapping("cleanBlackIp")
    public String cleanBlackIp(HttpServletRequest request, HttpServletResponse response){
        dc.pay.payrest.PayRestDevConFig.getInstance().cleanBlackIp();
        return "index";
    }





}
