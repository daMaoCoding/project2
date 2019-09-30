package dc.pay.business.mayidaifu;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSONObject;

import dc.pay.base.processor.DaifuResponseHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.ResponseDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author andrew
 * Sep 26, 2019
 */
@Slf4j
@ResponseDaifuHandler("MAYIDAIFU")
public final class MaYiDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("success");

    public static final String FLAG              ="MAYIDAIFU"; //商户账号
    
    //参数  说明  类型(长度)
    //params  加密参数(查看附签名规则)   string
    private static final String  params               = "params";
    //mcode   商户唯一标识  String(32)
    private static final String  mcode               = "mcode";
    
    //params加密前参数(json格式)：
    //参数  说明  类型(长度)
    //api_sn  api订单号  string
    private static final String  api_sn            = "api_sn";   
    //money   转账金额    float
    private static final String  money            = "money";   
    //status  30:执行异常,40:失败结束,50:已完成  int
    private static final String  status            = "status";   
    //sign    签名字符(查看附签名规则)   string
    private static final String  sign            = "sign";



    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {

        String flag_id = FLAG+":"+API_RESPONSE_PARAMS.get(mcode);
        String strFromRedis = handlerUtil.getStrFromRedis(flag_id);
        if ( StringUtils.isBlank(strFromRedis))
            throw new PayException("缓存里获取不到键："+flag_id+"的密钥，请检查 ");
        Map<String, String> jsonToMap = null;
        try {
//            Base
            byte[] encode = java.util.Base64.getDecoder().decode(API_RESPONSE_PARAMS.get(params).getBytes("utf-8"));
            String encode3 = Test.decode(new String(encode), strFromRedis);
            jsonToMap =  JSONObject.parseObject(encode3, Map.class);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("[蚂蚁代付]-[代付回调]-1.解密出错：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        String ordernumberR = jsonToMap.get(api_sn);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[蚂蚁代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> api_response_params, String api_key) throws PayException {
        Map<String, String> jsonToMap = null;
        try {
//            Base
            byte[] encode = java.util.Base64.getDecoder().decode(api_response_params.get(params).getBytes("utf-8"));
            String encode3 = Test.decode(new String(encode), api_key);
            jsonToMap =  JSONObject.parseObject(encode3, Map.class);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("[蚂蚁代付]-[代付回调]-2.自建签名：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        //生成md5
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(jsonToMap);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
            if(!sign.equals(paramKeys.get(i)) && (jsonToMap.get(paramKeys.get(i)) instanceof java.lang.String)){
                sb.append(paramKeys.get(i)).append("=").append(jsonToMap.get(paramKeys.get(i))).append("&");
            }else if(!sign.equals(paramKeys.get(i))){
                Object string2 = jsonToMap.get(paramKeys.get(i));
                sb.append(paramKeys.get(i)).append("=").append(string2+"").append("&");
            }
        }
        sb.append("key=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&key=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
        log.debug("2.[蚂蚁代付]-[代付回调]-自建签名：{}",pay_md5sign);
        return pay_md5sign;
    }



    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        String flag_id = FLAG+":"+API_RESPONSE_PARAMS.get(mcode);
        String strFromRedis = handlerUtil.getStrFromRedis(flag_id);
        if ( StringUtils.isBlank(strFromRedis))
            throw new PayException("缓存里获取不到键："+flag_id+"的密钥，请检查 ");
        Map<String, String> jsonToMap = null;
        try {
            byte[] encode = java.util.Base64.getDecoder().decode(api_response_params.get(params).getBytes("utf-8"));
            String encode3 = Test.decode(new String(encode), strFromRedis);
            jsonToMap =  JSONObject.parseObject(encode3, Map.class);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("[蚂蚁代付]-[代付回调]-3.验证回调金额：{}",e.getMessage(),e);
            throw new PayException(e.getMessage(),e);
        }
        boolean checkResult = false;
        String responseAmount =  HandlerUtil.getFen(jsonToMap.get(money));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[蚂蚁代付]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        String flag_id = FLAG+":"+API_RESPONSE_PARAMS.get(mcode);
        String strFromRedis = handlerUtil.getStrFromRedis(flag_id);
        Map<String, String> jsonToMap = null;
        try {
            byte[] encode = java.util.Base64.getDecoder().decode(api_response_params.get(params).getBytes("utf-8"));
            String encode3 = Test.decode(new String(encode), strFromRedis);
            jsonToMap =  JSONObject.parseObject(encode3, Map.class);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("[蚂蚁代付]-[代付回调]-4.验证第三方签名：{}",e.getMessage(),e);
        }
        
        boolean result = jsonToMap.get(sign).equalsIgnoreCase(signMd5);
        log.debug("4.[蚂蚁代付]-[代付回调]-验证第三方签名：{}",result);
        return result;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[蚂蚁代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    //回调订单状态
    //***给我平台业务系统返回ERROR状态需要谨慎：返回不恰当，会造成业务系统后台可解锁订单而造成多次出款-->需要卖肾赔钱
    //***因此，当不确定的情况下，哪怕返回UNKNOW，也不要返回ERROR
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
        String flag_id = FLAG+":"+API_RESPONSE_PARAMS.get(mcode);
        String strFromRedis = handlerUtil.getStrFromRedis(flag_id);
        Map<String, String> jsonToMap = null;
        try {
            byte[] encode = java.util.Base64.getDecoder().decode(api_response_params.get(params).getBytes("utf-8"));
            String encode3 = Test.decode(new String(encode), strFromRedis);
            jsonToMap =  JSONObject.parseObject(encode3, Map.class);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("[蚂蚁代付]-[代付回调]-6.订单状态：{}",e.getMessage(),e);
        }
        PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        String my_status = null;
        //status    30:执行异常,40:失败结束,50:已完成
        if(jsonToMap.containsKey(status)){
            if(jsonToMap.get(status) instanceof java.lang.String){
                my_status = jsonToMap.get(status);
            }else {
                Object string2 = jsonToMap.get(status);
                my_status = string2+"";
            }
           if( "30".equalsIgnoreCase(my_status) || "40".equalsIgnoreCase(my_status) ) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
//           if( "30".equalsIgnoreCase(my_status) ) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
           if( "50".equalsIgnoreCase(my_status) ) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
        }
        log.debug("6.[蚂蚁代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }





}