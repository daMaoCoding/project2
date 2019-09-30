package dc.pay.business.moneypay;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Aug 11, 2018
 */
@RequestPayHandler("MONEYPAY")
public final class MoneyPayPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(MoneyPayPayRequestHandler.class);

    //a)公共参数
    //参数名                类型               长度             是否可以为空         备注
    //app_id                String             Max(32)            Y                  为空
    //format                String             Max(40)            Y                  为空
    //timestamp             String             Max(19)            N                  发送请求的时间，格式"yyyyMMddHHmmss"
    //charset               String             Max(10)            N                  值为UTF-8
    //api_version           String             Max(4)             N                  接口版本默认1.5
    //client_ip             String             Max(15)            N                  参数名称：玩家ip：如198.33.223.12
    //biz_content           String                                N                  业务请求参数的集合，最大长度不限，除公共参数外所有请求参数都必须放在这个参数中传递，里面的内容是Json 字符串，而不是Json对象
    //sign                  String             Max(32)            N                  商户请求参数加密字符串
    //b)业务参数
    //参数名                   类型              长度          是否为空          备注
    //company_id               number           Max(8)           N               支付公司为商户分配的唯一账号
    //company_order_no         String           Max(32)          N               商户订单号
    //player_id                String           Max(64)          N               商户用户在商户系统存在的唯一ID
    //name                     String           Max(64)          Y               玩家部分真实姓名
    //card_no                  String           Max(64)          Y               玩家银行卡号或者支付宝账号（可不填写）
    //bank_addr                String           Max(128)                         开户行地址,channel_code 是3的话，该字段是必填字段
    //terminal                 number           Max(2)           N               1:mobile   2:pc
    //channel_code             number           Max(2)           N               1：支付宝
    //notify_url               String           Max(128)         Y               支付结果异步通知接口地址        如果为空，使用后台配置的回调接口地址；如果跟后台配置的接口不一致后，则使用后台默认配置的接口
    //amount_money             number           Max(8)           N               参数名称：商家订单金额  以元为单位，精确到小数点后两位.例如：12.01
    //extra_param              String           Max(1024)        Y               参数名称：业务扩展参数  目前为空字符串
    private static final String app_id                       ="app_id";
    private static final String format                       ="format";
    private static final String timestamp                    ="timestamp";
    private static final String charset                      ="charset";
    private static final String api_version                  ="api_version";
    private static final String client_ip                    ="client_ip";
    private static final String biz_content                  ="biz_content";
    private static final String company_id                   ="company_id";
    private static final String company_order_no             ="company_order_no";
    private static final String player_id                    ="player_id";
    private static final String name                         ="name";
    private static final String card_no                      ="card_no";
//    private static final String bank_addr                    ="bank_addr";
    private static final String terminal                     ="terminal";
    private static final String channel_code                 ="channel_code";
    private static final String notify_url                   ="notify_url";
    private static final String amount_money                 ="amount_money";
    private static final String extra_param                  ="extra_param";
    
//    private static final String trade_no                     ="trade_no";
//    private static final String actual_amount                ="actual_amount";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                //a)公共参数
                put(app_id,"");
                put(format,"");
                put(timestamp, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(charset,"UTF-8");
                put(api_version,"1.5");
                put(client_ip,channelWrapper.getAPI_Client_IP());
                //b)业务参数
                put(company_id, channelWrapper.getAPI_MEMBERID());
                put(company_order_no,channelWrapper.getAPI_ORDER_ID());
                put(player_id,  handlerUtil.getRandomStr(32));
                put(name,  handlerUtil.getRandomStr(32));
                put(channel_code,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(card_no,handlerUtil.getRandomStr(8));
                put(terminal,handlerUtil.isWapOrApp(channelWrapper) ? "1" : "2");
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(amount_money,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(extra_param,"");
            }
        };
        log.debug("[moneypay]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(company_id+"=").append(api_response_params.get(company_id)).append("&");
        signSrc.append(company_order_no+"=").append(api_response_params.get(company_order_no)).append("&");
        signSrc.append(player_id+"=").append(api_response_params.get(player_id)).append("&");
        signSrc.append(amount_money+"=").append(api_response_params.get(amount_money)).append("&");
        signSrc.append(api_version+"=").append(api_response_params.get(api_version)).append("&");
        signSrc.append(channel_code+"=").append(api_response_params.get(channel_code));
        signSrc.append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[moneypay]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        Map<String, String> tmp = new TreeMap<String, String>() {
            {
                put(company_id, payParam.get(company_id));
                put(company_order_no,payParam.get(company_order_no));
                put(player_id,payParam.get(player_id));
                put(terminal,payParam.get(terminal));
                put(channel_code,payParam.get(channel_code));
                put(notify_url,payParam.get(notify_url));
                put(amount_money,payParam.get(amount_money));
                put(extra_param,"");
                put(name,payParam.get(name));
                put(card_no,payParam.get(card_no));
            }
        };
        Map<String, String> param = new TreeMap<String, String>() {
            {
                put(app_id,"");
                put(format,"");
                put(timestamp, payParam.get(timestamp));
                put(charset,payParam.get(charset));
                put(api_version,payParam.get(api_version));
                put(client_ip,payParam.get(client_ip));
                put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);
                
//                put(biz_content, JSONObject.toJSONString(tmp));
                try {
                    //程序请求，则需要编码    拼接成html,则原始字符串
                    put(biz_content, handlerUtil.isWapOrApp(channelWrapper) ? JSONObject.toJSONString(tmp) : URLEncoder.encode(JSONObject.toJSONString(tmp),"UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
        HashMap<String, String> result = Maps.newHashMap();
    	if (HandlerUtil.isWapOrApp(channelWrapper)) {
          result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),param).toString().replace("method='post'","method='get'"));
        }
    	
//    	else{
////            String resultStr = RestTemplateUtil.sendByRestTemplate(channelWrapper.getAPI_CHANNEL_BANK_URL(), param, String.class, HttpMethod.GET);
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), param, String.class, HttpMethod.GET);
////            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), param,"UTF-8");
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[moneypay]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//                //log.error("[moneypay]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
//                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
//            }
//            System.out.println("请求返回=========>"+resultStr);
//            JSONObject resJson = JSONObject.parseObject(resultStr);
//            //只取正确的值，其他情况抛出异常
//            if (null != resJson && resJson.containsKey("status") && "1".equalsIgnoreCase(resJson.getString("status"))  && resJson.containsKey("codeimg") && StringUtils.isNotBlank(resJson.getString("codeimg"))) {
//                String code_url = resJson.getString("codeimg");
//                result.put(QRCONTEXT, code_url);
//            }else {
//                log.error("[moneypay]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//        }
        
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[moneypay]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[moneypay]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}