package dc.pay.business.xintiantianzhifu;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;



/** 
 * @author 
 */
public class ParamsMessage implements Serializable{

	private static final long serialVersionUID = 1460555946890382576L;
	
	/**
	 * 渠道编号
	 */
	private String channelNo;
	
	/**
	 * 方法名
	 */
	private String method;
	
	/**
	 * Header
	 */
	private HeaderMessage head;
	
	/**
	 * body加密字符串
	 */
	private String body;
	
	/**
	 * 签名
	 */
	private String sign;
	
	/**
	 * 加密密钥
	 */
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

	public HeaderMessage getHead() {
		return head;
	}

	public void setHead(HeaderMessage head) {
		this.head = head;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
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
	
	public Map<String,String> toMap(){
        Map<String,String> map = new HashMap<String, String>();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            Object obj;
            try {
                obj = field.get(this);
                if(obj!=null){
                    map.put(field.getName(), String.valueOf(obj));
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return map;
    }
	
	
	
}
