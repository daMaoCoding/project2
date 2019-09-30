package dc.pay.business.yuanqi;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;

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
import dc.pay.utils.DesUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.JsonUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Dec 5, 2017
 */
@RequestPayHandler("YUANQI")
public final class YuanQiRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YuanQiRequestHandler.class);

    //扫码属性
    private static final String QRCONTEXT = "QrContext";
	private static final String QRURL = "QrUrl";
	private static final String HTMLCONTEXT = "HtmlContext";
	//wap跳转属性
	private static final String JUMPURL = "JUMPURL";
	
    //ordercode		订单号	商户唯一	Varchar(24)----数字加字母格式
	private static final String ordercode  ="ordercode";
    //amount		金额		元为单位	
	private static final String amount  ="amount";
    //goodsId		交易交品号			132：银联扫码   142：微信     152：QQ钱包     172：支付宝    232：PC快捷   242：WAP快捷  252：苏宁钱包    212：京东钱包   213：五码合一
	private static final String goodsId  ="goodsId";
    //statedate		交易日期			YYYYMMDD年月日
	private static final String statedate  ="statedate";
    //merNo			商户号		
	private static final String merNo  ="merNo";
    //callbackurl	回调地址			系统通知回调信息
	private static final String callbackurl  ="callbackurl";
    //callbackMemo	回调附加信息		回调时原样送回
	private static final String callbackMemo  ="callbackMemo";
	
	//version		版本号	01	固定值：01
	private static final String version  ="version";
	//ret_url		返回URL		结果返回URL，仅适用于立即返回处理结果的接口。创新支付处理完请求
	private static final String ret_url  ="ret_url";
	//sign			签名字段		数据的加密校验字符串，目前只支持使用MD5签名算法对待签名数据进行签名
	private static final String sign  ="sign";
	//bankname		银行名称		输入中文，比如“农业银行”（与表1中的‘银行名称’必须一致）	--仅网银支付需提交该参数（产品代码为192）
	private static final String bankname  ="bankname";

    /**
     * 封装第三方所需要的参数
     * 
     * @return
     * @throws PayException
     * @author andrew
     * Dec 30, 2017
     */
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        //channelWrapper.setAPI_ORDER_ID(System.currentTimeMillis()+""); //// TODO: 2018/2/27);
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(merNo, channelWrapper.getAPI_MEMBERID());
                put(amount,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(callbackurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(ordercode,channelWrapper.getAPI_ORDER_ID());

                //如果是网银支付
                 if(HandlerUtil.isWY(channelWrapper)){
                	put(ret_url,channelWrapper.getAPI_WEB_URL());
                	put(bankname,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                	put(version,"01");
                	put(goodsId,"192");
                	put(callbackMemo,"01");
                }else {
                     if(HandlerUtil.isYLKJ(channelWrapper)){
                         if(HandlerUtil.isOrderFromWapOrApp(channelWrapper.getAPI_ORDER_FROM())){
                             put(goodsId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                         }else{
                             put(goodsId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                         }
                     }else{
                         put(goodsId,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                     }
                	put(statedate, DateUtil.formatDateTimeStrByParam("YYYYMMDD"));
				}
            }
        };
        log.debug("[元启]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    /**
     * 生成签名
     * 
     * @param api_response_params
     * @return
     * @throws PayException
     * @author andrew
     * Dec 5, 2017
     */
    protected String buildPaySign(Map api_response_params) throws PayException {
    	String desEncrypt = null;
    	//网银
    	if(HandlerUtil.isWY(channelWrapper)){
			//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
			//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
			StringBuffer signSrc= new StringBuffer();
			signSrc.append(api_response_params.get(ordercode).toString());
			signSrc.append(HandlerUtil.getFen(api_response_params.get(amount).toString()));
			signSrc.append(api_response_params.get(goodsId));
			signSrc.append(channelWrapper.getAPI_KEY());
			String paramsStr = signSrc.toString();
			desEncrypt = HandlerUtil.getMD5UpperCase(paramsStr,"UTF-8").toLowerCase();
			log.debug("[元启]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(desEncrypt));
    	}else {
    		/*
	    	  待加密数据是请求参数按照以下方式组装成的字符串：
			请求参数按照接口要求参数生成标准的JSON串。
			将JSON串用加密验证码进行全部加密。加密验证码商户可通过登录商户平台系统下载，假设安全校验码为12345678.
			注意事项：
			没有值的参数无需传递，也无需包含到待加密数据中。
			加密时将字符转化成字节流时指定的字符集与utf-8保持一致。
    		 */
    		String stringify = JsonUtil.stringify(api_response_params);
    		try {
    			desEncrypt = DesUtil.desEncrypt(stringify,channelWrapper.getAPI_KEY());
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    		log.debug("[元启]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(desEncrypt));
		}
        return desEncrypt;
    }

    /**
     * 生成返回给RequestPayResult对象detail字段的值
     * 
     * @param payParam
     * @param pay_md5sign
     * @return
     * @throws PayException
     * @author andrew
     * Dec 30, 2017
     */
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        Map<String,String> result = Maps.newHashMap();
        if(HandlerUtil.isWY(channelWrapper) ){
        	payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        	//保存第三方返回值
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
        }else{
        	String api_CHANNEL_BANK_URL = channelWrapper.getAPI_CHANNEL_BANK_URL()+"initOrder?merNo="+payParam.get(merNo);
            String resultStr = RestTemplateUtil.postStr(api_CHANNEL_BANK_URL, pay_md5sign,MediaType.ALL_VALUE.toString(),"Keep-Alive");
            if (StringUtils.isBlank(resultStr)) {
                log.error("[元启]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
            JSONObject jsonObject = null;
            try {
                jsonObject = JSONObject.parseObject(DesUtil.desDecrypt(resultStr,  channelWrapper.getAPI_KEY()));
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[元启]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(jsonObject) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(JSON.toJSONString(jsonObject));
            }
            //200：下单成功 404：下单不成功 504：请更换订单号重新提交
            if (null == jsonObject || (jsonObject.containsKey("result")  && !"200".equals(jsonObject.getString("result")))) {
                log.error("[元启]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(jsonObject) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(JSON.toJSONString(jsonObject));
            }
            //按不同的请求接口，向不同的属性设置值
            if(HandlerUtil.isWapOrApp(channelWrapper) || HandlerUtil.isYLKJ(channelWrapper)) {
//              result.put(JUMPURL, QRCodeUtil.decodeByUrl(jsonObject.get("imageValue").toString()));
                result.put(JUMPURL, jsonObject.get("codeUrl").toString());
            }else{
//              result.put(QRCONTEXT, QRCodeUtil.decodeByUrl(jsonObject.get("imageValue").toString()));
                result.put(QRCONTEXT, jsonObject.get("codeUrl").toString());
            }
            result.put("第三方返回",jsonObject.toString()); //保存全部第三方信息，上面的拆开没必要
        }
        List<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[元启]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    /**
     * 生成返回给前端使用的值
     * 
     * @param resultListMap
     * @return
     * @throws PayException
     * @author andrew
     * Dec 30, 2017
     */
    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (CollectionUtils.isEmpty(resultListMap) || resultListMap.size() != 1) {
        	throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
		}
        Map<String, String> qrMap = resultListMap.get(0);
        if (null != qrMap && qrMap.containsKey(QRCONTEXT)) {
        	requestPayResult.setRequestPayQRcodeContent(qrMap.get(QRCONTEXT));
        }else if (null != qrMap && qrMap.containsKey(HTMLCONTEXT)) {
        	requestPayResult.setRequestPayHtmlContent(qrMap.get(HTMLCONTEXT));
        }else if (null != qrMap && qrMap.containsKey(JUMPURL)) {
            requestPayResult.setRequestPayJumpToUrl(qrMap.get(JUMPURL));
        }
        requestPayResult.setRequestPayamount(channelWrapper.getAPI_AMOUNT());
        requestPayResult.setRequestPayOrderId(channelWrapper.getAPI_ORDER_ID());
        requestPayResult.setRequestPayOrderCreateTime(HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyy-MM-dd HH:mm:ss"));
        requestPayResult.setRequestPayQRcodeURL(null);
        requestPayResult.setRequestPayChannelBankName(channelWrapper.getAPI_CHANNEL_BANK_NAME());
        if (!ValidateUtil.requestesultValdata(requestPayResult)) {
        	throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
        }
        requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
        log.debug("[元启]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}