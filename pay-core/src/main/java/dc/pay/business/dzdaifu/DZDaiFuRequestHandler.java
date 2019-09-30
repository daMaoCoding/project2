package dc.pay.business.dzdaifu;

import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.business.qiqi.MD5Util;
import dc.pay.config.annotation.RequestDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.RsaUtil;

/**
 * 
 * @author andrew
 * Aug 5, 2019
 */
@RequestDaifuHandler("DZDAIFU")
public final class DZDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(DZDaiFuRequestHandler.class);

    //1.结算下单接口
    //调用地址：http://pdd.x315y.xyz:5933/Pay/YZfb/dfPay
    //顺序  参数名 参数描述    非空  长度  示例
    //1   memberid    商户编号    是   10位 M1000001
    private static final String  memberid               = "memberid";
    //2   orderid 商户订单号   是   1-50位   R_12345
    private static final String  orderid               = "orderid";
    //3   payAmount   金额  是   元为单位    100.00
    private static final String  payAmount               = "payAmount";
    //4   cardHolder  收款人姓名   是   <150    保时捷维修
    private static final String  cardHolder               = "cardHolder";
    //5   notifyUrl   服务器回调地址 是   <300    http://c.a.com/server
    private static final String  notifyUrl               = "notifyUrl";
    //6   bankName    收款人银行账号开户行  是   200 
    private static final String  bankName               = "bankName";
    //7   bankProvince    开户行省    否   200 山西省
//    private static final String  bankProvince               = "bankProvince";
    //8   bankCity    开户行市    否   200 太原市
//    private static final String  bankCity               = "bankCity";
    //9   bankCardNo  收款人银行卡号 是   200 6225****5625
    private static final String  bankCardNo               = "bankCardNo";
    //10  cardId  持卡人身份证号码    否   200 422********5326
//    private static final String  cardId               = "cardId";
    //11  mobile  银行卡预留手机号码   否   11  138****8596
//    private static final String  mobile               = "mobile";
    //12  bankCode    银行编码    否   200 银联号
//    private static final String  bankCode               = "bankCode";
    //13  bankBranchName  支行信息    是       
    private static final String  bankBranchName               = "bankBranchName";
    //14  send_type   结算方式    是       结算方式：T1，D0，目前只接受D0结算
    private static final String  send_type               = "send_type";
    //15  sign    签名  是       
//    private static final String  sign               = "sign";
    
    //2.查询支付接口
    //HTTP请求参数明细:
    //调用地址：http://pdd.x315y.xyz:5933/Pay/YZfb/dfquery
    //    顺序  参数名 参数描述    非空  长度  示例  排序
    //1   memberid    商户编号    是   10位 1000001 1
//    private static final String  memberid               = "memberid";
    //2   orderid 商户订单号   是   1-50位   R_12345 2
//    private static final String  orderid               = "orderid";
//    //3   sign    签名  是           3
//    private static final String  sign               = "sign";

    //响应参数定义：以 json 格式同步返回响应数据
    





    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        String api_KEY = channelWrapper.getAPI_KEY();
        if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
            log.error("[Dz代付]-[请求代付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
            throw new PayException("[Dz代付]-[请求代付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
        }
        try {
                payParam.put(memberid,channelWrapper.getAPI_MEMBERID());
                payParam.put(orderid,channelWrapper.getAPI_ORDER_ID());
                payParam.put(payAmount,handlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                payParam.put(cardHolder,channelWrapper.getAPI_CUSTOMER_NAME());
                payParam.put(notifyUrl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                payParam.put(bankName,channelWrapper.getAPI_CUSTOMER_BANK_NAME());
//                payParam.put(bankProvince,channelWrapper.getAPI_CUSTOMER_BANK_BRANCH());
                payParam.put(bankCardNo,channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
//                payParam.put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
                payParam.put(bankBranchName,channelWrapper.getAPI_CUSTOMER_BANK_SUB_BRANCH());
                payParam.put(send_type,"D0");

                //生成md5
                List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < paramKeys.size(); i++) {
                    sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
                sb.append("key=" + channelWrapper.getAPI_KEY().split("-")[0]);
                String signStr = sb.toString(); //.replaceFirst("&key=","")
                String md5sign = HandlerUtil.getMD5UpperCase(signStr, "UTF-8").toUpperCase();
                String pay_md5sign = URLEncoder.encode(RsaUtil.signByPrivateKey(md5sign, channelWrapper.getAPI_KEY().split("-")[1],"SHA1withRSA"));
                String postString = SystemUtil.mapToString(payParam) + "&"+channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME()+"=" + pay_md5sign;

                String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], postString, "application/x-www-form-urlencoded");

                details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
//                addQueryDaifuOrderJob(channelWrapper.getAPI_ORDER_ID());

                if(StringUtils.isNotBlank(resultStr) ){
                    return getDaifuResult(resultStr,false);
                }else{ throw new PayException(EMPTYRESPONSE);}


                //结束

        }catch (Exception e){
            e.printStackTrace();
            throw new PayException(e.getMessage());
        }
    }



    //查询代付
    //第三方确定转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    //第三方确定转账取消并不会再处理，返回 PayEumeration.DAIFU_RESULT.ERROR
    //如果第三方确定代付处理中，返回  PayEumeration.DAIFU_RESULT.PAYING
   // 其他情况抛异常
    @Override
    protected PayEumeration.DAIFU_RESULT queryDaifuAllInOne(Map<String, String> payParam,Map<String, String> details) throws PayException {
       if(1==2) throw new PayException("[Dz代付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            payParam.put(memberid,channelWrapper.getAPI_MEMBERID());
            payParam.put(orderid,channelWrapper.getAPI_ORDER_ID());

            //生成md5
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
            sb.append("key=" + channelWrapper.getAPI_KEY().split("-")[0]);
            String localMd5 = sb.toString(); //.replaceFirst("&key=","")

            String md5sign = HandlerUtil.getMD5UpperCase(localMd5, "UTF-8").toUpperCase();

            String pay_md5sign = URLEncoder.encode(RsaUtil.signByPrivateKey(md5sign, channelWrapper.getAPI_KEY().split("-")[1],"SHA1withRSA"));
            
            String postString = SystemUtil.mapToString(payParam) + "&"+channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME()+"=" + pay_md5sign;

            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1], postString, "application/x-www-form-urlencoded");

            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果

            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }



    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        if(1==2) throw new PayException("[Dz代付][代付余额查询]该功能未完成。");

        try {

            //组装参数
            payParam.put(memberid,channelWrapper.getAPI_MEMBERID());
            
            //生成md5
            String localMd5 = SystemUtil.mapToString(payParam) + "&key=" + channelWrapper.getAPI_KEY().split("-")[0];

            String md5sign =  HandlerUtil.getMD5UpperCase(localMd5, "UTF-8").toUpperCase();

            String pay_md5sign = URLEncoder.encode(RsaUtil.signByPrivateKey(md5sign, channelWrapper.getAPI_KEY().split("-")[1],"SHA1withRSA"));

            String postString = SystemUtil.mapToString(payParam) + "&sign=" + pay_md5sign;

            //发送请求获取结果
            String resultStr = RestTemplateUtil.postStr(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2], postString, "application/x-www-form-urlencoded");

            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果

            JSONObject jsonObj = JSON.parseObject(resultStr);
            //查询 余额，list只有一个元素吧？
            //技术123 2019/8/3 18:53:47
            //是的
            if(HandlerUtil.valJsonObj(jsonObj,"retCode","10000") && jsonObj.containsKey("available_money") && StringUtils.isNotBlank( jsonObj.getString("available_money"))  ){
                String balance =  jsonObj.getString("available_money");
                return Long.parseLong(balance);
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[Dz代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[Dz代付][代付余额查询]出错,错误:%s",e.getMessage()));
        }

    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    //***给我平台业务系统返回ERROR状态需要谨慎：返回不恰当，会造成业务系统后台可解锁订单而造成多次出款-->需要卖肾赔钱
    //***因此，当不确定的情况下，哪怕返回UNKNOW，也不要返回ERROR
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);
        //给我平台业务系统返回ERROR状态需要谨慎：返回不恰当，会造成业务系统后台可解锁订单而造成多次出款-->需要卖肾赔钱
        //因此，当不确定的情况下，哪怕返回UNKNOW，也不要返回ERROR
        //代付：点击请求代付 操作
        if(!isQuery){
            //技术123 2019/8/3 18:13:36
            //retCode不是10000的都是请求不成功的
            //技术123 2019/8/3 18:14:07
            //9999都是请求有错误的
            //1   retCode 处理结果码   是       10000处理成功
            //9 status  订单状态    是       （状态说明：1处理中，2成功，3失败或拒绝）
            //根据status字段判断是否下发成功 0未处理  1处理中  2成功   3处理失败
            //0未处理，这个我可以重新下发吗？还是我这边继续 等待？
            //技术123 2019/8/3 18:34:51
            //是的要等这边处理
            if( !HandlerUtil.valJsonObj(jsonObj,"retCode","10000")) return PayEumeration.DAIFU_RESULT.ERROR;
            if( HandlerUtil.valJsonObj(jsonObj,"retCode","10000") && HandlerUtil.valJsonObj(jsonObj,"status","3")) return PayEumeration.DAIFU_RESULT.ERROR;
            if( HandlerUtil.valJsonObj(jsonObj,"retCode","10000") && HandlerUtil.valJsonObj(jsonObj,"status","1","0")) return PayEumeration.DAIFU_RESULT.PAYING;
            if( HandlerUtil.valJsonObj(jsonObj,"retCode","10000") && HandlerUtil.valJsonObj(jsonObj,"status","2")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            throw new  PayException(resultStr);
        //代付：点击查询代付 操作
        }else{
            //1   retCode 处理结果码   是       10000处理成功
            if( HandlerUtil.valJsonObj(jsonObj,"retCode","10000")){
                //status    订单状态    是       （状态说明：1处理中，2成功，3失败或拒绝）
                //根据status字段判断是否下发成功 0未处理  1处理中  2成功   3处理失败
                //0未处理，这个我可以重新下发吗？还是我这边继续 等待？
                //技术123 2019/8/3 18:34:51
                //是的要等这边处理
                if( HandlerUtil.valJsonObj(jsonObj,"status","2")) return PayEumeration.DAIFU_RESULT.SUCCESS;
                if( HandlerUtil.valJsonObj(jsonObj,"status","1","0")) return PayEumeration.DAIFU_RESULT.PAYING;
                if( HandlerUtil.valJsonObj(jsonObj,"status","3")) return PayEumeration.DAIFU_RESULT.ERROR;
                throw new PayException(resultStr);                
            }
            throw new PayException(resultStr);
        }

    }


public static void main(String[] args) {

    
    String api_KEY = "N97A9jGK66a1tB9T6YoyMBiwZYoKW-MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAN4eA2bmqA2hdOgckYAU6rPu3f3co4iT4V7pZyo4s2W13s4EfdTxxtDvqK/Dd0+k3cQg9Cb+RZUlB06+NBkVL4oGwOseiA0QQEjkE0MXe8md23xz3pJ/4nyn9TBPDdxCRauSpA+t9BimIt+MJytWbwCaTJXm4Ga0vdJGZNvTIdodAgMBAAECgYEAugMgjjh0XJXuNcRXBZB+zZkpR8RTH7nYb4e/4dgCRRd+M8IHLyIbgjZyal+jt4s2PV3rmgwvcavE5uja8HaRi9J/m75PR6PzzGBr4G8NgZgUPiiZSgolvg7T/M/yDT18VlG45ttfxjTbLOg97LH0OGdSGiJZFOo/EIhhnmi7J4ECQQD0Vn30Q4FV5k3sFhWzyTeP4WGKIisRyT0Mr+WXgNR+Wd/6XdCI9FVG5n3qwm6NM3xbUveeA02T6xseBK6xR0wFAkEA6LgDPMMNrmUGKYYGvGx4nfLWP9fRe2vOnSVCMNlL+rrDoxlyfR/++toV094ZrrdC0PCeW257KHRCEM0ax+rJOQJBAM0oYpHarJpnyj3VPVu8NH8HC1Nk5S0F/9DLUMgpUELXTA15AEQ+g+wIVOntX84H0P8NZwSyWU0+N4K5A8Wuy9ECQGgIQqI6C/anAK81U/ONhgoN0Ysuvl2vHukPC9zsdtO6A9T4fj7DO+gF7/YLdm2tTPg8aH41EGdQOWxrVAqEwbkCQB+g6GpDTsVhLKft74OH02XTU+yoyxWIWtuCFDuv6+b8hDw5eaHvC1uZlouWwv3CXoB444O22fq6fdV/+PSaCRE=";
    if (null == api_KEY || !api_KEY.contains("-") || api_KEY.split("-").length != 2) {
        log.error("[Dz代付]-[请求代付]-“密钥（私钥）框”输入数据格式为【中间使用-分隔】：MD5Key-RSA私钥" );
        System.out.println("==============异常==================");
    }
    Map<String,String> payParam = new TreeMap<String, String>();
    //  //组装参数
    ////1   memberid    商户编号    是   10位 M1000001
    //private static final String                 = "memberid";
    ////2   orderid 商户订单号   是   1-50位   R_12345
    //private static final String                 = "orderid";
    ////3   payAmount   金额  是   元为单位    100.00
    //private static final String                 = "payAmount";
    ////4   cardHolder  收款人姓名   是   <150    保时捷维修
    //private static final String                 = "cardHolder";
    ////5   notifyUrl   服务器回调地址 是   <300    http://c.a.com/server
    //private static final String                 = "notifyUrl";
    ////6   bankName    收款人银行账号开户行  是   200 
    //private static final String                 = "bankName";
    ////7   bankProvince    开户行省    否   200 山西省
    //private static final String                 = "bankProvince";
    ////8   bankCity    开户行市    否   200 太原市
    //private static final String                 = "bankCity";
    ////9   bankCardNo  收款人银行卡号 是   200 6225****5625
    //private static final String                 = "bankCardNo";
    ////10  cardId  持卡人身份证号码    否   200 422********5326
    //private static final String  cardId               = "cardId";
    ////11  mobile  银行卡预留手机号码   否   11  138****8596
    //private static final String  mobile               = "mobile";
    ////12  bankCode    银行编码    否   200 银联号
    //private static final String  bankCode               = "bankCode";
    ////13  bankBranchName  支行信息    是       
    //private static final String  bankBranchName               = "bankBranchName";
    ////14  send_type   结算方式    是       结算方式：T1，D0，目前只接受D0结算
    //private static final String  send_type               = "send_type";
      payParam.put(memberid,"10258");
      payParam.put(orderid,"20190803132041607452");
      payParam.put(payAmount,"1.00");
      payParam.put(cardHolder,"王小军");
      payParam.put(notifyUrl,"http://66p.nsqmz6812.com:30000/respDaifuWeb/DZDAIFU_BANK_WEB_DF_ZCQBYX/");
      payParam.put(bankName,"中国建设银行");
    //  payParam.put(bankProvince,channelWrapper.getAPI_CUSTOMER_BANK_BRANCH());
      payParam.put(bankCardNo,"6217004160022335741");
    //  payParam.put(bankCode,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
      payParam.put(bankBranchName,"永寿县支行");
      payParam.put(send_type,"D0");
    
//      签名字段：bankBranchName=&bankCardNo=&bankName=&cardHolder=&memberid=&notifyUrl=&orderid=&payAmount=&send_type=D0&
      
      //生成md5
    //  String pay_md5sign = null;
      List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < paramKeys.size(); i++) {
          sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
      }
      sb.append("key=" + api_KEY.split("-")[0]);
      String signStr = sb.toString(); //.replaceFirst("&key=","")
    //  System.out.println(signStr);
    //  pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
      
    //  String localMd5 = SystemUtil.mapToString(payParam) + "&key=" + api_KEY.split("-")[0];
      String localMd5 = signStr;
      System.out.println("代付签名字符串:" + localMd5);
    //  String md5sign = MD5Util.MD5Encode(localMd5, "UTF-8").toUpperCase();
      String md5sign = null;
      try {
//          md5sign = HandlerUtil.getMD5UpperCase(localMd5, "UTF-8").toUpperCase();
          md5sign = MD5Util.MD5Encode(localMd5, "UTF-8").toUpperCase();;
//          md5sign = "CE64DCC001B2A3CBFDEE10976E6C460C";
    //      md5sign = HandlerUtil.getMD5UpperCase("bankBranchName=永寿县支行&bankCardNo=6217004160022335741&bankName=中国建设银行&cardHolder=王小军&memberid=10258&notifyUrl=http://66p.nsqmz6812.com:30000/respDaifuWeb/DZDAIFU_BANK_WEB_DF_ZCQBYX/&orderid=20190803132041607452&payAmount=1.00&send_type=D0&key=N97A9jGK66a1tB9T6YoyMBiwZYoKWg", "UTF-8").toUpperCase();
      } catch (Exception e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
      }
      System.out.println("代付md5签名结果:" + md5sign);
      System.out.println("代付加密私钥:" + api_KEY.split("-")[1]);
      String pay_md5sign = null;
      try {
//          pay_md5sign = URLEncoder.encode(MyRSAUtils.sign(api_KEY.split("-")[1], md5sign, "SHA1withRSA"), "UTF-8");
          pay_md5sign = URLEncoder.encode(RsaUtil.signByPrivateKey(md5sign, api_KEY.split("-")[1],"SHA1withRSA"));
//          pay_md5sign = RsaUtil.signByPrivateKey(md5sign, api_KEY.split("-")[1],"SHA1withRSA");
    //      pay_md5sign = MyRSAUtils.sign(api_KEY.split("-")[1], md5sign, "SHA1withRSA");
      } catch (Exception e) {
          e.printStackTrace();
      }
      pay_md5sign = pay_md5sign.replaceAll("\r|\n", "");
      String postString = SystemUtil.mapToString(payParam) + "&sign=" + pay_md5sign;
      System.out.println("代付提交数据postString：" + postString.toString());
      payParam.put("sign",pay_md5sign);
      System.out.println("代付提交数据payParam：" + JSON.toJSONString(payParam));
      System.out.println("代付提交地址：" + "http://pdd.x315y.xyz:5933/Pay/YZfb/dfPay");
//      String resultStr = SystemUtil.doPostQueryCmd("http://pdd.x315y.xyz:5933/Pay/YZfb/dfPay", postString);
      String resultStr = null;
    try {
        resultStr = RestTemplateUtil.postJson("http://pdd.x315y.xyz:5933/Pay/YZfb/dfPay", payParam);
    } catch (PayException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
//      String resultStr = RestTemplateUtil.postForm("http://pdd.x315y.xyz:5933/Pay/YZfb/dfPay", payParam,"utf-8");
      System.out.println("代付返回数据:" + resultStr);
    
      
    //  payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);
    //  //发送请求获取结果
    ////  String resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam);
    //  String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam,"utf-8");
    //  System.out.println("代付请求地址==>"+channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0]);
    //  System.out.println("代付请求返回==>"+resultStr);
    //  System.out.println("代付请求参数==>"+JSON.toJSONString(payParam));
    //  details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
    //  addQueryDaifuOrderJob(channelWrapper.getAPI_ORDER_ID());
    
      if(StringUtils.isNotBlank(resultStr) ){
    //      return getDaifuResult(resultStr,false);
      }else{System.out.println("===========异常==============");}
    
    
      //结束



}


private String sign(String data,String chw) {
    try {
        byte[] keyBytes = Base64.decodeBase64(chw.getBytes());
        PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyf = KeyFactory.getInstance("RSA","SunRsaSign");
        PrivateKey myprikey = keyf.generatePrivate(priPKCS8);
        Signature signet = Signature.getInstance("SHA1withRSA","SunRsaSign");
        signet.initSign(myprikey);
        byte[] infoByte = data.getBytes("UTF-8");
        signet.update(infoByte);
        byte[] signed = signet.sign();
        String sign = Base64.encodeBase64String(signed);
        return sign;
    } catch (Exception e) {
        e.printStackTrace();
    }
    return null;
}


//    public static String submitPost(String url, String params) throws PayException {
//        StringBuffer responseMessage = null;
//        java.net.HttpURLConnection connection = null;
//        java.net.URL reqUrl = null;
//        OutputStreamWriter reqOut = null;
//        InputStream in = null;
//        BufferedReader br = null;
//        int charCount = -1;
//        try {
//            responseMessage = new StringBuffer(128);
//            reqUrl = new java.net.URL(url);
//            connection = (java.net.HttpURLConnection) reqUrl.openConnection();
//            connection.setReadTimeout(50000);
//            connection.setConnectTimeout(100000);
//            connection.setDoOutput(true);
//            connection.setDoInput(true);
//            connection.setRequestMethod("POST");
//            reqOut = new OutputStreamWriter(connection.getOutputStream(),"UTF-8");
//            reqOut.write(params);
//            reqOut.flush();
//    
//            in = connection.getInputStream();
//            br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
//            while ((charCount = br.read()) != -1) {
//                responseMessage.append((char) charCount);
//            }
//        } catch (Exception e) {
//            throw new PayException("HT代付发送请求失败："+e.getMessage());
//        } finally {
//            try {
//                if (in != null) {
//                    in.close();
//                }
//                if (reqOut != null) {
//                    reqOut.close();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        return responseMessage.toString();
//    }


}