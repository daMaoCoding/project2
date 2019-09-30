package dc.pay.business.epayzhifu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ************************
 * @author tony 3556239829
 */

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

@RequestPayHandler("EPAYZHIFU")
public final class EPayZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(EPayZhiFuPayRequestHandler.class);



     private static final String     command = "command";  //	是	32	接口编码
     private static final String     serverCode = "serverCode";  //	是	32	服务编码
     private static final String     merchNo = "merchNo";  //	是	32	平台分配商户号12位长度 例如：666611000235
     private static final String     version = "version";  //	是	32	接口版本号，默认2.0
     private static final String     charset = "charset";  //	是	32	编码格式，默认utf-8
     private static final String     currency = "currency";  //	是	32	币种 默认:CNY 港币:HKD
     private static final String     reqIp = "reqIp";  //	是	32	请求方对外公网IP 例如：192.168.0.14
     private static final String     reqTime = "reqTime";  //	是	32	请求时间戳，格式yyyymmddhhmmss 例如：20180505120101
     private static final String     signType = "signType";  //	是	32	签名算法类型，目前仅支持MD5加密
     private static final String     sign = "sign";  //	是	350	加密签名值目前支持请求参数键名&拼接键值&拼接平台方指定的秘钥进行MD5加密，将所有参数按照参数名ASCII码从小到大排序用&拼接&最后拼接key 例如：MD5(abc=value1&bad=value3&bcd=value2&key=商户秘钥)不能有空值，参数区分大小写
     private static final String     payType = "payType";  //	是	32	支付类型
     private static final String     cOrderNo = "cOrderNo";  //	是	12-32	商户订单号
     private static final String     amount = "amount";  //	是	32	交易金额 取值范围0.01到10000000.00默认单位元
     private static final String     goodsName = "goodsName";  //	是	32	商品名称
     private static final String     goodsNum = "goodsNum";  //	是	32	产品数量
     private static final String     goodsDesc = "goodsDesc";  //	是	32	商品描述
     private static final String     memberId = "memberId";  //	是	32	商户生成的用户id
    // private static final String     returnUrl = "returnUrl";  //	否	256	返回的商户处理url
     private static final String     notifyUrl = "notifyUrl";  //	是	256	主动通知商户商户url


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(command,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
            payParam.put(serverCode,"ser2001");
            payParam.put(merchNo,channelWrapper.getAPI_MEMBERID());
            payParam.put(version,"2.0");
            payParam.put(charset,"utf-8");
            payParam.put(currency,"CNY");
            payParam.put(reqIp,channelWrapper.getAPI_Client_IP());
            payParam.put(reqTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(signType,"MD5");
            payParam.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
            payParam.put(cOrderNo,channelWrapper.getAPI_ORDER_ID());
            payParam.put(amount,HandlerUtil.getYuanWithoutZero(channelWrapper.getAPI_AMOUNT()));
            payParam.put(goodsName,channelWrapper.getAPI_ORDER_ID());
            payParam.put(goodsNum,"1");
            payParam.put(goodsDesc,channelWrapper.getAPI_ORDER_ID());
            payParam.put(memberId,System.currentTimeMillis()+"");
           // payParam.put(returnUrl,channelWrapper.getAPI_WEB_URL());
            payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        }
        log.debug("[epay支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString()    ))  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[epay支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map result = Maps.newHashMap();
        if (handlerUtil.isWapOrApp(channelWrapper) ||HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWebYlKjzf(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[epay支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[epay支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}