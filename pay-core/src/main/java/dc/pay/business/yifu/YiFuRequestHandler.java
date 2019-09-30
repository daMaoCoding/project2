package dc.pay.business.yifu;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * Jan 2, 2018
 */
@RequestPayHandler("YIFU")
public final class YiFuRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YiFuRequestHandler.class);

    //扫码属性
    private static final String QRCONTEXT = "QrContext";
	private static final String QRURL = "QrUrl";
	private static final String HTMLCONTEXT = "HtmlContext";
	//wap跳转属性
	private static final String JUMPURL = "JUMPURL";
	
	private static final String src_code		="src_code";
	private static final String out_trade_no    ="out_trade_no";
	private static final String total_fee       ="total_fee";
	private static final String time_start      ="time_start";
	private static final String goods_name      ="goods_name";
	private static final String trade_type      ="trade_type";
	private static final String finish_url      ="finish_url";
	private static final String mchid           ="mchid";
	private static final String extend          ="extend";
	private static final String bankName        ="bankName";
	private static final String cardType        ="cardType";
	private static final String key				="key";
	
    /**
     * 封装第三方所需要的参数
     * 
     * @return
     * @throws PayException
     * @author andrew
     * Jan 2, 2018
     */
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
    	if (null == channelWrapper.getAPI_MEMBERID() || !channelWrapper.getAPI_MEMBERID().contains("&")) {
			throw new PayException("商户号格式错误。正确格式如：src_code&mchid");
		}
    	ObjectMapper mapper = new ObjectMapper();
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(src_code, channelWrapper.getAPI_MEMBERID().split("&")[0]);
                put(mchid, channelWrapper.getAPI_MEMBERID().split("&")[1]);
                put(time_start ,DateUtil.formatDateTimeStrByParam("yyyyMMddHHmmss"));
                put(goods_name ,  "goods_name");
                put(total_fee ,  channelWrapper.getAPI_AMOUNT());
                put(finish_url,HandlerUtil.getDomain(channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL()));
                put(trade_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
                //网银
                if(channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WY_.name())){
                	put(trade_type,"80103");
                	Map<String, String> param = new TreeMap<String, String>();
                	param.put(cardType,"借记卡");
                	param.put(bankName,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                	try {
                		put(extend,mapper.writeValueAsString(param));
					} catch (Exception e) {
						
					}
                }else {
                	put(trade_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				}
            }
        };
        log.debug("[易付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }

    /**
     * 生成签名
     * 
     * @param api_response_params
     * @return
     * @throws PayException
     * @author andrew
     * Jan 2, 2018
     */
    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
    	Map<String,String> map = new TreeMap<>();
    	map = api_response_params;
    	//1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
    	//2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        List paramKeys = MapUtils.sortMapByKeyAsc(map);
        StringBuilder signSrc = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
        	if (null != map.get(paramKeys.get(i)) && StringUtils.isNotBlank(map.get(paramKeys.get(i)))) {
        		signSrc.append(paramKeys.get(i)).append("=").append(map.get(paramKeys.get(i))).append("&");
			}
        }
        signSrc.append(key+"=" + channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[易付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5)+"，参数："+paramsStr);
        return signMd5;
    }

    /**
     * 生成返回给RequestPayResult对象detail字段的值
     * 
     * @param payParam
     * @param pay_md5sign
     * @return
     * @throws PayException
     * @author andrew
     * Dec 5, 2017
     */
    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
    	payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        List<Map<String, String>> payResultList = Lists.newArrayList();
        String api_channel_bank_name = channelWrapper.getAPI_CHANNEL_BANK_NAME();
        String api_CHANNEL_BANK_URL = channelWrapper.getAPI_CHANNEL_BANK_URL();
        Map<String,String> result = Maps.newHashMap();
    	String tmpStr = null;
    	JSONObject jsonObject = null;
    	try {
    		tmpStr = RestTemplateUtil.postForm(api_CHANNEL_BANK_URL, payParam,"UTF-8").trim();
            if (null == tmpStr || StringUtils.isBlank(tmpStr)) {
            	log.error("[易付]3.1.发送支付请求，获取支付请求返回值异常:返回空");
            	throw new PayException("返回空");
            }
            jsonObject = JSONObject.parseObject(tmpStr);
            //支付成功返回：00；支付失败则返回其它
            if (null == jsonObject || (jsonObject.containsKey("respcd")  && !"0000".equals(jsonObject.getString("respcd")))) {
            	log.error("[易付]3.2.发送支付请求，获取支付请求返回值异常:"+HandlerUtil.UrlDecode(jsonObject.getString("respmsg")));
            	throw new PayException(HandlerUtil.UrlDecode(jsonObject.getString("respmsg")));
            }
    	} catch (Exception e) {
    		log.error("[易付]3.3.发送支付请求，及获取支付请求出错，订单号：" + channelWrapper.getAPI_ORDER_ID() + ",通道名称：" + api_channel_bank_name + ",postUrl:" + api_CHANNEL_BANK_URL + ",payForManagerCGIResultJsonObj" + e.getMessage(), e);
    		throw new PayException(e.getMessage(),e);
    	}
        String dataStr = jsonObject.getString("data");
        if (null == dataStr || StringUtils.isBlank(dataStr)) {
        	log.error("[易付]3.4.发送支付请求，获取支付请求返回值异常:data返回空");
        	throw new PayException("data返回空");
        }
        JSONObject data = JSONObject.parseObject(dataStr);
        //按不同的请求接口，向不同的属性设置值
        if(HandlerUtil.isWY(channelWrapper) || channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WAP_.name()) || channelWrapper.getAPI_CHANNEL_BANK_NAME().contains(PayEumeration.CHANNEL_TYPE._WAPAPP_.name())) {
        	result.put(JUMPURL, data.getString("pay_params"));
        }else{
        	result.put(QRCONTEXT, data.getString("pay_params"));
        }
        result.put("第三方返回",tmpStr); //保存全部第三方信息，上面的拆开没必要
        payResultList.add(result);
        log.debug("[易付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
        return payResultList;
    }

    /**
     * 生成返回给前端使用的值
     * 
     * @param resultListMap
     * @return
     * @throws PayException
     * @author andrew
     * Jan 2, 2018
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
        log.debug("[易付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}