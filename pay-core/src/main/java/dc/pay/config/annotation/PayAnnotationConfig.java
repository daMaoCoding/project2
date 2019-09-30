package dc.pay.config.annotation;/**
 * Created by admin on 2017/6/20.
 */

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * ************************
 * @author tony 3556239829
 */
//ClassPathScanningCandidateComponentProvider
@Configuration
@Import({ PayAnnotationConfig.HandlerScannerRegistrar.class })
public  class PayAnnotationConfig {
    private static final Logger logger = LoggerFactory.getLogger(PayAnnotationConfig.class);
    private  static Map<String,String> requestPayHandler =  Collections.synchronizedMap(Maps.newConcurrentMap());
    private  static Map<String,String> responsePayHandler = Collections.synchronizedMap(Maps.newConcurrentMap());
    private  static Map<String,String> requestDaifuHandler = Collections.synchronizedMap(Maps.newConcurrentMap());
    private  static Map<String,String> responseDaifuHandler = Collections.synchronizedMap(Maps.newConcurrentMap());

    public static class HandlerScannerRegistrar implements BeanFactoryAware, ImportBeanDefinitionRegistrar, ResourceLoaderAware {
        private BeanFactory beanFactory;
        private ResourceLoader resourceLoader;
        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            logger.debug("Searching for handler annotated with @RequestPayHandler/@ResponsePayHandler");
            PayHandlerScanner scanner = new PayHandlerScanner((BeanDefinitionRegistry) beanFactory);
            try {
                if (this.resourceLoader != null) {
                    scanner.setResourceLoader(this.resourceLoader);
                }
                scanner.scan("dc.pay.business");
            } catch (IllegalStateException ex) {
                logger.debug("Searching for handler annotated with @RequestPayHandler/@ResponsePayHandler:error...", ex);
            }
        }
        @Override
        public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
            this.beanFactory = beanFactory;
        }
        @Override
        public void setResourceLoader(ResourceLoader resourceLoader) {
            this.resourceLoader = resourceLoader;
        }
    }

    public final static class PayHandlerScanner extends ClassPathBeanDefinitionScanner {
        public PayHandlerScanner(BeanDefinitionRegistry registry) {
            super(registry);
        }
        @Override
        public void registerDefaultFilters() {
            this.addIncludeFilter(new AnnotationTypeFilter(RequestPayHandler.class));
            this.addIncludeFilter(new AnnotationTypeFilter(ResponsePayHandler.class));
            this.addIncludeFilter(new AnnotationTypeFilter(RequestDaifuHandler.class));
            this.addIncludeFilter(new AnnotationTypeFilter(ResponseDaifuHandler.class));
        }
        @Override
        public Set<BeanDefinitionHolder> doScan(String... basePackages) {
            Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);
            for (BeanDefinitionHolder holder : beanDefinitions) {
                BeanDefinition beanDefinition = holder.getBeanDefinition();
                AnnotationMetadata handlerMetadata = ((AnnotatedBeanDefinition) beanDefinition).getMetadata();
                String handlerClassName  = beanDefinition.getBeanClassName();
                if(handlerMetadata.hasAnnotation(RequestPayHandler.class.getName())){
                    MultiValueMap<String, Object> requestPayHandlers = handlerMetadata.getAllAnnotationAttributes(RequestPayHandler.class.getName());
                    String requestPayHandlerAnnotationValue = String.valueOf(requestPayHandlers.get("value").get(0));
                    if(!StringUtils.isBlank(requestPayHandlerAnnotationValue)){
                        requestPayHandler.put(requestPayHandlerAnnotationValue,handlerClassName);
                    }
                }
                else if(handlerMetadata.hasAnnotation(ResponsePayHandler.class.getName())){
                    MultiValueMap<String, Object> responsePayHandlers = handlerMetadata.getAllAnnotationAttributes(ResponsePayHandler.class.getName());
                    String responsePayHandlerAnnotationValue = String.valueOf(responsePayHandlers.get("value").get(0));
                    if(!StringUtils.isBlank(responsePayHandlerAnnotationValue)){
                        responsePayHandler.put(responsePayHandlerAnnotationValue,handlerClassName);
                    }
                }
                else if(handlerMetadata.hasAnnotation(RequestDaifuHandler.class.getName())){
                    MultiValueMap<String, Object> requestDaifuHandlers = handlerMetadata.getAllAnnotationAttributes(RequestDaifuHandler.class.getName());
                    String requestDaifuHandlerAnnotationValue = String.valueOf(requestDaifuHandlers.get("value").get(0));
                    if(!StringUtils.isBlank(requestDaifuHandlerAnnotationValue)){
                        requestDaifuHandler.put(requestDaifuHandlerAnnotationValue,handlerClassName);
                    }
                }
                else if(handlerMetadata.hasAnnotation(ResponseDaifuHandler.class.getName())){
                    MultiValueMap<String, Object> responseDaifuHandlers = handlerMetadata.getAllAnnotationAttributes(ResponseDaifuHandler.class.getName());
                    String responseDaifuHandlerAnnotationValue = String.valueOf(responseDaifuHandlers.get("value").get(0));
                    if(!StringUtils.isBlank(responseDaifuHandlerAnnotationValue)){
                        responseDaifuHandler.put(responseDaifuHandlerAnnotationValue,handlerClassName);
                    }
                }
            }
            return beanDefinitions;
        }
    }

    public static Map<String, String> getRequestPayHandler() {
        return requestPayHandler;
    }
    public static void setRequestPayHandler(Map<String, String> requestPayHandler) {
        PayAnnotationConfig.requestPayHandler = requestPayHandler;
    }
    public static Map<String, String> getResponsePayHandler() {
        return responsePayHandler;
    }
    public static void setResponsePayHandler(Map<String, String> responsePayHandler) {
        PayAnnotationConfig.responsePayHandler = responsePayHandler;
    }

    public static Map<String, String> getRequestDaifuHandler() {
        return requestDaifuHandler;
    }

    public static Map<String, String> getResponseDaifuHandler() {
        return responseDaifuHandler;
    }
}
