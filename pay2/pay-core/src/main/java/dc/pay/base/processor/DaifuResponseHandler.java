package dc.pay.base.processor;

import com.alibaba.fastjson.JSON;
import dc.pay.business.ResponseDaifuResult;
import dc.pay.config.RunTimeInfo;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.dao.daifu.RequestDaiFuDao;
import dc.pay.entity.ReqDaifuInfo;
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
public abstract class DaifuResponseHandler {
	private static final Logger log = LoggerFactory.getLogger(DaifuResponseHandler.class);
	protected ChannelWrapper channelWrapper;
	protected HandlerUtil handlerUtil;
	protected Map<String, String> API_RESPONSE_PARAMS;
	protected static final String APPLICATION_JSON_UTF8 = MediaType.APPLICATION_JSON.toString();
	protected static final String ResponsePayMsgSplit = "|";
	protected static final String BLANK = " ";
	protected static final String FALSE = "FALSE";
	protected static final String TRUE = "TRUE";
	protected RunTimeInfo runTimeInfo;


	public ResponseDaifuResult responseDaifu(RequestDaiFuDao requestDaifuDao, RunTimeInfo runTimeInfo) throws PayException {
		ResponseDaifuResult responseDaifuResult = null;
		Map<String, String> payParam = null;
		String pay_md5sign = null;
		String errMsg = "";
		List<Map<String, String>> requestDaifuResultDetail = null;
		if (null == API_RESPONSE_PARAMS || API_RESPONSE_PARAMS.isEmpty())   throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_EMPTY_ERROR);
		String orderId = processForOrderId(API_RESPONSE_PARAMS);
		if (StringUtils.isBlank(orderId)) throw new PayException("[响应代付]订单号空");
		ReqDaifuInfo reqDaifuInfo = requestDaifuDao.getReqDaifuInfo(orderId);
		if (ValidateUtil.valdataReqDaifuInfo(reqDaifuInfo)) {
			try {
				reqDaifuInfo.setAPI_KEY(RsaUtil.decryptAndCache(reqDaifuInfo.getAPI_KEY())); // 解密并缓存KEY
			} catch (PayException e) { // com.ddg.mq.pay.
				throw new PayException("解密API-KEY出错");
			}
			this.channelWrapper  =handlerUtil.createDaifuChannelWrapper(reqDaifuInfo,runTimeInfo);
			String signMd5 = buildPaySign(API_RESPONSE_PARAMS, channelWrapper.getAPI_KEY());
			boolean checkAmount = checkAmount(API_RESPONSE_PARAMS, channelWrapper.getAPI_AMOUNT());
			boolean isMd5SignPass = checkSignMd5(API_RESPONSE_PARAMS, signMd5);
			PayEumeration.DAIFU_RESULT resOrderStatus = resOrderStatus(API_RESPONSE_PARAMS);
			if(!checkAmount)errMsg=SERVER_MSG.RESPONSE_DAIFU_RESULT_AMOUNT_ERROR.getMsg();
            if(resOrderStatus !=PayEumeration.DAIFU_RESULT.SUCCESS) errMsg =errMsg.concat(BLANK).concat(SERVER_MSG.RESPONSE_DAIFU_RESULT_STATUS_ERROR.getMsg());
			if(!isMd5SignPass)errMsg=errMsg.concat(BLANK).concat(SERVER_MSG.RESPONSE_DAIFU_RESULT_SIGN_ERROR.getMsg());
			responseDaifuResult = responseForDaifu(checkAmount  && isMd5SignPass &&   resOrderStatus!=PayEumeration.DAIFU_RESULT.UNKNOW, reqDaifuInfo,errMsg,resOrderStatus,API_RESPONSE_PARAMS);
		}
		return responseDaifuResult;
	}



	public abstract String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException;



	protected static String jsonResponsePayMsg(String responseMsg){
		return APPLICATION_JSON_UTF8.concat(ResponsePayMsgSplit).concat(responseMsg);
	}

	protected static String stringResponsePayMsg(String responseMsg){
		return responseMsg;
	}



	protected abstract boolean checkAmount(Map<String, String> api_response_params, String amount)throws PayException;


	/**
	 * 很关键
	 * 1.第三方明确指定，此笔[代付取消][代付失败]，返回true
	 * 2.否则返回false
	 */
	protected abstract PayEumeration.DAIFU_RESULT resOrderStatus(Map<String, String> api_response_params)throws PayException;


	private ResponseDaifuResult responseForDaifu(boolean b, ReqDaifuInfo reqDaifuInfo, String errMsg, PayEumeration.DAIFU_RESULT resOrderStatus, Map<String, String> API_RESPONSE_PARAMS) {
		ResponseDaifuResult responseDaifuResult = new ResponseDaifuResult(b,reqDaifuInfo,errMsg,resOrderStatus);
		responseDaifuResult.setResponseDaifuMsg(b?responseSuccess():PayEumeration.RESPONSE_PAY_CODE.ERROR.getCodeValue());
		responseDaifuResult.setResponseDaifuSign(HandlerUtil.getResponseDaifuSign(responseDaifuResult, channelWrapper.getAPI_KEY())); // 签名
		if(b && resOrderStatus==PayEumeration.DAIFU_RESULT.ERROR )responseDaifuResult.setResponseDaifuErrorMsg("第三方回调确定转账取消或失败。"+JSON.toJSONString(API_RESPONSE_PARAMS));
		return responseDaifuResult;
	}


	private boolean valdataReqDaifuStatus(ReqDaifuInfo reqDaifuInfo) {
		if (reqDaifuInfo != null && StringUtils.isNotBlank(reqDaifuInfo.getAPI_ORDER_STATE())) {
			if (PayEumeration.API_ORDER_STATE.SUCCESS.getState().equalsIgnoreCase(reqDaifuInfo.getAPI_ORDER_STATE()) || "1".equalsIgnoreCase(reqDaifuInfo.getAPI_ORDER_STATE())) {
				return true;
			}
		}
		return false;
	}




	protected abstract String responseSuccess();

	protected abstract boolean checkSignMd5(Map<String, String> api_response_params, String signMd5);

	protected abstract String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException;


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

	public RunTimeInfo getRunTimeInfo() {
		return runTimeInfo;
	}

	public void setRunTimeInfo(RunTimeInfo runTimeInfo) {
		this.runTimeInfo = runTimeInfo;
	}
}