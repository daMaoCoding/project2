package dc.pay.base.processor;

import dc.pay.business.ResponsePayResult;
import dc.pay.business.caifubao.StringUtil;
import dc.pay.config.RunTimeInfo;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.dao.pay.RequestPayDao;
import dc.pay.entity.ReqPayInfo;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RsaUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

/**
 * ************************
 * 
 * @author tony 3556239829
 */
public abstract class PayResponseHandler {
	private static final Logger logForDbOnly = LoggerFactory.getLogger("logForDbOnly");
	private static final Logger log = LoggerFactory.getLogger(PayResponseHandler.class);
	protected ChannelWrapper channelWrapper;
	protected HandlerUtil handlerUtil;
	protected Map<String, String> API_RESPONSE_PARAMS;
	protected static final String APPLICATION_JSON_UTF8 = MediaType.APPLICATION_JSON.toString();
	protected static final String ResponsePayMsgSplit = "|";
	protected RunTimeInfo runTimeInfo;


	public abstract String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException;
	protected static String jsonResponsePayMsg(String responseMsg){
		return APPLICATION_JSON_UTF8.concat(ResponsePayMsgSplit).concat(responseMsg);
	}

	protected static String stringResponsePayMsg(String responseMsg){
		return responseMsg;
	}

	public ResponsePayResult responsePay(RequestPayDao requestPayDao)  {
		ResponsePayResult responsePayResult = null;
		Map<String, String> payParam = null;
		ReqPayInfo reqPayInfo=null;
		boolean isMd5SignPass = false;
		String signMd5 = null;
		String orderId=null;
		boolean payStatusAndMount = false;
		List<Map<String, String>> requestPayResultDetail = null;
		try {
			orderId = processForOrderId(API_RESPONSE_PARAMS);
			if(StringUtils.isBlank(orderId)) throw new PayException(orderId);
		}catch (Exception e){
			return  responseForPay(orderId,false, SERVER_MSG.RESPONSE_PAY_HANDLER_ORDERID_ERROR.getMsg() ,false);
		}
		try {
			reqPayInfo = requestPayDao.getReqPayInfo(orderId);
			if(reqPayInfo==null || StringUtils.isBlank(reqPayInfo.getAPI_ORDER_ID()) ) throw new PayException( SERVER_MSG.RESPONSE_PAY_HANDLER_GETREQPAYINFO_ERROR.getMsg() );
		}catch (Exception e){
			return  responseForPay(orderId,false, SERVER_MSG.RESPONSE_PAY_HANDLER_GETREQPAYINFO_ERROR.getMsg()  ,false);
		}
		if (reqPayInfo != null && StringUtils.isNotBlank(reqPayInfo.getAPI_ORDER_ID()) 	&& StringUtils.isNotBlank(orderId) && !reqPayInfo.getAPI_ORDER_ID().equalsIgnoreCase(orderId)) {
			reqPayInfo.setAPI_ORDER_ID(orderId);
		}
		try {
			reqPayInfo.setAPI_KEY(RsaUtil.decryptAndCache(reqPayInfo.getAPI_KEY())); // 解密并缓存KEY
		} catch (PayException e) {
			return  responseForPay(orderId,false, SERVER_MSG.RESPONSE_PAY_HANDLER_DECAPIKEY_ERROR.getMsg(),false);
		}
		try {
			this.channelWrapper = processForChannel(reqPayInfo);
		} catch (PayException e) {
			return  responseForPay(orderId,false, e.getMessage(),false);
		}
		if (valdataReqPayStatus(reqPayInfo)) {
			return responseForPay(orderId,true, SERVER_MSG.ORDER_HAS_BEN_PAYED.getMsg() + channelWrapper.getAPI_ORDER_ID(),true); // 已支付订单
		}
		try {
			  signMd5 = buildPaySign(API_RESPONSE_PARAMS, channelWrapper.getAPI_KEY());
		}catch (Exception e){
			return  responseForPay(orderId,false,  SERVER_MSG.RESPONSE_PAY_HANDLER_BUILDMD5_ERROR.getMsg()+e.getMessage(),false);
		}
		try {
			  payStatusAndMount = checkPayStatusAndMount(API_RESPONSE_PARAMS, channelWrapper.getAPI_AMOUNT());
			  if(!payStatusAndMount) throw new PayException(Boolean.toString(payStatusAndMount));
		}catch (Exception e){
			return  responseForPay(orderId,false, SERVER_MSG.RESPONSE_PAY_HANDLER_CHECKAMOUNTANDSTATUS_ERROR.getMsg()+e.getMessage(),false);
		}
		try {
			isMd5SignPass = checkSignMd5(API_RESPONSE_PARAMS, signMd5);
			if(!isMd5SignPass) throw new PayException(Boolean.toString(isMd5SignPass));
		}catch (Exception e){
			return  responseForPay(orderId,false, SERVER_MSG.RESPONSE_PAY_HANDLER_CHECKSIGN_ERROR.getMsg()+e.getMessage(),false);
		}
		responsePayResult = responseForPay(orderId,payStatusAndMount && isMd5SignPass, null);
		return responsePayResult;
	}

	protected abstract boolean checkPayStatusAndMount(Map<String, String> api_response_params, String amount)throws PayException;

	private ResponsePayResult responseForPay(String mapOrderId,boolean b, String errMsg, boolean... isPayed) {
		ResponsePayResult responsePayResult = new ResponsePayResult();
		if(null!=channelWrapper){
			responsePayResult.setResponseOrderID(channelWrapper.getAPI_ORDER_ID());
			responsePayResult.setResponsePayOid(channelWrapper.getAPI_OID());
			responsePayResult.setResponseOrderState(channelWrapper.getAPI_PAY_ORDER_STATUS());
			responsePayResult.setResponsePayChannel(channelWrapper.getAPI_CHANNEL_BANK_NAME());
			responsePayResult.setResponsePayAmount(channelWrapper.getAPI_AMOUNT());
			responsePayResult.setResponsePayMemberId(channelWrapper.getAPI_MEMBERID());
			responsePayResult.setResponsePayOtherParam(channelWrapper.getAPI_OTHER_PARAM());
		}

		if (b) {
			responsePayResult.setResponsePayMsg(responseSuccess());
			responsePayResult.setResponsePayCode(PayEumeration.RESPONSE_PAY_CODE.SUCCESS.getCodeValue());
			responsePayResult.setResponseOrderState(PayEumeration.API_ORDER_STATE.SUCCESS.getState());
		} else {
			responsePayResult.setResponsePayMsg("ERROR");
			responsePayResult.setResponsePayCode(PayEumeration.RESPONSE_PAY_CODE.ERROR.getCodeValue());
			responsePayResult.setResponsePayErrorMsg( StringUtils.isNotBlank(errMsg) ? errMsg : SERVER_MSG.RESPONSE_PAY_VALDATA_SIGN_ERROR.getMsg());
		}
		if (isPayed != null && isPayed.length == 1 && isPayed[0]) {
			responsePayResult.setPayed(true);
			responsePayResult.setResponsePayCode(PayEumeration.RESPONSE_PAY_CODE.SUCCESS.getCodeValue());
			responsePayResult.setResponsePayErrorMsg( SERVER_MSG.ORDER_HAS_BEN_PAYED.getMsg().concat(channelWrapper.getAPI_ORDER_ID()));
		}
		if(StringUtils.isNotBlank(mapOrderId)) responsePayResult.setResponseOrderID(mapOrderId);
		if(null!=responsePayResult && channelWrapper!=null) responsePayResult.setResponsePaySign(HandlerUtil.getResponsePaySign(responsePayResult, channelWrapper.getAPI_KEY())); // 签名
		return responsePayResult;
	}



	protected abstract String responseSuccess();

	protected abstract boolean checkSignMd5(Map<String, String> api_response_params, String signMd5);

	protected abstract String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException;

	private boolean valdataReqPayStatus(ReqPayInfo reqPayInfo) {
		if (reqPayInfo != null && StringUtils.isNotBlank(reqPayInfo.getAPI_ORDER_STATE())) {
			if (PayEumeration.API_ORDER_STATE.SUCCESS.getState().equalsIgnoreCase(reqPayInfo.getAPI_ORDER_STATE()) || "1".equalsIgnoreCase(reqPayInfo.getAPI_ORDER_STATE())) {
				return true;
			}
		}
		return false;
	}

	private ChannelWrapper processForChannel(ReqPayInfo reqPayInfo) throws PayException {
		ValidateUtil.valiRSA_KEY(reqPayInfo);
		ChannelWrapper channel = new ChannelWrapper();
		channel.setAPI_KEY(reqPayInfo.getAPI_KEY().replaceAll("\\s*", ""));
		channel.setAPI_PUBLIC_KEY(reqPayInfo.getAPI_PUBLIC_KEY().replaceAll("\\s*", ""));
		channel.setAPI_WEB_URL(reqPayInfo.getAPI_WEB_URL());
		channel.setAPI_JUMP_URL_PREFIX(reqPayInfo.getAPI_JUMP_URL_PREFIX());
		channel.setAPI_OTHER_PARAM(reqPayInfo.getAPI_OTHER_PARAM());
		channel.setAPI_Client_IP(reqPayInfo.getAPI_Client_IP());
		channel.setAPI_ORDER_FROM(reqPayInfo.getAPI_ORDER_FROM());
		channel.setAPI_OID(reqPayInfo.getAPI_OID());
		channel.setAPI_MEMBERID(reqPayInfo.getAPI_MEMBERID());
		channel.setAPI_AMOUNT(reqPayInfo.getAPI_AMOUNT());
		channel.setAPI_ORDER_ID(reqPayInfo.getAPI_ORDER_ID());
		channel.setAPI_OrDER_TIME(reqPayInfo.getAPI_OrDER_TIME());
		channel.setAPI_CHANNEL_BANK_NAME(reqPayInfo.getAPI_CHANNEL_BANK_NAME());
		channel.setAPI_PAY_ORDER_STATUS(reqPayInfo.getAPI_ORDER_STATE());
		return channel;
	}

	public RunTimeInfo getRunTimeInfo() {
		return runTimeInfo;
	}

	public void setRunTimeInfo(RunTimeInfo runTimeInfo) {
		this.runTimeInfo = runTimeInfo;
	}

	public ChannelWrapper getChannelWrapper() {
		return channelWrapper;
	}

	public void setChannelWrapper(ChannelWrapper channelWrapper) {
		this.channelWrapper = channelWrapper;
	}

	public Map<String, String> getAPI_RESPONSE_PARAMS() {
		return API_RESPONSE_PARAMS;
	}

	public void setAPI_RESPONSE_PARAMS(Map<String, String> API_RESPONSE_PARAMS) {
		this.API_RESPONSE_PARAMS = API_RESPONSE_PARAMS;
	}

	public HandlerUtil getHandlerUtil() {
		return handlerUtil;
	}

	public void setHandlerUtil(HandlerUtil handlerUtil) {
		this.handlerUtil = handlerUtil;
	}
}