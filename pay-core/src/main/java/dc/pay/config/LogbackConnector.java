package dc.pay.config;

import ch.qos.logback.core.db.DriverManagerConnectionSource;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class LogbackConnector extends DriverManagerConnectionSource {
    private static final Logger log =  LoggerFactory.getLogger(LogbackConnector.class);
    private static Properties props;
    static {
        try(final InputStream inputStream =LogbackConnector.class.getClassLoader().getResourceAsStream("logback_db_config.properties")){
            StringEncryptor encryptor = JasyptConfig.getStringEncryptor();
            props = new EncryptableProperties(encryptor);
            props.load(inputStream);
        }catch (IOException e) {
            log.error("logback读取数据库配置出错："+e.getMessage(),e);
        }

    }

    @Override
    public Connection getConnection() {
        StringEncryptor encryptor = JasyptConfig.getStringEncryptor();
       try {
            return DriverManager.getConnection(String.format("jdbc:mysql://%s/%s?characterEncoding=utf8&useSSL=false&user=%s&password=%s",
                    props.getProperty("logback.serverIp"),
                    props.getProperty("logback.databaseName"),
                    encryptor.decrypt(props.getProperty("logback.databaseUser").replaceAll("[密文]","")),
                    encryptor.decrypt(props.getProperty("logback.databasePassword").replaceAll("[密文]",""))));

           } catch (SQLException e) {
               e.printStackTrace();
       }
        return null;
    }




}
