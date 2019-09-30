package dc.pay.business.ninegzhifu;

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
@RequestPayHandler("NINEGZHIFU")
public final class NineGZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(NineGZhiFuPayRequestHandler.class);

    private static final String app_id            ="app_id";     //必填 商户appid
    private static final String order_no          ="order_no";   //必填 商户订单，原样回传给商户
    private static final String pay_amt           ="pay_amt";    //必填 ⽀付⾦额
    private static final String pay_cur           ="pay_cur";    //必填 （固定参数：CNY）⽀付货币
    private static final String goods_name        ="goods_name"; //必填 商品名字
    private static final String goods_num         ="goods_num";  //必填 商品数量
    private static final String goods_cat         ="goods_cat";  //必填 商品种类
    private static final String goods_desc        ="goods_desc"; //必填 商品描述
    private static final String return_url        ="return_url"; //必填 同步通知url
    private static final String notify_url        ="notify_url"; //必填 异步通知url
    private static final String user_id           ="user_id";    //必填 不参与签名 ⽤户在平台⾥的id，⽤于识别⽤户优化⻛控，可md5加密后再传过来，但必须确保同⼀个⽀付⽤户传过来的值⼀样
    private static final String username          ="username";   //必填 不参与签名 ⽤户在平台的⽤户名，⽤于调整⻛控和处理投诉
    private static final String pay_type          ="pay_type";   //必填 （值⻅附录）⽀付类型
//    private static final String state             ="state";      //选填 （原样回传给商户）商户⾃定义信息
    private static final String ts                ="ts";         //必填 unix格式时间戳


    @Override
    protected Map<String, String> buildPayParam() throws PayException {

        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(app_id, channelWrapper.getAPI_MEMBERID());
                put(order_no, channelWrapper.getAPI_ORDER_ID());
                put(pay_amt, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()) );
                put(pay_cur,"CNY");
                put(goods_name,channelWrapper.getAPI_ORDER_ID());
                put(goods_num,"1");
                put(goods_cat,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(goods_desc,channelWrapper.getAPI_ORDER_ID());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(user_id,HandlerUtil.getRandomStr(10));
                put(username,HandlerUtil.getRandomStr(10));
                put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(ts,String.valueOf(System.currentTimeMillis()));
            }
        };
        log.debug("[9G支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> params) throws PayException {
//        app_id=80000001notify_url=http://www.google.comorder_no=DR00010021pay_amt=100.00pay_cur=CNY
//        pay_type=alipay_h5return_url=http://www.google.com1487875053342141234ac23de93ba
        String paramsStr = String.format("app_id=%snotify_url=%sorder_no=%spay_amt=%spay_cur=%spay_type=%sreturn_url=%s%s%s",
                params.get(app_id),
                params.get(notify_url),
                params.get(order_no),
                params.get(pay_amt),
                params.get(pay_cur),
                params.get(pay_type),
                params.get(return_url),
                params.get(ts),
                channelWrapper.getAPI_KEY());
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[9G支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        try {

                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());

        } catch (Exception e) {
            log.error("[9G支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[9G支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[9G支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}