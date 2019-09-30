package dc.pay.business.baijiezhifu2;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

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
import dc.pay.utils.HttpUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * 
 * 
 * @author mikey
 * Jun 26, 2019
 */
@RequestPayHandler("BAIJIEZHIFU2")
public final class BaiJieZhiFu2PayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(BaiJieZhiFu2PayRequestHandler.class);

/*
    	数名			必选		类型			说明
    mchid			是		int			商户ID
    timestamp		是		string		时间戳(10位秒)
    nonce			是		string		随机码
    sign			是		string		签名
    data			是		Object		业务参数对象

    data对象的参数
    	参数名			必选		类型			说明
    ype				是		int			支付类型(100支付宝系列，200银行固码，更多请看支付类型说明)
    out_trade_no	是		string		商户订单号
    goodsname		是		string		商品名称
    total_fee		是		string		支付金额，如0.01，以字符格式保留两位小数
    notify_url		是		string		异步通知地址
    return_url		是		string		同步通知地址
    requestip		是		string		终端用户发起请求IP 
*/    
    
    private static final String mchid			= "mchid";	
    private static final String timestamp	    = "timestamp";		
    private static final String nonce		    = "nonce";			
    private static final String sign		    = "sign";			
    private static final String data            = "data";	

    private static final String paytype			= "paytype";				
    private static final String out_trade_no    = "out_trade_no";	
    private static final String goodsname	    = "goodsname";		
    private static final String total_fee	    = "total_fee";		
    private static final String notify_url	    = "notify_url";		
    private static final String return_url	    = "return_url";		
    private static final String requestip	    = "requestip";	  

    private static final String key	    		= "key";	  


  @Override
  protected Map<String, String> buildPayParam() throws PayException {
		Map<String, String> payParam = Maps.newHashMap();
		Map<String, String> dataMap = Maps.newHashMap();
		//data
		dataMap.put(paytype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
		dataMap.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
		dataMap.put(goodsname,channelWrapper.getAPI_ORDER_ID());
		dataMap.put(total_fee,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
		dataMap.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
		dataMap.put(return_url,channelWrapper.getAPI_WEB_URL());
		dataMap.put(requestip,channelWrapper.getAPI_Client_IP());
		//Head
		payParam.put(mchid,channelWrapper.getAPI_MEMBERID());
		payParam.put(timestamp,System.currentTimeMillis() / 1000 + "");
		payParam.put(nonce,HandlerUtil.getRandomStr(4));
		payParam.put(data,StringEscapeUtils.unescapeJavaScript(JSON.toJSONString(dataMap)));

		log.debug("[百捷支付2]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
		return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
		String pay_md5sign = null;
        Map<String, Object> payParamJson = rebuildSignMap(api_response_params);
        payParamJson = sortMapByKey(payParamJson);
		String returnstr="";
		Iterator<Entry<String, Object>> it = payParamJson.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Object> entry = it.next();
			returnstr+="&"+entry.getKey() + "=" + entry.getValue();
		}
		returnstr = returnstr.substring(1)+"&key="+channelWrapper.getAPI_KEY();
		pay_md5sign = HandlerUtil.getMD5UpperCase(returnstr).toLowerCase(); // 进行MD5运算，再将得到的字符串所有字符转换为大写
		log.debug("[百捷支付2]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(pay_md5sign));
		return pay_md5sign;
  }

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {

        Map<String, Object> payParamJson = rebuildMapforObject(payParam);
		payParamJson.put(sign, pay_md5sign);

        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
        	resultStr = request(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParamJson),"UTF-8");
        	//resultStr = RestTemplateUtil.request(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(payParamJson),"UTF-8");
        	if(StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")){
                result.put(HTMLCONTEXT,resultStr);
                payResultList.add(result);
            }else if(StringUtils.isNotBlank(resultStr) ){
                JSONObject jsonResultStr = JSON.parseObject(resultStr);	//0代表成功,其他代表失败
                if(null!=jsonResultStr && jsonResultStr.containsKey("error") && "0".equalsIgnoreCase(jsonResultStr.getString("error"))){
                	JSONObject dataObject = JSON.parseObject(jsonResultStr.getString("data"));
                    if(HandlerUtil.isWapOrApp(channelWrapper)){
                        result.put(JUMPURL, dataObject.getString("payurl"));
                    }else{
                        result.put(QRCONTEXT, dataObject.getString("payurl"));
                    }
                    payResultList.add(result);
                }else {throw new PayException(resultStr); }
			}else{ throw new PayException(EMPTYRESPONSE);}
        } catch (Exception e) {
             log.error("[百捷支付2]-3.发送支付请求，及获取支付请求结果出错：", e);
             throw new PayException(e.getMessage(), e);
        }
        log.debug("[百捷支付2]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}" ,JSON.toJSONString(payResultList));
        return payResultList;
    }

	protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
		RequestPayResult requestPayResult = new RequestPayResult();
		if (null != resultListMap && !resultListMap.isEmpty()) {
			if (resultListMap.size() == 1) {
				Map<String, String> resultMap = resultListMap.get(0);
				requestPayResult = buildResult(resultMap, channelWrapper, requestPayResult);
			}
			if (ValidateUtil.requestesultValdata(requestPayResult)) {
				requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
			} else {
				throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
			}
		} else {
			throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
		}
		log.debug("[百捷支付2]-[请求支付]-4.处理请求响应成功：{}", JSON.toJSONString(requestPayResult));
		return requestPayResult;
	}
	
	/**
	 * 	参照RestTemplateUtil - request , 调整 Content-Type 为 application/json
	 * @param url
	 * @param params
	 * @param CHARSET
	 * @return
	 * @throws PayException
	 */
    public static String request(String url, String params,String CHARSET) throws PayException {
        try {
           // System.out.println("请求报文:" + params);
            URL urlObj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(1000 * 5);
            conn.setRequestProperty("Charset", CHARSET);
            conn.setRequestProperty("Content-Type", "application/json");
            //原本是用form的方式呼叫Request,这边调整Content-Type
            //conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(params.length()));
            OutputStream outStream = conn.getOutputStream();
            outStream.write(params.toString().getBytes(CHARSET));
            outStream.flush();
            outStream.close();
            return getResponseBodyAsString(conn.getInputStream(),CHARSET);
        } catch (Exception ex) {
            throw new PayException(ex.getMessage());
        }
    }

    /**
     	* 获取响应报文
     */
    private static String getResponseBodyAsString(InputStream in,String CHARSET) throws PayException {
        try {
            BufferedInputStream buf = new BufferedInputStream(in);
            byte[] buffer = new byte[1024];
            StringBuffer data = new StringBuffer();
            int readDataLen;
            while ((readDataLen = buf.read(buffer)) != -1) {
                data.append(new String(buffer, 0, readDataLen, CHARSET));
            }
            System.out.println("响应报文=" + data);
            return data.toString();
        } catch (Exception ex) {
            throw new PayException(ex.getMessage());
        }

    }

	private Map<String,Object> rebuildMapforObject(Map<String,String> api_response_params) throws PayException {
        Map<String, Object> payParamJson = new HashMap<>();
		Map<String, Object> detailParamJson = new HashMap<>();
		detailParamJson.put(paytype,Integer.parseInt(channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG()));
		detailParamJson.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
		detailParamJson.put(goodsname,channelWrapper.getAPI_ORDER_ID());
		detailParamJson.put(total_fee,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
		detailParamJson.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
		detailParamJson.put(return_url,channelWrapper.getAPI_WEB_URL());
		detailParamJson.put(requestip,channelWrapper.getAPI_Client_IP());
		payParamJson.put(mchid,Integer.parseInt(api_response_params.get(mchid)));
		payParamJson.put(timestamp,api_response_params.get(timestamp));
		payParamJson.put(nonce,api_response_params.get(nonce));
		payParamJson.put(data,detailParamJson);
		return payParamJson;
	}
		
	private Map<String,Object> rebuildSignMap(Map<String,String> api_response_params) throws PayException {
        Map<String, Object> payParamJson = new HashMap<>();
		Map<String, Object> detailParamJson = new HashMap<>();
		Map<String, Object> post = new HashMap<String, Object>();
		detailParamJson.put(paytype,Integer.parseInt(channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG()));
		detailParamJson.put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
		detailParamJson.put(goodsname,channelWrapper.getAPI_ORDER_ID());
		detailParamJson.put(total_fee,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
		detailParamJson.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
		detailParamJson.put(return_url,channelWrapper.getAPI_WEB_URL());
		detailParamJson.put(requestip,channelWrapper.getAPI_Client_IP());
		payParamJson.put(mchid,Integer.parseInt(api_response_params.get(mchid)));
		payParamJson.put(timestamp,api_response_params.get(timestamp));
		payParamJson.put(nonce,api_response_params.get(nonce));
		post.putAll(payParamJson);
		post.putAll(detailParamJson);
		return post;
	}
	
    public static Map<String, Object> sortMapByKey(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        Map<String, Object> sortMap = new TreeMap<String, Object>(new Comparator<String>()
        {
            public int compare(String str1, String str2) {
                return str1.compareTo(str2);
            }
	    });
        sortMap.putAll(map);
        return sortMap;
    }
}