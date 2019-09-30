package dc.pay.business.fenghuolunzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

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

/**
 * 
 * @author andrew
 * Aug 29, 2019
 */
@RequestPayHandler("FENGHUOLUNZHIFU")
public final class FengHuoLunZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(FengHuoLunZhiFuPayRequestHandler.class);

    //字段名 变量名 类型  最大长度    说明  可空  是否签名
    //商户号 merchantCode    String  32  商户号：平台提供给商户 N   Y
    private static final String merchantCode                ="merchantCode";
    //商户订单号   merchantOrderNo String  32  商户订单号：必传，订单唯一标示，作为订单结果通知的标识，可按照你们业务生成   N   Y
    private static final String merchantOrderNo                ="merchantOrderNo";
    //付款类型    payType int 2   付款类型（1-支付宝、2-微信）    N   Y
    private static final String payType                ="payType";
    //付款金额    payPrice    int     付款金额（整数，以分为单位）：payPrice，如1元传100 N   Y
    private static final String payPrice                ="payPrice";
    //商户名称    merchantName    String  32  商户名称：必传，作为付款码图片上的商户名称   N   Y
    private static final String merchantName                ="merchantName";
    //异步通知地址  callbackUrl String  255 异步通知地址：必传，作为订单结果通知地址，具体回调接口可参考回调示例  N   
    private static final String callbackUrl                ="callbackUrl";
    //商户备注    merchantRemark  String  255 商户备注：可为空，作为付款说明，回调时会传递过去        
    private static final String merchantRemark                ="merchantRemark";
    //签名  sign    String  32  签名  N   
//    private static final String sign                ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[风火轮支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
            throw new PayException("[风火轮支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merchantCode, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(merchantOrderNo,channelWrapper.getAPI_ORDER_ID());
//                put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(payType,channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(payPrice,  channelWrapper.getAPI_AMOUNT());
                put(merchantName,"name");
                put(callbackUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(merchantRemark, channelWrapper.getAPI_MEMBERID());
            }
        };
        log.debug("[风火轮支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(merchantName+"=").append(api_response_params.get(merchantName)).append("&");
        signSrc.append(merchantCode+"=").append(api_response_params.get(merchantCode)).append("&");
        signSrc.append(merchantOrderNo+"=").append(api_response_params.get(merchantOrderNo)).append("&");
        signSrc.append(payPrice+"=").append(api_response_params.get(payPrice)).append("&");
        signSrc.append(payType+"=").append(api_response_params.get(payType)).append("&");
        signSrc.append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[风火轮支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();

        if (false) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[风火轮支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[风火轮支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
//            System.out.println("请求返回=========>"+resultStr);
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //   log.error("[风火轮支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //   throw new PayException(resultStr);
            //}
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[风火轮支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != jsonObject && jsonObject.containsKey("result") && "0".equalsIgnoreCase(jsonObject.getString("result"))  && 
            (jsonObject.containsKey("datas") && StringUtils.isNotBlank(jsonObject.getString("datas")) || 
             jsonObject.getJSONObject("datas").containsKey("payPic") && StringUtils.isNotBlank(jsonObject.getJSONObject("datas").getString("payPic")))
            ){
//            if (null != jsonObject && jsonObject.containsKey("result") && "1".equalsIgnoreCase(jsonObject.getString("result"))  && jsonObject.containsKey("codeimg") && StringUtils.isNotBlank(jsonObject.getString("codeimg"))) {
                String code_url = jsonObject.getJSONObject("datas").getString("payPic");
                result.put( JUMPURL, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[风火轮支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[风火轮支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[风火轮支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}