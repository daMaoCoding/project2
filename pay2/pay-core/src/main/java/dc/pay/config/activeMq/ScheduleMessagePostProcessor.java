package dc.pay.config.activeMq;

import javax.jms.JMSException;
import javax.jms.Message;

import lombok.Data;
import org.apache.activemq.ScheduledMessage;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jms.core.MessagePostProcessor;
 
/**
 * MQ延时投递处理器（注：ActiveMQ的配置文件中，要配置schedulerSupport="true"，否则不起作用）
 */
@Data
public class ScheduleMessagePostProcessor implements MessagePostProcessor {
    private long delay = 0L;
    private String corn = null;
 
    public ScheduleMessagePostProcessor(long delay) {
        this.delay = delay<=0?RandomUtils.nextLong(500, 2000) :delay;
    }
 
    public ScheduleMessagePostProcessor(String cron) {
        this.corn = cron;
    }
 
    public Message postProcessMessage(Message message) throws JMSException {
        if (delay > 0) {//5*1000L
            message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, delay);
            message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_PERIOD, 1*1000);
            message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_REPEAT, 1);
        }
        if (!StringUtils.isEmpty(corn)) { //"50 22 * * *"
            message.setStringProperty(ScheduledMessage.AMQ_SCHEDULED_CRON, corn);
        }
        return message;
    }
 
}