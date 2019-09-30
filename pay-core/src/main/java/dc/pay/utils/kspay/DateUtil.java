package dc.pay.utils.kspay;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class DateUtil {
    public static String getTimess() {
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss");
        String times = dateFormat.format(now);
        return times;
    }

    public static String getNewDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(5, -3);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        return formatter.format(calendar.getTime());
    }


    public static String getYear() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(5, -3);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy");
        return formatter.format(calendar.getTime());
    }


    public static String getNewDate1() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(5, -3);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

        return formatter.format(calendar.getTime());
    }


    public static String getTimess1() {
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String times = dateFormat.format(now);
        return times;
    }

    public static void main(String[] args) {
        System.out.println(getYear());
    }
}