package dc.pay.business.qichuan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import dc.pay.utils.qr.QRCodeUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 9, 2018
 */
@RequestPayHandler("QICHUAN")
public final class QiChuanPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(QiChuanPayRequestHandler.class);

    //字段名称  字段说明    类型  必填  备注
    //partner   商户号 int Y(参与签名) 例:10000
    private static final String partner                ="partner";
    //sdorderno 商户订单号不超过30  string  Y(参与签名) 201758985234234234
    private static final String sdorderno                 ="sdorderno";
    //paymoney  付款金额    float   Y(参与签名) 如：30.00(必须保留2位小数)
    private static final String paymoney                ="paymoney";
    //paytype   支付类型    string  Y   详细看附录1
    private static final String paytype                ="paytype";
    //bankcode  银行编号    string  Y   网银直连不可为空，其他支付方式可为空详见附录2
    private static final String bankcode              ="bankcode";
    //notifyurl 异步通知    string  Y(参与签名) 服务器通知
    private static final String notifyurl              ="notifyurl";
    //returnurl 同步  string  Y(参与签名) 浏览器跳转
    private static final String returnurl             ="returnurl";
    //remark    附加参数    string  N   按参数返回 不可超过30字
//    private static final String remark             ="remark";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(partner, channelWrapper.getAPI_MEMBERID());
                put(sdorderno,channelWrapper.getAPI_ORDER_ID());
                put(paymoney,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                if (handlerUtil.isWY(channelWrapper)) {
                    put(bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }else {
                    put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }
                put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(returnurl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[齐川]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(notifyurl+"=").append(api_response_params.get(notifyurl)).append("&");
        signSrc.append(partner+"=").append(api_response_params.get(partner)).append("&");
        signSrc.append(paymoney+"=").append(api_response_params.get(paymoney)).append("&");
        signSrc.append(returnurl+"=").append(api_response_params.get(returnurl)).append("&");
        signSrc.append(sdorderno+"=").append(api_response_params.get(sdorderno)).append("&");
        signSrc.append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[齐川]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
            //本家支付宝扫码，使用了wap通道来包装
            if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("QICHUAN_BANK_WEBWAPAPP_ZFB_SM")) {
                result.put( QRCONTEXT,  handlerUtil.getHtmlUrl(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam));
            }else {
                String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//              String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
              if (StringUtils.isBlank(resultStr)) {
                  log.error("[齐川]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                  throw new PayException(resultStr);
                  //log.error("[齐川]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
                  //throw new PayException("返回空,参数："+JSON.toJSONString(map));
              }
              Elements select = Jsoup.parse(resultStr).select("[id=code_url]");
              if (null == select || select.size() != 1) {
                  log.error("[齐川]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                  throw new PayException(resultStr);
                  //log.error("[齐川]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
                  //throw new PayException("返回空,参数："+JSON.toJSONString(map));
              }
              String src = select.first().attr("src");
              if (StringUtils.isBlank(src)) {
                  log.error("[齐川]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                  throw new PayException(resultStr);
              }
              src = (src.startsWith("http") ? src : "http://www.emarfoo.com/"+src);
              String qr = QRCodeUtil.decodeByUrl(src);
              if (StringUtils.isBlank(qr)) {
                  log.error("[齐川]-[请求支付]-3.4.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                  throw new PayException(resultStr);
              }
              result.put( QRCONTEXT, qr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[齐川]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[齐川]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}