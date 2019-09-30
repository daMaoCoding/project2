package dc.pay.business.huitianfudaifu;

/**
 * ************************
 * @author sunny
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.business.yidongzhifu.XmlUtils;
import dc.pay.config.annotation.RequestDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;
import java.util.UUID;


@RequestDaifuHandler("HUITIANDAIFU")
public final class HuiTianDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HuiTianDaiFuRequestHandler.class);

     //请求代付&查询代付-参数
     private static final String  version  = "version"; //   接口版本号为 2
     private static final String  agent_id = "agent_id";       //   商户编号如 1000001
     private static final String  batch_no = "batch_no";  //批量付款定单号（要保证唯一）。长度最长 20 字符
     private static final String  batch_amt= "batch_amt";   //  付款总金额不可为空，单位：元，小数点后保留两位。

     private static final String  batch_num  ="batch_num";       //	该次付款总笔数，付给多少人的数目
     private static final String  detail_data="detail_data";       //	是	20	收款人银行账户名	张三
     private static final String  notify_url ="notify_url";       //	支付后返回的商户处理页面
     private static final String  ext_param1 ="ext_param1";       //	商户自定义原样返回,长度最长 50 字符
     
     private static final String  key ="key";       //	商户自定义原样返回,长度最长 50 字符
     private static final String  sign ="sign";       //	商户自定义原样返回,长度最长 50 字符
     

    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
        try {
        		String detailData=String.format("%s^%s^%s^%s^%s^%s^%s^%s^%s^%s", 
        				UUID.randomUUID().toString().replaceAll("-", "").substring(0, 19),
        				channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG(),
        				"0",
        				channelWrapper.getAPI_CUSTOMER_BANK_NUMBER(),
        				channelWrapper.getAPI_CUSTOMER_NAME(),
        				HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()),
        				channelWrapper.getAPI_ORDER_ID(),
        				"贵州省",
        				"贵阳市",
        				channelWrapper.getAPI_CUSTOMER_BANK_BRANCH()
        		);
                //组装参数
                payParam.put(agent_id,channelWrapper.getAPI_MEMBERID());
                payParam.put(batch_no,channelWrapper.getAPI_ORDER_ID());
                payParam.put(batch_amt,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                payParam.put(ext_param1,channelWrapper.getAPI_ORDER_ID());
                payParam.put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                payParam.put(batch_num,"1");
                payParam.put(detail_data,detailData);
                payParam.put(version,"2");
                payParam.put(key, channelWrapper.getAPI_KEY());

                //生成md5
                String pay_md5sign = null;
                List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < paramKeys.size(); i++) {
                	sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
                sb.replace(sb.lastIndexOf("&"), sb.lastIndexOf("&") + 1, "" );	
                String signStr = sb.toString();
                pay_md5sign = HandlerUtil.getMD5UpperCase(signStr.toLowerCase()).toLowerCase();
                payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

                //发送请求获取结果
                payParam.remove(key);
                String payOutUrl=channelWrapper.getAPI_CHANNEL_BANK_URL()+"BatchTransfer.aspx";
                String resultStr = RestTemplateUtil.postForm(payOutUrl, payParam,null);
                details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
                //addQueryDaifuOrderJob(channelWrapper.getAPI_ORDER_ID());

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
       if(1==2) throw new PayException("[汇天付代付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            payParam.put(batch_no,channelWrapper.getAPI_ORDER_ID());
            payParam.put(agent_id,channelWrapper.getAPI_MEMBERID());
            payParam.put(version,"2");

            //生成md5
            String pay_md5sign = null;
            String signStr=String.format("%s%s%s%s", 
            		agent_id+"="+payParam.get(agent_id)+"&",
            		batch_no+"="+payParam.get(batch_no)+"&",
            		key+"="+channelWrapper.getAPI_KEY()+"&",
            		version+"="+payParam.get(version)
            );
            pay_md5sign = HandlerUtil.getMD5UpperCase(signStr.toLowerCase()).toLowerCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);


            //发送请求获取结果
            String payQueryUrl=channelWrapper.getAPI_CHANNEL_BANK_URL()+"BatchQuery.aspx";
            String resultStr = RestTemplateUtil.postForm(payQueryUrl, payParam,null);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }



    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
    	 try {
             //组装参数
             payParam.put(version,"2");
             payParam.put(agent_id,channelWrapper.getAPI_MEMBERID());
             payParam.put(key,channelWrapper.getAPI_KEY());

             //生成md5
             String pay_md5sign = null;
             List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
             StringBuilder sb = new StringBuilder();
             for (int i = 0; i < paramKeys.size(); i++) {
             	sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
             }
             sb.replace(sb.lastIndexOf("&"), sb.lastIndexOf("&") + 1, "" );	
             String signStr = sb.toString();
             pay_md5sign = HandlerUtil.getMD5UpperCase(signStr.toLowerCase()).toLowerCase();
             payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(),pay_md5sign);

             //发送请求获取结果
             String payQueryUrl=channelWrapper.getAPI_CHANNEL_BANK_URL()+"QueryBalance.aspx";
             String resultStr = RestTemplateUtil.postForm(payQueryUrl, payParam,"UTF-8");
             details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
             Map<String, String> rtnMap = XmlUtils.xmlToMap(resultStr.trim());
             if(rtnMap.containsKey("ret_code")&&rtnMap.get("ret_code").equals("0000")){
                 String balance = rtnMap.get("t_balance");
                 return Long.parseLong(HandlerUtil.getFen(balance));
             }else{ throw new PayException(resultStr);}
         } catch (Exception e){
             log.error("[Hey代付][代付余额查询]出错,错误消息：{},参数：{}",e.getMessage(),JSON.toJSONString(payParam),e);
             throw new PayException( String.format("[Hey代付][代付余额查询]出错,错误:%s",e.getMessage()));
         }
    }









    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {
    	Map<String, String> rtnMap = XmlUtils.xmlToMap(resultStr.trim());
        if(!isQuery){
            if(rtnMap.get("ret_code").equals("0000")) return PayEumeration.DAIFU_RESULT.PAYING;
            if(!rtnMap.get("ret_code").equals("0000")) return PayEumeration.DAIFU_RESULT.ERROR;
            throw new  PayException(resultStr);
        }else{
//        	状态	说明	-1	交易已取消   0	审核通过，正在处理   1 等待审核   2	审核未通过 3	付款失败 4	付款成功
        	String detailData=rtnMap.get("detail_data");
        	if(StringUtils.isBlank(detailData)) return PayEumeration.DAIFU_RESULT.ERROR;
            if( detailData.split("\\^")[4].equals("S")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            if( detailData.split("\\^")[4].equals("P")) return PayEumeration.DAIFU_RESULT.PAYING;
            if( detailData.split("\\^")[4].equals("F")) return PayEumeration.DAIFU_RESULT.ERROR;
            throw new PayException(resultStr);
        }

    }








}