package dc.pay.business.feixiangzhifu;

/**
 * ************************
 * @author tony 3556239829
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.base.processor.PayException;
import dc.pay.base.processor.PayRequestHandler;
import dc.pay.business.RequestPayResult;
import dc.pay.config.annotation.RequestPayHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.constant.SERVER_MSG;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;
import org.apache.commons.lang.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequestPayHandler("FEIXIANGZHIFU")
public final class FeiXiangZhiFuPayRequestHandler extends PayRequestHandler {
    //private static final Logger log = LoggerFactory.getLogger(GeFuPayRequestHandler.class);


    private  static final String	  TRDE_CODE = "TRDE_CODE";   //	   交易码   STRING(5)	否	20001
    private  static final String	  PRT_CODE = "PRT_CODE";   //	   机构号   STRING(10)	否	受理方预分配的渠道机构标识
    private  static final String	  VER_NO = "VER_NO";   //	   版本号   STRING(4)	否	1.0
    private  static final String	  MERC_ID = "MERC_ID";   //	   商户号   STRING(15)	否	受理方分配的商户标识
    private  static final String	  BIZ_CODE = "BIZ_CODE";   //	   业务编码   STRING(4)	否	见附录
    private  static final String	  ORDER_NO = "ORDER_NO";   //	   订单号   STRING(32)	否	对应机构系统唯一
    private  static final String	  TXN_AMT = "TXN_AMT";   //	   金额   STRING(10)	否	单位为元
    private  static final String	  PRO_DESC = "PRO_DESC";   //	   订单描述   STRING(128)	否
    private  static final String	  BNK_CD = "BNK_CD";   //	   银行编码   STRING(3)	是	网银必需，例：平安银行对应为307
    private  static final String	  ACC_TYP = "ACC_TYP";   //	   卡类型   STRING(1)	是	网银需要 0-借记 1-贷记
    private  static final String	  CLIENT_IP = "CLIENT_IP";   //	   客户端IP地址   STRING(15)	是	支付终端IP上送
    private  static final String	  NOTIFY_URL = "NOTIFY_URL";   //	   异步通知地址   STRING(150)	否
    private  static final String	  NON_STR = "NON_STR";   //	   随机字符串   STRING(32)	否
    private  static final String	  TM_SMP = "TM_SMP";   //	   时间戳   STRING(14)	否
    private  static final String	  SIGN_TYP = "SIGN_TYP";   //	   签名类型   STRING(5)	否
    private  static final String	  SIGN_DAT = "SIGN_DAT";   //	   签名数据   STRING(32)	否




    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        if(!channelWrapper.getAPI_MEMBERID().contains("&")){
            throw new PayException("商户号格式错误，正确格式请使用&符号链接[机构代码]和[商户号],如：1000168***&834651047****");
        }


        Map<String, String> payParam = Maps.newHashMap();
        if(1==1){  //HandlerUtil.isWY(channelWrapper)
            payParam.put(TRDE_CODE,"20001");
            payParam.put(PRT_CODE,channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(VER_NO,"1.0");
            payParam.put(MERC_ID,channelWrapper.getAPI_MEMBERID().split("&")[1]);

            if(HandlerUtil.isWY(channelWrapper)){
                payParam.put(BIZ_CODE,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                payParam.put(BNK_CD,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                payParam.put(ACC_TYP,"0" );
            }else{
                payParam.put(BIZ_CODE,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            }


            payParam.put(ORDER_NO,channelWrapper.getAPI_ORDER_ID());
            payParam.put(TXN_AMT,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()) );
            payParam.put(PRO_DESC, channelWrapper.getAPI_ORDER_ID());
            payParam.put(CLIENT_IP, channelWrapper.getAPI_Client_IP());
            payParam.put(NOTIFY_URL,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL() );
            payParam.put(NON_STR, HandlerUtil.getRandomNumber(10));
            payParam.put(TM_SMP, System.currentTimeMillis()+"");
            payParam.put(SIGN_TYP,"MD5" );
        }

        log.debug("[飞翔支付]-[请求支付]-1.组装请求参数完成：" + JSON.toJSONString(payParam));
        return payParam;
    }



    protected String buildPaySign(Map<String,String> params) throws PayException {
        String pay_md5sign = null;
        List paramKeys = MapUtils.sortMapByKeyAsc(params);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramKeys.size(); i++) {
              if(StringUtils.isBlank(params.get(paramKeys.get(i))) || SIGN_DAT.equalsIgnoreCase(paramKeys.get(i).toString())    )  //
                continue;
            sb.append(paramKeys.get(i)).append("=").append(params.get(paramKeys.get(i))).append("&");
        }
        sb.append("KEY=" + channelWrapper.getAPI_KEY());
        String signStr = sb.toString(); //.replaceFirst("&KEY=","")
        pay_md5sign = HandlerUtil.getMD5UpperCase(signStr).toLowerCase();
        log.debug("[飞翔支付]-[请求支付]-2.生成加密URL签名完成：" + JSON.toJSONString(pay_md5sign));
        return pay_md5sign;
    }


    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        Map result = Maps.newHashMap();
        String resultStr;
        try {
            if (1 == 2 && HandlerUtil.isWY(channelWrapper) && HandlerUtil.isYLKJ(channelWrapper) && HandlerUtil.isWapOrApp(channelWrapper)) {
                result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam).toString().replace("method='post'", "method='post'"));
                payResultList.add(result);
            } else {
/*				
				HtmlPage endHtml = handlerUtil.getEndHtml(channelWrapper.getAPI_CHANNEL_BANK_URL(), channelWrapper.getAPI_ORDER_ID(), payParam);
                String qrContent=null;
                if(null!=endHtml && endHtml.getByXPath("//input[@name='payurl']").size()==1){
                    HtmlInput payUrlInput = (HtmlInput) endHtml.getByXPath("//input[@name='payurl']").get(0);
                    if(payUrlInput!=null ){
                        String qrContentSrc = payUrlInput.getValueAttribute();
                        if(StringUtils.isNotBlank(qrContentSrc))  qrContent = QRCodeUtil.decodeByUrl(qrContentSrc);
                    }
                }
               if(StringUtils.isNotBlank(qrContent)){
                    result.put(QRCONTEXT, qrContent);
                    payResultList.add(result);
                }else {  throw new PayException(endHtml.asXml()); }
				
*/

                resultStr = RestTemplateUtil.postJson(channelWrapper.getAPI_CHANNEL_BANK_URL(), payParam);

                if (StringUtils.isNotBlank(resultStr) && resultStr.contains("<form") && !resultStr.contains("{")) {
                    result.put(HTMLCONTEXT, resultStr);
                    payResultList.add(result);
                } else if (StringUtils.isNotBlank(resultStr)) {

                    JSONObject jsonResultStr = JSON.parseObject(resultStr);
                    if (null != jsonResultStr && jsonResultStr.containsKey("RETURNCODE") && "0000".equalsIgnoreCase(jsonResultStr.getString("RETURNCODE"))) {
                        if (HandlerUtil.isWapOrApp(channelWrapper) && jsonResultStr.containsKey("QR_CODE") && StringUtils.isNotBlank(jsonResultStr.getString("QR_CODE"))) {
                            result.put(JUMPURL, jsonResultStr.getString("QR_CODE"));
                        } else if (HandlerUtil.isWEBWAPAPP_SM(channelWrapper) && jsonResultStr.containsKey("QR_CODE") && StringUtils.isNotBlank(jsonResultStr.getString("QR_CODE"))) {
                            result.put(QRCONTEXT, jsonResultStr.getString("QR_CODE"));
                        } else if (HandlerUtil.isWY(channelWrapper) && jsonResultStr.containsKey("RET_HTML") && StringUtils.isNotBlank(jsonResultStr.getString("RET_HTML"))) {
                            result.put(HTMLCONTEXT, jsonResultStr.getString("RET_HTML"));
                        } else {
                            throw new PayException(resultStr);
                        }
                        payResultList.add(result);

                    } else {
                        throw new PayException(resultStr);
                    }


                } else {
                    throw new PayException(EMPTYRESPONSE);
                }

            }
        } catch (Exception e) {
            log.error("[飞翔支付]3.发送支付请求，及获取支付请求结果出错：", e);
            throw new PayException(e.getMessage(), e);
        }
        log.debug("[飞翔支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}", JSON.toJSONString(payResultList));
        return payResultList;
    }


    protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
        RequestPayResult requestPayResult = new RequestPayResult();
        if (null != resultListMap && !resultListMap.isEmpty()) {
            if (resultListMap.size() == 1) {
                Map<String, String> resultMap = resultListMap.get(0);
                requestPayResult = buildResult(resultMap,channelWrapper,requestPayResult);
            }
            if (ValidateUtil.requestesultValdata(requestPayResult)) {
                requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
            } else {
                throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
            }
        } else {
            throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
        }
        log.debug("[飞翔支付]-[请求支付]-4.处理请求响应成功：" + JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}