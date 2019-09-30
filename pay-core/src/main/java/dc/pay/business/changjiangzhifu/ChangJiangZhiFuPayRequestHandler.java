package dc.pay.business.changjiangzhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.ValidateUtil;


/**
 * @author sunny
 * 04 09, 2019
 */
@RequestPayHandler("CHANGJIANGZHIFU")
public final class ChangJiangZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(ChangJiangZhiFuPayRequestHandler.class);

//    段名				变量名				必填				类型				示例值			描述
//    商户ID				pid					是				int				10003	
//    支付方式			type				是				string			alipay2	可选的参数是：alipay2（支付宝）、wechat2（微信）、alipay2qr（支付宝，只提供json数据）、wechat2qr（微信，只提供json数据）。
//    商户订单号			out_trade_no		是				string			1530844815	该订单号在同步或异步地址中原样返回
//    异步通知地址		notify_url			是				string			http://www.example.com/notify_url.php	服务器异步通知地址
//    同步通知地址		return_url			是				string			http://www.example.com/return_url.php	页面跳转通知地址
//    商品名称			name				是				string			VIP会员	
//    附加数据			attach				是				string			说明	附加数据，在查询API和支付通知中原样返回，该字段主要用于商户携带订单的自定义数据
//    商品金额			money				是				string			0.01	
//    签名字符串			sign				是				string			202cb962ac59075b964b07152d234b70	签名算法与请看下面示例
//    签名类型			sign_type			是				string			MD5	默认为MD5，不参与签名

  private static final String pid               	="pid";
  private static final String type           		="type";
  private static final String out_trade_no          ="out_trade_no";
  private static final String notify_url            ="notify_url";
  private static final String return_url            ="return_url";
  private static final String name              	="name";
  private static final String attach            	="attach";
  private static final String money           		="money";
  private static final String sign_type            	="sign_type";
  private static final String sitename            	="sitename";
  private static final String sign                	="sign";
  private static final String key                 	="key";

  @Override
  protected Map<String, String> buildPayParam() throws PayException {
      Map<String, String> payParam = new TreeMap<String, String>() {
          {
              put(pid, channelWrapper.getAPI_MEMBERID());
              put(out_trade_no,channelWrapper.getAPI_ORDER_ID());
              put(money,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
              put(notify_url,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
              put(type,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
              put(return_url,channelWrapper.getAPI_WEB_URL());
              put(sign_type,"MD5");
              put(name,channelWrapper.getAPI_ORDER_ID());
              put(attach,channelWrapper.getAPI_ORDER_ID());
              put(sitename,channelWrapper.getAPI_ORDER_ID());
          }
      };
      log.debug("[长江支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
      return payParam;
  }

   protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
      String signSrc=String.format("%s%s%s%s%s%s%s%s%s%s",
    		  attach+"="+api_response_params.get(attach)+"&",
    		  money+"="+api_response_params.get(money)+"&",
    		  name+"="+api_response_params.get(name)+"&",
    		  notify_url+"="+api_response_params.get(notify_url)+"&",
    		  out_trade_no+"="+api_response_params.get(out_trade_no)+"&",
    		  pid+"="+api_response_params.get(pid)+"&",
    		  return_url+"="+api_response_params.get(return_url)+"&",
    		  sitename+"="+api_response_params.get(sitename)+"&",
    		  type+"="+api_response_params.get(type),
    		  channelWrapper.getAPI_KEY()
      );
      String paramsStr = signSrc.toString();
      String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
      log.debug("[长江支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
      return signMD5;
  }

  protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
      payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
      HashMap<String, String> result = Maps.newHashMap();
      ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
      result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString());  //.replace("method='post'","method='get'"));
      payResultList.add(result);
      log.debug("[长江支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
      log.debug("[长江支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
      return requestPayResult;
  }
}