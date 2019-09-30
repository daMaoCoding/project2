package dc.pay.business.yqing;

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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * May 18, 2019
 */
@RequestPayHandler("YQING")
public final class YQingPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YQingPayRequestHandler.class);

    //3.1.1 请求参数列表
    //序号  参数名 参数说明    签名  必填  数据类型    说明
    //1   org_number  接入机构编码  YES YES String  由系统分配
    private static final String org_number               ="org_number";
    //2   merchant_number 交易商户编号  YES YES String  由系统分配
    private static final String merchant_number               ="merchant_number";
    //3   payType 接口服务名称  YES YES String  由系统分配
    private static final String payType               ="payType";
    //4   data    请求报文    YES YES String  请求报文，采用AES加密密文，十六进制，全部大写
    private static final String data               ="data";
    //5   sign    签名校验    NO  YES String  参考2.3关于签名内容
//    private static final String sign               ="sign";
    //3.1.2 Data参数列表
    //序号  参数名 参数说明    签名  必填  数据类型    说明
    //1   orderType   固定值 NO  YES String  固定值填10
    private static final String orderType               ="orderType";
    //2   amount  订单金额    NO  YES String  10000=100.00元
    private static final String amount               ="amount";
    //3   goodsName   商品名称    NO  NO  String  
//    private static final String goodsName               ="goodsName";
    //4   out_trade_no    订单号 NO  YES String  
    private static final String out_trade_no               ="out_trade_no";
    //5   notifyUrl   回调地址    NO  YES String  
    private static final String notifyUrl               ="notifyUrl";
    //3.1.3响应参数说明
    //序号  参数名 参数说明    签名  必填  数据类型    说明
    //1   code    状态值 -   -   String  成功：success    失败：fall
//    private static final String code               ="code";
    //2   msg 状态说明    -   -   String  
//    private static final String msg               ="msg";
    //3   data    成功后返回数据 -   -   String  out_trade_no 订单号      amount    订单金额    pay_url   支付链接
//    private static final String data               ="data";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="pay_md5sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[yqing]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号merchant_number&机构号org_number" );
            throw new PayException("[yqing]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号merchant_number&机构号org_number" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(orderType,"10");
                put(amount,  channelWrapper.getAPI_AMOUNT());
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[yqing]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
         StringBuilder signStr = new StringBuilder();
         signStr.append(channelWrapper.getAPI_MEMBERID().split("&")[1]);
         signStr.append(channelWrapper.getAPI_MEMBERID().split("&")[0]);
         signStr.append(channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
         try {
             String encrypt = AesUtil.parseByte2HexStr(AesUtil.encrypt2(JSON.toJSONString(api_response_params), channelWrapper.getAPI_KEY().substring(0, 16)));
            signStr.append(encrypt);
         } catch (Exception e) {
            e.printStackTrace();
            log.error("[yqing]-[请求支付]-2.0.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
         }
         signStr.append(channelWrapper.getAPI_KEY());
         String paramsStr =signStr.toString();
         String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[yqing]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
//        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        Map<String, String> map = new TreeMap<String, String>() {
            {
                put(org_number, channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(merchant_number, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
            }
        };
        try {
            String encrypt = AesUtil.parseByte2HexStr(AesUtil.encrypt2(JSON.toJSONString(payParam), channelWrapper.getAPI_KEY().substring(0, 16)));
            map.put(data, encrypt);
        } catch (Exception e) {
           e.printStackTrace();
           log.error("[yqing]-[请求支付]-3.1.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
           throw new PayException(e.getMessage(),e);
        }
        HashMap<String, String> result = Maps.newHashMap();
//        if(HandlerUtil.isWapOrApp(channelWrapper)){
        if(true){
            StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), map);
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),map).toString().replace("method='post'","method='get'"));
            //保存第三方返回值
            result.put(HTMLCONTEXT,htmlContent.toString());
        }
        
//        else{
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), map,"UTF-8");
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[yqing]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            if (!resultStr.contains("<form") || !resultStr.contains("</form>")) {
//                log.error("[yqing]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            Document document = Jsoup.parse(resultStr);  //Jsoup.parseBodyFragment(html)
//            Element formEl = document.getElementsByTag("form").first();
//            Map<String, String> secondPayParam = HandlerUtil.parseFormElement(formEl);
//            String resultStr2 = RestTemplateUtil.postForm(secondPayParam.get("action"), secondPayParam,"UTF-8");
//            if (StringUtils.isBlank(resultStr2)) {
//                log.error("[yqing]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr2);
//            }
//            Elements select = Jsoup.parse(resultStr2).select("[id=show_qrcode]");
//            if (null == select || select.size() < 1) {
//                log.error("[yqing]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr2);
//            }
//            String src = select.first().attr("src");
//            if (StringUtils.isBlank(src) || !src.contains("://")) {
//                log.error("[yqing]-[请求支付]-3.5.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr2);
//            }
//            String qr = QRCodeUtil.decodeByUrl(src);
//            if (StringUtils.isBlank(qr)) {
//                log.error("[yqing]-[请求支付]-3.6.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr2) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr2);
//            }
//            result.put(QRCONTEXT, qr);
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[yqing]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[yqing]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}