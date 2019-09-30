package dc.pay.business.huidezhifu;

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
 * May 27, 2019
 */
@RequestPayHandler("HUIDEZHIFU")
public final class HuiDeZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HuiDeZhiFuPayRequestHandler.class);

    //参数名称    变量名 类型长度    是否可空    说明
    //版本号 version varchar(5)  否   默认1.0
    private static final String version                    ="version";
    //商户编号    customerid  int(8)  否   商户后台获取
    private static final String customerid                    ="customerid";
    //商户订单号   sdorderno   varchar(100)    否   用户编号+生成订单号以免平台订单号重复冲突
    private static final String sdorderno                    ="sdorderno";
    //订单金额    total_fee   decimal(10,2)   否   精确到小数点后两位，例如10.24（最少1元）
    private static final String total_fee                    ="total_fee";
    //支付编号    paytype varchar(10) 否   详见附录1
    private static final String paytype                    ="paytype";
    //用户ip    client  varchar(20) 是   用户ip地址（仅适用h5支付）
    private static final String client                    ="client";
    //异步通知URL notifyurl   varchar(100)    否   不能带有任何参数
    private static final String notifyurl                    ="notifyurl";
    //同步跳转URL returnurl   varchar(100)    否   不能带有任何参数
    private static final String returnurl                    ="returnurl";
    //订单备注说明  remark  varchar(50) 否   不能为空（可以和commodityName相同）中文需要utf-8编码才能传输
    private static final String remark                    ="remark";
    //商品名称    commodityName   varchar(50) 否   不能为空，中文需要utf-8编码才能传输
    private static final String commodityName                    ="commodityName";
    //获取微信二维码 get_code    tinyint(1)  是   支付宝扫码默认传二维码链接（暂未启用该功能，可先填1）
//    private static final String get_code                    ="get_code";
    //获取json数据    get_json    Int(1)  是   H5可用，公众号暂时不能反回json数据，默认为0
//    private static final String get_json                    ="get_json";
    //设备编号    devicetype  Int（3）  是   应用类型：安卓填1         IOS填2    不填默认为1    （为微信h5支付时需要）
//    private static final String devicetype                    ="devicetype";
    //md5签名串  sign    varchar(32) 否   参照md5签名说明
//    private static final String sign                    ="sign";

    //signature    数据签名    32    是    　
//    private static final String signature  ="fxsign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version,"1.0");
                put(customerid, channelWrapper.getAPI_MEMBERID());
                put(sdorderno,channelWrapper.getAPI_ORDER_ID());
                put(total_fee,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                if (handlerUtil.isWapOrApp(channelWrapper)) {
                    put(client,channelWrapper.getAPI_Client_IP());
                }
                put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnurl,channelWrapper.getAPI_WEB_URL());
                put(remark,"name");
                put(commodityName,"name");
            }
        };
        log.debug("[汇德支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
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
        log.debug("[汇德支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        if (HandlerUtil.isWEBWAPAPP_SM(channelWrapper)) {
        if (true) {
//            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
        
//        else{
//
////          String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
//          String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST,defaultHeaders);
////          String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//          if (StringUtils.isBlank(resultStr)) {
////              log.error("[汇德支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
////              throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
//              log.error("[汇德支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
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
////                      log.error("[汇德支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
////                      throw new PayException(resultStr);
////                  }
////              }
//          }else {
//              log.error("[汇德支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//              throw new PayException(resultStr);
//          }
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[汇德支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[汇德支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}