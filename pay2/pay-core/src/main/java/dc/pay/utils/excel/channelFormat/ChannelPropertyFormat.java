package dc.pay.utils.excel.channelFormat;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import dc.pay.utils.Pinyin4jUtil;
import dc.pay.utils.UnicodeUtil;
import dc.pay.utils.excel.channelConfig.ExcelHelper;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.*;
import java.util.*;

/**
 * ************************
 *  1.根据[channel-excelJson.properties]格式化后输出到控制台。
 *  2.模糊匹配标准银行名称。
 *  3.注意：在生成excel前，要保证[channel-excelJson.properties]和[你的.properties]文件相同。
 * @author tony 3556239829
 */
public class ChannelPropertyFormat {
    public static String tmpStr = "%s= %s| %s | %s #-%s";
    public static String tmpNameStr0 = "(%s)";
    public static String tmpChannelName = "%s_%s";
    public static String tmpChannelRemark = "%s%s";
    private static final String spBank = "_BANK_";
    private static final String spDf = "_DF_";
    private static final String spWy = "_WY_";
    private static final String spPartn = "#[";
    private static Map<String,String> stdBankNames = null;
    private static ClassPathResource  channelResource = null;
    private static HandleBankMatch handleBankMatch =  null;
    private static final String stdBankFilePath = "channelConfig/stdBankNames.properties";
    private static String partnerName="";
    private static String channelRemarkOther="";
    private static String AllInOneBank="支持全部银行";
    private static Map<String,String> dfBankNames =Maps.newHashMap();
    private static Map<String,String> zfBankNames =Maps.newHashMap();

    static {
            try {
                stdBankNames = new HashMap<String,String>((Map) PropertiesLoaderUtils.loadProperties(new ClassPathResource(stdBankFilePath)));
                channelResource = new ClassPathResource("channelConfig/channel-excelJson.properties");
                handleBankMatch =  new HandleBankMatch(stdBankNames);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(0);
            }
    }

    public static void main(String[] args) throws IOException {
        // ClassPathResource  channelResource = new ClassPathResource("channelConfig/channel-tony.properties");
        if(channelResource.exists() && channelResource.isReadable()){
            List<String> configLine= Files.readLines(channelResource.getFile(), Charsets.UTF_8);
            configLine.forEach(lineStr->{
                lineStr = UnicodeUtil.unicodeToString(lineStr);
                if(lineStr.startsWith(spPartn)){ partnerName = ExcelHelper.getChannelCo(lineStr);  }
                String[] valueSplit = lineStr.split("\\||\\#");
                if(null!=valueSplit && lineStr.contains("=")&&  lineStr.contains(spBank)&&valueSplit.length==4){
                        String channelName    = valueSplit[0].split("=")[0].trim();
                        String channelFlag    = valueSplit[0].split("=")[1].trim();
                        channelFlag =  String.format("%" + (-25+(length(channelFlag)-channelFlag.length())) + "s",channelFlag);
                        String channelUrl     = valueSplit[1].trim();
                        channelUrl =  String.format("%" + -110 + "s",channelUrl);
                        String channelSign    = valueSplit[2].trim();
                        channelSign =  String.format("%" + -10 + "s",channelSign);
                        String channelRemark   = valueSplit[3].replace("-","").trim();
                        channelRemark = getChannelRemark(channelName,channelRemark);
                        channelName = getChannelName(partnerName,channelName,channelRemark);
                        String formated = String.format(tmpStr, channelName, channelFlag, channelUrl, channelSign, channelRemark);
                        System.out.println(formated);
                        checkBankNames(channelName,channelRemark);
                }else { System.out.println(lineStr);}
            });
        }
    }

    public static int length(String value) {
        int valueLength = 0;
        String chinese = "[\u0391-\uFFE5]";
        for (int i = 0; i < value.length(); i++) {
            String temp = value.substring(i, i + 1);
            if (temp.matches(chinese)) {
                valueLength += 2;
            } else {
                valueLength += 1;
            }
        }
        return valueLength;
    }

    private static  String getChannelName(String partnerName,String channelName,String channelRemark){
        channelName = channelName.trim();
        String bankRemarkName = channelRemark;
        if(channelRemark.contains(",")) bankRemarkName =channelRemark.split( ",")[0];
        if(StringUtils.isNotBlank(partnerName) && StringUtils.isNotBlank(channelName)){
           channelName = String.format(tmpChannelName,Pinyin4jUtil.getFullSpell(partnerName).toUpperCase(), channelName.substring(channelName.indexOf("_")+1));
       }

        if(StringUtils.isNotBlank(channelRemark) && !channelRemark.contains("(") && !channelRemark.contains("[") && StringUtils.isNotBlank(bankRemarkName)  && (channelName.toUpperCase().contains(spDf) || channelName.toUpperCase().contains(spWy))){
            channelName = String.format(tmpChannelName, channelName.substring(0, channelName.lastIndexOf("_")), Pinyin4jUtil.getFirstSpell(bankRemarkName).toUpperCase());
        }
        channelName =   String.format("%" + -40 + "s",channelName);
        return channelName.toUpperCase();
    }

    private static  String getChannelRemark(String channelName,String channelRemark){
        if(channelRemark.contains(AllInOneBank)) return channelRemark;
        if( channelName.toUpperCase().contains(spDf) || channelName.toUpperCase().contains(spWy) ){
            if(channelRemark.contains(",")){
                channelRemarkOther = channelRemark.substring(channelRemark.indexOf(","));
                channelRemark = channelRemark.substring(0, channelRemark.indexOf(","));
            }
            HashSet<String> resultSet = handleBankMatch.handleMatch(channelRemark);
            if(!resultSet.contains(channelRemark)){
                switch(resultSet.size()){
                    case 0 :
                        channelRemark = String.format(tmpNameStr0,channelRemark);
                        break;
                    default :
                        channelRemark = String.format(tmpNameStr0,channelRemark).concat(resultSet.toString());
                }
            }
            if(StringUtils.isNotBlank(channelRemarkOther)) channelRemark= String.format(tmpChannelRemark,channelRemark,channelRemarkOther);
        }
        return channelRemark;
    }


    public static void checkBankNames(String channelName,String channelRemark){
        if( channelName.toUpperCase().contains(spWy) && zfBankNames.containsKey(channelRemark)) System.err.format("银行名称重复：%s\n",channelRemark);
        if( channelName.toUpperCase().contains(spDf) && dfBankNames.containsKey(channelRemark)) System.err.format("银行名称重复：%s\n",channelRemark);
        zfBankNames.put(channelRemark,channelName);
        dfBankNames.put(channelRemark,channelName);
    }

}



