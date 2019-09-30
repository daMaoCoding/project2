package dc.pay.business.xinzhangtuozhifu;


import java.util.Map;
import java.util.TreeMap;

/**
 * Created by xieyuxing on 2017/9/22.
 */

public class CmcPayTool {
	
	
	/**
	 * 私钥加密方法
	 * @param args
	 * @date 2018-5-7
	 */
	public static String getSignString(Map<String, String> params,String privateKey1) {
		TreeMap<String, String> param = new TreeMap<String, String>(params);
		String signInfo = "";
		String sign = "";// 生成签名
		for (String pkey : param.keySet()) {
			signInfo += pkey + "=" + param.get(pkey) + "&";
		}
		signInfo = signInfo.substring(0, signInfo.length() - 1);
		System.out.println("签名字段排列："+signInfo);
		
		try {
			sign = Base64.encode(RSAUtils.encryptByPrivateKey(signInfo.getBytes("UTF-8"), privateKey1));//
			System.out.println("私钥加密签名==="+sign);
			
//			String str=new String(RSAUtils.decryptByPublicKey(Base64.decode(sign), pub));
//			System.out.println("公钥解密=="+str);
			
			//生成数字签名
//			String qming=RSAUtils.sign(Base64.decode(sign), privateKey1);
//			System.out.println("数字签名："+qming);
			
//			//验签名
//			boolean boo=RSAUtils.verify(Base64.decode(sign), pub, qming);
//			System.out.println(boo);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sign;
	}
	
	
	
	
	
	
	
	
	
}