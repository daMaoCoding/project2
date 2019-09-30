package dc.pay.business.nongxinzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
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
 * @author Cobby
 * Mar 1, 2019
 */
@RequestPayHandler("NONGXINZHIFU")
public final class NongXinZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(NongXinZhiFuPayRequestHandler.class);

//uid         商户uid        必填。  您的商户唯一标识，在“账户信息”-“支付信息”中获取。
//price       价格           必填。  单位：元。精确小数点后2位
//type        支付渠道        选填。  （如不填则默认alipayBank，暂不参与签名）。alipay：支付宝转账包；alipayBank：支付宝转银行；alipay2：支付宝原生码；alipay3：支付宝付款码；912：聚合企业扫码；913：支付宝红包码；914：微信固额扫码；915：支付宝口令码；
//notify_url  通知回调网址     必填。  用户支付成功后，我们服务器会主动发送一个post消息到这个网址。由您自定义。不要urlencode。例：http://www .aaa.com/qpay_notify
//return_url  跳转网址        必填。  用户支付成功后，我们会让用户浏览器自动跳转到这个网址。由您自定义。不要urlencode。例：http://www.aaa .com/qpay_return
//orderid     商户自定义订单号  必填。  我们会据此判别是同一笔订单还是新订单。我们回调时，会带上这个参数。例：201710192541
//orderuid    商户自定义客户号  选填。  我们会显示在您后台的订单列表中，方便您看到是哪个用户的付款，方便后台对账。强烈建议填写。可以填用户名，也可以填您数据库中的用户uid。例：xxx, xxx@aaa.com
//goodsname   商品名称        选填。  您的商品名称，用来显示在后台的订单名称。如未设置，我们会使用后台商品管理中对应的商品名称
//attach      附加内容        选填。  回调时将会根据传入内容原样返回
//key         秘钥           必填。  把使用到的所有参数，连Token一起，按 参数名 字母升序排序。把 参数值 拼接在一起。做md5-32位加密，取字符串小写。得到key。网址类型的参数值不要urlencode。
//format      返回格式        选填。  jsonhtml（场景二时传入），回调时将会返回json数据值
    private static final String uid                ="uid";         //商户uid
    private static final String price              ="price";       //价格
    private static final String type               ="type";        //支付渠道
    private static final String notify_url         ="notify_url";  //通知回调网址
    private static final String return_url         ="return_url";  //跳转网址
    private static final String orderid            ="orderid";     //商户自定义订单号
    private static final String goodsname          ="goodsname";   //商户自定义订单号
    private static final String orderuid           ="orderuid";    //商户自定义订单号
    private static final String format             ="format";      //jsonhtml（场景二时传入），回调时将会返回json数据值

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(uid, channelWrapper.getAPI_MEMBERID());
                put(price,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(orderid,channelWrapper.getAPI_ORDER_ID());
                put(orderuid,channelWrapper.getAPI_ORDER_ID());
                put(goodsname,"name");
                put(format,"jsonhtml");
            }
        };
        log.debug("[农信支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> params) throws PayException {

//     就按这个顺序拼接：goodsname + notify_url + orderid + orderuid + price + return_url + token + uid
         String paramsStr = String.format("%s%s%s%s%s%s%s%s",
                 params.get(goodsname),
                 params.get(notify_url),
                 params.get(orderid),
                 params.get(orderuid),
                 params.get(price),
                 params.get(return_url),
                 channelWrapper.getAPI_KEY(),
                 params.get(uid));
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[农信支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {
            
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            }else{
                String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
                JSONObject jsonObject;
                try {
                    jsonObject = JSONObject.parseObject(resultStr);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("[农信支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                if (null != jsonObject && jsonObject.containsKey("code") && "1".equalsIgnoreCase(jsonObject.getString("code"))
                                       && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))) {
                    String code_url = jsonObject.getString("data");
                    jsonObject = JSONObject.parseObject(code_url);
                    result.put( JUMPURL , jsonObject.getString("qrcode"));
                }else {
                    log.error("[农信支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            }
            
        } catch (Exception e) {
            log.error("[农信支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[农信支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[农信支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}