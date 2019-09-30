package dc.pay.business.pipifuzhifu;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RSAUtils {

//    private static String pri_mer_Key;  //商户私钥
//    private static String pubKey;  //真皮公钥
//
//    /**
//     * 这里配置公私钥
//     */
//    public RSAUtils() {
////        pri_mer_Key = readStringFromFile("private_by_merchant.key");
//
//        //这里配置您的商户后台私钥
//        pri_mer_Key = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDZf/VmChoe9+a5Fp991ZWTJp+iU6J7xXx1SiR7sGUREd2asRMgV4eOgUaEpTV5e0KLRaDi+ZFxa5PkR826OVZURXDWwDEERzms3ZqxUCSn9m90eQwJjKneg0uQlpuub/eIJNPeMAMsM2erlfKwJA3ieq0Nx2YkY8eunEqk7vv1g3lyUHlp46rOKJsZVWVm5Oss21kC7iqrzyVLBk+KIcWBVq5kBs58Fb9LUdg/YQzvRESy2FVTs+adM/yFNDq2gx3EGngXz2Ffua9uWwf3zJP5bbOR3geqCOvo+CTVeA19Ky2iv++3voZU68mFoaNFaVbDZ4obh7fB1UbflOasVEzVAgMBAAECggEABc+kZ9iHQCKReIqBMmR9fZP/md+wLKxnGd8kQGXDHYSjrhljEG8mQQl1L+AWIKvJaF0w4j+WwjgEmInOllYn11KPpD04UgvxH4xAMNKaWB5+ddcyGiM2+qn5X+CWQj/dljadrocL3qd08qGr/UbJVC7A5uhX80rzL9gfKm70LmgLP0QgjbsgvFcN9N4XQIX1qeZq2k/LJBcVxLLxSN/sGJoAhgvIn2R/uP/VnU812XgDwROAKiBffd0ARvBnuCkt/dbrUjVmWPJjuNMiDeU1JP7WVRO+piHpvjlpqnS0cr/HDSilwBJAvehhjf14LvH7UApZtuUSaLCwYDEvrXjBtQKBgQDv0l4vQVvFRlZfGFzbRAC+Ex3fUi3gTd0OS4eXsaka1nAqoyF0ja6nm/RyRmMGb2z8bV48RPBPBp1SQS2mG95nJBmeiMJAjWy76jeWpF09+SYQNXZuCBkFeUqjo1bc8CfMY5LzEIs6E0cMSgksy3QsLywVunN5LW6reXsA5zWh7wKBgQDoLBl/iviySpfbOra7K6HrVIBmI5JRG/m2iUJ8OMnQKjpqmYXIlJ3mx9hpKwTfqBD/41aJGmGxSQeDPQ3wxl9kKgf9gX7Kzc6I50jjXAXFq3w5FaGutCFVrQ9UeicyrW/kG2Z1KjJyryGHOtxr46NBYiJX+uo9ceH99NvH2TtxewKBgQC22I6EuvEsLJ+Hq3yIlIV4ZfzdmrttjBAhaQa9anADDUHB0FSel7N1CRvL0MyJSIUwxHYXv3v01Jc38ofKYO8ldkmuLJjmPK2NbFrrmO+aY45Yx1o/NB9XpA17S/rbQqMlWznb5l4wbig6P2xVYd12HpUfD+QgnLoHNPzKSH4EswKBgG8yD9RCHvJlP23EJRhnwVkQU56XktA98c821hzgpeeJEglUFfsHEskhFcjozaDAwcOMgjmP5RIfw/VbHH4gxDcY2lLT3oILJ8vx2brqq+kMRKaicZlWtFBwXlM7XeKrplc9uEiNOTQsJIa77i3ODLaX5ZL1nZJoJLCYwVpp9EpZAoGAClJFHVBV1o772tiHCRmf7JjaysoKOlH9ErZ/Q0bTY5Vrs8N4Scg3R+ojg/1hszGOW/aJ9bh08OmMWwjbJB/Wmi6ZelKokzRrcOqsuN8i9/x4f3iGpsDuKNElo6z5qxPxUw/CMLVM6NtTTLnvpvKpH+xYt1ZATytg1Da8QMOHP84=";
//
//        //这里配置您的商户后台公钥
//        pubKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1URX+RFaxnvmNMQ0kZM6" +
//                "/HPEaWUZZk0Hoy/uuMfbHegkngaYfK9KVHLL7NNl1Wuk+LaOIuToNecC3/DzF6Uj" +
//                "FNRSvlWiMTLPo3u4jpo5/i4+pNxuN0N2CVnE/6NauYD2QwyMnTSOHXtLJ2fvgSwr" +
//                "9cOjFBpv4VNjJnKQWGuKYcjc4V7JIUqrvuejNOi+gqA1CFKEDsdZktnjlv1LSDC/" +
//                "ovEs2ypXAeeit6oirtXY1u1pazvgfUVp/S9N0B6Pi4YZhqGDUk2W7cK+UziW1jnK" +
//                "7iLueT2GwneELerFZLMJQMxB3NAKwv/syD1LaOKQbukYB6d/CKzJZe7FVOq/zxnm" +
//                "BQIDAQAB";
//    }


    /**
     * 本方法使用SHA1withRSA签名算法产生签名
     *
     * @return String 签名的返回结果(16进制编码)。当产生签名出错的时候，返回null。
     */
    public static String signByPrivateKey(String src, String pri_mer_Key) {
        try {
            Signature           sigEng     = Signature.getInstance("SHA1withRSA");
            byte[]              pribyte    = base64decode(pri_mer_Key.trim());
            PKCS8EncodedKeySpec keySpec    = new PKCS8EncodedKeySpec(pribyte);
            KeyFactory          fac        = KeyFactory.getInstance("RSA");
            RSAPrivateKey       privateKey = (RSAPrivateKey) fac.generatePrivate(keySpec);
            sigEng.initSign(privateKey);
            sigEng.update(src.getBytes());
            byte[] signature = sigEng.sign();
            return base64encode(signature);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 使用公钥验证签名
     *
     * @param sign
     * @param src
     * @return
     */
    public static boolean verifyByPublicKey(String sign, String pubKey, String src) {
        try {
            Signature          sigEng    = Signature.getInstance("SHA1withRSA");
            byte[]             pubbyte   = base64decode(pubKey.trim());
            X509EncodedKeySpec keySpec   = new X509EncodedKeySpec(pubbyte);
            KeyFactory         fac       = KeyFactory.getInstance("RSA");
            RSAPublicKey       rsaPubKey = (RSAPublicKey) fac.generatePublic(keySpec);
            sigEng.initVerify(rsaPubKey);
            sigEng.update(src.getBytes());
            byte[] sign1 = base64decode(sign);
            return sigEng.verify(sign1);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * base64加密
     *
     * @param bstr
     * @return
     */
    @SuppressWarnings("restriction")
    private static String base64encode(byte[] bstr) {
        String str = new sun.misc.BASE64Encoder().encode(bstr);
        str = str.replaceAll("\r", "").replaceAll("\n", "").replaceAll("", "");
        return str;
    }

    /**
     * base64解密
     *
     * @param str
     * @return byte[]
     */
    @SuppressWarnings("restriction")
    private static byte[] base64decode(String str) {
        byte[] bt = null;
        try {
            sun.misc.BASE64Decoder decoder = new sun.misc.BASE64Decoder();
            bt = decoder.decodeBuffer(str);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bt;
    }

    /**
     * 从文件中读取所有字符串
     *
     * @param fileName
     * @return String
     */
//    private String readStringFromFile(String fileName) {
//        StringBuffer str = new StringBuffer();
//        try {
//            File file = new File(fileName);
//            FileReader fr = new FileReader(file);
//            char[] temp = new char[1024];
//            while (fr.read(temp) != -1) {
//                str.append(temp);
//            }
//            fr.close();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//
//        }
//        return str.toString();
//    }
}