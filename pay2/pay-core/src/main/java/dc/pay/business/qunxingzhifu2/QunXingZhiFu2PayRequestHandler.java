package dc.pay.business.qunxingzhifu2;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.ValidateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 *
 * @author andrew
 * Jul 11, 2019
 */
@RequestPayHandler("QUNXINGZHIFU2")
public final class QunXingZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(QunXingZhiFu2PayRequestHandler.class);

    private static final String mark       = "mark";      //     是     是     商户自己生成唯一不重复订单号，纯数字长度为12~20位
    private static final String name       = "name";      //     是     是     商品名称，最多15个字符
    private static final String money      = "money";     //     是     是     商品价格，精确到小数点后两位数字
    private static final String type       = "type";      //     是     是     通道类型，固定填：bankcard
    private static final String service    = "service";   //     是     否     支付类型，固定填：0：支付宝复制转卡 4:微信转卡 6：支付宝口碑转卡 7：支付宝复制转帐 9:H5转卡
    private static final String mn         = "mn";        //     是     是     平台分配商户号
    private static final String notify_url = "notify_url";//     是     是     商户异步通知url地址
    private static final String return_url = "return_url";//     是     是     商户页面返回通知url地址


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[群星支付2]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号mark&通道类型编码值service（向第三方获取当前使用通道编码值）" );
            throw new PayException("[群星支付2]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号mark&通道类型编码值service（向第三方获取当前使用通道编码值）" );
        }
        
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(mark, channelWrapper.getAPI_ORDER_ID());
                put(name, "name");
                put(money, HandlerUtil.getYuanWithoutZero(channelWrapper.getAPI_AMOUNT()));
                put(type, "bankcard3");
//                put(service, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(service, channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(mn, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(notify_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url, channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[群星支付2]-[请求支付]-1.组装请求参数完成：{}", JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List          paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc   = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (service.equals(paramKeys.get(i))) {
                continue;
            }
            String params = null;
            try {
                params = URLEncoder.encode(api_response_params.get(paramKeys.get(i)), "UTF-8").toLowerCase();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            signSrc.append(paramKeys.get(i)).append("=").append(params);
        }
//        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString().toLowerCase();
        paramsStr = paramsStr + channelWrapper.getAPI_KEY();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[群星支付2]-[请求支付]-2.生成加密URL签名完成：{}", JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();
        try {

            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString());

        } catch (Exception e) {
            log.error("[群星支付2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null", e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[群星支付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap, channelWrapper, requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[群星支付2]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}