package dc.pay.business.juxinfu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jun 14, 2019
 */
@RequestPayHandler("JUXINFU")
public final class JuXinFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JuXinFuPayRequestHandler.class);

    //请求地址    http://域名/apisubmit
    //参数名称    参数编码    必要(Y)/非必要(N)    字段长度    参数编码取值范围
    //版本号 version Y   定varchar(5) 默认1.0
    private static final String version                    ="version";
    //商户编号    customerid  Y   定int(8))    商户编号是聚信付商户在支付平台上开设的商户号码：商户后台获取
    private static final String customerid                    ="customerid";
    //支付类型    paytype Y   变varchar(10)    网银网关参数定为：wypay        注：每个参数直接决定商户使用哪条通道
    private static final String paytype                    ="paytype";
    //交易金额    total_fee   Y   变decimal(10,2)  订单的资金总额，单位为 RMB-元。精确到小数点后两位，例10.24
    private static final String total_fee                    ="total_fee";
    //商户订单号   sdorderno   Y   定Varchar(20)    确保唯一,长度不超过20，尽量随机生成，例如：时间戳+MD5(随机字符串+随机字符串)
    private static final String sdorderno                    ="sdorderno";
    //前端通知地址  returnurl   Y   变Varchar(50)    支付成功之后调起的前端界面URL，确保外网可以访问,并且视情况进行url encode (咨询运营),不允许带!@#+，不能带有任何参数，例如qq+v&c?!22
    private static final String returnurl                    ="returnurl";
    //异步通知地址  notifyurl   Y   变Varchar(50)    支付成功之后将异步返回给到商户服务端！确保外网可以访问，不能带有任何参数
    private static final String notifyurl                    ="notifyurl";
    //银行编码    bankcode    Y   定Varchar(10)    请参照网银银行编码附录(paytype为bank必传)
//    private static final String bankcode                    ="bankcode";
    //订单备注说明  remark  N   变Varchar(50)    商户订单附加信息，可做扩展参数，可为空
//    private static final String remark                    ="remark";
    //获取二维码链接 get_code    N   扫码类型支付方式必传1,0   值1为获取，值0不获取，只对paytype为扫码类型的支付方式有效，目前已废弃该参数，给个默认值即可
    private static final String get_code                    ="get_code";
    //签名参数    sign    Y   定String(32) 详见 签名说明
//    private static final String sign                    ="sign";
        
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version,"1.0");
                put(customerid, channelWrapper.getAPI_MEMBERID());
                put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(total_fee,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(sdorderno,channelWrapper.getAPI_ORDER_ID());
                put(returnurl,channelWrapper.getAPI_WEB_URL());
                put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(get_code,"0");

            }
        };
        log.debug("[聚信付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(version+"=").append(api_response_params.get(version)).append("&");
        signSrc.append(customerid+"=").append(api_response_params.get(customerid)).append("&");
        signSrc.append(total_fee+"=").append(api_response_params.get(total_fee)).append("&");
        signSrc.append(sdorderno+"=").append(api_response_params.get(sdorderno)).append("&");
        signSrc.append(notifyurl+"=").append(api_response_params.get(notifyurl)).append("&");
        signSrc.append(returnurl+"=").append(api_response_params.get(returnurl)).append("&");
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[聚信付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        if (HandlerUtil.isWEBWAPAPP_SM(channelWrapper)) {
        if (true) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
        
//        else{
//
////          String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
//          String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST,defaultHeaders);
////          String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//          if (StringUtils.isBlank(resultStr)) {
////              log.error("[聚信付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
////              throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
//              log.error("[聚信付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//              throw new PayException(resultStr);
//          }
//          resultStr = UnicodeUtil.unicodeToString(resultStr);
//          JSONObject resJson = JSONObject.parseObject(resultStr);
//          //只取正确的值，其他情况抛出异常
//          if (null != resJson && resJson.containsKey("status") && "1".equalsIgnoreCase(resJson.getString("status"))  && 
//                  resJson.containsKey("payurl") && StringUtils.isNotBlank(resJson.getString("payurl"))) {
//                  result.put(JUMPURL, resJson.getString("payurl"));
//
////              if (handlerUtil.isWapOrApp(channelWrapper)) {
////                  result.put(JUMPURL, resJson.getString("payurl"));
////              }else {
////                  try {
//////                      result.put(QRCONTEXT, URLDecoder.decode(resJson.getString("payurl"), "UTF-8"));
////                      result.put(JUMPURL, URLDecoder.decode(resJson.getString("payurl"), "UTF-8"));
////                  } catch (UnsupportedEncodingException e) {
////                      e.printStackTrace();
////                      log.error("[聚信付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
////                      throw new PayException(resultStr);
////                  }
////              }
//          }else {
//              log.error("[聚信付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//              throw new PayException(resultStr);
//          }
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[聚信付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[聚信付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}