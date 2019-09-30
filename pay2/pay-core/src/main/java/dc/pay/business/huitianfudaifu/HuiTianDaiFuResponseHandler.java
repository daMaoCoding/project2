package dc.pay.business.huitianfudaifu;

/**
 * ************************
 * @author sunny
 */
import dc.pay.base.processor.DaifuResponseHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.ResponseDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@ResponseDaifuHandler("HUITIANDAIFU")
public final class HuiTianDaiFuResponseHandler extends DaifuResponseHandler {
    private static  String RESPONSE_PAY_MSG = stringResponsePayMsg("ErrCode=0");

//    字段名 				填写类型 			说明 
//    ret_code 			必填 				返回码值 0000 表示查询成功，其他详见附录
//    ret_msg 			必填 				返回码信息ᨀ示
//    agent_id 			必填				 商户编号如 1000001
//    hy_bill_no 		必填				 payment 交易号(订单号)
//    status 			必填 				-1=无效,0=未处理,1=成功
//    batch_no 			必填 				商户系统内部的定单号
//    batch_amt 		必填				 成功付款金额
//    batch_num 		必填 				成功付款数量
//    detail_data		必填 				付款明细,单笔数据集里面按照“商户流水号^收款人帐号^收款人 姓名^付款金额^付款状态”来组织数据，每条整数据间用“|”符号分隔，付款状态 S 表示付款成功
//    ext_param1 		必填				 商家数据包，原样返回
//    sign	 			必填 				MD5 签名结果
    
    private static final String  ret_code  = "ret_code"; 
    private static final String  ret_msg   = "ret_msg"; 
    private static final String  agent_id  = "agent_id"; 
    private static final String  hy_bill_no= "hy_bill_no"; 
    private static final String  status= "status"; 
    private static final String  batch_no= "batch_no"; 
    private static final String  batch_amt= "batch_amt"; 
    private static final String  batch_num= "batch_num"; 
    private static final String  detail_data= "detail_data"; 
    private static final String  ext_param1= "ext_param1"; 
    private static final String  key= "key"; 
    private static final String  sign= "sign"; 

    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(batch_no);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[汇天付代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        String signStr=String.format("%s%s%s%s%s%s%s%s%s%s%s",
        		ret_code+"="+payParam.get(ret_code)+"&",
        		ret_msg+"="+payParam.get(ret_msg)+"&",
        		agent_id+"="+payParam.get(agent_id)+"&",
        		hy_bill_no+"="+payParam.get(hy_bill_no)+"&",
        		status+"="+payParam.get(status)+"&",
        		batch_no+"="+payParam.get(batch_no)+"&",
        		batch_amt+"="+payParam.get(batch_amt)+"&",
        		batch_num+"="+payParam.get(batch_num)+"&",
        		detail_data+"="+payParam.get(detail_data)+"&",
        		ext_param1+"="+payParam.get(ext_param1)+"&",
        		key+"="+channelWrapper.getAPI_KEY()
        );
        
        String pay_md5sign = HandlerUtil.getMD5UpperCase(signStr.toLowerCase()).toLowerCase();
        log.debug("2.[汇天付代付]-[代付回调]-自建签名：{}",pay_md5sign);
        return pay_md5sign;
    }



    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
        boolean checkResult = false;
        String responseAmount =  HandlerUtil.getFen(api_response_params.get(batch_amt));
        boolean checkAmount = amountDb.equalsIgnoreCase(responseAmount);
        if (checkAmount)   checkResult = true;
        log.debug("3.[汇天付代付]-[代付回调]-验证回调金额：{}",checkResult);
        return checkResult;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        boolean result = api_response_params.get(sign).equalsIgnoreCase(signMd5);
        log.debug("4.[汇天付代付]-[代付回调]-验证第三方签名：{}",result);
        return result;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[汇天付代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    //回调订单状态
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
        PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        if(api_response_params.containsKey(status)){
           if( "-1".indexOf(api_response_params.get(status))!=-1) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
           if( "0".indexOf(api_response_params.get(status))!=-1) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
           if( "1".indexOf(api_response_params.get(status))!=-1) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
        }
        log.debug("6.[汇天付代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }





}