package dc.pay.business.bofutong;

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
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.*;

@RequestPayHandler("BOFUTONG")
public final class BoFuTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BoFuTongPayRequestHandler.class);
     private static final String  pay_type   = "pay_type";

    //2.0扫码支付
    private static final String   amount  ="amount";                   //	金额(单位元)	是	金额(单位元)
    private static final String   transCode  ="transCode";             //	交易码	是	001：移动支付
    private static final String   service  ="service";                 //	服务类型	是	0002:微信0010:支付宝0015:QQ钱包 010700：京东钱包010800：银联钱包
    private static final String   reqDate  ="reqDate";                 //	请求日期	是	格式yyyyMMdd
    private static final String   reqTime  ="reqTime";                 //	请求时间	是	格式hhMMss
    private static final String   openId  ="openId";                   //	微信openId	是	service为0000,0001时必选,其他产品标识可选
    private static final String   requestIp  ="requestIp";             //	来源IP	是	请求方公网IP
    private static final String   dateTime  ="dateTime";               //	日期时间	是	格式yyyyMMddHHMMss
    private static final String   payChannel  ="payChannel";           //	支付渠道	是	WXP:微信,ALP:支付宝，QQ:qq支付,Other:其它
    private static final String   goodsDesc  ="goodsDesc";             //	商品描述	否	商品的具体描述
    private static final String   mode  ="mode";                       //	模式	是	T1,T0
    private static final String   goodsName  ="goodsName";             //	商品名称	是	商品的名称
    private static final String   merchantId  ="merchantId";           //	商户号	是	商户号
    private static final String   orderId  ="orderId";                 //	商户订单号	是	商户上送订单号，长度最好在12-25位
    private static final String   terminalId  ="terminalId";            //	终端号	是	不同终端号不一样，显示在对账单中,1-8位
    private static final String   corpOrg  ="corpOrg";                  //	资金合作机构	是	WXP:微信,ALP:支付宝，QQ:qq支付,Other:其它
    private static final String   offlineNotifyUrl  ="offlineNotifyUrl"; //	异步通知URL	是	异步通知url
    private static final String   sign  ="sign";                         //	签名信息	是	32位MD5加密大写


    //6.6.0H5支付(推荐) & 8.8.1银联快捷支付
    private static final String    payType	  = "payType";                 //必填	支付类型微信30，支付宝22 ，QQ钱包31
  //  private static final String    merchantId	  = "merchantId";          //必填	商户编号
  //  private static final String    orderId	  = "orderId";                 //必填	订单号
  //  private static final String    amount	  = "amount";                 //必填	订单总金额(单位元)
  //  private static final String    mode	  = "mode";                       //必填	模式T0
    private static final String    notifyUrl	  = "notifyUrl";          //必填	支付后返回的商户处理页面
    private static final String    returnUrl	  = "returnUrl";          //必填	支付后返回的商户显示页面
 //   private static final String    requestIp	  = "requestIp";          //必填	请求ip地址”.”替换为”_”(如 127_127_12_12)
    private static final String    orderTime	  = "orderTime";          //必填	提交单据的时间yyyyMMddHHmmss
 //   private static final String    goodsName	  = "goodsName";          //必填	商品名称，长度最长50字符，不能为空
    private static final String    goodsNum	  = "goodsNum";               //必填	产品数量，长度最长20字符
 //   private static final String    goodsDesc	  = "goodsDesc";          //必填	支付说明，长度50字符
 //   private static final String    sign	  = "sign";                      //必填	32位MD5加密大写



    //4.0网关支付（网银）
    private static final String   pageReturnUrl	 = "pageReturnUrl";             //重定向URL	    是	重定向URL
    //private static final String   notifyUrl	 = "notifyUrl";                     //异步通知URL	是	异步通知URL
    private static final String   merchantNo	 = "merchantNo";               //商户号	        是	商户号
    private static final String   memberId	 = "memberId";                     //买家用户标识	是	商户生成的用户ID
    private static final String   merchantName	 = "merchantName";             //商户名称	    是	商户名称
    //private static final String   orderTime	 = "orderTime";                     //订单提交日期	是	格式YYYYMMDDHHmmss
    //private static final String   orderId	 = "orderId";                       //商户订单号	是	商户订单号，长度最好在12-25位
    //private static final String   mode	 = "mode";                              //模式	        是	T1,T0
    private static final String   totalAmount	 = "totalAmount";               //订单金额	    是	以元为单位
    private static final String   currency	 = "currency";                     //币种	        是	交易币种，默认CNY, 即人民币
    private static final String   bankAbbr	 = "bankAbbr";                     //银行简称	    是	如ICBC等
    private static final String   cardType	 = "cardType";                     //卡类型	        是	0:借记卡
    private static final String   bankId	 = "bankId";                      //银行id	        是	见附录
    //private static final String   payType	 = "payType";                     //支付类型	    是	1108:B2C网关 1106:手机版网关 1107:PC版网关
    private static final String   validNum	 = "validNum";                     //有效期数量	    是	有效期数量
    private static final String   validUnit	 = "validUnit";                     //有效期单位	是	00:分 01:小时 02:日 03:月
    //private static final String   goodsName	 = "goodsName";                     //商品名称	    是	商品名称
    private static final String   goodsId	 = "goodsId";                     //商品编号	    否	商品编号
    //private static final String   goodsDesc	 = "goodsDesc";                     //商品描述	    否	商品描述
    //private static final String   sign	 = "sign";                                //签名信息	是	32位MD5加密大写

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(HandlerUtil.isWY(channelWrapper)){ //4.0网关支付（网银）
            payParam.put(pageReturnUrl,channelWrapper.getAPI_WEB_URL());
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(merchantNo,channelWrapper.getAPI_MEMBERID());
            payParam.put(memberId,HandlerUtil.getRandomStr(10));
            payParam.put(merchantName,channelWrapper.getAPI_MEMBERID());
            payParam.put(orderTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(orderId,channelWrapper.getAPI_ORDER_ID());
            payParam.put(mode,HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM())?"T1":"T0");
            payParam.put(totalAmount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(currency,"CNY");
            payParam.put(bankAbbr,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
            payParam.put(cardType,"0");
            payParam.put(bankId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
            payParam.put(payType,  HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM())?"1106":"1107");
            payParam.put(validNum,"30");
            payParam.put(validUnit,"00");
            payParam.put(goodsName,"HelloWorld");
            payParam.put(goodsId,RandomUtils.nextInt(1,100)+"");
            payParam.put(goodsDesc,"HelloWorld");

        }else if(HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)){ //6.6.0H5支付(推荐) & 8.8.1银联快捷支付
            payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
            payParam.put(orderId,channelWrapper.getAPI_ORDER_ID());
            payParam.put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(mode,"T0");
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(returnUrl,channelWrapper.getAPI_WEB_URL());
            payParam.put(requestIp,channelWrapper.getAPI_Client_IP().replaceAll("\\.","_"));
            payParam.put(orderTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(goodsName,"HelloWorld");
            payParam.put(goodsNum, RandomUtils.nextInt(1,10)+"");
            payParam.put(goodsDesc,"HelloWorld");

        }else if(channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_SM")){ //2.0扫码支付
            payParam.put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(transCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
            payParam.put(service,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
            payParam.put(reqDate, DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
            payParam.put(reqTime,DateUtil.formatDateTimeStrByParam("hhMMss"));
//          payParam.put(openId,);
            payParam.put(requestIp,channelWrapper.getAPI_Client_IP());
            payParam.put(dateTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHMMss"));
            payParam.put(payChannel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[2]);
            payParam.put(goodsDesc,"HelloWorld");
            payParam.put(mode,"T0");
            payParam.put(goodsName,"HelloTony");
            payParam.put(merchantId,channelWrapper.getAPI_MEMBERID());
            payParam.put(orderId,channelWrapper.getAPI_ORDER_ID());
            payParam.put(terminalId,channelWrapper.getAPI_MEMBERID());
            payParam.put(corpOrg,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[3]);
            payParam.put(offlineNotifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }

        log.debug("[铂富通]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()))  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY()).toString();
        pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString());
        log.debug("[铂富通]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
                Map result = Maps.newHashMap();
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
                payResultList.add(result);
            }else{
                HashMap<String, String> result = Maps.newHashMap();
                String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                JSONObject resultStrJsonObject = JSON.parseObject(resultStr);

                if(channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_SM")) { //微信，支付宝，QQ,银联，京东扫码
                    String code = HandlerUtil.getFromJsonObject(resultStrJsonObject,"code");
                    String bankUrl = HandlerUtil.getFromJsonObject(resultStrJsonObject,"bankUrl");
                    if(StringUtils.isNotBlank(code) && code.equalsIgnoreCase("520000") && StringUtils.isNotBlank(bankUrl)){
                        result.put(QRCONTEXT, bankUrl);
                        payResultList.add(result);
                    }else{
                        throw new PayException(resultStr);
                    }
                } else if(1==2){

                }else {
                    log.error("[铂富通]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(JSON.toJSONString(resultStr));
                }
            }
        } catch (Exception e) {
            log.error("[铂富通]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[铂富通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[铂富通]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}