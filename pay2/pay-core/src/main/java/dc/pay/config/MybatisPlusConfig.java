package dc.pay.config;

import dc.pay.admin.core.datascope.DataScopeInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MybatisPlus配置
 */
@Configuration
@MapperScan(basePackages = {"dc.pay.mapper", "dc.pay.admin.modular.*.dao", "dc.pay.admin.common.persistence.dao"})
//@tk.mybatis.spring.annotation.MapperScan(basePackages = "dc.pay.mapper")
public class MybatisPlusConfig {


    /**
     * 数据范围mybatis插件
     */
    @Bean
    public DataScopeInterceptor dataScopeInterceptor() {
        return new DataScopeInterceptor();
    }
}
