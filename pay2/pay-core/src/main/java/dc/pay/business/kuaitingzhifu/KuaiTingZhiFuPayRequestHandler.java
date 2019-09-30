package dc.pay.business.kuaitingzhifu;

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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * May 30, 2019
 */
@RequestPayHandler("KUAITINGZHIFU")
public final class KuaiTingZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KuaiTingZhiFuPayRequestHandler.class);

    private static final String version                ="version";   //    string    是    接口版本号    1.1
    private static final String customerid             ="customerid";//    string    是    商户ID
    private static final String sdorderno              ="sdorderno"; //    string    是    商户订单号
    private static final String total_fee              ="total_fee"; //    string    是    下单金额 两位小数
    private static final String paytype                ="paytype";   //    string    是    支付通道 后台查看 默认alipay所有用户开通    alipay
    private static final String notifyurl              ="notifyurl"; //    string    是    支付成功后异步回调地址(请不要带任何参数)
    private static final String returnurl              ="returnurl"; //    string    是    form表单模式下支付后跳转的地址(无参数)
//  private static final String bankcode               ="bankcode";  //    string    否    银行编号 保留字段可不填
    private static final String remark                 ="remark";    //    string    是    订单备注
    private static final String rettype                ="rettype";   //    string    是    返回类型 建议为form    form

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version,"1.1");
                put(customerid, channelWrapper.getAPI_MEMBERID());
                put(sdorderno,channelWrapper.getAPI_ORDER_ID());
                put(total_fee,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnurl,channelWrapper.getAPI_WEB_URL());
                put(remark,channelWrapper.getAPI_ORDER_ID());
                put(rettype,"form");
            }
        };
        log.debug("[快艇支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if ( StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
//        signSrc.deleteCharAt(signSrc.length()-1);
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[快艇支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {

//                String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//                JSONObject jsonObject;
//                try {
//                    jsonObject = JSONObject.parseObject(resultStr);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    log.error("[快艇支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    throw new PayException(resultStr);
//                }
//                if (null != jsonObject && jsonObject.containsKey("status") && "Success".equalsIgnoreCase(jsonObject.getString("status"))
//                        && jsonObject.containsKey("qr_code") && StringUtils.isNotBlank(jsonObject.getString("qr_code"))) {
//                    String code_url = jsonObject.getString("qr_code");
//                    if (handlerUtil.isWapOrApp(channelWrapper)) {
//                        result.put(JUMPURL, code_url);
//                    }else{
//                        result.put(QRCONTEXT, code_url);
//                    }
//                }else {
//                    log.error("[快艇支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    throw new PayException(resultStr);
//                }

            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        } catch (Exception e) {
            log.error("[快艇支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[快艇支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[快艇支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}