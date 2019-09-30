package dc.pay.business.tongfu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("TONGFU")
public final class TongFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TongFuPayRequestHandler.class);

    private static final String      MerId    ="MerId";	                //商户号          	 Y
    private static final String      MerOrderId    ="MerOrderId";	    //商户订单号     	Y
    private static final String      BankId    ="BankId";	            //银行通道编码       Y
    private static final String      OrderTime    ="OrderTime";	        //订单时间,格式为YYYYMMDDHHIISS        	Y
    private static final String      OrderAmount    ="OrderAmount";	    //订单金额,单位元        	Y
    private static final String      Version    ="Version";	            //版本号,固定值：V1.0.0
    private static final String      OrderType    ="OrderType";	        //订单类型,固定值：1
    private static final String      RenturnUrl    ="RenturnUrl";	    //取货地址,跳转到的商户系统连接地址
    private static final String      NotifyUrl    ="NotifyUrl";	        //异步通知地址
    private static final String      ComeIp    ="ComeIp";	            //来路IP
    private static final String      Product    ="Product";	            //商户商品名称
    private static final String      ProductDetail    ="ProductDetail";	//商品详细描述
    private static final String      Price    ="Price";	                //商品单价
    private static final String      Count    ="Count";	                //商品个数
    private static final String      Attach    ="Attach";	            //备注消息
    private static final String      Sign    ="Sign";	                //MD5签名


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(MerId , channelWrapper.getAPI_MEMBERID());
            payParam.put(MerOrderId ,channelWrapper.getAPI_ORDER_ID() );

         //   payParam.put(MerOrderId ,HandlerUtil.getRandomStrStartWithDate(4) ); //// TODO: 2018/3/19 注释


            payParam.put(BankId , channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(OrderTime , DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString2));
            payParam.put(OrderAmount , HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(Version , "V1.0.0");
            payParam.put(OrderType , "1");
            payParam.put(RenturnUrl , channelWrapper.getAPI_WEB_URL());
            payParam.put(NotifyUrl , channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(ComeIp , channelWrapper.getAPI_Client_IP());
            payParam.put(Product ,Product);
            payParam.put(ProductDetail , ProductDetail);
            payParam.put(Price ,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()) );
            payParam.put(Count ,"1" );
            payParam.put(Attach , Attach);
        }
        log.debug("[通付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }


    protected String buildPaySign(Map<String,String> params) throws PayException {
        String paramsStr = String.format("BankId=%s&MerId=%s&MerOrderId=%s&OrderAmount=%s&OrderTime=%s&key=%s",
                params.get(BankId),
                params.get(MerId),
                params.get(MerOrderId),
                params.get(OrderAmount),
                params.get(OrderTime),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[通付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||( HandlerUtil.isWapOrApp(channelWrapper)  && !channelWrapper.getAPI_CHANNEL_BANK_NAME().equalsIgnoreCase("GEFU_BANK_WAP_QQ_SM")  )  ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());//.replace("method='post'","method='get'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
            if(1==1 ) {  //扫码（银联扫码&微信&支付宝&QQ）
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "200".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("codeimg")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("codeimg"))){
                                result.put(QRCONTEXT, HandlerUtil.UrlDecode(jsonResultStr.getString("codeimg")));
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }

                } else if(1==2){//H5

                }else {
                    log.error("[通付]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(JSON.toJSONString(resultStr));
                }
            }
        } catch (Exception e) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
            payResultList.add(result);
            //log.error("[通付]3.发送支付请求，及获取支付请求结果出错：", e);
            //throw new PayException(e.getMessage(), e);

        }
        log.debug("[通付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap,channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[通付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}