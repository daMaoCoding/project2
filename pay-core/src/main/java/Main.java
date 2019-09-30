import dc.pay.config.JasyptConfig;
import dc.pay.utils.DateUtil;
import dc.pay.utils.RsaUtil;
import dc.pay.config.RunTimeInfo;
import dc.pay.utils.excel.channelConfig.ExcelHelper;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;


@Controller
@EnableAsync
@EnableWebMvc
@EnableCaching
@EnableScheduling
@EnableEurekaClient
@SpringBootApplication
@EnableTransactionManagement
@ComponentScan(basePackages = {"dc.pay"})
//@EnableDiscoveryClient   //private DiscoveryClient discoveryClient;
//@Slf4j
public class Main  {

    @Autowired
    RunTimeInfo runTimeInfo;

    private static final Logger log =  LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) throws Exception{
        if(null!=args && args.length>0 && "-enc".equalsIgnoreCase(args[0])){
            setupEncInit(args);
            return;
        }

         if(null!=args && args.length>0 && "-dec".equalsIgnoreCase(args[0])){
             setupDecInit(args);
            return;
        }

        if(null!=args && args.length>0 && "-excel".equalsIgnoreCase(args[0])){
            ExcelHelper.generateExcelChannelConfParam(ExcelHelper.getChannelConfParamAll());
            return;
        }

         // new SpringApplicationBuilder() .environment(new EncryptableEnvironment(new StandardServletEnvironment())).sources(Main.class).run(args);
         //ConfigurableApplicationContext run = SpringApplication.run(Main.class, args);
         SpringApplication.run(Main.class, args);
    }



    /**
     * 生成初始化加密参数
     * @param args
     */
    public static void  setupEncInit(String[] args){

        if(args.length==1){
            System.out.println("-------------------------生成RSA-1024密钥对---开始------------------------------------");
            RsaUtil.generateRSA();
            System.out.println("-------------------------生成RSA-1024密钥对---------------------------------------");
        }

        String dbEncrypt = null;
        String keyEncrypt = null;
        for (int i = 1; i < args.length; i++) {
            try {
                 dbEncrypt  =  JasyptConfig.encrypt(args[i]);
                 keyEncrypt = RsaUtil.encrypt(args[i]);
                System.out.println("----------------------------------------------------------------");
                System.out.println("原－－文： 　" + args[i]);
                System.out.println("数据库密：　 " + dbEncrypt);
                System.out.println("商户秘钥： 　" + keyEncrypt);
                System.out.println("----------------------------------------------------------------");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 生成初始化解密参数
     * @param args
     */
    public static void  setupDecInit(String[] args){
        if(args.length==1){
            System.out.println("----------------------------------------------------------------");
            RsaUtil.generateRSA();
            System.out.println("----------------------------------------------------------------");
        }

        String keyDecrypt = null;
        for (int i = 1; i < args.length; i++) {
            try {
                keyDecrypt = RsaUtil.decrypt(args[i]);
                System.out.println("----------------------------------------------------------------");
                System.out.println("秘－－文： 　" + args[i]);
                System.out.println("明－－文： 　" + keyDecrypt);
                System.out.println("----------------------------------------------------------------");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}