package dc.pay.business.shenmaozhifu;


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
 * @author cobby
 * Maay 18, 2019
 */
@RequestPayHandler("SHENMAOZHIFU")
public final class ShenMaoZhiFuPayRequestHandler extends PayRequestHandler {
     private static final Logger log = LoggerFactory.getLogger(ShenMaoZhiFuPayRequestHandler.class);

     private static final String     app_id      ="app_id";       //    string    是    商户id，申请后，由我们下发
     private static final String     amount      ="amount";       //    int       是    交易金额，单位分
     private static final String     order_no    ="order_no";     //    string    是    商户订单号，非重复6-128位数字
     private static final String     device      ="device";       //    string    是    参见下方表格
     private static final String     notify_url  ="notify_url";   //    string    是    通知地址,异步post通知,128以内
     private static final String     return_url  ="return_url";   //    string    是    通知地址,同步get 通知,128以内
     private static final String     api_type    ="api_type";     //    int             值1直接渲染支付视图，值2json返回

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){
            payParam.put( app_id, channelWrapper.getAPI_MEMBERID());
            payParam.put( amount,channelWrapper.getAPI_AMOUNT() );
            payParam.put( order_no,channelWrapper.getAPI_ORDER_ID() );
            payParam.put( device,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG() );
            payParam.put( notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );
            payParam.put( return_url, channelWrapper.getAPI_WEB_URL());
            payParam.put( api_type,"1" );
        }

        log.debug("[神猫支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> params) throws PayException {
//        app_id=xx&amount=xx&order_no=xx&device=xx&app_secret=xx&notify_url=xx
        String paramsStr = String.format("app_id=%s&amount=%s&order_no=%s&device=%s&app_secret=%s&notify_url=%s",
                params.get(app_id),
                params.get(amount),
                params.get(order_no),
                params.get(device),
                channelWrapper.getAPI_KEY(),
                params.get(notify_url)
        );
        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[神猫支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        try {

                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
                payResultList.add(result);

        } catch (Exception e) {
            log.error("[神猫支付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[神猫支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[神猫支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}