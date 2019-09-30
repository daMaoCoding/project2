package dc.pay.business.renrenfubaba;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.business.yqing.AesUtil;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;

/**
 * 
 * @author andrew
 * Sep 6, 2019
 */
@RequestPayHandler("RENRENFUBABA")
public final class RenRenFuBaBaPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RenRenFuBaBaPayRequestHandler.class);

    public static final String FLAG              ="RENRENFUBABA"; //商户账号
    public static final String MerchantAccount              ="MerchantAccount"; //商户账号
    public static final String Action              ="Action"; //接口名称：业务行为;对应四种业务类型英文名称代表
    public static final String Data              ="Data"; //业务参数:json格式;业务参数json包经过AES机密后的信息
    public static final String sign              ="sign"; //签名:使用MD5加密的签名
    
    //4.2 接口参数 名称 类型 是否必填 描述 备注
    //MerchantOrderNo string 是 商户订单号 商户内部订单号 
    private static final String MerchantOrderNo                ="MerchantOrderNo";
    //OrderPrice string 是 充值金额 
    private static final String OrderPrice                ="OrderPrice";
    //ChannelCode string 是 通道 可选值为 “ALIPAYH5”, “ALIPAY”,“ALIPAYKS”等。（详情见字典解释）
    private static final String ChannelCode                ="ChannelCode";
    //CallBackURL string 是 回调地址 接收本系统处理该笔充值请求的响应，系统会一直向该地址发送请求。商户根据系统的响应结果来处理自己的业务，系统的响 应时间范围是 24 小时。超时后则不再向该地址发送请求。
    private static final String CallBackURL                ="CallBackURL";
    //Message string 否 附加信息 
//    private static final String Message                ="Message";
    //PlatFormUserID string 否 平台传入用 户 ID
//    private static final String PlatFormUserID                ="PlatFormUserID";
    
//    private static final String key        ="key";
    //signature    数据签名    32    是    　
//    private static final String signature  ="Sign";

    static {
        String errorString = "Failed manually overriding key-length permissions.";
        int newMaxKeyLength;
        try {
            if ((newMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES")) < 256) {
                Class c = Class.forName("javax.crypto.CryptoAllPermissionCollection");
                Constructor con = c.getDeclaredConstructor();
                con.setAccessible(true);
                Object allPermissionCollection = con.newInstance();
                Field f = c.getDeclaredField("all_allowed");
                f.setAccessible(true);
                f.setBoolean(allPermissionCollection, true);
                c = Class.forName("javax.crypto.CryptoPermissions");
                con = c.getDeclaredConstructor();
                con.setAccessible(true);
                Object allPermissions = con.newInstance();
                f = c.getDeclaredField("perms");
                f.setAccessible(true);
                ((Map) f.get(allPermissions)).put("*", allPermissionCollection);
                c = Class.forName("javax.crypto.JceSecurityManager");
                f = c.getDeclaredField("defaultPolicy");
                f.setAccessible(true);
                Field mf = Field.class.getDeclaredField("modifiers");
                mf.setAccessible(true);
                mf.setInt(f, f.getModifiers() & ~Modifier.FINAL);
                f.set(null, allPermissions);
                newMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES");
            }
        } catch (Exception e) {
            throw new RuntimeException(errorString, e);
        }
        if (newMaxKeyLength < 256)
            throw new RuntimeException(errorString); // hack failed
    }
    
    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
        if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 3) {
            log.error("[人人付88]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号ID&登录账号&AESKey密钥");
            throw new PayException("[人人付88]-[请求支付]-“支付通道商号”输入数据格式为【中间使用&分隔】：商户号ID&登录账号&AESKey密钥");
        }
        try {
            handlerUtil.saveStrInRedis(FLAG+":"+channelWrapper.getAPI_MEMBERID().split("&")[1], channelWrapper.getAPI_MEMBERID().split("&")[2], 60*60*12*5);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[人人付88]-[请求支付]-1.向缓存中，存储密钥出错：{}",e.getMessage(),e);
            throw new PayException("[人人付88]-[请求支付]-1.向缓存中，存储密钥出错：{}",e);
        }
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(MerchantOrderNo,channelWrapper.getAPI_ORDER_ID());
                put(OrderPrice,  HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(ChannelCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                put(CallBackURL,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            }
        };
        log.debug("[人人付88]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

    /**
     * 得到加密业务参数的data
     *
     * @param paramMap
     * @return
     * @throws PayException 
     */
    public static String getReqEncryptData(Map<String, String> paramMap,String aesKey) throws PayException {
//        System.out.println("getReqEncryptData.aesKey:" + aesKey);
//        System.out.println("getReqEncryptData.paramMap to String:" + JSON.toJSONString(paramMap));
        String encryptData = "";
        try {
//            encryptData = AESUtil.encodeHexStr(AESUtil.encrypt(aesKey, JSON.toJSONString(paramMap)), false);
//            encryptData = encrypt(JSON.toJSONString(paramMap),aesKey);
            encryptData = AesUtil.parseByte2HexStr(AesUtil.encrypt2(JSON.toJSONString(paramMap), aesKey));
//            System.out.println(("getReqEncryptData.encryptData:" + encryptData));
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[人人付88]-[请求支付]-2.生成加密出错，签名出错：{}",e.getMessage(),e);
            log.error("[人人付88]-[请求支付]-2.生成加密出错，签名出错aesKey："+aesKey);
            log.error("[人人付88]-[请求支付]-2.生成加密出错，签名出错JSON.toJSONString(paramMap)："+JSON.toJSONString(paramMap));
            log.error("[人人付88]-[请求支付]-2.生成加密出错，签名出错AesUtil.encrypt2："+AesUtil.encrypt2(JSON.toJSONString(paramMap), aesKey));
            throw new PayException(e.getMessage(),e);
        }
        return encryptData;
    }
    
    protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        //1、参数列表中，除去signature外，其他所有非空的参数都要参与签名，值为空的参数不用参与签名。
        //2、签名顺序按照参数名a到z的顺序排序，若遇到相同的首字母，则看第二个字母，以此类推，组成规则如下：
        
        String reqEncryptData = getReqEncryptData(api_response_params,channelWrapper.getAPI_MEMBERID().split("&")[2]);
        
        StringBuffer signSrc= new StringBuffer();
        signSrc.append("Action=").append("deposit").append("&");
        signSrc.append("Data=").append(reqEncryptData).append("&");
        signSrc.append("MerchantAccount=").append(channelWrapper.getAPI_MEMBERID().split("&")[1]).append("&");
        signSrc.append("Key=").append(channelWrapper.getAPI_KEY());
        String paramsStr = signSrc.toString();
        String signMd5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[人人付88]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMd5));
        return signMd5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
//        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        
        String reqEncryptData = getReqEncryptData(payParam,channelWrapper.getAPI_MEMBERID().split("&")[2]);
        Map<String,String> map = new LinkedHashMap<>();
        map.put(Action, "deposit");
        map.put(Data, reqEncryptData);
        map.put(MerchantAccount, channelWrapper.getAPI_MEMBERID().split("&")[1]);
        map.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

        HashMap<String, String> result = Maps.newHashMap();

        if (false) {
            result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());
            //result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));
        }else{
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(map),MediaType.APPLICATION_JSON_VALUE);
//            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL(), JSON.toJSONString(map),"application/x-www-form-urlencoded");
//            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL(), map, String.class, HttpMethod.POST,defaultHeaders);
            String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), map);
//            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL(), map,"UTF-8");
            //if (StringUtils.isBlank(resultStr)) {
            //    log.error("[人人付88]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //    throw new PayException(resultStr);
            //    //log.error("[人人付88]-[请求支付]-3.1.发送支付请求，获取支付请求返回值异常:返回空,参数：{}",JSON.toJSONString(map));
            //    //throw new PayException("返回空,参数："+JSON.toJSONString(map));
            //}
//            System.out.println("请求返回=========>"+resultStr);
            //if (!resultStr.contains("{") || !resultStr.contains("}")) {
            //   log.error("[人人付88]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            //   throw new PayException(resultStr);
            //}
            //JSONObject jsonObject = JSONObject.parseObject(resultStr);
            try {
                JSONObject jsonObject = JSONObject.parseObject(resultStr);
              //只取正确的值，其他情况抛出异常
                //if (null != jsonObject && jsonObject.containsKey("result_code") && "SUCCESS".equalsIgnoreCase(jsonObject.getString("result_code"))  && 
                //(jsonObject.containsKey("qrcode") && StringUtils.isNotBlank(jsonObject.getString("qrcode")) || 
                // jsonObject.containsKey("pay_url") && StringUtils.isNotBlank(jsonObject.getString("pay_url")))
                //){
                if (null != jsonObject && jsonObject.containsKey("Code") && "0".equalsIgnoreCase(jsonObject.getString("Code"))  && jsonObject.containsKey("Result") && StringUtils.isNotBlank(jsonObject.getString("Result")) && StringUtils.isNotBlank(jsonObject.getString("Sign"))) {
                    //使用seckey对除sign以外的公共参数进行md5加密生成sign值
                    String toSignStr = "Code=" + jsonObject.getString("Code") + "&Result=" + jsonObject.getString("Result") + "&Key=" + channelWrapper.getAPI_KEY();
                    String signMd5 = HandlerUtil.getMD5UpperCase(toSignStr);
                    if (jsonObject.getString("Sign").equalsIgnoreCase(signMd5)) {
                        //解密响应
                        try {
                            String resData = AESUtil.decrypt(channelWrapper.getAPI_MEMBERID().split("&")[2], jsonObject.getString("Result"));
//                            String resData = new String(AesUtil.decrypt2(AesUtil.parseHexStr2Byte(API_RESPONSE_PARAMS.get(data)), ApiKey.substring(0, 16)));
                            JSONObject jsonObject2 = JSONObject.parseObject(resData);
                            if (null != jsonObject2 && jsonObject2.containsKey("URL") && StringUtils.isNotBlank(jsonObject2.getString("URL"))) {
                                result.put( JUMPURL, jsonObject2.getString("URL"));
                            }else {
                                log.error("[人人付88]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                                throw new PayException(resultStr);
                            }
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            log.error("[人人付88]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                            throw new PayException(resultStr);
                        }
                    }
                }else {
                    log.error("[人人付88]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                    throw new PayException(resultStr);
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[人人付88]-[请求支付]-3.1.发送支付请求，及获取支付请求结果：" + JSON.toJSONString(resultStr) + "订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                throw new PayException(resultStr);
            }
        }
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        payResultList.add(result);
        log.debug("[人人付88]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
        return payResultList;
    }

    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult=  buildResult(resultMap, channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[人人付88]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
    
    /**
     * 加密
     * 
     * @param content   要加密的内容
     * @param key       用来生成128位密钥的密码
     * @return
     */
    public static String encrypt(String content, String key) {  
            try {    
                /**
                 *  KeyGenerator : 是个类，此类提供（对称）密钥生成器的功能。在生成密钥后，可以重复使用同一个 KeyGenerator 对象来生成更多的密钥。
                 *  使用其中的getInstance（String algorithm）方法进行构造对象；
                 *  algorithm ： 所请求密钥算法的标准名称。
                 */
                KeyGenerator kgen = KeyGenerator.getInstance("AES");  
                /**
                 * Random : 此类的实例用于生成伪随机数流。
                 * SecureRandom : 是Random的直接子类。此类提供强加密随机数生成器 (RNG)。还必须生成非确定性输出（而）。
                 * SecureRandom(byte[] seed) :  构造一个实现默认随机数算法的安全随机数生成器 (RNG)。使用指定的种子字节设置种子。
                 * init(int keysize, SecureRandom random) : 使用用户提供的随机源初始化此密钥生成器，使其具有确定的密钥大小。
                 * keysize : 密钥大小。这是特定于算法的一种规格，是以位数为单位指定的。
                 * random : 此密钥生成器的随机源
                 */
                kgen.init(128, new SecureRandom(key.getBytes())); 
                /**
                 * java.security包中有接口 Key，SecretKey是Key的子接口，SecretKeySpec是SecretKey的实现类。
                 * Key : Key 是所有密钥的顶层接口。它定义了供所有密钥对象共享的功能。
                 * SecretKey : 此接口不包含方法或常量。其唯一目的是分组秘密密钥（并为其提供类型安全）。
                 * SecretKeySpec : 可以使用此类来根据一个字节数组构造一个 SecretKey
                 * 
                 * generateKey() : 生成一个密钥。
                 */
                SecretKey secretKey = kgen.generateKey();
                /**
                 * getEncoded() : 是Key接口中的方法；返回基本编码格式的密钥，如果此密钥不支持编码，则返回 null。
                 */
                byte[] enCodeFormat = secretKey.getEncoded(); 
                /**
                 * SecretKeySpec(byte[] key, String algorithm) : SecretKeySpec的构造方法之一，根据给定的字节数组构造一个密钥。
                 * key : 密钥的密钥内容。复制该数组的内容来防止后续修改。
                 * algorithm : 与给定的密钥内容相关联的密钥算法的名称。
                 */
                SecretKeySpec keySpec = new SecretKeySpec(enCodeFormat, "AES");  
                /**
                 * Cipher : 该类为加密和解密提供加密密码功能。
                 * getInstance(String transformation) : 通过指定转换模式的方式获得实例化对象。
                 * transformation : 转换的名称，例如:AES 或者  DES/CBC/PKCS5Padding。转换始终包括加密算法的名称（例如，DES），后面可能跟有一个反馈模式和填充方案。
                 */
//                Cipher cipher = Cipher.getInstance("AES");
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                byte[] byteContent = content.getBytes("utf-8");  
                /**
                 * init(int opmode, Key key) : 用密钥初始化此 cipher。为以下 4 种操作之一初始化该 cipher：加密、解密、密钥包装或密钥打开，这取决于 opmode 的值。
                 * opmode : 此 cipher 的操作模式（其为如下之一：ENCRYPT_MODE、DECRYPT_MODE、WRAP_MODE 或 UNWRAP_MODE）
                 * key : 密钥
                 */
                cipher.init(Cipher.ENCRYPT_MODE, keySpec);  
                /**
                 * doFinal(byte[] input) : 按单部分操作加密或解密数据，或者结束一个多部分操作。数据将被加密或解密（具体取决于此 Cipher 的初始化模式）。
                 * input : 输入的数组，即：要加密或解密的内容
                 */
                byte[] result = cipher.doFinal(byteContent); 
                /**
                 * parseByte2HexStr(String result) : 自定义的一套将二进制数据转换为十六进制的数据的方法； 
                 *
                 * ！注：在这儿，加密后的byte数组是不能强制转换成字符串的(即：new String（result）); 换言之,字符串和byte数组在这种情况下不是互逆的。
                 * 处理方式有两种：
                 *      1.将result转化为十六进制的数据再做处理（需要自己写一个转换方法）；
                 *      2.将result进行Base64(也可以用 Base64)再次加密在进行强制转换（不需要自己写方法，省事儿）。
                 */
                return parseByte2HexStr(result);
//                return AesUtil.parseByte2HexStr(result);
//              return new String(Base64.encode(result));
                
            } catch (Exception e) {  
                    e.printStackTrace();  
            }
            return null;  
    }  
    
    /**
     * 将二进制数组 ——> 十六进制数字
     * @param buf   要转换的数组
     * @return
     */
    private static String parseByte2HexStr(byte buf[]) {  
        StringBuffer sb = new StringBuffer();  
        for (int i = 0; i < buf.length; i++) {
            /**
             * Integer : 该类在对象中包装了一个基本类型 int 的值（即：Integer是int的包装类）。该类提供了多个方法，能在 int 类型和 String 类型之间互相转换，还提供了处理 int 类型时非常有用的其他一些常量和方法。
             * toHexString(int i) : 以十六进制的无符号整数形式返回一个整数参数的字符串表示形式。
             * i : 要转换成字符串的整数。
             * buf[i] & 0xFF : 将字节数组中每个字节拆解成2位16进制整数(原因是：每个字节(即：byte)占8位(即：bit)，16进制的基数是由4位组成)
             */
            String hex = Integer.toHexString(buf[i] & 0xFF);  
            if (hex.length() == 1) {  
                /**
                 * 因为toHexString(int i)将需要转换的i值转换为十六进制（基数 16）的无前导 0 的 ASCII 数字字符串，所以要重新加上
                 */
                hex = '0' + hex;  
            }  
            /**
             * toUpperCase() : 使用默认语言环境的规则将此 String 中的所有字符都转换为大写。
             */
            sb.append(hex.toUpperCase());  
        }  
        return sb.toString();  
    } 
}