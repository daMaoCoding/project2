package dc.pay.business.xingrubaozhifu;

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
 * 
 * @author andrew
 * Aug 5, 2019
 */
@RequestPayHandler("XINGRUBAOZHIFU")
public final class XingRuBaoZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XingRuBaoZhiFuPayRequestHandler.class);

  //#   参数名 含义  类型  说明
    //1   uid 商户uid   string(18)  必填。您的商户唯一标识，注册后在设置里获得。一个18位字符串
    private static final String uid                ="uid";
    //2   price   价格  float   必填。单位：元。精确小数点后2位
    private static final String price                ="price";
    //3   istype  支付渠道    int 必填。1：支付宝；2：微信支付；3：QQ；4：京东；5：网关；6：银联扫码；7：银联快捷 所有通道支持电脑+手机H5页面自动识别
    private static final String istype                ="istype";
    //4   notify_url  通知回调网址  string(255) 必填。用户支付成功后，我们服务器会主动发送一个post消息到这个网址。由您自定义。不要urlencode。例：http://www.aaa.com/xingrubao_notify
    private static final String notify_url                ="notify_url";
    //5   return_url  跳转网址    string(255) 必填。用户支付成功后，我们会让用户浏览器自动跳转到这个网址。由您自定义。不要urlencode。例：http://www.aaa.com/xingrubao_return
    private static final String return_url                ="return_url";
    //6   orderid 商户自定义订单号    string(50)  必填。我们会据此判别是同一笔订单还是新订单。我们回调时，会带上这个参数。例：201710192541
    private static final String orderid                ="orderid";
    //7   orderuid    商户自定义客户号    string(100) 选填。我们会显示在您后台的订单列表中，方便您看到是哪个用户的付款，方便后台对账。强烈建议填写。可以填用户名，也可以填您数据库中的用户uid。例：xxx, xxx@aaa.com
    private static final String orderuid                ="orderuid";
    //8   goodsname   商品名称    string(100) 选填。您的商品名称，用来显示在后台的订单名称。如未设置，我们会使用后台商品管理中对应的商品名称
    private static final String goodsname                ="goodsname";
    //10  version 协议版本号   int 必填。当前为2
    private static final String version                ="version";
    //9   key 秘钥  string(32)  必填。把使用到的所有参数，连Token一起，按参数名字母升序排序。把参数名和参数值拼接在一起。做md5-32位加密，取字符串小写。得到key。网址类型的参数值不要urlencode。
//    private static final String key                ="key";
    
    private static final String key        ="token";
    //signature    数据签名    32    是    　
//    private static final String signature  ="signature";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
//        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
//        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 3) {
//            log.error("[星入宝支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//            throw new PayException("[星入宝支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&机构号" );
//        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(uid, channelWrapper.getAPI_MEMBERID());
                put(price,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(istype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(orderid,channelWrapper.getAPI_ORDER_ID());
                put(goodsname,"name");
                put(orderuid,handlerUtil.getRandomStr(6));
                put(version,"2");
            }
        };
        log.debug("[星入宝支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(goodsname+"=").append(api_response_params.get(goodsname)).append("&");
        signSrc.append(istype+"=").append(api_response_params.get(istype)).append("&");
        signSrc.append(notify_url+"=").append(api_response_params.get(notify_url)).append("&");
        signSrc.append(orderid+"=").append(api_response_params.get(orderid)).append("&");
        signSrc.append(orderuid+"=").append(api_response_params.get(orderuid)).append("&");
        signSrc.append(price+"=").append(api_response_params.get(price)).append("&");
        signSrc.append(return_url+"=").append(api_response_params.get(return_url)).append("&");
        signSrc.append(key+"=").append(channelWrapper.getAPI_KEY()).append("&");
        signSrc.append(uid+"=").append(api_response_params.get(uid)).append("&");
        signSrc.append(version+"=").append(api_response_params.get(version));
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[星入宝支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        HashMap<String, String> result = Maps.newHashMap();

//        if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
        if (true) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
        
//        else{
////            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
////            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            //if (StringUtils.isBlank(resultStr)) {
//            //    log.error("[星入宝支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//            //    throw new PayException(resultStr);
//            //    //log.error("[星入宝支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
//            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
//            //}
//            System.out.println("请求返回=========>"+resultStr);
//            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
//            //   log.error("[星入宝支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//            //   throw new PayException(resultStr);
//            //}
//            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
//            JSONObject jsonObject;
//            try {
//                jsonObject = JSONObject.parseObject(resultStr);
//            } catch (Exception e) {
//                e.printStackTrace();
//                log.error("[星入宝支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            //只取正确的值，其他情况抛出异常
//            //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
//            //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
//            // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
//            //){
//            if (null != jsonObject && jsonObject.containsKey("status") && "1".equalsIgnoreCase(jsonObject.getString("status"))  && jsonObject.containsKey("codeimg") && StringUtils.isNotBlank(jsonObject.getString("codeimg"))) {
//                String code_url = jsonObject.getString("codeimg");
//                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
//                //if (handlerUtil.isWapOrApp(channelWrapper)) {
//                //    result.put(JUMPURL, code_url);
//                //}else{
//                //    result.put(QRCONTEXT, code_url);
//                //}
//            }else {
//                log.error("[星入宝支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[星入宝支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[星入宝支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}