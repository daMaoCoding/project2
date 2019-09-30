package dc.pay.business.lianzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * 
 * @author andrew
 * May 23, 2019
 */
@RequestPayHandler("LIANZHIFU")
public final class LianZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LianZhiFuPayRequestHandler.class);

    //字段名 说明  类型  描述  是否必填    是否参与签名
    //model   模块名 String  传入固定值: H5_PAY   是   
    private static final String model                ="model";
    //merchantCode    商户号 String  平台分配的商户号    可以在商户服务平台-商户管理-商户信息页面中查看商户号 长度4~20位 是   是
    private static final String merchantCode                ="merchantCode";
    //outOrderId  商户订单号   String  商户系统唯一的订单编号，只能为数字或字母，长度5~30位    是   是
    private static final String outOrderId                ="outOrderId";
    //amount  支付金额    Long    单位分    只能为正整数，最小为1000  是   是
    private static final String amount                ="amount";
    //goodsName   商品名称    String  最大20个字符     
//    private static final String goodsName                ="goodsName";
    //goodsExplain    商品描述    String  最大50个字符     
//    private static final String goodsExplain                ="goodsExplain";
    //ext 扩展字段    String  商户任意输入，将在异步通知中原样返回    最大60个字符    
//    private static final String ext                ="ext";
    //orderCreateTime 商户订单创建时间    String  格式为yyyyMMddHHmmss    比如：20160405093921   是   是
    private static final String orderCreateTime                ="orderCreateTime";
    //noticeUrl   通知商户服务端地址   String  异步通知地址，请填写正确的地址以防止收不到通知 是   是
    private static final String noticeUrl                ="noticeUrl";
    //goodsMark   商品标记    String          
//    private static final String goodsMark                ="goodsMark";
    //payChannel  渠道编码    String  21-微信扫码    30-支付宝扫码    31-QQ钱包扫码    32-银联扫码    33-京东扫码    241-微信H5    242-支付宝H5    243-QQ钱H5    244-京东H5    245-网银快捷    246-网银    247-支付宝扫码原生    248-支付宝原生    249-微信小程序   288-微信原生扫码支付    266-微信Wap支付 是   是
    private static final String payChannel                ="payChannel";
    //bankCode    银行编码    String  请参考4.3银行编码      
    private static final String bankCode                ="bankCode";
    //phone   手机号码    String          
//    private static final String phone                ="phone";
    //accountNo   身份证 String          
//    private static final String accountNo                ="accountNo";
    //ip  ip地址    String  app和网页支付提交用户端ip 是   
    private static final String ip                ="ip";
    //sign    签名  String  详情见签名机制 是   
//    private static final String sign                ="sign";

    private static final String key        ="KEY";
    //signature    数据签名    32    是    　
//    private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if ((null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) && !handlerUtil.isWY(channelWrapper)) {
            log.error("[链支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
            throw new PayException("[链支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
//                put(model,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(model,"H5_PAY");
                put(merchantCode, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(outOrderId,channelWrapper.getAPI_ORDER_ID());
                put(amount,  channelWrapper.getAPI_AMOUNT());
                put(orderCreateTime,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(noticeUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                if (handlerUtil.isWY(channelWrapper)) {
                    put(payChannel,"246");
                    put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                }else {
                    put(payChannel,channelWrapper.getAPI_MEMBERID().split("&")[1]);
                }
                put(ip,channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[链支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!bankCode.equals(paramKeys.get(i)) && !model.equals(paramKeys.get(i)) && !ip.equals(paramKeys.get(i))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key +"="+ channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = handlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[链支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
//        if (handlerUtil.isWY(channelWrapper) || handlerUtil.isWapOrApp(channelWrapper)) {
        if (true) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
        
//        else{
////            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//            //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            //if (StringUtils.isBlank(resultStr)) {
//            //    log.error("[链支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//            //    throw new PayException(resultStr);
//            //    //log.error("[链支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
//            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
//            //}
////            System.out.println("请求返回=========>"+resultStr);
//            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
//            //   log.error("[链支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//            //   throw new PayException(resultStr);
//            //}
//            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
//            JSONObject jsonObject;
//            try {
//                jsonObject = JSONObject.parseObject(resultStr);
//            } catch (Exception e) {
//                e.printStackTrace();
//                log.error("[链支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            //只取正确的值，其他情况抛出异常
//            if (null != jsonObject && jsonObject.containsKey("code") && "00".equalsIgnoreCase(jsonObject.getString("code"))
//                    && jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))
//                    && jsonObject.getJSONObject("data").containsKey("url") && StringUtils.isNotBlank(jsonObject.getJSONObject("data").getString("url"))
//            
//            ){
////            if (null != jsonObject && jsonObject.containsKey("code") && "0000".equalsIgnoreCase(jsonObject.getString("code"))
////                    && jsonObject.containsKey("success") && "true".equalsIgnoreCase(jsonObject.getString("success"))
////                    && jsonObject.containsKey("qrCode") && StringUtils.isNotBlank(jsonObject.getString("qrCode"))) {
//                String code_url = jsonObject.getJSONObject("data").getString("url");
//                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
//                //if (handlerUtil.isWapOrApp(channelWrapper)) {
//                //    result.put(JUMPURL, code_url);
//                //}else{
//                //    result.put(QRCONTEXT, code_url);
//                //}
//            }else {
//                log.error("[链支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[链支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[链支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}