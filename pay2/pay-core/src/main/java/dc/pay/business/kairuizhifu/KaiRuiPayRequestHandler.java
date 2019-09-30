package dc.pay.business.kairuizhifu;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("KAIRUIZHIFU")
public final class KaiRuiPayRequestHandler extends PayRequestHandler {
     private static final Logger log = LoggerFactory.getLogger(KaiRuiPayRequestHandler.class);
     private static final String     app_id  ="app_id";                //	string	是	商户id，申请后，由我们下发
     private static final String     amount  ="amount";                //	int	是	交易金额，单位分
     private static final String     order_no  ="order_no";            //	string	是	商户订单号，非重复6-128位数字
     private static final String     device  ="device";                //	string	是	参见下方表格
     private static final String     notify_url  ="notify_url";        //	string	是	通知地址,异步post通知,128以内
     private static final String     return_url  ="return_url";        //	string	是	通知地址,同步get 通知,128以内
     private static final String     sign  ="sign";                    //	string	是	签名，规则参照上述说明
     private static final String     merchant  ="merchant";            //	string	是	商户名称，将显示在收银台上
     private static final String     goods  ="goods";                  //	string	是	商品名称，将显示在收银台上





    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put( app_id, channelWrapper.getAPI_MEMBERID());
            payParam.put( amount,channelWrapper.getAPI_AMOUNT() );
            payParam.put( order_no,channelWrapper.getAPI_ORDER_ID() );
            payParam.put( device,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG() );
            payParam.put( notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );
            payParam.put( return_url, channelWrapper.getAPI_WEB_URL());
            payParam.put( merchant, channelWrapper.getAPI_ORDER_ID());
            payParam.put( goods,"PAY" );
        }

        log.debug("[凯瑞]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String paramsStr = String.format("app_id=%s&amount=%s&order_no=%s&device=%s&app_secret=%s&notify_url=%s",
                params.get(app_id),
                params.get(amount),
                params.get(order_no),
                params.get(device),
                channelWrapper.getAPI_KEY(),
                params.get(notify_url)
        );
        String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[凯瑞]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1==1 ||HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)   ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
                payResultList.add(result);
            }else{
                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET).trim();
                JSONObject jsonResultStr = JSON.parseObject(resultStr);
            if(1==1 ) {  //扫码（银联扫码&微信&支付宝&QQ）
                    if(null!=jsonResultStr && jsonResultStr.containsKey("code") && "200".equalsIgnoreCase(jsonResultStr.getString("code")) && jsonResultStr.containsKey("codeimg")){
                            if(StringUtils.isNotBlank(jsonResultStr.getString("codeimg"))){
                                result.put(QRCONTEXT, HandlerUtil.UrlDecode(jsonResultStr.getString("codeimg")));
                                payResultList.add(result);
                            }
                    }else {
                        throw new PayException(resultStr);
                    }

                } else if(1==2){//H5

                }else {
                    log.error("[凯瑞]3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(JSON.toJSONString(resultStr));
                }
            }
        } catch (Exception e) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
            payResultList.add(result);
            //log.error("[凯瑞]3.发送支付请求，及获取支付请求结果出错：", e);
            //throw new PayException(e.getMessage(), e);
        }
        log.debug("[凯瑞]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[凯瑞]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}