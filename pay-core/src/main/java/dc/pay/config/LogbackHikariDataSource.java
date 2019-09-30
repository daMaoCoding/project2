package dc.pay.config;/**
 * Created by admin on 2017/6/10.
 */

import com.zaxxer.hikari.HikariDataSource;
import org.jasypt.encryption.StringEncryptor;

/**
 * ************************
 * @author tony 3556239829
 */
public class LogbackHikariDataSource extends HikariDataSource {

    private  final static StringEncryptor encryptor = JasyptConfig.getStringEncryptor();

    @Override
    public void setPassword(String password) {
        String decryptPassword = encryptor.decrypt(password.replaceAll("[密文]",""));
        super.setPassword(decryptPassword);
    }

    @Override
    public void setUsername(String username) {
        String decryptUsername = encryptor.decrypt(username.replaceAll("[密文]",""));
        super.setUsername(decryptUsername);
    }


}
