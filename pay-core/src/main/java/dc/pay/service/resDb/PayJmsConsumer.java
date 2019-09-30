package dc.pay.service.resDb;

import com.alibaba.fastjson.JSON;
import dc.pay.base.processor.PayException;
import dc.pay.business.ResponseDaifuResult;
import dc.pay.config.RunTimeInfo;
import dc.pay.constant.PayEumeration;
import dc.pay.entity.daifu.ResDaiFuList;
import dc.pay.entity.pay.ResPayList;
import dc.pay.entity.po.AutoQueryDaifu;
import dc.pay.service.daifu.ResponseDaiFuService;
import dc.pay.service.pay.ResponsePayService;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.jms.Session;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static dc.pay.config.activeMq.ActiveMQConfig.ORDER_QUEUE_AUTO_DAIFU;
import static dc.pay.config.activeMq.ActiveMQConfig.ORDER_QUEUE_DAIFU;
import static dc.pay.config.activeMq.ActiveMQConfig.ORDER_QUEUE_PAY;

@Slf4j
@Component
public class PayJmsConsumer {

    @Autowired
    private ResponsePayService responsePayService;

    @Autowired
    private ResponseDaiFuService responseDaiFuService;

    @Autowired
    RunTimeInfo runTimeInfo;

    @Autowired
    HandlerUtil handlerUtil;

    @Autowired
    RedisTemplate redisTemplate;

    private ExecutorService executor = new ThreadPoolExecutor(HandlerUtil.nThreads, HandlerUtil.nThreads, 0L, TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(HandlerUtil.MAX_QUEUQ_SIZE),new ThreadPoolExecutor.CallerRunsPolicy());

    @JmsListener(destination = ORDER_QUEUE_PAY)
    public void receiveMessage(@Payload ResPayList resPayList, @Headers MessageHeaders headers, Message message, Session session) {
//        log.debug("JmsListener 收到消息： <" + resPayList + ">");
//        log.debug("- - - - - - - - - - - - - - - - - - - - - - - -");
//        log.debug("######          Message Details           #####");
//        log.debug("- - - - - - - - - - - - - - - - - - - - - - - -");
//        log.debug("headers: " + headers);
//        log.debug("message: " + message);
//        log.debug("session: " + session);
//        log.debug("resPayList的Json串：{}",  resPayList.getChannel());
//        log.debug("- - - - - - - - - - - - - - - - - - - - - - - -");
        executor.execute(new Runnable() {
            @Override
            public void run() {
                // 耗时且复杂的消息处理逻辑
                responsePayService.saveAndResDbMsgNextTime(resPayList,false); //再次发送通知
            }
        });
  //       responsePayService.saveAndResDbMsgNextTime(resPayList); //再次发送通知

    }



    @JmsListener(destination = ORDER_QUEUE_DAIFU)
    public void receiveMessage(@Payload ResDaiFuList resDaiFuList, @Headers MessageHeaders headers, Message message, Session session) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                // 耗时且复杂的消息处理逻辑
                responseDaiFuService.saveAndResDbMsgNextTime(resDaiFuList,false); //再次发送通知
            }
        });
    }


    @JmsListener(destination = ORDER_QUEUE_AUTO_DAIFU)
    public void receiveMessage(@Payload AutoQueryDaifu autoQueryDaifu, @Headers MessageHeaders headers, Message message, Session session) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                boolean needQueryAgain = true;
                try {
                    if(null==autoQueryDaifu) return;
                    String queryDaifuRes = RestTemplateUtil.postJson(autoQueryDaifu.getServerUrl(), autoQueryDaifu.getParams());
                    if(StringUtils.isNotBlank(queryDaifuRes)){
                        ResponseDaifuResult responseDaifuResult = JSON.parseObject(queryDaifuRes, ResponseDaifuResult.class);
                        if(null!=responseDaifuResult && responseDaifuResult.getResponseDaifuCode().equalsIgnoreCase("SUCCESS")&& (responseDaifuResult.getResponseOrderState().equalsIgnoreCase(PayEumeration.DAIFU_RESULT.SUCCESS.getCodeValue())  || responseDaifuResult.getResponseOrderState().equalsIgnoreCase(PayEumeration.DAIFU_RESULT.ERROR.getCodeValue()) )){
                            needQueryAgain=false;
                            redisTemplate.opsForHash().delete(autoQueryDaifu.getKey(),autoQueryDaifu.getOrderId());
                        }
                    }
                } catch (PayException e) {
                    log.error("[代付][自动查询]失败，订单号：{}，错误：{}",autoQueryDaifu.getOrderId(),e.getMessage(),e);
                }
                if(needQueryAgain && autoQueryDaifu.getTimes()<=autoQueryDaifu.getTotalTimes()){
                    autoQueryDaifu.setMqInProcess(false);
                    handlerUtil.addQueryOrderJob(autoQueryDaifu.getOrderId(),autoQueryDaifu.getServerId(),autoQueryDaifu.getServerUrl(),autoQueryDaifu.getParams(),autoQueryDaifu.getTimes(),autoQueryDaifu.getTotalTimes());
                }
            }
        });
    }


}
