package dc.pay.payrest.config;/**
 * Created by admin on 2017/6/24.
 */

import dc.pay.payrest.util.RestTemplateUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@Component
@Qualifier("SpringCloudConfigForWeb")
public class SpringCloudConfigForWeb {


    /**
     * 负载平衡RestTemplate
     * @return
     */
    @Bean
    @Qualifier("payForWebLoadBalanced")
    @LoadBalanced
    RestTemplate loadBalanced() {
        return RestTemplateUtil.getRestTemplate();
    }


    /**
     * 非负载平衡RestTemplate
     * @return
     */
    @Primary
    @Bean
    @Qualifier("payForWebRestTemplate")
    RestTemplate restTemplate() {
        return RestTemplateUtil.getRestTemplate();
    }

}
