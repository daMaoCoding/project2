package dc.pay.business.beilibaozhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;

import net.sf.json.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.business.kuaitongbaozhifu.RSAUtils;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestPayHandler("BEILIGBAOZHIFU")
public final class BeiLiBaoZhiFuPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);

    private static final String    merchantNumber="merchantNumber";              //  快通宝商户号   是	String(32)	PAY10017681024	商户在快通宝注册与使用的商户编号
    private static final String    requestTime="requestTime";              //  请求时间   是	String(14)	20170808161616	请求时间：yyyyMMddHHmmss
    private static final String    sign="sign";              //  签名信息   是	String		使用商户证书对报文签名后值
    private static final String    version="version";

    private static final String    orderNumber="orderNumber";              //  商户单号   是	String(50)	order201712010001	商户上送订单号，保持唯一值。
    private static final String    amount="amount";              //  交易金额   是	String(20)	1000	以分为单位，10.00元填入值为1000
    private static final String    currency="currency";              //  币种   是	String(10)	CNY	CNY-人民币，默认为CNY
    private static final String    commodityName="commodityName";              //  商品名称   是	String(20)		用于描述该笔交易或商品的主体信息
    private static final String    commodityDesc="commodityDesc";              //  商品描述   是	String(500)		用于描述该笔交易或商品的主体信息
    private static final String    payType="payType";              //  付类型   是	String(20)	QQ_NATIVE	详见“附录：交易类型-聚合支付”
    private static final String    notifyUrl="notifyUrl";              //  异步通知地址   是	String(255)	0	支付结果后台异步通知地址。(不能含有’字符. 如果含有?&=字符, 必须先对该地址做URL编码)
    private static final String    orderCreateIp="orderCreateIp";              //  商户发起支付请求的IP   是	String(16)
    private static final String    commodityRemark="commodityRemark";              //  用于描述该笔交易或商品的主体信息
    private static final String    returnUrl="returnUrl";              //  支付成功后，跳转到商户定义的地址中（不能含有’字符，如果含有?&=字符, 必须先对该地址做URL编码）

    private static final String    cardType="cardType";               //   银行卡类型。    储蓄卡：SAVINGS    信用卡：CREDIT（暂不支持）
    private static final String    SAVINGS="SAVINGS";               //
    private static final String    bankNumber="returnUrl";            //   银行编号

    private static final String    CNY="CNY";            //   CNY
    private static final String    Version="V1.1.0";            //回调增加订单号


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        JSONObject businessHead = new JSONObject();
        businessHead.put(merchantNumber, channelWrapper.getAPI_MEMBERID());
        businessHead.put(version, Version);
        businessHead.put(requestTime, HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMddHHmmss"));

        JSONObject businessContext = new JSONObject();
        businessContext.put(orderNumber, channelWrapper.getAPI_ORDER_ID());//商户单号 需要确保唯一
        businessContext.put(amount,channelWrapper.getAPI_AMOUNT());
        businessContext.put(currency, CNY);
        businessContext.put(commodityName, channelWrapper.getAPI_ORDER_ID());//商品名称
        businessContext.put(commodityDesc,channelWrapper.getAPI_ORDER_ID());//商品描述
        businessContext.put(commodityRemark, channelWrapper.getAPI_ORDER_ID());//商品备注
        businessContext.put(payType,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());// 1.支付宝扫码 WECHAT_NATIVE 2.银联扫码 UNION_NATIVE 3.QQ钱包扫码 QQ_NAIVE
        businessContext.put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
        businessContext.put(returnUrl, channelWrapper.getAPI_WEB_URL());
        businessContext.put(orderCreateIp, channelWrapper.getAPI_Client_IP());//商户发起支付请求的IP

        if(HandlerUtil.isWY(channelWrapper)){
            businessContext.put(payType,HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM())?"B2C_H5":"B2C");
            businessContext.put(cardType,SAVINGS );
            businessContext.put(bankNumber, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        }

        payParam.put("businessHead",businessHead.toString());
        payParam.put("businessContext",businessContext.toString());
        log.debug("[贝立宝支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> params) throws PayException {
        return null;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        JSONObject jsonResult = null;
        JSONObject businessHead = JSONObject.fromObject(payParam.get("businessHead"));
        JSONObject businessContext = JSONObject.fromObject(payParam.get("businessContext"));

        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            String context = RSAUtils.verifyAndEncryptionToString(businessContext, businessHead, channelWrapper.getAPI_KEY(), channelWrapper.getAPI_PUBLIC_KEY());
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("context", context);

           //   jsonResult = HttpClients.doPost(channelWrapper.getAPI_CHANNEL_BANK_URL(), jsonParam);

            resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), jsonParam);
            jsonResult =JSONObject.fromObject(resultStr);

            if (jsonResult.getBoolean("success")){
                String resultContext = jsonResult.getString("context");
                String decrypt = RSAUtils.decryptByPrivateKey(resultContext,channelWrapper.getAPI_KEY() );
              //  boolean isVerify = RSAUtils.verify(decrypt, channelWrapper.getAPI_PUBLIC_KEY());   //验证失败，第三方不要求验证。
                JSONObject resultJson = JSONObject.fromObject(decrypt);
                if ( null!=resultJson && resultJson.containsKey("businessContext")  && null!=resultJson.getJSONObject("businessContext")
                        &&  resultJson.getJSONObject("businessContext").containsKey("payurl")
                        && StringUtils.isNotBlank(resultJson.getJSONObject("businessContext").getString("payurl"))
                        && !ValidateUtil.isHaveChinese(resultJson.getJSONObject("businessContext").getString("payurl"))) {
                        if(HandlerUtil.isWapOrApp(channelWrapper)){
                            result.put(JUMPURL,  resultJson.getJSONObject("businessContext").getString("payurl"));
                        }else{
                            result.put(QRCONTEXT,  resultJson.getJSONObject("businessContext").getString("payurl"));
                        }
                        payResultList.add(result);
                }else{ throw new PayException(resultJson.toString());}

            }else{ throw new PayException(jsonResult.toString());}

        } catch (Exception e) {
             log.error("[贝立宝支付]3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[贝立宝支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
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
        log.debug("[贝立宝支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}