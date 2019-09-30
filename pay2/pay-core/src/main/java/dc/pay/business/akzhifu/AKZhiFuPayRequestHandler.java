package dc.pay.business.akzhifu;

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
 * Jul 30, 2019
 */
@RequestPayHandler("AKZHIFU")
public final class AKZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(AKZhiFuPayRequestHandler.class);

  //3.参数说明：
    //参数名称    变量名 类型长度    是否可空    说明
    //版本号 bb  varchar(5)  no  默认1.0
    private static final String bb                ="bb";
    //商户编号    shid    int(8)  no  商户后台获取
    private static final String shid                ="shid";
    //商户订单号   ddh varchar(20) no  商户自定义组成
    private static final String ddh                ="ddh";
    //订单金额    je  decimal(18,2)   no  精确到小数点后两位，例如10.24
    private static final String je                ="je";
    //支付通道    zftd    varchar(10) no  详见附录1
    private static final String zftd                ="zftd";
    //异步通知URL ybtz    varchar(50) no  不能带有任何参数
    private static final String ybtz                ="ybtz";
    //同步跳转URL tbtz    varchar(50) no  不能带有任何参数
    private static final String tbtz                ="tbtz";
    //订单名称    ddmc    varchar(50) no  自定义
    private static final String ddmc                ="ddmc";
    //订单备注    ddbz    varchar(50) no  自定义
    private static final String ddbz                ="ddbz";
    //md5签名串  sign    varchar(32) no  参照订单MD5签名
//    private static final String sign                ="sign";

//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[AK支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
            throw new PayException("[AK支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(bb,"1.0");
                put(shid, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(ddh,channelWrapper.getAPI_ORDER_ID());
                put(je,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
//                put(zftd,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(zftd,channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(ybtz,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(tbtz,channelWrapper.getAPI_WEB_URL());
                put(ddmc,"name");
                put(ddbz,"name");
            }
        };
        log.debug("[AK支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(shid+"=").append(api_response_params.get(shid)).append("&");
        signSrc.append(bb+"=").append(api_response_params.get(bb)).append("&");
        signSrc.append(zftd+"=").append(api_response_params.get(zftd)).append("&");
        signSrc.append(ddh+"=").append(api_response_params.get(ddh)).append("&");
        signSrc.append(je+"=").append(api_response_params.get(je)).append("&");
        signSrc.append(ddmc+"=").append(api_response_params.get(ddmc)).append("&");
        signSrc.append(ddbz+"=").append(api_response_params.get(ddbz)).append("&");
        signSrc.append(ybtz+"=").append(api_response_params.get(ybtz)).append("&");
        signSrc.append(tbtz+"=").append(api_response_params.get(tbtz)).append("&");
        signSrc.append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[AK支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }
    
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();

//        if (HandlerUtil.isWapOrApp(channelWrapper)) {
        if (true) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
        
//        else{
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST);
//            //String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//            if (StringUtils.isBlank(resultStr)) {
//                log.error("[AK支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//                //log.error("[AK支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
//                //throw new PayException("返回空,参数："+JSON.toJSONString(map));
//            }
//            System.out.println("请求返回=========>"+resultStr);
//            if (!resultStr.contains("{") || !resultStr.contains("}")) {
//               log.error("[AK支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//               throw new PayException(resultStr);
//            }
//            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
//            JSONObject jsonObject;
//            try {
//                jsonObject = JSONObject.parseObject(resultStr);
//            } catch (Exception e) {
//                e.printStackTrace();
//                log.error("[AK支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//            //只取正确的值，其他情况抛出异常
//            if (null != jsonObject && jsonObject.containsKey("status") && "1".equalsIgnoreCase(jsonObject.getString("status"))  && jsonObject.containsKey("codeimg") && StringUtils.isNotBlank(jsonObject.getString("codeimg"))) {
//                String code_url = jsonObject.getString("codeimg");
//                result.put( handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT, code_url);
//                //if (handlerUtil.isWapOrApp(channelWrapper)) {
//                //    result.put(JUMPURL, code_url);
//                //}else{
//                //    result.put(QRCONTEXT, code_url);
//                //}
//            }else {
//                log.error("[AK支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//                throw new PayException(resultStr);
//            }
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[AK支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[AK支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}