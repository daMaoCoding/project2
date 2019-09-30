package dc.pay.business.xinxuanyangzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
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
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jun 14, 2019
 */
@RequestPayHandler("XINXUANYANGZHIFU")
public final class XinXuanYangZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(XinXuanYangZhiFuPayRequestHandler.class);

    //字段  类型  是否必填    最大长度    描述
    //version string  M   10  接口版本,固定值:1.0
    private static final String version                    ="version";
    //method  string  M   4   网关支付:web、API支付:json,默认web
    private static final String method                    ="method";
    //act int M   1   到账类型:固定值0
    private static final String act                    ="act";
    //mchid   int M   10  商户编号
    private static final String mchid                    ="mchid";
    //amount  float   M   10  订单金额:单位元,精确到分。如10.00
    private static final String amount                    ="amount";
    //out_trade_id    string  M   100 商户订单号
    private static final String out_trade_id                    ="out_trade_id";
    //type    int M   10  通道编号,说明见公共参数
    private static final String type                    ="type";
    //callback_url    String  M   255 异步通知地址:无需URL编码可带参数,参数发送方式:POST(上分逻辑处理)
    private static final String callback_url                    ="callback_url";
    //return_url  String  M   255 同步通知地址:支付完成返回地址,参数发送方式:GET(请勿做回调上分验证)
    private static final String return_url                    ="return_url";
    //applydate   String  M   50  下单时间:格式YmdHis,如:20180120142910
    private static final String applydate                    ="applydate";
    //bankcode    string  O   10  银行编码,说明见公共参数
    private static final String bankcode                    ="bankcode";
    //goodsname   String  O   50  商品名称
//    private static final String goodsname                    ="goodsname";
    //attach  String  O   50  备注信息:原路返回提交字符串,一般用于识别校验
//    private static final String attach                    ="attach";
    //signtype    int M   10  签名类型:0-MD5 1-RSA 默认:0
    private static final String signtype                    ="signtype";
    //sign    String  M   1000    报文签名:非空字段ASCII排序后+商户密钥进行md5加密后转大写。签名说明    amount=10.00&applydate=2018-12-25 21:11:04&callback_url=http://aa.com/notify.php&mchid=10005&out_trade_id=ZF181225211058560658&return_url=http://aa.com/return.php&type=1&key=c7520a496eb8a5318b7dde4e8261a881b17fe236
//    private static final String sign                    ="sign";
        
    private static final String key                    ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 3) {
            log.error("[新烜洋支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号mchid&通道编号type（向第三方获取当前使用编码值）&银行编码bankcode（向第三方获取当前使用编码值）" );
            throw new PayException("[新烜洋支付]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号mchid&通道编号type（向第三方获取当前使用编码值）&银行编码bankcode（向第三方获取当前使用编码值）" );
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(version,"1.0");
                put(method,"web");
                put(act,"0");
                put(mchid, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(out_trade_id,channelWrapper.getAPI_ORDER_ID());
//                put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(type,channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(callback_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(return_url,channelWrapper.getAPI_WEB_URL());
                put(applydate,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
//                put(bankcode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                put(bankcode,channelWrapper.getAPI_MEMBERID().split("&")[2]);
                put(signtype,"0");            
            }
        };
        log.debug("[新烜洋支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
                signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
            }
        }
        //最后一个&转换成#
        //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
        //删除最后一个字符
        //signSrc.deleteCharAt(paramsStr.length()-1);
        signSrc.append(key + "="+channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[新烜洋支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
//        if (HandlerUtil.isWEBWAPAPP_SM(channelWrapper)) {
        if (true) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }
        
//        else{
//
////          String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParam),MediaType.APPLICATION_JSON_VALUE);
//          String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST,defaultHeaders);
////          String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
//          if (StringUtils.isBlank(resultStr)) {
////              log.error("[新烜洋支付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
////              throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
//              log.error("[新烜洋支付]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//              throw new PayException(resultStr);
//          }
//          resultStr = UnicodeUtil.unicodeToString(resultStr);
//          JSONObject resJson = JSONObject.parseObject(resultStr);
//          //只取正确的值，其他情况抛出异常
//          if (null != resJson && resJson.containsKey("code") && "1".equalsIgnoreCase(resJson.getString("code"))  && 
//                  resJson.containsKey("payurl") && StringUtils.isNotBlank(resJson.getString("payurl"))) {
//                  result.put(JUMPURL, resJson.getString("payurl"));
//
////              if (handlerUtil.isWapOrApp(channelWrapper)) {
////                  result.put(JUMPURL, resJson.getString("payurl"));
////              }else {
////                  try {
//////                      result.put(QRCONTEXT, URLDecoder.decode(resJson.getString("payurl"), "UTF-8"));
////                      result.put(JUMPURL, URLDecoder.decode(resJson.getString("payurl"), "UTF-8"));
////                  } catch (UnsupportedEncodingException e) {
////                      e.printStackTrace();
////                      log.error("[新烜洋支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
////                      throw new PayException(resultStr);
////                  }
////              }
//          }else {
//              log.error("[新烜洋支付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
//              throw new PayException(resultStr);
//          }
//        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[新烜洋支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[新烜洋支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}