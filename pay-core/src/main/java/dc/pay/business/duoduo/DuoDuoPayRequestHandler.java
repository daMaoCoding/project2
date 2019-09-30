package dc.pay.business.duoduo;

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
 * Jul 9, 2018
 */
@RequestPayHandler("DUODUO")
public final class DuoDuoPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DuoDuoPayRequestHandler.class);

    //字段名                  变量名                  必填            说明
    //商户ID                  mchid                    是            商户在平台的 商户ID号
    //商户订单号              mchno                    是            商户自己生成的订单号 由英文、数字、_、- 、组成，不能含有特殊符号 如：2999218888886、no2999218888886 ，长度不能超过32位
    //提交模式                pattern                  是            general （固定值）一般模式，默认为此模式      develop （固定值）开发模式
    //订单类型                tradetype                是            alipayh5 （固定值）表示发起支付宝h5支付      支付宝H5 目前只支持提交如下固定金额：10. 50. 100. 200. 300. 500. 1000. 2000. 5000. 10000. 20000商户需要开通了对应的产品
    //订单金额                totalfee                 是            订单需要支付的金额，单位：元（人民币），可保留2位小数，如： 60 、 80.18
    //订单描述                descrip                  是            长度不能超过127位 可以由中文、英文、数字、_、- 、组成不能含有特殊符号 如： XX充值中心-XX会员充值 含有中文需要utf-8编码
    //附加数据                attach                   否            可为空， 商户的附加数据，回调的时候会原样返回，主要用于商户携带订单的自定义数据 如：XX分店 长度不能超过127位， 可以由中文、英文、数字、_、- 、组成不能含有特殊符号 含有中文需要utf-8编码
    //终端IP                  clientip                 是            长度不能超过46位， 订单生成的机器 IP
    //通知地址                notifyurl                是            接收平台异步通知回调地址，通知url必须为直接可访问的url，不能携带参数。 如： http://www.xxxx.com/wxpay/pay.php
    //同步通知地址            returnurl                否            可为空， 订单支付成功后同步跳转的地址, url必须为直接可访问的url，不能携带参数。 如： http://www.xxxx.com/wxpay/pay.php
    //MD5签名                 sign                     是            sign加密时要按照下面示例：mchid=10000&mchno=201803051730&pattern=general&tradetype=weixin&totalfee=60&descrip=xxxx&attach=xxxx&clientip=127.0.0.1&notifyurl=http://xxxx.cn/wxpay/pay.php&returnurl=http://xxxx.cn/wxpay/pay.php&key=c4b70b766ea78fe1689f4e4e1afa291a      key值为商户在平台的 通信KEY      组织好需要提交的数据按以上排列进行MD5加密后 赋值给 sign
    private static final String mchid                           ="mchid";
    private static final String mchno                           ="mchno";
    private static final String pattern                         ="pattern";
    private static final String tradetype                       ="tradetype";
    private static final String totalfee                        ="totalfee";
    private static final String descrip                         ="descrip";
    private static final String attach                          ="attach";
    private static final String clientip                        ="clientip";
    private static final String notifyurl                       ="notifyurl";
    private static final String returnurl                       ="returnurl";


    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(mchid, channelWrapper.getAPI_MEMBERID());
            	put(mchno,channelWrapper.getAPI_ORDER_ID());
            	put(pattern,"general");
            	put(tradetype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            	put(totalfee,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            	put(descrip,  "name");
            	put(attach,  "name");
            	put(clientip,  channelWrapper.getAPI_Client_IP());
            	put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	put(returnurl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[多多]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(mchid+"=").append(api_response_params.get(mchid)).append("&");
        signSrc.append(mchno+"=").append(api_response_params.get(mchno)).append("&");
        signSrc.append(pattern+"=").append(api_response_params.get(pattern)).append("&");
        signSrc.append(tradetype+"=").append(api_response_params.get(tradetype)).append("&");
        signSrc.append(totalfee+"=").append(api_response_params.get(totalfee)).append("&");
        signSrc.append(descrip+"=").append(api_response_params.get(descrip)).append("&");
        signSrc.append(attach+"=").append(api_response_params.get(attach)).append("&");
        signSrc.append(clientip+"=").append(api_response_params.get(clientip)).append("&");
        signSrc.append(notifyurl+"=").append(api_response_params.get(notifyurl)).append("&");
        signSrc.append(returnurl+"=").append(api_response_params.get(returnurl)).append("&");
        signSrc.append("key="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[多多]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        if (HandlerUtil.isWapOrApp(channelWrapper)) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[多多]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[多多]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}