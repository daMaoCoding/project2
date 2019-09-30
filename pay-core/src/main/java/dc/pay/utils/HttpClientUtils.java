package dc.pay.utils;

import dc.pay.config.PayHttpRequestRetryHandler;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class HttpClientUtils {
    private static final Logger log =  LoggerFactory.getLogger(HttpClientUtils.class);
    private static HttpClientBuilder httpClientBuilder = null;
    private static  PoolingHttpClientConnectionManager connMgr= null;
    private static  RequestConfig requestConfig ;

    static {
        httpClientBuilder = HttpClientBuilder.create().disableAutomaticRetries().setRedirectStrategy(new LaxRedirectStrategy());//利用LaxRedirectStrategy处理POST重定向问题
        try {

            // setup a Trust Strategy that allows all certificates.
            //
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    return true;
                }
            }).build();
/*
            List<Header> headers = new ArrayList<>();
            headers.add(new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.16 Safari/537.36"));
            headers.add(new BasicHeader("Accept-Encoding", "gzip,deflate"));
            headers.add(new BasicHeader("Accept-Language", "zh-CN,zh;q=0.8,en;q=0.6"));
            headers.add(new BasicHeader("Connection", "keep-alive"));
            httpClientBuilder.setDefaultHeaders(headers);*/


            httpClientBuilder.setSSLContext(sslContext);
            httpClientBuilder.setRetryHandler(new PayHttpRequestRetryHandler());


            // don't check Hostnames, either.
            //      -- use SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you don't want to weaken
            HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;

            // here's the special part:
            //      -- need to create an SSL Socket Factory, to use our weakened "trust strategy";
            //      -- and create a Registry, to register it.
            //
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslSocketFactory)
                    .build();

            // now, we create connection-manager using our Registry.
            //      -- allows multi-threaded use
            connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            connMgr.setMaxTotal(3000); // 将最大连接数增加到1000
            connMgr.setDefaultMaxPerRoute(500);// 将每个路由基础的连接增加到20
            //HttpHost localhost = new HttpHost("www.yeetrack.com", 80);
            //connMgr.setMaxPerRoute(new HttpRoute(localhost), 50);//将目标主机的最大连接数增加到50

            requestConfig = RequestConfig.custom()
                    .setConnectTimeout(20 * 1000)    //  连接超时
                    .setSocketTimeout(20 * 1000)    // 等待数据超时时间
                    .setConnectionRequestTimeout(20*1000)  // 连接超时时间
                    .setConnectionRequestTimeout(20*1000)//连接不够用的等待时间，不宜过长，必须设置，比如连接不够用时
                    .setMaxRedirects(200)
                    .build();

        } catch (Exception e) {
            log.error("[HttpClientUtils]初始化错误，{}",e.getMessage(),e);
        }
    }


    private static ConnectionKeepAliveStrategy keepAliveStrat = new DefaultConnectionKeepAliveStrategy() {
        @Override
        public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
            long keepAlive = super.getKeepAliveDuration(response, context);
            if (keepAlive == -1) {
                //如果服务器没有设置keep-alive这个参数，我们就把它设置成10秒
                 keepAlive = 60000;
            }
            return keepAlive;
        }

    };


    public static CloseableHttpClient getCloseableHttpClient(BasicCookieStore cookieStore){
        if(null==cookieStore || cookieStore.getCookies().size()>20){
            cookieStore = new BasicCookieStore();
        }
        CloseableHttpClient client = httpClientBuilder.setKeepAliveStrategy(keepAliveStrat).setConnectionManager(connMgr).setDefaultRequestConfig(requestConfig).setDefaultCookieStore(cookieStore).setConnectionManagerShared(true).build();
        return client;
    }


    public static CloseableHttpClient getCloseableHttpClientNoRedirect(BasicCookieStore cookieStore){
        if(null==cookieStore || cookieStore.getCookies().size()>20){
            cookieStore = new BasicCookieStore();
        }
        CloseableHttpClient client = httpClientBuilder.setKeepAliveStrategy(keepAliveStrat).setConnectionManager(connMgr).setDefaultRequestConfig(requestConfig).setDefaultCookieStore(cookieStore).disableRedirectHandling().setConnectionManagerShared(true).build();
        return client;
    }

    public static RequestConfig getRequestConfig() {
        return requestConfig;
    }

/*
        <bean id="httpClient" class="com.hupengcool.util.HttpClientUtils" factory-method="acceptsUntrustedCertsHttpClient"/>
        <bean id="clientHttpRequestFactory" class="org.springframework.http.client.HttpComponentsClientHttpRequestFactory">
            <constructor-arg ref="httpClient"/>
        </bean>
        <bean id="restTemplate" class=" org.springframework.web.client.RestTemplate">
            <constructor-arg ref="clientHttpRequestFactory" />
        </bean>
     */
//    @Test
//    public void test01() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
//        BasicCookieStore cookieStore = new BasicCookieStore();
//        CloseableHttpClient httpClient = HttpClientUtils.getCloseableHttpClient(cookieStore);
//        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
//        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory);
//
//
//        //String result = restTemplate.getForObject("http://www.baidu.com",String.class);
//       // System.out.println(result);
//
//        HttpClient httpClient1 = clientHttpRequestFactory.getHttpClient();
//        System.out.println(httpClient1.getClass());
//    }
//

}