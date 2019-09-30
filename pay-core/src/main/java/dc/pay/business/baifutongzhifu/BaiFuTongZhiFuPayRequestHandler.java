package dc.pay.business.baifutongzhifu;

import java.util.*;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;

/**
 * @author cobby
 * Jan 25, 2019
 */
@RequestPayHandler("BAIFUTONGZHIFU")
public final class BaiFuTongZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BaiFuTongZhiFuPayRequestHandler.class);

    private static final String money                ="money";         //money    long    是    订单金额,单位分
    private static final String return_url           ="return_url";    //return_url    String    否    用户支付完成跳转的页面
    private static final String group_id             ="group_id";      //group_id    String    是    接口使用者ID 由系统开发商提供
    private static final String notify_url           ="notify_url";    //notify_url    String    是    用于接收支付成功消息的地址
    private static final String pay_code             ="pay_code";      //pay_code    String    是    支付方式:alipay支付宝,wechat微信支付,必传其中一个值。 选择微信支付返回的支付链接只能生成二维码让用户使用手机扫码,而不能直接唤醒微信APP
    private static final String user_order_sn        ="user_order_sn"; //user_order_sn    String    是    订单号 本系统存在订单号重复判断，请勿提交重复单号
    private static final String subject              ="subject";       //subject    String    是    订单标题
    private static final String pay_from             ="pay_from";      //pay_from    String    否    电脑网站支付还是手机网页支付;值为WEB或WAP选其一;不传默认为WAP
    private static final String ip                   ="ip";            //ip    String    是    玩家IP地址或玩家唯一标识ID

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(money,  channelWrapper.getAPI_AMOUNT());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(group_id, channelWrapper.getAPI_MEMBERID());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(pay_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(user_order_sn,channelWrapper.getAPI_ORDER_ID());
                put(subject,channelWrapper.getAPI_ORDER_ID());
                put(pay_from,channelWrapper.getAPI_ORDER_FROM());
                put(ip,channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[百福通支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> params) throws PayException {
//      sign=md5(group_id的值+user_order_sn的值+money的值+notify_url的值+pay_code的值+商户秘钥),注意顺序,md5后小写
         String paramsStr = String.format("%s%s%s%s%s%s",
                params.get(group_id),
                params.get(user_order_sn),
                params.get(money),
                params.get(notify_url),
                params.get(pay_code),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[百福通支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {

            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
            }
        } catch (Exception e) {
            log.error("[百福通支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[百福通支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[百福通支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}