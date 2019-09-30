package dc.pay.business.shangrubao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Nov 19, 2018
 */
@RequestPayHandler("SHANGRUBAO")
public final class ShangRuBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ShangRuBaoPayRequestHandler.class);

    //请求参数：
    //#              参数名              含义                       类型                说明
    //1              uid                 商户uid                     string(24)          必填。您的商户唯一标识，注册后在设置里获得。一个24位字符串
    //2              price               价格                        float               必填。单位：元。精确小数点后2位
    //3              istype              支付渠道                    int                 必填。1：支付宝；2：微信支付；3：QQ钱包支付
    //4              notify_url          通知回调网址                string(255)         必填。用户支付成功后，我们服务器会主动发送一个post消息到这个网址。由您自定义。不要urlencode。例：http://www.aaa.com/kuairubao_notify
    //5              return_url          跳转网址                    string(255)         必填。用户支付成功后，我们会让用户浏览器自动跳转到这个网址。由您自定义。不要urlencode。例：http://www.aaa.com/kuairubao_return
    //6              orderid             商户自定义订单号            string(50)          必填。我们会据此判别是同一笔订单还是新订单。我们回调时，会带上这个参数。例：201710192541,不同的订单订单号不能重复
    //7              orderuid            商户自定义客户号            string(100)         选填。我们会显示在您后台的订单列表中，方便您看到是哪个用户的付款，方便后台对账。强烈建议填写。可以填用户名，也可以填您数据库中的用户uid。例：xxx, xxx@aaa.com
    //8              goodsname           商品名称                    string(100)         选填。您的商品名称，用来显示在后台的订单名称。如未设置，我们会使用后台商品管理中对应的商品名称
    //10             version             协议版本号                  int                 必填。当前为2
    //11             isgo_alipay         是否自动打开支付宝          int                 选填。当前为1，当发起支付宝支付的时候传入才有效果，1表示自动打开，0表示不自动打开
    //9              key                 秘钥                        string(32)          必填。把使用到的所有参数，连Token一起，按参数名字母升序排序。把参数名和参数值拼接在一起。做md5-32位加密，取字符串小写。得到key。网址类型的参数值不要urlencode。
    private static final String uid                      ="uid";
    private static final String price                    ="price";
    private static final String istype                   ="istype";
    private static final String notify_url               ="notify_url";
    private static final String return_url               ="return_url";
    private static final String orderid                  ="orderid";
    private static final String orderuid                 ="orderuid";
    private static final String goodsname                ="goodsname";
    private static final String version                  ="version";
    private static final String isgo_alipay              ="isgo_alipay";
//    private static final String key                      ="key";

    private static final String key        ="token";
    //signature    数据签名    32    是    　
    private static final String signature  ="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(uid, channelWrapper.getAPI_MEMBERID());
                put(price,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
//                put(price,  HandlerUtil.getYuanWithoutZero(channelWrapper.getAPI_AMOUNT()));
                put(istype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(orderid,channelWrapper.getAPI_ORDER_ID());
                put(orderuid,handlerUtil.getRandomStr(8));
                put(goodsname,"name");
                put(version,"2");
                if (handlerUtil.isZFB(channelWrapper)) {
                    put(isgo_alipay,"1");
                }
            }
        };
        log.debug("[商入宝]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        Map<String,String> map = new TreeMap<>(api_response_params);
        map.put(key, channelWrapper.getAPI_KEY());
        List paramKeys = MapUtils.sortMapByKeyAsc(map);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!isgo_alipay.equals(paramKeys.get(i)) && StringUtils.isNotBlank(map.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(map.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[商入宝]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[商入宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[商入宝]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}