package dc.pay.business.tiantiandaifu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * 
 * @Description 下游可直接调用本工具类中的方法，工具类进行加密、请求、解密操作
 *
 */
public class ApiUtil {
    
    /**
     * 
     * @Description 支付、代付、查询接口调用此方法
     * @param  requestParam公共请求参数（合作编号、交易服务码、签名类型等参数，具体可查看api文档）  apiParam接口参数（商户号、金额、订单号等参数，具体可查看api文档对应业务）  publicKey公钥字符串   url请求地址
     * @return 公共返回参数（返回代码、返回消息、签名字符串等参数，具体可查看api文档），参数中有apiParam（map形式，订单号、平台订单号等参数，具体可查看api文档对应业务）
     * @throws IOException 
     */
    public static Map<String,Object> transaction(Map<String,String> requestParam,Map<String,String> apiParam,String publicKey,String url) throws IOException{
        
        StringBuffer sb=new StringBuffer("");
        for(Entry<String, String> entry : apiParam.entrySet()){
            String mapKey = entry.getKey();
            Object mapValue = entry.getValue();
            sb.append(mapKey).append("=").append(mapValue).append("&");
        }
        String paramStr=sb.substring(0, sb.length()-1);
//        System.out.println("加密前数据：" + paramStr);
        String sign = base64Encode(encrypt(publicKey, paramStr.getBytes("UTF-8")));
//        System.out.println("加密后数据：" + sign);
        requestParam.put("sign", sign);
        StringBuffer requestStr=new StringBuffer("");
        for(Entry<String, String> entry : requestParam.entrySet()){
            String mapKey = entry.getKey();
            Object mapValue = entry.getValue();
            requestStr.append(mapKey).append("=").append(mapValue).append("&");
        }
//        System.out.println("请     求的数据："+requestStr);
        String responseStr = post(url, requestStr.toString());
//        System.out.println("响应的数据："+responseStr);
        Map<String, Object> responseMap = new Gson().fromJson(responseStr, new TypeToken<Map<String, Object>>() {
        }.getType());
        String responseSign = (String) responseMap.get("pl_sign");
//        System.out.println("解密前数据：" + responseSign);
        String responseSignStr = new String(verify(publicKey, base64Decode(responseSign)), "UTF-8");
//        System.out.println("解密后数据：" + responseSignStr);
        String [] apiStr=responseSignStr.split("&");
        Map<String,Object> responseApiParam=new HashMap<String,Object>();
        for(String apiParamStr:apiStr){
            String [] paramKeyValues=apiParamStr.split("=");
            responseApiParam.put(paramKeyValues[0], paramKeyValues.length == 1 ? "" : apiParamStr.replace(paramKeyValues[0] + "=", ""));
        }
        responseMap.put("apiParam", responseApiParam);
        return responseMap;
    }
    
    public static String transaction1(Map<String,String> requestParam,Map<String,String> apiParam,String publicKey,String url) throws IOException{
        
        StringBuffer sb=new StringBuffer("");
        for(Entry<String, String> entry : apiParam.entrySet()){
            String mapKey = entry.getKey();
            Object mapValue = entry.getValue();
            sb.append(mapKey).append("=").append(mapValue).append("&");
        }
        String paramStr=sb.substring(0, sb.length()-1);
//        System.out.println("加密前数据：" + paramStr);
        String sign = base64Encode(encrypt(publicKey, paramStr.getBytes("UTF-8")));
//        System.out.println("加密后数据：" + sign);
        requestParam.put("sign", sign);
        StringBuffer requestStr=new StringBuffer("");
        for(Entry<String, String> entry : requestParam.entrySet()){
            String mapKey = entry.getKey();
            Object mapValue = entry.getValue();
            requestStr.append(mapKey).append("=").append(mapValue).append("&");
        }
//        System.out.println("请求的数据："+requestStr);
        String responseStr = post(url, requestStr.toString());
//        System.out.println("响应的数据："+responseStr);
        Map<String, Object> responseMap = new Gson().fromJson(responseStr, new TypeToken<Map<String, Object>>() {
        }.getType());
        String responseSign = (String) responseMap.get("pl_sign");
//        System.out.println("解密前数据：" + responseSign);
        String responseSignStr = new String(verify(publicKey, base64Decode(responseSign)), "UTF-8");
//        System.out.println("解密后数据：" + responseSignStr);
        return responseSignStr;
    }
    
    public static String transaction2(Map<String,String> requestParam,Map<String,String> apiParam,String publicKey,String url) throws IOException{
        
        StringBuffer sb=new StringBuffer("");
        for(Entry<String, String> entry : apiParam.entrySet()){
            String mapKey = entry.getKey();
            Object mapValue = entry.getValue();
            sb.append(mapKey).append("=").append(mapValue).append("&");
        }
        String paramStr=sb.substring(0, sb.length()-1);
//        System.out.println("加密前数据：" + paramStr);
        String sign = base64Encode(encrypt(publicKey, paramStr.getBytes("UTF-8")));
//        System.out.println("加密后数据：" + sign);
        requestParam.put("sign", sign);
        StringBuffer requestStr=new StringBuffer("");
        for(Entry<String, String> entry : requestParam.entrySet()){
            String mapKey = entry.getKey();
            Object mapValue = entry.getValue();
            requestStr.append(mapKey).append("=").append(mapValue).append("&");
        }
//        System.out.println("请求的数据："+requestStr);
        String responseStr = post(url, requestStr.toString());
//        System.out.println("响应的数据："+responseStr);
//        Map<String, Object> responseMap = new Gson().fromJson(responseStr, new TypeToken<Map<String, Object>>() {
//        }.getType());
//        String responseSign = (String) responseMap.get("pl_sign");
//        System.out.println("解密前数据：" + responseSign);
//        String responseSignStr = new String(verify(publicKey, base64Decode(responseSign)), "UTF-8");
//        System.out.println("解密后数据：" + responseSignStr);
        return responseStr;
    }
    
    /**
     * 
     * @Description 回调的解密方法
     * @param plSign异步通知中的pl_sign参数值    publicKey公钥字符串
     * @return pl_sign解密后的参数与值
     * @throws IOException 
     *
     */
    public static Map<String,Object> decrypt(String plSign,String publicKey) throws IOException{
//        System.out.println("签名字符串：" + plSign);
        plSign=URLDecoder.decode(plSign, "UTF-8");
        byte[] b = null;
        String sign = plSign.replace(" ","+");// url传输过程中+号变成空格，再把空格转成+号
        b = verify(publicKey, base64Decode(sign));
        String responseSignStr = new String(b);
//        System.out.println("解密结果：" + responseSignStr);
        String [] apiStr=responseSignStr.split("&");
        Map<String,Object> signParam=new HashMap<String,Object>();
        for(String apiParamStr:apiStr){
            String [] paramKeyValues=apiParamStr.split("=");
            signParam.put(paramKeyValues[0], paramKeyValues.length == 1 ? "" : apiParamStr.replace(paramKeyValues[0] + "=", ""));
        }
        return signParam;
        
    }
    
    
    
    public static String post(String url, String request) {
        OutputStream oos = null;
        InputStream iis = null;
        String response = null;
        try {
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);
            httpURLConnection.setAllowUserInteraction(true);

            oos = httpURLConnection.getOutputStream();
            oos.write(request.toString().getBytes("UTF-8"));
            oos.flush();

            iis = httpURLConnection.getInputStream();
            response = readInputStream(iis, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } // 关闭OutputStream[END]
            if (iis != null) {
                try {
                    iis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } // 关闭InputStream[END]
        }
        return response;
    }
    
    public static String readInputStream(InputStream inputStream, String encoding)
    {
        if (inputStream == null)
        {
            return null;
        }

        String str = null;
        try
        {
            int length = 0;
            byte[] bytes = new byte[1024];
            StringBuffer buffer = new StringBuffer();
            while ((length = inputStream.read(bytes, 0, bytes.length)) != -1)
            {
                String read = new String(bytes, 0, length, "ISO_8859_1");
                buffer.append(read);
            }
            byte[] allBytes = buffer.toString().getBytes("ISO_8859_1");
            str = new String(allBytes, encoding);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                inputStream.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return str;
    }
    
    
    private static final Integer KEY_SIZE = 1024;

    private static final Integer MAX_ENCRYPT_SIZE = 117;

    private static final Integer MAX_DECRYPT_SIZE = 128;

    public static void generateKeyPair(String path) throws IOException, NoSuchProviderException
    {
        KeyPairGenerator keyPairGenerator = null;
        try
        {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA","SunJCE");
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }

        keyPairGenerator.initialize(KEY_SIZE);

        KeyPair keyPair = keyPairGenerator.genKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        byte[] publicKeyBytes = publicKey.getEncoded();
        byte[] privateKeyBytes = privateKey.getEncoded();

        String publicKeyString = base64Encode(publicKeyBytes);
        String privateKeyString = base64Encode(privateKeyBytes);

        byte[] publicKeyBase64Bytes = publicKeyString.getBytes();
        byte[] privateKeyBase64Bytes = privateKeyString.getBytes();

        File publicKeyFile = new File(path + "public.key");
        File privateKeyFile = new File(path + "private.key");

        FileOutputStream publicKeyFos = new FileOutputStream(publicKeyFile);
        FileOutputStream privateKeyFos = new FileOutputStream(privateKeyFile);

        publicKeyFos.write(publicKeyBase64Bytes);
        privateKeyFos.write(privateKeyBase64Bytes);

        publicKeyFos.flush();
        publicKeyFos.close();
        privateKeyFos.flush();
        privateKeyFos.close();

    }

    /**
     * encrypt<br>
     * result use BASE64 encode
     * 
     * @param publicKeyStr
     * @param needEncrypt
     * @return
     */
    public static byte[] encrypt(String publicKeyStr, byte[] needEncrypt)
    {
        byte[] encrypt = null;
        try
        {
            PublicKey publicKey = getPublicKey(publicKeyStr);
            encrypt = encrypt(publicKey, needEncrypt);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return encrypt;
    }

    /**
     * 
     * decrypt<br>
     * input was BASE64 encode
     * 
     * @param privateKeyStr
     * @param needDecrypt
     * @return
     */
    public static byte[] decrypt(String privateKeyStr, byte[] needDecrypt)
    {
        byte[] decrypt = null;

        try
        {
            PrivateKey privateKey = getPrivateKey(privateKeyStr);
            decrypt = decrypt(privateKey, needDecrypt);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return decrypt;
    }

    /**
     * sign
     * 
     * @param privateKeyStr
     * @param needSign
     * @return
     */
    public static byte[] sign(String privateKeyStr, byte[] needSign)
    {
        byte[] encrypt = null;
        try
        {
            PrivateKey privateKey = getPrivateKey(privateKeyStr);
            encrypt = encrypt(privateKey, needSign);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return encrypt;
    }

    /**
     * verify
     * 
     * @param publicKeyStr
     * @param needVerify
     * @return
     */
    public static byte[] verify(String publicKeyStr, byte[] needVerify)
    {
        byte[] decrypt = null;
        try
        {
            PublicKey publicKey = getPublicKey(publicKeyStr);
            decrypt = decrypt(publicKey, needVerify);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return decrypt;
    }

    private static byte[] encrypt(Key key, byte[] needEncryptBytes)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, IOException, NoSuchProviderException
    {
        if (needEncryptBytes == null)
        {
            return null;
        }

        Cipher cipher = Cipher.getInstance("RSA","SunJCE");

        // encrypt
        cipher.init(Cipher.ENCRYPT_MODE, key);

        ByteArrayInputStream iis = new ByteArrayInputStream(needEncryptBytes);
        ByteArrayOutputStream oos = new ByteArrayOutputStream();
        int restLength = needEncryptBytes.length;
        while (restLength > 0)
        {
            int readLength = restLength < MAX_ENCRYPT_SIZE ? restLength : MAX_ENCRYPT_SIZE;
            restLength = restLength - readLength;

            byte[] readBytes = new byte[readLength];
            iis.read(readBytes);

            byte[] append = cipher.doFinal(readBytes);
            oos.write(append);
        }
        byte[] encryptedBytes = oos.toByteArray();

        return encryptedBytes;
    }

    private static byte[] decrypt(Key key, byte[] needDecryptBytes) throws IOException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException
    {
        if (needDecryptBytes == null)
        {
            return null;
        }
        Cipher cipher = Cipher.getInstance("RSA","SunJCE");

        // decrypt
        cipher.init(Cipher.DECRYPT_MODE, key);

        ByteArrayInputStream iis = new ByteArrayInputStream(needDecryptBytes);
        ByteArrayOutputStream oos = new ByteArrayOutputStream();
        int restLength = needDecryptBytes.length;
        while (restLength > 0)
        {
            int readLength = restLength < MAX_DECRYPT_SIZE ? restLength : MAX_DECRYPT_SIZE;
            restLength = restLength - readLength;

            byte[] readBytes = new byte[readLength];
            iis.read(readBytes);

            byte[] append = cipher.doFinal(readBytes);
            oos.write(append);
        }
        byte[] decryptedBytes = oos.toByteArray();

        return decryptedBytes;
    }

    private static PublicKey getPublicKey(String publicKeyStr)
            throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException
    {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA","SunRsaSign");

        byte[] publicKeyBytes = base64Decode(publicKeyStr);
        KeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        return publicKey;
    }

    private static PrivateKey getPrivateKey(String privateKeyStr)
            throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException
    {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA","SunRsaSign");;

        byte[] privateKeyBytes = base64Decode(privateKeyStr);
        KeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        return privateKey;
    }

    /**
     * BASE64 encode
     * 
     * @param needEncode
     * @return
     */
    static String base64Encode(byte[] needEncode)
    {
        String encoded = null;
        if (needEncode != null)
        {
            encoded = new BASE64Encoder().encode(needEncode);
        }
        return encoded;
    }

    /**
     * BASE64 decode
     * 
     * @param needDecode
     * @return
     * @throws IOException
     */
    static byte[] base64Decode(String needDecode) throws IOException
    {
        byte[] decoded = null;
        if (needDecode != null)
        {
            decoded = new BASE64Decoder().decodeBuffer(needDecode);
        }
        return decoded;
    }

}
