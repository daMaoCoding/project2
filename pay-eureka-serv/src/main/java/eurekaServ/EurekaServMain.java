package eurekaServ;/**
 * Created by admin on 2017/6/24.
 */

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@EnableEurekaServer
@SpringBootApplication
public class EurekaServMain {
    public static void main(String[] args) {
        //new SpringApplicationBuilder(EurekaServMain.class).web(true).run(args);
        SpringApplication.run(EurekaServMain.class, args);
    }
}
