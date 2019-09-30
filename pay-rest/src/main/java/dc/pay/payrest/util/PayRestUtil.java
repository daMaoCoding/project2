package dc.pay.payrest.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public class PayRestUtil {
    public static String formatDateTimeStrByParam(String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(new Date());
    }
    public static String curDateTimeStr() {
        SimpleDateFormat sdf = new SimpleDateFormat(PayRestUtil.Const.dateTimeString);
        return sdf.format(new Date());
    }
    public static String formatDateTime(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(PayRestUtil.Const.dateTimeString);
        return sdf.format(date);
    }

   public static class  Const{
        public static final String dateString = "yyyy-MM-dd";
        public static final String timeString = "HH:mm:ss";
        public static final String dateTimeString = "yyyy-MM-dd HH:mm:ss";
        public static final String dateTimeString2 = "yyyyMMddHHmmss";
    }
}
