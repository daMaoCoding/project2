package dc.pay.business.elianfuzhifu;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Cobby
 * May 13, 2019
 */
@RequestPayHandler("ELIANFUZHIFU")
public final class ELianFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ELianFuZhiFuPayRequestHandler.class);

      private static  final    String   parter = "parter";       //     商户
      private static  final    String   type = "type";       //     银行类型
      private static  final    String   value = "value";       //       金额单位元（人民币），2位小数，最小支    付金额为  0.02
      private static  final    String   orderid = "orderid";       //     商户订单号
      private static  final    String   callbackurl = "callbackurl";       //     下行异步通知



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(parter,channelWrapper.getAPI_MEMBERID());
            payParam.put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(value,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(orderid,channelWrapper.getAPI_ORDER_ID());
            payParam.put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }

        log.debug("[E联付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String paramsStr = String.format("parter=%s&type=%s&value=%s&orderid=%s&callbackurl=%s%s",
                params.get(parter),
                params.get(type),
                params.get(value),
                params.get(orderid),
                params.get(callbackurl),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[E联付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        try {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        } catch (Exception e) {
            log.error("[E联付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        payResultList.add(result);
        log.debug("[E联付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[E联付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}