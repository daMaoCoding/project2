package dc.pay.business.xinfubao;

/**
 * ************************
 *
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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.*;

@RequestPayHandler("XINFUBAO")
public final class XinFuBaoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinFuBaoPayRequestHandler.class);

    private static final String RESPCODE = "respCode";
    private static final String MESSAGE = "message";
    private static final String BARCODE = "barCode";
    private static final String QRCONTEXT = "QrContext";
    private static final String HTMLCONTEXT = "HtmlContext";
    private static final String PARSEHTML = "parseHtml";


    private static final String retCode = "retCode";      //结果代码,返回1表示成功
    private static final String QrCode = "qrcode"; //二维码地址，当选择扫码支付时，会返回二维码的链接地址，由商户自行生成二维码




    private static final String  accountType   ="accountType";    //:0  银行卡种类
    private static final String  pnum   ="pnum";    //:1
    private static final String  tranChannel   ="tranChannel";    //:103
   // private static final String  payMode   ="payMode";    //:00024  00020-银行卡



    private static final String versionId = "versionId";                    //:1.0
    private static final String orderAmount = "orderAmount";                //:300 分
    private static final String orderDate = "orderDate";                    //:20171015124242  yyyyMMddHHmmss
    private static final String currency = "currency";                      //:RMB
    private static final String transType = "transType";                    //:008
    private static final String asynNotifyUrl = "asynNotifyUrl";            //:http://test.tongle.net/return_text.aspx
    private static final String synNotifyUrl = "synNotifyUrl";              //:http://test.tongle.net/return_text.aspx?trade_no=1000201708181756895236
    private static final String signType = "signType";                     //:MD5
    private static final String merId = "merId";                           //:<%=PayConfig.merId%>
    private static final String prdOrdNo = "prdOrdNo";                    //:59071442505379
    private static final String payMode = "payMode";                       //:00022,微信支付宝QQ
    private static final String receivableType = "receivableType";        //:D00 到账
    private static final String prdAmt = "prdAmt";                       //:1商品价格
    private static final String prdName = "prdName";                    //:100元移动充值卡
    private static final String prdDesc = "prdDesc";                    //:
    private static final String signData = "signData";










    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        SortedMap<String, String> payParam = Maps.newTreeMap();
        payParam.put(versionId,   "1.0");
        payParam.put(orderAmount,   channelWrapper.getAPI_AMOUNT());
        payParam.put(orderDate, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
        payParam.put(currency,   "RMB");
        payParam.put(transType,   "008");
        payParam.put(asynNotifyUrl,   channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
      //  payParam.put(synNotifyUrl,   "");
        payParam.put(signType,   "MD5");
        payParam.put(merId,   channelWrapper.getAPI_MEMBERID());
        payParam.put(prdOrdNo,   channelWrapper.getAPI_ORDER_ID());
        payParam.put(payMode,   channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
        payParam.put(receivableType,  "D00");
        payParam.put(prdAmt,   channelWrapper.getAPI_AMOUNT());
        payParam.put(prdName,   prdName);

        if(HandlerUtil.isWY(channelWrapper)){
            payParam.put(accountType,   "0");
            payParam.put(payMode,   "00020");
            payParam.put(pnum,   "1");
            payParam.put(receivableType,   "D00");
            payParam.put(prdDesc,   channelWrapper.getAPI_ORDER_ID());
            payParam.put(tranChannel,  "103");
        }else if(HandlerUtil.isWapOrApp(channelWrapper)){
            payParam.put(accountType,   "0");
            payParam.put(tranChannel,  "103");
            payParam.put(receivableType,   "D00");
            payParam.put(prdDesc,   channelWrapper.getAPI_ORDER_ID());
            payParam.put(pnum,   "1");
        }


        log.debug("[信付宝]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map payParam) throws PayException {
        String pay_md5sign = XinFuBaoPayUtil.createSign(payParam,channelWrapper.getAPI_KEY()); //把数组所有元素，按照“参数=参数值”的模式用“&”字符拼接成字符串
       // String pay_md5sign = XinFuBaoPayUtil.sign(prestr, channelWrapper.getAPI_KEY(), "UTF-8");
        log.debug("[信付宝]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();

        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  || HandlerUtil.isWapOrApp(channelWrapper)    ) {
                HashMap<String, String> result = Maps.newHashMap();
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='post'"));
                payResultList.add(result);
            }else {
                String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                //resultStr = new String(resultStr.getBytes("ISO-8859-1"), "GBK");
                JSONObject responseJsonObject = JSONObject.parseObject(resultStr);
                String respCode = responseJsonObject.getString(retCode);
                String respQrCode = responseJsonObject.getString(QrCode);
                if ("1".equalsIgnoreCase(respCode) && StringUtils.isNotBlank(respQrCode)) {
                    HashMap<String, String> result = Maps.newHashMap();
                    result.put(QRCONTEXT, respQrCode);
                    result.put(PARSEHTML, resultStr);
                    payResultList.add(result);

                } else {
                    throw new PayException(resultStr);
                }
            }

        } catch (Exception e) {
            log.error("[信付宝]3.发送支付请求，及获取支付请求结果出错：{}", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[信付宝]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                if (null != resultMap && resultMap.containsKey(QRCONTEXT)) {
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                    requestPayResult.setRequestPayQRcodeContent(resultMap.get(QRCONTEXT));
                }
                if (null != resultMap && resultMap.containsKey(HTMLCONTEXT)) {
                    requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
                    requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
                    requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
                    requestPayResult.setRequestPayQRcodeURL(null);
                    requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
                    requestPayResult.setRequestPayHtmlContent(resultMap.get(HTMLCONTEXT));
                }

            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[信付宝]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}