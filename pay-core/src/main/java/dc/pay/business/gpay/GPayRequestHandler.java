package dc.pay.business.gpay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Nov 14, 2018
 */
@RequestPayHandler("GPAY")
public final class GPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(GPayRequestHandler.class);

  //支付宝扫码支付
    //参数                    说明                                                    批注
    //HashKey                 厂商HashKey                                             由GPay提供
    //HashIV                  厂商HashIV                                              由GPay提供
    //MerTradeID              店家交易编号                                            店家自行设定，不得小于6个字符，不得重复。只支持英文及数字
    //MerProductID            店家商品代号                                            店家自行设定，不得小于6个字符。只支持英文及数字
    //MerUserID               店家消费者ID                                            店家自行设定，限英文或数字组合，最少6个字符
    //Amount                  交易金额                                                目前只支持整数金额。例如 1.00,10.00,20.00,30.00, 100.00…
    //TradeDesc               交易描述                                                
    //ItemName                商品名称                                                
    //NotifyUrl               交易异步回调网址                                        需含带http或https的标头。当使用者支付成功时，会呼叫该网址
    //ReturnCodeURL           是否只回传支付链结地址，由贵司自行生成支付页面          0: 由GPay生成支付页面，1: 回传支付链结地址，由贵司生成支付页面。该参数使用 0 时，请用FORM POST传送此API。
    //Validate                检查码                                                  编码方式请参阅备注一
    private static final String HashKey                   ="HashKey";
    private static final String HashIV                    ="HashIV";
    private static final String MerTradeID                ="MerTradeID";
    private static final String MerProductID              ="MerProductID";
    private static final String MerUserID                 ="MerUserID";
    private static final String Amount                    ="Amount";
    private static final String TradeDesc                 ="TradeDesc";
    private static final String ItemName                  ="ItemName";
    private static final String NotifyUrl                 ="NotifyUrl";
    private static final String ReturnCodeURL             ="ReturnCodeURL";
//    private static final String Validate                  ="Validate";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="Validate";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String api_KEY = channelWrapper.getAPI_KEY();
        if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 3) {
            log.error("[GPAY]-[请求支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：ValidateKey（密钥二）-HashIV（密钥一）-商号（HashKey）" );
            throw new PayException("[GPAY]-[请求支付]-“密钥（私钥）”输入数据格式为【中间使用-分隔】：ValidateKey（密钥二）-HashIV（密钥一）-商号（HashKey）" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(HashKey, channelWrapper.getAPI_KEY().split("-")[2]);
                put(HashIV, channelWrapper.getAPI_KEY().split("-")[1]);
                put(MerTradeID,channelWrapper.getAPI_ORDER_ID());
                put(MerProductID,  HandlerUtil.getRandomStr(8));
                put(MerUserID,  HandlerUtil.getRandomStr(8));
                //                  交易金额                                                目前只支持整数金额。例如 1.00,10.00,20.00,30.00, 100.00…
                put(Amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(TradeDesc,"name");
                put(ItemName,"name");
                put(NotifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                //           是否只回传支付链结地址，由贵司自行生成支付页面          0: 由GPay生成支付页面，1: 回传支付链结地址，由贵司生成支付页面。该参数使用 0 时，请用FORM POST传送此API。
//                put(ReturnCodeURL,"1");
                put(ReturnCodeURL, handlerUtil.isWapOrApp(channelWrapper) ? "0" : "1");
            }
        };
        log.debug("[GPAY]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(channelWrapper.getAPI_KEY().split("-")[0]);
        signSrc.append(api_response_params.get(HashKey));
        signSrc.append(api_response_params.get(MerTradeID));
        signSrc.append(api_response_params.get(MerProductID));
        signSrc.append(api_response_params.get(MerUserID));
        signSrc.append(api_response_params.get(Amount));
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[GPAY]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
            //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
            if (StringUtils.isBlank(resultStr)) {
                log.error("[GPAY]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
                //log.error("[GPAY]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            }
            if (!resultStr.contains("{") || !resultStr.contains("}")) {
                log.error("[GPAY]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //JSONObject resJson = JSONObject.parseObject(resultStr);
            JSONObject resJson;
            try {
                resJson = JSONObject.parseObject(resultStr);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[GPAY]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            //只取正确的值，其他情况抛出异常
            if (null != resJson && resJson.containsKey("Retcode") && "1".equalsIgnoreCase(resJson.getString("Retcode"))  && resJson.containsKey("CodeURL") && StringUtils.isNotBlank(resJson.getString("CodeURL"))) {
                String code_url = resJson.getString("CodeURL");
                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, handlerUtil.UrlDecode(code_url));
            }else {
                log.error("[GPAY]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[GPAY]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[GPAY]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}