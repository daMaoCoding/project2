package dc.pay.business.yunbao;

/**
 * @author 谢青
 * @Description: String字符串工具类
 * @date 2017/9/20 0020 17:36:04 ${tags}
 */
public class StringUtil {



  public static boolean isBlank(CharSequence cs) {
    int strLen;
    if (cs != null && (strLen = cs.length()) != 0) {
      for(int i = 0; i < strLen; ++i) {
        if (!Character.isWhitespace(cs.charAt(i))) {
          return false;
        }
      }

      return true;
    } else {
      return true;
    }
  }

  public static boolean isEmpty(CharSequence cs) {
    return cs == null || cs.length() == 0;
  }

  public static boolean isBlankOrEmpty(String str) {
        return isBlank(str) || isEmpty(str);
    }
}
