package dc.pay.business.qianguizhifu;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jan 2, 2019
 */
@RequestPayHandler("QIANGUIZHIFU")
public final class QianGuiZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(QianGuiZhiFuPayRequestHandler.class);

    //参数名               必填          类型               示例值 说明
    //ly_version            是           string             2.0 接口版本号默认为2.0
    private static final String ly_version                ="ly_version";
    //ly_parter             是           string             615214  云支付系统为商户分配的用户ID
    private static final String ly_parter                ="ly_parter";
    //ly_key                是           string             E290F28DC133D99C8495746CA6A0175B    云支付系统为商户分配的用户key
//    private static final String ly_key                ="ly_key";
    //ly_money              是           string             1   订单金额,以分为单位
    private static final String ly_money                ="ly_money";
    //ly_orderno            是           string             20150806125346  商户系统内部订单号，要求32个字符内，只能是数字、大小写字母，且在同一个商户号下唯一。
    private static final String ly_orderno                ="ly_orderno";
    //ly_trade_type         是           string             pay_alipay_code 订单的付款类型-详见文档中心-参数规定内的编码
    private static final String ly_trade_type                ="ly_trade_type";
    //ly_scan_code          是           string             true    如果订单类型为扫码支付,true为返回二维码连接,false为不返回二维码连接
    private static final String ly_scan_code                ="ly_scan_code";
    //ly_return_url         是           string             http://www.xwbtech.cn/return_url.php    同步接收支付结果通知的回调地址，通知url必须为外网可访问的url，不能携带参数。
    private static final String ly_return_url                ="ly_return_url";
    //ly_notify_url         是           string             http://www.xwbtech.cn/notify_url.php    异步接收支付结果通知的回调地址，通知url必须为外网可访问的url，不能携带参数。
    private static final String ly_notify_url                ="ly_notify_url";
    //ly_notes              否           string             备注  附加数据，在支付通知中以[Base64编码]原样返回，可作为自定义参数使用。
    private static final String ly_notes                ="ly_notes";
    //ly_sign_type          是           string             MD5 签名类型，默认为MD5。
    private static final String ly_sign_type                ="ly_sign_type";
    //ly_sign               是           string             c380bec2bfd727a4b6845133519f3ad6    通过签名算法计算得出的签名值，详见文档->接口规则->签名规则（小写32位）
//    private static final String ly_sign                ="ly_sign";

    private static final String key        ="ly_key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(ly_version,"2.0");
                put(ly_parter, channelWrapper.getAPI_MEMBERID());
                put(ly_money,  channelWrapper.getAPI_AMOUNT());
                put(ly_orderno,channelWrapper.getAPI_ORDER_ID());
                put(ly_trade_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(ly_scan_code,"true");
                put(ly_return_url,channelWrapper.getAPI_WEB_URL());
                put(ly_notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(ly_notes, channelWrapper.getAPI_MEMBERID());
                put(ly_sign_type,"MD5");
            }
        };
        log.debug("[钱柜支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[钱柜支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
      String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//      String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//      String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//      String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
      if (StringUtils.isBlank(resultStr)) {
          log.error("[钱柜支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
          //log.error("[钱柜支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
          //throw new PayException("返回空,参数："+JSON.toJSONString(map));
      }
      if (!resultStr.contains("{") || !resultStr.contains("}")) {
         log.error("[钱柜支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
         throw new PayException(resultStr);
      }
      //JSONObject jsonObject = JSONObject.parseObject(resultStr);
      JSONObject jsonObject;
      try {
          jsonObject = JSONObject.parseObject(resultStr);
      } catch (Exception e) {
          e.printStackTrace();
          log.error("[钱柜支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
      //只取正确的值，其他情况抛出异常
      if (null != jsonObject && jsonObject.containsKey("IsSuccess") && "true".equalsIgnoreCase(jsonObject.getString("IsSuccess"))  && jsonObject.containsKey("Data") && StringUtils.isNotBlank(jsonObject.getString("Data"))) {
          JSONObject jsonObject2 = jsonObject.getJSONObject("Data");
          if (null != jsonObject2 && jsonObject2.containsKey("CodeUrl") && StringUtils.isNotBlank(jsonObject2.getString("CodeUrl"))) {
              String code_url = jsonObject2.getString("CodeUrl");
              result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
          }else {
              log.error("[钱柜支付]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              throw new PayException(resultStr);
          }
      }else {
          log.error("[钱柜支付]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          throw new PayException(resultStr);
      }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[钱柜支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[钱柜支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}