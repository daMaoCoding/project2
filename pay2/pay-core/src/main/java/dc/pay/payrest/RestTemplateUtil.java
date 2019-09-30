package dc.pay.payrest;/**
 * Created by admin on 2017/6/25.
 */

import com.alibaba.fastjson.JSON;
import org.apache.http.HttpResponse;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public final class RestTemplateUtil {

    private static HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
    private static HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = null;
    private static final Logger log =  LoggerFactory.getLogger(RespPayController.class);

    static {
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    return true;
                }
            }).build();
            httpClientBuilder.setSSLContext(sslContext);
            HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslSocketFactory)
                    .build();
            PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            connMgr.setMaxTotal(1000); // 将最大连接数增加到250
            connMgr.setDefaultMaxPerRoute(100);// 将每个路由基础的连接增加到20
            //HttpHost localhost = new HttpHost("www.yeetrack.com", 80);
            //connMgr.setMaxPerRoute(new HttpRoute(localhost), 50);//将目标主机的最大连接数增加到50
            httpClientBuilder.setConnectionManager(connMgr);
            clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(getCloseableHttpClient());
        } catch (Exception e) {
            log.error("[PayServ-RespPayController]Http连接池初始化错误，"+e.getMessage(),e);
        }

    }

    private static CloseableHttpClient getCloseableHttpClient(){
        CloseableHttpClient client = httpClientBuilder.setKeepAliveStrategy(keepAliveStrat).setConnectionManagerShared(true).build();
        return client;
    }


    public static RestTemplate getRestTemplate(){
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory);
        return restTemplate;
    }



    private static ConnectionKeepAliveStrategy keepAliveStrat = new DefaultConnectionKeepAliveStrategy() {
        @Override
        public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
            long keepAlive = super.getKeepAliveDuration(response, context);
            if (keepAlive == -1) {
                //如果服务器没有设置keep-alive这个参数，我们就把它设置成20秒
                keepAlive = 20000;
            }
            return keepAlive;
        }
    };




    /**
     * 转发内容至支付网关
     * @param mapParams
     * @return  {"responsePayCode":"SUCCESS","responseOrderID":"456_1496025545611_1496108539616","responseOrderState":"SUCCESS","responsePayErrorMsg":null,"responsePayTotalTime":68,"responsePayMsg":"000000"}
     */
    public static String  postForPayServ(RestTemplate restTemplate,String payServUrl,Map<String,String> mapParams) {
        if (null != mapParams) {
            try {
                // SimpleClientHttpRequestFactory httpClientFactory = new SimpleClientHttpRequestFactory();
                //httpClientFactory.setConnectTimeout(5000);
                //httpClientFactory.setReadTimeout(15000);
                // RestTemplate restTemplate = new RestTemplate(httpClientFactory);
                // HttpEntity<String> formEntity = new HttpEntity<String>(new Gson().toJson(mapParams), headers);
                HttpHeaders headers = new HttpHeaders();
                MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
                //headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
                headers.setContentType(type);

                headers.add("Accept", MediaType.APPLICATION_JSON.toString());
                headers.add("Connection","close");
                HttpEntity<String> formEntity = new HttpEntity<String>(JSON.toJSONString(mapParams), headers);
                String result = restTemplate.postForObject(payServUrl, formEntity, String.class);
                return result;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }


}
