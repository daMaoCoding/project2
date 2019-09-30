package dc.pay.business.jiyifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dc.pay.utils.*;
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

/**
 * 该第三方，签名加密对空格的处理，请求与回调的处理方式是不一样的
 * 
 * @author andrew
 * May 22, 2018
 */
@RequestPayHandler("JIYIFU")
public final class JiYiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(JiYiFuPayRequestHandler.class);

	//字段名					变量名					必填				类型					说明
	//商户号					spid					是				String(10)				商户号
	//通知回调URL				notify_url				是				String(255)				接收我司异步通知通知的URL，需给绝对路径，255字符内，格式如:http://apitest.boxitech.com/notify.html
	//成功跳转URL				pay_show_url			是				String(255)				支付成功后跳转的URL，需给绝对路径，255字符内，格式如:http://apitest.boxitech.com/success.html
	//商户订单号				sp_billno				是				String(32)				商户系统内部的订单号,32个字符内、可包含字母
	//商户ip					spbill_create_ip		是				String(16)				请注意：此IP为终端用户的IP，不能是商户平台本身IP，否则支付失败！
	//支付类型					pay_type				是				String(8)				支付方式：H5支付:800209
	//发起交易时间				tran_time				是				String(14)				发起交易的时间，格式为yyyyMMddhhmmss
	//交易金额					tran_amt				是				int						单位为分
	//币种类型					cur_type				是				String(8)				港币：HKD 人民币：CNY暂仅支持CNY
	//商品描述					item_name				是				String(128)				商品名称
	//用户唯一标识				pc_userid				是				String(32)				用户在商户应用的唯一标识，建议为用户帐号
    //签名					sign					是				String(32)				签名
	private static final String spid				="spid";
	private static final String notify_url			="notify_url";
	private static final String pay_show_url		="pay_show_url";
	private static final String sp_billno			="sp_billno";
	private static final String spbill_create_ip	="spbill_create_ip";
	private static final String pay_type			="pay_type";
	private static final String tran_time			="tran_time";
	private static final String tran_amt			="tran_amt";
	private static final String cur_type			="cur_type";
	private static final String item_name			="item_name";
	private static final String pc_userid			="pc_userid";
	
	//网银
	//中文名				字段名				必填			类型					说明
	//商户号				spid				是			string(10)			商户/平台在我司注册的账号。我司维度唯一，固定长度10位
	//用户号				sp_userid			是			string(20)			持卡人在商户/平台注册的账号。商户/平台维度唯一，必须为纯数字如果需要计算用户累计限额, 必填
	//商户订单号			spbillno			是			string(32)			商户/平台生成的订单号。商户/平台维度唯一，必须为纯字母和数字
	//订单交易金额			money				是			bigint				订单的支付金额。
	//金额类型				cur_type			是			int					订单金额的类型。1 – 人民币(单位：分)
	//页面回调地址			return_url			是			string(255)			网银支付结果页面通知地址。(不能含有英文引号字符.)
	//后台回调地址			notify_url			是			string(255)			网银支付结果后台通知地址。(不能含有英文引号字符.)
	//订单备注				memo				是			string(255)			订单的商品的名称。
	//银行卡类型			card_type			是			int					银行卡类型。1：借记卡	2：贷记卡
	//银行代号				bank_segment		是			string(4)			我司内部区分不同银行的4位数字。详见文档银行代号部分
	//用户类型				user_type			是			int					发起支付交易的用户的类型。1：个人	2：企业
	//渠道类型				channel				是			int					商户的用户使用的终端类型。(目前固定为1)
	//签名类型				encode_type			是			string(5)			签名的方法。目前支持：MD5、RSA(建议用MD5)
	private static final String sp_userid				="sp_userid";
	private static final String spbillno				="spbillno";
	private static final String money					="money";
//	private static final String return_url				="return_url";
	private static final String memo					="memo";
	private static final String card_type				="card_type";
	private static final String bank_segment			="bank_segment";
	private static final String user_type				="user_type";
	private static final String channel					="channel";
	private static final String encode_type				="encode_type";
	
	//微信 qq 
	private static final String out_channel				="out_channel";
	
	//微信 qq  银联 
	private static final String bank_mch_name			="bank_mch_name";
	private static final String bank_mch_id				="bank_mch_id";
	
	//signature	数据签名	32	是	　
//	private static final String signature		="sign";

    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
            	put(spid, channelWrapper.getAPI_MEMBERID());
            	put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            	if (handlerUtil.isWY(channelWrapper)) {
					put(sp_userid,(int)((Math.random()*9+1)*1000000)+"");
					put(spbillno,channelWrapper.getAPI_ORDER_ID());
//					put(spbillno,System.currentTimeMillis()+"");
					put(money,  channelWrapper.getAPI_AMOUNT());
					put(cur_type,"1");
					put(memo,"name");
					put(card_type,"1");
					put(bank_segment,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
					put(user_type,"1");
					put(channel,"1");
					put(encode_type,"MD5");
				}else {
					put(pay_show_url,channelWrapper.getAPI_WEB_URL());
					put(sp_billno,channelWrapper.getAPI_ORDER_ID());
//					put(sp_billno,System.currentTimeMillis()+"");
					put(spbill_create_ip,handlerUtil.getRandomIp(channelWrapper));
					put(pay_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
					//微信 qq
					if (channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("WX") || channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("QQ")) {
						put(out_channel,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
						put(bank_mch_name,handlerUtil.getRandomStr(5));
						put(bank_mch_id,(int)((Math.random()*9+1)*1000000)+"");
					}else if (handlerUtil.isYLWAP(channelWrapper)) {
						put(bank_mch_name,handlerUtil.getRandomStr(5));
						put(bank_mch_id,(int)((Math.random()*9+1)*1000000)+"");
					}
					put(tran_time,  HandlerUtil.getDateTimeByMilliseconds(channelWrapper.getAPI_OrDER_TIME(), "yyyyMMddHHmmss"));
					put(tran_amt,  channelWrapper.getAPI_AMOUNT());
					put(cur_type,"CNY");
					put(item_name,"name");
					put(pc_userid, channelWrapper.getAPI_MEMBERID());
				}
            }
        };
        log.debug("[极易付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
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
        signSrc.append("key=" + channelWrapper.getAPI_KEY());
		String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
        log.debug("[极易付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(signMd5));
        return signMd5;
    }

	protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
		payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
		HashMap<String, String> result = Maps.newHashMap();
		if (HandlerUtil.isWY(channelWrapper)) {
			result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
		}else{
			String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam,"UTF-8");
			if (StringUtils.isBlank(resultStr)) {
				log.error("[极易付]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空"+",参数："+JSON.toJSONString(payParam));
				throw new PayException("返回空"+",参数："+JSON.toJSONString(payParam));
			}
			if (!resultStr.contains("<retcode>00</retcode>")) {
				log.error("[极易付]-[请求支付]-3.2.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}
			Map<String, String> map = null;
			try {
				map = XmlUtil.toMap(resultStr.getBytes(), "utf-8");
			} catch (Exception e) {
				e.printStackTrace();
				log.error("[极易付]-[请求支付]-3.3.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
				throw new PayException(resultStr);
			}
			result.put(handlerUtil.isWapOrApp(channelWrapper) ? JUMPURL : QRCONTEXT,handlerUtil.isWapOrApp(channelWrapper) ?  map.get("jump_url") :  map.get("qrcode"));
		}
		ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
		payResultList.add(result);
		log.debug("[极易付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：" + JSON.toJSONString(payResultList));
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
        log.debug("[极易付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}