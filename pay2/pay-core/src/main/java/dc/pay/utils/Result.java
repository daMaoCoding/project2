package dc.pay.utils;

import com.google.common.collect.Maps;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.CloseableHttpClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ************************
 * httpClient 结果封装
 * @author tony 3556239829
 */
public class Result {

    private CloseableHttpClient closeableHttpClient;
    private HttpClient httpClient;
    private List<Cookie> cookies;
    private HttpEntity httpEntity;
    private HashMap<String, Header> headerAll;
    private Map<String, String> headerAllMap;
    private int statusCode;
    private String body;

    public List<Cookie> getCookies() {
        return cookies;
    }

    public void setCookies(List<Cookie> cookies) {
        this.cookies = cookies;
    }

    public CloseableHttpClient getCloseableHttpClient() {
           return closeableHttpClient;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(CloseableHttpClient closeableHttpClient) {
        this.closeableHttpClient = closeableHttpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }


    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setHeaders(Header[] headers) {
        headerAll = new HashMap<String, Header>();
        headerAllMap = Maps.newHashMap();
        for (Header header : headers) {
            headerAll.put(header.getName(), header);
            headerAllMap.put(header.getName(),header.getValue());
        }
    }


    public HashMap<String, Header> getHeaderAll() {
        return headerAll;
    }


    public Map<String, String> getHeaderAllMap() {
        return headerAllMap;
    }

    public void setHttpEntity(HttpEntity entity) {
        this.httpEntity = entity;
    }

    public HttpEntity getHttpEntity() {
        return httpEntity;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

}