package dc.pay.config;/**
 * Created by admin on 2017/6/10.
 */

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

/**
 * ************************
 * @author tony 3556239829
 */
public  final  class JasyptConfig {
    private static StringEncryptor stringEncryptor;
    static {
        stringEncryptor = initStringEncryptor();
    }
    public static StringEncryptor initStringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword("3556239829");
        config.setAlgorithm("PBEWithMD5AndDES");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }
    public static StringEncryptor getStringEncryptor() {
        return stringEncryptor;
    }
    public static String encrypt(String str){
       return  stringEncryptor.encrypt(str);
    }
  public static String decrypt(String str){
       return  stringEncryptor.decrypt(str);
    }




}
