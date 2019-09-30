package dc.pay.business.dianfuzhifu;

import dc.pay.business.huilianfuzhifu.HexUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @ClassName: Md5Util
 * @Description: md5加密
 * @date 2017年10月25日 上午9:28:36
 */
public class Md5Util {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Md5Util.class);

    public static String sign(String text, String key, String inputCharset) {
        text = text + key;
        String mysign = DigestUtils.md5Hex(getContentBytes(text, inputCharset));
        return mysign;
    }


    public static byte[] getContentBytes(String content, String charset) {
        if (charset == null || "".equals(charset)) {
            return content.getBytes();
        }
        try {
            return content.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("MD5签名过程中出现错误,指定的编码集不对,您目前指定的编码集是:" + charset);
        }
    }


    public static String MD5(String content) {
        if (isNotEmpty(content)) {
            try {
                return HexUtil.byte2hex(MessageDigest.getInstance("md5").digest(content.getBytes()));
            } catch (NoSuchAlgorithmException e) {
                logger.error("MD5加密错误！" + e.getMessage());
            }
        } else {
            logger.error("MD5加密内容为空！");
        }
        return null;
    }


    /***
     *
     * @Description 判断内容是否不为空
     * @return
     */
    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }


    /***
     *
     * @Description 判断内容是否为空
     * @return
     */
    public static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true;
        } else if (obj instanceof String) {
            return StringUtils.isEmpty((String) obj);
        }
        return false;
    }


}