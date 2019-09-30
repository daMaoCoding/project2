package dc.pay.business.quantongyun;

/**
 * ************************
 * @author tony 3556239829
 */

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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("QUANTONGYUN")
public final class QuanTongYunPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(QuanTongYunPayRequestHandler.class);

     private static final String      p0_Cmd	  = "p0_Cmd";                         //业务类型  Buy
     private static final String      p1_MerId	  = "p1_MerId";                       //商户编号
     private static final String      p2_Order	  = "p2_Order";                       //商户订单号
     private static final String      p3_Amt	  = "p3_Amt";                         //支付金额  单位:元
     private static final String      p4_Cur	  = "p4_Cur";                         //交易币种  CNY
     private static final String      p5_Pid	  = "p5_Pid";                         //商品名称
     private static final String      p6_Pcat	  = "p6_Pcat";                        //商品种类
     private static final String      p7_Pdesc	  = "p7_Pdesc";                       //商品描述
     private static final String      p8_Url	  = "p8_Url";                         //商户接收支付成功数据的地址
     private static final String      pa_MP	  = "pa_MP";                              //商户扩展信息   原样返回
     private static final String      pd_FrpId	  = "pd_FrpId";                       //支付通道编码
     private static final String      pr_NeedResponse	  = "pr_NeedResponse";        //应答机制  1
     private static final String      hmac	  = "hmac";                               //签名数据
     private static final String      sign	  = "sign";                               //签名数据






    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(p0_Cmd,"Buy");
            payParam.put(p1_MerId,channelWrapper.getAPI_MEMBERID());
            payParam.put(p2_Order,channelWrapper.getAPI_ORDER_ID());
            payParam.put(p3_Amt,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(p4_Cur,"CNY");
            payParam.put(p5_Pid,"PAY");
            payParam.put(p6_Pcat,"PAY-C");
            payParam.put(p7_Pdesc,"PAY-D");
            payParam.put(p8_Url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(pa_MP,pa_MP);
            payParam.put(pd_FrpId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        }

        log.debug("[全通云付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(params.get(paramKeys.get(i))) || hmac.equalsIgnoreCase(paramKeys.get(i).toString() ) ||  sign.equalsIgnoreCase(paramKeys.get(i).toString())  )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i)));
            if(i < paramKeys.size()-1)sb.append("&");
        }
       // sb.append(channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = QuanTongYunDigestUtil.hmacSign(signStr, channelWrapper.getAPI_KEY());
        log.debug("[全通云付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (HandlerUtil.isFS(channelWrapper)|| HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||  HandlerUtil.isWapOrApp(channelWrapper) ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());//.replace("method='post'","method='get'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
            if(1==1 ) {  //扫码（银联扫码&微信&支付宝&QQ）
                    if(null!=jsonResultStr && jsonResultStr.containsKey("status") && "0".equalsIgnoreCase(jsonResultStr.getString("status")) && jsonResultStr.containsKey("payImg")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("payImg"))){
                                result.put(QRCONTEXT, HandlerUtil.UrlDecode(jsonResultStr.getString("payImg")));
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }

                } else if(1==2){//H5

                }else {
                    log.error("[全通云付]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(JSON.toJSONString(resultStr));
                }
            }
        } catch (Exception e) {
            log.error("[全通云付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[全通云付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[全通云付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}