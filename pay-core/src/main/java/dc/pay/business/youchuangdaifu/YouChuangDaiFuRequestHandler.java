package dc.pay.business.youchuangdaifu;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.RequestDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

/**
 * @author Cobby
 * July 06, 2019
 */
@RequestDaifuHandler("YOUCHUANGDAIFU")
public final class YouChuangDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YouChuangDaiFuRequestHandler.class);

    //请求代付&查询代付-参数
    private static final String mer_id         = "mer_id";        //    商户号    Y       支付分配给商户的 mer_id
    private static final String timestamp      = "timestamp";     //    请求时间  Y    时间戳,格式yyyy-MM-dd HH:mm:ss
    private static final String terminal       = "terminal";      //    终端类型  Y    默认PC  详见4.1.2 支付类型表
    private static final String version        = "version";       //    版本号    Y    01
    private static final String amount         = "amount";        //    金额      Y    代付金额(单位 分)
    private static final String businessnumber = "businessnumber";//    业务订单号    Y    需保证不重复
    private static final String bankcardnumber = "bankcardnumber";//    银行卡号码    Y
    private static final String bankcardname   = "bankcardname";  //    持卡人姓名    Y
    private static final String bankname       = "bankname";      //    开户行名称    Y    银行名称(银行卡类型为企业是必填，个人跨行也需要必填)例:中国工商银行
    private static final String back_url       = "back_url";      //    异步回调url   Y    付款状态通知回调地址
    private static final String cash_type      = "cash_type";     //    提现类型    N    可空（空为对私代付），1=>对私代付 2=>对公代付 3为易收付代付
    private static final String user_id        = "user_id";       //    用户标识    N    商户下用户唯一标识，不可重复，cash_type 为2时必填
    private static final String sign_type      = "sign_type";     //    签名算法类型 Y    默 认 md5


    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String, String> payParam, Map<String, String> details) throws PayException {
        try {
            String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
            if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
                log.error("[优创代付]-[代付]-“代付通道商号”输入数据格式为【中间使用&分隔】：商户号&提现类型(提现类型 1=>对私代付 2=>对公代付 3=>为易收付代付");
                throw new PayException("[优创代付]-[代付]-“代付通道商号”输入数据格式为【中间使用&分隔】：商户号&提现类型(提现类型 1=>对私代付 2=>对公代付 3=>为易收付代付");
            }
            //组装参数
            payParam.put(mer_id, channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(timestamp, DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString));
            payParam.put(terminal, "PC");
            payParam.put(version, "01");
            payParam.put(amount, channelWrapper.getAPI_AMOUNT());
            payParam.put(businessnumber, channelWrapper.getAPI_ORDER_ID());
            payParam.put(bankcardnumber, channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
            payParam.put(bankcardname, channelWrapper.getAPI_CUSTOMER_NAME());
            payParam.put(bankname, channelWrapper.getAPI_CUSTOMER_BANK_NAME());
            payParam.put(back_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(cash_type, channelWrapper.getAPI_MEMBERID().split("&")[1]);
            if ("2".equalsIgnoreCase(channelWrapper.getAPI_MEMBERID().split("&")[1])) {
                payParam.put(user_id, "user" + HandlerUtil.randomStr(6));
            }
            payParam.put(sign_type, "md5");

            //生成md5
            String        pay_md5sign = null;
            List          paramKeys   = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder sb          = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if (StringUtils.isBlank(payParam.get(paramKeys.get(i))) || sign_type.equalsIgnoreCase(paramKeys.get(i).toString()))
                    continue;
                sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
            sb.append(channelWrapper.getAPI_KEY());
            String signStr = sb.toString(); //.replaceFirst("&key=","")
            pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

            //发送请求获取结果
            String url       = channelWrapper.getAPI_CHANNEL_BANK_URL() + "/pay.bank.to";
            String resultStr = RestTemplateUtil.postJson(url, JSON.toJSONString(payParam)).trim();
//              String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(channelWrapper.getAPI_CHANNEL_BANK_URL().split(",")[0], payParam, String.class, HttpMethod.POST);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果


            if (StringUtils.isNotBlank(resultStr)) {
                return getDaifuResult(resultStr, false);
            } else {
                throw new PayException(EMPTYRESPONSE);
            }


            //结束

        } catch (Exception e) {
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
    protected PayEumeration.DAIFU_RESULT queryDaifuAllInOne(Map<String, String> payParam, Map<String, String> details) throws PayException {
        if (1 == 2) throw new PayException("[优创代付][代付][查询订单状态]该功能未完成。");
        try {
            //组装参数
            payParam.put(mer_id, channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(timestamp, DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString));
            payParam.put(terminal, "PC");
            payParam.put(version, "01");
            payParam.put(businessnumber, channelWrapper.getAPI_ORDER_ID());
            payParam.put(sign_type, "md5");

            //生成md5
            String        pay_md5sign = null;
            List          paramKeys   = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder sb          = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if (StringUtils.isBlank(payParam.get(paramKeys.get(i))) || sign_type.equalsIgnoreCase(paramKeys.get(i).toString()))  //
                    continue;
                sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
            sb.append(channelWrapper.getAPI_KEY());
            String signStr = sb.toString(); //.replaceFirst("&key=","")
            pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);


            //发送请求获取结果
            String url       = channelWrapper.getAPI_CHANNEL_BANK_URL() + "/pay.order.query";
            String resultStr = RestTemplateUtil.postStr(url, JSON.toJSONString(payParam), MediaType.APPLICATION_JSON_VALUE).trim();
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            if (StringUtils.isNotBlank(resultStr)) {
                return getDaifuResult(resultStr, true);
            } else {
                throw new PayException(EMPTYRESPONSE);
            }

        } catch (Exception e) {
            throw new PayException(e.getMessage());
        }

    }


    //查询代付-余额，正常返回余额单位分，否则抛异常
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String, String> payParam, Map<String, String> details) throws PayException {
        if (1 == 2) throw new PayException("[优创代付][代付余额查询]该功能未完成。");

        try {
//          Is_ysf  是否易收付（收付一体）   Y   1是易收付(可查询每条单独的通道的易收付余额)，0不是易收付（查询的是商户总余额）
            //组装参数
            payParam.put(mer_id, channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(timestamp, DateUtil.formatDateTimeStrByParam(DateUtil.dateTimeString));
            payParam.put(terminal, "PC");
            payParam.put(version, "01");
            payParam.put("Is_ysf", "0");
            payParam.put(sign_type, "md5");


            //生成md5
            String        pay_md5sign = null;
            List          paramKeys   = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder sb          = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if (StringUtils.isBlank(payParam.get(paramKeys.get(i))) || sign_type.equalsIgnoreCase(paramKeys.get(i).toString()))  //
                    continue;
                sb.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
            }
            sb.append(channelWrapper.getAPI_KEY());
            String signStr = sb.toString(); //.replaceFirst("&key=","")
            pay_md5sign = HandlerUtil.getMD5UpperCase(signStr);
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);


            //发送请求获取结果
            String url       = channelWrapper.getAPI_CHANNEL_BANK_URL() + "/pay.balance.query";
            String resultStr = RestTemplateUtil.postStr(url, JSON.toJSONString(payParam), MediaType.APPLICATION_JSON_VALUE).trim();
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
//          result    返回码   请求结果标志    success：请求授理成功，不代表交易成功   error：业务失败   exception：网络异常失败，不代表交易失败
            JSONObject jsonObj = JSON.parseObject(resultStr);
            if (HandlerUtil.valJsonObj(jsonObj, "result", "success")) {
                String balance = HandlerUtil.valJsonObjInSideJsonObj(jsonObj, "data", "account_balance");
                return Long.parseLong(HandlerUtil.getFen(balance));
            } else {
                throw new PayException(resultStr);
            }
        } catch (Exception e) {
            log.error("[优创代付][代付余额查询]出错,错误消息：{},参数：{}", e.getMessage(), JSON.toJSONString(payParam), e);
            throw new PayException(String.format("[优创代付][代付余额查询]出错,错误:%s", e.getMessage()));
        }

    }


    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr, boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);
        JSONObject jsonObject;
        try {
            jsonObject = JSONObject.parseObject(jsonObj.getString("data"));
        } catch (Exception e) {
            e.printStackTrace();
            log.error("[优创代付]-[请求代付]-1.1.发送代付请求，及获取代付请求结果：" + JSON.toJSONString(resultStr) + "代付订单号：" + channelWrapper.getAPI_ORDER_ID() + " ,通道：" + channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG());
            throw new PayException(resultStr);
        }
//        result   返回码    请求结果标志  success：请求授理成功，不代表交易成功         error：业务失败
//        data 参数  status    交易状态      成功/处理中/失败
        if (!isQuery) {
            if (HandlerUtil.valJsonObj(jsonObj, "result", "error")) return PayEumeration.DAIFU_RESULT.ERROR;
            if (HandlerUtil.valJsonObj(jsonObj, "result", "success") && HandlerUtil.valJsonObj(jsonObject, "status", "处理中"))
                return PayEumeration.DAIFU_RESULT.PAYING;
            if (HandlerUtil.valJsonObj(jsonObj, "result", "success") && HandlerUtil.valJsonObj(jsonObject, "status", "失败"))
                return PayEumeration.DAIFU_RESULT.ERROR;
            if (HandlerUtil.valJsonObj(jsonObj, "result", "success") && HandlerUtil.valJsonObj(jsonObject, "status", "成功"))
                return PayEumeration.DAIFU_RESULT.PAYING;
            return PayEumeration.DAIFU_RESULT.UNKNOW;
        } else {
//         result    返回码    请求结果标志 success：请求授理成功，不代表交易成功 error：业务失败 exception：网络异常失败，不代表交易失败
//         status    交易状态    成功/处理中/失败
            if (resultStr.contains("订单信息不存在")) return PayEumeration.DAIFU_RESULT.ERROR;
            if (HandlerUtil.valJsonObj(jsonObj, "result", "error")) return PayEumeration.DAIFU_RESULT.ERROR;
            if (HandlerUtil.valJsonObj(jsonObj, "result", "exception")) return PayEumeration.DAIFU_RESULT.PAYING;
            if (HandlerUtil.valJsonObj(jsonObj, "result", "success") && HandlerUtil.valJsonObj(jsonObject, "status", "处理中"))
                return PayEumeration.DAIFU_RESULT.PAYING;
            if (HandlerUtil.valJsonObj(jsonObj, "result", "success") && HandlerUtil.valJsonObj(jsonObject, "status", "失败"))
                return PayEumeration.DAIFU_RESULT.ERROR;
            if (HandlerUtil.valJsonObj(jsonObj, "result", "success") && HandlerUtil.valJsonObj(jsonObject, "status", "成功"))
                return PayEumeration.DAIFU_RESULT.SUCCESS;
            new PayException(resultStr);
        }
        return PayEumeration.DAIFU_RESULT.UNKNOW;
    }


}