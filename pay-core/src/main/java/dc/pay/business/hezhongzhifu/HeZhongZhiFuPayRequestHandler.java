package dc.pay.business.hezhongzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.http.HttpMethod;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.Sha1Util;
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author andrew
 * Jul 22, 2019
 */
@RequestPayHandler("HEZHONGZHIFU")
public final class HeZhongZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HeZhongZhiFuPayRequestHandler.class);

    //参数名称    中文名 数据类型&长度 说明  是否必填
    //v_pagecode  协议包编码   String (4)  1009  pc端支付宝    1080  支付宝H5    1081  银联扫码  是
    private static final String v_pagecode                ="v_pagecode";
    //v_mid       商户编号    String (50)     初始单上所填商户编号为准    是
    private static final String v_mid                ="v_mid";
    //v_oid   商户订单编号        String (64)     该参数格式为：订单生成    日期-商户编号-商户流水号。例如：20100101-888-12345。商流水号为数字，每日内不可重复，并且不能包括除数字、英文字母和“-”外以其它字符。流水号可为一组也可以用“-”间隔成几组。禁止订单号中出现：or ,and ,update,delete,select,create,这种字样的字符串。    是
    private static final String v_oid                ="v_oid";
    //v_rcvname       收货人姓名   String(64)  考虑到系统编码可能不统一的问题，建议统一用商户编号的值代替。  是
    private static final String v_rcvname                ="v_rcvname";
    //v_rcvaddr       收货人地址   String (128)    建议使用商户编号的值代替。   是
    private static final String v_rcvaddr                ="v_rcvaddr";
    //v_rcvtel        收货人电话   String (32) 建议使用商户编号的值代替。   是
    private static final String v_rcvtel                ="v_rcvtel";
    //v_goodsname 商品名称    String (100)        是
    private static final String v_goodsname                ="v_goodsname";
    //v_goodsdescription  交易商品描述  String (256)        是
    private static final String v_goodsdescription                ="v_goodsdescription";
    //v_rcvpost   收货人邮政编码 String (10) 建议使用商户编号的值代替。   是
    private static final String v_rcvpost                ="v_rcvpost";
    //v_qq    收货人QQ   String (32)     是
    private static final String v_qq                ="v_qq";
    //v_amount        订单总金额   String (16) 单位：元，小数点后保留    两位，如13.45。  是
    private static final String v_amount                ="v_amount";
    //v_ymd   订单产生日期  String (8)  格式为yyyymmdd，例如：20100101 是
    private static final String v_ymd                ="v_ymd";
    //v_orderstatus   商户配货状态  String (1)  0为未配齐，1为已配齐；一般商户该参数无实际意义，建议统一配置为1（已配齐）  是
    private static final String v_orderstatus                ="v_orderstatus";
    //v_ordername     订货人姓名   String (64)     建议统一用商户编号的值代替。  是
    private static final String v_ordername                ="v_ordername";
    //v_bankno    银行编号    String (64)     银行编号     非网银为：0000   是
    private static final String v_bankno                ="v_bankno";
    //v_moneytype     支付币种    String (2)  0为人民币，1为美元，2为欧元，3为英镑，4为日元，5为韩元，6为澳大利亚元，7为卢布，8为瑞士法郎，9为港币，10为新加坡元，11为澳门元。 是
    private static final String v_moneytype                ="v_moneytype";
    //v_url   异步通知回调地址        String (200)    在此地址放置接收程序用于接收平台返回的支付确认消息，URL参数是以http://或https://开头的完整URL地址。    是
    private static final String v_url                ="v_url";
    //v_noticeurl     同步返回商户页面地址        String (200)    为消费者完成购物后返回的商户页面，此地址为页面连接方式的返回地址，URL参数是以http://或https://开头的完整URL地址。 是
    private static final String v_noticeurl                ="v_noticeurl";
    //v_app   接入方式    String  固定值： 值为"app",表示app接入； 值为web，表示web接入    Web返回二维码页面    App返回二维码地址（需要用二维码生成器生成二维码）  
    private static final String v_app                ="v_app";
    //v_alipayid  支付宝真实姓名 String (64) 交易类型为支付宝2和手机支付宝2时必填 否
    private static final String v_alipayid                ="v_alipayid";
    //v_alipayaccount 支付宝帐号   String (64) 交易类型为支付宝2和手机支付宝2时必填 否
    private static final String v_alipayaccount                ="v_alipayaccount";
    //v_os    支付手机系统增加ios设备的成功率   String (64) Android 、 IOS   否
//    private static final String v_os                ="v_os";
    //v_sign  验签字段（SHA1加密）    String(200) v_pagecode=协议包编码&v_mid=商户编号&v_oid=商户订单编号&v_amount=订单总金额&v_ymd=订单日期&v_bankno=银行编号（直接接上）商户SHA密钥   (加密串需要转换成大写)
//    private static final String v_sign                ="v_sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="v_sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
//        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
//        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 3) {
//            log.error("[合众支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//            throw new PayException("[合众支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(v_pagecode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(v_mid, channelWrapper.getAPI_MEMBERID());
                put(v_oid,channelWrapper.getAPI_ORDER_ID());
                put(v_rcvname, channelWrapper.getAPI_MEMBERID());
                put(v_rcvaddr, channelWrapper.getAPI_MEMBERID());
                put(v_rcvtel, channelWrapper.getAPI_MEMBERID());
                put(v_goodsname,"name");
                put(v_goodsdescription,"name");
                put(v_rcvpost, channelWrapper.getAPI_MEMBERID());
                put(v_qq,  HandlerUtil.getRandomNumber(8));
                put(v_amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(v_ymd,  DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
                //v_orderstatus   商户配货状态  String (1)  0为未配齐，1为已配齐；一般商户该参数无实际意义，建议统一配置为1（已配齐）  是
                put(v_orderstatus,"1");
                put(v_ordername, channelWrapper.getAPI_MEMBERID());
                //v_bankno    银行编号    String (64)     银行编号     非网银为：0000   是
                put(v_bankno, !HeZhongZhiFuPayRequestHandler.this.handlerUtil.isWY(channelWrapper) ? "0000" : "");
                put(v_moneytype,"0");
                put(v_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(v_noticeurl,channelWrapper.getAPI_WEB_URL());
                //v_app   接入方式    String  固定值： 值为"app",表示app接入； 值为web，表示web接入    Web返回二维码页面    App返回二维码地址（需要用二维码生成器生成二维码）
//                put(v_app,"app");
                put(v_app,"web");
                put(v_alipayid,  HandlerUtil.getRandomNumber(8));
                put(v_alipayaccount,  HandlerUtil.getRandomNumber(8));
                
            }
        };
        log.debug("[合众支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(v_pagecode+"=").append(api_response_params.get(v_pagecode)).append("&");
        signSrc.append(v_mid+"=").append(api_response_params.get(v_mid)).append("&");
        signSrc.append(v_oid+"=").append(api_response_params.get(v_oid)).append("&");
        signSrc.append(v_amount+"=").append(api_response_params.get(v_amount)).append("&");
        signSrc.append(v_ymd+"=").append(api_response_params.get(v_ymd)).append("&");
        signSrc.append(v_bankno+"=").append(api_response_params.get(v_bankno));
        signSrc.append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = Sha1Util.getSha1(paramsStr).toUpperCase();
        log.debug("[合众支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }
    
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();

//        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
        if (false) {
//            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.GET);
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[合众支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[合众支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
//            System.out.println("请求返回=========>"+resultStr);
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //   log.error("[合众支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //   throw new PayException(resultStr);
            //}
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            JSONArray parseArray;
            try {
//                jsonObject = JSONObject.parseObject(resultStr);
                parseArray = JSONObject.parseArray(resultStr);
//                Object parse = JSONObject.parse(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[合众支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            int size = parseArray.size();
            if (1 != size) {
                log.error("[合众支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
            //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
            // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
            //){
            JSONObject jsonObject = parseArray.getJSONObject(0);
            if (null != jsonObject && jsonObject.containsKey("returnMsg") && StringUtils.isNotBlank(jsonObject.getString("returnMsg"))) {
                String code_url = jsonObject.getString("returnMsg");
                result.put( JUMPURL, code_url);
                //if (handlerUtil.isWapOrApp(channelWrapper)) {
                //    result.put(JUMPURL, code_url);
                //}else{
                //    result.put(QRCONTEXT, code_url);
                //}
            }else {
                log.error("[合众支付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[合众支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[合众支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}