package dc.pay.business.junbaofu;

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
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author andrew
 * Jan 28, 2019
 */
@Slf4j
@RequestPayHandler("JUNBAOFU")
public final class JunBaoFuPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);

    private static  final  String uid="uid";    //  商户uid   int(10) 必填。您的商户唯一标识，注册后在设置里获得。
    private static  final  String price="price";    //  价格  float   必填。单位：元。精确小数点后2位
    private static  final  String type="type";    //    支付渠道    int 必填。1：微信支付；2：支付宝
    private static  final  String notify_url="notify_url";    //    通知回调网址  string(255) 必填。用户支付成功后，我们服务器会主动发送一个post消息到这个网址。由您自定义。不要urlencode。例：http://www .aaa.com/vpay_notify
    private static  final  String return_url="return_url";    //    跳转网址    string(255) 必填。用户支付成功后，我们会让用户浏览器自动跳转到这个网址。由您自定义。不要urlencode。例：http://www.aaa .com/vpay_return
    private static  final  String order_id="order_id";    //    商户自定义订单号    string(50)  必填。我们会据此判别是同一笔订单还是新订单。我们回调时，会带上这个参数。例：201710192541
    private static  final  String order_uid="order_uid";    //  商户自定义客户号    string(100) 选填。我们会显示在您后台的订单列表中，方便您看到是哪个用户的付款，方便后台对账。强烈建议填写。可以填用户名，也可以填您数据库中的用户uid。例：xxx, xxx@aaa.com
    private static  final  String order_name="order_name";    //    商品名称    string(100) 选填。您的商品名称，用来显示在后台的订单名称。如未设置，我们会使用后台商品管理中对应的商品名称
    private static  final  String key="key";    //  秘钥  string(32)  必填。把使用到的所有参数，连Token一起，按参数名字母升序排序。把参数值拼接在一起。做md5-32位加密，取字符串小写。得到key。网址类型的参数值不要urlencode。

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        //HandlerUtil.isWY(channelWrapper)
        payParam.put(uid,channelWrapper.getAPI_MEMBERID());
        payParam.put(price,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
        payParam.put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        payParam.put(return_url,channelWrapper.getAPI_WEB_URL());
        payParam.put(order_id,channelWrapper.getAPI_ORDER_ID());
        payParam.put(order_uid, System.currentTimeMillis()+"");
        payParam.put(order_name,channelWrapper.getAPI_ORDER_ID() );
        log.debug("[郡保富]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(api_response_params.get(notify_url));
        signSrc.append(api_response_params.get(order_id));
        signSrc.append(api_response_params.get(order_name));
        signSrc.append(api_response_params.get(order_uid));
        signSrc.append(api_response_params.get(price));
        signSrc.append(api_response_params.get(return_url));
        signSrc.append(channelWrapper.getAPI_KEY());
        signSrc.append(api_response_params.get(type));
        signSrc.append(api_response_params.get(uid));
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[郡保富]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map result = Maps.newHashMap();
        if (true) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
//          String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
          String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
          if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
              result.put(HTMLCONTEXT,resultStr);
          }else if(StringUtils.isNotBlank(resultStr) ){
              JSONObject jsonResultStr = JSON.parseObject(resultStr);
              if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "1".equalsIgnoreCase(jsonResultStr.getString("code"))
                      && jsonResultStr.containsKey("data") && null!=jsonResultStr.getJSONObject("data")){
                  if(HandlerUtil.isWapOrApp(channelWrapper) && jsonResultStr.getJSONObject("data").containsKey("callappqrcode")&& StringUtils.isNotBlank( jsonResultStr.getJSONObject("data").getString("callappqrcode")   ) ){
                      result.put(JUMPURL, jsonResultStr.getJSONObject("data").getString("callappqrcode") );
                  }else if( jsonResultStr.getJSONObject("data").containsKey("qrcode")&& StringUtils.isNotBlank( jsonResultStr.getJSONObject("data").getString("qrcode") )){
                      result.put(QRCONTEXT, jsonResultStr.getJSONObject("data").getString("qrcode") );
                  }else {throw new PayException(resultStr); }
              }else {throw new PayException(resultStr); }
          }else{ throw new PayException(EMPTYRESPONSE);}
             
        }
        
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[郡保富]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[郡保富]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}