package dc.pay.business.dzdaifu;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;

public class SystemUtil {

    public static String doPostQueryCmd(String strURL, String req) {
        String result = null;
        BufferedReader in = null;
        BufferedOutputStream out = null;
        try {
            URL url = new URL(strURL);
            URLConnection con = url.openConnection();
            HttpURLConnection httpUrlConnection = (HttpURLConnection) con;
            httpUrlConnection.setRequestMethod("POST");
            con.setUseCaches(false);
            con.setDoInput(true);
            con.setDoOutput(true);
            out = new BufferedOutputStream(con.getOutputStream());
            byte outBuf[] = req.getBytes("utf-8");
            out.write(outBuf);
            out.close();

            in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
            StringBuffer sb = new StringBuffer();
            String data = null;

            while ((data = in.readLine()) != null) {
                sb.append(data);
            }

            System.out.println("res:" + sb.toString());
            result = sb.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        if (result == null)
            return "";
        else
            return result;
    }


    public static String mapToString(Map<String, String> params) {

        StringBuffer sb = new StringBuffer();
        String result = "";

        if (params == null || params.size() <= 0) {
            return "";
        }
        for (String key : params.keySet()) {
            String value = params.get(key);
            if (value == null || value.equals("")) {
                continue;
            }
//            sb.append(key + "=" + value + "&");
            try {
                sb.append(key + "=" + URLEncoder.encode(value, "UTF-8") + "&");
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        result = sb.toString().substring(0, sb.length() - 1);

        return result;
    }


}
