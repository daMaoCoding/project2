package dc.pay.business.xinabazhifu2;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
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
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author andrew
 * Sep 9, 2019
 */
@RequestPayHandler("XINABAZHIFU2")
public final class XinABaZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinABaZhiFu2PayRequestHandler.class);

    //下列数据项为所有业务上送数据的公共数据项
    //上送数据公共字段
    //参数名称 中文名称 是否必填 参与签名 参数说明
    //merchantNo 商户号 是 是 平台分配的商户号
    private static final String merchantNo                ="merchantNo";
    //businessType 业务类型标识 是 是 order: 订单类型, query: 订单状态查询,
    private static final String businessType                ="businessType";
    //timeStamp 时间戳 是 是 系统当前时间戳(长整型数值串)
    private static final String timeStamp                ="timeStamp";
    //ipAddr 客户端 IP 地址 是 是 请填写真实下单客户 ip 地址
    private static final String ipAddr                ="ipAddr";
    //data 业务数据封装 是 是 将业务数据转为 json 字符串后. 再次使用 base64 编码. 存入此字段中
    private static final String data                ="data";
    //sign 数据签名 是 否 见本手册加解密章节
//    private static final String sign                ="sign";
    
    //参数名称 中文名称 是否必填 适用通道类型 参数说明
    //order_no 商户订单号 是 全部 在同一商户下需保证唯一。 最大长度 30 个英文字符
    private static final String order_no                ="order_no";
    //order_money 订单金额(分) 是 全部 订单金额采用分为计算单位. 不带小数点的正整数. 例如 1 元. 需要转换为 100 分
    private static final String order_money                ="order_money";
    //channel 支付通道编码 是 全部 支付通道编码。 见 “支付通道编码信息” 表
    private static final String channel                ="channel";
    //sync_url 同步通知地址 是 全部 支付成功后前端跳转地址
    private static final String sync_url                ="sync_url";
    //async_url 异步通知地址 是 全部 支付成功后异步回调地址, post 方式调用。发送数据格式为 json
    private static final String async_url                ="async_url";
    //extend 客户端扩展字段 是 全部 回调时原样返回
    private static final String extend                ="extend";
    //cardNo 卡号 是 快捷支付
//    private static final String cardNo                ="cardNo";
    //cardAccount 持卡人姓名 是 快捷支付
//    private static final String cardAccount                ="cardAccount";
    //cardPhone 手机号码 是 快捷支付
//    private static final String cardPhone                ="cardPhone";
    
    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
            log.error("[新A8支付2]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
            throw new PayException("[新A8支付2]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号&通道类型编码值（向第三方获取当前使用通道编码值）" );
        }
        String time = System.currentTimeMillis()+"";
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(order_no,channelWrapper.getAPI_ORDER_ID());
                put(order_money,  channelWrapper.getAPI_AMOUNT());
                put(channel,channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(sync_url,channelWrapper.getAPI_WEB_URL());
                put(async_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(extend,channelWrapper.getAPI_Client_IP()+"&"+time);
                
                put(merchantNo, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(businessType, "order");
                put(timeStamp, time);
                put(ipAddr, channelWrapper.getAPI_Client_IP());
            }
        };
        log.debug("[新A8支付2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        
        Map<String,String> map = new TreeMap<>();
        map.put(order_no,api_response_params.get(order_no));
        map.put(order_money, api_response_params.get(order_money));
        map.put(channel,api_response_params.get(channel));
        map.put(sync_url,api_response_params.get(sync_url));
        map.put(async_url,api_response_params.get(async_url));
        map.put(extend,api_response_params.get(extend));
        
        byte[] encode = Base64.getEncoder().encode(JSON.toJSONString(map).getBytes());
        String my_data = null;
        try {
            my_data = new String(encode,"utf-8");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("[新A8支付2]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        };
        
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        StringBuffer signSrc= new StringBuffer();
        signSrc.append(businessType+"=").append(api_response_params.get(businessType)).append("&");
        signSrc.append(data+"=").append(my_data).append("&");
        signSrc.append(ipAddr+"=").append(api_response_params.get(ipAddr)).append("&");
        signSrc.append(merchantNo+"=").append(api_response_params.get(merchantNo)).append("&");
        signSrc.append(timeStamp+"=").append(api_response_params.get(timeStamp)).append("&");
        signSrc.append(key+"=").append(channelWrapper.getAPI_KEY());
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        String paramsStr = signSrc.toString();
//        System.out.println("签名源串paramsStr=========>"+paramsStr);
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        
//        System.out.println("签名源串map=========>"+JSON.toJSONString(map));
        log.debug("[新A8支付2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> api_response_params, String pay_md5sign) throws PayException {
        
        Map<String,String> my_map = new TreeMap<>();
        my_map.put(order_no,api_response_params.get(order_no));
        my_map.put(order_money, api_response_params.get(order_money));
        my_map.put(channel,api_response_params.get(channel));
        my_map.put(sync_url,api_response_params.get(sync_url));
        my_map.put(async_url,api_response_params.get(async_url));
        my_map.put(extend,api_response_params.get(extend));
        
        byte[] encode = Base64.getEncoder().encode(JSON.toJSONString(my_map).getBytes());
        String my_data = null;
        try {
            my_data = new String(encode,"utf-8");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("[新A8支付2]-[请求支付]-2.生成加密URL签名出错，签名出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        };
        
        Map<String,String> map = new LinkedHashMap<>();
        map.put(businessType, api_response_params.get(businessType));
        map.put(data, my_data);
        map.put(ipAddr, api_response_params.get(ipAddr));
        map.put(merchantNo, api_response_params.get(merchantNo));
        map.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        map.put(timeStamp, api_response_params.get(timeStamp));
//        System.out.println("请求地址=========>"+channelWrapper.getAPI_CHANNEL_BANK_URL());
//        System.out.println("请求参数api_response_params=========>"+JSON.toJSONString(api_response_params));
//        System.out.println("请求参数map=========>"+JSON.toJSONString(map));
        Map<String,String> result = Maps.newHashMap();
//        if(HandlerUtil.isWY(channelWrapper) || HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)){
        if(false){
            StringBuffer htmlContent = HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), map);
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
            //保存第三方返回值
            result.put(HTMLCONTEXT,htmlContent.toString());
        }else{
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), map,"UTF-8");
//            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), map);
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[新A8支付2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[新A8支付2]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
//            System.out.println("请求返回=========>"+resultStr);
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //    log.error("[新A8支付2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //}

            try {
                JSONObject jsonObject = JSONObject.parseObject(new String(resultStr.getBytes("ISO-8859-1"), "GBK"));                
                //只取正确的值，其他情况抛出异常
                if (null != jsonObject && jsonObject.containsKey("success") && "true".equalsIgnoreCase(jsonObject.getString("success"))  && 
                        jsonObject.containsKey("data") && StringUtils.isNotBlank(jsonObject.getString("data"))
                ){
                    String string = new String(Base64.getDecoder().decode(jsonObject.getString("data").getBytes()),"utf-8");
//                    System.out.println("请求返回string=========>"+string);
                    JSONObject parseObject2 = JSONObject.parseObject(string);
//                 jsonObject.getJSONObject("data").containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getJSONObject("data").getString("pay_url")))
                if (null != parseObject2 &&  parseObject2.containsKey("payUrl") && StringUtils.isNotBlank(parseObject2.getString("payUrl"))) {
                        result.put( JUMPURL, parseObject2.getString("payUrl"));
                }else {
                    log.error("[新A8支付2]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
                }else {
                    log.error("[新A8支付2]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            } catch (Exception e) {
                log.error("[新A8支付2]-[请求支付]-3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + channelWrapper.getAPI_CHANNEL_BANK_NAME() + ",postUrl:" + channelWrapper.getAPI_CHANNEL_BANK_URL() + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
                //log.error("[盛盈付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + resultStr + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(e.getMessage(),e);
            }
        }
        List<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新A8支付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新A8支付2]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}