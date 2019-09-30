package dc.pay.business.dorazhifu;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * ************************
 * @author tony 3556239829
 */
@Slf4j
@RequestPayHandler("DORAZHIFU")
public final class DoraZhiFuPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);


     private static final String     timestamp = "timestamp";      // 时间戳
     private static final String     terminal = "terminal";      // 终端类型  3
     private static final String     api_version = "api_version";      //  Api版本号  1.6(固定)
     private static final String     notify_url = "notify_url";      //  回调地址
     private static final String     company_id = "company_id";      //  商户id
     private static final String     player_id = "player_id";      //  玩家id
     private static final String     company_order_id = "company_order_id";      //  商户订单号
     private static final String     amount_money = "amount_money";      //  充值金额
     private static final String     channel_code = "channel_code";      // 渠道编码
     private static final String     sign = "sign";      // 签名



    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(timestamp,System.currentTimeMillis()+"");
            payParam.put(terminal,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
            payParam.put(api_version,"1.6");
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(company_id,channelWrapper.getAPI_MEMBERID());
            payParam.put(player_id,HandlerUtil.getRandomNumber(10));
            payParam.put(company_order_id, channelWrapper.getAPI_ORDER_ID());
            payParam.put(amount_money, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(channel_code, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
        }

        log.debug("[Dora支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
              if(StringUtils.isBlank(params.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("api_Key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("[Dora支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map result = Maps.newHashMap();
        if (1==1 && HandlerUtil.isWY(channelWrapper) &&  HandlerUtil.isYLKJ(channelWrapper)  &&  HandlerUtil.isWapOrApp(channelWrapper)    ) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
        }else{
            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
            if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                result.put(HTMLCONTEXT,resultStr);
            }else if(StringUtils.isNotBlank(resultStr) ){
                try {
                    resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
                if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "200".equalsIgnoreCase(jsonResultStr.getString("code"))
                        && jsonResultStr.containsKey("data") && null!=jsonResultStr.getJSONObject("data")
                        && jsonResultStr.getJSONObject("data").containsKey("scalper_url")
                        && StringUtils.isNotBlank(jsonResultStr.getJSONObject("data").getString("scalper_url") )  ){
//                    if(HandlerUtil.isWapOrApp(channelWrapper)){
//                        result.put(JUMPURL, jsonResultStr.getJSONObject("data").getString("scalper_url") );
//                    }else{
//                        result.put(QRCONTEXT, jsonResultStr.getJSONObject("data").getString("scalper_url") );
//                    }
                    result.put(JUMPURL, jsonResultStr.getJSONObject("data").getString("scalper_url") );
                }else {throw new PayException(resultStr); }
            }else{ throw new PayException(EMPTYRESPONSE);}
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[Dora支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[Dora支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}