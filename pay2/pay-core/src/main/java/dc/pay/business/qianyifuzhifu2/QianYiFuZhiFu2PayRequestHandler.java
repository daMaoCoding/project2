package dc.pay.business.qianyifuzhifu2;

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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * May 31, 2019
 */
@RequestPayHandler("QIANYIFUZHIFU2")
public final class QianYiFuZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(QianYiFuZhiFu2PayRequestHandler.class);

    private static final String pid                ="pid";         //商户ID        是   Int    1001
    private static final String gateway            ="gateway";     //支付网关       否   String    solidcode    不填写使用系统默认的。    bankcard:转账到银行卡, redpacket:红包转账,    pay009:好友转账,
    private static final String type               ="type";        //支付方式       是   String    alipay    alipay:支付宝,tenpay:财付通,    qqpay:QQ钱包,wxpay:微信支付,    union:云闪付,gateway:网银
    private static final String out_trade_no       ="out_trade_no";//商户订单号     是    String    20160806151343349
    private static final String notify_url         ="notify_url";  //异步通知地址    是   String    http://kk.fffpay.cc/notify_url.php    服务器异步通知地址
    private static final String return_url         ="return_url";  //跳转通知地址    是   String    http://kk.fffpay.cc/return_url.php    页面跳转通知地址
    private static final String name               ="name";        //商品名称       是    String    VIP会员
    private static final String money              ="money";       //商品金额       是    String    1.00
//  private static final String sitename           ="sitename";    //网站名称       否    String    彩虹云任务
    private static final String sign_type          ="sign_type";   //签名类型       是    String    MD5    默认为MD5

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(pid, channelWrapper.getAPI_MEMBERID());
                put(gateway, "pay009");
                put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(name,"name");
                put(money,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(sign_type,"MD5");
            }
        };
        log.debug("[千益付2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    @Override
    protected String buildPaySign(Map<String, String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (!sign_type.equals(paramKeys.get(i)) && StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //删除最后一个字符
        signSrc.deleteCharAt(signSrc.length()-1);
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[千益付2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();
        try {

                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());

        } catch (Exception e) {
            log.error("[千益付2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[千益付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[千益付2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}