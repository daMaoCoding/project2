package dc.pay.business.lufengzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import dc.pay.utils.Sha1Util;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.ValidateUtil;


/**
 * @author sunny
 * 06 18, 2019
 */
@RequestPayHandler("LUFENGZHIFU")
public final class LuFengZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(LuFengZhiFuPayRequestHandler.class);

//    参数名				参数			可空				加入签名				说明
//    商户ID				mch_id		N				Y				商户id，由商户后台分配
//    支付分区ID			zone_id		N				Y				支付分区ID,商户后台添加分区，自动生成，
//    银行类型			trade_type	N				Y				支付类型，具体请参考附录1
//    金额				total_fee	N				Y				单位元（人民币），2位小数，最小支付金额为10.00
//    商户订单号			out_trade_no	N			Y				商户系统订单号，该订单号将作为接口的返回数据。该值需在商户系统内唯一，系统暂时不检查该值是否唯一
//    备注消息			remark		Y				N				备注信息，下行中会原样返回。若该值包含中文，请注意编码,不超过300字符
//    MD5签名			sign		N				-				32位小写MD5签名值，utf-8编码

  private static final String mch_id               ="mch_id";
  private static final String zone_id              ="zone_id";
  private static final String trade_type           ="trade_type";
  private static final String total_fee            ="total_fee";
  private static final String out_trade_no         ="out_trade_no";
  private static final String remark               ="remark";
  
  private static final String signType            ="signType";
  private static final String sign                ="sign";
  private static final String key                 ="key";
  
  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(mch_id, channelWrapper.getAPI_MEMBERID().split("&")[0]);
              put(zone_id, channelWrapper.getAPI_MEMBERID().split("&")[1]);
              put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
              put(total_fee,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(trade_type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
          }
      };
      log.debug("[禄丰支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      List paramKeys = MapUtils.sortMapByKeyAsc(api_response_params);
      StringBuilder signSrc = new StringBuilder();
      for (int i = 0; i < paramKeys.size(); i++) {
          if (StringUtils.isNotBlank(api_response_params.get(paramKeys.get(i)))) {
          	signSrc.append(paramKeys.get(i)).append("=").append(api_response_params.get(paramKeys.get(i))).append("&");
          }
      }
      //最后一个&转换成#
      //signSrc.replace(signSrc.lastIndexOf("&"), signSrc.lastIndexOf("&") + 1, "#" );
      signSrc.append(channelWrapper.getAPI_KEY());
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[禄丰支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));  //.replace("method='post'","method='get'"));
      payResultList.add(result);
      log.debug("[禄丰支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
      return payResultList;
  }

  protected RequestPayResult buildResult(List<Map<String, String>> resultListMap) throws PayException {
      RequestPayResult requestPayResult = new RequestPayResult();
      if (null != resultListMap && !resultListMap.isEmpty()) {
          if (resultListMap.size() == 1) {
              Map<String, String> resultMap = resultListMap.get(0);
              requestPayResult=  buildResult(resultMap, channelWrapper,requestPayResult);
          }
          if (ValidateUtil.requestesultValdata(requestPayResult)) {
              requestPayResult.setRequestPayCode(PayEumeration.REQUEST_PAY_CODE.SUCCESS.getCodeValue());
          } else {
              throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT_VERIFICATION_ERROR);
          }
      } else {
          throw new PayException(SERVER_MSG.REQUEST_PAY_RESULT__ERROR);
      }
      log.debug("[禄丰支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}