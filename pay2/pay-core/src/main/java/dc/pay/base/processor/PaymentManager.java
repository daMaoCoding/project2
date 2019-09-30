package dc.pay.base.processor;

import com.alibaba.fastjson.JSON;
import dc.pay.business.RequestDaifuResult;
import dc.pay.business.RequestPayResult;
import dc.pay.business.ResponseDaifuResult;
import dc.pay.business.ResponsePayResult;
import dc.pay.config.RunTimeInfo;
import dc.pay.config.annotation.PayAnnotationConfig;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.dao.daifu.RequestDaiFuDao;
import dc.pay.dao.pay.RequestPayDao;
import dc.pay.entity.ReqDaifuInfo;
import dc.pay.entity.ReqDaifuQueryBalance;
import dc.pay.entity.ReqPayInfo;
import dc.pay.entity.daifu.ResDaiFuList;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.*;

/**
 * ************************
 * @author tony 3556239829
 */
@Component
public final class PaymentManager {
    private static final Logger log =  LoggerFactory.getLogger(PaymentManager.class);
    private static final Map<String, String> requestPayHandlers =  PayAnnotationConfig.getRequestPayHandler();
    private static final Map<String, String> responsePayHandlers = PayAnnotationConfig.getResponsePayHandler();
    private static final Map<String, String> requestDaifuHandlers =  PayAnnotationConfig.getRequestDaifuHandler();
    private static final Map<String, String> responseDaifuHandlers = PayAnnotationConfig.getResponseDaifuHandler();
    private PaymentManager(){}
    private static    String serverId  ;
    @Autowired
    HandlerUtil handlerUtil;
    @Autowired
    @Qualifier("reqPayDao")
    RequestPayDao reqPayDao;
    @Autowired
    @Qualifier("reqDaiFuDao")
    RequestDaiFuDao reqDaiFuDao;
    @Autowired
    RunTimeInfo runTimeInfo;
    @PostConstruct
    public void init() {
        serverId = runTimeInfo.getServerId();
    }


    /**
     * 请求支付
     */
    public RequestPayResult requestPay(ReqPayInfo reqPayInfo) throws PayException {
        if(!ValidateUtil.valdataReqPayInfo(reqPayInfo)){
            log.error("请求支付参数不正确，支付通道："+reqPayInfo.getAPI_CHANNEL_BANK_NAME()+"订单号："+reqPayInfo.getAPI_ORDER_ID()+",详情："+JSON.toJSONString(reqPayInfo));
            throw  new PayException(SERVER_MSG.REQUEST_PAY_INFO__ERROR);
        }
        long timeOut = handlerUtil.parseLong(reqPayInfo.getAPI_TIME_OUT());
        RequestPayResult requestPayResult = null;
        final  ExecutorService executor = Executors.newSingleThreadExecutor();
               FutureTask<RequestPayResult> future = null;
        future = new FutureTask<RequestPayResult>(
                new Callable<RequestPayResult>() {
                    public RequestPayResult call() throws Exception {
                        PayRequestHandler payRequestHandler = getPayRequestHandler(reqPayInfo);
                        RequestPayResult requestPayResult = payRequestHandler.requestPay();
                        return requestPayResult;
                }
        });
        executor.execute(future);
        try {
            requestPayResult = future.get(timeOut, TimeUnit.MILLISECONDS);
            requestPayResult =HandlerUtil.addHostNameInErrMSG(requestPayResult);
        }catch (Exception e) {
            if(e instanceof  TimeoutException) throw  new PayException(e.toString()+": 耗时 "+ new DecimalFormat("0.000").format(timeOut / 1000.00)+" 秒,处理超时");
            throw  new PayException(e.getMessage(),e);
        }finally {
            executor.shutdownNow();
        }
        return requestPayResult;
    }


    /**
     * 请求支付处理器
     */
    private  PayRequestHandler getPayRequestHandler(ReqPayInfo reqPayInfo) throws PayException {
        String api_channel_bank_name = reqPayInfo.getAPI_CHANNEL_BANK_NAME();
        api_channel_bank_name  = api_channel_bank_name.substring(0,api_channel_bank_name.indexOf("_BANK", 0));
        String payRequestHandlerclzz = requestPayHandlers.get(api_channel_bank_name);
        PayRequestHandler payRequestHandler  = createReqPayChannelHandler(reqPayInfo,payRequestHandlerclzz);
        return payRequestHandler;
    }


    private  <T extends PayRequestHandler> T createReqPayChannelHandler(ReqPayInfo reqPayInfo,String className) throws PayException {
        T payRequestHandler = null;
        try{
               ChannelWrapper channel = handlerUtil.createPayChannelWrapper(reqPayInfo);
               payRequestHandler =(T)Class.forName(className).newInstance();
               payRequestHandler.setChannelWrapper(channel);
               payRequestHandler.setHandlerUtil(handlerUtil);
               payRequestHandler.setRunTimeInfo(runTimeInfo);
        } catch (Exception e) {
            log.error("[版本低，请升级]创建具体支付请求处理类出错，info: {}, ServerID:{} ",JSON.toJSONString(reqPayInfo),serverId,e);
            throw new PayException("[版本低，请升级]创建具体支付请求处理类出错，Chnanel_NAME: ["+reqPayInfo.getAPI_CHANNEL_BANK_NAME()+"]: ServerID: "+serverId,e);
        }
        return payRequestHandler;
    }




    /**
     * 响应支付
     */
    public ResponsePayResult responsePay(String channelName , Map<String,String> responseMap){
        final  ExecutorService exec = Executors.newFixedThreadPool(1);
        Callable<ResponsePayResult> call = null;
        long timeOut =  PayEumeration.DEFAULT_TIME_OUT_RESPAY;
        ResponsePayResult responsePayResult =null;
        try{
            call = new Callable<ResponsePayResult>() {
                public ResponsePayResult call() throws Exception {
                    PayResponseHandler payResponseHandler = getPayResponseHandler(channelName,responseMap);
                    ResponsePayResult responsePayResult = payResponseHandler.responsePay(reqPayDao);
                    return responsePayResult;
                }
            };
            Future<ResponsePayResult> future = exec.submit(call);
            responsePayResult = future.get(timeOut, TimeUnit.MILLISECONDS);
        }catch (Exception e){
                log.error("响应支付出错：通道名{} ,参数{}",channelName,JSON.toJSONString(responseMap), e.toString(),e);
                responsePayResult = new ResponsePayResult(PayEumeration.RESPONSE_PAY_CODE.ERROR, e.getMessage());
        }finally {
            exec.shutdown();
        }
        if(StringUtils.isBlank(responsePayResult.getResponsePayChannel())){
            responsePayResult.setResponsePayChannel(channelName);
        }
        return responsePayResult;
    }


    /**
     * 响应支付处理器
     */
    private  PayResponseHandler getPayResponseHandler(String channelName,Map<String,String> responseMap) throws PayException {
        channelName = channelName.substring(0,channelName.indexOf("_BANK", 0));
        String payResponseHandlerclzz = responsePayHandlers.get(channelName);
        PayResponseHandler payResponseHandler  = createResPayChannelHandler(responseMap,payResponseHandlerclzz);
        return payResponseHandler;
    }


    private  <T extends PayResponseHandler> T createResPayChannelHandler(Map<String,String> responseMap,String className) throws PayException {
        T payResponseHandler = null;
        try {
            payResponseHandler =(T)Class.forName(className).newInstance();
            payResponseHandler.setAPI_RESPONSE_PARAMS(responseMap);
            payResponseHandler.setHandlerUtil(handlerUtil);
            payResponseHandler.setRunTimeInfo(runTimeInfo);
        } catch (Exception e) {
            log.error("[版本低，请升级]创建具体支付响应处理类出错，ClassName:{}, respMap: {}, ServerID:{} ",className,JSON.toJSONString(responseMap),serverId,e);
            throw new PayException("[版本低，请升级]创建具体支付响应处理类出错：ClassName: ["+className+"]: ServerID: "+serverId,e);
        }
        return payResponseHandler;
    }



    /**
     * 请求代付
     */
    public RequestDaifuResult requestDaifu(ReqDaifuInfo reqDaifuInfo)  {
        try {
            if(ValidateUtil.valdataReqDaifuInfo(reqDaifuInfo,serverId)){
                reqDaifuInfo.setAPI_KEY(RsaUtil.decryptAndCache(reqDaifuInfo.getAPI_KEY())); //解密并缓存KEY original
            }
        } catch (PayException e) {
            log.error("请求[代付]参数不正确，支付通道："+reqDaifuInfo.getAPI_CHANNEL_BANK_NAME()+"订单号："+reqDaifuInfo.getAPI_ORDER_ID()+",详情："+JSON.toJSONString(reqDaifuInfo));
            return new RequestDaifuResult(false, reqDaifuInfo,e.getMessage(),PayEumeration.DAIFU_RESULT.ERROR);
        }

        RequestDaifuResult requestDaifuResult = null;
        final  ExecutorService executor = Executors.newSingleThreadExecutor();
        FutureTask<RequestDaifuResult> future = null;
        future = new FutureTask<RequestDaifuResult>(
                new Callable<RequestDaifuResult>() {
                    public RequestDaifuResult call() throws Exception {
                        DaifuRequestHandler daifuRequestHandler = getDaifuRequestHandler(reqDaifuInfo);
                        RequestDaifuResult requestDaifuResult = daifuRequestHandler.requestDaifu(reqDaifuInfo);
                        return requestDaifuResult;
                    }
                });
        executor.execute(future);

        try {
            requestDaifuResult = future.get(PayEumeration.DEFAULT_TIME_OUT_REQDAIFU, TimeUnit.MILLISECONDS);
        }catch (Exception e) {
            if(e instanceof  TimeoutException){
                return new RequestDaifuResult(true, reqDaifuInfo,e.toString()+": 耗时 "+ new DecimalFormat("0.000").format(PayEumeration.DEFAULT_TIME_OUT_REQDAIFU / 1000.00)+" 秒,处理超时",PayEumeration.DAIFU_RESULT.UNKNOW);
            }
            else if(e instanceof ExecutionException && e.getMessage().contains("DaifuException")){
                return new RequestDaifuResult(false, reqDaifuInfo,e.getMessage(),PayEumeration.DAIFU_RESULT.ERROR);
            }
            return new RequestDaifuResult(true, reqDaifuInfo,e.getMessage(),PayEumeration.DAIFU_RESULT.UNKNOW);
        }finally {
            executor.shutdownNow();
        }
        return requestDaifuResult;
    }


    /**
     * 请求代付处理器
     */
    private  DaifuRequestHandler getDaifuRequestHandler(ReqDaifuInfo reqDaifuInfo) throws DaifuException {
        try {
            String api_channel_bank_name = reqDaifuInfo.getAPI_CHANNEL_BANK_NAME();
            api_channel_bank_name  = api_channel_bank_name.contains("_BANK")?api_channel_bank_name.substring(0,api_channel_bank_name.indexOf("_BANK", 0)):api_channel_bank_name;
            DaifuRequestHandler payRequestHandler  = createReqDaifuChannelHandler(reqDaifuInfo,  requestDaifuHandlers.get(api_channel_bank_name));
            return payRequestHandler;
        }catch (Exception e){
            log.error("[版本低，请升级]创建具体[代付]请求处理类出错，info: {}, ServerID:{} ",JSON.toJSONString(reqDaifuInfo),serverId,e);
            throw new DaifuException("[版本低，请升级]创建具体[代付]请求处理类出错，Chnanel_NAME: ["+reqDaifuInfo.getAPI_CHANNEL_BANK_NAME()+"]: ServerID: "+serverId);
        }

    }



    private  <T extends DaifuRequestHandler> T createReqDaifuChannelHandler(ReqDaifuInfo reqDaifuInfo,String className) throws DaifuException {
        try {
            T payRequestHandler = null;
            ChannelWrapper channel = handlerUtil.createDaifuChannelWrapper(reqDaifuInfo,runTimeInfo);
            ValidateUtil.valiRSA_KEY(reqDaifuInfo);
            payRequestHandler =(T)Class.forName(className).newInstance();
            payRequestHandler.setChannelWrapper(channel);
            payRequestHandler.setRunTimeInfo(runTimeInfo);
            payRequestHandler.setHandlerUtil(handlerUtil);
            return payRequestHandler;
        } catch (Exception e) {
            log.error("[版本低，请升级]创建具体[代付]请求处理类出错，info: {}, ServerID:{} ",JSON.toJSONString(reqDaifuInfo),serverId,e);
            throw new DaifuException("[版本低，请升级]创建具体[代付]请求处理类出错，Chnanel_NAME: ["+reqDaifuInfo.getAPI_CHANNEL_BANK_NAME()+"]: ServerID: "+serverId);
        }
    }


    /**
     * 响应代付
     */
    public ResponseDaifuResult responseDaifu(String channelName , Map<String,String> responseMap){
        final  ExecutorService exec = Executors.newFixedThreadPool(1);
        Callable<ResponseDaifuResult> call = null;
        long timeOut =  PayEumeration.DEFAULT_TIME_OUT_RESDAIFU;
        ResponseDaifuResult responseDaifuResult =null;
        try{
            if (null == responseMap || responseMap.isEmpty())  throw new PayException(SERVER_MSG.RESPONSE_DAIFU_RESULT_EMPTY_ERROR);
            call = new Callable<ResponseDaifuResult>() {
                public ResponseDaifuResult call() throws Exception {
                    DaifuResponseHandler daifuResponseHandler = getDaifuResponseHandler(channelName,responseMap);
                    ResponseDaifuResult responseDaifuResult = daifuResponseHandler.responseDaifu(reqDaiFuDao,runTimeInfo);
                    return responseDaifuResult;
                }
            };
            Future<ResponseDaifuResult> future = exec.submit(call);
            responseDaifuResult = future.get(timeOut, TimeUnit.MILLISECONDS);
        }catch (Exception e){
            log.error("响应[代付]出错：通道名{} ,参数{}",channelName,JSON.toJSONString(responseMap), e.toString(),e);
            responseDaifuResult = new ResponseDaifuResult(false, e.getMessage());
        }finally {
            exec.shutdown();
        }
        if(StringUtils.isBlank(responseDaifuResult.getResponseDaifuChannel())){
            responseDaifuResult.setResponseDaifuChannel(channelName);
        }
        return responseDaifuResult;
    }




    /**
     * 代付结果查询
     */
    public ResDaiFuList  queryDaifu(ReqDaifuInfo reqDaifuInfo) throws PayException {
        if(ValidateUtil.valdataReqDaifuInfo(reqDaifuInfo)){
            final  ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                FutureTask<ResDaiFuList> future = null;
                future = new FutureTask<ResDaiFuList>(
                        new Callable<ResDaiFuList>() {
                            public ResDaiFuList call() throws Exception {
                                DaifuRequestHandler daifuRequestHandler = getDaifuRequestHandler(reqDaifuInfo);
                                ResDaiFuList resDaiFuList = daifuRequestHandler.queryDaifu(reqDaifuInfo);
                                return resDaiFuList;
                            }
                        });
                executor.execute(future);
                return future.get(PayEumeration.DEFAULT_QUERY_DIAFU_TIME_OUT_REQDAIFU, TimeUnit.MILLISECONDS);
            }catch (Exception e) {
                if(e instanceof  TimeoutException) throw  new PayException(e.toString()+": 耗时 "+ new DecimalFormat("0.000").format(PayEumeration.DEFAULT_QUERY_DIAFU_TIME_OUT_REQDAIFU / 1000.00)+" 秒,查询超时");
                throw  new PayException(e.getMessage(),e);
            }finally {
                executor.shutdownNow();
            }
        }
        throw  new PayException(SERVER_MSG.REQUEST_DAIFU_INFO__ERROR);
    }



    /**
     * 代付-余额，查询
     */
    public ReqDaifuQueryBalance getQueryDaifuBalance(ReqDaifuInfo reqDaifuInfo) throws PayException {
        if(ValidateUtil.valdataReqDaifuInfoForQueryBalance(reqDaifuInfo)){
            final  ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                FutureTask<ReqDaifuQueryBalance> future = null;
                future = new FutureTask<ReqDaifuQueryBalance>(
                        new Callable<ReqDaifuQueryBalance>() {
                            public ReqDaifuQueryBalance call() throws Exception {
                                DaifuRequestHandler daifuRequestHandler = getDaifuRequestHandler(reqDaifuInfo);
                                ReqDaifuQueryBalance reqDaifuQueryBalance = daifuRequestHandler.getQueryDaifuBalance(reqDaifuInfo);
                                return reqDaifuQueryBalance;
                            }
                        });
                executor.execute(future);
                return future.get(PayEumeration.DEFAULT_QUERY_DIAFU_TIME_OUT_REQDAIFU, TimeUnit.MILLISECONDS);
            }catch (Exception e) {
                if(e instanceof  TimeoutException) throw  new PayException(e.toString()+": 耗时 "+ new DecimalFormat("0.000").format(PayEumeration.DEFAULT_QUERY_DIAFU_TIME_OUT_REQDAIFU / 1000.00)+" 秒,查询超时");
                throw  new PayException(e.getMessage(),e);
            }finally {
                executor.shutdownNow();
            }
        }
        throw  new PayException(SERVER_MSG.REQUEST_DAIFU_QUERY_BALANCE_INFO_ERROR);
    }


    /**
     * 响应代付处理器
     */
    private  DaifuResponseHandler getDaifuResponseHandler(String channelName,Map<String,String> responseMap) throws PayException {
        channelName = channelName.substring(0,channelName.indexOf("_BANK", 0));
        String daifuResponseHandlerclzz = responseDaifuHandlers.get(channelName);
        DaifuResponseHandler payResponseHandler  = createResDaifuChannelHandler(responseMap,daifuResponseHandlerclzz);
        return payResponseHandler;
    }


    private  <T extends DaifuResponseHandler> T createResDaifuChannelHandler(Map<String,String> responseMap,String className) throws PayException {
        T daifuResponseHandler = null;
        try {
            daifuResponseHandler =(T)Class.forName(className).newInstance();
            daifuResponseHandler.setAPI_RESPONSE_PARAMS(responseMap);
            daifuResponseHandler.setHandlerUtil(handlerUtil);
            daifuResponseHandler.setRunTimeInfo(runTimeInfo);
        } catch (Exception e) {
            log.error("[版本低，请升级]创建具体[代付]响应处理类出错，ClassName:{}, respMap: {}, ServerID:{} ",className,JSON.toJSONString(responseMap),serverId,e);
            throw new PayException("[版本低，请升级]创建具体支付响应处理类出错：ClassName: ["+className+"]: ServerID: "+serverId,e);
        }
        return daifuResponseHandler;
    }


}