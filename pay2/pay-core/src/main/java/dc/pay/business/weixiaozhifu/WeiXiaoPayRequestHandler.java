package dc.pay.business.weixiaozhifu;

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
import dc.pay.business.caifubao.StringUtil;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestPayHandler("WEIXIAOZHIFU")
public final class WeiXiaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(WeiXiaoPayRequestHandler.class);

    private  static final  String 	merchantId = "merchantId";	           //  商户号   ,           是	string(20)	商户识别号，由微笑支付分配
    private  static final  String 	payMode = "payMode";	               //  支付方式   ,         是	string(10)
    private  static final  String 	orderNo = "orderNo";	               //  商户订单号   ,       是	string(20)	商户网站唯一订单号
    private  static final  String 	orderAmount = "orderAmount";	       //  总金额   ,           是	double	商户订单总金额，最小单位：分。例：12.34
    private  static final  String 	goods = "goods";	                   //  商品名称   ,         是	string(50)	商品名称，可留空
    private  static final  String 	notifyUrl = "notifyUrl";	           //  后台通知地址   ,      是	string(255)	接收微笑支付通知的url，需给绝对路径，255字符内格式  如:http://wap.abc.com/notify.php，确保通过互联网可访问该地址
    private  static final  String 	returnUrl = "returnUrl";	           //  交易后返回商户地址,   是	string(255)	交易完成后返回商户网站的地址，需给绝对路径 微信、支付宝扫码支付不支持返回，可以留空 银行卡支付时若留空，则支付后将不会返回商家页面
    private  static final  String 	bank = "bank";	                       //  银行代码   ,          是	string(10)	参照第10章节：银行编号 银行卡支付时需填写，其他支付方式留空
    private  static final  String 	memo = "memo";	                       //  订单备注   ,          否	string(128)	商户订单备注，异步通知时原样返回  字段原样返回，如输入非英文数字，请自行进行UrlEncode和UrlDecode，避免乱码
    private  static final  String 	encodeType = "encodeType";	           //  签名方式   ,          是	string(10)	固定值：SHA2
    private  static final  String 	signSHA2 = "signSHA2";	               //  签名   ,              否	string(128)	签名结果，详见“第4 章 签名规则”
    private  static final  String 	Bank = "Bank";
    private  static final  String 	SHA2 = "SHA2";




    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();

        if(!channelWrapper.getAPI_MEMBERID().contains("&") || channelWrapper.getAPI_MEMBERID().split("&").length!=2 || StringUtil.isBlank(channelWrapper.getAPI_MEMBERID().split("&")[0]) || StringUtil.isBlank(channelWrapper.getAPI_MEMBERID().split("&")[1]) ){
            throw new PayException("商户号格式错误：正确填写格式： 商户号&HashIV  ");
        }

        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(merchantId,channelWrapper.getAPI_MEMBERID().split("&")[0] );
            payParam.put(payMode, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG() );
            payParam.put(orderNo, channelWrapper.getAPI_ORDER_ID() );
          //  payParam.put(orderNo, System.currentTimeMillis()+""); //TODO: 临时
            payParam.put(orderAmount, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()) );
            payParam.put(goods, goods );
            payParam.put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );
            payParam.put(returnUrl, channelWrapper.getAPI_WEB_URL() );
            payParam.put(bank,"");
            if(HandlerUtil.isWY(channelWrapper)){
                 payParam.put(bank,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                 payParam.put(payMode,Bank);
            }
            payParam.put(memo, memo);
            payParam.put(encodeType,SHA2);
        }

        log.debug("[微笑支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(  signSHA2.equalsIgnoreCase(paramKeys.get(i).toString()  )  ||   memo.equalsIgnoreCase(paramKeys.get(i).toString() ) )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("HashIV=" +channelWrapper.getAPI_MEMBERID().split("&")[1]);
        String signStr = "SHA2Key=".concat(channelWrapper.getAPI_KEY()).concat("&").concat(sb.toString()); //.replaceFirst("&key=","")

       // pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();

        try {
            signStr =  URLEncoder.encode(signStr,"UTF-8").toLowerCase();
            pay_md5sign = new Sha256Hash(signStr, null).toString().toUpperCase();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }


        log.debug("[微笑支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
//            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)   ) {  //QQ wap 会返回 跳转地址，其他返回二维码地址
            if (true) {  //QQ wap 会返回 跳转地址，其他返回二维码地址
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString()); //.replace("method='post'","method='get'"));
                payResultList.add(result);
            }
            
//            else{
//                resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
//                resultStr= UnicodeUtil.unicodeToString(resultStr);
//                resultStr= resultStr.replace("\\\"", "").replace("\\", "").replace("\"", "");
//                
//                if(StringUtils.isNotBlank(resultStr) && resultStr.contains("http")){
//                    if(HandlerUtil.isWapOrApp(channelWrapper)){
//                        result.put(JUMPURL,resultStr);
//                    }else{
//                        result.put(QRCONTEXT, QRCodeUtil.decodeByUrl(resultStr));
//                    }
//                    payResultList.add(result);
//                }else{
//                    log.error("[微笑支付]3.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    throw new PayException(resultStr);
//                }
//            }
        } catch (Exception e) {
           log.error("[微笑支付]3.发送支付请求，及获取支付请求结果出错：", e);
           throw new PayException(e.getMessage(), e);
        }
        log.debug("[微笑支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[微笑支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}