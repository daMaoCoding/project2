package dc.pay.business.yinbang;

import java.io.Serializable;

public class JsonResult implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private String respCode;
	
	private String message;
	
	private Object data;
	
	public JsonResult(){}
	
	public JsonResult(String respCode, String message) {
		super();
		this.respCode = respCode;
		this.message = message;
	}
	public JsonResult(String respCode, String message, Object data) {
	    this(respCode,message) ;
		this.data = data;
	}
	
	public String getRespCode() {
		return respCode;
	}

	public void setRespCode(String respCode) {
		this.respCode = respCode;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

}
