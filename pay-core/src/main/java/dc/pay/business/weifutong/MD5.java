package dc.pay.business.weifutong;

import java.io.UnsupportedEncodingException;
import java.security.SignatureException;
import org.apache.commons.codec.digest.DigestUtils;

/** 
* 功能：MD5签名
* */
public class MD5 {
    public static void main(String[] args) {
        
        String str1="\"amount\"";
        String str2="\"0.01\"";
        String str3="\"fundChannel\"";
        String str4="\"ALIPAYACCOUNT\"";
        String str = "buyer_logon_id=447***@163.com&buyer_user_id=2088202766951165&charset=UTF-8&fee_type=CNY&fund_bill_list=[{"+str1+":"+str2+","+str3+":"+str4+"}]&mch_id=102510298268&nonce_str=1490337600981&openid=2088202766951165&out_trade_no=B201703240000011251&out_transaction_id=2017032421001004160261346304&pay_result=0&result_code=0&sign_type=MD5&status=0&time_end=20170324144000&total_fee=1&trade_type=pay.alipay.native&transaction_id=102510298268201703241129061786&version=2.0";
        //String str = "buyer_logon_id=447***@163.com&buyer_user_id=2088202766951165&charset=UTF-8&fee_type=CNY&mch_id=102510298268&nonce_str=1490337989432&openid=2088202766951165&out_trade_no=B201703240000011251&out_transaction_id=2017032421001004160261346304&pay_result=0&result_code=0&sign_type=MD5&status=0&time_end=20170324144000&total_fee=1&trade_type=pay.alipay.native&transaction_id=102510298268201703241129061786&version=2.0";
        System.out.println(MD5.sign(str, "&key=c6a58c32aef5a19c6233d93bf43ed0f5", "utf-8"));
    	
		//System.out.println(str1);
    }

    /**
     * 签名字符串
     * @param text 需要签名的字符串
     * @param key 密钥
     * @param input_charset 编码格式
     * @return 签名结果
     */
    public static String sign(String text, String key, String input_charset) {
    	text = text + key;
    	System.out.println(text);
        return DigestUtils.md5Hex(getContentBytes(text, input_charset));
    }
    
    /**
     * 签名字符串
     * @param text 需要签名的字符串
     * @param sign 签名结果
     * @param key 密钥
     * @param input_charset 编码格式
     * @return 签名结果
     */
    public static boolean verify(String text, String sign, String key, String input_charset) {
    	text = text + key;
    	String mysign = DigestUtils.md5Hex(getContentBytes(text, input_charset));
    	if(mysign.equals(sign)) {
    		return true;
    	}
    	else {
    		return false;
    	}
    }

    /**
     * @param content
     * @param charset
     * @return
     * @throws SignatureException
     * @throws UnsupportedEncodingException 
     */
    private static byte[] getContentBytes(String content, String charset) {
        if (charset == null || "".equals(charset)) {
            return content.getBytes();
        }
        try {
            return content.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("MD5签名过程中出现错误,指定的编码集不对,您目前指定的编码集是:" + charset);
        }
    }

}