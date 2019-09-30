package dc.pay.business.anyifu;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

/** */
/**
 * <p>
 * RSA公钥/私钥/签名工具包
 * </p>
 * <p>
 * 罗纳德·李维斯特（Ron [R]ivest）、阿迪·萨莫尔（Adi [S]hamir）和伦纳德·阿德曼（Leonard [A]dleman）
 * </p>
 * <p>
 * 字符串格式的密钥在未在特殊说明情况下都为BASE64编码格式<br/>
 * 由于非对称加密速度极其缓慢，一般文件不使用它来加密而是使用对称加密，<br/>
 * 非对称加密算法可以用来对对称加密的密钥加密，这样保证密钥的安全也就保证了数据的安全
 * </p>
 * 
 * @author IceWee
 * @date 2012-4-26
 * @version 1.0
 */
public class RSAUtils {


	public static final String ENCODING = "UTF-8";

	/***
	 * deviceType 是安卓
	 */
	public static final String ANDROID = "android";

	/** */
	/**
	 * 加密算法RSA
	 */
	public static final String KEY_ALGORITHM = "RSA";

	/** */
	/**
	 * 签名算法
	 */
	public static final String SIGNATURE_ALGORITHM = "MD5withRSA";

	/** */
	/**
	 * 获取公钥的key
	 */
	private static final String PUBLIC_KEY = "RSAPublicKey";

	/**
	 * 获取公钥的key对应的Encoded值
	 */
	public static final String PUBLIC_KEY_VALUE = "publicEncoded";

	/** */
	/**
	 * 获取私钥的key
	 */
	private static final String PRIVATE_KEY = "RSAPrivateKey";

	/**
	 * 获取私钥的key对应的Encoded值
	 */
	public static final String PRIVATE_KEY_VALUE = "privateEncoded";

	/** */
	/**
	 * RSA最大加密明文大小
	 */
	private static final int MAX_ENCRYPT_BLOCK = 116;

	/** */
	/**
	 * RSA最大解密密文大小
	 */
	private static final int MAX_DECRYPT_BLOCK = 128;

	/**
	 * 获取RSA私钥串
	 * 
	 * @param in
	 *            RSA私钥证书文件流
	 * @param fileSuffix
	 *            RSA私钥名称，决定编码类型|PFX、JKS、PEM...
	 * @param password
	 *            RSA私钥保护密钥|口令
	 * @param keyAlgorithm
	 *            密钥算法
	 * @return RSA私钥对象
	 * @throws ServiceException
	 */
	public static String getRSAPrivateKeyByFileSuffix(InputStream in, String fileSuffix, String password, String keyAlgorithm)
			throws Exception {
		String keyType = "";
		if ("keystore".equalsIgnoreCase(fileSuffix)) {
			keyType = "JKS";
		} else if ("pfx".equalsIgnoreCase(fileSuffix) || "p12".equalsIgnoreCase(fileSuffix)) {
			keyType = "PKCS12";
		} else if ("jck".equalsIgnoreCase(fileSuffix)) {
			keyType = "JCEKS";
		} else if ("pem".equalsIgnoreCase(fileSuffix) || "pkcs8".equalsIgnoreCase(fileSuffix)) {
			keyType = "PKCS8";
		} /*
		 * else if ("pkcs1".equalsIgnoreCase(fileSuffix)) { keyType = "PKCS1"; }
		 */else {
			keyType = "JKS";
		}

		try {
			PrivateKey priKey = null;
			if ("JKS".equals(keyType) || "PKCS12".equals(keyType) || "JCEKS".equals(keyType)) {
				KeyStore ks = KeyStore.getInstance(keyType);
				if (password != null) {
					char[] cPasswd = password.toCharArray();
					ks.load(in, cPasswd);
					Enumeration<String> aliasenum = ks.aliases();
					String keyAlias = null;
					while (aliasenum.hasMoreElements()) {
						keyAlias = (String) aliasenum.nextElement();
						priKey = (PrivateKey) ks.getKey(keyAlias, cPasswd);
						if (priKey != null)
							break;
					}
				}
			} else {
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				StringBuilder sb = new StringBuilder();
				String readLine = null;
				while ((readLine = br.readLine()) != null) {
					if (readLine.charAt(0) == '-') {
						continue;
					} else {
						sb.append(readLine);
						sb.append('\r');
					}
				}
				if ("PKCS8".equals(keyType)) {
					// org.apache.commons.codec.binary.Base64
					PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(Base64.decode(sb.toString()));
					KeyFactory keyFactory = KeyFactory.getInstance(keyAlgorithm);
					priKey = keyFactory.generatePrivate(priPKCS8);
				} /*
				 * else if ("PKCS1".equals(keyType)) { RSAPrivateKeyStructure
				 * asn1PrivKey = new RSAPrivateKeyStructure((ASN1Sequence)
				 * ASN1Sequence.fromByteArray(sb.toString().getBytes()));
				 * KeySpec rsaPrivKeySpec = new
				 * RSAPrivateKeySpec(asn1PrivKey.getModulus(),
				 * asn1PrivKey.getPrivateExponent()); KeyFactory keyFactory =
				 * KeyFactory.getInstance(keyAlgorithm); priKey =
				 * keyFactory.generatePrivate(rsaPrivKeySpec); }
				 */
			}

			return Base64.encode(priKey.getEncoded());
		} catch (FileNotFoundException e) {
			throw new FileNotFoundException("私钥路径文件不存在");
		} catch (KeyStoreException e) {
			throw new KeyStoreException("获取KeyStore对象异常");
		} catch (IOException e) {
			throw new IOException("读取私钥异常");
		} catch (NoSuchAlgorithmException e) {
			throw new NoSuchAlgorithmException("生成私钥对象异常");
		} catch (CertificateException e) {
			throw new CertificateException("加载私钥密码异常");
		} catch (UnrecoverableKeyException e) {
			throw new UnrecoverableKeyException("生成私钥对象异常");
		} catch (InvalidKeySpecException e) {
			throw new InvalidKeySpecException("生成私钥对象异常");
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
			}
		}
	}

	/** */
	/**
	 * <p>
	 * 生成密钥对(公钥和私钥)
	 * </p>
	 * 
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Object> genKeyPair() throws Exception {
		KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
		keyPairGen.initialize(1024);
		KeyPair keyPair = keyPairGen.generateKeyPair();
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
		RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
		Map<String, Object> keyMap = new HashMap<String, Object>(2);
		keyMap.put(PUBLIC_KEY, publicKey);
		keyMap.put(PRIVATE_KEY, privateKey);

		keyMap.put(PUBLIC_KEY_VALUE, Base64.encode(publicKey.getEncoded()));
		keyMap.put(PRIVATE_KEY_VALUE, Base64.encode(privateKey.getEncoded()));
		return keyMap;
	}

	/**
	 * <p>
	 * 用私钥对信息生成数字签名
	 * </p>
	 * 
	 * @param data
	 *            已加密数据
	 * @param privateKey
	 *            私钥(BASE64编码)
	 * @return
	 * @throws Exception
	 */
	public static String sign(byte[] data, String privateKey) throws Exception {
		byte[] keyBytes = Base64.decode(privateKey);
		PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
		PrivateKey privateK = keyFactory.generatePrivate(pkcs8KeySpec);
		Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
		signature.initSign(privateK);
		signature.update(data);
		return Base64.encode(signature.sign());
	}

	public static String sign(String data, String privateKey) throws Exception {

		byte[] keyBytes = Base64.decode(privateKey);
		PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
		PrivateKey privateK = keyFactory.generatePrivate(pkcs8KeySpec);
		Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
		signature.initSign(privateK);
		signature.update(data.toString().getBytes("UTF-8"));

		String signValue = Base64.encode(signature.sign());

		return signValue;
	}

	/** */
	/**
	 * <p>
	 * 校验数字签名
	 * </p>
	 * 
	 * @param data
	 *            已加密数据
	 * @param publicKey
	 *            公钥(BASE64编码)
	 * @param sign
	 *            数字签名
	 * @param keyType
	 *            编码格式
	 * @return
	 * @throws Exception
	 */
	public static boolean verify(byte[] data, String publicKey, String sign, String keyType) throws Exception {
		if ("PKCS12".equals(keyType)) {
			return verify(data, publicKey, sign);
		} else if ("X.509".equals(keyType)) {
			byte[] keyBytes = Base64.decode(publicKey);
			CertificateFactory factory = CertificateFactory.getInstance(keyType);
			Certificate cert = factory.generateCertificate(new ByteArrayInputStream(keyBytes));
			PublicKey pubKey = cert.getPublicKey();
			Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
			signature.initVerify(pubKey);
			signature.update(data);
			return signature.verify(Base64.decode(sign));
		}
		throw new Exception("==>校验数字签名：未知的证书公钥编码格式！");
	}

	/** */
	/**
	 * <p>
	 * 校验数字签名
	 * </p>
	 * 
	 * @param data
	 *            已加密数据
	 * @param publicKey
	 *            公钥(BASE64编码)
	 * @param sign
	 *            数字签名
	 * @return
	 * @throws Exception
	 */
	public static boolean verify(byte[] data, String publicKey, String sign) throws Exception {
		byte[] keyBytes = Base64.decode(publicKey);
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
		PublicKey publicK = keyFactory.generatePublic(keySpec);
		Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
		signature.initVerify(publicK);
		signature.update(data);
		return signature.verify(Base64.decode(sign));
	}

	public static boolean verify(String data, String publicKey, String sign) throws Exception {

		byte[] keyBytes = Base64.decode(publicKey);
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
		PublicKey publicK = keyFactory.generatePublic(keySpec);
		Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
		signature.initVerify(publicK);
		signature.update(data.toString().getBytes("UTF-8"));

		boolean signFlag = signature.verify(Base64.decode(sign));

		return signFlag;
	}

	/** */
	/**
	 * <P>
	 * 私钥解密
	 * </p>
	 * 
	 * @param encryptedData
	 *            已加密数据
	 * @param privateKey
	 *            私钥(BASE64编码)
	 * @return
	 * @throws Exception
	 */
	public static byte[] decryptByPrivateKey(byte[] encryptedData, String privateKey) throws Exception {
		byte[] keyBytes = Base64.decode(privateKey);
		PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
		Key privateK = keyFactory.generatePrivate(pkcs8KeySpec);
		Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
		cipher.init(Cipher.DECRYPT_MODE, privateK);
		int inputLen = encryptedData.length;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int offSet = 0;
		byte[] cache;
		int i = 0;
		// 对数据分段解密
		while (inputLen - offSet > 0) {
			if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
				cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
			} else {
				cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);
			}
			out.write(cache, 0, cache.length);
			i++;
			offSet = i * MAX_DECRYPT_BLOCK;
		}
		byte[] decryptedData = out.toByteArray();
		out.close();
		return decryptedData;
	}

	/** */
	/**
	 * <p>
	 * 公钥解密
	 * </p>
	 * 
	 * @param encryptedData
	 *            已加密数据
	 * @param publicKey
	 *            公钥(BASE64编码)
	 * @return
	 * @throws Exception
	 */
	public static byte[] decryptByPublicKey(byte[] encryptedData, String publicKey) throws Exception {
		byte[] keyBytes = Base64.decode(publicKey);
		X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
		Key publicK = keyFactory.generatePublic(x509KeySpec);
		Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
		cipher.init(Cipher.DECRYPT_MODE, publicK);
		int inputLen = encryptedData.length;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int offSet = 0;
		byte[] cache;
		int i = 0;
		// 对数据分段解密
		while (inputLen - offSet > 0) {
			if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
				cache = cipher.doFinal(encryptedData, offSet, MAX_DECRYPT_BLOCK);
			} else {
				cache = cipher.doFinal(encryptedData, offSet, inputLen - offSet);
			}
			out.write(cache, 0, cache.length);
			i++;
			offSet = i * MAX_DECRYPT_BLOCK;
		}
		byte[] decryptedData = out.toByteArray();
		out.close();
		return decryptedData;
	}

	/** */
	/**
	 * <p>
	 * 公钥加密
	 * </p>
	 * 
	 * @param data
	 *            源数据
	 * @param publicKey
	 *            公钥(BASE64编码)
	 * @param deviceType
	 *            类型(IOS还是android)
	 * @return
	 * @throws Exception
	 */
	public static byte[] encryptByPublicKey(byte[] data, String publicKey, String deviceType) throws Exception {
		byte[] keyBytes = Base64.decode(publicKey);
		X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
		Key publicK = keyFactory.generatePublic(x509KeySpec);
		// 对数据加密
		Cipher cipher = null;
		if (deviceType != null && !deviceType.equals("") && deviceType.toLowerCase().equals(ANDROID)) {
			// 如果是安卓机
			cipher = Cipher.getInstance("RSA/ECB/NoPadding");
		} else {
			cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		}
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

	/** */
	/**
	 * <p>
	 * 私钥加密
	 * </p>
	 * 
	 * @param data
	 *            源数据
	 * @param privateKey
	 *            私钥(BASE64编码)
	 * @return
	 * @throws Exception
	 */
	public static byte[] encryptByPrivateKey(byte[] data, String privateKey) throws Exception {
		byte[] keyBytes = Base64.decode(privateKey);
		// 实例化PKCS8EncodedKeySpec对象
		PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
		// 实例化KeyFactory对象，并指定RSA算法
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
		Key privateK = keyFactory.generatePrivate(pkcs8KeySpec);
		Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
		cipher.init(Cipher.ENCRYPT_MODE, privateK);
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

	/** */
	/**
	 * <p>
	 * 获取私钥
	 * </p>
	 * 
	 * @param keyMap
	 *            密钥对
	 * @return
	 * @throws Exception
	 */
	public static String getPrivateKey(Map<String, Object> keyMap) throws Exception {
		Key key = (Key) keyMap.get(PRIVATE_KEY);
		return Base64.encode(key.getEncoded());
	}

	/** */
	/**
	 * <p>
	 * 获取公钥
	 * </p>
	 * 
	 * @param keyMap
	 *            密钥对
	 * @return
	 * @throws Exception
	 */
	public static String getPublicKey(Map<String, Object> keyMap) throws Exception {
		Key key = (Key) keyMap.get(PUBLIC_KEY);
		return Base64.encode(key.getEncoded());
	}

	public static void main(String[] args) throws Exception {
		Map<String, Object> keyMap = RSAUtils.genKeyPair();
		String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDLRpUS1NYTg0a1MIg3f1frb0p2yCMdKdLqgnHGGYtoloDoN31b+KUKQZ7lZqrTV4bSovWK9mAw2QgBc98A3NDl2dEjUIPFXOoKAH4AdHrYjW9VsBMycjG8epMzn48l0jzXcwi8Tv1PBkH5nqr7PaTsT5CLfTVf9YW88IlsEbP5KwIDAQAB";
		String privateKey = "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAMtGlRLU1hODRrUwiDd/V+tvSnbIIx0p0uqCccYZi2iWgOg3fVv4pQpBnuVmqtNXhtKi9Yr2YDDZCAFz3wDc0OXZ0SNQg8Vc6goAfgB0etiNb1WwEzJyMbx6kzOfjyXSPNdzCLxO/U8GQfmeqvs9pOxPkIt9NV/1hbzwiWwRs/krAgMBAAECgYBwB7NrEH/3exdCDsBUDcvPxqM8earYjM9gTYoUyslJWAEtej+Nq6iR0X6YIZAuMAPvHL7E2mpDIanG2yxhVlGODz9DoaNNMEZUA+RRkyBYMRxLfgO7ViNyBneEaWQsShgzCyPezdeUuI6qWBhESU5hT333UFhfd5ZlzQAMf5SAgQJBAP01CEQmhYJGDtDoOFS4MTybQOFlk6G2M0u3jhxEFdJgY4185xnjNEFu57kDPGj6JKn7cPb7fcGlm6F3nvwcSsECQQDNhI+tQyAeUTHsKFg4d/YZZGkbJUd7GxtWuOzCRv1Fm4khAzJnyIOmwXxvCd8nUaXcWNTSZH7/IxPeKB+v09rrAkAYhKwaPUisRrBkljfuLC/IWJg9uyJChGwPJuUB463hQyggqTmPjiqfM2gIyEFvQNmQBCL6J3wT5j9dsUGZ0/uBAkBZNDagv8gLILcIiCJysC8Tqm+spqu2FXfyVmX9lY6NTgthVt/kCDaMhOMb1y8TA+94Ct6lS5WL7I/NF6FZUh1tAkAq2ZYNb0BM97lP+GOo71HHqYAM4r8jdnSMEcqJ9oNSJc4EF8FcU/QaIhco9Si7TpW+UG+NumKxQmGtQj5InIb2";

		// System.out.println(new
		// String(RSAUtils.encryptByPublicKey("admin".getBytes(), publicKey,
		// null),"utf-8"));

		String pwd = "eacbe43be6ce943172e0d29d844ef3cf9df0473d4eb06b0d175378be38b2be394d94370b83d34c311a7085a1f345e85991b37f6df96d8a57f6345555385b5378c12113d57258f32f49b526444c1c26745075f2dc31fa7b00b8c72f947f8fd83d3fa6641e547c2e5e8d4412e9";

		String pwd1 = new String(RSAUtils.decryptByPrivateKey(pwd.getBytes("utf-8"), privateKey));

		System.out.println(pwd1);

	}
}
