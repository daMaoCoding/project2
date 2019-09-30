package dc.pay.business.mafuzhifu;

import java.util.*;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.utils.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * @author cobby
 * Jan 26, 2019
 */
@RequestPayHandler("MAFUZHIFU")
public final class MaFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(MaFuZhiFuPayRequestHandler.class);

    private static final String uid              ="uid";       //商户uid 必填。您的商户唯一标识，注册后在设置里获得。
    private static final String money            ="money";     //价格    必填。单位：元。精确小数点后2位
    private static final String pay_way          ="pay_way";   //支付渠道 必填。1：支付宝；2：微信支付
    private static final String format           ="format";    //支付类型 必填。1：json返回 推荐；2：网页支付
    private static final String notify_url       ="notify_url";//通知回调网址 必填。用户支付成功后，我们服务器会主动发送一个post消息到这个网址。由您自定义。不要urlencode。例：http://www .aaa.com/qpay_notify
    private static final String return_url       ="return_url";//跳转网址    必填。用户支付成功后，我们会让用户浏览器自动跳转到这个网址。由您自定义。不要urlencode。例：http://www.aaa .com/qpay_return
    private static final String order_id         ="order_id";  //商户自定义订单号 必填。我们会据此判别是同一笔订单还是新订单。我们回调时，会带上这个参数。例：201710192541
    private static final String way_id           ="way_id";    //收款账号ID     必填。随机填0，指定收款账号就填您的后台收款账号ID
//    private static final String goodsname        ="goodsname"; //商户自定义商品名称 选填。我们会显示在您后台的订单列表中，方便后台对账。强烈建议填写。
//    private static final String remark           ="remark";    //备注   选填。我们会显示在您后台的订单列表中，方便后台对账。强烈建议填写。
    private static final String key              ="key";       //秘钥   必填。把使用到的所有参数，连Token一起，按参数名字母升序排序(ascii码正序)。把参数值拼接在一起（空参数不参与签名）。做md5-32位加密，取字符串小写。得到key。网址类型的参数值不要urlencode。

    //signature    数据签名    32    是    　
    private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(uid, channelWrapper.getAPI_MEMBERID());
	            put(money,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
	            put(pay_way,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
	            put(format,"1");
	            put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
	            put(return_url,channelWrapper.getAPI_WEB_URL());
	            put(order_id,channelWrapper.getAPI_ORDER_ID());
	            put(way_id,"0");
            }
        };
        log.debug("[码付支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key + "="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[码付支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

	    HashMap<String, String> result = Maps.newHashMap();
        try {

            if ( HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
//                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
            }else{
	            /*{
		            //提示给用户的文字信息，会根据不同场景，展示不同内容
		            "msg":"付款即时到账 未到账可联系我们",
			            "data":{
		            //二维码信息，如果没返回，说明存在错误，参考msg的信息
		            "qrcode":"HTTPS://QR.ALIPAY.COM/FKX08406GFWYYSF0YRNC10",
				            //支付渠道：1-支付宝；2-微信
				            "pay_way":"1",
				            //显示给用户的订单金额(一定要把这个价格显示在支付页上，而不是订单金额)
				            "realprice":0.05,
				            //订单过期时间
				            "order_expire_time":180
	            },
		            //code 0成功 500失败。
		            "code":0,
			            //判断支付成功后，要同步跳转的URL
			            "url":"https://www.mafupay.cn/"
	            }*/
	            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),"application/json");

	            if (StringUtils.isBlank(resultStr)) {
                    log.error("[码付支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (!resultStr.contains("{") || !resultStr.contains("}")) {
                   log.error("[码付支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                   throw new PayException(resultStr);
                }
                JSONObject resJson;
                try {
                    resJson = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[码付支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                //只取正确的值，其他情况抛出异常
                if (null != resJson && resJson.containsKey("code") && "0".equalsIgnoreCase(resJson.getString("code"))  &&
		                resJson.containsKey("data") && StringUtils.isNotBlank(resJson.getString("data"))) {
	                JSONObject data = JSONObject.parseObject(resJson.getString("data").toString());
	                String code_url = data.getString("qrcode");
                    result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                }else {
                    log.error("[码付支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            }
            
        } catch (Exception e) {
            log.error("[码付支付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[码付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult=  buildResult(resultMap, channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[码付支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}