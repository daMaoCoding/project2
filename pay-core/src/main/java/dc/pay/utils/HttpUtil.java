package dc.pay.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HTMLParser;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import dc.pay.base.processor.PayException;
import dc.pay.constant.SERVER_MSG;
import org.apache.commons.lang.StringUtils;
import org.apache.http.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.*;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * ************************
 * httputil工具类
 * @author tony 3556239829
 */
public class HttpUtil {
    private static final Logger log =  LoggerFactory.getLogger(HttpUtil.class);

    /**
     * 组装getURL参数
     * @param url
     * @param params
     * @return
     * @throws IOException
     */
    public static String getURLWithParam(String url,Map<String, String> params)  {
       return null == params&&CollectionUtils.isEmpty(params) ? url : url + "?" + parseParam(params);
    }


    public  static String getPOSTURLWithParam(String url,Map<String, String> payParam){
        StringBuffer sbHtml = new StringBuffer();
        sbHtml.append("<form id='postForm' name='mobaopaysubmit' action='"+ url + "' method='post'>");
        for (Map.Entry<String, String> entry : payParam.entrySet()) {
            sbHtml.append("<input type='hidden' name='"+ entry.getKey() + "' value='" + entry.getValue()+ "'/>");
        }
        sbHtml.append("</form>");
        sbHtml.append("<script>document.forms['postForm'].submit();</script>");
        return sbHtml.toString();
    }

    /**
     * //TODO get请求
     *
     * @param url     请求地址
     * @param headers 请求头
     * @param params  请求参数
     * @return
     * @throws IOException
     * @throws ClientProtocolException
     */
    public static Result get(String url, Map<String, String> headers, Map<String, String> params) throws IOException {
        CloseableHttpClient     client = null;
        CloseableHttpResponse  response = null;
        Result  result = new Result();
        HttpGet  httpGet = null;
        try{
            BasicCookieStore  cookieStore = new BasicCookieStore();
             client = HttpClientUtils.getCloseableHttpClient(cookieStore);
            //client = RestTemplateUtil.getCloseableHttpClient();
            url = (null == params|| CollectionUtils.isEmpty(params) ? url : url + "?" + parseParam(params));
            log.debug("[HttpUtil-get]: {}",url);
            httpGet = new HttpGet(url);
            httpGet.setHeaders(parseHeader(headers));
            response = client.execute(httpGet,HttpClientContext.create());
            HttpEntity entity = new BufferedHttpEntity(response.getEntity());
            final StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() >= org.apache.http.HttpStatus.SC_MULTIPLE_CHOICES||statusLine.getStatusCode()<org.apache.http.HttpStatus.SC_OK) {
                //主动回收httpclient连接
                EntityUtils.consume(entity);
                throw new HttpResponseException(statusLine.getStatusCode(),  statusLine.getReasonPhrase());
            }
            result.setHttpClient(client);
            result.setCookies(cookieStore.getCookies());
            result.setStatusCode(response.getStatusLine().getStatusCode());
            result.setHeaders(response.getAllHeaders());
            result.setHttpEntity(entity);
            result.setBody(EntityUtils.toString(entity));
        }catch (IOException ex){
            throw  ex;
        }finally {
            if(null!=client)
                client.close();
            if(httpGet!=null)
                httpGet.releaseConnection();
            if(null!=response)
                response.close();
        }
        return result;
    }



    public static Result getNoRedirects(String url,Map<String, String> headers) throws IOException {
        HttpClient client = HttpClientBuilder.create().disableRedirectHandling().build();
        HttpResponse response = null;
        Result  result = new Result();
        try{
            BasicCookieStore  cookieStore = new BasicCookieStore();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeaders(parseHeader(headers));
            response = client.execute(httpGet);
            HttpEntity entity = new BufferedHttpEntity(response.getEntity());
            final StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() >= org.apache.http.HttpStatus.SC_MULTIPLE_CHOICES||statusLine.getStatusCode()<org.apache.http.HttpStatus.SC_OK) {
                //主动回收httpclient连接
                EntityUtils.consume(entity);
                throw new HttpResponseException(statusLine.getStatusCode(),  statusLine.getReasonPhrase());
            }
            result.setHttpClient(client);
            result.setCookies(cookieStore.getCookies());
            result.setStatusCode(response.getStatusLine().getStatusCode());
            result.setHeaders(response.getAllHeaders());
            result.setHttpEntity(entity);
            result.setBody(EntityUtils.toString(entity));
        }catch (Exception ex){
            log.error("发送Get非转向请求失败: "+url+ex.getMessage(),ex);
            throw  ex;
        }finally {

        }
        return result;
        //assertThat(response.getStatusLine().getStatusCode(), equalTo(301));
    }


/*

    public static Result getNoRedirects(String url, Map<String, String> headers, Map<String, String> params) throws IOException {
        CloseableHttpClient     client = null;
        CloseableHttpResponse  response = null;
        Result  result = new Result();
        try{
            BasicCookieStore  cookieStore = new BasicCookieStore();
            client = HttpClientUtils.getCloseableHttpClientNoRedirect(cookieStore);
            url = (null == params ? url : url + "?" + parseParam(params));
            log.debug("[HttpUtil-get]: {}",url);
            HttpGet  get = new HttpGet(url);
           // get.setConfig(createConfig(10000, false));
           // get.setHeaders(parseHeader(headers));
            response = client.execute(get,HttpClientContext.create());
            HttpEntity entity = new BufferedHttpEntity(response.getEntity());
            result.setHttpClient(client);
            result.setCookies(cookieStore.getCookies());
            result.setStatusCode(response.getStatusLine().getStatusCode());
            result.setHeaders(response.getAllHeaders());
            result.setHttpEntity(entity);
            result.setBody(EntityUtils.toString(entity));
        }catch (IOException ex){
            throw  ex;
        }finally {
            if(null!=client)
                client.close();
            if(null!=response)
                response.close();
        }
        return result;
    }
*/



    /**
     *  302会自动转向
     * @param url      请求地址
     * @param headers  请求头
     * @param params   请求参数
     * @param encoding 请求编码
     * @return
     * @throws IOException
     * @throws ClientProtocolException
     */
    public static Result post(String url, Map<String, String> headers, Map<String, String> params, String encoding) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        CloseableHttpClient     client = null;
        CloseableHttpResponse  response = null;
        HttpPost  httpPost = null;
        Result  result = new Result();
        try {
            BasicCookieStore cookieStore = new BasicCookieStore();
            client = HttpClientUtils.getCloseableHttpClient(cookieStore);
            //client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
            HttpPost  post = new HttpPost(url);
            post.setConfig(HttpClientUtils.getRequestConfig());
            List<NameValuePair> list = new ArrayList<NameValuePair>();
            for (String temp : params.keySet()) {
                list.add(new BasicNameValuePair(temp, params.get(temp)));
            }
            post.setEntity(new UrlEncodedFormEntity(list, Charset.forName(encoding)));
            if(null!=headers && !headers.isEmpty())  post.setHeaders(parseHeader(headers));
            response = client.execute(post, HttpClientContext.create());
            HttpEntity  entity = new BufferedHttpEntity(response.getEntity()) ;
            final StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() >= org.apache.http.HttpStatus.SC_MULTIPLE_CHOICES||statusLine.getStatusCode()<org.apache.http.HttpStatus.SC_OK) {
                //主动回收httpclient连接
                EntityUtils.consume(entity);
                throw new HttpResponseException(statusLine.getStatusCode(),  statusLine.getReasonPhrase());
            }
            result.setHttpClient(client);
            result.setCookies(cookieStore.getCookies());
            result.setStatusCode(response.getStatusLine().getStatusCode());
            result.setHeaders(response.getAllHeaders());
            result.setHttpEntity(entity);
            result.setBody(EntityUtils.toString(entity, encoding));
        } catch (Exception e) {
            log.error("[HttpUtil]post出错： {}",e.getMessage(),e);
           throw  e;
        }finally {
            if(null!=client)
                client.close();
            if(httpPost!=null)
                httpPost.releaseConnection();
            if(null!=response)
                response.close();
        }
        return result;
    }



    public static JSONObject postJson(String url, Map<String, String> headers,Map<String,String> params) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        CloseableHttpClient     client = null;
        CloseableHttpResponse  response = null;
        JSONObject jsonResponse = null;
        HttpPost  httpPost = null;
        try {
            BasicCookieStore cookieStore = new BasicCookieStore();
            client = HttpClientUtils.getCloseableHttpClient(cookieStore);
            //client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
            HttpPost  post = new HttpPost(url);
            post.setConfig(HttpClientUtils.getRequestConfig());
            StringEntity stringEntity = new StringEntity(JSON.toJSONString(params));
            stringEntity.setContentEncoding("UTF-8");
            stringEntity.setContentType("application/json");
            post.setEntity(stringEntity);
            if(null!=headers && !headers.isEmpty())  post.setHeaders(parseHeader(headers));
            response = client.execute(post, HttpClientContext.create());
            if(response.getStatusLine().getStatusCode() == HttpStatus.OK.value()){
                HttpEntity entity = response.getEntity();
                final StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() >= org.apache.http.HttpStatus.SC_MULTIPLE_CHOICES||statusLine.getStatusCode()<org.apache.http.HttpStatus.SC_OK) {
                    //主动回收httpclient连接
                    EntityUtils.consume(entity);
                    throw new HttpResponseException(statusLine.getStatusCode(),  statusLine.getReasonPhrase());
                }
                String charset = EntityUtils.getContentCharSet(entity);
                InputStreamReader inputStreamReader = new InputStreamReader(entity.getContent(), charset);
                BufferedReader bufferedReader=new BufferedReader(inputStreamReader);
                String line;
                StringBuilder stringBuilder=new StringBuilder();
                while ((line=bufferedReader.readLine())!=null){
                    stringBuilder.append(line);
                }
                bufferedReader.close();
                inputStreamReader.close();
                jsonResponse =JSON.parseObject(stringBuilder.toString());
            }

        } catch (Exception e) {
            log.error("[HttpUtil]postJson出错： {}",e.getMessage(),e);
            throw  e;
        }finally {
            if(null!=client)
                client.close();
            if(httpPost!=null)
                httpPost.releaseConnection();
            if(null!=response)
                response.close();
        }
        return jsonResponse;
    }





    public static Result postJson(String url, Map<String, String> headers,Map<String,String> params,String encoding) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        CloseableHttpClient     client = null;
        CloseableHttpResponse  response = null;
        Result  result = new Result();
        HttpPost  httpPost = null;
        try {
            BasicCookieStore cookieStore = new BasicCookieStore();
            client = HttpClientUtils.getCloseableHttpClient(cookieStore);
            //client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
            HttpPost  post = new HttpPost(url);
            post.setConfig(HttpClientUtils.getRequestConfig());

            StringEntity stringEntity = new StringEntity(JSON.toJSONString(params));
            stringEntity.setContentEncoding("UTF-8");
            stringEntity.setContentType("application/json");
            post.setEntity(stringEntity);

            if(null!=headers && !headers.isEmpty())  post.setHeaders(parseHeader(headers));
            response = client.execute(post, HttpClientContext.create());
            HttpEntity  entity = new BufferedHttpEntity(response.getEntity()) ;
            final StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() >= org.apache.http.HttpStatus.SC_MULTIPLE_CHOICES||statusLine.getStatusCode()<org.apache.http.HttpStatus.SC_OK) {
                //主动回收httpclient连接
                EntityUtils.consume(entity);
                throw new HttpResponseException(statusLine.getStatusCode(),  statusLine.getReasonPhrase());
            }
            result.setHttpClient(client);
            result.setCookies(cookieStore.getCookies());
            result.setStatusCode(response.getStatusLine().getStatusCode());
            result.setHeaders(response.getAllHeaders());
            result.setHttpEntity(entity);
            result.setBody(EntityUtils.toString(entity, encoding));

        } catch (Exception e) {
            log.error("[HttpUtil]postJson出错： {}",e.getMessage(),e);
            throw  e;
        }finally {
            if(null!=client)
                client.close();
            if(httpPost!=null)
                httpPost.releaseConnection();
            if(null!=response)
                response.close();
        }
        return result;
    }



    //post xml
    public static String  postXml(String url,String  xmlString) throws PayException{
        if(StringUtils.isNotBlank(url) && StringUtils.isNotBlank(xmlString) ){
            CloseableHttpClient httpclient=null;
            HttpPost httpPost = null;
            try {
                httpclient = HttpClients.createDefault();
                httpPost = new HttpPost(url);
                httpPost.addHeader("Content-Type","text/html;charset=UTF-8");
                StringEntity stringEntity = new StringEntity(xmlString,"UTF-8");
                stringEntity.setContentEncoding("UTF-8");
                httpPost.setEntity(stringEntity);
                //CloseableHttpResponse response = httpclient.execute(httpPost);
                // System.out.println("Executing request " + httpPost.getRequestLine());
                //  Create a custom response handler
                ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                    @Override
                    public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {//
                        int status = response.getStatusLine().getStatusCode();
                        if (status >= 200 && status < 300) {
                            org.apache.http.HttpEntity entity = response.getEntity();
                            return entity != null ? EntityUtils.toString(entity) : null;
                        } else {
                            throw new ClientProtocolException(  "Unexpected response status: " + status);
                        }
                    }
                };
                return httpclient.execute(httpPost, responseHandler);

            } catch (Exception e) {
                log.error(e.getMessage(),e);
                throw new PayException("提交postXml出错"+e.getMessage());
            }finally {
                if(null!=httpclient) {
                    try {
                        httpclient.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(httpPost!=null)
                    httpPost.releaseConnection();

            }
        }
        return null;

    }








    //TODO 转换header
   static synchronized  Header[] parseHeader(Map<String, String> headers) {
        if (null == headers || headers.isEmpty()) {
            return getDefaultHeaders();
        }
        Header[] allHeader = new BasicHeader[headers.size()];
        int i = 0;
        for (String str : headers.keySet()) {
            allHeader[i] = new BasicHeader(str, headers.get(str));
            i++;
        }
        return allHeader;
    }

   //TODO 默认header
    public static Header[] getDefaultHeaders() {
        Header[] allHeader = new BasicHeader[2];
        allHeader[0] = new BasicHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        allHeader[1] = new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.146 Safari/537.36");
        return allHeader;
    }


     //TODO 转换参数列表
    private static String parseParam(Map<String, String> params)  {
        try {
            if (null == params || params.isEmpty()) {
                return "";
            }
            StringBuffer sb = new StringBuffer();
            for (String key : params.keySet()) {
                sb.append(key + "=" + URLEncoder.encode(params.get(key),"UTF-8")  + "&");
            }
            return sb.substring(0, sb.length() - 1);
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 释放httpclient对象
     */
    public static void closeClient(CloseableHttpClient client) {
        if (null != client) {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



/********************************************************************************************************************/


    /**
     *  RestTemplate    模拟form的 Post提交
     * @param url       提交地址
     * @param paramMap  参数(普通Map)
     * @return  字符串 Or Json格式
     */
//    public static String sendPostForMap(String url,Map paramMap,String charsetName){
//        String result = RestTemplateUtil.postForm(url, paramMap,charsetName);
//        return result;
//    }



    /**
     * 发送string
     * @param url
     * @param params
     * @return
     */
    public static String sendPostForString(String url, String params) throws PayException{
        HttpURLConnection conn = null;
        OutputStream outStream = null;
        try {
            URL urlObj = new URL(url);
            conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(1000 * 16);
            conn.setReadTimeout(1000*16);
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");  //application/x-www-form-urlencoded
            conn.setRequestProperty("Content-Length",String.valueOf(params.length()));
            outStream = conn.getOutputStream();
            outStream.write(params.toString().getBytes("UTF-8"));
            outStream.flush();
            return getResponseBodyAsString(conn.getInputStream());
        } catch (Exception e) {
            throw  new PayException(e.getMessage(),e);
        }finally {
            if(null!=conn){
                conn.disconnect();
            }
            if(null!=outStream){
                try{
                    outStream.close();
                }catch (Exception e){
                    throw  new PayException(e.getMessage(),e);
                }finally {
                    outStream = null;
                }
            }
        }
    }





    /**
     * 发送string-代理
     */
    public  static   String sendPostForStringProxy(String url, String params) throws PayException {
        String reqPayProxyserv = HandlerUtil.getReqPayProxyserv();
        if(StringUtils.isBlank(reqPayProxyserv)){
            return sendPostForString(url, params);
        }else{
            String[] proxys = reqPayProxyserv.split(",");
            for (int i = 0; i < proxys.length; i++) {
                if(!proxys[i].contains(":"))
                    continue;
                String proxyServ = proxys[i].split(":")[0];
                String proxyServPort = proxys[i].split(":")[1];

                HttpURLConnection conn = null;
                OutputStream outStream = null;
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyServ, Integer.parseInt(proxyServPort)));
                try {
                    URL urlObj = new URL(url);
                    conn = (HttpURLConnection) urlObj.openConnection(proxy);
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    conn.setUseCaches(false);
                    conn.setConnectTimeout(1000 * 20);
                    conn.setReadTimeout(1000*20);
                    conn.setRequestProperty("Charset", "UTF-8");
                    conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");  //application/x-www-form-urlencoded
                    conn.setRequestProperty("Content-Length",String.valueOf(params.length()));
                    outStream = conn.getOutputStream();
                    outStream.write(params.toString().getBytes("UTF-8"));
                    outStream.flush();
                    return getResponseBodyAsString(conn.getInputStream());
                } catch (Exception e) {
                    if(i==proxys.length-1)
                     throw  new PayException(e.getMessage(),e);
                    continue;
                }finally {
                    if(null!=conn){
                        conn.disconnect();
                    }
                    if(null!=outStream){
                        try{
                            outStream.close();
                        }catch (Exception e){
                            throw  new PayException(e.getMessage(),e);
                        }finally {
                            outStream = null;
                        }
                    }
                }


            }
        }
        return null;
    }


    /**
     * 读取response响应结果
     */
    private static String getResponseBodyAsString(InputStream inputStream) throws PayException {
        BufferedInputStream bufferedInputStream = null;
        try {
            bufferedInputStream = new BufferedInputStream(inputStream);
            byte[] buffer = new byte[1024];
            StringBuffer data = new StringBuffer();
            int readDataLen;
            while ((readDataLen = bufferedInputStream.read(buffer)) != -1) {
                data.append(new String(buffer, 0, readDataLen, "UTF-8"));
            }
            return data.toString();
        } catch (Exception e) {
            throw  new PayException(SERVER_MSG.REQUEST_PAY_ERROR,e);
        }finally {
            if(null!=inputStream){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    throw  new PayException(SERVER_MSG.REQUEST_PAY_ERROR,e);
                }finally {
                    inputStream = null;
                }
            }
            if(null!=bufferedInputStream){
                try {
                    bufferedInputStream.close();
                } catch (IOException e) {
                    throw  new PayException(SERVER_MSG.REQUEST_PAY_ERROR,e);
                }
            }
        }
    }





    /**
     * 通用获取request中参数
     */
    public static void  getParameterMap(HttpServletRequest request) throws IOException {
        // 将资料解码
        String reqBody = null;

        // 读取请求内容
        try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(request.getInputStream()))) {
            String line = null;
            StringBuilder sb = new StringBuilder();
            while((line = bufferedReader.readLine())!=null){
                sb.append(line);
            }
            reqBody = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Object parse = JSONObject.parse(reqBody);
        String contentType = request.getContentType();
        Map map = request.getParameterMap();
        for (Object key : map.keySet()) {
            System.out.println(key+":"+request.getParameter(key.toString()));
        }
    }


/********************************************************************************************************************/


    /**
     * for QiQi支付
     */

    public static String doPost(String url, Map<String, ?> map){
        String result = null;

        HttpPost httpPost = new HttpPost(url);
        //拼接参数
        List<NameValuePair> list = new ArrayList<NameValuePair>();
        Iterator<? extends Map.Entry<String, ?>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ?> elem = (Map.Entry<String, ?>) iterator.next();
            BasicNameValuePair basicNameValuePair = new BasicNameValuePair(elem.getKey(), elem.getValue() == null ? "" : elem.getValue().toString());
            list.add(basicNameValuePair);
        }

        try {

            if (list.size() > 0) {
                httpPost.setEntity(new UrlEncodedFormEntity(list, Charset.forName("utf-8")));
            }
            CloseableHttpResponse response = HttpClients.createDefault().execute(httpPost);

            try {
                if (response != null) {
                    HttpEntity resEntity = response.getEntity();
                    if (resEntity != null) {
                        result = EntityUtils.toString(resEntity);
                    }
                }
            } finally {
                if(null!=response) response.close();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }





    /**
     * for 豪联支付
     */

    public static String doPostRedirect(String url, Map<String, String> map){
        String result = null;

        HttpPost httpPost = new HttpPost(url);
        //拼接参数
        List<NameValuePair> list = new ArrayList<NameValuePair>();
        Iterator<Map.Entry<String, String>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> elem = (Map.Entry<String, String>) iterator.next();
            list.add(new BasicNameValuePair(elem.getKey(),elem.getValue() == null ? "" : elem.getValue().toString()));
        }

        try {

            if (list.size() > 0) {
                httpPost.setEntity(new UrlEncodedFormEntity(list, Charset.forName("utf-8")));
            }



            HttpClientBuilder builder = HttpClients.custom()
                    .disableAutomaticRetries() //关闭自动处理重定向
                    .setRedirectStrategy(new LaxRedirectStrategy());//利用LaxRedirectStrategy处理POST重定向问题
            CloseableHttpClient client = builder.build();

            CloseableHttpResponse response = client.execute(httpPost);

            try {
                if (response != null) {
                    HttpEntity resEntity = response.getEntity();
                    final StatusLine statusLine = response.getStatusLine();
                    if (statusLine.getStatusCode() >= org.apache.http.HttpStatus.SC_MULTIPLE_CHOICES||statusLine.getStatusCode()<org.apache.http.HttpStatus.SC_OK) {
                        //主动回收httpclient连接
                        EntityUtils.consume(resEntity);
                        throw new HttpResponseException(statusLine.getStatusCode(),  statusLine.getReasonPhrase());
                    }
                    if (resEntity != null) {
                        result = EntityUtils.toString(resEntity);
                    }
                }
            } finally {
                if(null!=response)  response.close();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }


//HtmlPage mainPage =handlerUtil.getEndHtml(channelWrapper.getAPI_CHANNEL_BANK_URL(),channelWrapper.getAPI_ORDER_ID(),payParam);
// final HtmlPage pakcagePage = (HtmlPage) mainPage.getFrameByName("mainwindow").getEnclosedPage();
    public static HtmlPage sendByHtmlUnit(String url) throws PayException {
        Page page = null;
        try (final WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
               webClient.getOptions().setJavaScriptEnabled(true); //启用JS解释器，默认为true
               webClient.getOptions().setUseInsecureSSL(true);//接受任何主机连接 无论是否有有效证书
               webClient.getOptions().setCssEnabled(false); //禁用css支持
               webClient.getOptions().setThrowExceptionOnScriptError(false); //js运行错误时，是否抛出异常
               webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
               webClient.getOptions().setTimeout(10000); //设置连接超时时间 ，这里是10S。如果为0，则无限期等待
               //webClient.waitForBackgroundJavaScript(10 * 1000);
               webClient.getCookieManager().setCookiesEnabled(true);
               webClient.setAjaxController(new NicelyResynchronizingAjaxController());// 设置Ajax异步
               page= webClient.getPage(url);
               if(page instanceof  HtmlPage){
                   return (HtmlPage)page;
               }else{
                   WebResponse response = page.getWebResponse();
                   String json = response.getContentAsString();
                   throw  new PayException(json);
                  // if(response.getContentType().equals("application/json")) {
                  //    Map<String, String> map = new Gson().fromJson(json, new TypeToken<Map<String, String>>() {}.getType());
                  // }
               }
        }catch (Exception e){
                throw new PayException("访问解析第三方页面错误：  "+e.getMessage());
        }
            // webClient.closeAllWindows();
    }


    public static HtmlPage getHtml(WebClient client, String html, String url, WebWindow window) {
        try {
            URL u = new URL(StringUtils.isBlank(url) ? "http://www.baidu.com" : url);
            StringWebResponse response = new StringWebResponse(html, u);
            HtmlPage page = HTMLParser.parseHtml(response, window != null ? window : client.getCurrentWindow());
            return page;
        } catch (Exception e) {
            throw new RuntimeException("字符串转换为htmlPage出错", e);
        }
    }
    public static HtmlPage getHtml(String html) {
        return getHtml(new WebClient(), html, null,null);
    }


  //get  http://www.baidu.com/?a=b&b=中文GBK
    public static String receiveBySend(String urlStr, String encoding) throws Exception {
        InputStream is = null;
        BufferedReader br = null;
        InputStreamReader isr = null;
        HttpURLConnection conn = null;
        try {
            StringBuffer sb = new StringBuffer();
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            is = conn.getInputStream();
            isr = new InputStreamReader(is, encoding);
            br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            String xml = sb.toString();
            return xml;
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                br.close();
                isr.close();
                is.close();
                conn.disconnect();
            } catch (Exception e) {
            }
        }
    }

}