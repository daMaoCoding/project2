package dc.pay.config;

import dc.pay.admin.config.properties.BeetlProperties;
import dc.pay.config.beetl.BeetlConfiguration;
import org.beetl.core.resource.ClasspathResourceLoader;
import org.beetl.ext.spring.BeetlSpringViewResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * web 配置类
 */
@Configuration
public class BeetlConfig {

    @Autowired
    BeetlProperties beetlProperties;

    /**
     * beetl的配置
     */
    @Bean(initMethod = "init")
    public BeetlConfiguration beetlConfiguration() {
        BeetlConfiguration beetlConfiguration = new BeetlConfiguration();
        beetlConfiguration.setResourceLoader(new ClasspathResourceLoader(BeetlConfig.class.getClassLoader(), beetlProperties.getPrefix()));
        beetlConfiguration.setConfigProperties(beetlProperties.getProperties());

        //获取项目根目录路径
        // root = evt.getServletContext().getRealPath("/");
        //根据项目根目录路径创建一个WebAppResourceLoader
        //WebAppResourceLoader webAppResourceLoader = new WebAppResourceLoader(root, "UTF-8");
        //重新设置ResourceLoader
        //ServletGroupTemplate.instance().getGroupTemplate().setResourceLoader(webAppResourceLoader);

        //WebAppResourceLoader resourceload = (WebAppResourceLoader) groupTemplate.getResourceLoader();
        //resourceload.setRoot(resourceload.getRoot()+"/templates/common/tags");
        //groupTemplate.setResourceLoader(resourceload);
        return beetlConfiguration;
    }

    /**
     * beetl的视图解析器
     */
    @Bean
    public BeetlSpringViewResolver beetlViewResolver() {
        BeetlSpringViewResolver beetlSpringViewResolver = new BeetlSpringViewResolver();
        beetlSpringViewResolver.setConfig(beetlConfiguration());
        beetlSpringViewResolver.setContentType("text/html;charset=UTF-8");
        beetlSpringViewResolver.setOrder(0);
        return beetlSpringViewResolver;
    }
}
