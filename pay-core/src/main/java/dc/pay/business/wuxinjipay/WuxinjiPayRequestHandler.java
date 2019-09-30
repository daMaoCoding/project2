package dc.pay.business.wuxinjipay;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.HttpUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.Result;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ************************
 * @author tony 3556239829
 */
@RequestPayHandler("WUXINJIPAY")
public class WuxinjiPayRequestHandler  extends PayRequestHandler {
    private static final Logger log =  LoggerFactory.getLogger(WuxinjiPayRequestHandler.class);
    @Override
    protected Map<String, String> buildPayParam() throws PayException{
        Map<String, String> payParam = new HashMap<String, String>() {
            {
                put("input_charset", "UTF-8");
                put("notify_url", channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put("pay_type", "1");
                put("bank_code", channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put("merchant_code", channelWrapper.getAPI_MEMBERID());
                put("order_no", channelWrapper.getAPI_ORDER_ID());
                put("order_amount", HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put("order_time",HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(),"yyyy-MM-dd HH:mm:ss"));
            }
        };
        log.debug("[五心支付]-[请求支付]-1.组装请求参数完成："+ JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> payParam) throws PayException {
        String pay_md5sign ;
        String needSignStr;
        List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
        }
        sb.append("key="+channelWrapper.getAPI_KEY());
        try {
                   needSignStr = new String(sb.toString().getBytes(),"UTf-8");  //要求转UTF-8
                   pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString());

        } catch (UnsupportedEncodingException e) {
            log.error("五心级支付：生成加密URL签名-错误"+sb.toString()+","+e.getMessage());
            throw new PayException(SERVER_MSG.REQUEST_PAY_BUILDSIGN_ERROR,e);
        }
        log.debug("[五心支付]-[请求支付]-2.生成加密URL签名完成："+ JSON.toJSONString(pay_md5sign));
        return  pay_md5sign;
    }
    @Override
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String,String>> payResultList = Lists.newArrayList();
        try{
            Result firstPayresult = HttpUtil.post(channelWrapper.getAPI_CHANNEL_BANK_URL(), null, payParam, "UTF-8");
            Document document = Jsoup.parse(firstPayresult.getBody());  //Jsoup.parseBodyFragment(html)
            Elements err = document.getElementsByTag("err");
            if(err.size()>0){
                String errHtml = err.first().html();
                throw new PayException(errHtml);
            }
        } catch (Exception e) {
            throw new PayException(SERVER_MSG.REQUEST_PAY_PARSE_RESULT_ERROR,e);
        }
        log.debug("[五心支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功："+ JSON.toJSONString(payResultList));
        return payResultList;
    }
    @Override
    protected RequestPayResult buildResult(List<Map<String, String>> result) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult(); //请求支付结果
        log.debug("[五心支付]-[请求支付]-4.处理请求响应成功："+ JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}