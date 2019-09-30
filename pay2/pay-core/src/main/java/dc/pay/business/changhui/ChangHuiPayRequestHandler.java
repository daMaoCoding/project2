package dc.pay.business.changhui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 *
 * 
 * @author kevin
 * Jul 20, 2018
 */
@RequestPayHandler("CHANGHUI")
public final class ChangHuiPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ChangHuiPayRequestHandler.class);

    private static final String      p0_Cmd	  	  = "p0_Cmd";                         //业务类型  Buy
    private static final String      p1_MerId	  = "p1_MerId";                       //商户编号
    private static final String      p2_Order	  = "p2_Order";                       //商户订单号
    private static final String      p3_Cur	      = "p3_Cur";                         //交易币种  CNY
    private static final String      p4_Amt	      = "p4_Amt";                         //支付金额  单位:元
    private static final String      p5_Pid	      = "p5_Pid";                         //商品名称
    private static final String      p6_Pcat	  = "p6_Pcat";                        //商品种类
    private static final String      p7_Pdesc	  = "p7_Pdesc";                       //商品描述
    private static final String      p8_Url	  	  = "p8_Url";                         //商户接收支付成功数据的地址
    private static final String      p9_MP	      = "p9_MP";                          //商户扩展信息   原样返回
    private static final String      pa_FrpId	  = "pa_FrpId";                       //支付通道编码
    private static final String      pg_BankCode  = "pg_BankCode";                    //网银编码
    private static final String      hmac	      = "hmac";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(p0_Cmd,"Buy");
                put(p1_MerId,channelWrapper.getAPI_MEMBERID());
                put(p2_Order,channelWrapper.getAPI_ORDER_ID());
                put(p3_Cur,"CNY");
                put(p4_Amt,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(p5_Pid,"PAY");
                put(p6_Pcat,"PAY-C");
                put(p7_Pdesc,"PAY-D");
                put(p8_Url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(p9_MP,"p9_MP");
                if(HandlerUtil.isWY(channelWrapper)) {
                	put(pa_FrpId,"OnlinePay");
                	put(pg_BankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                }else {
                	put(pa_FrpId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());	
                }
            }
        };
        log.debug("[畅汇]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(StringUtils.isBlank(api_response_params.get(paramKeys.get(i))) || hmac.equalsIgnoreCase(paramKeys.get(i).toString() )  )  //
                continue;
            sb.append(api_response_params.get(paramKeys.get(i)));
            
        }
        String signStr = sb.toString();
        //System.out.println("验签字符串=========>"+signStr);
        String pay_md5sign = HmacMd5Util.hmacSign(signStr, channelWrapper.getAPI_KEY());
        log.debug("[畅汇]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        Map result = Maps.newHashMap();
        //System.out.println("请求地址=========>"+channelWrapper.getAPI_CHANNEL_BANK_URL());
    	//System.out.println("请求参数=========>"+JSON.toJSONString(payParam));
        try {
            if (HandlerUtil.isWY(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)  ||  HandlerUtil.isWapOrApp(channelWrapper) ) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            }else{
            	String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam, String.class, HttpMethod.POST).trim();
                if (StringUtils.isBlank(resultStr)) {
                    log.error("[畅汇]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(payParam));
                    throw new PayException("返回空,参数："+JSON.toJSONString(payParam));
                }
                //System.out.println("请求返回=========>"+resultStr);
                JSONObject resJson = JSON.parseObject(resultStr);
                //只取正确的值，其他情况抛出异常
                if(null !=resJson && resJson.containsKey("r1_Code") && "1".equalsIgnoreCase(resJson.getString("r1_Code")) && resJson.containsKey("r3_PayInfo")){
                    result.put(QRCONTEXT, HandlerUtil.UrlDecode(resJson.getString("r3_PayInfo")));
                }else {
                	log.error("[畅汇]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                	throw new PayException(resultStr);
                }
            }
        } catch (Exception e) {
        	log.error("[畅汇]-[请求支付]-3.3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(null != e.getMessage() ? e.getMessage() : "请求第三方，返回Null",e);
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
        log.debug("[畅汇]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[畅汇]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }

}