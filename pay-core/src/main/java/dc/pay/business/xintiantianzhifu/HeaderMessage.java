package dc.pay.business.xintiantianzhifu;

import java.io.Serializable;

/**
 * 请求报文头公共类
 */
public class HeaderMessage implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8094516946457873664L;
	
	private String channelNo;
	private String method;
	private String version;
	private String userReqNo;
	private String reqTime;
	private String sign;
	private String encryptKey;
	
	public String getChannelNo() {
		return channelNo;
	}

	public void setChannelNo(String channelNo) {
		this.channelNo = channelNo;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	

	public String getUserReqNo() {
		return userReqNo;
	}

	public void setUserReqNo(String userReqNo) {
		this.userReqNo = userReqNo;
	}

	public String getReqTime() {
		return reqTime;
	}

	public void setReqTime(String reqTime) {
		this.reqTime = reqTime;
	}
	
	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}

	public String getEncryptKey() {
		return encryptKey;
	}

	public void setEncryptKey(String encryptKey) {
		this.encryptKey = encryptKey;
	}
	
}
