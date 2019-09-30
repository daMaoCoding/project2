package dc.pay.business.dududuzhifu;

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
 * Nov 10, 2018
 */
@RequestPayHandler("DUDUDUZHIFU")
public final class DuDuDuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DuDuDuZhiFuPayRequestHandler.class);

    //字段名             变量名             必填           类型                备注
    //mchid              商户ID              是            String(32)          商户在平台的 商户ID号
    //mchno              商户订单号          是            String(32)          商户自己生成的订单号 由英文、数字、_、- 、组成，不能含有特殊符号 如：2999218888886、no2999218888886 ，长度不能超过32位
    //tradetype          订单类型            是            String(32)          weixin 表示发起微信扫码支付 weixinh5 表示发起微信h5支付 alipay 表示发起支付宝扫码支付 alipayh5 表示发起支付宝h5支付
    //totalfee           支付金额            是            String(32)          订单需要支付的金额，单位：分（人民币），如： 10.00元= 1000
    //descrip            订单描述            是            String(225)         长度不能超过127位 可以由中文、英文、数字、_、- 、组成不能含有特殊符号如： XX充值中心-XX会员充值 含有中文需要utf-8编码
    //attach             附加数据            否            String(225)         可为空， 商户的附加数据，回调的时候会原样返回，主要用于商户携带订单的自定义数据 如：XX分店长度不能超过127位， 可以由中文、英文、数字、_、- 、组成不能含有特殊符号 含有中文需要utf-8编码 
    //clientip           终端IP              是            String(64)          长度不能超过46位， 订单生成的机器 IP
    //notifyurl          异步通知地址        是            String(225)         接收平台异步通知回调地址，通知url必须为直接可访问的url，不能携带参数。如： http://www.xxxx.com/wxpay/pay.php
    //returnurl          同步通知地址        否            String(225)         可为空， 订单支付成功后同步跳转的地址, url必须为直接可访问的url，不能携带参数。如： http://www.xxxx.com/wxpay/pay.php
    //sign               MD5签名             是            String(32)          sign加密时要按照下面示例： mchid=10000&mchno=201803051730&tradetype=alipayh5&totalfee=1000&descrip=xxxx&attach=xxxx&clientip=127.0.0.1&notifyurl=http://xxxx.cn/wxpay/pay.php&returnurl=http://xxxx.cn/wxpay/pay.php&key=c4b70b766ea78fe1689f4e4e1afa291a key值为商户在平台的 通信KEY 组织好需要提交的数据按以上排列进行MD5加密后 赋值给 sign
    //返回code值说明
    private static final String mchid                                     ="mchid";
    private static final String mchno                                     ="mchno";
    private static final String tradetype                                 ="tradetype";
    private static final String totalfee                                  ="totalfee";
    private static final String descrip                                   ="descrip";
    private static final String attach                                    ="attach";
    private static final String clientip                                  ="clientip";
    private static final String notifyurl                                 ="notifyurl";
    private static final String returnurl                                 ="returnurl";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mchid, channelWrapper.getAPI_MEMBERID());
                put(mchno,channelWrapper.getAPI_ORDER_ID());
                put(tradetype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(totalfee,  channelWrapper.getAPI_AMOUNT());
                put(descrip, "name");
                put(attach, channelWrapper.getAPI_MEMBERID());
                put(clientip, channelWrapper.getAPI_Client_IP());
                put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnurl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[嘟嘟嘟支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(mchid+"=").append(api_response_params.get(mchid)).append("&");
        signSrc.append(mchno+"=").append(api_response_params.get(mchno)).append("&");
        signSrc.append(tradetype+"=").append(api_response_params.get(tradetype)).append("&");
        signSrc.append(totalfee+"=").append(api_response_params.get(totalfee)).append("&");
        signSrc.append(descrip+"=").append(api_response_params.get(descrip)).append("&");
        signSrc.append(attach+"=").append(api_response_params.get(attach)).append("&");
        signSrc.append(clientip+"=").append(api_response_params.get(clientip)).append("&");
        signSrc.append(notifyurl+"=").append(api_response_params.get(notifyurl)).append("&");
        signSrc.append(returnurl+"=").append(api_response_params.get(returnurl)).append("&");
        signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[嘟嘟嘟支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
//            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
//        else{
////            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
//            //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[嘟嘟嘟支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//                //log.error("[嘟嘟嘟支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
//                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
//            }
//            System.out.println("请求返回=========>"+resultStr);
//            if (handlerUtil.isWapOrApp(channelWrapper)) {
//                result.put( HTMLCONTEXT, resultStr);
//            }else {
//                if (!resultStr.contains("{") || !resultStr.contains("}")) {
//                    log.error("[嘟嘟嘟支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    throw new PayException(resultStr);
//                }
//                //JSONObject resJson = JSONObject.parseObject(resultStr);
//                JSONObject resJson;
//                try {
//                    resJson = JSONObject.parseObject(resultStr);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    log.error("[嘟嘟嘟支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    throw new PayException(resultStr);
//                }
//                //只取正确的值，其他情况抛出异常
//                if (null != resJson && resJson.containsKey("status") && "1".equalsIgnoreCase(resJson.getString("status"))  && resJson.containsKey("codeimg") && StringUtils.isNotBlank(resJson.getString("codeimg"))) {
//                    String code_url = resJson.getString("codeimg");
//                    result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
//                    //if (handlerUtil.isWapOrApp(channelWrapper)) {
//                    //    result.put(JUMPURL, code_url);
//                    //}else{
//                    //    result.put(QRCONTEXT, code_url);
//                    //}
//                }else {
//                    log.error("[嘟嘟嘟支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    throw new PayException(resultStr);
//                }
//            }
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[嘟嘟嘟支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[嘟嘟嘟支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}