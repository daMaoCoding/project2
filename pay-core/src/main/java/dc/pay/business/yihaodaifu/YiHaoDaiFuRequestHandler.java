package dc.pay.business.yihaodaifu;


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
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * @author Cobby
 * July 16, 2019
 */
@RequestDaifuHandler("YIHAODAIFU")
public final class YiHaoDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(YiHaoDaiFuRequestHandler.class);

    //请求代付&查询代付-参数

    private static final String mer_id       = "mer_id";            //商户号     20   商户号   否
    private static final String out_trade_no = "out_trade_no";      //商户订单号  32      否
    private static final String pay_type     = "pay_type";          //支付类型    3   016：代付接口   否
    private static final String total_fee    = "total_fee";         //代付金额    8   单位：分   否
    private static final String acct_name    = "acct_name";         //收款人姓名   32      否
    private static final String acct_no      = "acct_no";           //银行账号    32      否
    private static final String bank_name    = "bank_name";         //开户行名称   32      否
    private static final String cnaps_name   = "cnaps_name";        //开户行支行   32      否
    private static final String notify_url   = "notify_url";        //后台同步URL  100   代付完成后，CP或渠道需要同步支付成功数据，此参数为同步地址，同步参数参见 1.2   是
    private static final String proxy_date   = "proxy_date";        //代付日期      8   格式：20180704，为空默认今天   是
    private static final String nonce_str    = "nonce_str";         //随机字符串    32   不能重复   否
//  private static final String  biz_id            =  "biz_id";            //通道id     11      是，默认0
//  private static final String  bank_code         =  "bank_code";         //支行行号     32      是
//  private static final String  bank_sett_no      =  "bank_sett_no";      //账户联行号   32      是
//  private static final String  certificate_code  =  "certificate_code";  //收款人身份证 18      是
//  private static final String  sub_mer_id        =  "sub_mer_id";        //子商户号   20   子商户号   是
//  private static final String  Sign              =  "Sign";              //签名          32   MD5签名，参考2.2备注  中的签名方法   否


    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String, String> payParam, Map<String, String> details) throws PayException {
        try {

            //组装参数
            payParam.put(mer_id, channelWrapper.getAPI_MEMBERID());
            payParam.put(out_trade_no, channelWrapper.getAPI_ORDER_ID());
            payParam.put(pay_type, "016");
            payParam.put(total_fee, channelWrapper.getAPI_AMOUNT());
            payParam.put(acct_name, channelWrapper.getAPI_CUSTOMER_NAME());
            payParam.put(acct_no, channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
            payParam.put(bank_name, channelWrapper.getAPI_CUSTOMER_BANK_NAME());
            payParam.put(cnaps_name, channelWrapper.getAPI_CUSTOMER_BANK_SUB_BRANCH());
            payParam.put(notify_url, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(proxy_date, DateUtil.formatDateTimeStrByParam("yyyyMMdd"));
            payParam.put(nonce_str, HandlerUtil.randomStr(10));
//            acct_name,acct_no,mer_id,nonce_str,out_trade_no,total_fee
            String paramsStr = String.format("acct_name=%s&acct_no=%s&mer_id=%s&nonce_str=%s&out_trade_no=%s&total_fee=%s&key=%s",
                    payParam.get(acct_name),
                    payParam.get(acct_no),
                    payParam.get(mer_id),
                    payParam.get(nonce_str),
                    payParam.get(out_trade_no),
                    payParam.get(total_fee),
                    channelWrapper.getAPI_KEY());
            String pay_md5sign = HandlerUtil.getMD5UpperCase(paramsStr).toLowerCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

            String url = channelWrapper.getAPI_CHANNEL_BANK_URL() + "/proxy_server/proxy_api";
            //发送请求获取结果
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(url, payParam, String.class, HttpMethod.GET);
            resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
//                addQueryDaifuOrderJob(channelWrapper.getAPI_ORDER_ID());  //自动查询 (如有回调则无需 自动查询)

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
        if (1 == 2) throw new PayException("[壹号代付][代付][查询订单状态]该功能未完成。");
        try {


            //组装参数
            payParam.put(out_trade_no, channelWrapper.getAPI_ORDER_ID());


            //生成md5
            StringBuilder sb   = new StringBuilder((payParam.size() + 1) * 10);
            List<String>  keys = new ArrayList<String>(payParam.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                sb.append(key).append("=");
                sb.append(payParam.get(key));
                sb.append("&");
            }
//            sb.setLength(sb.length() - 1);
            String preStr      = sb.toString() + "key=" + channelWrapper.getAPI_KEY();
            String pay_md5sign = HandlerUtil.getMD5UpperCase(preStr).toUpperCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

            //发送请求获取结果
            String url       = channelWrapper.getAPI_CHANNEL_BANK_URL() + "/proxy_server/proxy_query";
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(url, payParam, String.class, HttpMethod.GET);
            resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
            //发送请求获取结果
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
        if (1 == 2) throw new PayException("[壹号代付][代付余额查询]该功能未完成。");


        try {
            //组装参数
            payParam.put(mer_id, channelWrapper.getAPI_MEMBERID());
            payParam.put("biz_id", "3");

            //生成md5
            StringBuilder sb   = new StringBuilder((payParam.size() + 1) * 10);
            List<String>  keys = new ArrayList<String>(payParam.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                sb.append(key).append("=");
                sb.append(payParam.get(key));
                sb.append("&");
            }
//            sb.setLength(sb.length() - 1);
            String preStr      = sb.toString() + "key=" + channelWrapper.getAPI_KEY();
            String pay_md5sign = HandlerUtil.getMD5UpperCase(preStr).toLowerCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

            //发送请求获取结果
            String url = channelWrapper.getAPI_CHANNEL_BANK_URL() + "/proxy_server/query_balance";
            System.out.println("请求参数:" + JSON.toJSONString(payParam));
            String resultStr = RestTemplateUtil.sendByRestTemplateRedirect(url, payParam, String.class, HttpMethod.GET);
            resultStr = new String(resultStr.getBytes("ISO-8859-1"), "UTF-8");
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            JSONObject jsonObj = JSON.parseObject(resultStr);//请求状态(0000 代表请求成功，其他都是失败)
            if (HandlerUtil.valJsonObj(jsonObj, "resp_code", "0000") && jsonObj.containsKey("balance")) {
                String balance = jsonObj.getString("balance");
                return Long.parseLong(HandlerUtil.getFen(balance));
            } else {
                throw new PayException(resultStr);
            }
        } catch (Exception e) {
            log.error("[壹号代付][代付余额查询]出错,错误消息：{},参数：{}", e.getMessage(), JSON.toJSONString(payParam), e);
            throw new PayException(String.format("[壹号代付][代付余额查询]出错,错误:%s", e.getMessage()));
        }

    }


    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr, boolean isQuery) throws PayException {
        /**
         * 3返回码表
         * 返回码(resp_code)    描述(resp_msg)
         * 0000    请求成功
         */
        JSONObject jsonObj = JSON.parseObject(resultStr);
        if (!isQuery) {
            if (HandlerUtil.valJsonObj(jsonObj, "resp_code", "0000")) return PayEumeration.DAIFU_RESULT.PAYING;
            if (!HandlerUtil.valJsonObj(jsonObj, "resp_code", "0000")) return PayEumeration.DAIFU_RESULT.ERROR;
            return PayEumeration.DAIFU_RESULT.ERROR;
        } else {
            // 0    代付成功              1    代付处理中              2    代付失败              -1   订单不存在
            if (resultStr.contains("订单不存在")) return PayEumeration.DAIFU_RESULT.ERROR;
            if (HandlerUtil.valJsonObj(jsonObj, "resp_code", "0")) return PayEumeration.DAIFU_RESULT.SUCCESS;
            if (HandlerUtil.valJsonObj(jsonObj, "resp_code", "1")) return PayEumeration.DAIFU_RESULT.PAYING;
            if (HandlerUtil.valJsonObj(jsonObj, "resp_code", "2", "-1")) return PayEumeration.DAIFU_RESULT.ERROR;
            new PayException(resultStr);
        }
        return PayEumeration.DAIFU_RESULT.UNKNOW;
    }


}