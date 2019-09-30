package dc.pay.business.chuangxinzhifu;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpClient {
	public static String sendPost(Map<String,Object> params,String url) {
		CloseableHttpClient client = HttpClientBuilder.create().build();
		RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(30000).setConnectTimeout(30000).setSocketTimeout(30000).build();
		HttpPost httpPost = new HttpPost(url);
		httpPost.setConfig(requestConfig);
		if (null != params) {
			List<NameValuePair> pairList = new ArrayList<NameValuePair>(params.size());
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				NameValuePair pair = new BasicNameValuePair(entry.getKey(), entry.getValue().toString());
				pairList.add(pair);
			}
			httpPost.setEntity(new UrlEncodedFormEntity(pairList, Charset.forName("UTF-8")));
		}
		HttpResponse resultResp = null;
		String result = null;
		try {
			resultResp = client.execute(httpPost);
			if (resultResp.getStatusLine().getStatusCode() == 200) {
				result = EntityUtils.toString(resultResp.getEntity(), "utf8");
			}
			return result;
		} catch (Exception e) {
			log.error("请求httpPost错误：",e);
		}
		return result;
	}
	
}
