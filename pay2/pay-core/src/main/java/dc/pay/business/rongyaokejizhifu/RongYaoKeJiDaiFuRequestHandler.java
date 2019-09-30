package dc.pay.business.rongyaokejizhifu;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.RequestDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author Mikey
 * Jun 19, 2019
 */
@RequestDaifuHandler("RONGYAOKEJIDAIFU")
public final class RongYaoKeJiDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RongYaoKeJiDaiFuRequestHandler.class);

    //请求代付&查询代付-参数
	/*
 	字段Key		字段名称			必填		备注
 merNo			商户编号			Y		商户唯一标识，在平台中开通的商户编号
 orderNo		商户请求流水号		Y		请求唯一标识，请务必保证流水唯一
 orderAmt		代付金额			Y		订单金融，整数，货币种类为人民币，以元为单位
 userName		收款方姓名			Y		25汉字长度以内
 cardNo			收款方账号			Y		
 bankName		收款银行			Y		请严格参见银行列表中支付的银行名称
 sign			签名(MD5)			Y		请参考2.3参数签名机制
bankProvince	 收款银行省			N		收款人开户行所在省份，如：山东省
bankCity		收款银行市			N		收款人开户行所在地区，如：济南市
bankFullName	收款银行全称			N		银行 + 分行 + 支行名称，如：中国民生银行股份有限公司济南高新支行

	 */

	private static final String merNo		= "merNo";	      //商户唯一标识，在平台中开通的商户编号
	private static final String orderNo		= "orderNo";	  //请求唯一标识，请务必保证流水唯一
	private static final String orderAmt	= "orderAmt";     //订单金融，整数，货币种类为人民币，以元为单位
	private static final String userName	= "userName";     //25汉字长度以内
	private static final String cardNo		= "cardNo";	      //
	private static final String bankName	= "bankName";     //请严格参见银行列表中支付的银行名称
	private static final String bankProvince	= "bankProvince";    
	private static final String bankCity		= "bankCity";    
	private static final String bankFullName	= "bankFullName";   

	private static final String sign		= "sign";	      //请参考2.3参数签名机制

    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
		try {
			// 组装参数
			//所有参数全部提交，非必填参数传空值
			payParam.put(merNo,		channelWrapper.getAPI_MEMBERID());
			payParam.put(orderNo,	channelWrapper.getAPI_ORDER_ID());
			payParam.put(orderAmt,	HandlerUtil.getYuanWithoutZero(channelWrapper.getAPI_AMOUNT()));//订单金融，整数，货币种类为人民币，以元为单位
			payParam.put(userName,	channelWrapper.getAPI_CUSTOMER_NAME());
			payParam.put(cardNo, 	channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
			payParam.put(bankName, 	channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
			payParam.put(bankProvince, 	"");
			payParam.put(bankCity, 		"");
			payParam.put(bankFullName, 	"");

            //生成md5
            String pay_md5sign = null;
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
            sb.append("key=" + channelWrapper.getAPI_KEY());
            String signStr = sb.toString();
            pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
            payParam.put(sign,pay_md5sign);

            //发送请求获取结果
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0] ,payParam,null );
            resultStr = UnicodeUtil.unicodeToString(resultStr);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            addQueryDaifuOrderJob(channelWrapper.getAPI_ORDER_ID());  //自动查询

            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,false);
            }else{ throw new PayException(EMPTYRESPONSE);}
            
			// 结束

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
       if(1==2) throw new PayException("[荣耀科技代付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            payParam.put(merNo,channelWrapper.getAPI_MEMBERID());
            payParam.put(orderNo,channelWrapper.getAPI_ORDER_ID());

            //生成md5
            String pay_md5sign = null;
            List paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if(StringUtils.isBlank(payParam.get(paramKeys.get(i))) || sign.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                    continue;
                sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
            sb.append("key=" + channelWrapper.getAPI_KEY());
            String signStr = sb.toString();
            pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
            payParam.put(sign,pay_md5sign);


            //发送请求获取结果
            String resultStr = RestTemplateUtil.postForm(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[1] ,payParam,null );
            resultStr = UnicodeUtil.unicodeToString(resultStr);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            if(StringUtils.isNotBlank(resultStr) ){
                return getDaifuResult(resultStr,true);
            }else{ throw new PayException(EMPTYRESPONSE);}

        }catch (Exception e){throw new PayException(e.getMessage()); }

    }




    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String,String> payParam,Map<String,String> details) throws PayException {
    	throw new PayException("[荣耀科技支付][代付余额查询]第三方不支持此功能。");
    }

    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr,boolean isQuery) throws PayException {

        if(!isQuery){
        	/*
	        	0000	交易成功（平台已成功接收请求，成功付款）
	        	0001	交易处理中（平台已成功接收请求，等待付款）
                0002	交易失败
                0003	商户不存在
                0004	商户状态异常
                0005	金额格式不正确
                0006	必输域不能为空
                0007	交易存在风险
                0008	加密验签失败
                0009	记录已存在
                0010	日累计金额或笔数超限
                0011	余额不足
                0012	单笔金额超限
                0013	低于最低交易限额
                0014	风控受限
                0015	访问过于频繁，请稍后重试
                0016	系统繁忙，请稍后再试
                0017	数据格式有误
        	*/
            JSONObject jsonObj = JSON.parseObject(resultStr);
            if (HandlerUtil.valJsonObj(jsonObj, "code", "0002", "0003", "0004", "0005", "0006", "0007", "0008", "0009", "0010"
                    , "0011", "0012", "0013", "0014", "0015", "0016", "0017"))
                return PayEumeration.DAIFU_RESULT.ERROR;
            if (HandlerUtil.valJsonObj(jsonObj, "code", "0000")) return PayEumeration.DAIFU_RESULT.PAYING;
            if(HandlerUtil.valJsonObj(jsonObj,"code","0001")) return PayEumeration.DAIFU_RESULT.PAYING;
            throw new  PayException(resultStr);
        }else{
        	/*
	        	ERROR：请求错误
	        	SUCCESSED：代付成功
	        	FAILED：代付失败
	        	PROCESSING：处理中
        	 */
        	JSONObject jsonObj = JSON.parseObject(resultStr);
            if (HandlerUtil.valJsonObj(jsonObj, "status", "ERROR")) return PayEumeration.DAIFU_RESULT.UNKNOW;
            if(HandlerUtil.valJsonObj(jsonObj,"status","SUCCESSED")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            if(HandlerUtil.valJsonObj(jsonObj,"status","PROCESSING")) return PayEumeration.DAIFU_RESULT.PAYING;
            if(HandlerUtil.valJsonObj(jsonObj,"status","FAILED")) return PayEumeration.DAIFU_RESULT.ERROR;
            throw new PayException(resultStr);
        }
    }






}