package dc.pay.service.resDb;

import dc.pay.base.ResListI;
import dc.pay.config.activeMq.ScheduleMessagePostProcessor;
import dc.pay.entity.daifu.ResDaiFuList;
import dc.pay.entity.pay.ResPayList;
import dc.pay.entity.po.AutoQueryDaifu;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ScheduledMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;
import javax.jms.*;

import static dc.pay.config.activeMq.ActiveMQConfig.ORDER_QUEUE_AUTO_DAIFU;
import static dc.pay.config.activeMq.ActiveMQConfig.ORDER_QUEUE_DAIFU;
import static dc.pay.config.activeMq.ActiveMQConfig.ORDER_QUEUE_PAY;

@Slf4j
@Service
public class PayJmsSender {

    @Autowired
    private JmsTemplate jmsTemplate;


    public void sendLater(ResListI resList, long delay) {
        if(resList instanceof ResPayList )     jmsTemplate.convertAndSend(ORDER_QUEUE_PAY, resList,new ScheduleMessagePostProcessor(delay));
        if(resList instanceof ResDaiFuList )   jmsTemplate.convertAndSend(ORDER_QUEUE_DAIFU, resList,new ScheduleMessagePostProcessor(delay));
    }


    public void sendLater(AutoQueryDaifu queryDaifu,long delay){
        jmsTemplate.convertAndSend(ORDER_QUEUE_AUTO_DAIFU, queryDaifu,new ScheduleMessagePostProcessor(delay));
    }


    public void send(ResListI resList) {
        if(resList instanceof  ResPayList)  jmsTemplate.convertAndSend(ORDER_QUEUE_PAY, resList);
        if(resList instanceof  ResDaiFuList)  jmsTemplate.convertAndSend(ORDER_QUEUE_DAIFU, resList);


    }


    public void send(Queue queue, String msg, long delay) {
        log.info("发送MQ延时消息:msg={},delay={}", msg, delay);
        jmsTemplate.send(queue, new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                TextMessage tm = session.createTextMessage(msg);
                tm.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, delay);
                tm.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_PERIOD, 1*1000);
                tm.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_REPEAT, 1);
                return tm;
            }
        });
    }




}
