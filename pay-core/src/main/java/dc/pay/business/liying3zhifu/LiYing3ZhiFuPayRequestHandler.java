package dc.pay.business.liying3zhifu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author andrew
 * Jan 23, 2019
 */
@RequestPayHandler("LIYING3ZHIFU")
public final class LiYing3ZhiFuPayRequestHandler extends PayRequestHandler {
   private static final Logger log = LoggerFactory.getLogger(LiYing3ZhiFuPayRequestHandler.class);

    private static final  String    mch_id  ="mch_id";                   //商户号          是   Y   String(32)  由系统分配小商户的商户号
    private static final  String    trade_type  ="trade_type";           //订单类型        是    Y   String(32)  交易类型
    private static final  String    out_trade_no    ="out_trade_no";     //商户订单号      是 Y   String(32)  商户订单号，18个字符内，确保唯一(纯数字串)
    private static final  String    body    ="body";                     //商品描述        否        String(100) 商品描述
    private static final  String    attach  ="attach";                   //附加信息        否        String(100) 商户附加信息，可做扩展参数
    private static final  String    total_fee   ="total_fee";            //总金额          是   Y   Int 总金额，以分为单位，不允许包含任何字符
    private static final  String    bank_id ="bank_id";                  //支付银行        是    Y   String(32)  付款银行代码，空白则跳转收银台（其他支付方式留空，参与验签）
    private static final  String    notify_url  ="notify_url";           //通知地址        是    Y   String(255) 接收推送通知的URL，确保外网可以访问
    private static final  String    return_url  ="return_url";           //返回商家地址    是  Y       支付完成后返回商家地址
    private static final  String    time_start  ="time_start";           //订单生成时间    是  Y   String(14)  订单生成时间，格式为 yyyymmddhhmmss
    private static final  String    nonce_str   ="nonce_str";            //随机字符串      是 Y   String(32)  随机英文及数字字符串，不长于 32 位
    private static final  String    sign    ="sign";                    //签名             是      String(128) 详见”签名说明”

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(mch_id,channelWrapper.getAPI_MEMBERID());
            payParam.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(total_fee,channelWrapper.getAPI_AMOUNT());
            if(HandlerUtil.isWY(channelWrapper)){//网银
                payParam.put(trade_type,"10");
                payParam.put(bank_id,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }else{
                payParam.put(trade_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                payParam.put(bank_id,"");
            }
            payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(return_url,channelWrapper.getAPI_WEB_URL());
            payParam.put(time_start, DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
            payParam.put(nonce_str,HandlerUtil.getRandomStrStartWithDate(5));
        }
        if(channelWrapper.getAPI_CHANNEL_BANK_URL().endsWith("/trade")){ //扫码接口无需参数  return_url  bank_id,（过接口地址判断参数）
            payParam.remove(return_url);
            payParam.remove(bank_id);
        }
        log.debug("[利盈3支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if( sign.equalsIgnoreCase(paramKeys.get(i).toString()))  //StringUtils.isBlank(params.get(paramKeys.get(i))) ||
                continue;
            //地址 url encode
            if( notify_url.equalsIgnoreCase(paramKeys.get(i).toString())  ||  return_url.equalsIgnoreCase(paramKeys.get(i).toString())  ){
                sb.append(paramKeys.get(i)).append("=").append(HandlerUtil.UrlEncode(params.get(paramKeys.get(i)) )).append("&");
            }else{
                sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
            }
        }
        sb.append("key="+channelWrapper.getAPI_KEY());//"key="+
        String pay_md5sign = HandlerUtil.getMD5UpperCase(sb.toString());
        log.debug("[利盈3支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return  pay_md5sign;

    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        payParam.put(body, body);
        payParam.put(attach, attach);
        Map result = Maps.newHashMap();
        if (true) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
        
//        else{
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//            //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[利盈3支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//                //log.error("[利盈3支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
//                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
//            }
//            System.out.println("请求返回=========>"+resultStr);
//            if (!resultStr.contains("{") || !resultStr.contains("}")) {
//               log.error("[利盈3支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//               throw new PayException(resultStr);
//            }
//            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
//            JSONObject jsonObject;
//            try {
//                jsonObject = JSONObject.parseObject(resultStr);
//            } catch (Exception e) {
//                e.printStackTrace();
//                log.error("[利盈3支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            //只取正确的值，其他情况抛出异常
//            //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
//            //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
//            // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
//            //){
//            if (null!=jsonObject && jsonObject.containsKey("status") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("status"))
//                    && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))
//                    ) {
//                String code_url = jsonObject.getString("code_url");
//                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
//                //if (handlerUtil.isWapOrApp(channelWrapper)) {
//                //    result.put(JUMPURL, code_url);
//                //}else{
//                //    result.put(QRCONTEXT, code_url);
//                //}
//            }else {
//                log.error("[利盈3支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[利盈3支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap,channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[利盈3支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

}