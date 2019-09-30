package dc.pay.business.wangpaizhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Apr 20, 2019
 */
@RequestPayHandler("WANGPAIZHIFU")
public final class WangPaiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(WangPaiZhiFuPayRequestHandler.class);

    //参数名 参数  必填  可空  加入签名    说明
    //商户ID    parter  Y   N   Y   商户id，由王牌支付分配
    private static final String parter                ="parter";
    //银行类型    b_type  Y   N   Y   支付类型，具体请参考附录1
    private static final String b_type                ="b_type";
    //金额  amount  Y   N   Y   单位元（人民币），4位小数
    private static final String amount                ="amount";
    //商户订单号   orderid Y   N   Y   商户系统订单号，该订单号将作为王牌支付接口的返回数据。该值需在商户系统内唯一，王牌支付系统暂时不检查该值是否唯一
    private static final String orderid                ="orderid";
    //下行异步通知地址    callbackurl Y   N   Y   下行异步通知过程的返回地址，需要以http://开头且没有任何参数
    private static final String callbackurl                ="callbackurl";
    //下行同步通知地址    hrefbackurl Y   Y   N   下行同步通知过程的返回地址(在支付完成后接口将会跳转到的商户系统连接地址)。
    private static final String hrefbackurl                ="hrefbackurl";
    //商品描述信息  goodsinfo   Y   N   Y   商品描述信息
    private static final String goodsinfo                ="goodsinfo";
    //随机字符串   nonce_str   Y   N   Y   随机字符串，32位
    private static final String nonce_str                ="nonce_str";
    //附加消息    attach  Y   Y   N   附加信息，不超过128位，下行中会原样返回。若该值包含中文，请注意编码(必填可空)
    private static final String attach                ="attach";
    //MD5签名   sign    Y   N   -   32位大写MD5签名值，GB2312编码(此签名为客户端提交上来的签名)
    private static final String sign                ="sign";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
//        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
//        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 3) {
//            log.error("[王牌支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//            throw new PayException("[王牌支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
//                //参数名 参数  必填  可空  加入签名    说明
//                //商户ID    parter  Y   N   Y   商户id，由王牌支付分配
//                private static final String                 ="parter";
//                //银行类型    b_type  Y   N   Y   支付类型，具体请参考附录1
//                private static final String                 ="b_type";
//                //金额  amount  Y   N   Y   单位元（人民币），4位小数
//                private static final String                 ="amount";
//                //商户订单号   orderid Y   N   Y   商户系统订单号，该订单号将作为王牌支付接口的返回数据。该值需在商户系统内唯一，王牌支付系统暂时不检查该值是否唯一
//                private static final String                 ="orderid";
//                //下行异步通知地址    callbackurl Y   N   Y   下行异步通知过程的返回地址，需要以http://开头且没有任何参数
//                private static final String                 ="callbackurl";
//                //下行同步通知地址     Y   Y   N   下行同步通知过程的返回地址(在支付完成后接口将会跳转到的商户系统连接地址)。
//                private static final String                 ="hrefbackurl";
//                //商品描述信息  goodsinfo   Y   N   Y   商品描述信息
//                private static final String                 ="goodsinfo";
//                //随机字符串   nonce_str   Y   N   Y   随机字符串，32位
//                private static final String                 ="nonce_str";
//                //附加消息    attach  Y   Y   N   附加信息，不超过128位，下行中会原样返回。若该值包含中文，请注意编码(必填可空)
//                private static final String                 ="attach";
//                //MD5签名   sign    Y   N   -   32位大写MD5签名值，GB2312编码(此签名为客户端提交上来的签名)
//                private static final String sign                ="sign";
                put(parter, channelWrapper.getAPI_MEMBERID());
                put(b_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(orderid,channelWrapper.getAPI_ORDER_ID());
                put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(hrefbackurl,channelWrapper.getAPI_WEB_URL());
                put(goodsinfo,"name");
                put(nonce_str,  HandlerUtil.getRandomStr(6));
//                put(attach,"1");
                
            }
        };
        log.debug("[王牌支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(amount+"=").append(api_response_params.get(amount)).append("&");
        signSrc.append(b_type+"=").append(api_response_params.get(b_type)).append("&");
        signSrc.append(callbackurl+"=").append(api_response_params.get(callbackurl)).append("&");
        signSrc.append(goodsinfo+"=").append(api_response_params.get(goodsinfo)).append("&");
        signSrc.append(nonce_str+"=").append(api_response_params.get(nonce_str)).append("&");
        signSrc.append(orderid+"=").append(api_response_params.get(orderid)).append("&");
        signSrc.append(parter+"=").append(api_response_params.get(parter));
        signSrc.append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[王牌支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
//        System.out.println("请求地址=========>"+channelWrapper.getAPI_CHANNEL_BANK_URL());
//        System.out.println("请求参数=========>"+JSON.toJSONString(payParam));
//        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
        if (true) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE).trim();
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[王牌支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[王牌支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
            System.out.println("请求返回=========>"+resultStr);
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //   log.error("[王牌支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //   throw new PayException(resultStr);
            //}
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[王牌支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
            //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
            // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
            //){
            if (null != jsonObject && jsonObject.containsKey("status") && "1".equalsIgnoreCase(jsonObject.getString("status"))  && jsonObject.containsKey("codeimg") && StringUtils.isNotBlank(jsonObject.getString("codeimg"))) {
                String code_url = jsonObject.getString("codeimg");
                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[王牌支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[王牌支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[王牌支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}