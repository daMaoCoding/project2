package dc.pay.business.yinghuazhifu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

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
 * 
 * 
 * @author sunny
 * Dec 18, 2018
 */
@RequestPayHandler("YINGHUAZHIFU")
public final class YingHuaZhiFuPayRequestHandler extends PayRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YingHuaZhiFuPayRequestHandler.class);

//    参数			含义					长度				是否非空				备注
//    spid			商户代码				4				非空	网众未来统一分配的商户代码spid
//    orderid		商户订单号			小于100			非空	该订单号由商户系统生成。唯一确定一条订单。（用数字或字母或“_”）
//    mz			订单金额				10				非空	单位为元，可支持两位小数点
//    uid			用户ID				50				非空	能标识用户的标志。
//    spsuc			返回商户页面			128				非空	商户端显示成功充值地址   格式：http://或https：//开头）
//    ordertype		支付类型				1				非空	支付类型（1、支付宝/2、微信；3、网银支付；4、QQ支付； 5、快捷支付；6、京东钱包；7、银联钱包；8、聚合支付）
//    interfacetype	开通类型				1				非空	接口类型（1：扫码；2 :公众号; 3：App；4：WAP；5：服务窗；6：直连）WAP兼容问题请查看第三章
//    productname	商品名称				50				非空	商品名称
//    sign			MD5加密信息			32				非空	MD5数据（加密然后转大写）
//    notifyurl		异步回调地址			256				非空	商户的异步回调通知地址  格式：http://或https：//开头）
//    banktype		银行卡类型			10				非空	1，借记卡；2，信用卡； 

    private static final String spid               	="spid";
    private static final String orderid           	="orderid";
    private static final String mz           		="mz";
    private static final String uid           		="uid";
    private static final String spsuc          		="spsuc";
    private static final String ordertype           ="ordertype";
    private static final String interfacetype       ="interfacetype";
    private static final String productname         ="productname";
    private static final String notifyurl         	="notifyurl";
    private static final String banktype            ="banktype";
    private static final String sign                ="sign";
    private static final String key                 ="key";
    


    @Override
    protected Map<String, String> buildPayParam() throws PayException {
        Map<String, String> payParam = new TreeMap<String, String>() {
            {
                put(spid, channelWrapper.getAPI_MEMBERID());
                put(orderid,channelWrapper.getAPI_ORDER_ID());
                put(mz,HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
                put(notifyurl,channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
                put(ordertype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
                put(interfacetype,channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[1]);
                put(spsuc,channelWrapper.getAPI_WEB_URL());
                put(productname,channelWrapper.getAPI_ORDER_ID());
                put(uid,channelWrapper.getAPI_MEMBERID());
                put(banktype,"1");
            }
        };
        log.debug("[盈华支付]-[请求支付]-1.组装请求参数完成：{}" ,JSON.toJSONString(payParam));
        return payParam;
    }

     protected String buildPaySign(Map<String,String> api_response_params) throws PayException {
        String signSrc=String.format("%s%s%s%s%s%s%s", 
        		api_response_params.get(spid),
        		api_response_params.get(orderid),
        		channelWrapper.getAPI_KEY(),
        		api_response_params.get(mz),
        		api_response_params.get(spsuc),
        		api_response_params.get(ordertype),
        		api_response_params.get(interfacetype)
        );
        String paramsStr = signSrc.toString();
        String signMD5 = HandlerUtil.getMD5UpperCase(paramsStr);
        log.debug("[盈华支付]-[请求支付]-2.生成加密URL签名完成：{}" ,JSON.toJSONString(signMD5));
        return signMD5;
    }

    protected List<Map<String, String>> sendRequestGetResult(Map<String, String> payParam, String pay_md5sign) throws PayException {
        payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);
        HashMap<String, String> result = Maps.newHashMap();
        ArrayList<Map<String, String>> payResultList = Lists.newArrayList();
        result.put(HTMLCONTEXT, HandlerUtil.getHtmlContent(channelWrapper.getAPI_CHANNEL_BANK_URL(),payParam).toString().replace("method='post'","method='get'"));  //.replace("method='post'","method='get'"));
        payResultList.add(result);
        log.debug("[盈华支付]-[请求支付]-3.发送支付请求，及获取支付请求结果成功：{}",JSON.toJSONString(payResultList));
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
        log.debug("[盈华支付]-[请求支付]-4.处理请求响应成功：{}",JSON.toJSONString(requestPayResult));
        return requestPayResult;
    }
}