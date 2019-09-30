package dc.pay.business.bsdaifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.DaifuResponseHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.ResponseDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

@Slf4j
@ResponseDaifuHandler("BSDAIFU")
public final class BSDaiFuResponseHandler extends DaifuResponseHandler {
    private static final String RESPONSE_PAY_MSG = stringResponsePayMsg("0");


      private static final String   order="order";
      private static final String   transData="transData";

      private static final String   resultFlagDesc="resultFlagDesc";    // "成功",
      private static final String   respDesc="respDesc";    // "成功",
      private static final String   tranTime="tranTime";    // "20190311112707",
      private static final String   orderNo="orderNo";    // "20190311112657733501",
      private static final String   bsSerial="bsSerial";    // "155227482000636140",
      private static final String   resultFlag="resultFlag";    // "0",
      private static final String   respCode="respCode";    // "0000",
      private static final String   tranFee="tranFee";    // "100"





    @Override
    public String processForOrderId(Map<String, String> API_RESPONSE_PARAMS) throws PayException {
        String ordernumberR = API_RESPONSE_PARAMS.get(order);
        if (StringUtils.isBlank(ordernumberR))  throw new PayException(SERVER_MSG.RESPONSE_PAY_RESULT_ERROR);
        log.debug("1.[BS代付]-[代付回调]-获取回调订单号：{}",ordernumberR);
        return ordernumberR;
    }


    @Override
    protected String buildPaySign(Map<String, String> payParam, String api_key) throws PayException {
        return TRUE;
    }



    @Override
    protected boolean checkAmount(Map<String, String> api_response_params, String amountDb) throws PayException {
       // JSONObject resParams =  getParams(api_response_params);
       // boolean checkResult = false;
       // String responseAmount = resParams.getString(tranFee);
       // boolean checkAmount =   HandlerUtil.isRightAmount(amountDb,responseAmount,"100");//第三方回调金额差额1元内
       // if (checkAmount)   checkResult = true;
       // log.debug("3.[BS代付]-[代付回调]-验证回调金额：{}",checkResult);
        return true;
    }

    //检查回调签名
    @Override
    protected boolean checkSignMd5(Map<String, String> api_response_params, String signMd5) {
        return true;
    }


    //响应回调的内容
    @Override
    protected String responseSuccess() {
        log.debug("5.[BS代付]-[代付回调]-响应第三方内容：{}",RESPONSE_PAY_MSG);
        return RESPONSE_PAY_MSG;
    }



    //回调订单状态
    @Override
    protected PayEumeration.DAIFU_RESULT  resOrderStatus(Map<String, String> api_response_params) throws PayException {
        PayEumeration.DAIFU_RESULT  orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        JSONObject jsonObj =  getParams(api_response_params);
        if(jsonObj==null) return orderStatus;
        if(HandlerUtil.valJsonObj(jsonObj,"respCode","0000") && HandlerUtil.valJsonObj(jsonObj,"resultFlag","0")) orderStatus = PayEumeration.DAIFU_RESULT.SUCCESS;
        if(HandlerUtil.valJsonObj(jsonObj,"respCode","0000") && HandlerUtil.valJsonObj(jsonObj,"resultFlag","1")) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
        if(HandlerUtil.valJsonObj(jsonObj,"respCode","0000") && HandlerUtil.valJsonObj(jsonObj,"resultFlag","2")) orderStatus = PayEumeration.DAIFU_RESULT.PAYING;
        if(HandlerUtil.valJsonObj(jsonObj,"respCode","0000") && HandlerUtil.valJsonObj(jsonObj,"resultFlag","3")) orderStatus = PayEumeration.DAIFU_RESULT.ERROR;
        if(HandlerUtil.valJsonObj(jsonObj,"respCode","0001","0002","0003","0004","0005")) orderStatus = PayEumeration.DAIFU_RESULT.UNKNOW;
        log.debug("6.[BS代付]-[代付回调]-订单状态：{}",orderStatus);
        return orderStatus;
    }




    private JSONObject getParams(Map<String, String> api_response_params) throws PayException {
        if(null==api_response_params || !api_response_params.containsKey(transData) || StringUtils.isBlank(api_response_params.get(transData))) throw new PayException("第三方返回结果错误。"+JSON.toJSONString(api_response_params));
        JSONObject jsonObj = new JSONObject();
        String decrypt =api_response_params.get(transData);
        try{
            jsonObj = JSON.parseObject(decrypt);
        }catch (Exception e){
            try {
                decrypt = SecurityUtils.decrypt(decrypt, channelWrapper.getAPI_KEY().split("&")[2]);
                jsonObj =JSON.parseObject(decrypt);
            } catch (Exception e1) {
                throw new PayException("请检查后台配置的商户RSA私钥是否正确。"+decrypt);
            }
        }
        return jsonObj;
    }



}