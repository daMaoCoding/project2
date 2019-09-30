package dc.pay.business.huofenghuangdaifu;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.RequestDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.UnicodeUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Cobby
 * June 19, 2019
 */
@RequestDaifuHandler("HUOFENGHUANGDAIFU")
public final class HuoFengHuangDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(HuoFengHuangDaiFuRequestHandler.class);

    //请求代付&查询代付-参数
    private static final String mchid        = "mchid";       //    商户号    是    是    平台分配商户号
    private static final String out_trade_no = "out_trade_no";//    订单号    是    是    保证唯一值
    private static final String money        = "money";       //    金额      是    是    单位：元
    private static final String bankname     = "bankname";    //    开户行名称 是    是
    private static final String subbranch    = "subbranch";   //    支行名称   是    是
    private static final String accountname  = "accountname"; //    开户名    是    是
    private static final String cardnumber   = "cardnumber";  //    银行卡号   是    是
    private static final String province     = "province";    //    省份      是    是
    private static final String city         = "city";        //    城市      是    是


    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回 PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String, String> payParam, Map<String, String> details) throws PayException {
        try {

            //组装参数
            payParam.put(mchid, channelWrapper.getAPI_MEMBERID());
            payParam.put(out_trade_no, channelWrapper.getAPI_ORDER_ID());
            payParam.put(money, HandlerUtil.getYuan(channelWrapper.getAPI_AMOUNT()));
            payParam.put(bankname, channelWrapper.getAPI_CUSTOMER_BANK_NAME());
            payParam.put(subbranch, channelWrapper.getAPI_CUSTOMER_BANK_SUB_BRANCH());
            payParam.put(accountname, channelWrapper.getAPI_CUSTOMER_NAME());
            payParam.put(cardnumber, channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
            payParam.put(province, channelWrapper.getAPI_CUSTOMER_BANK_BRANCH());
            payParam.put(city, channelWrapper.getAPI_CUSTOMER_BANK_SUB_BRANCH());
            Map<String, String> extendsParam = new HashMap<>();
            extendsParam.put("extends", "1");
            String listStr       = JSON.toJSONString(extendsParam);
            String encodedString = Base64.getEncoder().encodeToString(listStr.getBytes());
            payParam.put("extends", encodedString);

            //生成md5
            StringBuilder sb   = new StringBuilder((payParam.size() + 1) * 10);
            List<String>  keys = new ArrayList<String>(payParam.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                sb.append(key).append("=");
                sb.append(payParam.get(key));
                sb.append("&");
            }
            String preStr = sb.toString() + "key=" + channelWrapper.getAPI_KEY();

            String pay_md5sign = HandlerUtil.getMD5UpperCase(preStr);
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

            String url = channelWrapper.getAPI_CHANNEL_BANK_URL() + "/Payment_Dfpay_add.html";
            //发送请求获取结果
            String resultStr = RestTemplateUtil.postForm(url, payParam, "UTF-8");
            resultStr = UnicodeUtil.unicodeToString(resultStr);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            addQueryDaifuOrderJob(channelWrapper.getAPI_ORDER_ID());  //自动查询 (无回调 自动查询)

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
        try {


            //组装参数
            payParam.put(mchid, channelWrapper.getAPI_MEMBERID());
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
            String url       = channelWrapper.getAPI_CHANNEL_BANK_URL() + "/Payment_Dfpay_query.html";
            String resultStr = RestTemplateUtil.postForm(url, payParam, "UTF-8");
            resultStr = UnicodeUtil.unicodeToString(resultStr);
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


    //查询代付-余额，正常返回余额单位分，否则抛异常(无查余额接口)
    @Override
    protected long queryDaifuBalanceAllInOne(Map<String, String> payParam, Map<String, String> details) throws PayException {

        try {
            //组装参数
            payParam.put(mchid, channelWrapper.getAPI_MEMBERID());

            //生成md5
            StringBuilder sb   = new StringBuilder((payParam.size() + 1) * 10);
            List<String>  keys = new ArrayList<String>(payParam.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                sb.append(key).append("=");
                sb.append(payParam.get(key));
                sb.append("&");
            }
            String preStr      = sb.toString() + "key=" + channelWrapper.getAPI_KEY();
            String pay_md5sign = HandlerUtil.getMD5UpperCase(preStr).toUpperCase();
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), pay_md5sign);

            //发送请求获取结果
            String url       = channelWrapper.getAPI_CHANNEL_BANK_URL() + "/Payment_Dfpay_balance.html";
            String resultStr = RestTemplateUtil.postForm(url, payParam, "UTF-8");
            resultStr = UnicodeUtil.unicodeToString(resultStr);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            JSONObject jsonObj = JSON.parseObject(resultStr);//请求状态(200：成功，500：系统异常，404：必填项为空)
            if (HandlerUtil.valJsonObj(jsonObj, "status", "success") && jsonObj.containsKey("balance")) {
                String balance = jsonObj.getString("balance");
                return Long.parseLong(HandlerUtil.getFen(balance));
            } else {
                throw new PayException(resultStr);
            }
        } catch (Exception e) {
            log.error("[火凤凰代付][代付余额查询]出错,错误消息：{},参数：{}", e.getMessage(), JSON.toJSONString(payParam), e);
            throw new PayException(String.format("[火凤凰代付][代付余额查询]出错,错误:%s", e.getMessage()));
        }

    }


    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr, boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);
//          请求状态(成功:success 失败：error)
        if (!isQuery) {
            if (HandlerUtil.valJsonObj(jsonObj, "status", "error")) return PayEumeration.DAIFU_RESULT.ERROR;
            if (HandlerUtil.valJsonObj(jsonObj, "status", "success")) return PayEumeration.DAIFU_RESULT.PAYING;
            return PayEumeration.DAIFU_RESULT.ERROR;
        } else {
//          请求状态(status    状态    是    success:请求成功（不代表业务成功），error：请求失败)
//          1 成功   2 失败   3 处理中    4 待处理    5 审核驳回    6 待审核  7 交易不存在  8 未知状态
//          当status=success和refCode=1同时成立时才表示转账成功
            if (HandlerUtil.valJsonObj(jsonObj, "status", "error")) return PayEumeration.DAIFU_RESULT.ERROR;
            if (HandlerUtil.valJsonObj(jsonObj, "status", "success") && HandlerUtil.valJsonObj(jsonObj, "refCode", "1"))
                return PayEumeration.DAIFU_RESULT.SUCCESS;
            if (HandlerUtil.valJsonObj(jsonObj, "refCode", "3", "4", "6")) return PayEumeration.DAIFU_RESULT.PAYING;
            if (HandlerUtil.valJsonObj(jsonObj, "refCode", "2", "5", "7", "8")) return PayEumeration.DAIFU_RESULT.ERROR;
            new PayException(resultStr);
        }
        return PayEumeration.DAIFU_RESULT.UNKNOW;
    }


}