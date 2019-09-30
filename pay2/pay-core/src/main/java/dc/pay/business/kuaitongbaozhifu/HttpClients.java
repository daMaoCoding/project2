package dc.pay.business.kuaitongbaozhifu;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletRequest;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * The Class HttpClients.
 *
 * @author wangxian--->2015-8-12 14:58:17$$
 */
public class HttpClients {


    /**
     * Builder.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The Class Builder.
     *
     * @author wangxian--->2015-8-12 14:58:17$$
     */
    public static class Builder {

        /**
         * The url character.
         */
        private static String URL_CHARACTER = "utf-8"; // 统一字符集

        /**
         * The http client.
         */
        private static HttpClient httpClient;

        /**
         * The cookie store.
         */
        static CookieStore cookieStore = null;

        /**
         * The http response.
         */
        private String httpResponse;

        /**
         * Builds the.
         *
         * @return the builder
         */
        public Builder build() {
            return this;
        }

        /**
         * Inits the.
         *
         * @return the builder
         */
        public Builder init() {
            synchronized (HttpClients.class) {
                if (httpClient == null) {
                    PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
                    cm.setMaxTotal(1000);
                    httpClient = new DefaultHttpClient(cm);
                    Runtime.getRuntime().addShutdownHook(
                            new Thread(new Runnable() {

                                @Override
                                public void run() {
                                    destroy();
                                }
                            }));
                }
            }
            return this;
        }

        /**
         * Destroy.
         */
        private void destroy() {
            synchronized (HttpClients.class) {
                if (httpClient != null) {
                    httpClient.getConnectionManager().shutdown();
                    httpClient = null;
                }
            }
        }

        /**
         * As response string.
         *
         * @return the string
         */
        public String asResponseString() {
            return httpResponse;
        }

        /**
         * Checks if is success.
         *
         * @return true, if is success
         */
        public boolean isSuccess() {
            return StringUtils.isNotBlank(httpResponse);
        }

        public Builder post(final String url, final Map<String, Object> params) {
            return post(url, params, null);
        }

        /**
         * Post.
         *
         * @param url    the url
         * @param params the params
         * @return the builder
         */
        public Builder post(final String url, final Map<String, Object> params,
                            final Map<String, String> headres) {
            this.httpResponse = invoke(new InvokeCallable<String>() {

                @Override
                public String call(HttpClient httpClient) {
                    httpClient.getParams().setParameter(
                            CoreProtocolPNames.PROTOCOL_VERSION,
                            HttpVersion.HTTP_1_1);
                    HttpClientParams.setCookiePolicy(httpClient.getParams(),
                            CookiePolicy.BROWSER_COMPATIBILITY);
                    HttpPost httpPost = new HttpPost(url);
                    List<NameValuePair> nameValuePairs = Lists.newArrayList();
                    for (Map.Entry<String, Object> entry : params.entrySet()) {
                        nameValuePairs.add(new BasicNameValuePair(entry
                                .getKey(), entry.getValue() + ""));
                    }
                    if (headres != null) {
                        for (Map.Entry<String, String> entry : headres
                                .entrySet()) {
                            httpPost.addHeader(entry.getKey(), entry.getValue());
                        }
                    }
                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs,
                            Charset.defaultCharset()));
                    HttpContext context = new BasicHttpContext();
                    try {
                        HttpResponse remoteResponse = httpClient.execute(
                                httpPost, context);
                        return EntityUtils.toString(remoteResponse.getEntity(),
                                URL_CHARACTER);
                    } catch (Exception e) {
                        httpPost.abort();
                        return null;
                    } finally {
                        if (httpPost != null)
                            httpPost.releaseConnection();
                    }
                }
            });
            return this;
        }

        public Builder get(final String url) {
            return get(url, null);
        }

        /**
         * Gets the.
         *
         * @param url the url
         * @return the builder
         */
        public Builder get(final String url, final Map<String, String> headres) {
            this.httpResponse = invoke(new InvokeCallable<String>() {

                @Override
                public String call(HttpClient httpClient) {
                    httpClient.getParams().setParameter(
                            CoreProtocolPNames.PROTOCOL_VERSION,
                            HttpVersion.HTTP_1_1);
                    httpClient
                            .getParams()
                            .setParameter(CoreProtocolPNames.USER_AGENT,
                                    "Mozilla/5.0 (X11; U; Linux i686; zh-CN; rv:1.9.1.2) Gecko/20090803");
                    HttpClientParams.setCookiePolicy(httpClient.getParams(),
                            CookiePolicy.BROWSER_COMPATIBILITY);
                    HttpGet httpGet = new HttpGet(url);
                    if (headres != null) {
                        for (Map.Entry<String, String> entry : headres
                                .entrySet()) {
                            httpGet.addHeader(entry.getKey(), entry.getValue());
                        }
                    }
                    HttpContext context = new BasicHttpContext();
                    try {
                        HttpResponse remoteResponse = httpClient.execute(
                                httpGet, context);
                        return EntityUtils.toString(remoteResponse.getEntity(),
                                URL_CHARACTER);
                    } catch (Exception e) {
                        httpGet.abort();
                        return null;
                    } finally {
                        if (httpGet != null)
                            httpGet.releaseConnection();
                    }
                }
            });
            return this;
        }

        /**
         * The Interface InvokeCallable.
         *
         * @param <T> the generic type
         * @author wangxian--->2015-8-12 14:58:17$$
         */
        interface InvokeCallable<T> {

            /**
             * Call.
             *
             * @param httpClient the http client
             * @return the t
             */
            T call(HttpClient httpClient);
        }

        /**
         * Invoke.
         *
         * @param <T>      the generic type
         * @param callable the callable
         * @return the t
         */
        private <T> T invoke(InvokeCallable<T> callable) {
            try {
                return callable.call(httpClient);
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }

    }

    /**
     * POST 请求
     *
     * @param url       url地址
     * @param jsonParam 参数
     * @return
     */
    public static JSONObject doPost(String url, JSONObject jsonParam) {
        // post请求返回结果
        DefaultHttpClient httpClient = new DefaultHttpClient();
        JSONObject jsonResult = null;
        HttpPost method = new HttpPost(url);
        try {
            if (null != jsonParam) {
                // 解决中文乱码问题
                StringEntity entity = new StringEntity(jsonParam.toString(), "UTF-8");
                entity.setContentEncoding("UTF-8");
                entity.setContentType("application/json");
                method.setEntity(entity);
            }
            HttpResponse result = httpClient.execute(method);
            url = URLDecoder.decode(url, "UTF-8");
            /** 请求发送成功，并得到响应 **/
            if (result.getStatusLine().getStatusCode() == 200) {
                String str = "";
                try {
                    /** 读取服务器返回过来的json字符串数据 **/
                    str = EntityUtils.toString(result.getEntity(), "utf-8");

                    /** 把json字符串转换成json对象 **/
                    jsonResult = JSONObject.fromObject(str);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonResult;
    }



    /**
     * htppsPOST请求忽略SSL
     *
     * @param url       url地址
     * @param jsonParam 参数
     * @return
     */
    public static JSONObject doHttpsPost(String url, JSONObject jsonParam) {
        // post请求返回结果
        DefaultHttpClient httpClient = new DefaultHttpClient();
        JSONObject jsonResult = null;
        HttpPost method = new HttpPost(url);
        try {
            if (null != jsonParam) {
                // 解决中文乱码问题
                StringEntity entity = new StringEntity(jsonParam.toString(), "UTF-8");
                entity.setContentEncoding("UTF-8");
                entity.setContentType("application/json");
                method.setEntity(entity);
            }
            enableSSL(httpClient);
            HttpResponse result = httpClient.execute(method);
            url = URLDecoder.decode(url, "UTF-8");
            /** 请求发送成功，并得到响应 **/
            if (result.getStatusLine().getStatusCode() == 200) {
                String str = "";
                try {
                    /** 读取服务器返回过来的json字符串数据 **/
                    str = EntityUtils.toString(result.getEntity(), "utf-8");

                    /** 把json字符串转换成json对象 **/
                    jsonResult = JSONObject.fromObject(str);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonResult;
    }

    /**
     * 访问https的网站
     *
     * @param httpclient
     */
    public static void enableSSL(DefaultHttpClient httpclient) {
        // 调用ssl
        try {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, new TrustManager[]{truseAllManager}, null);
            SSLSocketFactory sf = new SSLSocketFactory(sslcontext);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            Scheme https = new Scheme("https", sf, 443);
            httpclient.getConnectionManager().getSchemeRegistry().register(https);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 重写验证方法，取消检测ssl
     */
    public static TrustManager truseAllManager = new X509TrustManager() {

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                throws java.security.cert.CertificateException {
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                throws java.security.cert.CertificateException {
        }

    };
    /**
     * 小树
     */
    public static String post(String url, JSONObject jsonParam) {
        // post请求返回结果
        DefaultHttpClient httpClient = new DefaultHttpClient();
        String resulr = "";
        HttpPost method = new HttpPost(url);
        try {
            if (null != jsonParam) { // 解决中文乱码问题
                StringEntity entity = new StringEntity(jsonParam.toString(), "UTF-8");
                entity.setContentEncoding("UTF-8");
                entity.setContentType("application/json");
                method.setEntity(entity);
            }
            HttpResponse result = httpClient.execute(method);
            url = URLDecoder.decode(url, "UTF-8");
            /** 请求发送成功，并得到响应 **/
            if (result.getStatusLine().getStatusCode() == 200) {
                /** 读取服务器返回过来的json字符串数据 **/
                resulr = EntityUtils.toString(result.getEntity(), "utf-8");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resulr;
    }

    public static String post(String url, String param) throws Exception {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        String resulr = "";
        HttpPost method = new HttpPost(url);

        if (null != param) { // 解决中文乱码问题
            StringEntity entity = new StringEntity(param, "UTF-8");
            entity.setContentEncoding("UTF-8");
            entity.setContentType("application/json");
            method.setEntity(entity);
        }
        HttpResponse result = httpClient.execute(method);
        url = URLDecoder.decode(url, "UTF-8");
        if (result.getStatusLine().getStatusCode() == 200) {
            resulr = EntityUtils.toString(result.getEntity(), "utf-8");
        }

        return resulr;
    }


    /**
     * 信任https证书的post请求
     * @param url
     * @param param
     * @return
     * @throws Exception
     */
    public static String httpsPost(String url, String param) throws Exception {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        String resulr = "";
        HttpPost method = new HttpPost(url);

        if (null != param) { // 解决中文乱码问题
            StringEntity entity = new StringEntity(param, "UTF-8");
            entity.setContentEncoding("UTF-8");
            entity.setContentType("application/json");
            method.setEntity(entity);
        }
        enableSSL(httpClient);
        HttpResponse result = httpClient.execute(method);
        url = URLDecoder.decode(url, "UTF-8");
        if (result.getStatusLine().getStatusCode() == 200) {
            resulr = EntityUtils.toString(result.getEntity(), "utf-8");
        }

        return resulr;
    }

    /**
     * 获取请求Body
     *
     * @param request
     * @return
     */
    public static String getBodyString(ServletRequest request) {
        StringBuilder sb = new StringBuilder();
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            inputStream = request.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
            String line = "";
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }

    /**
     * 向指定URL发送GET方法的请求
     *
     * @param url   发送请求的URL
     * @param param 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendGet(String url, String param) {
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url + "?" + param;
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            // 遍历所有的响应头字段
            for (String key : map.keySet()) {
                System.out.println(key + "--->" + map.get(key));
            }
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }


    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url   发送请求的 URL
     * @param param 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     */
    public static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！" + e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }


    public static String sendFrom(Map<String, Object> map, String url) {
        CloseableHttpClient httpClient = org.apache.http.impl.client.HttpClients.createDefault();
        //创建post请求
        try {
            HttpPost httpPost = new HttpPost(url);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            for (Map.Entry<String, Object> stringStringEntry : map.entrySet()) {
                builder.addPart(stringStringEntry.getKey(), new StringBody((String) stringStringEntry.getValue(), ContentType.TEXT_PLAIN));
            }
            HttpEntity reqEntity = builder.build();
            httpPost.setEntity(reqEntity);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            try {
                //获取响应对象
                HttpEntity resEntity = response.getEntity();
                if (reqEntity != null) {
                    //响应长度
                    //获取响应内容
                    return EntityUtils.toString(resEntity);
                }
                EntityUtils.consume(resEntity);
            } finally {
                response.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
