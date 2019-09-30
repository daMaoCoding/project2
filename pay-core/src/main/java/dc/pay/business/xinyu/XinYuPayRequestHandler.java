package dc.pay.business.xinyu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dc.pay.utils.XmlUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.ChannelWrapper;
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
import dc.pay.utils.qr.QRCodeUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Aug 3, 2018
 */
@RequestPayHandler("XINYU")
public final class XinYuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinYuPayRequestHandler.class);

    //接口登录
    private static final String tokenUrl                      ="http://api.xinyuzhifu.com/auth/access-token";
    //GET 请求
    //字段名        变量名                必填            类型            说明
    //应用ID        appid                  是            String            应用ID与商户ID一致
    //密码          secretid               是            String            接口密钥
    //返回结果
    //数据按XML的格式实时返回
    //字段名        变量名                必填            类型            说明
    //token         token                  是            String(256)       登录令牌
    //有效期        token_expir_second     是            String(32)        token有效期，单位：s
    private static final String appid                       ="appid";
    private static final String secretid                    ="secretid";
    private static final String token                       ="token";
//    private static final String token_expir_second          ="token_expir_second";
    
    //通过POST XML 内容体进行请求
    //字段名                变量名                 必填             类型                说明
    //版本号                version                 是             String(8)            版本号，version默认值是1.0
    //字符集                charset                 否             String(8)            可选值 UTF-8 ，默认为 UTF-8
    //签名方式              sign_type               否             String(8)            签名类型，取值：MD5默认：MD5
    //商户号                mch_id                  是             String(32)           商户号，由平台分配
    //商户订单号            out_trade_no            是             String(32)           商户系统内部的订单号 ,32个字符内、 可包含字母,确保在商户系统唯一
    //设备号                device_info             否             String(32)           终端设备号
    //商品描述              body                    是             String(127)          商品描述
    //附加信息              attach                  否             String(127)          商户附加信息，可做扩展参数
    //总金额                total_fee               是             Int                  总金额，以分为单位，不允许包含任何字、符号
    //终端IP                mch_create_ip           是             String(16)           订单生成的机器 IP
    //通知地址              notify_url              是             String(255)          接收平台通知的URL，需给绝对路径，255字符内格式如:http://wap.tenpay.com/tenpay.asp，确保平台能通过互联网访问该地址
    //订单生成时间          time_start              否             String(14)           订单生成时间，格式为yyyyMMddHHmmss，如2009年12月25日9点10分10秒表示为20091225091010。时区为GMT+8 beijing。该时间取自商户服务器。注：订单生成时间与超时时间需要同时传入才会生效
    //订单超时时间          time_expire             否             String(14)           订单失效时间，格式为yyyyMMddHHmmss，如2009年12月27日9点10分10秒表示为20091227091010。时区为GMT+8 beijing。该时间取自商户服务器。注：订单生成时间与超时时间需要同时传入才会生效
    //操作员                op_user_id              否             String(32)           操作员帐号,默认为商户号
    //商品标记              goods_tag               否             String(32)           商品标记，微信平台配置的商品标记，用于优惠券或者满减使用
    //商品ID                product_id              否             String(32)           预留字段此 id 为静态可打印的二维码中包含的商品 ID，商户自行维护
    //随机字符串            nonce_str               是             String(32)           随机字符串，不长于 32 位
    //是否限制信用卡        limit_credit_pay        否             String(32)           限定用户使用时能否使用信用卡，值为1，禁用信用卡；值为0或者不传此参数则不禁用
    //签名                  sign                    是             String(32)           MD5签名结果，详见“安全规范”
    //返回结果
    //数据按XML的格式实时返回
    //字段名                变量名                  必填            类型                说明
    //版本号                version                  是            String(8)            版本号，version默认值是1.0
    //字符集                charset                  是            String(8)            可选值 UTF-8 ，默认为 UTF-8
    //签名方式              sign_type                是            String(8)            签名类型，取值：MD5默认：MD5
    //返回状态码            status                   是            String(16)            0表示成功，非0表示失败此字段是通信标识，非交易标识，交易是否成功需要查看result_code 来判断
    //返回信息              message                  否            String(128)            返回信息，如非空，为错误原因签名失败参数格式校验错误
    //以下字段在    status 为 0的时候有返回                                           
    //字段名                变量名                  必填            类型                说明
    //业务结果              result_code              是            String(16)            0表示成功，非0表示失败
    //签名                  sign                     是            String(32)            MD5签名结果，详见“安全规范”
    //以下字段在 status 和 result_code 都为 0的时候有返回                           
    //字段名                变量名                  必填            类型                说明
    //二维码链接            code_url                 是            String(64)            商户可用此参数自定义去生成二维码后展示出来进行扫码支付
    //二维码图片            code_img_url             是            String(256)            此参数的值即是根据code_url生成的可以扫码支付的二维码图片地址
    private static final String version                       ="version";
    private static final String charset                       ="charset";
    private static final String sign_type                     ="sign_type";
    private static final String mch_id                        ="mch_id";
    private static final String out_trade_no                  ="out_trade_no";
//    private static final String device_info                   ="device_info";
    private static final String body                          ="body";
//    private static final String attach                        ="attach";
    private static final String total_fee                     ="total_fee";
    private static final String mch_create_ip                 ="mch_create_ip";
    private static final String notify_url                    ="notify_url";
//    private static final String time_start                    ="time_start";
//    private static final String time_expire                   ="time_expire";
//    private static final String op_user_id                    ="op_user_id";
//    private static final String goods_tag                     ="goods_tag";
//    private static final String product_id                    ="product_id";
    private static final String nonce_str                     ="nonce_str";
//    private static final String limit_credit_pay              ="limit_credit_pay";
//    private static final String sign                          ="sign";
//    private static final String version                       ="version";
//    private static final String charset                       ="charset";
//    private static final String sign_type                     ="sign_type";
//    private static final String status                        ="status";
//    private static final String message                       ="message";
    private static final String result_code                   ="result_code";
//    private static final String sign                          ="sign";
    private static final String code_url                      ="code_url";
//    private static final String code_img_url                  ="code_img_url";

    //银联快捷
    //银行代号    bank_id 是   String  银行代号，跳转至收银台支付，该值为固定onlineBankPay
    private static final String bank_id                       ="bank_id";
    
    private static final String pay_info                      ="pay_info";

    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version,"1.0");
                put(charset,"UTF-8");
                put(sign_type,"MD5");
                put(mch_id, channelWrapper.getAPI_MEMBERID());
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(body,"name");
//                put(attach, channelWrapper.getAPI_MEMBERID());
                put(total_fee,  channelWrapper.getAPI_AMOUNT());
                put(mch_create_ip,channelWrapper.getAPI_Client_IP());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(nonce_str,handlerUtil.getRandomStr(10));
                if (handlerUtil.isWebYlKjzf(channelWrapper)) {
                    put(bank_id,"onlineBankPay");
                }
            }
        };
        log.debug("[信誉]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        signSrc.append(key+"=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[信誉]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        String resultStr = RestTemplateUtil.postXml(channelWrapper.getAPI_CHANNEL_BANK_URL()+"?token="+token(channelWrapper), XmlUtil.map2Xml(payParam, false, "xml", true));
        if (StringUtils.isBlank(resultStr)) {
            log.error("[信誉]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        Map<String, String> resJson = XmlUtil.xml2Map(resultStr);
        //只取正确的值，其他情况抛出异常
        if (null != resJson && resJson.containsKey(result_code) && "0".equalsIgnoreCase(resJson.get(result_code))  
//                && resJson.containsKey(code_url) && StringUtils.isNotBlank(resJson.get(code_url))
                ) {
            if (handlerUtil.isWapOrApp(channelWrapper) || handlerUtil.isWebYlKjzf(channelWrapper)) {
                String my_pay_info = resJson.get(pay_info);
                if (StringUtils.isBlank(my_pay_info)) {
                    log.error("[信誉]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
//            String qrtext = QRCodeUtil.decodeByUrl("https://api.xinyuzhifu.com/common/qrcode?width=300&height=300&content=123456");
                result.put(JUMPURL, my_pay_info);
            }else {
                String qrtext = QRCodeUtil.decodeByUrl(resJson.get(code_url));
                if (StringUtils.isBlank(qrtext)) {
                    log.error("[信誉]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
//            String qrtext = QRCodeUtil.decodeByUrl("https://api.xinyuzhifu.com/common/qrcode?width=300&height=300&content=123456");
                result.put(QRCONTEXT, qrtext);
            }
        }else {
            log.error("[信誉]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[信誉]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
        return payResultList;
    }

    private String token(ChannelWrapper channelWrapper) throws PayException {
        Map<String,String> map = new TreeMap<>();
        map.put(appid, channelWrapper.getAPI_MEMBERID());
        map.put(secretid, channelWrapper.getAPI_KEY());
        String resultStr = RestTemplateUtil.sendByRestTemplate(tokenUrl, map, String.class, HttpMethod.GET);
        if (StringUtils.isBlank(resultStr)) {
            log.error("[信誉]-[请求支付]-token().发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            throw new PayException("返回空,参数："+JSON.toJSONString(map));
        }
        Map<String, String> xml2Map = XmlUtil.xml2Map(resultStr);
        String myToken = xml2Map.get(token);
        if (StringUtils.isBlank(myToken)) {
            log.error("[信誉]-[请求支付]-token().发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
        return myToken;
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
        log.debug("[信誉]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}