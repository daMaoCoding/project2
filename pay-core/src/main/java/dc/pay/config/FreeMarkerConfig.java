package dc.pay.config;

import com.jagregory.shiro.freemarker.ShiroTags;
import freemarker.template.TemplateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * ************************
 * @author tony 3556239829
 */
@Configuration
public class FreeMarkerConfig   { //extends FreeMarkerConfigurer
    @Autowired
    private freemarker.template.Configuration configuration;

//    @Override
//    public void afterPropertiesSet() throws IOException, TemplateException {
//        super.afterPropertiesSet();
//        freemarker.template.Configuration cfg = this.getConfiguration();
//        cfg.setSharedVariable("shiro", new ShiroTags());//shiro标签
//        cfg.setNumberFormat("#");//防止页面输出数字,变成2,000
//        //可以添加很多自己的要传输到页面的[方法、对象、值]
//    }

    @PostConstruct
    public void setSharedVariable() {
        try {
            configuration.setSharedVariable("shiro", new ShiroTags());
            configuration.setNumberFormat("#");//防止页面输出数字,变成2,000, 可以添加很多自己的要传输到页面的[方法、对象、值]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
