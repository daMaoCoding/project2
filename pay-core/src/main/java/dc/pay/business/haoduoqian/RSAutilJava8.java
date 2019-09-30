package dc.pay.business.haoduoqian;

import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;



public class RSAutilJava8 {

	public static final String SIGN_ALGORITHMS = "SHA1WithRSA";
	private static final String KEY_ALGORITHM = "RSA";
	private static final int KEY_SIZE = 2048;
	private static final int DECRYPT_BLOCK_SIZE = KEY_SIZE / 8;
	public static String CHAR_SET = "UTF-8";

	private static final int MAX_ENCRYPT_BLOCK = 117;

	private static final int MAX_DECRYPT_BLOCK = 128;

	public static String encryptByPublicKey(String content, String publicKey) throws Exception {
		
		return Base64.getEncoder().encodeToString(encryptByPublicKey(content.getBytes("UTF-8"), publicKey));
	}
	//2.1 2.3
	public static byte[] encryptByPublicKey(byte[] data, String publicKey) throws Exception {
		byte[] keyBytes = Base64.getDecoder().decode(publicKey);
		X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		Key publicK = keyFactory.generatePublic(x509KeySpec);

		Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
		cipher.init(1, publicK);
		int inputLen = data.length;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int offSet = 0;

		int i = 0;

		while (inputLen - offSet > 0) {
			byte[] cache;
			if (inputLen - offSet > 117) {
				cache = cipher.doFinal(data, offSet, 117);
			} else {
				cache = cipher.doFinal(data, offSet, inputLen - offSet);
			}
			out.write(cache, 0, cache.length);
			i++;
			offSet = i * 117;
		}
		byte[] encryptedData = out.toByteArray();
		out.close();
		return encryptedData;
	}

	//2.1 2.3
	public static String sign(String content, String privateKey) {
		try {
			PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey));
			KeyFactory keyf = KeyFactory.getInstance("RSA");
			PrivateKey priKey = keyf.generatePrivate(priPKCS8);
			java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);
			signature.initSign(priKey);
			signature.update(content.getBytes());
			byte[] signed = signature.sign();
			return Base64.getEncoder().encodeToString(signed);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	//2.2
	public static boolean validateByPublicKey(String content, String sign, String publicKey) {
		
		
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			byte[] publicBytes = Base64.getDecoder().decode(publicKey);
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
			PublicKey pubKey = keyFactory.generatePublic(keySpec);

			java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);

			signature.initVerify(pubKey);
			signature.update(content.getBytes("UTF-8"));
			
			sign = sign.replaceAll("[\\s*\t\n\r]", "");
			
			boolean bverify = signature.verify(Base64.getDecoder().decode(sign.getBytes("UTF-8")));
			return bverify;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	//2.1 2.3 2.2
	public static String decryptByPrivateKey(String content, String privateKey) throws Exception {
		byte[] privateKeyBytes = Base64.getDecoder().decode(privateKey);
		
		content = content.replaceAll("[\\s*\t\n\r]", "");
		
		byte[] decryptBytes = Base64.getDecoder().decode(content);

		if (decryptBytes.length <= DECRYPT_BLOCK_SIZE) {
			return new String(decrypt(decryptBytes, privateKeyBytes), "UTF-8");
		} else {
			byte[] buffer = null;

			int index = ((decryptBytes.length - 1) / DECRYPT_BLOCK_SIZE) + 1;
			byte[] blockBytes = new byte[DECRYPT_BLOCK_SIZE];
			for (int i = 0; i < index; i++) {
				if (i == index - 1) {
					blockBytes = new byte[DECRYPT_BLOCK_SIZE];
				}
				int startIndex = i * DECRYPT_BLOCK_SIZE;
				int endIndex = startIndex + DECRYPT_BLOCK_SIZE;
				blockBytes = Arrays.copyOfRange(decryptBytes, startIndex,
						endIndex > decryptBytes.length ? decryptBytes.length : endIndex);
				if (buffer == null) {
					buffer = decrypt(blockBytes, privateKeyBytes);
				} else {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					baos.write(buffer);
					baos.write(decrypt(blockBytes, privateKeyBytes));
					buffer = baos.toByteArray();
					baos.close();
				}
			}
			return new String(buffer, "UTF-8");
		}
	}

	public static byte[] decrypt(byte[] decrypt, byte[] privateKeyBytes) throws Exception {
		PrivateKey privateKey = codeToPrivateKey(privateKeyBytes);

		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		byte[] resultBytes = cipher.doFinal(decrypt);
		return resultBytes;
	}

	public static PrivateKey codeToPrivateKey(byte[] privateKey) throws Exception {
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKey);
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
		PrivateKey keyPrivate = keyFactory.generatePrivate(keySpec);
		return keyPrivate;
	}

	public static boolean verify(String content, String sign, String public_key) throws Exception {
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		byte[] encodedKey = Base64.getDecoder().decode(public_key);
		PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));

		java.security.Signature signature = java.security.Signature.getInstance(SIGN_ALGORITHMS);

		signature.initVerify(pubKey);
		signature.update(content.getBytes(CHAR_SET));

		boolean bverify = signature.verify(Base64.getDecoder().decode(sign));
		return bverify;

	}

}
