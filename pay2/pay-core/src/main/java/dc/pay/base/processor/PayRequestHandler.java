package dc.pay.base.processor;


import dc.pay.business.RequestPayResult;
import dc.pay.business.caifubao.StringUtil;
import dc.pay.config.RunTimeInfo;
import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

/**
 * ************************
 * @author tony 3556239829
 */
public abstract class PayRequestHandler{
    private static final Logger log =  LoggerFactory.getLogger(PayRequestHandler.class);
    public static  final String  QRCONTEXT = "QrContext";
    public static  final String HTMLCONTEXT = "HtmlContext";
    public static  final String JUMPURL = "JUMPURL";
    public static  final String PARSEHTML = "parseHtml";
    public static final String METHOD = "method";
    public static final String ACTION = "action";
    public static final String Detial = "Detial";
    public static final String SUCCESS = "SUCCESS";
    public static final String ERROR = "ERROR";
    public static final String       ISO88591	  ="ISO-8859-1";
    public static final String       UTF8	  ="UTF-8";
    public static final String       GBK	  ="GBK";
    public static final String       EMPTYRESPONSE = "第三方接口[返回空],请将时间/商户号/金额/通道名 发给第三方核对排错。";
    public static final String       WY_JMP = "/wy/jmp/";
    public static final String       PAYMENT = "payment";
    protected ChannelWrapper channelWrapper ;
    protected HandlerUtil handlerUtil ;
    protected RunTimeInfo runTimeInfo;
    public static  HttpHeaders defaultHeaders = new HttpHeaders();

    static {
        //headers.add("Content-Type","application/x-www-form-urlencoded");
        defaultHeaders.add("user-agent","PostmanRuntime/7.1.5");
    }


    public   RequestPayResult requestPay(){
        RequestPayResult requestPayResult  = null;
        Map<String, String> payParam = null;
        String  pay_md5sign  = null;
        Long  channelUsedTimeBegin = getNowMilliSecond();
        List<Map<String,String>> requestPayResultDetail =null;
        try{
                payParam = buildPayParam();
                pay_md5sign = buildPaySign(payParam);
                channelUsedTimeBegin = getNowMilliSecond();
                requestPayResultDetail = sendRequestGetResult(payParam, pay_md5sign);
                requestPayResult = buildResult(requestPayResultDetail);
                requestPayResult.setDetail(requestPayResultDetail);
                requestPayResult.setRequestPayChannelTime((getNowMilliSecond())-channelUsedTimeBegin);
                requestPayResult.setRequestPayOtherParam(channelWrapper.getAPI_OTHER_PARAM());
            if(!StringUtil.isBlank(requestPayResult.getRequestPayHtmlContent())){//网银跳转
                String jumpUrlPrefix = StringUtils.isBlank(channelWrapper.getAPI_JUMP_URL_PREFIX())?HandlerUtil.getDomain(channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL()):channelWrapper.getAPI_JUMP_URL_PREFIX();
                String url = HandlerUtil.subUrl(jumpUrlPrefix).concat(WY_JMP).concat(channelWrapper.getAPI_ORDER_ID().trim());
                requestPayResult.setRequestPayJumpToUrl(url);
              //  if(HandlerUtil.isWEBWAPAPP_SM(channelWrapper)){ requestPayResult.setRequestPayQRcodeContent(url);  }else{  requestPayResult.setRequestPayJumpToUrl(url); }  //有扫码的也需要跳转如亿闪付
            }

            if(null!=requestPayResult && StringUtils.isNotBlank(requestPayResult.getRequestPayJumpToUrl()) ){
                 requestPayResult.setRequestPayJumpToUrl( HandlerUtil.fixAppNoSupportHTTPS(  requestPayResult.getRequestPayJumpToUrl()) );
            }


       }catch (Exception e){
            log.error("[payRequestHandler]处理请求支付出错：{}",e.getMessage(),e);
            requestPayResult = new RequestPayResult(e,channelWrapper.getAPI_ORDER_ID(),channelWrapper.getAPI_AMOUNT(),channelWrapper.getAPI_CHANNEL_BANK_NAME());
            requestPayResult.setRequestPayChannelTime((getNowMilliSecond())-channelUsedTimeBegin);
           if(null!=payParam && !payParam.isEmpty() ){
               requestPayResult.adddetail(payParam); //安全起见不记录
           }
       }
        return requestPayResult;
    }
    private long getNowMilliSecond() {
        return System.nanoTime()/1000000L;
    }
    public ChannelWrapper getChannelWrapper() {
        return channelWrapper;
    }
    public void setChannelWrapper(ChannelWrapper channelWrapper) {
        this.channelWrapper = channelWrapper;
    }
    protected abstract Map<String, String> buildPayParam() throws PayException, UnsupportedEncodingException;
    protected abstract String  buildPaySign(Map<String, String> payParam) throws PayException;
    protected abstract List<Map<String,String>>  sendRequestGetResult(Map<String, String> payParam,String pay_md5sign) throws PayException;
    protected  abstract RequestPayResult buildResult(List<Map<String,String>> result) throws PayException;
    public HandlerUtil getHandlerUtil() {  return handlerUtil; }
    public void setHandlerUtil(HandlerUtil handlerUtil) { this.handlerUtil = handlerUtil; }

    public RunTimeInfo getRunTimeInfo() {
        return runTimeInfo;
    }

    public void setRunTimeInfo(RunTimeInfo runTimeInfo) {
        this.runTimeInfo = runTimeInfo;
    }

    protected final    RequestPayResult buildResult(Map<String, String> resultMap, ChannelWrapper channelWrapper, RequestPayResult requestPayResult){
        if(null!=resultMap && resultMap.containsKey(QRCONTEXT)){
            requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
            requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
            requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(),"yyyy-MM-dd HH:mm:ss"));
            requestPayResult.setRequestPayQRcodeURL(null);
            requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
            requestPayResult.setRequestPayQRcodeContent(resultMap.get(QRCONTEXT));
        }
        if(null!=resultMap && resultMap.containsKey(HTMLCONTEXT)){
            requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
            requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
            requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(),"yyyy-MM-dd HH:mm:ss"));
            requestPayResult.setRequestPayQRcodeURL(null);
            requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
            requestPayResult.setRequestPayHtmlContent(resultMap.get(HTMLCONTEXT));
        }
        if(null!=resultMap && resultMap.containsKey(JUMPURL)){
            requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
            requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
            requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(),"yyyy-MM-dd HH:mm:ss"));
            requestPayResult.setRequestPayQRcodeURL(null);
            requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
            requestPayResult.setRequestPayJumpToUrl(resultMap.get(JUMPURL));
        }
        return requestPayResult;
    }



}
