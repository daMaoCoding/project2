package dc.pay.business.juze;

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
 * Jul 24, 2018
 */
@RequestPayHandler("JUZE")
public final class JuZePayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JuZePayRequestHandler.class);

    //参数           名称             值                                        备注
    //uid            商户编号         15000001                                 必须,
    //type           返回类型         img                                      必须,img html json
    //bank           支付通道         AliPaySK                                 必须,AliPaySK|WxPaySK
    //ordid          订单编号         123456789                                必须,不能重复
    //amt            订单金额         10                                       必须,整数
    //info           附带信息         Abcdefg                                  必须,随意
    //ret            通知地址         http://www.a.com/ret                     必须,需能远程访问
    //jump           跳转地址         http://www.a.com/jump                    可填,暂时保留
    //sign           签名             f9314e67f999c2ddbfd60fb7136b26ef        必须uid,type,bank,amt,ordid,info,ret,jump,skey以上参数按顺序连接,采用md5(32位)加密
    private static final String uid                ="uid";
    private static final String type               ="type";
    private static final String bank               ="bank";
    private static final String ordid              ="ordid";
    private static final String amt                ="amt";
    private static final String info               ="info";
    private static final String ret                ="ret";
    private static final String jump               ="jump";

    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(uid, channelWrapper.getAPI_MEMBERID());
                put(type,"json");
                put(bank,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(ordid,channelWrapper.getAPI_ORDER_ID());
                put(amt,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
//                put(info,URLEncoder.encode(channelWrapper.getAPI_MEMBERID()));
//                put(info,HandlerUtil.UrlEncode(channelWrapper.getAPI_MEMBERID()));
//                put(ret,HandlerUtil.UrlEncode(channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL()));
//                put(jump,HandlerUtil.UrlEncode(channelWrapper.getAPI_WEB_URL()));
                put(info,channelWrapper.getAPI_MEMBERID());
                put(ret,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(jump,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[聚泽]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(api_response_params.get(uid));
        signSrc.append(api_response_params.get(type));
        signSrc.append(api_response_params.get(bank));
        signSrc.append(api_response_params.get(amt));
        signSrc.append(api_response_params.get(ordid));
        signSrc.append(HandlerUtil.UrlEncode(api_response_params.get(info)));
        signSrc.append(HandlerUtil.UrlEncode(api_response_params.get(ret)));
        signSrc.append(HandlerUtil.UrlEncode(api_response_params.get(jump)));
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[聚泽]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();
        if (StringUtils.isBlank(resultStr)) {
            log.error("[聚泽]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
            throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
        }
        JSONObject resJson = JSONObject.parseObject(resultStr);
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey("redirect") && StringUtils.isNotBlank(resJson.getString("redirect"))) {
            result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, resJson.getString("redirect"));
        }else {
            log.error("[聚泽]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[聚泽]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[聚泽]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}