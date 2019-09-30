package dc.pay.config;/**
 * Created by admin on 2017/7/4.
 */

import dc.pay.utils.RestTemplateUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;


/**
 * ************************
 *
 * @author tony 3556239829
 */
@Configuration
public class RestTemplateConfiguration {

    @Bean
    @Primary
    @Qualifier("polledRestTemplate")
    RestTemplate polledRestTemplate() {
        return RestTemplateUtil.getRestTemplate();
    }



    @Bean
    @LoadBalanced
    @Qualifier("loadBalancedRestTemplate")
    RestTemplate polledLoadBalancedRestTemplate() {
        return new RestTemplate();
    }



    @Bean
    public StringHttpMessageConverter stringHttpMessageConverter() {
        return RestTemplateUtil.stringHttpMessageConverter();
    }



    @Bean
    @ConditionalOnMissingBean({RestOperations.class, RestTemplate.class})
    public RestOperations restOperations() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        //requestFactory.setReadTimeout(8000);
        //requestFactory.setConnectTimeout(8000);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        // 使用 utf-8 编码集的 conver 替换默认的 conver（默认的 string conver 的编码集为 "ISO-8859-1"）
         RestTemplateUtil.setMessageConverter(restTemplate);
        return restTemplate;
    }

}
