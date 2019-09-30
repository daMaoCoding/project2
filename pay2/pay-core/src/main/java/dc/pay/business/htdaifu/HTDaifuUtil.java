package dc.pay.business.htdaifu;

import dc.pay.base.processor.PayException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public class HTDaifuUtil {


    public static String createParam(Map<String, String> map) {
        try {
            if (map == null || map.isEmpty()) {
                return null;
            }
            //对参数名按照ASCII升序排序
            Object[] key = map.keySet().toArray();
            Arrays.sort(key);
            //生成加密原串
            StringBuffer res = new StringBuffer(128);
            for (int i = 0; i < key.length; i++) {
                res.append(key[i] + "=" + map.get(key[i]) + "&");
            }
            String rStr = res.substring(0, res.length() - 1);
            //System.out.println("请求接口加密原串 = " + rStr);
            return rStr ;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }



    public static String createParam(Map<String, String> map, String appInitKey) {
        try {
            if (map == null || map.isEmpty()) {
                return null;
            }
            //对参数名按照ASCII升序排序
            Object[] key = map.keySet().toArray();
            Arrays.sort(key);
            //生成加密原串
            StringBuffer res = new StringBuffer(128);
            for (int i = 0; i < key.length; i++) {
                res.append(key[i] + "=" + map.get(key[i]) + "&");
            }
            String rStr = res.substring(0, res.length() - 1);
            System.out.println("请求接口加密原串 = " + rStr);

            return rStr + "&hmac=" + getKeyedDigestUTF8(rStr, appInitKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    public static String getKeyedDigestUTF8(String strSrc, String key) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(strSrc.getBytes("UTF8"));
            String result="";
            byte[] temp;
            temp=md5.digest(key.getBytes("UTF8"));
            for (int i=0; i<temp.length; i++){
                result+=Integer.toHexString((0x000000ff & temp[i]) | 0xffffff00).substring(6);
            }
            return result;
        } catch (Exception e) {e.printStackTrace(); }
        return null;
    }


    public static String submitPost(String url, String params) throws PayException {
        StringBuffer responseMessage = null;
        java.net.HttpURLConnection connection = null;
        java.net.URL reqUrl = null;
        OutputStreamWriter reqOut = null;
        InputStream in = null;
        BufferedReader br = null;
        int charCount = -1;
        try {
            responseMessage = new StringBuffer(128);
            reqUrl = new java.net.URL(url);
            connection = (java.net.HttpURLConnection) reqUrl.openConnection();
            connection.setReadTimeout(50000);
            connection.setConnectTimeout(100000);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            reqOut = new OutputStreamWriter(connection.getOutputStream(),"UTF-8");
            reqOut.write(params);
            reqOut.flush();

            in = connection.getInputStream();
            br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            while ((charCount = br.read()) != -1) {
                responseMessage.append((char) charCount);
            }
        } catch (Exception e) {
            throw new PayException("HT代付发送请求失败："+e.getMessage());
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (reqOut != null) {
                    reqOut.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return responseMessage.toString();
    }


    public static Map<String,String> codes = new HashMap<String,String>(){{
        put("0","成功");
        put("1","失败");
        put("3","处理中");
        put("15030","非空参数异常");
        put("15031","MD5验证签名失败");
        put("15040","没有权限调用该接口！");
        put("11111","对不起，系统忙");
        put("15001","接口版本号错误");
        put("15002","命令字错误");
        put("15005","订单日期格式错误");
        put("15028","操作类型错误");
        put("10018","订单不存在");
        put("23002","des解密异常");
        put("15009","商户订单金额为空或格式不正确");
        put("21002","银行不存在");
        put("10001","商户不存在");
        put("25034","获取手续费失败");
        put("10141","交易金额超限（大于限制的最大额或小于限制的最小额）");
        put("23006","交易类型错误");
        put("17007","服务商渠道选择失败");
        put("11112","商户IP验证失败");
        put("10024","订单重复");
        put("10071","商户类型错误");
    }};




}
