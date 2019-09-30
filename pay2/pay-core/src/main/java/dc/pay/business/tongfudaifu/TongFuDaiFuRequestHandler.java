package dc.pay.business.tongfudaifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.RequestDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


@RequestDaifuHandler("TONGFUDAIFU")
public final class TongFuDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(TongFuDaiFuRequestHandler.class);

    //请求代付&查询代付-参数
    private static final String  merchno = "merchno";  //商户号
    private static final String  amount = "amount";  //金额 单位:元
    private static final String  traceno = "traceno";  //订单号
    private static final String  goodsName = "goodsName";
    private static final String  settleType = "settleType";  //0
    private static final String  bankName = "bankName";  //工商银行上东分行
    private static final String  mobile = "mobile";  // 固定
    private static final String  accountName = "accountName";  //账户名
    private static final String  accountno = "accountno";  //银行卡号
    private static final String  bankno = "bankno";  //银行联行号
    private static final String  bankType="bankType";// 1开户行,2结算行(传1的话 就传分行信息；传2的话 就传总行信息)



    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
            if(!channelWrapper.getAPI_KEY().contains("&") ||channelWrapper.getAPI_KEY().split("&").length!=2 ){
                details.put(RESPONSEKEY, "密钥格式错误：请填写，[交易密钥]&[代付秘钥]，如：ABC&123");
                return PayEumeration.DAIFU_RESULT.ERROR;
            }

            //组装参数
            payParam.put(merchno,channelWrapper.getAPI_MEMBERID());
            payParam.put(amount,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(traceno,channelWrapper.getAPI_ORDER_ID());
            payParam.put(settleType,"0");
            payParam.put(bankName,channelWrapper.getAPI_CUSTOMER_BANK_BRANCH());
            payParam.put(mobile,"13098521100");
            payParam.put(accountName, channelWrapper.getAPI_CUSTOMER_NAME());
            payParam.put(accountno, channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
            payParam.put(bankno, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            payParam.put(bankType,"2" );
            payParam.put(goodsName,channelWrapper.getAPI_ORDER_ID() );

            //生成md5
            StringBuilder sb = new StringBuilder((payParam.size() + 1) * 10);
            List<String> keys = new ArrayList<String>(payParam.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                sb.append(key).append("=");
                sb.append(payParam.get(key));
                sb.append("&");
            }
            sb.setLength(sb.length() - 1);
            String preStr = sb.toString()+"&" +channelWrapper.getAPI_KEY().split("&")[0]+channelWrapper.getAPI_KEY().split("&")[1];
            String pay_md5sign =  HandlerUtil.getMD5UpperCase(preStr.getBytes(GBK)).toUpperCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

            //发送请求获取结果
            String url = HandlerUtil.getUrlWithEncode(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0],payParam,GBK);
            String resultStr= HttpUtil.receiveBySend(url, GBK);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            addQueryDaifuOrderJob(channelWrapper.getAPI_ORDER_ID());  //自动查询

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
        if(1==2) throw new PayException("[通付代付][代付][查询订单状态]该功能未完成。");
        try {

            if(!channelWrapper.getAPI_KEY().contains("&") ||channelWrapper.getAPI_KEY().split("&").length!=2 ){
                details.put(RESPONSEKEY, "密钥格式错误：请填写，[交易密钥]&[代付秘钥]，如：ABC&123");
                throw new PayException("密钥格式错误：请填写，[交易密钥]&[代付秘钥]，如：ABC&1238");
            }


            //组装参数
            payParam.put(merchno,channelWrapper.getAPI_MEMBERID());
            payParam.put(traceno,channelWrapper.getAPI_ORDER_ID());


            //生成md5
            StringBuilder sb = new StringBuilder((payParam.size() + 1) * 10);
            List<String> keys = new ArrayList<String>(payParam.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                sb.append(key).append("=");
                sb.append(payParam.get(key));
                sb.append("&");
            }
            sb.setLength(sb.length() - 1);
            String preStr = sb.toString()+"&" +channelWrapper.getAPI_KEY().split("&")[0];
            String pay_md5sign =  HandlerUtil.getMD5UpperCase(preStr.getBytes(GBK)).toUpperCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

            //发送请求获取结果
            String url = HandlerUtil.getUrlWithEncode(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1],payParam,GBK);
            String resultStr= HttpUtil.receiveBySend(url, GBK);

            //发送请求获取结果
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }




    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        if(1==2) throw new PayException("[通付代付][代付余额查询]该功能未完成。");

        if(!channelWrapper.getAPI_KEY().contains("&") ||channelWrapper.getAPI_KEY().split("&").length!=2 ){
            details.put(RESPONSEKEY, "密钥格式错误：请填写，[交易密钥]&[代付秘钥]，如：ABC&123");
            throw new PayException("密钥格式错误：请填写，[交易密钥]&[代付秘钥]，如：ABC&1238");
        }


        try {
            //组装参数
            payParam.put(merchno,channelWrapper.getAPI_MEMBERID());

            //生成md5
            StringBuilder sb = new StringBuilder((payParam.size() + 1) * 10);
            List<String> keys = new ArrayList<String>(payParam.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                sb.append(key).append("=");
                sb.append(payParam.get(key));
                sb.append("&");
            }
            sb.setLength(sb.length() - 1);
            String preStr = sb.toString()+"&" +channelWrapper.getAPI_KEY().split("&")[0];
            String pay_md5sign =  HandlerUtil.getMD5UpperCase(preStr.getBytes(GBK)).toUpperCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

            //发送请求获取结果
            String url = HandlerUtil.getUrlWithEncode(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[2],payParam,GBK);
            String resultStr= HttpUtil.receiveBySend(url, GBK);

            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            JSONObject jsonObj = JSON.parseObject(resultStr);
            if(HandlerUtil.valJsonObj(jsonObj,"respCode","00") && jsonObj.containsKey("totalAmount") ){
                String balance = jsonObj.getString("totalAmount");
                return Long.parseLong(HandlerUtil.getFen(balance));
            }else{ throw new PayException(resultStr);}
        } catch (Exception e){
            log.error("[通付代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
            throw new PayException( String.format("[通付代付][代付余额查询]出错,错误:%s",e.getMessage()));
        }

    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);
        if(!isQuery){
            if (HandlerUtil.valJsonObj(jsonObj, "respCode", "99")) return PayEumeration.DAIFU_RESULT.PAYING;
            if (HandlerUtil.valJsonObj(jsonObj, "respCode", "88","06","94","A0")) return PayEumeration.DAIFU_RESULT.ERROR;
            //if (HandlerUtil.valJsonObj(jsonObj, "respCode", "00")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            return PayEumeration.DAIFU_RESULT.ERROR;  //XP：除了返回99处理中，返回任何其他的，都代表失败，可以再次重复出款，而不会造成重复出款
        }else {
            if( resultStr.contains("找不到交易"))return PayEumeration.DAIFU_RESULT.ERROR;
            if (HandlerUtil.valJsonObj(jsonObj, "respCode", "10")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            if (HandlerUtil.valJsonObj(jsonObj, "respCode", "66")) return PayEumeration.DAIFU_RESULT.PAYING;
            if (HandlerUtil.valJsonObj(jsonObj, "respCode", "77", "88")) return PayEumeration.DAIFU_RESULT.ERROR;
            new PayException(resultStr);
        }
        return PayEumeration.DAIFU_RESULT.UNKNOW;
    }






}