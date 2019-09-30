package dc.pay.entity.comparator;

import dc.pay.entity.ReqPayInfo;

import java.util.Comparator;

public class ComparatorReqPayInfo implements Comparator {
    public int compare(Object arg0, Object arg1) {
        ReqPayInfo reqPayInfo0 = (ReqPayInfo) arg0;
        ReqPayInfo reqPayInfo1 = (ReqPayInfo) arg1;
        Integer reqPayInfoSN0 = 0;
        Integer reqPayInfoSN1 = 0;
        try{
            reqPayInfoSN0 = Integer.valueOf(reqPayInfo0.getAPI_SEQUENCE_NUMBER());
            reqPayInfoSN1 = Integer.valueOf(reqPayInfo1.getAPI_SEQUENCE_NUMBER());
        }catch (Exception ex){

        }



        int flag =reqPayInfoSN0.compareTo(reqPayInfoSN1);
        if (flag == 0) {
            return reqPayInfo0.getAPI_CHANNEL_BANK_NAME().compareTo(reqPayInfo1.getAPI_CHANNEL_BANK_NAME());
        } else {
            return flag;
        }
    }
}