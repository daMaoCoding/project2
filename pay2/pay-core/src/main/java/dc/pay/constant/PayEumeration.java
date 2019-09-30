package dc.pay.constant;

/**
 * ************************
 * @author tony 3556239829
 */
public interface PayEumeration {

      String split = ",";
      String SIGN_TYPE = "MD5";
      String CHAR_SET = "UTF-8";
      Long DEFAULT_TIME_OUT_REQPAY =25*1000L ;
      Long DEFAULT_TIME_OUT_RESPAY =25*1000L ;
      Long DEFAULT_TIME_OUT_RESPAYDB =30*1000L ;
      Long DEFAULT_TIME_OUT_REQDAIFU =25*1000L ;
      Long DEFAULT_QUERY_DIAFU_TIME_OUT_REQDAIFU =15*1000L ;
      Long DEFAULT_TIME_OUT_RESDAIFU =30*1000L ;
      Long DEFAULT_TIME_OUT_RESDAIFUDB =30*1000L ;
      String WEB = "WEB";
      String WAP = "WAP";
      String APP = "APP";
      String TJ_BY_DAY = "TJ_BY_DAY";
      String TJ_BY_SEVEN_DAY = "TJ_BY_SEVEN_DAY";
      String TJ_BY_MONTH = "TJ_BY_MONTH";
      String TJ_BY_HALF_YEAR = "TJ_BY_HALF_YEAR";
      String TJ_BY_YEAR = "TJ_BY_YEAR";
      String TJ_STATUS_NEW = "TJ_STATUS_NEW";
      String TJ_STATUS_WORING = "TJ_STATUS_WORING";
      String TJ_STATUS_FINISH = "TJ_STATUS_FINISH";
      String DATA_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
      String queryDaifuFromKey="queryDaifuFrom";
      String queryDaifuFromAdmin="后台查询";
      String queryDaifuFromAuto="自动查询";
      int TJ_LOCKED_STATUS_ON = 1;
      int TJ_LOCKED_STATUS_OFF = 0;
      String  DELIMITER_OR = "|";
      String AutoQueryDaifuKeyTMP="AutoQueryDaifu:%s";
      String AutoQueryDaifuMqSendedTMP="%s|wait";
      long  nextTimeAutoQueryDaifu = 60000L;
      long oneHourMilSec = 60*60*1000L;

    /**
     * 提交方式
     */
      enum HTTP_METHOD {
        POST,
        PUT,
        GET,
        DELETE
    }

    /**
     * 请求支付结果
     */
      enum REQUEST_PAY_CODE {
        SUCCESS("SUCCESS"), ERROR("ERROR");
        String codeValue;
        REQUEST_PAY_CODE(String codeValue) {
            this.codeValue = codeValue;
        }
        public String getCodeValue() {
            return codeValue;
        }
    }

    /**
     * 响应支付结果
     */
      enum RESPONSE_PAY_CODE {
        SUCCESS("SUCCESS"), ERROR("ERROR");
        String codeValue;

        RESPONSE_PAY_CODE(String codeValue) {
            this.codeValue = codeValue;
        }

        public String getCodeValue() {
            return codeValue;
        }
    }



    /**
     * 支付信息状态
     */
      enum API_ORDER_STATE {
        NEW("NEW"),
        PAYING("PAYING"),
        SUCCESS("SUCCESS"), //发现db更改了值，支付也改为数字，妈的，仁至义尽了。`status` tinyint(1) NOT NULL COMMENT '状态 0待确认1已存入2已取消 3：已锁定',
        FAILED("FAILED");
        String state;

        API_ORDER_STATE(String state) {
            this.state = state;
        }

        public String getState() {
            return state;
        }
    }


    
    /**
     * 通道类型
     * 
     * @author andrew
     * Dec 11, 2017
     */
    enum CHANNEL_TYPE {
    	_WEBWAPAPP_,
    	//pc应用
    	_WEB_,
    	_WAPAPP_,
    	//wap应用【H5】
    	_WAP_,
    	//app应用
    	_APP_,
    	//网银
    	_WY_
    }



    //代付结果
    enum  DAIFU_RESULT {
        SUCCESS("SUCCESS"), ERROR("ERROR"),PAYING("PAYING"),UNKNOW("UNKNOW");
        String codeValue;
        DAIFU_RESULT(String codeValue) {
            this.codeValue = codeValue;
        }
        public String getCodeValue() {
            return codeValue;
        }
    }


}
