package dc.pay.utils;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HttpResponse {

	private int responseCode;
	private String body;

	public int getResponseCode() {
		return this.responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public String getBody() {
		return this.body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String toString() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		StringBuffer sb = new StringBuffer();
		Field[] fields = this.getClass().getDeclaredFields();
		sb.append(this.getClass().getName() + "{");
		for (int i = 0; i < fields.length; i++) {
			try {
				if (fields[i].get(this) instanceof Date) {
					if (fields[i].get(this) != null) {
						sb.append(fields[i].getName() + ":").append(
								sdf.format(fields[i].get(this))).append(";");
						continue;
					}
				}
				sb.append(fields[i].getName()).append(":").append(
						fields[i].get(this)).append(";");
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			}
		}
		sb.append("}");
		return sb.toString();
	}
}
