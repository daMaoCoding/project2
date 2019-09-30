package dc.pay.config;

import com.alibaba.fastjson.JSON;
import org.springframework.boot.ApplicationPid;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.logging.LoggingApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.boot.logging.LogFile;

class LoggingSystemProperties {

	private final Environment environment;
	LoggingSystemProperties(Environment environment) {
		this.environment = environment;
	}
	public void apply() {
		apply(null);
	}
	public void apply(LogFile logFile) {
		RelaxedPropertyResolver propertyResolver = RelaxedPropertyResolver.ignoringUnresolvableNestedPlaceholders(this.environment, "logging.");
		setSystemProperty(propertyResolver, "LOG_EXCEPTION_CONVERSION_WORD","exception-conversion-word");
		setSystemProperty(propertyResolver, "CONSOLE_LOG_PATTERN", "pattern.console");
		setSystemProperty(propertyResolver, "FILE_LOG_PATTERN", "pattern.file");
		setSystemProperty(propertyResolver, "LOG_LEVEL_PATTERN", "pattern.level");
		setSystemProperty(propertyResolver, "LOG_JDBC_URL", "logging.jdbcUrl"); //jdbc-jdbcUrl
		setSystemProperty("PID", new ApplicationPid().toString());
		if (logFile != null) {
			logFile.applyToSystemProperties();
		}
	}

	private void setSystemProperty(RelaxedPropertyResolver propertyResolver,
			String systemPropertyName, String propertyName) {
		setSystemProperty(systemPropertyName, propertyResolver.getProperty(propertyName));
	}

	private void setSystemProperty(String name, String value) {
		//System.out.println(name+"$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$"+value);
		if (System.getProperty(name) == null && value != null) {
			System.setProperty(name, value);

		}
		//System.exit(0);
	}

}
