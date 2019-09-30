package dc.pay.business.hongshengzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author andrew
 * Aug 27, 2019
 */
@RequestPayHandler("HONGSHENGZHIFU")
public final class HongShengZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HongShengZhiFuPayRequestHandler.class);
    
    //请求参数
    //序号  参数  字段名称    必选  说明
    //１   mer_id  商户号 是   商户唯一ID ，请求接口、返回接口，异步返回接口、查询接口共用此参数。
    private static final String mer_id                ="mer_id";
    //2   timestamp   订单时间    是   时间格式yyyy-MM-dd HH:mm:ss
    private static final String timestamp                ="timestamp";
    //3   terminal    支付类型    是   详见支付类型表
    private static final String terminal                ="terminal";
    //4   version 版本号 是   01
    private static final String version                ="version";
    //5   amount  金额  是   交易金额(单位分)
    private static final String amount                ="amount";
    //6   backurl 同步返回的url    是   支付成功的同步返回的url
    private static final String backurl                ="backurl";
    //7   failUrl 同步返回的url    是   支付失败的同步返回的url
    private static final String failUrl                ="failUrl";
    //8   ServerUrl   异步返回的url    是   以form-data形式将返回数据post提交到该url
    private static final String ServerUrl                ="ServerUrl";
    //9   businessnumber  订单号 是   商品订单号以[‘A~Z,a~z,0~9’]组成的[10,64]位字符串，不能重复
    private static final String businessnumber                ="businessnumber";
    //10  goodsName   商品名称    是   商品名称或描述
    private static final String goodsName                ="goodsName";
    //11  IsCredit    是否直连    否   直连必填(0:非直联 1：直联)
//    private static final String IsCredit                ="IsCredit";
    //12  BankCode    银行编码    否   直连必填，详见银行编码表
//    private static final String BankCode                ="BankCode";
    //13  ProductType 网银类型    否   直连必填(1=个人网银, 2=企业网银)
    private static final String ProductType                ="ProductType";
    //14  user_id 用户id    否   付款人唯一id(影响支付成功率，支付通道路由轮寻时必填)
    private static final String user_id                ="user_id";
    //19  sign    签名  是   签名，详见
//    private static final String sign                ="sign";
    //20  sign_type   签名算法类型  是   默认md5
    private static final String sign_type                ="sign_type";
    
//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";
    
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[鸿昇支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
            throw new PayException("[鸿昇支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mer_id, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(timestamp,  DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"));
//                put(terminal,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(terminal,channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(version,"01");
                put(amount,  channelWrapper.getAPI_AMOUNT());
                put(backurl,  channelWrapper.getAPI_WEB_URL());
                put(failUrl,  channelWrapper.getAPI_WEB_URL());
                put(ServerUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(businessnumber,channelWrapper.getAPI_ORDER_ID());
                put(goodsName,"name");
//                put(IsCredit,"1");
                put(ProductType,"1");
                put(user_id,HongShengZhiFuPayRequestHandler.this.handlerUtil.getRandomNumber(8));
                put(sign_type,"md5");
            }
        };
        log.debug("[鸿昇支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }
    
    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!sign_type.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[鸿昇支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }
    
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();

        if (false) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[鸿昇支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[鸿昇支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
            System.out.println("请求返回=========>"+resultStr);
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //   log.error("[鸿昇支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //   throw new PayException(resultStr);
            //}
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            JSONObject jsonObject;
            try {
                jsonObject = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[鸿昇支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != jsonObject && jsonObject.containsKey("code") && "MSG_OK".equalsIgnoreCase(jsonObject.getString("code"))  && 
            (jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data")) && 
             jsonObject.getJSONObject("data").containsKey("trade_qrcode") && StringUtils.isNotBlank(jsonObject.getJSONObject("data").getString("trade_qrcode")))
            ){
//            if (null != jsonObject && jsonObject.containsKey("code") && "MSG_OK".equalsIgnoreCase(jsonObject.getString("code"))  && jsonObject.containsKey("codeimg") && StringUtils.isNotBlank(jsonObject.getString("codeimg"))) {
                String code_url = jsonObject.getJSONObject("data").getString("trade_qrcode");
                result.put( JUMPURL, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[鸿昇支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[鸿昇支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[鸿昇支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}
