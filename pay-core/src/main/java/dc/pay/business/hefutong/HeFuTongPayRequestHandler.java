package dc.pay.business.hefutong;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jul 20, 2018
 */
@RequestPayHandler("HEFUTONG")
public final class HeFuTongPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HeFuTongPayRequestHandler.class);

    //输入项              输入项名称                       注释
    //mch_id              商户编号                                 
    //out_trade_no        商户订单号                       
    //body                商户描述                   
    //sub_openid          用户的openid                     微信公众号支付时候必须填写，其他情况不用传参数
    //callback_url        前台通知地址                     
    //notify_url          后台通知地址                     
    //total_fee           金额                             以“元”为单位必须保留两位小数
    //service             接口类型                         wx:微信
    //way                 支付方式                         h5：公众号支付或者其他js支付
    //Appid               应用编号                         没有应用时可以不传此参数
    //format              返回数据格式                     json或者xml这两个格式当为json时直接返回json数据，xml时跳转到平台收银台 
    //mch_create_ip       请求客户的ip                     wap支付时，wap发起H5终端IP，和微信客户端获得IP需要为同一个IP
    //sub_openid          用户的openid                     公众号支付时必须填写，其他支付不用填写，必须是报备公众号获取的用户openid
    //goods_tag           网银支付网银编码（第三方收银台时此参数为空）          非网银可以为空，网银编码为：
    //sign                签名                            Md5(mch_id + out_trade_no + callback_url + notify_url + total_fee + service + way+ format + 商户密钥)加密编码格式为utf-8签名后的字符串不区分大小写
    private static final String mch_id                     ="mch_id";
    private static final String out_trade_no               ="out_trade_no";
    private static final String body                       ="body";
//    private static final String sub_openid                 ="sub_openid";
    private static final String callback_url               ="callback_url";
    private static final String notify_url                 ="notify_url";
    private static final String total_fee                  ="total_fee";
    private static final String service                    ="service";
    private static final String way                        ="way";
//    private static final String Appid                      ="Appid";
    private static final String format                     ="format";
    private static final String mch_create_ip              ="mch_create_ip";
//    private static final String goods_tag                  ="goods_tag";


    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mch_id, channelWrapper.getAPI_MEMBERID());
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(body,"name");
                put(callback_url,channelWrapper.getAPI_WEB_URL());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(total_fee,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(service,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(way,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                put(format,"json");
                put(mch_create_ip,channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[合付通]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(api_response_params.get(mch_id));
        signSrc.append(api_response_params.get(out_trade_no));
        signSrc.append(api_response_params.get(callback_url));
        signSrc.append(api_response_params.get(notify_url));
        signSrc.append(api_response_params.get(total_fee));
        signSrc.append(api_response_params.get(service));
        signSrc.append(api_response_params.get(way));
        signSrc.append(api_response_params.get(format));
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[合付通]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }
    
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();
        if (StringUtils.isBlank(resultStr)) {
            log.error("[合付通]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
            throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
        }
        JSONObject resJson = JSONObject.parseObject(resultStr);
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("success") && "true".equalsIgnoreCase(resJson.getString("success"))  && resJson.containsKey("pay_info") && StringUtils.isNotBlank(resJson.getString("pay_info"))) {
            result.put(JUMPURL, resJson.getString("pay_info"));
        }else {
            log.error("[合付通]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[合付通]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[合付通]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}