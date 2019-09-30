package dc.pay.business.kpaydaifu;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.base.processor.DaifuRequestHandler;
import dc.pay.base.processor.PayException;
import dc.pay.config.annotation.RequestDaifuHandler;
import dc.pay.constant.PayEumeration;
import dc.pay.utils.HandlerUtil;
import dc.pay.utils.MapUtils;
import dc.pay.utils.RestTemplateUtil;
import dc.pay.utils.XmlUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author Cobby
 * June 20, 2019
 */
@RequestDaifuHandler("KPAYDAIFU")
public final class KPayDaiFuRequestHandler extends DaifuRequestHandler {
    private static final Logger log = LoggerFactory.getLogger(KPayDaiFuRequestHandler.class);

    //请求代付&查询代付-参数
    private static final String version = "version";               //    版本号    string(3)    Y    默认填写2.0
    private static final String charset = "charset";               //    字符集    string(10)    Y    默认填写UTF-8
    private static final String spid = "spid";                  //    商户号    string(10)    Y    我司分配的商户号
    private static final String spbillno = "spbillno";              //    商户代付单号    string(32)    Y    商户系统内部的订单号    确保在商户系统唯一必须为数字
    private static final String tranAmt = "tranAmt";               //    交易金额    string(18)    Y    交易金额不大于1亿，单位为分
    private static final String acctName = "acctName";              //    收款人姓名    string(16)    Y    收款人姓名
    private static final String acctId = "acctId";                //    收款人账号    string(20)    Y    收款人账号
    private static final String acctType = "acctType";              //    账号类型    string(1)    Y    0-借记卡 1-贷记卡 2-对公 暂不支持贷记卡
    //  private static final String  certType =  "certType";             //    证件类型    string(20)    N    1-身份证 对私必传
//  private static final String  certId   =  "certId";               //    证件号码    string(20)    N    证件号码 对私必传
//  private static final String  mobile   =  "mobile";               //    银行预留手机号    string(11)    N    收款人手机号码对私必传
    private static final String bankName = "bankName";              //    开户行名称    string(24)    Y    开户行名称
    private static final String bankCode = "bankCode";              //    银行编码    string(4)        k-pay 内部区分不同银行的4位数字。 详见文档银行代号部分
    //  private static final String  bankBranchName =  "bankBranchName"; //    支行名称    string(24)    N    支行名称对公必传
//  private static final String  bankSettleNo   =  "bankSettleNo";   //    支行联行号    string(20)    N    支行联行号对公必传
    private static final String accountNo = "accountNo";             //    账户编号    string(20)    Y    出款账户编号，建议可动态设置
    private static final String notifyUrl = "notifyUrl";             //    后台通知地址    string(255)    Y    接收k-pay 通知的URL，需给绝对路径，255字符内格式如:http://wap.tenpay.com/tenpay.asp，确保k-pay 能通过互联网访问该地址
    //  private static final String  attach   =  "attach";               //    保留字段    string(255)    N    原样返回
    private static final String signType = "signType";              //    签名类型    string(10)    Y    目前只支持MD5
//  private static final String  sign     =  "sign";                 //    必填  签名

    //请求代付
    //如果抛异常，订单状态就是未知的，确定不成功要返回PayEumeration.DAIFU_RESULT.ERROR,而不是抛异常，
    //确定成功，等待支付，返回  PayEumeration.DAIFU_RESULT.PAYING
    //确定已转账完毕并成功，返回，PayEumeration.DAIFU_RESULT.SUCCESS
    @Override
    protected PayEumeration.DAIFU_RESULT requestDaifuAllInOne(Map<String, String> payParam, Map<String, String> details) throws PayException {
        try {
            String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
            if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
                log.error("[Kpay代付]-[代付]-“代付通道商号”输入数据格式为【中间使用&分隔】：商户号&资金账号( 即第三方后台 - 资金管理中-内充-对应的出款账户编号");
                throw new PayException("[Kpay代付]-[代付]-“代付通道商号”输入数据格式为【中间使用&分隔】：商户号&资金账号( 即第三方后台 - 资金管理中-内充-对应的出款账户编号");
            }
            //组装参数
            payParam.put(version, "2.0");
            payParam.put(charset, "UTF-8");
            payParam.put(spid, channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(spbillno, channelWrapper.getAPI_ORDER_ID());
            payParam.put(tranAmt, channelWrapper.getAPI_AMOUNT());
            payParam.put(acctName, channelWrapper.getAPI_CUSTOMER_NAME());
            payParam.put(acctId, channelWrapper.getAPI_CUSTOMER_BANK_NUMBER());
            payParam.put(acctType, "0");
            payParam.put(bankName, channelWrapper.getAPI_CUSTOMER_BANK_BRANCH());
            payParam.put(bankCode, channelWrapper.getAPI_CHANNEL_BANK_NAME_FlAG().split(",")[0]);
            payParam.put(accountNo, channelWrapper.getAPI_MEMBERID().split("&")[1]);
            payParam.put(notifyUrl, channelWrapper.getAPI_CHANNEL_BANK_NOTIFYURL());
            payParam.put(signType, "MD5");

            //生成md5
            List          paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder signSrc   = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if (!signType.equals(paramKeys.get(i)) && StringUtils.isNotBlank(payParam.get(paramKeys.get(i)))) {
                    signSrc.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
            }
            signSrc.append("key=" + channelWrapper.getAPI_KEY());
            String paramsStr = signSrc.toString();
            String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr);
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), signMd5);
            //发送请求获取结果
            String              url             = channelWrapper.getAPI_CHANNEL_BANK_URL() + "/payment/paymentPay";
            String              resultStr       = RestTemplateUtil.postXml(url, XmlUtil.map2Xml(payParam, false, "xml", true));
            Map<String, String> stringStringMap = XmlUtil.xml2Map(resultStr);
            resultStr = JSON.toJSONString(stringStringMap);
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
        if (1 == 2) throw new PayException("[Kpay代付][代付][查询订单状态]该功能未完成。");
        try {

            //version     版本号    string(3)    Y    默认填写2.0
            //charset     字符集    string(10)    Y    默认填写UTF-8
            //spid        商户号    string(10)    Y    我司分配的商户号
            //spbillno    商户代付单号    string(32)    N   商户系统内部的订单号与transaction_id二选一
            //transactionId    k-pay 代扣单号    string(32)    N   k-pay 代扣单号与spbillno二选一，transaction_id若传，则优先使用
            //signType    签名类型    string(10)    Y    目前只支持MD5
            //sign        关键参数签名    string(32)    Y    签名（MD5）
            //组装参数
            payParam.put(version, "2.0");
            payParam.put(charset, "UTF-8");
            payParam.put(spid, channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(spbillno, channelWrapper.getAPI_ORDER_ID());
            payParam.put(signType, "MD5");


            //生成md5
            List          paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder signSrc   = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if (!signType.equals(paramKeys.get(i)) && StringUtils.isNotBlank(payParam.get(paramKeys.get(i)))) {
                    signSrc.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
            }
            signSrc.append("key=" + channelWrapper.getAPI_KEY());
            String paramsStr = signSrc.toString();
            String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr);
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), signMd5);

            //发送请求获取结果
            String              url             = channelWrapper.getAPI_CHANNEL_BANK_URL() + "/payment/paymentQuery";
            String              resultStr       = RestTemplateUtil.postXml(url, XmlUtil.map2Xml(payParam, false, "xml", true));
            Map<String, String> stringStringMap = XmlUtil.xml2Map(resultStr);
            resultStr = JSON.toJSONString(stringStringMap);
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

//        version     版本号    string(3)       Y    默认填写2.0
//        charset     字符集    string(10)      Y    默认填写UTF-8
//        spid        商户号    string(10)      Y    我司分配的商户号
//        spbillno    查询流水号    String(32)   Y    余额查询流水
//        accountNo   账户编号    String(32)    N    可选，如传此参数，将只查询此账户余额
//        signType    签名类型    string(10)     Y    目前只支持MD5
//        sign        关键参数签名    string(32)  Y    签名(MD5)
        try {
            String aPI_MEMBERID = channelWrapper.getAPI_MEMBERID();
            if (null == aPI_MEMBERID || !aPI_MEMBERID.contains("&") || aPI_MEMBERID.split("&").length != 2) {
                log.error("[Kpay代付]-[代付]-“代付通道商号”输入数据格式为【中间使用&分隔】：商户号&资金账号( 即第三方后台 - 资金管理中-内充-对应的出款账户编号");
                throw new PayException("[Kpay代付]-[代付]-“代付通道商号”输入数据格式为【中间使用&分隔】：商户号&资金账号( 即第三方后台 - 资金管理中-内充-对应的出款账户编号");
            }
            //组装参数
            payParam.put(version, "2.0");
            payParam.put(charset, "UTF-8");
            payParam.put(spid, channelWrapper.getAPI_MEMBERID().split("&")[0]);
            payParam.put(spbillno, HandlerUtil.randomStr(8));
            payParam.put(accountNo, channelWrapper.getAPI_MEMBERID().split("&")[1]);
            payParam.put(signType, "MD5");

            //生成md5
            List          paramKeys = MapUtils.sortMapByKeyAsc(payParam);
            StringBuilder signSrc   = new StringBuilder();
            for (int i = 0; i < paramKeys.size(); i++) {
                if (!signType.equals(paramKeys.get(i)) && StringUtils.isNotBlank(payParam.get(paramKeys.get(i)))) {
                    signSrc.append(paramKeys.get(i)).append("=").append(payParam.get(paramKeys.get(i))).append("&");
                }
            }
            signSrc.append("key=" + channelWrapper.getAPI_KEY());
            String paramsStr = signSrc.toString();
            String signMd5   = HandlerUtil.getMD5UpperCase(paramsStr);
            payParam.put(channelWrapper.getAPI_CHANNEL_SIGN_PARAM_NAME(), signMd5);

            //发送请求获取结果
            String              url             = channelWrapper.getAPI_CHANNEL_BANK_URL() + "/payment/balance";
            String              resultStr       = RestTemplateUtil.postXml(url, XmlUtil.map2Xml(payParam, false, "xml", true));
            Map<String, String> stringStringMap = XmlUtil.xml2Map(resultStr);
            resultStr = JSON.toJSONString(stringStringMap);
            details.put(RESPONSEKEY, resultStr);//强制必须保存下第三方结果
            JSONObject jsonObj = JSON.parseObject(resultStr);// 0为请求成功，非0不成功
            if (HandlerUtil.valJsonObj(jsonObj, "retcode", "0") && jsonObj.containsKey("availableBalance")) {
                String balance = jsonObj.getString("availableBalance"); // 单位分
                return Long.parseLong(balance);
            } else {
                throw new PayException(resultStr);
            }
        } catch (Exception e) {
            log.error("[Kpay代付][代付余额查询]出错,错误消息：{},参数：{}", e.getMessage(), JSON.toJSONString(payParam), e);
            throw new PayException(String.format("[Kpay代付][代付余额查询]出错,错误:%s", e.getMessage()));
        }

    }


    //获取代付结果
    //获取[全部]明确的结果，返回第三方结果与之对应的我们的结果
    //未知的结果，抛出异常。
    private PayEumeration.DAIFU_RESULT getDaifuResult(String resultStr, boolean isQuery) throws PayException {
        JSONObject jsonObj = JSON.parseObject(resultStr);
//          0为请求成功，非0发起支付失败，非0需将通过回调或者查询确认代付交易结果
//        10010  生成订单失败   10039  收款人姓名不合法     10059  当前通道出款已关闭
        if (!isQuery) {
            if (HandlerUtil.valJsonObj(jsonObj, "retcode", "10010", "10039", "10059", "10040"))
                return PayEumeration.DAIFU_RESULT.ERROR;
            if (HandlerUtil.valJsonObj(jsonObj, "retcode", "0")) return PayEumeration.DAIFU_RESULT.PAYING;
            if (!HandlerUtil.valJsonObj(jsonObj, "retcode", "0")) return PayEumeration.DAIFU_RESULT.UNKNOW;
            return PayEumeration.DAIFU_RESULT.ERROR;
        } else {
//          retcode    结果    string(5)    Y    0为请求成功，非0发起支付失败 10012 订单不存在
//          result    代付状态    processreview-等待复核     processsuccess-转账成功    processing-转账处理中     processfailed-转账失败     processreject-复核驳回
            if (HandlerUtil.valJsonObj(jsonObj, "retcode", "10012", "10040")) return PayEumeration.DAIFU_RESULT.ERROR;
            if (resultStr.contains("订单不存在")) return PayEumeration.DAIFU_RESULT.ERROR;
            if (HandlerUtil.valJsonObj(jsonObj, "retcode", "0") && HandlerUtil.valJsonObj(jsonObj, "result", "processsuccess"))
                return PayEumeration.DAIFU_RESULT.SUCCESS;
            if (HandlerUtil.valJsonObj(jsonObj, "retcode", "0") && HandlerUtil.valJsonObj(jsonObj, "result", "processreview", "processing"))
                return PayEumeration.DAIFU_RESULT.PAYING;
            if (HandlerUtil.valJsonObj(jsonObj, "retcode", "0") && HandlerUtil.valJsonObj(jsonObj, "result", "processfailed", "processreject"))
                return PayEumeration.DAIFU_RESULT.ERROR;
            new PayException(resultStr);
        }
        return PayEumeration.DAIFU_RESULT.UNKNOW;
    }


}