package dc.pay.business.youyoufuzhifu;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jan 3, 2019
 */
@RequestPayHandler("YOUYOUFUZHIFU")
public final class YouYouFuZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YouYouFuZhiFuPayRequestHandler.class);

    //参数                                         必需  说明
    //cid                    y                     创建接入商公司时生成的的数字，登录YF收付平台获取
    private static final String cid                ="cid";
    //uid                    y                     此订单的会员ID，运营网站系统的会员唯一标识
    private static final String uid                ="uid";
    //time                   y                     此次操作的时间，格式: UNIX时间戳 秒值，超过３分钟则为非法订单 (即此参数值和YF系统收到请求的时间差不能超过３分钟)为了避免订单时间经常出错导致提交失败，建议使用服务器的时间，不要使用会员客户端的时间
    private static final String time                ="time";
    //amount                 y                     充值的金额，单位 元 ，最多可带两位小数点，如: 1.23, 2.6
    private static final String amount                ="amount";
    //order_id               y                     接入商网站生成的订单号，保证唯一性
    private static final String order_id                ="order_id";
    //ip                     y                     此次操作的会员IP地址，YF系统会获取会员的真实IP和此参数进行比较，不相同则为非法订单。同时用于对会员进行省份分类。
    private static final String ip                ="ip";
    //sign                   y                     使用公司API KEY对参数组成的字符串进行签名，签名算法见示例代码，字符串必须是按此顺序组合，只此6个：cid=xxx&uid=xxx&time=xxx&amount=xxx&order_id=xxx&ip=xxx
//    private static final String sign                ="sign";
    //syncurl                n                     订单状态改变后同步跳转地址，格式 http://xxx.com/path
    private static final String syncurl                ="syncurl";
    //clevel                 n                     指定会员等级分组: beginner,junior,experienced,vip1,vip2,vip3
//    private static final String clevel                ="clevel";
//    gflag                  n                     分组标识，如果有此参数，则只分配此分组内的收款卡
//    private static final String gflag                ="gflag";
    //type                   n                      存款方式，可选，值如下：         remit: 银行卡转账 ,   qrcode: 二维码存款,  online: 在线网银         quick: 快捷支付,  quickp2p: 快捷直连如果没有带入此参数，则自动进去YF收付的收银台，由会员选择存款方式
    private static final String type                ="type";
    //tflag                  n                     存款银行，可选，如果没有传入 type 参数，则此参数无效。如下：    ALIPAY: 支付宝 , WebMM: 微信支付,  QQPAY: QQ钱包    ABC: 农业银行 … (见后面的银行编码表)
    private static final String tflag                ="tflag";
    //extend                 n                     自定义参数，推送订单异步通知原样返回
//    private static final String extend                ="extend";
    //from_username          n                     存款银行卡姓名，可选，用于 type = remit/quickp2p 情况
//    private static final String from_username                ="from_username";
    //from_cardnumber        n                     存款银行卡号，可选，用于 type = remit/quickp2p 情况
//    private static final String from_cardnumber                ="from_cardnumber";
    //comment                n                     存款附言，可选，用于 type = remit 情况
//    private static final String comment                ="comment";
    //idnumber               n                     身份证号， 可选，用于 type = quickp2p 情况
//    private static final String idnumber                ="idnumber";
    //phonenumber            n                     手机号码，可选，用于 type = quickp2p 情况
//    private static final String phonenumber                ="phonenumber";

//    private static final String key        ="apikey";
    //signature    数据签名    32    是    　
//    private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(cid, channelWrapper.getAPI_MEMBERID());
                put(uid, handlerUtil.getRandomStr(6));
                put(time,StringToTimestamp(DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss"))+"");
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(order_id,channelWrapper.getAPI_ORDER_ID());
                put(ip,channelWrapper.getAPI_Client_IP());
                
                //type                   n                      存款方式，可选，值如下：         remit: 银行卡转账 ,   qrcode: 二维码存款,  online: 在线网银         quick: 快捷支付,  quickp2p: 快捷直连如果没有带入此参数，则自动进去YF收付的收银台，由会员选择存款方式
                //tflag                  n                     存款银行，可选，如果没有传入 type 参数，则此参数无效。如下：    ALIPAY: 支付宝 , WebMM: 微信支付,  QQPAY: QQ钱包    ABC: 农业银行 … (见后面的银行编码表)
                if (handlerUtil.isWEBWAPAPP_SM(channelWrapper) || handlerUtil.isWapOrApp(channelWrapper)) {
                    //type                   n                      存款方式，可选，值如下：         remit: 银行卡转账 ,   qrcode: 二维码存款,  online: 在线网银         quick: 快捷支付,  quickp2p: 快捷直连如果没有带入此参数，则自动进去YF收付的收银台，由会员选择存款方式
                    put(type,"qrcode");
                    //                  n                     存款银行，可选，如果没有传入 type 参数，则此参数无效。如下：    ALIPAY: 支付宝 , WebMM: 微信支付,  QQPAY: QQ钱包    ABC: 农业银行 … (见后面的银行编码表)
                    put(tflag,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }else if (handlerUtil.isWY(channelWrapper)) {
                    //type                   n                      存款方式，可选，值如下：         remit: 银行卡转账 ,   qrcode: 二维码存款,  : 在线网银         quick: 快捷支付,  quickp2p: 快捷直连如果没有带入此参数，则自动进去YF收付的收银台，由会员选择存款方式
                    put(type,"online");
                    //                  n                     存款银行，可选，如果没有传入 type 参数，则此参数无效。如下：    ALIPAY: 支付宝 , WebMM: 微信支付,  QQPAY: QQ钱包    ABC: 农业银行 … (见后面的银行编码表)
                    put(tflag,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }else if (handlerUtil.isWebYlKjzf(channelWrapper)) {
                    //Joey 
                    //@小哥哥丫 @彩7๑小月亮 不好意思，让贵司久等了，快捷维护完成了，如下图，在烦请代入以下参数cid，uid，time，amount，order_id，ip，sign，type，而type = quick，就可以在输入客户的卡号
                    put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                    put(tflag,"ABC");
                }
                put(syncurl,channelWrapper.getAPI_WEB_URL());
            }
        };
        log.debug("[友付支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(cid+"=").append(api_response_params.get(cid)).append("&");
        signSrc.append(uid+"=").append(api_response_params.get(uid)).append("&");
        signSrc.append(time+"=").append(api_response_params.get(time)).append("&");
        signSrc.append(amount+"=").append(api_response_params.get(amount)).append("&");
        signSrc.append(order_id+"=").append(api_response_params.get(order_id)).append("&");
        signSrc.append(ip+"=").append(api_response_params.get(ip));
        String paramsStr = signSrc.toString();
        String signMd5 = null;
        try {
            signMd5 = HmacSHA1Encrypt1(paramsStr, channelWrapper.getAPI_KEY());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[友付支付]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        log.debug("[友付支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }
    
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
      //支付宝扫码，不允许电脑上直接扫码  这方法靠谱
//        if ((handlerUtil.isWEBWAPAPP_SM(channelWrapper) && HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM())) || 
//                channelWrapper.getAPI_ORDER_ID().startsWith("T") || 
//                handlerUtil.isWapOrApp(channelWrapper)) {
//            StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
//            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
//            //保存第三方返回值
//            result.put(HTMLCONTEXT,htmlContent.toString());
//        }else if(handlerUtil.isWY(channelWrapper)){
//            StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
//            result.put(HTMLCONTEXT,htmlContent.toString());
//        }else{
//            throw new PayException("请在APP或者WAP应用上使用通道......");
//        }
        
        StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);
        result.put(HTMLCONTEXT,htmlContent.toString());
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[友付支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[友付支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
    /**
     * String(yyyy-MM-dd HH:mm:ss)转10位时间戳
     * @param time
     * @return
     * @throws PayException 
     */
    public static Integer StringToTimestamp(String time) throws PayException{
        int times = 0;
        try {  
            times = (int) ((Timestamp.valueOf(time).getTime())/1000);  
        } catch (Exception e) {  
            e.printStackTrace();  
        }
        if(times==0){
            throw new PayException(SERVER_MSG.REQUEST_PAY_INFO__ERROR);
        }
        return times; 
    }
    
    private static final String MAC_NAME1 = "HmacSHA1";
    /**
     * 使用 HMAC-SHA1 签名方法对对encryptText进行签名
     * @param encryptText 被签名的字符串
     * @param encryptKey  密钥
     * @return
     * @throws Exception
     */
    public static String HmacSHA1Encrypt1(String encryptText, String encryptKey) throws Exception
    {
        SecretKeySpec signingKey = new SecretKeySpec(encryptKey.getBytes(), MAC_NAME1);
        Mac mac = Mac.getInstance(MAC_NAME1);
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(encryptText.getBytes());
        String result = Base64.getEncoder().encodeToString(rawHmac);

        return result;
    }
}