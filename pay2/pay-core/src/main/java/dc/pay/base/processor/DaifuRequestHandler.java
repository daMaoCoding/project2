package dc.pay.base.processor;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import dc.pay.business.RequestDaifuResult;
import dc.pay.business.RequestPayResult;
import dc.pay.business.ResponseDaifuResult;
import dc.pay.business.caifubao.StringUtil;
import dc.pay.config.RunTimeInfo;
import dc.pay.constant.PayEumeration;
import dc.pay.entity.ReqDaifuInfo;
import dc.pay.entity.ReqDaifuQueryBalance;
import dc.pay.entity.daifu.ResDaiFuList;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ************************
 * @author tony 3556239829
 */
public abstract class DaifuRequestHandler {

    private static final Logger log =  LoggerFactory.getLogger(DaifuRequestHandler.class);
    public static final String       ACTION = "action";
    public static final String       SUCCESS = "SUCCESS";
    public static final String       ERROR = "ERROR";
    public static final String       ISO88591	  ="ISO-8859-1";
    public static final String       UTF8	  ="UTF-8";
    public static final String       GBK	  ="GBK";
    public static final String       RESPONSEKEY="第三方返回";
    private static final String       queryDaifuUrlTMP="%s/reqDaiFu/query/%s";
    public static final String       EMPTYRESPONSE = "第三方接口[返回空],请将时间/商户号/金额/通道名 发给第三方核对排错。";
    protected ChannelWrapper channelWrapper ;
    protected HandlerUtil handlerUtil ;
    protected RunTimeInfo runTimeInfo;
    protected  String queryDaifuUrl;
    public static  HttpHeaders defaultHeaders = new HttpHeaders();



    static {
        //headers.add("Content-Type","application/x-www-form-urlencoded");
        defaultHeaders.add("user-agent","PostmanRuntime/7.1.5");
    }


    /**
     *子类需要重写的请求代付方法
     * 如果想保存服务器返回内容，填入details中
     * 代付请求成功，返回true,否则返回false
     */
    protected abstract PayEumeration.DAIFU_RESULT requestDaifuAllInOne( Map<String,String> params,Map<String,String> details) throws PayException ;
    protected abstract PayEumeration.DAIFU_RESULT queryDaifuAllInOne(Map<String,String> params,Map<String, String> detail) throws PayException ;
    protected abstract long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException ;


    //请求代付
    public  RequestDaifuResult requestDaifu(ReqDaifuInfo reqDaifuInfo){
        RequestDaifuResult requestDaifuResult  = new RequestDaifuResult(true, reqDaifuInfo,null,PayEumeration.DAIFU_RESULT.UNKNOW);
        PayEumeration.DAIFU_RESULT daifuResult = null;
        try {
            daifuResult = requestDaifuAllInOne(requestDaifuResult.getParams(), requestDaifuResult.getDetails());
            if(daifuResult==null) throw new PayException("请求代付结果中，订单状态不正确。");
            requestDaifuResult.setRequestDaifuOrderState(daifuResult.getCodeValue());
            if(!ValidateUtil.requestDaifuResValdata(requestDaifuResult)){
                requestDaifuResult.setRequestDaifuErrorMsg("验证请求代付结果不正确");
            }
        } catch (PayException e) {
            String  errMsg = StringUtils.isNotBlank(e.getMessage())?e.getMessage():requestDaifuResult.getDetails().get(RESPONSEKEY);
            log.error("请求[代付]失败： {} ,第三方返回：{}",e.getMessage(),requestDaifuResult.getDetails().get(RESPONSEKEY),e);
            requestDaifuResult.setRequestDaifuErrorMsg(errMsg);
            return  requestDaifuResult;
        }

        //处理失败的，再没有可能入款的，所有订单状态不正常都写入返回
        if(daifuResult==PayEumeration.DAIFU_RESULT.ERROR) requestDaifuResult.setRequestDaifuCode(false);
        if(daifuResult!=PayEumeration.DAIFU_RESULT.PAYING) requestDaifuResult.setRequestDaifuErrorMsg(requestDaifuResult.getDetails().get(RESPONSEKEY));

        return requestDaifuResult;
    }



    //查询代付
    public  ResDaiFuList  queryDaifu( ReqDaifuInfo reqDaifuInfo){
        ResponseDaifuResult responseDaifuResult  = new ResponseDaifuResult(false, reqDaifuInfo,null);
        Map<String,String> params = Maps.newHashMap();
        Map<String,String> details = Maps.newHashMap();

        try{
            PayEumeration.DAIFU_RESULT daifuResult = queryDaifuAllInOne(params,details);
            if(null==daifuResult) throw new PayException("该第三方不支持代付查询,或该功能尚未未完成，请联系客服。");
            responseDaifuResult.setResponseDaifuCodeWithBoolean(true);
            responseDaifuResult.setResponseOrderState(daifuResult.getCodeValue());
            if(PayEumeration.DAIFU_RESULT.ERROR == daifuResult) responseDaifuResult.setResponseDaifuErrorMsg(details.get(RESPONSEKEY));
        }catch (Exception e){
            log.error("[daifuRequestHandler][查询代付]处理出错：参数：{},错误：{}",JSON.toJSONString(params),e.getMessage(),e);
            responseDaifuResult  = new ResponseDaifuResult(false,reqDaifuInfo, e.getMessage(),PayEumeration.DAIFU_RESULT.UNKNOW);
        }
        responseDaifuResult.setResponseDaifuSign(HandlerUtil.getResponseDaifuSign(responseDaifuResult, channelWrapper.getAPI_KEY())); // 签名
        return   new ResDaiFuList(responseDaifuResult, params, details.get(RESPONSEKEY), 0, reqDaifuInfo);
    }




    //查询代付-余额
    public  ReqDaifuQueryBalance getQueryDaifuBalance(ReqDaifuInfo reqDaifuInfo){
        Map<String,String> params = Maps.newHashMap();
        Map<String,String> details = Maps.newHashMap();
        long balance  = 0;
        try{
            balance = queryDaifuBalanceAllInOne(params,details);
            if(balance<0) throw new PayException("第三方代付账户余额负数？："+balance);
        }catch (Exception e){
            return new  ReqDaifuQueryBalance(false,e.getMessage(),reqDaifuInfo);
        }

        return new  ReqDaifuQueryBalance(true,null,reqDaifuInfo,balance);
    }


    protected  void addQueryDaifuOrderJob(String orderId) {
        try {
            if(null!=handlerUtil && StringUtils.isNotBlank(orderId)){
                HashMap<String, String> param = Maps.newHashMap();
                param.put(PayEumeration.queryDaifuFromKey,PayEumeration.queryDaifuFromAuto);
                String serverUrl = getQueryDaifuUrl(orderId);
                handlerUtil.addQueryOrderJob(orderId,serverUrl,param);
                return ;
            }
            throw new PayException("Handler空/orderId 空。");
        }catch (Exception e){
            log.error("[代付][计划查询]出错："+e.getMessage(),e);
        }
    }


    public ChannelWrapper getChannelWrapper() {
        return channelWrapper;
    }
    public void setChannelWrapper(ChannelWrapper channelWrapper) {
        this.channelWrapper = channelWrapper;
    }
    public HandlerUtil getHandlerUtil() {  return handlerUtil; }
    public void setHandlerUtil(HandlerUtil handlerUtil) { this.handlerUtil = handlerUtil; }
    public RunTimeInfo getRunTimeInfo() {
        return runTimeInfo;
    }
    public void setRunTimeInfo(RunTimeInfo runTimeInfo) {
        this.runTimeInfo = runTimeInfo;
    }
    public String getQueryDaifuUrl(String orderid) throws PayException {
        if(null!=runTimeInfo) return String.format(queryDaifuUrlTMP,runTimeInfo.getServerUrl(),orderid);
        throw new PayException("[代付]无法获取本地订单查询URL,订单号："+orderid);
    }
}
