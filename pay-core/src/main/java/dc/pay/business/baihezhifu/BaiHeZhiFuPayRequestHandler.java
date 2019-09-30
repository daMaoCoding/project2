package dc.pay.business.baihezhifu;

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
 * May 17, 2019
 */
@RequestPayHandler("BAIHEZHIFU")
public final class BaiHeZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BaiHeZhiFuPayRequestHandler.class);

    //支付接口参数【请求方式：GET或POST均可】【如遇问题可尝试用html表单提交参数】
    //参数名称    参数含义    必填  说明
    //fxid    商务号 是   唯一号，由百合支付提供
    private static final String fxid                    ="fxid";
    //fxddh   商户订单号   是   仅允许字母或数字类型,不超过22个字符，不要有中文
    private static final String fxddh                    ="fxddh";
    //fxdesc  商品名称    是   utf-8编码
    private static final String fxdesc                    ="fxdesc";
    //fxfee   支付金额    是   请求的价格(单位：元) 可以0.01元
    private static final String fxfee                    ="fxfee";
    //fxnotifyurl 异步通知地址  是   异步接收支付结果通知的回调地址，通知url必须为外网可访问的url，不能携带参数。
    private static final String fxnotifyurl                    ="fxnotifyurl";
    //fxbackurl   同步通知地址  是   支付成功后跳转到的地址，不参与签名。
    private static final String fxbackurl                    ="fxbackurl";
    //fxpay   请求类型 【支付宝Bank（PC）：zfbzk】【支付宝H5：zfbh5】【支付宝扫码（PC）：zfbsm】【微信扫码（PC）：wxsm】【微信H5：wxh5】    是   请求支付的接口类型。
    private static final String fxpay                    ="fxpay";
    //fxnotifystyle   异步数据类型  否   异步返回数据的类型，默认1 返回数据为表单数据（Content-Type: multipart/form-data），2 返回post json数据。
    private static final String fxnotifystyle                    ="fxnotifystyle";
    //fxattch 附加信息    否   原样返回，utf-8编码
    private static final String fxattch                    ="fxattch";
    //fxsmstyle   扫码模式    否   用于扫码模式（sm），仅带sm接口可用，默认0返回扫码图片，为1则返回扫码跳转地址。
    private static final String fxsmstyle                    ="fxsmstyle";
    //fxbankcode  银行类型    否   用于网银直连模式，请求的银行编号，参考银行附录,仅网银接口可用。
//    private static final String fxbankcode                    ="fxbankcode";
    //fxfs    反扫付款码数字 否   用于用户被扫，用户的付款码数字,仅反扫接口可用。
//    private static final String fxfs                    ="fxfs";
    //fxuserid    快捷模式绑定商户id  否   用于识别用户绑卡信息，仅快捷接口可用。
//    private static final String fxuserid                    ="fxuserid";
    //fxip    支付用户IP地址    是   用户支付时设备的IP地址
    private static final String fxip                    ="fxip";
    //fxsign  签名【md5(商务号+商户订单号+支付金额+异步通知地址+商户秘钥)】 是   通过签名算法计算得出的签名值。
//    private static final String fxsign                    ="fxsign";

    //signature    数据签名    32    是    　
//    private static final String signature  ="fxsign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[百合支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
            throw new PayException("[百合支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
//                put(fxid, channelWrapper.getAPI_MEMBERID());
                put(fxid, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(fxddh,channelWrapper.getAPI_ORDER_ID());
                put(fxdesc,"name");
                put(fxfee,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(fxnotifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(fxbackurl,channelWrapper.getAPI_WEB_URL());
//                put(fxpay,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(fxpay,channelWrapper.getAPI_MEMBERID().split("&")[1]);
//                put(fxnotifystyle,"json");
                put(fxnotifystyle,"2");
                if (handlerUtil.isWEBWAPAPP_SM(channelWrapper)) {
                    put(fxsmstyle,"1");
                }
                put(fxattch,channelWrapper.getAPI_MEMBERID());
//                if (handlerUtil.isWY(channelWrapper)) {
//                    put(fxbankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                }else {
//                }
//                if (handlerUtil.isFS(channelWrapper)) {
//                    put(fxfs,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                }
//                if (快捷) {
//                    fxuserid
//                }
                put(fxip,channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[百合支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(api_response_params.get(fxid));
        signSrc.append(api_response_params.get(fxddh));
        signSrc.append(api_response_params.get(fxfee));
        signSrc.append(api_response_params.get(fxnotifyurl));
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[百合支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
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
////              log.error("[百合支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
////              throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
//              log.error("[百合支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
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
////                      log.error("[百合支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
////                      throw new PayException(resultStr);
////                  }
////              }
//          }else {
//              log.error("[百合支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//              throw new PayException(resultStr);
//          }
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[百合支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[百合支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}