package dc.pay.business.yingfubaozhifu;

import net.sf.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import dc.pay.base.processor.PayException;
import dc.pay.business.jifudaifu.JiFuRSAUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


public class RSAUtils {


    /**
     * 加密算法RSA
     */
    public static final String KEY_ALGORITHM = "RSA";

    /**
     * 签名算法
     */
    public static final String SIGNATURE_ALGORITHM = "MD5withRSA";

    /**
     * 获取公钥的key
     */
    private static final String PUBLIC_KEY = "RSAPublicKey";

    /** */
    /**
     * 获取私钥的key
     */
    private static final String PRIVATE_KEY = "RSAPrivateKey";

    /**
     * RSA最大加密明文大小
     */
    private static final int MAX_ENCRYPT_BLOCK = 117;

    /**
     * RSA最大解密密文大小
     */
    private static final int MAX_DECRYPT_BLOCK = 128;

    /**
     * 编码
     */
    public static final String CHARSET_NAME = "UTF-8";


    /**
     * <p>
     * 用私钥对信息生成数字签名
     * </p>
     *
     * @param data       已加密数据
     * @param privateKey 私钥(BASE64编码)
     * @return
     * @throws Exception
     */
    public static String sign(String data, String privateKey) throws Exception {
        privateKey = privateKey.replaceAll("\n", "");
        return sign(data.getBytes(CHARSET_NAME), privateKey);
    }

    /**
     * <p>
     * 用私钥对信息生成数字签名
     * </p>
     *
     * @param data       已加密数据
     * @param privateKey 私钥(BASE64编码)
     * @return
     * @throws Exception
     */
    public static String sign(byte[] data, String privateKey) throws Exception {
        byte[] keyBytes = Base64.decode(privateKey);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM,"SunRsaSign");
        PrivateKey privateK = keyFactory.generatePrivate(pkcs8KeySpec);
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateK);
        signature.update(data);
        return Base64.encode(signature.sign());
    }

    /**
     * <P>
     * 私钥解密
     * </p>
     *
     * @param encryptedData 已加密数据
     * @param privateKey    私钥(BASE64编码)
     * @return
     * @throws Exception
     */
    public static String decryptByPrivateKey(String encryptedData, String privateKey) throws Exception {
        privateKey = privateKey.replaceAll("\n", "");
        return new String(decryptByPrivateKey(Base64.decode(encryptedData), privateKey), CHARSET_NAME);
    }

    /**
     * <p>
     * 公钥加密
     * </p>
     *
     * @param data      源数据
     * @param publicKey 公钥(BASE64编码)
     * @return
     * @throws Exception
     */
    public static String encryptByPublicKey(String data, String publicKey) throws Exception {
        publicKey = publicKey.replaceAll("\n", "");
        return Base64.encode(encryptByPublicKey(data.getBytes(CHARSET_NAME), publicKey));
    }

    /**
     * <p>
     * 公钥加密
     * </p>
     *
     * @param data      源数据
     * @param publicKey 公钥(BASE64编码)
     * @return
     * @throws Exception
     */
    public static byte[] encryptByPublicKey(byte[] data, String publicKey) throws Exception {
        byte[] keyBytes = Base64.decode(publicKey);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM,"SunRsaSign");
        Key publicK = keyFactory.generatePublic(x509KeySpec);
        // 对数据加密
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm(),"SunJCE");
        cipher.init(Cipher.ENCRYPT_MODE, publicK);
        int inputLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段加密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] encryptedData = out.toByteArray();
        out.close();
        return encryptedData;
    }

    /**
     * 参数按照参数名ASCII码从小到大排序（字典序拼）接成 json 字符串 生成签名
     *
     * @param context
     * @param rsaPrivateKey
     * @return
     * @throws Exception
     */
    public static String verify(Map context, String rsaPrivateKey) throws Exception {
        SortedMap signParams = new TreeMap();
        signParams.putAll(context);
        return RSAUtils.sign(JSONObject.fromObject(signParams).toString(), rsaPrivateKey);
    }

    /**
     * 签名和加密 私钥签名，公钥加密
     *
     * @param businessContext
     * @param businessHead
     * @param privateKey
     * @param publicKey
     * @return
     * @throws Exception
     */
    public static String verifyAndEncryptionToString(JSONObject businessContext, JSONObject businessHead, String privateKey, String publicKey) throws Exception {
        businessHead.put("sign",verify(businessContext,privateKey));
        JSONObject context = new JSONObject();
        context.put("businessContext",businessContext);
        context.put("businessHead",businessHead);
        return encryptByPublicKey(context.toString(),publicKey);

    }
    
	/**
	 * 加密解密共用核心代码，分段加密解密
	 */
	public static byte[] doFinal(byte[] decryptData, Cipher cipher,int blockSize)
			throws IllegalBlockSizeException, BadPaddingException, IOException {
		int offSet = 0;
		byte[] cache = null;
		int i = 0;
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		while (decryptData.length - offSet > 0) {
			if (decryptData.length - offSet > blockSize) {
				cache = cipher.doFinal(decryptData, offSet, blockSize);
			} else {
				cache = cipher.doFinal(decryptData, offSet, decryptData.length - offSet);
			}
			out.write(cache, 0, cache.length);
			i++;
			offSet = i * blockSize;
		}
		byte[] encryptedData = out.toByteArray();
		out.close();
		return encryptedData;
	}

	/**
	 * 私钥解密
	 */
	public static byte[] decryptByPrivateKey(byte[] data, String privateKeyValue) {
		byte[] key = Base64.decode(privateKeyValue);
		try {
			// 取得私钥
			PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(key);
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM,"SunRsaSign");
			// 生成私钥
			PrivateKey privateKey = keyFactory.generatePrivate(pkcs8KeySpec);
			// 数据解密
			Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm(),"SunJCE");
			cipher.init(Cipher.DECRYPT_MODE, privateKey);
			int blockSize = cipher.getOutputSize(data.length);
			return doFinal(data, cipher,blockSize);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
		return null;
	}
    
	public static boolean verify(Map<String,String> params,String sign,String PLATFORM_PUBLIC_KEY , String API_KEY) throws Exception {
		//生成签名字符串
		StringBuilder sb = new StringBuilder();
		//对key进行排序
		Object[] keys = params.keySet().toArray();
		Arrays.sort(keys);
		for(Object key : keys) {
			sb.append("&").append(key).append("=").append(params.get(key));
		}
		String str = JSONObject.fromObject(params).toString();
		//平台公钥
		PublicKey publicKey  = JiFuRSAUtils.getPublicKey(PLATFORM_PUBLIC_KEY);
        byte[] keyBytes = publicKey.getEncoded();
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey key = keyFactory.generatePublic(keySpec);
        Signature signature = Signature.getInstance("MD5withRSA");
        signature.initVerify(key);
        signature.update(str.getBytes());
        return signature.verify(Base64.decode(sign));
	}

	public static  Map<String,String> parseData(String data,String PRIVATE_KEY) throws PayException {
		byte[] result = decryptByPrivateKey(org.apache.shiro.codec.Base64.decode(data), PRIVATE_KEY);
		String resultData = null;
		try {
			resultData = new String(result, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			 throw new PayException("盈付宝支付,解析响应支付返回加密数据出错");
		}
		net.sf.json.JSONObject jsonObj =  net.sf.json.JSONObject.fromObject(resultData);
		net.sf.json.JSONObject businessHead =  net.sf.json.JSONObject.fromObject(jsonObj.getString("businessHead"));
		net.sf.json.JSONObject businessContext =  net.sf.json.JSONObject.fromObject(jsonObj.getString("businessContext"));
		Map<String, String> metaSignMap = new TreeMap<String, String>();
		metaSignMap.put("amount",  businessContext.getString("amount") );
		metaSignMap.put("orderNumber",  businessContext.getString("orderNumber") );
		metaSignMap.put("orderTime",  businessContext.getString("orderTime") );
		metaSignMap.put("payType",  businessContext.getString("payType") );
		metaSignMap.put("fee",  businessContext.getString("fee") );
		metaSignMap.put("orderStatus",  businessContext.getString("orderStatus") );
		metaSignMap.put("currency",  businessContext.getString("currency") );
		metaSignMap.put("payOrderNumber",  businessContext.getString("payOrderNumber") );
		metaSignMap.put("sign",  businessHead.getString("sign") );
		return metaSignMap;
	}


}
