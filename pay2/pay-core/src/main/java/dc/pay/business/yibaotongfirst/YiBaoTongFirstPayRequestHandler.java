package dc.pay.business.yibaotongfirst;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
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
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author andrew
 * May 23, 2018
 */
@RequestPayHandler("YIBAOTONGFIRST")
public final class YiBaoTongFirstPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YiBaoTongFirstPayRequestHandler.class);

  //字段名					填写类型			说明
  	//version				必填				当前接口版本 V1.0
  	//merchantNum			必填				分配给商家的商户号
  	//nonce_str				必填				随机字符串
  	//merMark				必填				分配给商家的商户标识
  	//client_ip				必填				客户端ip，如127.0.0.1
  	//orderTime				必填				订单时间（格式: yyyy-MM-dd HH:mm:ss）
  	//payType				必填				支付类型（QQCode:QQ扫码，QQH5：QQH5，wechatCode：微信扫码	wechatH5：微信H5，aliCode：支付宝扫码，aliH5：支付宝H5，B2C：网银支付，kuaijie：快捷支付）
  	//orderNum				必填				商户订单号（此订单号必须在自定义订单号前拼接商户标识，示例：orderNum =ABC10000,ABC为商户标识）
  	//amount				必填				订单金额，单位（分）
  	//body					必填				订单描述
  	//signType				必填				签名类型（MD5）不参与签名
  	//bank_code				选填				payType为B2C时必填，参照附录中的银行代码对照表
  	//notifyUrl				选填				后台通知地址，如不填则不发送通知
  	//sign					必填				MD5签名结果
  	private static final String version				="version";
  	private static final String merchantNum			="merchantNum";
  	private static final String nonce_str			="nonce_str";
  	private static final String merMark				="merMark";
  	private static final String client_ip			="client_ip";
  	private static final String orderTime			="orderTime";
  	private static final String payType				="payType";
  	private static final String orderNum			="orderNum";
  	private static final String amount				="amount";
  	private static final String body				="body";
  	private static final String signType			="signType";
//  	private static final String bank_code			="bank_code";
  	private static final String notifyUrl			="notifyUrl";
//  	private static final String sign				="sign";

      @Override
      protected Map<String, String> buildPayParam() throws PayException {
  		String api_MEMBERID = channelWrapper.getAPI_MEMBERID();
    	if (null == api_MEMBERID || !api_MEMBERID.contains("&") || api_MEMBERID.split("&").length != 2) {
            log.error("[亿宝通1.0]-[请求支付]-1.1.“支付通道商号”输入数据格式为：商户号&商户标识" );
            throw new PayException("[亿宝通1.0]-[请求支付]-1.1.“支付通道商号”输入数据格式为：商户号&商户标识" );
		}
          Map<String, String> payParam = new TreeMap<String, String>() {
              {
              	put(version, "V1.0");
              	put(merchantNum, channelWrapper.getAPI_MEMBERID().split("&")[0]);
              	put(nonce_str, handlerUtil.getRandomStr(5));
              	put(merMark, channelWrapper.getAPI_MEMBERID().split("&")[1]);
              	put(client_ip, HandlerUtil.getRandomIp(channelWrapper));
              	put(orderTime, DateUtil.formatDateTimeStrByParam("yyyy-MM-dd HH:mm:ss")); //yyyy-MM-dd HH:mm:ss
              	put(orderNum, channelWrapper.getAPI_ORDER_ID());
              	put(amount, channelWrapper.getAPI_AMOUNT());
              	put(body, "name");
              	put(signType, "MD5");
              	put(payType,  channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              	put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              }
          };
          log.debug("[亿宝通1.0]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
          return payParam;
      }

      protected String buildPaySign(Map params) throws PayException {
          StringBuffer signSrc= new StringBuffer();
          signSrc.append(version		+"=").append(params.get(version		)).append("&");
          signSrc.append(merchantNum	+"=").append(params.get(merchantNum	)).append("&");
          signSrc.append(nonce_str	+"=").append(params.get(nonce_str	)).append("&");
          signSrc.append(merMark		+"=").append(params.get(merMark		)).append("&");
          signSrc.append(client_ip	+"=").append(params.get(client_ip	)).append("&");
          signSrc.append(payType		+"=").append(params.get(payType		)).append("&");
          signSrc.append(orderNum		+"=").append(params.get(orderNum	)).append("&");
          signSrc.append(amount		+"=").append(params.get(amount		)).append("&");
          signSrc.append(body			+"=").append(params.get(body		)).append("&");
          signSrc.append("key=").append(channelWrapper.getAPI_KEY());
          String signInfo = signSrc.toString();
          String signMd5 = HandlerUtil.getMD5UpperCase(signInfo);
          log.debug("[亿宝通1.0]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
          return signMd5;
      }

  	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
  		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
  		HashMap<String, String> result = Maps.newHashMap();
  		if (HandlerUtil.isWapOrApp(channelWrapper)) {
  			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
  		}else{
  			String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
  			if (StringUtils.isBlank(resultStr)) {
  				log.error("[亿宝通1.0]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
  				throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
  			}
  			try {
				resultStr = new String(resultStr.getBytes("ISO-8859-1"), "GBK");
				JSONObject resJson = JSONObject.parseObject(resultStr);
				if (!resJson.containsKey("resp_Code") || !"S".equals(resJson.getString("resp_Code")) || !resJson.containsKey("qyCode")) {
					log.error("[亿宝通1.0]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
					throw new PayException(resultStr);
				}
				result.put(QRCONTEXT, resJson.getString("qyCode"));
  			} catch (UnsupportedEncodingException e) {
  				e.printStackTrace();
  				log.error("[亿宝通1.0]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
  				throw new PayException(resultStr);
  			}
  		}
  		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
  		payResultList.add(result);
  		log.debug("[亿宝通1.0]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
          log.debug("[亿宝通1.0]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
          return requestPayResult;
      }
}