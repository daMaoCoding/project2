package dc.pay.business.youjiu;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Cobby
 * Jan 10, 2019
 */
@RequestPayHandler("YOUJIU")
public final class YouJiuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YouJiuPayRequestHandler.class);

    private static final String pay_memberid            ="pay_memberid";     // 商户号      是    是 平台分配商户号
    private static final String pay_orderid             ="pay_orderid";      // 订单号      是    是 上送订单号唯一, 字符长度20
    private static final String pay_applydate           ="pay_applydate";    // 提交时间     是    是 时间格式：2016-12-26 18:18:18
    private static final String pay_bankcode            ="pay_bankcode";     // 银行编码     是    是 参考后续说明
    private static final String pay_callbackurl         ="pay_callbackurl";  // 服务端通知   是    是 服务端返回地址.（POST返回数据）
    private static final String pay_notifyurl           ="pay_notifyurl";    // 页面跳转通知  是    是 页面跳转返回地址（POST返回数据）
    private static final String pay_amount              ="pay_amount";       // 订单金额     是    是 商品金额
    private static final String pay_md5sign             ="pay_md5sign";      // MD5签名     是     否 请看MD5签名字段格式
//  private static final String pay_attach              ="pay_attach";       // 附加字段     否    否 此字段在返回时按原样返回 (中文需要url编码)
    private static final String pay_productname         ="pay_productname";  // 商品名称     是    否
//  private static final String pay_productnum          ="pay_productnum";   // 商品数量     否    否
//  private static final String pay_productdesc         ="pay_productdesc";  // 商品描述     否    否
//  private static final String pay_producturl          ="pay_producturl";   // 商户链接     否    否
    private static final String key                     ="key";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[游久]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&道类型编码值（向第三方获取当前使用通道编码值）" );
            throw new PayException("[游久]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&道类型编码值（向第三方获取当前使用通道编码值）" );
        }
        
        Map<String, String> payParam = Maps.newHashMap();
        payParam.put(pay_memberid,channelWrapper.getAPI_MEMBERID().split("&")[0]);
        payParam.put(pay_orderid,channelWrapper.getAPI_ORDER_ID());
        payParam.put(pay_amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
        payParam.put(pay_applydate,DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString));
//        payParam.put(pay_bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        payParam.put(pay_bankcode,channelWrapper.getAPI_MEMBERID().split("&")[1]);
        payParam.put(pay_notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        payParam.put(pay_callbackurl,channelWrapper.getAPI_WEB_URL());
        payParam.put(pay_productname,channelWrapper.getAPI_ORDER_ID());
        log.debug("[游久]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {

         String pay_md5signA = null;
         List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
         StringBuilder sb = new StringBuilder();
         for (int i = 0; i < paramKeys.size(); i++) {
             if(StringUtils.isBlank(api_response_params.get(paramKeys.get(i))) || pay_productname.equalsIgnoreCase(paramKeys.get(i).toString())  ||pay_productname.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                 continue;
             sb.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
         }
         sb.append(key + "=" + channelWrapper.getAPI_KEY());
         String signStr = sb.toString();
         pay_md5signA = HandlerUtil.getMD5UpperCase(signStr);
         log.debug("[游久]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5signA));
         return pay_md5signA;

    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String md5sign) throws PayException {
        payParam.put(pay_md5sign,md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        try {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());

        } catch (Exception e) {
            log.error("[游久]-[请求支付]-3.5.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[游久]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[游久]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}