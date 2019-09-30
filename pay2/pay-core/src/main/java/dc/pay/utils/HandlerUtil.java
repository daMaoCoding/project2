package dc.pay.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.ChannelWrapper;
import dc.pay.base.processor.PayException;
import dc.pay.business.RequestPayResult;
import dc.pay.business.ResponseDaifuResult;
import dc.pay.business.ResponsePayResult;
import dc.pay.config.ChannelParamConfig;
import dc.pay.config.PayProps;
import dc.pay.config.RunTimeInfo;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.entity.ReqDaifuInfo;
import dc.pay.entity.ReqPayInfo;
import dc.pay.entity.daifu.ReqDaiFuList;
import dc.pay.entity.pay.ReqPayInfoList;
import dc.pay.entity.pay.ReqPayList;
import dc.pay.entity.po.AutoQueryDaifu;
import dc.pay.service.daifu.ReqDaiFuListService;
import dc.pay.service.pay.ReqPayListService;
import dc.pay.utils.excel.channelConfig.ExcelChannel;
import dc.pay.utils.excel.channelConfig.ExcelHelper;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.*;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ************************
 * 支付请求/响应工具类
 * @author tony 3556239829
 */

@Component
public class HandlerUtil implements ApplicationContextAware {
    private static final Logger log =  LoggerFactory.getLogger(HandlerUtil.class);
    private static ApplicationContext applicationContext = null;
    private static PayProps payProps;
    private static final char HEX_DIGITS[] = { '0', '1', '2', '3', '4', '5', '6', '7','8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    public static final String METHOD = "method";
    public static final String ACTION = "action";
    public static final String TMPORDERKEY = "TMPORDER:";
    public static final String REQPAYGETQRHTMLPATH = "/reqPay/getreqthml/";
    public static final String HttpSchema = "http://";
    public static final String PortSpl = ":";
    public static final String DEFAULT_API_Client_IP = "123.123.123.123";
    public static final String OK = "OK";
    public static final String wzfh = "未知分行";
    public static final String wzzh = "未知支行";

    //国内ip
    public static Set<String> igonCliIP = new HashSet<String>(){{
        add("211.21.55.11");
        add("110.54.194.144");
        add("112.199.32.122");
        add("123.123.123.123");
    }};

    private static final  Random random = new Random();
    private static  String DB_INTERFACE_GET_REQPAYINFO_URL;           //查询请求支付URL
    private static  String DB_INTERFACE_GET_REQDAIFUINFO_URL;         //查询请求代付URL
    private static  String DB_INTERFACE_SEND_PAYRESULT_URL;           //支付通知结果URL
    private static  String DB_INTERFACE_SEND_DAIFURESULT_URL;         //代付通知结果URL
    private static  String DB_INTERFACE_GET_REQPAYINFO_HEADER;        //查询请求支付头
    private static  String DB_INTERFACE_GET_REQDAIFUINFO_HEADER;      //查询请求代付头
    private static  String REQ_PAY_PROXYSERV;                      //获得代理服务器配置
    public static final  String hostName = RunTimeInfo.getLocalHostName();

//  private static final  String DB_INTERFACE_GET_REQPAYINFO_URL = PayProps.getReqPayInfoUrl();     //查询请求支付URL
//  private static final  String DB_INTERFACE_SEND_PAYRESULT_URL = PayProps.getSendPayResultUrl();  //通知结果URL
//  private static final  String DB_INTERFACE_GET_REQPAYINFO_HEADER=PayProps.getReqPayInfoHeader(); //查询请求支付头

    public static final String YES = "是";
    private static  final List<ExcelChannel> channelConfParam = ExcelHelper.getChannelConfParamAll();
    private static  final Map<String,String>  channelCoConfigParamAll =  ExcelHelper.getAllPayCo();
    private static  final Map<String,String>  channelAndCoCname =  ExcelHelper.getAllPayChannelAndCoCname();
    private static  final Map<String,String>  channelAndCoId =  ExcelHelper.getAllPayChannelAndCoId();
    private static  final SimpleClientHttpRequestFactory httpClientFactory = new SimpleClientHttpRequestFactory();
    public static final  int  nThreads = 100;   //线程
    public static final  int  MAX_QUEUQ_SIZE = 500; //队列
    public static final int   RESP_DB_TIMES = 8;  //最多通知数据库次数

    @Autowired
    @Qualifier("polledRestTemplate")
    RestTemplate polledRestTemplate;

    @Autowired
    @LoadBalanced
    @Qualifier("loadBalancedRestTemplate")
    RestTemplate loadBalancedRestTemplate;

    @Autowired
    RedisTemplate redisTemplate;
    private HashOperations<String, String, Object> hashOps;

    @PostConstruct
    void postConstructInit(){
        hashOps = redisTemplate.opsForHash();
    }

    @Autowired
    private LoadBalancerClient loadBalancerClient;
    @Autowired
    private ChannelParamConfig channelParamConfig;
    @Autowired
    private ReqPayListService reqPayListService;
    @Autowired
    private ReqDaiFuListService reqDaiFuListService;
    @Autowired
    RunTimeInfo runTimeInfo;




    //获取不规范命名的Eureka实例
    private URI getEurekaInstanceURI(String url) throws PayException {
        String instanceName = getEurekaInstance(url);
        URI uri = null;
        try {
            uri= loadBalancerClient.choose(instanceName).getUri();
             return uri;
        } catch (Exception e) {
            log.error("Eureka服务器无该实例名：{} ,完整配置：",url);
        }
        return null;
    }


    /**
     * 初始化
     */
    private static void init(){
        DB_INTERFACE_GET_REQPAYINFO_URL    = payProps.getReqPayInfoUrl();
        DB_INTERFACE_GET_REQDAIFUINFO_URL = payProps.getReqDaifuInfoUrl();
        DB_INTERFACE_SEND_PAYRESULT_URL    = payProps.getSendPayResultUrl();
        DB_INTERFACE_SEND_DAIFURESULT_URL = payProps.getSendDaifuResultUrl();
        DB_INTERFACE_GET_REQPAYINFO_HEADER = payProps.getReqPayInfoHeader();
        DB_INTERFACE_GET_REQDAIFUINFO_HEADER = payProps.getReqDaifuInfoHeader();
        REQ_PAY_PROXYSERV = payProps.getReqPayProxyServ();
       // httpClientFactory.setConnectTimeout(3000);
       // httpClientFactory.setReadTimeout(3000);

    }

    private  HandlerUtil(PayProps payProps) {
        this.payProps = payProps;
        init();
    }


    /**
     * 通过订单号获得请求支付信息
     * @param orderId 业务系统统一定义的订单号
     * @return
     */
    public  ReqPayInfo getReqPayinfoByOrderIdNoCache(String orderId,Map<String,String> resParams) throws PayException {
        if(StringUtils.isBlank(orderId)){
            log.error("通过订单号获得请求支付信息失败，订单号，或参数为空，,当前时间{}，订单号{}，参数：{},当前时间{}：",System.currentTimeMillis(),orderId,JSON.toJSONString(resParams));
            throw  new PayException("通过订单号获得请求支付信息失败，订单号，或参数为空"+System.currentTimeMillis());
        }
        ReqPayInfo reqPayInfo = null;
        long start = getNowMilliSecond();
        String bankChannel= null;
        String result="";
        try {
            HttpHeaders headers = new HttpHeaders();
            String mediaType = "application/json; charset=UTF-8";
            MediaType type = MediaType.parseMediaType(mediaType);
            // headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
            headers.setContentType(type);
            headers.add("Accept", MediaType.APPLICATION_JSON.toString());
            headers.add("Connection","close");
            headers.add(DB_INTERFACE_GET_REQPAYINFO_HEADER.split("=")[0],DB_INTERFACE_GET_REQPAYINFO_HEADER.split("=")[1]);//增加特定header
            HttpEntity<Map> formEntity=new HttpEntity<Map>(resParams,headers);
             RestTemplate restTemplate = new RestTemplate(httpClientFactory);
            // RestTemplate restTemplate = RestTemplateUtil.getRestTemplate();
            //String result =  polledRestTemplate.exchange(DB_INTERFACE_GET_REQPAYINFO_URL, HttpMethod.POST,formEntity,String.class,orderId).getBody();
              if(ValidateUtil.isIpHost(DB_INTERFACE_GET_REQPAYINFO_URL)||DB_INTERFACE_GET_REQPAYINFO_URL.contains("localhost")){//常规.ip,端口，localhost
                 // result = polledRestTemplate.postForObject(DB_INTERFACE_GET_REQPAYINFO_URL, formEntity, String.class,orderId);
                  result = RestTemplateUtil.getRestTemplate().postForObject(DB_INTERFACE_GET_REQPAYINFO_URL, formEntity, String.class,orderId);
              }else{//命名不规范的Eureka实例名
                  if(DB_INTERFACE_GET_REQPAYINFO_URL.contains("_")){
                      URI    eurekaInstanceURI = getEurekaInstanceURI(DB_INTERFACE_GET_REQPAYINFO_URL);
                      String eurekaInstanceUrlSuffix = subString(DB_INTERFACE_GET_REQPAYINFO_URL,getEurekaInstance(DB_INTERFACE_GET_REQPAYINFO_URL));
                      //result =  polledRestTemplate.exchange(eurekaInstanceURI+eurekaInstanceUrlSuffix, HttpMethod.POST,formEntity,String.class,orderId).getBody();
                      result =  restTemplate.exchange(eurekaInstanceURI+eurekaInstanceUrlSuffix, HttpMethod.POST,formEntity,String.class,orderId).getBody();
                  }else{//正常的Eureka实例
                      result =  RestTemplateUtil.getRestTemplate().postForObject(DB_INTERFACE_GET_REQPAYINFO_URL, formEntity, String.class,orderId);
                  }
              }
             reqPayInfo = JSON.parseObject(result, ReqPayInfo.class);
            //reqPayInfo.setAPI_KEY(RsaUtil.decryptAndCache(reqPayInfo.getAPI_KEY())); //解密并缓存KEY,统一使用老板自定义秘钥jar
            if( null!=reqPayInfo){bankChannel = reqPayInfo.getAPI_CHANNEL_BANK_NAME();}else{throw new PayException("通过订单号获得请求支付信息-失败返回空，订单号"+orderId);};
            return reqPayInfo;
        } catch (Exception ex) {
            log.error("通过订单号获得请求支付信息失败，数据库返回：{}，订单号：{},提交地址：{},错误消息：{}",JSON.toJSONString(result),orderId,DB_INTERFACE_GET_REQPAYINFO_URL,ex.getMessage());
            try{
                ReqPayList reqPayList = reqPayListService.getByOrderId(orderId);
                if(null!=reqPayList && reqPayList.getReqPayInfo()!=null){
                    log.error("通过订单号获得DB请求支付信息失败，使用本地流水表获取订单支付信息：成功：订单号：{},订单信息：{}",orderId,JSON.toJSONString(reqPayList));
                    bankChannel =  reqPayList.getReqPayInfo().getAPI_CHANNEL_BANK_NAME();
                    return reqPayList.getReqPayInfo();
                }else{
                    log.error("通过订单号获得本地流水表请求支付信息失败，本地流水表返回,本地返回：{},订单号：{},消息：{}",JSON.toJSONString(reqPayList),orderId,ex.getMessage());
                    throw  new PayException(SERVER_MSG.REQUEST_PAY_GETDB_REQPAYINFO_ERROR,ex);
                }
            }catch (Exception e){
                throw  new PayException(SERVER_MSG.REQUEST_PAY_GETDB_REQPAYINFO_ERROR,e);
            }
        }finally {
            log.debug("[HandlerUtil],获取订单支付信息完毕耗时：{}毫秒，订单号：{},支付通道：{},提交地址：{},订单信息：{}",String.valueOf((getNowMilliSecond())-start),orderId,bankChannel,DB_INTERFACE_GET_REQPAYINFO_URL,JSON.toJSONString(reqPayInfo));
        }
    }




    /**
     * 通过订单号获得请求代付信息,转发rest请求的map数据给dbi
     */
    public ReqDaifuInfo getReqDaifuInfoByOrderIdNoCache(String orderId, Map<String,String> resParams) throws PayException {
        if(MapUtils.isEmpty(resParams)){resParams=Maps.newHashMap();}
        if(StringUtils.isBlank(orderId)){
            log.error("通过订单号获得请求[代付]信息失败，订单号，或参数为空，,当前时间{}，订单号{}，参数：{},当前时间{}：",System.currentTimeMillis(),orderId,JSON.toJSONString(resParams));
            throw  new PayException("通过订单号获得请求[代付]信息失败，订单号，或参数为空"+System.currentTimeMillis());
        }
        ReqDaifuInfo reqDaifuInfo = null;
        long start = getNowMilliSecond();
        String bankChannel= null;
        String result="";
        try {
            HttpHeaders headers = new HttpHeaders();
            String mediaType = "application/json; charset=UTF-8";
            MediaType type = MediaType.parseMediaType(mediaType);
            // headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
            headers.setContentType(type);
            headers.add("Accept", MediaType.APPLICATION_JSON.toString());
            headers.add("Connection","close");
            headers.add(DB_INTERFACE_GET_REQDAIFUINFO_HEADER.split("=")[0],DB_INTERFACE_GET_REQDAIFUINFO_HEADER.split("=")[1]);//增加特定header
            HttpEntity<Map> formEntity=new HttpEntity<Map>(resParams,headers);
            RestTemplate restTemplate = new RestTemplate(httpClientFactory);
            if(ValidateUtil.isIpHost(DB_INTERFACE_GET_REQDAIFUINFO_URL)||DB_INTERFACE_GET_REQDAIFUINFO_URL.contains("localhost")){//常规.ip,端口，localhost
                // result = polledRestTemplate.postForObject(DB_INTERFACE_GET_REQPAYINFO_URL, formEntity, String.class,orderId);
                result = RestTemplateUtil.getRestTemplate().postForObject(DB_INTERFACE_GET_REQDAIFUINFO_URL, formEntity, String.class,orderId);
            }else{//命名不规范的Eureka实例名
                if(DB_INTERFACE_GET_REQDAIFUINFO_URL.contains("_")){
                    URI    eurekaInstanceURI = getEurekaInstanceURI(DB_INTERFACE_GET_REQDAIFUINFO_URL);
                    String eurekaInstanceUrlSuffix = subString(DB_INTERFACE_GET_REQDAIFUINFO_URL,getEurekaInstance(DB_INTERFACE_GET_REQDAIFUINFO_URL));
                    //result =  polledRestTemplate.exchange(eurekaInstanceURI+eurekaInstanceUrlSuffix, HttpMethod.POST,formEntity,String.class,orderId).getBody();
                    result =  restTemplate.exchange(eurekaInstanceURI+eurekaInstanceUrlSuffix, HttpMethod.POST,formEntity,String.class,orderId).getBody();
                }else{//正常的Eureka实例
                    result =  RestTemplateUtil.getRestTemplate().postForObject(DB_INTERFACE_GET_REQDAIFUINFO_URL, formEntity, String.class,orderId);
                }
            }
            reqDaifuInfo = JSON.parseObject(result, ReqDaifuInfo.class);
            log.debug("通过订单号请求[DB]获得[代付]信息，订单号：{}，结果：{}",orderId,result);
            //reqPayInfo.setAPI_KEY(RsaUtil.decryptAndCache(reqPayInfo.getAPI_KEY())); //解密并缓存KEY,统一使用老板自定义秘钥jar
            if( null!=reqDaifuInfo){bankChannel = reqDaifuInfo.getAPI_CHANNEL_BANK_NAME();}else{throw new PayException("通过订单号获得请求[代付]信息-失败返回空，订单号"+orderId);};
            ValidateUtil.valdataReqDaifuInfo(reqDaifuInfo);
            return  reqDaifuInfo;
        } catch (Exception ex) {
            log.error("通过订单号获得请求[代付]信息失败，数据库返回：{}，订单号：{},提交地址：{},错误消息：{}",JSON.toJSONString(result),orderId,DB_INTERFACE_GET_REQDAIFUINFO_URL,ex.getMessage());
            ReqDaiFuList reqDaiFuList = reqDaiFuListService.getByOrderId(orderId);
            if(null!=reqDaiFuList && reqDaiFuList.getReqDaifuInfo()!=null){
                log.error("通过订单号获得DB请求[代付]信息失败，使用本地流水表获取订[代付]付信息：成功：订单号：{},订单信息：{}",orderId,JSON.toJSONString(reqDaiFuList));
                return reqDaiFuList.getReqDaifuInfo();
            }else{
                log.error("通过订单号获得本地流水表请求[代付]信息失败，本地流水表返回,本地返回：{},订单号：{},消息：{}",JSON.toJSONString(reqDaiFuList),orderId,ex.getMessage());
                throw  new PayException(SERVER_MSG.REQUEST_PAY_GETDB_REQDAIFUINFO_ERROR,ex);
            }
        }finally {
            log.debug("[HandlerUtil],获取订单代付信息完毕耗时：{}毫秒，订单号：{},支付通道：{},提交地址：{},订单信息：{}",String.valueOf((getNowMilliSecond())-start),orderId,bankChannel,DB_INTERFACE_GET_REQDAIFUINFO_URL,JSON.toJSONString(reqDaifuInfo));
        }
    }















    /**
     * 通过订单号获得请求支付信息
     * @param orderId 业务系统统一定义的订单号
     * @return
     */
    public ReqPayInfoList getReqPayinfoByOrderIdBatchNoCache(String orderId) throws PayException {
        long start = getNowMilliSecond();
        String result="";
        ReqPayInfoList reqPayInfoList=null;
        try {
            HttpHeaders headers = new HttpHeaders();
            String mediaType = "application/json; charset=UTF-8";
            MediaType type = MediaType.parseMediaType(mediaType);
            // headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
            headers.setContentType(type);
            headers.add("Accept", MediaType.APPLICATION_JSON.toString());
            headers.add("Connection","close");
            headers.add(DB_INTERFACE_GET_REQPAYINFO_HEADER.split("=")[0],DB_INTERFACE_GET_REQPAYINFO_HEADER.split("=")[1]);//增加特定header
            HttpEntity<String> formEntity = new HttpEntity<>(headers);
            //RestTemplate restTemplate = new RestTemplate(httpClientFactory);
            // RestTemplate restTemplate = RestTemplateUtil.getRestTemplate();
            //String result =  polledRestTemplate.exchange(DB_INTERFACE_GET_REQPAYINFO_URL, HttpMethod.POST,formEntity,String.class,orderId).getBody();
              if(ValidateUtil.isIpHost(DB_INTERFACE_GET_REQPAYINFO_URL)||DB_INTERFACE_GET_REQPAYINFO_URL.contains("localhost")){//常规.ip,端口，localhost
                  result = polledRestTemplate.postForObject(DB_INTERFACE_GET_REQPAYINFO_URL, formEntity, String.class,orderId);
              }else{//命名不规范的Eureka实例名
                  if(DB_INTERFACE_GET_REQPAYINFO_URL.contains("_")){
                      URI    eurekaInstanceURI = getEurekaInstanceURI(DB_INTERFACE_GET_REQPAYINFO_URL);
                      String eurekaInstanceUrlSuffix = subString(DB_INTERFACE_GET_REQPAYINFO_URL,getEurekaInstance(DB_INTERFACE_GET_REQPAYINFO_URL));
                      result =  polledRestTemplate.exchange(eurekaInstanceURI+eurekaInstanceUrlSuffix, HttpMethod.POST,formEntity,String.class,orderId).getBody();
                  }else{//正常的Eureka实例
                      result = loadBalancedRestTemplate.postForObject(DB_INTERFACE_GET_REQPAYINFO_URL, formEntity, String.class,orderId);
                  }
              }
            reqPayInfoList = JSON.parseObject(result, ReqPayInfoList.class);
            //reqPayInfo.setAPI_KEY(RsaUtil.decryptAndCache(reqPayInfo.getAPI_KEY())); //解密并缓存KEY,统一使用老板自定义秘钥jar
            //bankChannel = reqPayInfo.getAPI_CHANNEL_BANK_NAME();
            return reqPayInfoList;
        } catch (Exception ex) {
            log.error("数据库返回：{}",result);
            log.error("通过订单号获得请求支付信息失败，订单号："+orderId+",提交地址："+DB_INTERFACE_GET_REQPAYINFO_URL+",消息："+ ex.getMessage(),ex);
            throw  new PayException(SERVER_MSG.REQUEST_PAY_GETDB_REQPAYINFO_ERROR,ex);
        }finally {
            log.debug("[HandlerUtil],获取订单支付信息耗时："+String.valueOf((getNowMilliSecond())-start)+"毫秒，订单号："+orderId+",返回批量请求支付信息："+JSON.toJSONString(reqPayInfoList)+",提交地址："+DB_INTERFACE_GET_REQPAYINFO_URL);
        }
    }


    public static long getNowMilliSecond() {
        return System.nanoTime()/1000000L;
    }

    /**
     * 响应数据库接口通知[支付/代付]状态
     * tyep:1,支付，2，代付
     */
    public  String responseForDbInterface(final Serializable obj,int type){
        if(type!=1 && type!=2){
            log.error("响应数据库接口通知状态失败-类型错误，结果：{},类型:{}", JSON.toJSONString(obj),type);
            return "PAY_RES_DB_ERROR";
        }
        long dbTimeOut =PayEumeration.DEFAULT_TIME_OUT_RESPAYDB ;
        final String actionName = type==1?"支付":"代付";
        final String  headerName=type==1?DB_INTERFACE_GET_REQPAYINFO_HEADER.split("=")[0]:DB_INTERFACE_GET_REQDAIFUINFO_HEADER.split("=")[0];
        final String headerValue=type==1?DB_INTERFACE_GET_REQPAYINFO_HEADER.split("=")[1]:DB_INTERFACE_GET_REQDAIFUINFO_HEADER.split("=")[1];
       final String url=type==1?DB_INTERFACE_SEND_PAYRESULT_URL:DB_INTERFACE_SEND_DAIFURESULT_URL;
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        FutureTask<String> future = null;
        future = new FutureTask<String>(
                new Callable<String>() {
                    public String call() throws Exception {
                        String dbResult = null;
                        if(null!=obj){
                            try {
                                HttpHeaders headers = new HttpHeaders();
                                MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
                                headers.setContentType(type);
                                headers.add("Accept", MediaType.APPLICATION_JSON.toString());
                                headers.add("Connection","close");
                                headers.add(headerName,headerValue);//增加特定header
                                List<Charset> acceptCharset = Collections.singletonList(StandardCharsets.UTF_8);
                                headers.setAcceptCharset(acceptCharset);
                                HttpEntity<String> formEntity = new HttpEntity<String>(JSON.toJSONString(obj), headers);

                                if(ValidateUtil.isIpHost(url)||url.contains("localhost")){//常规.ip,端口，localhost
                                    dbResult = polledRestTemplate.postForObject(url, formEntity, String.class);
                                    log.info("通知[{}]成功，[常规，ip,端口，localhost通知]：通知地址：{},通知内容：{},通知结果：{}",actionName,url,JSON.toJSONString(formEntity),dbResult);
                                }else{//命名不规范的Eureka实例名
                                    if(url.contains("_")){
                                        URI    eurekaInstanceURI = getEurekaInstanceURI(url);
                                        String eurekaInstanceUrlSuffix = subString(url,getEurekaInstance(url));
                                        dbResult =  polledRestTemplate.exchange(eurekaInstanceURI+eurekaInstanceUrlSuffix, HttpMethod.POST,formEntity,String.class).getBody();
                                        log.info("通知DB[{}]成功，[特殊Eureka实例名]：通知地址：{},通知内容：{},通知结果：{}",actionName,url,JSON.toJSONString(formEntity),dbResult);
                                    }else{//正常的Eureka实例
                                        dbResult = loadBalancedRestTemplate.postForObject(url, formEntity, String.class);
                                        log.info("通知DB[{}]成功，[正常Eureka实例名]：通知地址：{},通知内容：{},通知结果：{}",actionName,url,JSON.toJSONString(formEntity),dbResult);
                                    }
                                }
                                return dbResult;
                            } catch (Exception ex) {
                                log.error("响应数据库接口通知DB[{}]状态失败-网络问题，结果：{},数据库返回信息:{}",actionName, JSON.toJSONString(obj),dbResult,ex);
                                return dbResult;
                            }
                        }
                        return dbResult;
                    }
                });
        executor.execute(future);
        try {
            return future.get(dbTimeOut, TimeUnit.MILLISECONDS);
        }catch (Exception e) {
            log.error("[响应数据库接口通知[{}]状态失败]：{}",actionName,e.getMessage(),e);
        }finally {
            executor.shutdownNow();
        }
        return null;
    }





    public  String sendToMS(String urlOrInstances,Map<String,String> spHeader,String postJsondata,HttpMethod method){
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        FutureTask<String> future = null;
        long dbTimeOut =PayEumeration.DEFAULT_TIME_OUT_RESPAYDB ;
        future = new FutureTask<String>(new Callable<String>() {
                    public String call() throws Exception {
                        String dbResult = null;
                        if(StringUtils.isNotBlank(postJsondata)){
                            try {
                                HttpHeaders headers = new HttpHeaders();
                                MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
                                headers.setContentType(type);
                                headers.add("Accept", MediaType.APPLICATION_JSON.toString());
                                headers.add("Connection","close");
                                if(MapUtils.isNotEmpty(spHeader))  spHeader.forEach((k,v)-> headers.add(k,v)); //增加特定header
                                HttpEntity<String> formEntity = new HttpEntity<String>(postJsondata, headers);
                                if(ValidateUtil.isIpHost(urlOrInstances)||urlOrInstances.contains("localhost")){//常规.ip,端口，localhost
                                    if(method!=null && method==HttpMethod.GET){
                                        dbResult = polledRestTemplate.getForObject(urlOrInstances,String.class, formEntity);
                                    }else{
                                        dbResult = polledRestTemplate.postForObject(urlOrInstances, formEntity, String.class);
                                    }
                                }else{//命名不规范的Eureka实例名
                                    if(urlOrInstances.contains("_")){
                                        URI    eurekaInstanceURI = getEurekaInstanceURI(urlOrInstances);
                                        String eurekaInstanceUrlSuffix = subString(urlOrInstances,getEurekaInstance(urlOrInstances));
                                        dbResult =  polledRestTemplate.exchange(eurekaInstanceURI+eurekaInstanceUrlSuffix, method,formEntity,String.class).getBody();
                                    }else{//正常的Eureka实例
                                        if(method!=null && method==HttpMethod.GET){
                                            dbResult = loadBalancedRestTemplate.getForObject(urlOrInstances,String.class, formEntity);
                                        }else{
                                            dbResult = loadBalancedRestTemplate.postForObject(urlOrInstances, formEntity, String.class);
                                        }
                                    }
                                }
                                return dbResult;
                            } catch (Exception ex) {
                                log.error("SendToSM失败-网络问题，urlOrInstances：{},header:{},data:{},数据库返回信息:{}", urlOrInstances,JSON.toJSONString(spHeader),postJsondata,dbResult,ex);
                                return dbResult;
                            }
                        }
                        return dbResult;
                    }
                });
        executor.execute(future);
        try {
            return future.get(dbTimeOut, TimeUnit.MILLISECONDS);
        }catch (Exception e) {}finally {executor.shutdownNow();}
        return null;
    }


    /**
     * 生成随机字符
     */
    public static String randomStr(int num) {
        char[] randomMetaData = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
                'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
                'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4',
                '5', '6', '7', '8', '9' };
        Random random = new Random();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < num; i++) {
            buf.append(randomMetaData[random.nextInt(randomMetaData.length - 1)]);
        }
        return buf.toString();
    }





    /**
     * [公用]-对字符串md5加密,并转成大写
     * DigestUtils.md5Hex(pendingToSign.toString());
     * @param str
     * @return
     */
    public static String getMD5UpperCase(String str) throws PayException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");// 拿到一个MD5转换器（如果想要SHA1参数换成”SHA1”）
            messageDigest.update(str.getBytes());

            byte[] resultByteArray = messageDigest.digest();
            return byteArrayToHex(resultByteArray); // 字符数组转换成字符串返回

           // return new BigInteger(1, messageDigest.digest()).toString(16).toUpperCase();;
        } catch (Exception e) {
            log.error("[公用]-对字符串md5加密,并转成大写出错{}", e.getMessage());
            throw new PayException(SERVER_MSG.REQUEST_PAY_BUILDSIGN_ERROR,e);
        }
    }

    public static String getMD5UpperCase( byte[] strByte) throws PayException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");// 拿到一个MD5转换器（如果想要SHA1参数换成”SHA1”）
            messageDigest.update(strByte);
            byte[] resultByteArray = messageDigest.digest();
            return byteArrayToHex(resultByteArray); // 字符数组转换成字符串返回
            // return new BigInteger(1, messageDigest.digest()).toString(16).toUpperCase();;
        } catch (Exception e) {
            log.error("[公用]-对字符串md5加密,并转成大写出错{}", e.getMessage());
            throw new PayException(SERVER_MSG.REQUEST_PAY_BUILDSIGN_ERROR,e);
        }
    }



    /**
     *  java php md5
     */
    public static String getPhpMD5(String str) throws PayException {
        try{
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(str.getBytes());
            byte[] md5Bytes = md5.digest();
            String res = "";
            for (int i = 0; i < md5Bytes.length; i++) {
                int temp = md5Bytes[i] & 0xFF;
                if (temp <= 0XF) { // 转化成十六进制不够两位，前面加零
                    res += "0";
                }
                res += Integer.toHexString(temp);
            }
            return res;
        }catch (Exception e){
            throw new PayException(SERVER_MSG.REQUEST_PAY_BUILDSIGN_ERROR,e);
        }
    }



    //优付支付
    public static String md5(String str)   {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte[] byteDigest = md.digest();
            int i;

            //字符数组转换成字符串
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < byteDigest.length; offset++) {
                i = byteDigest[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            // 32位加密
            return buf.toString().toUpperCase();
            // 16位的加密
            //return buf.toString().substring(8, 24).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }



    /**
     * 将字节数组换成成16进制的字符串
     * @param byteArray
     * @return
     */
    public static String byteArrayToHex(byte[] byteArray) {
        // 初始化一个字符数组，用来存放每个16进制字符
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        // new一个字符数组，这个就是用来组成结果字符串的（解释一下：一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方））
        char[] resultCharArray = new char[byteArray.length * 2];
        // 遍历字节数组，通过位运算（位运算效率高），转换成字符放到字符数组中去
        int index = 0;
        for (byte b : byteArray) {
            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
            resultCharArray[index++] = hexDigits[b & 0xf];
        }
        // 字符数组组合成字符串返回
        return new String(resultCharArray);
    }




    /**
     * MD5字符串加密
     * @param s
     * @param encoding
     * @return
     */
    public final static String getMD5UpperCase(String s, String encoding) throws PayException {
        try {
            byte[] btInput = s.getBytes(encoding);
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = HEX_DIGITS[byte0 >>> 4 & 0xf];
                str[k++] = HEX_DIGITS[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            throw new PayException(SERVER_MSG.REQUEST_PAY_BUILDSIGN_ERROR,e);
        }
    }



    /**
     * 整数(包括负数) - [a, b] - rand()%(b-a+1)+a
     */
    public static int getRandomIntBetweenAA(int min,int max) {
        if (min == max) {
            return min;
        }
        if(min < max){
            return   (int)(random.nextDouble()*(max-min+1))+min;  // min 到max 之间的整数
        }
        if(min > max){
            return (int)(random.nextDouble()*(min-max+1))+max;
        }
        return 0;
    }


    /**
     * [公用]通过毫秒数格式化日期字符串
     * @param milliseconds
     * @param dateTimeFormatter
     * @return
     */
    public static  String getDateTimeByMilliseconds(String  milliseconds,String dateTimeFormatter){
        Date date = new Date(Long.parseLong(milliseconds));
        SimpleDateFormat format = new SimpleDateFormat(dateTimeFormatter);
        return format.format(date);
    }


    /**
     * [公用]-通过日期时间字符串，返回毫秒数
     * @param dateTime
     * @param dateTimeFormatter
     * @return
     */
    public static String getMillisecondsByDateTime(String dateTime,String dateTimeFormatter){
        SimpleDateFormat sdf = new SimpleDateFormat(dateTimeFormatter);
        long millionSeconds = 0;// 毫秒
        try {
            millionSeconds = sdf.parse(dateTime).getTime();
        } catch (ParseException e) {
            log.error("[公用]-通过日期时间字符串，返回毫秒数出错{}", e.getMessage(),e);
            return null;
        }
         return String.valueOf(millionSeconds);
    }


    /**
     * 获取全局通道中-具体通道银行名称
     * @param channelName  业务系统统一定义的具体通道名称
     * @return 通道标志
     */
    public String getChannelBankName(String channelName){
        if(StringUtils.isNotBlank(channelName)){
            return  channelParamConfig.getChannelBankName(channelName.trim());
        }
        return null;
    }



    /**
     * 获取全局通道中-具体通道签名名称
     * @param channelName  业务系统统一定义的具体通道名称
     * @return 通道标志
     */
    public   String getChannelBankSignParamName(String channelName){
        if(StringUtils.isNotBlank(channelName)){
            return  channelParamConfig.getChannelBankSignParamName(channelName.trim());
        }
        return null;
    }



    /**
     * 获取全局通道中-具体通道提交请求URL
     * @param channelName  业务系统统一定义的具体通道名称
     * @return 通道标志
     */
    public  String getChannelBankRequestURL(String channelName){
        if(StringUtils.isNotBlank(channelName)){
            return  channelParamConfig.getChannelBankRequestURL(channelName.trim());
        }
        return null;
    }


    /**
     * url 转Map(新百付)
     * @param str
     * @return
     */
    public static Map<String,String> urlToMap(String str){
        String[] respMstList = str.trim().split("&");
        HashMap<String, String> maps = Maps.newHashMap();
        for (String s : respMstList) {
            String[] split = s.split("=",2);
             maps.put(split[0].trim(),split[1].trim());
        }
       return maps;
    }


    /**
     * 转换Long
     * @param longStr
     * @return
     */
    public static long parseLong(String longStr){
        long i =0L;
         try{
             i = Long.parseLong(longStr);
         }catch (Exception e){
             log.error("转换Long出错：使用默认超时时间 {}", e.getMessage(),e);
             return PayEumeration.DEFAULT_TIME_OUT_REQPAY;
         }
        return i;
    }



    /**
     * [公用]
     * 分转元，保留2位小数
     * @param fenStr 分-字符串
     * @return       元-字符串
     */
    public static String getYuan(String fenStr) throws PayException {
        try{
            double  yuanDouble = Long.parseLong(fenStr) / 100.00;
            String  yuanStr = new DecimalFormat("0.00").format(yuanDouble);
            return yuanStr;
        }catch (Exception ex){
            log.error("分转元出错 {}", ex.getMessage());
            throw new PayException(SERVER_MSG.REQUEST_PAY_AMOUNT__ERROR);
        }
    }

    /**
     * [公用]
     * 分转元，保留1位小数
     *
     * @param fenStr 分-字符串
     * @return 元-字符串
     */
    public static String getYuanWithoutOne(String fenStr) throws PayException {
        try {
            double yuanDouble = Long.parseLong(fenStr) / 100.00;
            String yuanStr    = new DecimalFormat("0.0").format(yuanDouble);
            return yuanStr;
        } catch (Exception ex) {
            log.error("分转元出错 {}", ex.getMessage());
            throw new PayException(SERVER_MSG.REQUEST_PAY_AMOUNT__ERROR);
        }
    }



    /**
     * [公用]
     * 分转元，不保留小数
     * @param fenStr 分-字符串
     * @return       元-字符串
     */
    public static String getYuanWithoutZero(String fenStr) throws PayException {
        try{
            double  yuanDouble = Long.parseLong(fenStr) / 100.00;
            String  yuanStr = new DecimalFormat("0.00").format(yuanDouble);
            return yuanStr.endsWith(".00")?yuanStr = yuanStr.replaceAll("\\.00",""):yuanStr.endsWith("0")?yuanStr=yuanStr.substring(0,yuanStr.length()-1):yuanStr;
        }catch (Exception ex){
            log.error("分转元出错 {}", ex.getMessage());
            throw new PayException(SERVER_MSG.REQUEST_PAY_AMOUNT__ERROR);
        }
    }


    /**
     * 元转分
     * @param yuanStr 元-字符串
     * @return        分-字符串
     * @throws PayException
     */
    public static  String getFen(String yuanStr) throws PayException {
        try{
            Double  fenDouble = Double.parseDouble(yuanStr) * 100.00;
            String  fenStr = new DecimalFormat("0").format(fenDouble.doubleValue());
            return fenStr;
        }catch (Exception ex){
            log.error("元转分出错: {}", ex.getMessage());
            throw new PayException(SERVER_MSG.REQUEST_PAY_AMOUNT__ERROR,ex);
        }
    }



    /**
     * map 转Json
     * @param map
     * @return
     */
    public static String mapToJson(Map<String, String> map) {
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        StringBuilder json = new StringBuilder();
        json.append("{");
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            String key = entry.getKey();
            String value = entry.getValue();
            json.append("\"").append(key).append("\"");
            json.append(":");
            json.append("\"").append(value).append("\"");
            if (it.hasNext()) {
                json.append(",");
            }
        }
        json.append("}");
        return json.toString();
    }




    /**
     * map 转Json
     * @param map
     * @return
     */
    public static String mapToJsonWithSpParam(Map<String, ?> map,Set<String> spParam) {
        Iterator<? extends Map.Entry<String, ?>> it = map.entrySet().iterator();
        StringBuilder json = new StringBuilder();
        json.append("{");
        while (it.hasNext()) {
            Map.Entry<String, ?> entry = it.next();
            String key = entry.getKey();
            String value = entry.getValue().toString();
            json.append("\"").append(key).append("\"");
            json.append(":");
            if(spParam!=null && spParam.contains(key)){
                json.append(value);
            }else if(entry.getValue() instanceof Map){
                json.append(mapToJsonWithSpParam((Map<String, Map<String,?>>)entry.getValue(),spParam));
            }else{
                json.append("\"").append(value).append("\"");
            }
            if (it.hasNext()) {
                json.append(",");
            }
        }
        json.append("}");
        return json.toString();
    }


    public static String simpleMapToJsonStr(Map map) {
        if (map == null || map.isEmpty()) {
            return "null";
        }
        String jsonStr = "{";
        Set keySet = map.keySet();
        for (Object key : keySet) {
            jsonStr += "\"" + key + "\":\"" + map.get(key) + "\",";
        }
        jsonStr = jsonStr.substring(0, jsonStr.length() - 1);
        jsonStr += "}";
        return jsonStr;
    }



    /**
     * JSON转Map
     * @param str  {"responsePayCode":"SUCCESS","responseOrderID":"456_1496025545611_1496108539616","responseOrderState":"SUCCESS","responsePayErrorMsg":null,"responsePayTotalTime":68,"responsePayMsg":"000000"}
     * @return
     */
    public static   Map<String,String>  jsonToMap(String str) {
        String sb = str.substring(2, str.length() - 2);
        String[] name = sb.split("\\\",\\\"");
        String[] nn = null;
        Map<String, String> map = new HashMap<>();
        for (String aName : name) {
            nn = aName.split("\\\":\\\"");
            map.put(nn[0], nn.length==2?nn[1]:"");
        }
        return map;
    }


    /**
     * 解析 htmlForm表单
     */
    public static Map<String,String> parseFormElement(Element formElement){
        Elements inputElems = null;
        if(null!=formElement){
            String method = formElement.attr(METHOD);  //提交方法
            String action = formElement.attr(ACTION); //提交地址
            inputElems = formElement.getElementsByTag("input");
              if(null==inputElems || inputElems.size()<=0)
                  inputElems = formElement.getElementsByTag("INPUT");
            if(null!=inputElems && inputElems.size()>0){
                HashMap<String, String> paramMap = Maps.newHashMap();
                paramMap.put(METHOD,method);
                paramMap.put(ACTION,action);
                for (Element input : inputElems) {
                    String name = input.attr("name"); //表单属性
                    String value = input.attr("value"); //表单值
                    paramMap.put(name, value);
                }
                return paramMap;
            }
        }
        return null;
    }

    /**
     * 自动提交表单
     */
    public static Result sendToThreadPayServ(String Referer,Map<String,String> payParam,String api_channel_bank_url) throws Exception {
        Map<String, String> headers = Maps.newHashMap();
        if(StringUtils.isNotBlank(Referer)){ //添加header标识
            headers.put("Referer",Referer);
            headers.put("Cache-Control","no-cache");
        }
        if(null!=payParam && !payParam.isEmpty()){
            String method = payParam.get(METHOD);
            String action = payParam.get(ACTION);
            if(StringUtils.isNotBlank(method) && StringUtils.isNotBlank(action) && StringUtils.isNotBlank(api_channel_bank_url)){
                action = getActionUrl(action,api_channel_bank_url); //表单提交地址
                payParam = cleanPayParam(payParam);
                if(method.equalsIgnoreCase(PayEumeration.HTTP_METHOD.GET.name()))
                   return  HttpUtil.get(action, headers, payParam);
                if(method.equalsIgnoreCase(PayEumeration.HTTP_METHOD.POST.name()))
                    return  HttpUtil.post(action, headers, payParam, "UTF-8");
            }
        }
        return null;
    }






    /**
     * 自动提交表单
     */
    public static Result sendToThreadPayServ(Map<String,String> payParam,String api_channel_bank_url)  {
        if(null!=payParam && !payParam.isEmpty()){
            String method = payParam.get(METHOD);
            String action = payParam.get(ACTION);
            if(StringUtils.isNotBlank(method) && StringUtils.isNotBlank(action) && StringUtils.isNotBlank(api_channel_bank_url)){
                action = getActionUrl(action,api_channel_bank_url); //表单提交地址
                payParam = cleanPayParam(payParam);
                try {
                    if(method.equalsIgnoreCase(PayEumeration.HTTP_METHOD.GET.name()))
                        return  HttpUtil.get(action, null, payParam);
                    if(method.equalsIgnoreCase(PayEumeration.HTTP_METHOD.POST.name()))
                        return  HttpUtil.post(action, null, payParam, "UTF-8");
                }catch (Exception e){
                    log.error("发送请求失败：参数:{},地址：{}",JSON.toJSONString(payParam),api_channel_bank_url,e);
                    return null;
                }
            }
        }
        return null;
    }



    /**
     * 表单提交地址
     */
    public static  String getActionUrl(String action,String api_channel_bank_url){
        if(StringUtils.isNotBlank(action)){
            if(action.startsWith("http")||action.startsWith("https")){
                return action;
            }else{
                if(action.startsWith("/")){
                    return api_channel_bank_url.substring(0,api_channel_bank_url.indexOf("/",9)).concat(action);
                }else{
                    return api_channel_bank_url.substring(0,api_channel_bank_url.lastIndexOf("/")+1).concat(action);
                }
            }
        }
        return null;
    }


    /**
     * 简单提交表单
     */
    public static String simplePostFormSecond(String url,String firstPayResultStr ) throws PayException {
        Map<String, String> secondPayParam = HandlerUtil.parseFormElement(Jsoup.parse(firstPayResultStr).getElementsByTag("form").first());
        String secondPayDomainUrl =  secondPayParam.get(HandlerUtil.ACTION).startsWith("http")||secondPayParam.get(HandlerUtil.ACTION).startsWith("https")?HandlerUtil.getDomain(secondPayParam.get(HandlerUtil.ACTION)):url;
        return HandlerUtil.sendToThreadPayServ(secondPayParam, secondPayDomainUrl).getBody();
    }



    /**
     * 清理程序辅助添加的参数
     */
    public static Map<String,String> cleanPayParam(Map<String,String>  payParam){
        payParam.remove(METHOD);
        payParam.remove(ACTION);
        return payParam;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if(this.applicationContext == null){
            this.applicationContext = applicationContext;
        }

    }

    private static  ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    //通过name获取 Bean.
    public static Object getBean(String name){
        return getApplicationContext().getBean(name);
    }

    //通过class获取Bean.
    public static <T> T getBean(Class<T> clazz){
        return getApplicationContext().getBean(clazz);
    }

    //通过name,以及Clazz返回指定的Bean
    public static <T> T getBean(String name,Class<T> clazz){
        return getApplicationContext().getBean(name, clazz);
    }



    /**
     * 生成随机数
     */
    public static  int getRandomNumber(int max,int min){
        if(max<min){
            int tmp = max;
                max = min;
                min = tmp;
        }
        Random random = new Random();
        int num = random.nextInt(max)%(max-min+1) + min;
        return num;
    }

    public static  String getRandomNumber(int length){
        if(length==0)return "";
        return new BigInteger(130, random).toString().substring(0,length);
    }


    public static String getRandomStrStartWithDate(int length){
        if(length==0)return "";
        return new Date().getTime() + randomString(7);
    }

    public static String randomString(int length) {
        if(length==0)return "";
        String str = new BigInteger(130, random).toString(32);
        return str.substring(0, length);
    }

    /**
     * 获取随机数
     * @param num 个数
     */
    public static String getRandomStr(int num) {
        if(num==0)return "";
        char[] randomMetaData = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g',
                'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
                't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E',
                'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q',
                'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2',
                '3', '4', '5', '6', '7', '8', '9' };
        Random random = new Random();
        String tNonceStr = "";
        for (int i = 0; i < num; i++) {
            tNonceStr += (randomMetaData[random
                    .nextInt(randomMetaData.length - 1)]);
        }
        return tNonceStr;
    }



    /*
    * 随机生成国内IP地址
    */
    public static String getRandomIp(final  ChannelWrapper channelWrapper){
        if(channelWrapper!=null && StringUtils.isNotBlank(channelWrapper.getAPI_Client_IP())){
            return channelWrapper.getAPI_Client_IP();
        }else{
            //ip范围
            int[][] range = {{607649792,  608174079  }, //36.56.0.0-36.63.255.255
                    {1038614528, 1039007743 }, //61.232.0.0-61.237.255.255
                    {1783627776, 1784676351 }, //106.80.0.0-106.95.255.255
                    {2035023872, 2035154943 }, //121.76.0.0-121.77.255.255
                    {2078801920, 2079064063 }, //123.232.0.0-123.235.255.255
                    {-1950089216,-1948778497}, //139.196.0.0-139.215.255.255
                    {-1425539072,-1425014785}, //171.8.0.0-171.15.255.255
                    {-1236271104,-1235419137}, //182.80.0.0-182.92.255.255
                    {-770113536, -768606209 }, //210.25.0.0-210.47.255.255
                    {-569376768, -564133889 }, //222.16.0.0-222.95.255.255
            };
            Random rdint = new Random();
            int index = rdint.nextInt(10);
            String ip = num2ip(range[index][0]+new Random().nextInt(range[index][1]-range[index][0]));
            return ip;
        }
    }


    //随机Ip
    public static String randomIp() {
        Random r = new Random();
        StringBuffer str = new StringBuffer();
        str.append(r.nextInt(1000000) % 255);
        str.append(".");
        str.append(r.nextInt(1000000) % 255);
        str.append(".");
        str.append(r.nextInt(1000000) % 255);
        str.append(".");
        str.append(0);
        return str.toString();
    }


    /*
    * 将十进制转换成ip地址
    */
    public static String num2ip(int ip) {
        int [] b=new int[4] ;
        String x = "";
        b[0] = (int)((ip >> 24) & 0xff);
        b[1] = (int)((ip >> 16) & 0xff);
        b[2] = (int)((ip >> 8) & 0xff);
        b[3] = (int)(ip & 0xff);
        x=Integer.toString(b[0])+"."+Integer.toString(b[1])+"."+Integer.toString(b[2])+"."+Integer.toString(b[3]);
        return x;
    }

    /**
     * 查找域名，兼容域名 & ip & ip:端口  等，要以http或者https开头
     */
    public static String getDomain(String str) throws PayException {
        if(StringUtils.isNotBlank(str)) str = str.trim();
        try{
            Pattern patternDomain = Pattern.compile("^((http://)|(https://))?([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}((:\\d*){0,1})(/{0,1})",Pattern.CASE_INSENSITIVE);
            Pattern patternIp = Pattern.compile("^((http://)|(https://))?((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)((:\\d*){0,1})(/{0,1})",Pattern.CASE_INSENSITIVE);
            Pattern eurekainstalce = Pattern.compile("^((http://)|(https://))?([a-zA-Z0-9_]([a-zA-Z0-9_\\-]{0,61}[a-zA-Z0-9_])?)((:\\d*){0,1})(/{0,1})",Pattern.CASE_INSENSITIVE);
            Matcher matcherDomain = patternDomain.matcher(str);
            Matcher matcherIp     = patternIp.matcher(str);
            Matcher matcherEurekainstalce     = eurekainstalce.matcher(str);
            if(matcherDomain.find()){
                return (matcherDomain.group());
            }
            if(matcherIp.find()){
                return (matcherIp.group());
            }
            if(matcherEurekainstalce.find()){
                return (matcherEurekainstalce.group());
            }
        }catch (Exception ex){
             log.error("[HandlerUtil查找域名]出错：未找到: "+str);
             throw new PayException(SERVER_MSG.REQUEST_PAY_VALIDATE_REQPAYINFO_DOMAIN_ERROR);
        }
        return null;
    }


    public static boolean isRigthDomain(String url){
        String regex = "^([hH][tT]{2}[pP]:/*|[hH][tT]{2}[pP][sS]:/*|[fF][tT][pP]:/*)?(([A-Za-z0-9-~]+).)+([A-Za-z0-9-~\\/])+(\\?{0,1}(([A-Za-z0-9-~]+\\={0,1})([A-Za-z0-9-~]*)\\&{0,1})*)$";
        Pattern pattern = Pattern.compile(regex);
        if (pattern.matcher(url).matches() && url.contains(".")) {
           return true;
        } else {
            return false;
        }
    }




    /**
     * 查找EurekaInstanceName,要以http或者https开头,不能带端口
     */
    public static String getEurekaInstance(String str){
        Pattern patternDomain = Pattern.compile("^((http://)|(https://))?([a-zA-Z0-9\\-\\_]([a-zA-Z0-9\\-\\_]{0,61}[a-zA-Z0-9])?)(/{0,1})",Pattern.CASE_INSENSITIVE);
        Matcher matcherDomain = patternDomain.matcher(str);
        if(matcherDomain.find()){
           return (matcherDomain.group().replaceAll("http://","").replaceAll("http://","").replaceAll("/",""));
        }
        log.error("[HandlerUtil查找EurekaInstanceName]出错：未找到, {}",str);
        return "";
    }


    /**
     * 截取指定字符串后的字符串
     */
    public static String subString(String sourceStr,String indexOfStr){
        return sourceStr.substring(sourceStr.indexOf(indexOfStr)+indexOfStr.length(),sourceStr.length());
    }


    /**
     * 解码URL
     */
    public static String  UrlDecode(String str) {
        //String str = "weixin%3a%2f%2fwxpay%2fbizpayurl%3fpr%3ddyMTcMV";
        try {
            return URLDecoder.decode(str.trim(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }


    /**
     * 编码URL
     */
    public static String  UrlEncode(String str)  {
        //String str = "weixin%3a%2f%2fwxpay%2fbizpayurl%3fpr%3ddyMTcMV";
        try {
            return URLEncoder.encode(str.trim(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }


    /**
     * 去掉url中的路径，留下请求参数部分
     */
    private static String truncateUrlPage(String strURL)
    {
        String strAllParam=null;
        String[] arrSplit=null;
        strURL=strURL.trim().toLowerCase();
        arrSplit=strURL.split("[?]");
        if(strURL.length()>1){
            if(arrSplit.length>1){
                if(arrSplit[1]!=null){
                    strAllParam=arrSplit[1];
                }
            }
        }
        return strAllParam;
    }



    /**
     * 解析出url参数中的键值对
     * 如 "index.jsp?Action=del&id=123"，解析出Action:del,id:123存入map中
     * @param URL  url地址
     * @return  url请求参数部分
     */
    public static Map<String, String> getUrlParams(String URL)
    {
        Map<String, String> mapRequest = new HashMap<String, String>();
        String[] arrSplit=null;
        String strUrlParam=truncateUrlPage(URL);
        if(strUrlParam==null) {
            return mapRequest;
        }
        //每个键值为一组 www.2cto.com
        arrSplit=strUrlParam.split("[&]");
        for(String strSplit:arrSplit) {
            String[] arrSplitEqual=null;
            arrSplitEqual= strSplit.split("[=]");
            //解析出键值
            if(arrSplitEqual.length>1){
                //正确解析
                mapRequest.put(arrSplitEqual[0], arrSplitEqual[1]);
            }
            else{
                if(arrSplitEqual[0]!=""){
                    //只有参数没有值，不加入
                    mapRequest.put(arrSplitEqual[0], "");
                }
            }
        }
        return mapRequest;
    }

    /**
     * 替换空白符号
     *  \n 回车(\u000a)
     *  \t 水平制表符(\u0009)
     *  \s 空格(\u0008)
     *  \r 换行(\u000d)
     * @param str
     * @return
     */
    public static String replaceBlank(String str) {
        String dest = "";
        if (str!=null) {
            Pattern p = Pattern.compile("\t|\r|\n");  //  "\\s*|\t|\r|\n"
            Matcher m = p.matcher(str);
            dest = m.replaceAll("");
        }
        return dest.replaceAll("\"","'");
    }

    //替换斜杠
    public static String replaceSlash(String resultStr) {
        if(StringUtils.isNotBlank(resultStr)){
            resultStr = resultStr.replaceAll("\\\\","");
            if(StringUtils.isNotBlank(resultStr) && resultStr.startsWith("\"") && resultStr.endsWith("\""))   resultStr = resultStr.substring(1,resultStr.length()-1);
        }
        return resultStr;
    }


    public static String getReqPayProxyserv() {
        return REQ_PAY_PROXYSERV;
    }



    /**
     * 查找通道中文名称
     */
    public static String  getChannelCNameByChannelName(String channelName){
        if(StringUtils.isBlank(channelName))
            return "";
        String channelCNameResult = "";
        for (ExcelChannel excelChannel : channelConfParam) {
            String excelChannelName = excelChannel.get通道名称().trim();
            if(channelName.trim().equalsIgnoreCase(excelChannelName)){
                    channelCNameResult+=excelChannel.get第三方名称()+" / ";
                if(YES.equalsIgnoreCase(excelChannel.get微信二维码()) || YES.equalsIgnoreCase(excelChannel.get微信公众号()) ||  YES.equalsIgnoreCase(excelChannel.get微信反扫())){
                    channelCNameResult+= "微信";
                }
                else if(YES.equalsIgnoreCase(excelChannel.get支付宝二维码()) || YES.equalsIgnoreCase(excelChannel.get支付宝公众号()) ||  YES.equalsIgnoreCase(excelChannel.get支付宝反扫())  ){
                    channelCNameResult+= "支付宝";
                }else if(YES.equalsIgnoreCase(excelChannel.getQQ钱包二维码()) ||  YES.equalsIgnoreCase(excelChannel.getQQ钱包反扫())  ){
                    channelCNameResult+= "QQ钱包";
                } else if(YES.equalsIgnoreCase(excelChannel.get百度钱包())){
                    channelCNameResult+= "百度钱包";
                }else if(YES.equalsIgnoreCase(excelChannel.get京东钱包())  || YES.equalsIgnoreCase(excelChannel.get京东快捷支付())  ){
                    channelCNameResult+= "京东钱包";
                }else if(YES.equalsIgnoreCase(excelChannel.get银联钱包())   || YES.equalsIgnoreCase(excelChannel.get银联快捷支付())   ){
                    channelCNameResult+= "银联钱包";
                }else if(YES.equalsIgnoreCase(excelChannel.get网上银行())){
                    channelCNameResult+= "网银"+" / "+excelChannel.get银行名称();
                }else if(YES.equalsIgnoreCase(excelChannel.get代付通道())){
                    channelCNameResult+= "代付"+" / "+excelChannel.get银行名称();
                }
                if(channelName.contains("_WAP_")||channelName.contains("_WAPAPP_")){
                    channelCNameResult+="_WAP";
                }
                if(channelName.endsWith("_GZH")){
                    channelCNameResult+="_公众号";
                }
                if(channelName.endsWith("_FS")){
                    channelCNameResult+="_反扫";
                }
                if(channelName.endsWith("_KJZF") &&!channelName.contains("_WY_") ){
                    channelCNameResult+="_快捷支付";
                }
                break;
            }
        }
        return channelCNameResult;
    }




    /**
     * 请求结果错误消息，前增加处理的服务器名
     */
    public static final synchronized RequestPayResult addHostNameInErrMSG(RequestPayResult requestPayResult){
        if(null!=requestPayResult && !"SUCCESS".equalsIgnoreCase(requestPayResult.getRequestPayCode())){
           requestPayResult.setRequestPayErrorMsg(requestPayResult.getRequestPayErrorMsg()); //+" --[Server: "+hostName+"]"
        }
        return requestPayResult;
    }

    public static final synchronized String encryptOrderId(String str) {
        String sn = "helloworld"; // 密钥
        int[] snNum = new int[str.length()];
        String result = "";
        String temp = "";
        for (int i = 0, j = 0; i < str.length(); i++, j++) {
            if (j == sn.length())
                j = 0;
            snNum[i] = str.charAt(i) ^ sn.charAt(j);
        }
        for (int k = 0; k < str.length(); k++) {

            if (snNum[k] < 10) {
                temp = "00" + snNum[k];
            } else {
                if (snNum[k] < 100) {
                    temp = "0" + snNum[k];
                }
            }
            result += temp;
        }
        return result;
    }


    public static final synchronized  String subUrl(String url){
        if(StringUtils.isNotBlank(url) && url.endsWith("/") || url.endsWith("\\")){
            url = url.substring(0,url.length()-1);
        }
        return url;
    }

    public static   final synchronized boolean  isQQSM(ChannelWrapper channelWrapper){
        return channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_QQ_SM");
    }
    
    public static   final synchronized boolean  isWapOrApp(ChannelWrapper channelWrapper){
        return channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAP_")||channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAPAPP_");
    }


    public static   final synchronized boolean  isWEBWAPAPP(ChannelWrapper channelWrapper){
        return channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WEBWAPAPP_");
    }

    public static   final synchronized boolean  isWEBWAPAPP_SM(ChannelWrapper channelWrapper){
        return channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WEBWAPAPP_")  && channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_SM");
    }

    public static   final synchronized boolean  isWebJdKjzf(ChannelWrapper channelWrapper){
        return channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_WEBWAPAPP_JD_KJZF");
    }


    //web 银联快捷支付
    public static   final synchronized boolean  isWebYlKjzf(ChannelWrapper channelWrapper){
        return channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_WEBWAPAPP_YL_KJZF");
    }


    //web 微信公众号
    public static   final synchronized boolean  isWebWxGZH(ChannelWrapper channelWrapper){
        return channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_WEBWAPAPP_WX_GZH");
    }

    //web 支付宝公众号
    public static   final synchronized boolean  isWebZfbGZH(ChannelWrapper channelWrapper){
        return channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_WEBWAPAPP_ZFB_GZH");
    }

    //web 网银快捷支付	@author andrew
    public static   final synchronized boolean  isWebWyKjzf(ChannelWrapper channelWrapper){
        return channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_WEB_WY_KJZF") ||    channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_WEBWAPAPP_WY_KJZF");
    }

    public static   final synchronized boolean  isWxGZH(ChannelWrapper channelWrapper){
        return channelWrapper!=null && channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WX_") && channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_GZH") ;
    }

    public static   final synchronized boolean  isWxSM(ChannelWrapper channelWrapper){
        return channelWrapper!=null &&  channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_WEBWAPAPP_WX_SM") ;
    }


    public static   final synchronized boolean  isZfbSM(ChannelWrapper channelWrapper){
        return channelWrapper!=null &&  channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_WEBWAPAPP_ZFB_SM") ;
    }


    public static   final synchronized boolean  isWY(ChannelWrapper channelWrapper){
        return channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WY_");
    }

    public static   final synchronized boolean  isFS(ChannelWrapper channelWrapper){
        return channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_FS");
    }

    public static   final synchronized boolean  isFS(ReqPayInfo reqPayInfo){
        return reqPayInfo.getAPI_CHANNEL_BANK_NAME().endsWith("_FS");
    }


    //需要使用我方反扫页面的通道
    public static final  boolean isNotDirectFS(String channelName){
        Set<String> directChannelNames = new HashSet<String>(){{
//            add("RENXIN_BANK_WEBWAPAPP_WX_FS");
//            add("RENXIN_BANK_WEBWAPAPP_ZFB_FS");
//            add("CHENGHUI_BANK_WEBWAPAPP_ZFB_FS");
//            add("MANGGUO_BANK_WEBWAPAPP_WX_FS");
//            add("SHANYIFU_BANK_WEBWAPAPP_ZFB_FS");
//            add("SHANYIFU_BANK_WEBWAPAPP_WX_FS");
//            add("SHANYIFU_BANK_WAP_ZFB_FS");
//            add("SHANYIFU_BANK_WAP_WX_FS");
//            add("SHUNFU_BANK_WEBWAPAPP_ZFB_FS");
//            add("SHUNFU_BANK_WEBWAPAPP_WX_FS");
//            add("BAIFU_BANK_WEBWAPAPP_WX_FS");
//            add("BAIFU_BANK_WEBWAPAPP_ZFB_FS");
            add("ZHITONGBAO_BANK_WEBWAPAPP_WX_FS");
            add("ZHITONGBAO_BANK_WEBWAPAPP_QQ_FS");
            add("ZHITONGBAO_BANK_WEBWAPAPP_ZFB_FS");
        }};
        return directChannelNames.contains(channelName);
    }


    public static   final synchronized boolean  isYL(ChannelWrapper channelWrapper){
        return channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_YL_");
    }

    public static   final synchronized boolean  isYLSM(ChannelWrapper channelWrapper){
        return channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_WEBWAPAPP_YL_SM");
    }

    public static   final synchronized boolean  isJDSM(ChannelWrapper channelWrapper){
        return channelWrapper.getAPI_CHANNEL_BANK_NAME().endsWith("_WEBWAPAPP_JD_SM");
    }


    public static   final synchronized boolean  isYLWAP(ChannelWrapper channelWrapper){
    	return channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_WAP_YL_");
    }


    /**
     *
     * @param channelWrapper
     * @return
     * @author andrew
     * Feb 5, 2018
     */
    public static   final synchronized boolean  isYLKJ(ChannelWrapper channelWrapper){
        return channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_YL_KJZF");
    }


    public static   final synchronized boolean  isZFB(ChannelWrapper channelWrapper){
        return channelWrapper.getAPI_CHANNEL_BANK_NAME().contains("_ZFB_");
    }



    public static final synchronized StringBuffer getHtmlContent(String actionUrl,Map<String, ?> payParam){
        StringBuffer sbHtml = new StringBuffer();
        sbHtml.append("<header>");
        sbHtml.append("<meta http-equiv='expires' content='0'>");
        sbHtml.append("<meta http-equiv='pragma' content='no-cache'>");
        sbHtml.append("<meta http-equiv='cache-control' content='no-cache'>");
        sbHtml.append("<meta name='viewport' content='width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0'>");
        sbHtml.append("<meta name='format-detection' content='telephone=no'>");
        sbHtml.append("<meta name='apple-mobile-web-app-capable' content='yes'>");
        sbHtml.append("</header>");
        sbHtml.append("<input type='hidden' id='timestamp' value='"+System.currentTimeMillis()+"'/>");
        sbHtml.append("<div id='msg' style='padding:8px;border:1px solid #96c2f1;background:#eff7ff;text-align: center;'> 正在支付...... <div>");
        sbHtml.append("<form id='payform' name='payform' action='" + actionUrl + "' method='post'>");
        for (Map.Entry<String, ?> entry : payParam.entrySet()) {
            sbHtml.append("<input type='hidden' name='" + entry.getKey() + "' value='" + entry.getValue() + "'/>");
        }
        sbHtml.append("</form>");
        sbHtml.append("<script>var timestampC=document.getElementById('timestamp').value; var timestampB=new Date().getTime();if(timestampB-timestampC>86400000){  document.getElementById('msg').innerHTML='下单时间过长，为避免重复支付，请先退出浏览器后，重新下单。';}else{document.forms['payform'].submit();}</script>");
       return sbHtml;
    }


    public static final synchronized String getHtmlUrl(String actionUrl,Map<String, ?> payParam){
        if(StringUtils.isNotBlank(actionUrl) && MapUtils.isNotEmpty(payParam)){
            StringBuffer sbHtml = new StringBuffer();
            sbHtml.append(actionUrl.trim()).append("?");
            for (Map.Entry<String, ?> entry : payParam.entrySet()) {
                if(StringUtils.isBlank(entry.getKey())) continue;
                sbHtml.append( entry.getKey() + "=" + entry.getValue() + "&");
            }
            return sbHtml.substring(0,sbHtml.length()-1);
        }
      return null;
    }







    public static final Map<String,String> getAllOid(){ //ALL
        LinkedHashMap<String, String> oidMaps = new LinkedHashMap<String,String>(){{
            put("500","(TW)-500-500VIP");//---9
            put("188","(TW)-188-彩8");
            put("199","(TW)-199-彩99");
            put("78", "(TW)-78-玩彩票");
            put("699","(TW)-699-699彩票");
            put("200","(TW)-200-彩366");
            put("300","(TW)-300-彩889(彩1)");
            put("66", "(TW)-66-彩票网(彩66)");
            put("99", "(TW)-99-金龙彩票");
            put("256","(TW)-256-彩256");//---5
            put("83", "(TW)-83-好彩客");
            put("100","(TW)-100-彩132");
            put("77", "(TW)-77-彩77");
            put("35", "(TW)-35-35彩票");

            put("33", "(HK)-33-彩票33");//---5
            put("38", "(HK)-38-彩宝");
            put("58", "(HK)-58-58彩票");
            put("9",  "(HK)-9-9万");
            put("11", "(HK)-11-时时彩");
            put("16", "(HK)-16-彩16");//---6
            put("88", "(HK)-88-好彩票");
            put("728","(HK)-728-728");
            put("111","(HK)-111-大赢家");
            put("288","(HK)-288-288彩票");
            put("68", "(HK)-68-乐彩客");
        }};
        return oidMaps;
    }


    public static final Map<String,String> getAllPayType(){  //ALL
        TreeMap<String, String> payTypeMaps = new TreeMap<String,String>(){{
            put("_WX_","微信");
            put("_ZFB_","支付宝");
            put("_QQ_","QQ");
            put("_JD_","京东");
            put("_BD_","百度");
            put("_WY_","网银");
            put("_YL_","银联");
        }};
        return payTypeMaps;
    }


    public static final Map<String,String> getAllSearchResult(){      //ALL
        TreeMap<String, String> searchResultTypeMaps = new TreeMap<String,String>(){{
            put("SUCCESS","成功");
            put("ERROR","失败");
        }};
        return searchResultTypeMaps;
    }



    //3 APP-Android，4 APP-IOS，5 APP-Other，6 WEB，7 Windows，8 Mac,9 WAP
    public static String getOrderForm(String dbFromId){
        if(PayEumeration.APP.equalsIgnoreCase(dbFromId)) return PayEumeration.APP;
        if(PayEumeration.WEB.equalsIgnoreCase(dbFromId)) return PayEumeration.WEB;
        if(PayEumeration.WAP.equalsIgnoreCase(dbFromId)) return PayEumeration.WAP;
        if(StringUtils.isNotBlank(dbFromId)){
            switch (dbFromId){
                case "3": return PayEumeration.APP;
                case "4": return PayEumeration.APP;
                case "5": return PayEumeration.APP;
                case "6": return PayEumeration.WEB;
                case "7": return PayEumeration.WEB;
                case "8": return PayEumeration.WEB;
                case "9": return PayEumeration.WAP;
                default: return  PayEumeration.WEB;
            }
        }
        return PayEumeration.WEB;
    }


    public static  boolean isOrderFromWapOrApp(String dbFromId){
        return !getOrderForm(dbFromId).equalsIgnoreCase(PayEumeration.WEB);
    }




    public static final Map<String,String> getAllPayCo(){
        return channelCoConfigParamAll;
    }

    public static final Map<String,String> getAllPayChannelAndCoCname(){
        return channelAndCoCname;
    }

    public static Map<String, String> getChannelAndCoId() {
        return channelAndCoId;
    }

    //解密js前端
    public static String decodeQRContext(String encodeHideUrl){
        //String [] world=new String[] {"length","charCodeAt","charAt","fromCharCode","","join","decoder"};
       // String encodeHideUrl = "e3WqfHmvPj9we4ixZYlwZnm7dHG6eYKtQ4CzQUWpb4[LOll>";
        String hidUrlResult = "";
        for (int i = 0; i < encodeHideUrl.length(); i++) {
            hidUrlResult+=(char)(encodeHideUrl.charAt(i)-1);
        }
        return  new String(Base64.getDecoder().decode(hidUrlResult));
    }

    public static boolean sourceFromPayRest(Map<String,String> params){
        if(!org.springframework.util.CollectionUtils.isEmpty(params)&&  params.containsKey("sources") && params.get("sources").startsWith("pay-rest"))return true;
        return false;
    }


    public static String getFsAuthCode(ChannelWrapper channelWrapper){
        if(null!=channelWrapper && !org.springframework.util.CollectionUtils.isEmpty(channelWrapper.getResParams()) && StringUtils.isNotBlank(channelWrapper.getResParams().get("authCode"))){
            return channelWrapper.getResParams().get("authCode");
        }
        return null;
    }


    public static void writeObjectToDisk(Object o,String fileFullPath){
        try {
            ObjectOutputStream os = new ObjectOutputStream( new FileOutputStream(fileFullPath));
            os.writeObject(o);// 将User对象写进文件
            os.close();
        } catch (IOException e) { }
    }


    public static Object  readObjectToDisk(String fileFullPath){
        try {
            ObjectInputStream is = new ObjectInputStream(new FileInputStream( fileFullPath));
            Object o =  is.readObject();
            is.close();
            return o;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String plusLong(String a,String b){
        long al = 0 ,  bl = 0;
        if(isIntNumber(a))   al = Long.parseLong(a);
        if(isIntNumber(b))   bl = Long.parseLong(b);
        return Long.toString(al+bl);
    }



    public static String divLong(String a,String b){
        double ad= 0 ,bd = 0;
        if(isIntNumber(a))   ad = Double.parseDouble(a);
        if(isIntNumber(b))   bd = Double.parseDouble(b);
        if(ad==0||bd==0) return "0.00";
        return String.format("%.2f", (ad/bd)*100);
    }



    public static boolean isIntNumber(String str) {
        if(StringUtils.isNotBlank(str)){
            Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
            return pattern.matcher(str).matches();
        }
        return false;
    }

    public static String getFromJsonObject(JSONObject jsonObject,String key){
        if(null!=jsonObject && jsonObject.containsKey(key))
            return jsonObject.getString(key);
        return null;
    }


    public static boolean valJsonObj(JSONObject jsonResultStr,String key,String value){
        if(jsonResultStr!=null && StringUtils.isNotBlank(key)&& jsonResultStr.containsKey(key)){
            if(StringUtils.isNotBlank(value) ){
                 return value.equalsIgnoreCase(jsonResultStr.getString(key));
            }
            return StringUtils.isNotBlank(jsonResultStr.getString(key));
        }
        return false;
    }



    public static boolean valJsonObj(JSONObject jsonResultStr,String key,String ... values){
        if(jsonResultStr!=null && StringUtils.isNotBlank(key)&& jsonResultStr.containsKey(key)){
            if(null!=values && values.length>0){
                for (String value : values) {
                    if(StringUtils.isNotBlank(value) ){
                        if(value.equalsIgnoreCase(jsonResultStr.getString(key))) return true;
                    }
                }
                return  false;
            }
            return  false;
        }
        return false;
    }





    public static boolean valJsonObjInSideJsonObj(JSONObject jsonObject,String jsonObjkey,String key,String ... value){
        if(jsonObject!=null && jsonObject.containsKey(jsonObjkey) && null!=jsonObject.getJSONObject(jsonObjkey)){
            JSONObject jsonObj = jsonObject.getJSONObject(jsonObjkey);
           return valJsonObj(jsonObj,key,value);
        }
        return false;
    }



    public static String  valJsonObjInSideJsonObj(JSONObject jsonObject,String jsonObjkey,String key){
        if(jsonObject!=null && jsonObject.containsKey(jsonObjkey) && null!=jsonObject.getJSONObject(jsonObjkey)  &&jsonObject.getJSONObject(jsonObjkey).containsKey(key) ){
            return jsonObject.getJSONObject(jsonObjkey).getString(key);
        }
        return null;
    }




    public  void  saveStrInRedis(String key,String value,long timeout){
        redisTemplate.opsForValue().set(key,value,timeout, TimeUnit.SECONDS);
    }

    public  String  getStrFromRedis(String key){
    	Object object = redisTemplate.opsForValue().get(key);
        return null == object ? null : object.toString();
    }

    public void saveMapInRedis(String key,long timeout,Map map){
        redisTemplate.opsForHash().putAll(key,map);
        redisTemplate.expire(key, timeout, TimeUnit.SECONDS);//过期时间
    }

    public  Map<String, String> getMapFromRedis(String key){
        return redisTemplate.opsForHash().entries(key);
    }


   //获得最终html,orderID随便写
   //HtmlPage mainPage =handlerUtil.getEndHtml(channelWrapper.getAPI_CHANNEL_BANK_URL(),channelWrapper.getAPI_ORDER_ID(),payParam);
   // final HtmlPage pakcagePage = (HtmlPage) mainPage.getFrameByName("mainwindow").getEnclosedPage();
   //final HtmlImage htmlImage = (HtmlImage) htmlPage.getByXPath("//img[@id='Image']").get(0);
   //qrContent = htmlImage.getSrcAttribute();
    public HtmlPage getEndHtml(String url,String orderId,Map<String,?> payParam) throws PayException {
        HtmlPage htmlPage = null;
        String TMPORDERID=HandlerUtil.TMPORDERKEY.concat(orderId);
        StringBuffer htmlContent = HandlerUtil.getHtmlContent(url, payParam);
        saveStrInRedis(TMPORDERID,htmlContent.toString(),3000L);
        String payuUrl = HttpSchema.concat(RunTimeInfo.startInfo.getIpAddress().concat(PortSpl).concat(RunTimeInfo.startInfo.getPort()).concat(REQPAYGETQRHTMLPATH)).concat(TMPORDERID);
        return HttpUtil.sendByHtmlUnit(payuUrl);
    }

    public static HtmlPage getEndHtml(String url) throws PayException {
        return HttpUtil.sendByHtmlUnit(url);
    }



    //支付金额范围内
    public static boolean isRightAmount(String orderAmount,String realPayedAmount,String allowAmount){
        return dc.pay.utils.StrUtil.parseLong(realPayedAmount)  >= dc.pay.utils.StrUtil.parseLong(orderAmount)-dc.pay.utils.StrUtil.parseLong(allowAmount);
    }

    /**
    * 允许支付偏差判断
    *
    * @param orderAmount       db订单金额      分
    * @param realPayedAmount   实际支付金额      分
    * @param allowAmount       允许偏差金额      分
    * @return                  返回true，表明在允许的范围里
    * @author andrew
    * Jul 24, 2018
    */
    public static boolean isAllowAmountt(String orderAmount,String realPayedAmount,String allowAmount){
        return dc.pay.utils.StrUtil.parseLong(allowAmount) >= Math.abs(dc.pay.utils.StrUtil.parseLong(orderAmount)-dc.pay.utils.StrUtil.parseLong(realPayedAmount));
    }

    //回调Map,只有Key,key为json
    public static Map<String,String> getReturnMaps(Map<String,String> apiresponse){
        HashMap<String,String> result = Maps.newHashMap();
        if(apiresponse!=null && apiresponse.keySet().size()==1){
            Set<String> keySet = apiresponse.keySet();
            String jsonObj = keySet.iterator().next();
            Map map = JSON.parseObject(jsonObj, Map.class);
            map.forEach((key,value)->{
                result.put(String.valueOf(key),String.valueOf(value));
            });
            return result;
        }
        return  result;
    }


  public static Long  addStringNumber(String a,String b){
        return  StrUtil.parseLong(a)+StrUtil.parseLong(b);
    }


    //去空格
    public static String trim(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("\\s*", "");
    }


    //通过商户号获取密钥-请求支付
    public String getApiKeyFromReqPayMemberId(String memberId){
        if(StringUtils.isNotBlank(memberId)){
            ReqPayList reqPayList = reqPayListService.getReqpayListByMemberId(memberId);
            if(null!=reqPayList && reqPayList.getReqPayInfo()!=null){
                String api_key_enc = reqPayList.getReqPayInfo().getAPI_KEY();
                try {
                    return RsaUtil.decryptAndCache(api_key_enc);
                } catch (PayException e) {
                    return null;
                }
            }
        }
        return null;
    }


    //通过商户号获取公钥-请求代付
    public String getApiKeyFromReqDaifuMemberId(String memberId){
        if(StringUtils.isNotBlank(memberId)){
            ReqDaiFuList reqDaifuList = reqDaiFuListService.getReqDaifuListByMemberId(memberId);
            if(null!=reqDaifuList && reqDaifuList.getReqDaifuInfo()!=null){
                String api_key_enc = reqDaifuList.getReqDaifuInfo().getAPI_KEY();
                try {
                    return RsaUtil.decryptAndCache(api_key_enc);
                } catch (PayException e) {
                    return null;
                }
            }
        }
        return null;
    }


    //通过商户号获取密钥-请求代付
    public String getApiPublicKeyFromReqDaifuMemberId(String memberId) {
        if (StringUtils.isNotBlank(memberId)) {
            ReqDaiFuList reqDaifuList = reqDaiFuListService.getReqDaifuListByMemberId(memberId);
            if (null != reqDaifuList && reqDaifuList.getReqDaifuInfo() != null) {
                String api_key_enc = reqDaifuList.getReqDaifuInfo().getAPI_PUBLIC_KEY();
                try {
                    return RsaUtil.decryptAndCache(api_key_enc);
                } catch (PayException e) {
                    return null;
                }
            }
        }
        return null;
    }




    //只有key 或者只有value 是json 的map,转map
    public static Map<String,String> oneSizeJsonMapToMap(Map<String, String> maps){
        JSONObject jsonObj = new JSONObject(true);
        Map<String, String> oMaps=Maps.newLinkedHashMap();
        if(null!=maps && maps.size()==1){
            String oKey="";
            String oValue="";
            for(String key : maps.keySet()){
                oValue = maps.get(key); oKey=key.trim();
            }
            if(StringUtils.isNotBlank(oKey) && StringUtils.isBlank(oValue) && oKey.contains("{") && oKey.contains("}")){
               // oMaps = jsonObj.toJavaObject(JSON.parseObject(oKey), Map.class);
                oMaps =  jsonObj.parseObject(oKey, new TypeReference<LinkedHashMap<String, String>>() {},Feature.OrderedField);
            }else if(StringUtils.isNotBlank(oValue) && StringUtils.isBlank(oKey)  && oValue.contains("{") && oValue.contains("}")){
               // oMaps = jsonObj.toJavaObject(JSON.parseObject(oValue), Map.class);
                oMaps =  jsonObj.parseObject(oValue, new TypeReference<LinkedHashMap<String, String>>() {},Feature.OrderedField);
            }
        }
        return oMaps;
    }

    //只有key 或者只有value 是json 的map,转map
    public static Map<String,String> oneSizeJsonUrlEncodeMapToMap(Map<String, String> maps){
        JSONObject jsonObj = new JSONObject(true);
        Map<String, String> oMaps=Maps.newLinkedHashMap();
        if(null!=maps && maps.size()==1){
            String oKey="";
            String oValue="";
            for(String key : maps.keySet()){
                oValue = UrlDecode(maps.get(key)); oKey=UrlDecode(key.trim());
            }
            if(StringUtils.isNotBlank(oKey) && StringUtils.isBlank(oValue) && oKey.contains("{") && oKey.contains("}")){
                // oMaps = jsonObj.toJavaObject(JSON.parseObject(oKey), Map.class);
                oMaps =  jsonObj.parseObject(oKey, new TypeReference<LinkedHashMap<String, String>>() {},Feature.OrderedField);
            }else if(StringUtils.isNotBlank(oValue) && StringUtils.isBlank(oKey)  && oValue.contains("{") && oValue.contains("}")){
                // oMaps = jsonObj.toJavaObject(JSON.parseObject(oValue), Map.class);
                oMaps =  jsonObj.parseObject(oValue, new TypeReference<LinkedHashMap<String, String>>() {},Feature.OrderedField);
            }
        }
        return oMaps;
    }




    //跳转域名转小写
    public static String fixAppNoSupportHTTPS(String url){
        try {
            String domain = HandlerUtil.getDomain(url);
            return url.replace(domain,domain.toLowerCase());
        } catch (PayException e) {
            return url;
        }
    }



    public static   List<String> getImgSrc(String html){
        LinkedList<String> resList = Lists.newLinkedList();
        Pattern p = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>");//<img[^<>]*src=[\'\"]([0-9A-Za-z.\\/]*)[\'\"].(.*?)>");
        Matcher m = p.matcher(html);
        while(m.find()){
            resList.add(m.group(1));
        }
        return resList;
    }

    public static Map convertStringMapToIntMapByKey( Map<String,String>  params,String... keys) throws PayException {
        LinkedHashMap  converRes = Maps.newLinkedHashMap();
        try{
            if(null!=params && !params.isEmpty() && null!=keys && keys.length>0){
                converRes.putAll(params);
                for (String key : keys) {
                    converRes.put(key,Long.parseLong(params.get(key)));
                }
            }
        }catch (Exception e){ throw new PayException("参数转换错误，"+JSON.toJSONString(params)); }
        return converRes;
    }



    //创建代付channelWraper
    public  ChannelWrapper createDaifuChannelWrapper(final ReqDaifuInfo reqDaifuInfo, RunTimeInfo runTimeInfo) throws PayException {
        ValidateUtil.valiRSA_KEY(reqDaifuInfo);
        ChannelWrapper channel = new ChannelWrapper();
        channel.setAPI_KEY(reqDaifuInfo.getAPI_KEY().replaceAll("\\s*", ""));
        channel.setAPI_PUBLIC_KEY(reqDaifuInfo.getAPI_PUBLIC_KEY().replaceAll("\\s*", ""));
        channel.setAPI_OTHER_PARAM(reqDaifuInfo.getAPI_OTHER_PARAM());
        channel.setAPI_Client_IP(reqDaifuInfo.getAPI_Client_IP());
        channel.setAPI_OID(reqDaifuInfo.getAPI_OID());
        channel.setAPI_MEMBERID(reqDaifuInfo.getAPI_MEMBERID());
        channel.setAPI_AMOUNT(reqDaifuInfo.getAPI_AMOUNT());
        channel.setAPI_ORDER_ID(reqDaifuInfo.getAPI_ORDER_ID());
        channel.setAPI_OrDER_TIME(reqDaifuInfo.getAPI_OrDER_TIME());
        channel.setAPI_CHANNEL_BANK_NAME(reqDaifuInfo.getAPI_CHANNEL_BANK_NAME());
        channel.setAPI_CHANNEL_BANK_NOTIFYURL(reqDaifuInfo);
        channel.setAPI_CHANNEL_BANK_URL(getChannelBankRequestURL(reqDaifuInfo.getAPI_CHANNEL_BANK_NAME()));
        channel.setAPI_CHANNEL_BANK_NAME_FlAG(getChannelBankName(reqDaifuInfo.getAPI_CHANNEL_BANK_NAME()));
        channel.setAPI_CHANNEL_SIGN_PARAM_NAME(getChannelBankSignParamName(reqDaifuInfo.getAPI_CHANNEL_BANK_NAME()));
        channel.setAPI_CUSTOMER_ACCOUNT(reqDaifuInfo.getAPI_CUSTOMER_ACCOUNT());
        channel.setAPI_CUSTOMER_NAME(reqDaifuInfo.getAPI_CUSTOMER_NAME());
        channel.setAPI_CUSTOMER_BANK_NAME(reqDaifuInfo.getAPI_CUSTOMER_BANK_NAME());
        channel.setAPI_CUSTOMER_BANK_BRANCH(StringUtils.isBlank(reqDaifuInfo.getAPI_CUSTOMER_BANK_BRANCH())?wzfh:reqDaifuInfo.getAPI_CUSTOMER_BANK_BRANCH());
        channel.setAPI_CUSTOMER_BANK_SUB_BRANCH(StringUtils.isBlank(reqDaifuInfo.getAPI_CUSTOMER_BANK_SUB_BRANCH())?wzzh:reqDaifuInfo.getAPI_CUSTOMER_BANK_SUB_BRANCH());
        channel.setAPI_CUSTOMER_BANK_NUMBER(reqDaifuInfo.getAPI_CUSTOMER_BANK_NUMBER());
        channel.setResParams(reqDaifuInfo.getResParams());
        return channel;
    }



    //创建请求channelWraper
    public  ChannelWrapper createPayChannelWrapper(final ReqPayInfo reqPayInfo) throws PayException {
        ChannelWrapper channel = new ChannelWrapper();
        ValidateUtil.valiRSA_KEY(reqPayInfo);
        channel.setAPI_KEY(reqPayInfo.getAPI_KEY().replaceAll("\\s*", ""));
        channel.setAPI_PUBLIC_KEY(reqPayInfo.getAPI_PUBLIC_KEY().replaceAll("\\s*", ""));
        channel.setAPI_WEB_URL(reqPayInfo.getAPI_WEB_URL());
        channel.setAPI_JUMP_URL_PREFIX(reqPayInfo.getAPI_JUMP_URL_PREFIX());
        channel.setAPI_OTHER_PARAM(reqPayInfo.getAPI_OTHER_PARAM());
        channel.setAPI_Client_IP(reqPayInfo.getAPI_Client_IP());
        channel.setAPI_ORDER_FROM(reqPayInfo.getAPI_ORDER_FROM());
        channel.setAPI_OID(reqPayInfo.getAPI_OID());
        channel.setAPI_MEMBERID(reqPayInfo.getAPI_MEMBERID());
        channel.setAPI_AMOUNT(reqPayInfo.getAPI_AMOUNT());
        channel.setAPI_ORDER_ID(reqPayInfo.getAPI_ORDER_ID());
        channel.setAPI_OrDER_TIME(reqPayInfo.getAPI_OrDER_TIME());
        channel.setAPI_CHANNEL_BANK_NAME(reqPayInfo.getAPI_CHANNEL_BANK_NAME());
        channel.setAPI_CHANNEL_BANK_NOTIFYURL(reqPayInfo.getAPI_NOTIFY_URL_PREFIX(),reqPayInfo.getAPI_CHANNEL_BANK_NAME());
        channel.setAPI_CHANNEL_BANK_URL(getChannelBankRequestURL(reqPayInfo.getAPI_CHANNEL_BANK_NAME()));
        channel.setAPI_CHANNEL_BANK_NAME_FlAG(getChannelBankName(reqPayInfo.getAPI_CHANNEL_BANK_NAME()));
        channel.setAPI_CHANNEL_SIGN_PARAM_NAME(getChannelBankSignParamName(reqPayInfo.getAPI_CHANNEL_BANK_NAME()));
        channel.setResParams(reqPayInfo.getResParams());
        if(StringUtils.isNotBlank(reqPayInfo.getAPI_MEMBER_PLATFORMID())){
            channel.setAPI_MEMBER_PLATFORMID(reqPayInfo.getAPI_MEMBER_PLATFORMID());
        }else{
            channel.setAPI_MEMBER_PLATFORMID(reqPayInfo.getAPI_MEMBERID());
        }
        return channel;
    }




    public static  String getResponseDaifuSign(ResponseDaifuResult responseDaifuResult, String apiKey) {
        String paramsStr = String.format(
                "responseOrderID=%s&responseOrderState=%s&responseDaifuAmount=%s&responseDaifuChannel=%s&responseDaifuCode=%s&responseDaifuOid=%s&key=%s",
                responseDaifuResult.getResponseOrderID(), responseDaifuResult.getResponseOrderState(),
                responseDaifuResult.getResponseDaifuAmount(), responseDaifuResult.getResponseDaifuChannel(),
                responseDaifuResult.getResponseDaifuCode(), responseDaifuResult.getResponseDaifuOid(),
                apiKey);
        try {
            return HandlerUtil.getMD5UpperCase(paramsStr);
        } catch (PayException e) {
            log.error("生成[响应代付]签名出错：" + paramsStr);
            e.printStackTrace();
            return "";
        }
    }

    public static  String getResponsePaySign(ResponsePayResult responsePayResult, String apiKey) {
        String paramsStr = String.format(
                "responseOrderID=%s&responseOrderState=%s&responsePayAmount=%s&responsePayChannel=%s&responsePayCode=%s&responsePayOid=%s&key=%s",
                responsePayResult.getResponseOrderID(), responsePayResult.getResponseOrderState(),
                responsePayResult.getResponsePayAmount(), responsePayResult.getResponsePayChannel(),
                responsePayResult.getResponsePayCode(), responsePayResult.getResponsePayOid(),
                apiKey);
        try {
            return HandlerUtil.getMD5UpperCase(paramsStr);
        } catch (PayException e) {
            log.error("生成[响应支付]签名出错：" + paramsStr);
            e.printStackTrace();
            return "";
        }
    }


    //自动查询，默认每分钟查询1次，查询2小时
    public void addQueryOrderJob(String orderId,String serverUrl,Map<String,String> params){
        addQueryOrderJob( orderId, null, serverUrl, params,0 ,120);
    }


    public void addQueryOrderJob(String orderId,String serverId,String serverUrl,Map<String,String> params,int times,int totalTimes)  {
        serverId = null==serverId?runTimeInfo.getServerId():serverId;
        String key = String.format(PayEumeration.AutoQueryDaifuKeyTMP,serverId);
        //redisTemplate.expire(key, 7, TimeUnit.DAYS);
        String hashKey =orderId;
        AutoQueryDaifu hashValue = new AutoQueryDaifu(orderId,key, serverId,  serverUrl,  System.currentTimeMillis()+PayEumeration.nextTimeAutoQueryDaifu, times,totalTimes, params,false);
        hashOps.put(key,hashKey,hashValue);
    }


    public static String  getUrlWithEncode(String baseUrl,Map<String,String>payParam,String encodeing ) throws UnsupportedEncodingException {
                StringBuffer sbrr = new StringBuffer();
                sbrr.append(baseUrl);
                boolean flag = true;
                Set<String> set = payParam.keySet();
                Iterator<String> it = set.iterator();
                while(it.hasNext()){
                    String key = it.next();
                    if(!payParam.containsKey(key)){
                        continue;
                    }
                    String value = payParam.get(key);
                    value = URLEncoder.encode(value, encodeing);
                    if(flag){
                        sbrr.append("?");
                        flag = false;
                    }else{
                        sbrr.append("&");
                    }
                    sbrr.append(key + "=" + value);
                }
                return sbrr.toString();
    }


}




