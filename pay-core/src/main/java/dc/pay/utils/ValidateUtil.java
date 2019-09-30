package dc.pay.utils;/**
 * Created by admin on 2017/6/22.
 */

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.assertj.core.util.Sets;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dc.pay.base.processor.PayException;
import dc.pay.business.RequestDaifuResult;
import dc.pay.business.RequestPayResult;
import dc.pay.entity.ReqDaifuInfo;
import dc.pay.entity.ReqPayInfo;

/**
 * ************************
 *
 * @author tony 3556239829
 */
public class ValidateUtil {
    private static final Logger log =  LoggerFactory.getLogger(ValidateUtil.class);

    public static final Map<String, String> devIp = new HashMap<String,String>(){{
        put("172.28.240.35","预生产环境");
        put("172.28.200.41","测试环境");
     // put("192.168.137.1","本地开发");
    }};



    /**
     * 验证域名(兼容ip,ip:port,域名)，必须以http,或者https开头可以以/结尾
     * @param data
     * @return
     */
    public static boolean valdateDomain(String data) throws PayException {
        String domain = HandlerUtil.getDomain(data);
        if(!StringUtils.isBlank(domain)){
            return true;
        }
        return false;
    }


    /**
     * 验证long类型金额
     * @param data
     * @return
     */
    public static boolean valdateLongMoney(String data){
        String  regEx = "^(([1-9]\\d{0,19}))$";  //^(([1-9]\d{0,9})|0)(\.\d{1,2})?$
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(data);
        return  matcher.find() && (new BigDecimal(data).compareTo(new BigDecimal(Long.MAX_VALUE)))<1;
    }



    /**
     * 验证日期格式
     * @return
     */
    public static boolean valdateRiQi(String dataStr, String formater){
        if(StringUtils.isBlank(dataStr) || StringUtils.isBlank(formater)) return false;
         try{
             DateTimeFormatter format = DateTimeFormat.forPattern(formater);
             DateTime dateTime = DateTime.parse(dataStr, format);
             if(null!=dateTime) return true;
         }catch (Exception e){
             return false;
         }
         return false;
    }



    public static Date getDate(String dataStr, String formater){
        DateTimeFormatter format = DateTimeFormat.forPattern(formater);
        DateTime dateTime = DateTime.parse(dataStr, format);
        return dateTime.toDate();
    }


    /**
     * [共用]-请求支付结果检查
     * @param result
     * @return
     */
    public static boolean requestesultValdata(RequestPayResult result){
        if(null == result)
            return false;
        if(StringUtils.isBlank(result.getRequestPayamount()) || Double.parseDouble(result.getRequestPayamount())<=0.00) //请求支付金额
            return false;
        if(StringUtils.isBlank(result.getRequestPayOrderId())) //业务订单id
            return false;
        if(StringUtils.isBlank(result.getRequestPayOrderCreateTime())) //业务订单创建时间
            return false;
        if(StringUtils.isBlank(result.getRequestPayChannelBankName()))
            return false;
        if( ( StringUtils.isBlank(result.getRequestPayQRcodeContent())  || "null".equalsIgnoreCase(result.getRequestPayQRcodeContent()))  && StringUtils.isBlank(result.getRequestPayHtmlContent()) && StringUtils.isBlank(result.getRequestPayJumpToUrl()))  //支付二维内容或html内容有一即可
            return false;
        return true;
    }




    /**
     * [共用]-请求代付结果检查
     */
    public static boolean requestDaifuResValdata(RequestDaifuResult result){
        if(null == result)
            return false;

        if(StringUtils.isBlank(result.getRequestDaifuAmount()) || Double.parseDouble(result.getRequestDaifuAmount())<=0.00) //请求支付金额
            return false;
        if(StringUtils.isBlank(result.getRequestDaifuOrderId())) //业务订单id
            return false;
        if(StringUtils.isBlank(result.getRequestDaifuOrderState())) //业务订单状态
            return false;
        if(StringUtils.isBlank(result.getRequestDaifuOrderCreateTime())) //业务订单创建时间
            return false;
        if(StringUtils.isBlank(result.getRequestDaifuChannelBankName()))
            return false;
        if(!result.getRequestDaifuCode().equalsIgnoreCase("SUCCESS") && !result.getRequestDaifuCode().equalsIgnoreCase("ERROR"))
            return false;
        return true;
    }





    /**
     * 检查支付请求参数
     */
    public static boolean valdataReqPayInfo(ReqPayInfo reqPayInfo) throws PayException {
        if( null == reqPayInfo)
            return false;
        if(StringUtils.isBlank(reqPayInfo.getAPI_CHANNEL_BANK_NAME()) ||
                StringUtils.isBlank(reqPayInfo.getAPI_AMOUNT())    ||
                StringUtils.isBlank(reqPayInfo.getAPI_KEY())       ||
                StringUtils.isBlank(reqPayInfo.getAPI_MEMBERID())  ||
                StringUtils.isBlank(reqPayInfo.getAPI_ORDER_ID())  ||
                !valdateLongMoney(reqPayInfo.getAPI_AMOUNT())  )
            throw new PayException("通道名称/金额/密钥/商户号/订单号，错误。");

        if(!HandlerUtil.isRigthDomain(reqPayInfo.getAPI_JUMP_URL_PREFIX()) ){
            throw new PayException("跳转地址，错误："+reqPayInfo.getAPI_JUMP_URL_PREFIX());
        }
        if(!HandlerUtil.isRigthDomain(reqPayInfo.getAPI_NOTIFY_URL_PREFIX())){
            throw new PayException("回调地址，错误："+reqPayInfo.getAPI_NOTIFY_URL_PREFIX());
        }

        return true;
    }






    /**
     * 检查代付请求参数-For 查询/请求
     */
    public static boolean valdataReqDaifuInfo(ReqDaifuInfo reqDaifuInfo) throws PayException {
        if( null == reqDaifuInfo)
            throw new PayException("DBI/本地流水表获取[代付信息出错,空。]");
        if(StringUtils.isBlank(reqDaifuInfo.getAPI_CHANNEL_BANK_NAME()) ||
                StringUtils.isBlank(reqDaifuInfo.getAPI_AMOUNT())    ||
                StringUtils.isBlank(reqDaifuInfo.getAPI_KEY())       ||
                StringUtils.isBlank(reqDaifuInfo.getAPI_MEMBERID())  ||
                StringUtils.isBlank(reqDaifuInfo.getAPI_ORDER_ID())  ||
                !valdateLongMoney(reqDaifuInfo.getAPI_AMOUNT())  )
            throw new PayException("通道名称/金额/密钥/商户号/订单号，错误。");
        if(!HandlerUtil.isRigthDomain(reqDaifuInfo.getAPI_NOTIFY_URL_PREFIX())){
            throw new PayException("回调地址，错误："+reqDaifuInfo.getAPI_NOTIFY_URL_PREFIX());
        }
        if(StringUtils.isBlank(reqDaifuInfo.getAPI_CUSTOMER_BANK_NAME())   ||StringUtils.isBlank(reqDaifuInfo.getAPI_CUSTOMER_NAME())  ||StringUtils.isBlank(reqDaifuInfo.getAPI_CUSTOMER_BANK_NUMBER()) ){
            throw new PayException("银行名称/收款人姓名/收款银行账号/，错误。");
        }
        return true;
    }

    public static boolean valdataReqDaifuInfo(ReqDaifuInfo reqDaifuInfo,String serverId) throws PayException{
        if(StringUtils.isNotBlank(serverId)&& serverId.split(":").length==3 && null!=reqDaifuInfo && StringUtils.isNotBlank(reqDaifuInfo.getAPI_AMOUNT()) && Long.parseLong(reqDaifuInfo.getAPI_AMOUNT())>1100 && devIp.containsKey(serverId.split(":")[0])){
            throw new PayException(devIp.get(serverId.split(":")[0])+",禁止代付超过：11元。");
        }

        return valdataReqDaifuInfo(reqDaifuInfo);
    }



    /**
     * 检查代付请求参数-For 查询/请求
     */
    public static boolean valdataReqDaifuInfoForQueryBalance(ReqDaifuInfo reqDaifuInfo) throws PayException {
        if( null == reqDaifuInfo)
            throw new PayException("查询第三方[代付]余额参数不正确：参数空");
        if(StringUtils.isBlank(reqDaifuInfo.getAPI_CHANNEL_BANK_NAME()) ||
                StringUtils.isBlank(reqDaifuInfo.getAPI_KEY())       ||
                StringUtils.isBlank(reqDaifuInfo.getAPI_MEMBERID())  ||
                StringUtils.isBlank(reqDaifuInfo.getAPI_OID())  )
            throw new PayException("查询第三方[代付]余额参数不正确：第三方ID/密钥/商户号/OID，错误。");
        return true;
    }










    /**
     * 查找ip,http://127.0.0.11:8080/helloWorld
     * @param str
     * @return
     */
    public static boolean isIpHost(String str){
        Pattern patternDomain = Pattern.compile("^((http://)|(https://))?((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)((:\\d*){0,1})(/{0,1})",Pattern.CASE_INSENSITIVE);
        Matcher matcherDomain = patternDomain.matcher(str);
        if(matcherDomain.find()){
            return true;
        }
        return false;
    }




    /**
     * 中文字符
     * @param str
     * @return
     */
    public static boolean isHaveChinese(String str){
//        Pattern pattern = Pattern.compile("[u4E00-u9FA5]",Pattern.CASE_INSENSITIVE);
//        Matcher matcher = pattern.matcher(str);
//        if(matcher.find()){
//            return !true;
//        }
//        return !false;
        if (str == null) return false;
        for (char c : str.toCharArray()) {
            if (isChinese(c)) return true;// 有一个中文字符就返回
        }
        return false;
    }



    public static boolean isChinese(char c) {
        return c >= 0x4E00 &&  c <= 0x9FA5;
    }




    public  static void valiRSA_KEY(ReqPayInfo reqPayInfo) throws PayException {
        if(null!=reqPayInfo){
//            if(reqPayInfo.getAPI_KEY().contains("--") ||reqPayInfo.getAPI_PUBLIC_KEY().contains("--")){
//                throw new PayException("公钥/私钥/不允许包含--，如果使用的是RSA,请删除公钥私钥开头结尾的-----BEGIN END----,详情咨询客服");
//            }
            
            if(reqPayInfo.getAPI_KEY().contains("--") ||reqPayInfo.getAPI_PUBLIC_KEY().contains("--") ||
                    isHaveChinese(reqPayInfo.getAPI_PUBLIC_KEY())
                    ){
                throw new PayException("公钥/私钥/不允许包含--、中文，如果使用的是RSA,请删除公钥私钥开头结尾的-----BEGIN END----,详情咨询客服");
            }
        }
    }

    public  static void valiRSA_KEY(ReqDaifuInfo reqDaifuInfo) throws PayException {
        if(null!=reqDaifuInfo){
            if(reqDaifuInfo.getAPI_KEY().contains("--") ||reqDaifuInfo.getAPI_PUBLIC_KEY().contains("--")){
                throw new PayException("公钥/私钥/不允许包含--，如果使用的是RSA,请删除公钥私钥开头结尾的-----BEGIN END----,详情咨询客服");
            }
        }
    }


}
