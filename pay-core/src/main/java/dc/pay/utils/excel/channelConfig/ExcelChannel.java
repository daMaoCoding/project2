package dc.pay.utils.excel.channelConfig;/**
 * Created by admin on 2017/7/13.
 */

import com.alibaba.fastjson.annotation.JSONType;

/**
 * ************************
 *  汉字威武
 * @author tony 3556239829
 */
@JSONType(orders={"序号", "第三方名称","第三方ID"})
public class ExcelChannel {
    private static final String  YES="是";
    private static final String  NO="否";
    private String 序号 ;
    private String 第三方名称;
    private String 第三方ID;
    private String 通道名称;

    private String 支持WEB=NO;
    private String 支持WAP=NO;
    private String 支持APP=NO;

    private String 微信二维码=NO;
    private String 微信公众号=NO;
    private String 微信反扫=NO;

    private String 支付宝二维码=NO;
    private String 支付宝公众号=NO;
    private String 支付宝反扫=NO;

    private String QQ钱包二维码=NO;
    private String QQ钱包反扫=NO;
    private String 财付通=NO;
    private String 百度钱包=NO;
    private String 京东钱包=NO;
    private String 京东快捷支付=NO;

    private String 银联钱包=NO;
    private String 银联快捷支付=NO;

    private String 快钱=NO;
    private String 备注;
    private String 网上银行=NO;
    private String 代付通道=NO;

    private String 银行名称;

    public void set银行名称(String 银行名称) {
        this.银行名称 = 银行名称;
    }


    public String get代付通道() {
        return 代付通道;
    }

    public void set代付通道(String 代付通道) {
        this.代付通道 = 代付通道;
    }



    public String get支付宝公众号() {
        return 支付宝公众号;
    }

    public void set支付宝公众号(String 支付宝公众号) {
        this.支付宝公众号 = 支付宝公众号;
    }

    /*默认必填*/
    private String 商号显示=YES;
    private String 商号必填=YES;
    public void set商号必填(String 商号必填) {
            this.商号必填 = 商号必填;
            this.商号显示=商号必填;
    }

    /*默认必填*/
    private String 商家密钥显示=YES;
    private String 商家密钥必填=YES;
    public void set商家密钥必填(String 商家密钥必填) {
        this.商家密钥必填 = 商家密钥必填;
        this.商家密钥显示=商家密钥必填;
    }



    private String 跳转网址显示=NO;
    private String 跳转网址必填=NO;
    public void set跳转网址必填(String 跳转网址必填) {
        this.跳转网址必填 = 跳转网址必填;
        this.跳转网址显示=YES;
    }

    private String 上传密钥文件显示=NO;
    private String 上传密钥文件必填=NO;
    public void set上传密钥文件必填(String 上传密钥文件必填) {
        this.上传密钥文件必填 = 上传密钥文件必填;
        this.上传密钥文件显示=YES;
    }


    private String 商家公钥显示=NO;
    private String 商家公钥必填=NO;
    public void set商家公钥必填(String 商家公钥必填) {
        this.商家公钥必填 = 商家公钥必填;
        this.商家公钥显示=YES;
    }

    private String 上传公钥文件显示=NO;
    private String 上传公钥文件必填=NO;
    public void set上传公钥文件必填(String 上传公钥文件必填) {
        this.上传公钥文件必填 = 上传公钥文件必填;
        this.上传公钥文件显示=YES;
    }


    private String 公钥口令显示=NO;
    private String 公钥口令必填=NO;
    public void set公钥口令必填(String 公钥口令必填) {
        this.公钥口令必填 = 公钥口令必填;
        this.公钥口令显示=YES;
    }



    private String 终端号显示=NO;
    private String 终端号必填=NO;
    public void set终端号必填(String 终端号必填) {
        this.终端号必填 = 终端号必填;
        this.终端号显示=YES;
    }


    private String 业务号显示=NO;
    private String 业务号必填=NO;
    public void set业务号必填(String 业务号必填) {
        this.业务号必填 = 业务号必填;
        this.业务号显示=YES;
    }



    private String 排序显示=YES;;
    private String 排序必填=NO;;
    private String 支付名称显示=YES;
    private String 支付名称必填=NO;
    private String 支付描述显示=YES;
    private String 支付描述必填=NO;
    private String 停用金额显示=YES;
    private String 停用金额必填=NO;
    private String 最小金额显示=YES;
    private String 最小金额必填=NO;
    private String 最大金额显示=YES;
    private String 最大金额必填=NO;
    private String 支付通道商家名称显示=YES;
    private String 支付通道商家名称必填=NO;
    private String 单笔最大转账额度显示=YES;
    private String 单笔最大转账额度必填=NO;
    private String 每天最大转账额度显示=YES;
    private String 每天最大转账额度必填=NO;
    private String 一般到账时间说明显示=YES;
    private String 一般到账时间说明必填=NO;
    private String 备注显示=YES;
    private String 备注必填=NO;

    public String get微信公众号() {
        return 微信公众号;
    }

    public void set微信公众号(String 微信公众号) {
        this.微信公众号 = 微信公众号;
    }

    public String get京东快捷支付() {
        return 京东快捷支付;
    }

    public void set京东快捷支付(String 京东快捷支付) {
        this.京东快捷支付 = 京东快捷支付;
    }

    public String get银联快捷支付() {
        return 银联快捷支付;
    }

    public void set银联快捷支付(String 银联快捷支付) {
        this.银联快捷支付 = 银联快捷支付;
    }

    public ExcelChannel(String 序号) {
        this.序号 = 序号;
    }

    public ExcelChannel() {
    }

    public String get序号() {
        return 序号;
    }

    public String get第三方名称() {
        return 第三方名称;
    }

    public String get通道名称() {
        return 通道名称;
    }

    public String get支持WEB() {
        return 支持WEB;
    }

    public String get支持WAP() {
        return 支持WAP;
    }

    public String get支持APP() {
        return 支持APP;
    }

    public String get微信二维码() {
        return 微信二维码;
    }

    public String get支付宝二维码() {
        return 支付宝二维码;
    }

    public String getQQ钱包二维码() {
        return QQ钱包二维码;
    }

    public String get财付通() {
        return 财付通;
    }

    public String get百度钱包() {
        return 百度钱包;
    }

    public String get京东钱包() {
        return 京东钱包;
    }

    public String get银联钱包() {
        return 银联钱包;
    }

    public String get快钱() {
        return 快钱;
    }

    public String get网上银行() {
        return 网上银行;
    }

    public String get银行名称() {
        return 银行名称;
    }

    public String get商号显示() {
        return 商号显示;
    }

    public String get商号必填() {
        return 商号必填;
    }

    public String get跳转网址显示() {
        return 跳转网址显示;
    }

    public String get跳转网址必填() {
        return 跳转网址必填;
    }

    public String get商家密钥显示() {
        return 商家密钥显示;
    }

    public String get商家密钥必填() {
        return 商家密钥必填;
    }

    public String get上传密钥文件显示() {
        return 上传密钥文件显示;
    }

    public String get上传密钥文件必填() {
        return 上传密钥文件必填;
    }

    public String get商家公钥显示() {
        return 商家公钥显示;
    }

    public String get商家公钥必填() {
        return 商家公钥必填;
    }

    public String get上传公钥文件显示() {
        return 上传公钥文件显示;
    }

    public String get上传公钥文件必填() {
        return 上传公钥文件必填;
    }

    public String get公钥口令显示() {
        return 公钥口令显示;
    }

    public String get公钥口令必填() {
        return 公钥口令必填;
    }

    public String get终端号显示() {
        return 终端号显示;
    }

    public String get终端号必填() {
        return 终端号必填;
    }

    public String get业务号显示() {
        return 业务号显示;
    }

    public String get业务号必填() {
        return 业务号必填;
    }

    public String get备注() {
        return 备注;
    }

    public String get排序显示() {
        return 排序显示;
    }

    public String get排序必填() {
        return 排序必填;
    }

    public String get支付名称显示() {
        return 支付名称显示;
    }

    public String get支付名称必填() {
        return 支付名称必填;
    }

    public String get支付描述显示() {
        return 支付描述显示;
    }

    public String get支付描述必填() {
        return 支付描述必填;
    }

    public String get停用金额显示() {
        return 停用金额显示;
    }

    public String get停用金额必填() {
        return 停用金额必填;
    }

    public String get最小金额显示() {
        return 最小金额显示;
    }

    public String get最小金额必填() {
        return 最小金额必填;
    }

    public String get最大金额显示() {
        return 最大金额显示;
    }

    public String get最大金额必填() {
        return 最大金额必填;
    }

    public String get支付通道商家名称显示() {
        return 支付通道商家名称显示;
    }

    public String get支付通道商家名称必填() {
        return 支付通道商家名称必填;
    }

    public String get单笔最大转账额度显示() {
        return 单笔最大转账额度显示;
    }

    public String get单笔最大转账额度必填() {
        return 单笔最大转账额度必填;
    }

    public String get每天最大转账额度显示() {
        return 每天最大转账额度显示;
    }

    public String get每天最大转账额度必填() {
        return 每天最大转账额度必填;
    }

    public String get一般到账时间说明显示() {
        return 一般到账时间说明显示;
    }

    public String get一般到账时间说明必填() {
        return 一般到账时间说明必填;
    }

    public String get备注显示() {
        return 备注显示;
    }

    public String get备注必填() {
        return 备注必填;
    }

    public void set备注(String 备注) {
        this.备注 = 备注;
    }

    public void set序号(String 序号) {
        this.序号 = 序号;
    }

    public void set第三方名称(String 第三方名称) {
        this.第三方名称 = 第三方名称;
    }

    public void set通道名称(String 通道名称) {
        this.通道名称 = 通道名称;
    }

    public void set支持WEB(String 支持WEB) {
        this.支持WEB = 支持WEB;
    }

    public void set支持WAP(String 支持WAP) {
        this.支持WAP = 支持WAP;
    }

    public void set支持APP(String 支持APP) {
        this.支持APP = 支持APP;
    }

    public void set微信二维码(String 微信二维码) {
        this.微信二维码 = 微信二维码;
    }

    public void set支付宝二维码(String 支付宝二维码) {
        this.支付宝二维码 = 支付宝二维码;
    }

    public void setQQ钱包二维码(String QQ钱包二维码) {
        this.QQ钱包二维码 = QQ钱包二维码;
    }

    public void set财付通(String 财付通) {
        this.财付通 = 财付通;
    }

    public void set百度钱包(String 百度钱包) {
        this.百度钱包 = 百度钱包;
    }

    public void set京东钱包(String 京东钱包) {
        this.京东钱包 = 京东钱包;
    }

    public void set银联钱包(String 银联钱包) {
        this.银联钱包 = 银联钱包;
    }

    public void set快钱(String 快钱) {
        this.快钱 = 快钱;
    }

    public void set网上银行(String 网上银行) {
        this.网上银行 = 网上银行;
    }

    public void set商号显示(String 商号显示) {
        this.商号显示 = 商号显示;
    }

    public void set跳转网址显示(String 跳转网址显示) {
        this.跳转网址显示 = 跳转网址显示;
    }

    public void set商家密钥显示(String 商家密钥显示) {
        this.商家密钥显示 = 商家密钥显示;
    }

    public void set上传密钥文件显示(String 上传密钥文件显示) {
        this.上传密钥文件显示 = 上传密钥文件显示;
    }

    public void set商家公钥显示(String 商家公钥显示) {
        this.商家公钥显示 = 商家公钥显示;
    }

    public void set上传公钥文件显示(String 上传公钥文件显示) {
        this.上传公钥文件显示 = 上传公钥文件显示;
    }

    public void set公钥口令显示(String 公钥口令显示) {
        this.公钥口令显示 = 公钥口令显示;
    }

    public void set终端号显示(String 终端号显示) {
        this.终端号显示 = 终端号显示;
    }

    public void set业务号显示(String 业务号显示) {
        this.业务号显示 = 业务号显示;
    }

    public void set排序显示(String 排序显示) {
        this.排序显示 = 排序显示;
    }

    public void set排序必填(String 排序必填) {
        this.排序必填 = 排序必填;
    }

    public void set支付名称显示(String 支付名称显示) {
        this.支付名称显示 = 支付名称显示;
    }

    public void set支付名称必填(String 支付名称必填) {
        this.支付名称必填 = 支付名称必填;
    }

    public void set支付描述显示(String 支付描述显示) {
        this.支付描述显示 = 支付描述显示;
    }

    public void set支付描述必填(String 支付描述必填) {
        this.支付描述必填 = 支付描述必填;
    }

    public void set停用金额显示(String 停用金额显示) {
        this.停用金额显示 = 停用金额显示;
    }

    public void set停用金额必填(String 停用金额必填) {
        this.停用金额必填 = 停用金额必填;
    }

    public void set最小金额显示(String 最小金额显示) {
        this.最小金额显示 = 最小金额显示;
    }

    public void set最小金额必填(String 最小金额必填) {
        this.最小金额必填 = 最小金额必填;
    }

    public void set最大金额显示(String 最大金额显示) {
        this.最大金额显示 = 最大金额显示;
    }

    public void set最大金额必填(String 最大金额必填) {
        this.最大金额必填 = 最大金额必填;
    }

    public void set支付通道商家名称显示(String 支付通道商家名称显示) {
        this.支付通道商家名称显示 = 支付通道商家名称显示;
    }

    public void set支付通道商家名称必填(String 支付通道商家名称必填) {
        this.支付通道商家名称必填 = 支付通道商家名称必填;
    }

    public void set单笔最大转账额度显示(String 单笔最大转账额度显示) {
        this.单笔最大转账额度显示 = 单笔最大转账额度显示;
    }

    public void set单笔最大转账额度必填(String 单笔最大转账额度必填) {
        this.单笔最大转账额度必填 = 单笔最大转账额度必填;
    }

    public void set每天最大转账额度显示(String 每天最大转账额度显示) {
        this.每天最大转账额度显示 = 每天最大转账额度显示;
    }

    public void set每天最大转账额度必填(String 每天最大转账额度必填) {
        this.每天最大转账额度必填 = 每天最大转账额度必填;
    }

    public void set一般到账时间说明显示(String 一般到账时间说明显示) {
        this.一般到账时间说明显示 = 一般到账时间说明显示;
    }

    public void set一般到账时间说明必填(String 一般到账时间说明必填) {
        this.一般到账时间说明必填 = 一般到账时间说明必填;
    }

    public void set备注显示(String 备注显示) {
        this.备注显示 = 备注显示;
    }

    public void set备注必填(String 备注必填) {
        this.备注必填 = 备注必填;
    }

    public String get第三方ID() {
        return 第三方ID;
    }

    public void set第三方ID(String 第三方ID) {
        this.第三方ID = 第三方ID;
    }

    public String get微信反扫() {
        return 微信反扫;
    }

    public void set微信反扫(String 微信反扫) {
        this.微信反扫 = 微信反扫;
    }

    public String get支付宝反扫() {
        return 支付宝反扫;
    }

    public void set支付宝反扫(String 支付宝反扫) {
        this.支付宝反扫 = 支付宝反扫;
    }

    public String getQQ钱包反扫() {
        return QQ钱包反扫;
    }

    public void setQQ钱包反扫(String QQ钱包反扫) {
        this.QQ钱包反扫 = QQ钱包反扫;
    }


}
