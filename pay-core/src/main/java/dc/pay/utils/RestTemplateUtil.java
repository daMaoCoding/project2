package dc.pay.utils;/**
 * Created by admin on 2017/6/15.
 */

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import dc.pay.config.RestTemplateConfiguration;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.config.interceptor.TokenInterceptor;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public class RestTemplateUtil {
    private static final Logger log =  LoggerFactory.getLogger(RestTemplateUtil.class);

    private static PoolingHttpClientConnectionManager connMgr;
    private static  RequestConfig requestConfig ;
    private static HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = null;
    private static final HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
    private static final HttpClientBuilder httpClientBuilder  =  HttpClientBuilder.create();
    private static final HttpClientBuilder httpClientBuilderRedirect = HttpClientBuilder.create().setRedirectStrategy(new PayLaxRedirectStrategy()).setDefaultRequestConfig(requestConfig);
    private static final  HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;


    //    Collection<Header> headers = Lists.<Header>newArrayList(
    //            //new BasicHeader("X-Twilio-Client", "java-" + Twilio.VERSION),
    //            // new BasicHeader(  HttpHeaders.USER_AGENT, "twilio-java/" + Twilio.VERSION + " (" + Twilio.JAVA_VERSION + ") custom"),
    //            //new BasicHeader(HttpHeaders.ACCEPT, "application/json"),
    //            new BasicHeader(HttpHeaders.ACCEPT_ENCODING, "utf-8"),
    //            new BasicHeader(HttpHeaders.ACCEPT_CHARSET, "utf-8")
    //    );
    //  httpClientBuilderRedirect.setDefaultHeaders(headers);


    static {
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    return true;
                }
            }).build();
            httpClientBuilder.setSSLContext(sslContext);
            httpClientBuilder.setRetryHandler(new dc.pay.config.PayHttpRequestRetryHandler());
            httpClientBuilderRedirect.setSSLContext(sslContext);

            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslSocketFactory)
                    .build();
            connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            connMgr.setMaxTotal(3000); // 将最大连接数
            connMgr.setDefaultMaxPerRoute(500);// 将每个路由基础的连接增加到20

            requestConfig = RequestConfig.custom()
                .setConnectTimeout(16 * 1000)    //  确定建立连接之前的超时时间,0无限制
                .setSocketTimeout(16 * 1000)    // 等待数据超时时间
                .setConnectionRequestTimeout(10*1000)//连接超时时间,连接不够用的等待时间，不宜过长，必须设置，比如连接不够用时
                .setMaxRedirects(200)
                .build();

            //HttpHost localhost = new HttpHost("www.yeetrack.com", 80);
            //connMgr.setMaxPerRoute(new HttpRoute(localhost), 50);//将目标主机的最大连接数增加到50
            httpClientBuilder.setConnectionManager(connMgr);
            clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(getCloseableHttpClient());
        } catch (Exception e) {
            log.error("[PayServ-RespPayController]Http连接池初始化错误，{}" , e.getMessage(), e);
        }
    }

    public static CloseableHttpClient getCloseableHttpClient(){
        CookieStore cookieStore = new BasicCookieStore();
        CloseableHttpClient client = httpClientBuilder.setKeepAliveStrategy(keepAliveStrat)
                                                      .setConnectionManagerShared(true)
                                                      .setConnectionManager(connMgr)
                                                      .setDefaultRequestConfig(requestConfig)
                                                      .setDefaultCookieStore(cookieStore)
                                                      .build();
        //CloseableHttpClient client = HttpClients.custom().setConnectionManager(connMgr).build();
        //return HttpClients.custom().setConnectionManager(connMgr).setDefaultRequestConfig(requestConfig).setDefaultCookieStore(cookieStore).build();
        return client;
    }


    private static ConnectionKeepAliveStrategy keepAliveStrat = new DefaultConnectionKeepAliveStrategy() {
        @Override
        public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
            long keepAlive = super.getKeepAliveDuration(response, context);
            if (keepAlive == -1) {
                //如果服务器没有设置keep-alive这个参数，我们就把它设置成60秒
                keepAlive = 60000;
            }
            return keepAlive;
        }

    };



    /**
     * RestTemplate
     * @return
     */
    public static  RestTemplate getRestTemplate(){
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory);
        return restTemplate;
    }


    public static  List<HttpMessageConverter<?>> setMessageConverter(RestTemplate restTemplate){
        List<HttpMessageConverter<?>> messageConverters = restTemplate.getMessageConverters();
        Iterator<HttpMessageConverter<?>> iterator = messageConverters.iterator();
        while (iterator.hasNext()) {
            HttpMessageConverter<?> converter = iterator.next();
            if (converter instanceof StringHttpMessageConverter) {
                iterator.remove();
            }
        }
        messageConverters.add(stringHttpMessageConverter());
        restTemplate.setMessageConverters(messageConverters);
        return messageConverters;
    }

    public static StringHttpMessageConverter stringHttpMessageConverter() {
        StringHttpMessageConverter converter = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        converter.setWriteAcceptCharset(false);
        return converter;
    }




    /**
     * Map转MultiValueMap
     * @param param
     * @return
     */
    public static MultiValueMap<String, String> convMapToMultiMap(Map<String,String> param){
        MultiValueMap<String, String>  mutMap= new LinkedMultiValueMap<>();
        Set<Map.Entry<String, String>> entries = param.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            mutMap.add(entry.getKey(),entry.getValue());
        }
        return mutMap;
    }


//    /**
//     *  RestTemplate 模拟form的 Post提交
//     * @param url  提交地址
//     * @param paramMap  参数(普通Map)
//     * @return  字符串 Or Json格式
//     */
//    public static String post(String url,Map paramMap){
//        HttpHeaders headers = new HttpHeaders();
//        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");  //application/x-www-form-urlencoded; charset=UTF-8
//        headers.setContentType(type);
//        //headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
//        HttpEntity<MultiValueMap> formEntity = new HttpEntity<MultiValueMap>(convMapToMultiMap(paramMap), headers);
//        // RestTemplate restTemplate = new RestTemplate(httpClientFactory);
//        RestTemplate restTemplate = RestTemplateUtil.getRestTemplate();
//        String result = restTemplate.postForObject(url, formEntity, String.class); //no news is good news
//        return result;
//    }


    /**
     *  RestTemplate 模拟form的 Post提交  //FormHttpMessageConverter是默认utf-8编码
     * @param url  提交地址
     * @param paramMap  参数(普通Map)
     * @return  字符串 Or Json格式
     */
    public static String postForm(String url,Map paramMap,String charsetName){
        String charsetNameValue = "UTF-8";
        if(org.apache.commons.lang.StringUtils.isNotBlank(charsetName)){
            charsetNameValue = charsetName;
        }
        HttpHeaders headers = new HttpHeaders();
        //MediaType mediaType = MediaType.parseMediaType("application/json; charset=UTF-8");
        //headers.setContentType(type);
        MimeType mimeType = MimeTypeUtils.parseMimeType("application/x-www-form-urlencoded");
        MediaType mediaType = new MediaType(mimeType.getType(),mimeType.getSubtype(), Charset.forName(charsetNameValue));
        headers.setContentType(mediaType);
        //headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Accept", MediaType.APPLICATION_JSON_UTF8.toString()); //APPLICATION_JSON_UTF8 /APPLICATION_JSON
        MultiValueMap multiValueMap = convMapToMultiMap(paramMap);
        HttpEntity<MultiValueMap> formEntity = new HttpEntity<MultiValueMap>(multiValueMap, headers);
        // RestTemplate restTemplate = new RestTemplate(httpClientFactory);
        RestTemplate restTemplate = RestTemplateUtil.getRestTemplate();
        String result = restTemplate.postForObject(url, formEntity, String.class); //no news is good news
        return result;
    }



    /**
     * restFull 请求
     */
    public static  String postJson(String payServUrl, Map  mapParams) throws PayException {
        if (null != mapParams) {
            try {
                RestTemplate restTemplate = getRestTemplate();
                HttpHeaders headers = new HttpHeaders();
                MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
                headers.setContentType(type);
                headers.add("Accept", MediaType.APPLICATION_JSON.toString());
               // headers.add("Connection","close");
                HttpEntity<String> formEntity = new HttpEntity<String>(JSON.toJSONString(mapParams), headers);
                String result = restTemplate.postForObject(payServUrl, formEntity, String.class);
                return result;
            } catch (Exception ex) {
               throw new PayException(ex.getMessage());
            }
        }
        return "";
    }



    /**
     * restFull 请求
     */
    public static  String postJson(String payServUrl,String jsonParamsStr) throws PayException {
        if (StringUtils.isNotBlank(jsonParamsStr) ) {
            try {
                RestTemplate restTemplate = getRestTemplate();
                HttpHeaders headers = new HttpHeaders();
                MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
                headers.setContentType(type);
                headers.add("Accept", MediaType.APPLICATION_JSON.toString());
                // headers.add("Connection","close");
                HttpEntity<String> formEntity = new HttpEntity<String>(jsonParamsStr, headers);
                String result = restTemplate.postForObject(payServUrl, formEntity, String.class);
                return result;
            } catch (Exception ex) {
                throw new PayException(ex.getMessage());
            }
        }
        return "";
    }



    /**
     * 
     * @param payServUrl	请求地址
     * @param data			入参数
     * @param mediaType
     * @param connection	close	Keep-Alive
     * @return
     * @author andrew
     * Dec 30, 2017
     */
    public static  String postStr(String payServUrl, String data,String mediaType,String connection) throws PayException {
        if (null != data) {
            try {
                RestTemplate restTemplate = getRestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.add("Accept", mediaType);
                headers.add("Connection",connection);
                HttpEntity<String> formEntity = new HttpEntity<String>(data, headers);
                String result = restTemplate.postForObject(payServUrl, formEntity, String.class);
                return result;
            } catch (Exception ex) {
                throw new PayException(ex.getMessage());
            }
        }
        return null;
    }

    /**
     * 自定义请求头：通用请求方法
     * 
     * @param payServUrl
     * @param data
     * @param headersMap
     * @return
     * @author andrew
     * Aug 7, 2018
     */
    public static  String postStr(String payServUrl, String data,Map<String, String> headersMap) throws PayException {
        if (null != data) {
            try {
                RestTemplate restTemplate = getRestTemplate();
                HttpHeaders headers = new HttpHeaders();
                if (null == headersMap) {
                    headersMap = new HashMap<>();
                }
                Set<Map.Entry<String, String>> entries = headersMap.entrySet();
                for (Map.Entry<String, String> entry : entries) {
                    headers.add(entry.getKey(),entry.getValue());
                }
//                headers.add("Accept", mediaType);
//                headers.add("Connection",connection);
                HttpEntity<String> formEntity = new HttpEntity<String>(data, headers);
                String result = restTemplate.postForObject(payServUrl, formEntity, String.class);
                return result;
            } catch (Exception ex) {
                throw new PayException(ex.getMessage());
            }
        }
        return null;
    }

    
    public static  String postStr(String payServUrl, String data,String contentType) throws PayException {
        if (null != data) {
            try {
                RestTemplate restTemplate = getRestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.add("Content-Type", contentType);
                HttpEntity<String> formEntity = new HttpEntity<String>(data, headers);
                String result = restTemplate.postForObject(payServUrl, formEntity, String.class);
                return result;
            } catch (Exception ex) {
                throw new PayException(ex.getMessage());
            }
        }
        return null;
    }




    /**
     * 提交请求 data={"",""}
     */
    public static String request(String url, String params,String CHARSET) throws PayException {
        try {
           // System.out.println("请求报文:" + params);
            URL urlObj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(1000 * 5);
            //conn.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
            //conn.setRequestProperty("Accept","*/*");
            conn.setRequestProperty("Charset", CHARSET);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(params.length()));
            OutputStream outStream = conn.getOutputStream();
            outStream.write(params.toString().getBytes(CHARSET));
            outStream.flush();
            outStream.close();
            return getResponseBodyAsString(conn.getInputStream(),CHARSET);
        } catch (Exception ex) {
            throw new PayException(ex.getMessage());
        }
    }



    /**
     * 获取响应报文
     */
    private static String getResponseBodyAsString(InputStream in,String CHARSET) throws PayException {
        try {
            BufferedInputStream buf = new BufferedInputStream(in);
            byte[] buffer = new byte[1024];
            StringBuffer data = new StringBuffer();
            int readDataLen;
            while ((readDataLen = buf.read(buffer)) != -1) {
                data.append(new String(buffer, 0, readDataLen, CHARSET));
            }
            System.out.println("响应报文=" + data);
            return data.toString();
        } catch (Exception ex) {
            throw new PayException(ex.getMessage());
        }

    }








    /**
     * 
     * @param payServUrl	请求地址
     * @param data			入参数
     * @param mediaType		告诉服务器，我要发什么类型的数据
     * @param connection	close	Keep-Alive
     * @param accept		告诉服务器，能接受什么类型
     * @return
     * @author andrew
     * Apr 14, 2018
     */
    public static  String postStr(String payServUrl, String data,String mediaType,String connection,String accept) throws PayException {
        if (null != data) {
            try {
                RestTemplate restTemplate = getRestTemplate();
                HttpHeaders headers = new HttpHeaders();
                MediaType type = MediaType.parseMediaType(mediaType);
                headers.setContentType(type);
                headers.add("Accept", accept);
                headers.add("Connection",connection);
                HttpEntity<String> formEntity = new HttpEntity<String>(data, headers);
                String result = restTemplate.postForObject(payServUrl, formEntity, String.class);
                return result;
            } catch (Exception ex) {
                throw new PayException(ex.getMessage());
            }
        }
        return null;
    }


/*
    MultiValueMap<String, String> requestEntity = new LinkedMultiValueMap<>();
    requestEntity.add("clientFlag", clientFlag);
    requestEntity.add("xml", xml);
    requestEntity.add("verifyData", strMd5);
　　 String s = REST_TEMPLATE.postForObject("http://10.10.129.19/svsr/Receive.asmx/OrderXML", requestEntity, String.class);
*/

    /**
     * UTF8RestTemplate
     * @param url
     * @param params
     * @param var
     * @param method
     * @param <T>
     * @return
     */
    public static <T> T sendByRestTemplate(String url, Map<String,T> params, Class<T> var, HttpMethod method) {
        if(MapUtils.isEmpty(params)) params = Maps.newHashMap();
        RestTemplate restTemplate = RestTemplateUtil.getRestTemplate();
        restTemplate.getInterceptors().add(new TokenInterceptor());
        FormHttpMessageConverter fc = new FormHttpMessageConverter();
        StringHttpMessageConverter s = stringHttpMessageConverter();  //StandardCharsets.UTF_8
        List<HttpMessageConverter<?>> partConverters = new ArrayList<HttpMessageConverter<?>>();
        partConverters.add(s);
        partConverters.add(new ResourceHttpMessageConverter());
        fc.setPartConverters(partConverters);
        restTemplate.getMessageConverters().addAll(Arrays.asList(fc, new MappingJackson2HttpMessageConverter()));
        MultiValueMap<String, T> map = new LinkedMultiValueMap<>();
        map.setAll(params);
        //HttpHeaders headers = new HttpHeaders();
        // headers.set("Accept", "text/plain, application/json, application/*+json,  */*");
        //MediaType type = MediaType.parseMediaType("application/x-www-form-urlencoded; charset=UTF-8");
        //headers.setContentType(type);
        //HttpEntity<String> requestEntity = new HttpEntity<String>(PostStrUtils.getPostStrFromMap(paramMap),  headers);
        //String msg = restTemplate.postForObject(url,requestEntity, String.class);
        return switchMethod(url, params, var, method, restTemplate, map);
    }




    /**
     * UTF8RestTemplate
     * @param url
     * @param params
     * @param var
     * @param method
     * @param <T>
     * @return
     */
    public static <T> T sendByRestTemplateRedirect(String url, Map<String,T> params, Class<T> var, HttpMethod method) {
        if(StringUtils.isNotBlank(url)){  //HandlerUtil.getUrlParams()
            if(params==null)params = Maps.newHashMap();
            final RestTemplate restTemplate = new RestTemplate();
            httpComponentsClientHttpRequestFactory.setHttpClient(httpClientBuilderRedirect.build());
            restTemplate.setRequestFactory(httpComponentsClientHttpRequestFactory);
            restTemplate.getInterceptors().add(new TokenInterceptor());
            FormHttpMessageConverter fc = new FormHttpMessageConverter();
            StringHttpMessageConverter s = stringHttpMessageConverter();  //StandardCharsets.UTF_8
            List<HttpMessageConverter<?>> partConverters = new ArrayList<HttpMessageConverter<?>>();
            partConverters.add(s);
            partConverters.add(new ResourceHttpMessageConverter());
            fc.setPartConverters(partConverters);
            restTemplate.getMessageConverters().addAll(Arrays.asList(fc, new MappingJackson2HttpMessageConverter()));
            MultiValueMap<String, T> map = new LinkedMultiValueMap<>();
            map.setAll(params);
            return switchMethod(url, params, var, method, restTemplate, map);

        }
        return null;
    }


    public static <T> T sendByRestTemplateRedirect(String url, Map<String,T> params, Class<T> var, HttpMethod method,HttpHeaders headers) {
        if(StringUtils.isNotBlank(url)){  //HandlerUtil.getUrlParams()
            if(params==null)params = Maps.newHashMap();
            final RestTemplate restTemplate = new RestTemplate();
            httpComponentsClientHttpRequestFactory.setHttpClient(httpClientBuilderRedirect.build());
            restTemplate.setRequestFactory(httpComponentsClientHttpRequestFactory);
            restTemplate.getInterceptors().add(new TokenInterceptor());
            FormHttpMessageConverter fc = new FormHttpMessageConverter();
            StringHttpMessageConverter s = stringHttpMessageConverter();  //StandardCharsets.UTF_8
            List<HttpMessageConverter<?>> partConverters = new ArrayList<HttpMessageConverter<?>>();
            partConverters.add(s);
            partConverters.add(new ResourceHttpMessageConverter());
            fc.setPartConverters(partConverters);
            restTemplate.getMessageConverters().addAll(Arrays.asList(fc, new MappingJackson2HttpMessageConverter()));
            MultiValueMap<String, T> map = new LinkedMultiValueMap<>();
            map.setAll(params);
            return switchMethod(url, params, var, method, restTemplate, map,headers);
        }
        return null;
    }






    //简单2次提交表单
    public static String sendByRestTemplateRedirectWithSendSimpleForm(String url, Map<String,String> payParam, HttpMethod method) throws PayException {
        String firstPayResultStr =    RestTemplateUtil.sendByRestTemplateRedirect(url, payParam, String.class, method);
        if(StringUtils.isNotBlank(firstPayResultStr) && firstPayResultStr.contains("<form")){
            Map<String, String> secondPayParam = HandlerUtil.parseFormElement(Jsoup.parse(firstPayResultStr).getElementsByTag("form").first());
            String secondPayDomainUrl =  secondPayParam.get(HandlerUtil.ACTION).startsWith("http")||secondPayParam.get(HandlerUtil.ACTION).startsWith("https")?HandlerUtil.getDomain(secondPayParam.get(HandlerUtil.ACTION)):url;
            return HandlerUtil.sendToThreadPayServ(secondPayParam, secondPayDomainUrl).getBody();
        }
        return "";
    }




    private static <T> T switchMethod(String url, Map<String, T> params, Class<T> var, HttpMethod method, RestTemplate restTemplate, MultiValueMap<String, T> map) {
        switch (method) {
            case POST:
                return restTemplate.postForObject(url, map, var);
            case GET:
                String getParams = params==null||params.isEmpty()?"":"?" .concat( map.keySet().stream().map(k -> String.format("%s={%s}", k, k)).collect(Collectors.joining("&")) );
                //  if(null!=params && !params.isEmpty()) return restTemplate.getForObject(url + getParams, var, params);
                return restTemplate.getForObject(url + getParams, var,params);
            default:
                return restTemplate.postForObject(url, map, var);
        }
    }



    private static <T> T switchMethod(String url, Map<String, T> params, Class<T> var, HttpMethod method, RestTemplate restTemplate, MultiValueMap<String, T> map,HttpHeaders headers ) {
        switch (method) {
            case POST:
                return restTemplate.postForObject(url,null!=headers? new HttpEntity(map, headers): map, var);
            case GET:
                String getParams = params==null||params.isEmpty()?"":"?" .concat( map.keySet().stream().map(k -> String.format("%s={%s}", k, k)).collect(Collectors.joining("&")) );
                return    restTemplate.exchange(url + getParams, HttpMethod.GET, new HttpEntity<>(headers), var, params).getBody();
            default:
                return restTemplate.postForObject(url, null!=headers? new HttpEntity(map, headers): map, var);
        }
    }

    static class FollowRedirectsCommonsClientHttpRequestFactory extends SimpleClientHttpRequestFactory {
        //        RestTemplate restTemplate = RestTemplateUtil.getRestTemplate();
        //        RestTemplate restTemplate = new RestTemplate(new FollowRedirectsCommonsClientHttpRequestFactory());
        @Override
        protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
            super.prepareConnection(connection, httpMethod);
            connection.setInstanceFollowRedirects(true);
        }
    }



    //post xml
    public static String postXml(String url,String xmlString) {
        RestTemplate restTemplate =  getRestTemplate();
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
        messageConverters.add(stringHttpMessageConverter());
        restTemplate.setMessageConverters(messageConverters);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        HttpEntity<String> request = new HttpEntity<String>(xmlString, headers);
        final ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        return response.getBody();
    }



    public static Map<String,Boolean> payapiurlhealthcheck(Set<String> apiUrlSet){
        HashMap<String, Boolean> checkResult = Maps.newHashMap();
        if(null!=apiUrlSet && apiUrlSet.size()>0){
            apiUrlSet.forEach(apiUrl->{
                try{
                    if(HandlerUtil.isRigthDomain(apiUrl)){
                        String  resultStr = RestTemplateUtil.sendByRestTemplateRedirect(apiUrl, null, String.class, HttpMethod.GET);
                        checkResult.put(apiUrl, resultStr.equalsIgnoreCase("welcome"));
                    }else{
                        checkResult.put(apiUrl, false);
                    }
                }catch (Exception e){
                    checkResult.put(apiUrl, false);
                }
            });

        }
        return checkResult;
    }

    /**
     *  RestTemplate 模拟form的 Post提交  //FormHttpMessageConverter是默认utf-8编码
     * @param url  提交地址
     * @param paramMap  参数(普通Map)
     * @return  字符串 Or Json格式
     */
    public static String postForm(String url,Map paramMap,String charsetName,Map<String, String> headersMap){
        String charsetNameValue = "UTF-8";
        if(org.apache.commons.lang.StringUtils.isNotBlank(charsetName)){
            charsetNameValue = charsetName;
        }
        HttpHeaders headers = new HttpHeaders();
        //MediaType mediaType = MediaType.parseMediaType("application/json; charset=UTF-8");
        //headers.setContentType(type);
        MimeType mimeType = MimeTypeUtils.parseMimeType("application/x-www-form-urlencoded");
        MediaType mediaType = new MediaType(mimeType.getType(),mimeType.getSubtype(), Charset.forName(charsetNameValue));
        if (null == headersMap) {
            headersMap = new HashMap<>();
        }
        Set<Map.Entry<String, String>> entries = headersMap.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            headers.add(entry.getKey(),entry.getValue());
        }
        headers.setContentType(mediaType);
        //headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        //headers.add("Accept", MediaType.APPLICATION_JSON_UTF8.toString()); //APPLICATION_JSON_UTF8 /APPLICATION_JSON
        MultiValueMap multiValueMap = convMapToMultiMap(paramMap);
        HttpEntity<MultiValueMap> formEntity = new HttpEntity<MultiValueMap>(multiValueMap, headers);
        // RestTemplate restTemplate = new RestTemplate(httpClientFactory);
        RestTemplate restTemplate = RestTemplateUtil.getRestTemplate();
        String result = restTemplate.postForObject(url, formEntity, String.class); //no news is good news
        return result;
    }





}
