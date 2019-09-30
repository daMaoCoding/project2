package dc.pay.utils.excel.channelConfig;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dc.pay.utils.ChannelParamConfigUtil;
import dc.pay.utils.DateUtil;
import dc.pay.utils.HttpUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * ************************
 *  读取通道配置，写入Excel文件
 * @author tony 3556239829
 */
public class ExcelHelper {
    private final Logger log =  LoggerFactory.getLogger(ExcelHelper.class);

    private  static final String  lineSp="#############################";
    private  static final String  YES="是";
    private  static final String  NO="否";
    private  static final String channelNameSplit = "_";
    private  static final String channelCommentSplit = ",";
    //private  static final String filePath =ExcelHelper.class.getClassLoader().getResource("通道配置99.xls").getPath();
    public  static final String  excelOutputFileName= "通道配置"+DateUtil.formatDateTimeStrByParam("_yyyy-MM-dd_HH.mm.ss")+".xls";

    private static List<ExcelChannel> excelChannelListALL=null;          //文件内容对象,ALL
    private static List<ExcelChannel> excelChannelListJsonAndExcel=null;  //文件内容对象,JSON&Excel

    static{
        try {
            excelChannelListALL  = getExcelChannelListALL();
            excelChannelListJsonAndExcel = getExcelChannelListJsonAndxcel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized List<ExcelChannel> getExcelChannelListALL(){
        Map<String,LinkedList<String>> fileMap = Maps.newLinkedHashMap();         //1.文件内容
        fileMap = readChannelParamConfigResources(ChannelParamConfigUtil.getChannelParamConfigResourceAll(),fileMap);  //2.读取文件通道配置，填充FileMap
        fileMap = filterFileMap(fileMap);                                     //3.清理FileMap
        return  processFileMap(fileMap);                           //4.处理FileMap
    }

    public static synchronized  List<ExcelChannel> getExcelChannelListJsonAndxcel(){
        Map<String,LinkedList<String>> fileMap = Maps.newLinkedHashMap();         //1.文件内容
        fileMap = readChannelParamConfigResources(ChannelParamConfigUtil.getChannelParamConfigResourceJsonAndExcel(),fileMap);  //2.读取文件通道配置，填充FileMap
        fileMap = filterFileMap(fileMap);                                     //3.清理FileMap
        return  processFileMap(fileMap);                           //4.处理FileMap
    }



    public static synchronized Map<String,LinkedList<String>>  readChannelParamConfigResources(List<Resource> resources,Map<String,LinkedList<String>> fileMap ) {
            Map<String,LinkedList<String>> resultMap = Maps.newLinkedHashMap();
        if(null!=resources && !resources.isEmpty()){
            for (Resource channelResource : resources) {
                if(!channelResource.exists()) continue;
                InputStream inputStream=null;
                try {
                    inputStream= channelResource.getInputStream();
                    resultMap.putAll(readFileByLines(inputStream,fileMap));
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    IOUtils.closeQuietly(inputStream);
                }
            }
        }
        return resultMap;
    }


    public static void main(String[] args) throws IOException, IllegalAccessException, URISyntaxException {
        generateExcelChannelConfParam(getChannelConfParamJsonAndExcel());
    }





    /*入口.读取并创建excel表格*/
    public static String generateExcelChannelConfParam(List<ExcelChannel>  excelChannelList) throws IOException, IllegalAccessException, URISyntaxException {
        testChannelBankCName(excelChannelList); //00.检查银行名称，不通过直接退出
        String filePath = getExcelOutputFilePath(excelOutputFileName);
        printExcelChannelList(excelChannelList);                                 //5.打印excelChannelList
        PoiOperater.createExcelFile(excelChannelList,filePath);                  //6.创建通道配置表Excel
        System.out.println("=========================================================================================");
        System.out.println("生成Excel通道配置文件完成，文件路径："+filePath);
        System.out.println("=========================================================================================");
        return filePath;
    }



    /*入口.获得通道配置参数*/
    public static List<ExcelChannel>  getChannelConfParamAll()  {
        return excelChannelListALL;                                                //6.Controler文件内容对象
    }


   /*入口.获得通道配置参数*/
    public static List<ExcelChannel>  getChannelConfParamJsonAndExcel()  {
        return excelChannelListJsonAndExcel;                                        //6.Controler文件内容对象
    }



    /*获得输出excel目录*/
    public static String getExcelOutputFilePath(String excelOutputFileName) {
        String filePath = null;
        File file = new File(".");
        try {
            filePath = URLDecoder.decode(file.getCanonicalPath(), "utf-8");// 转化为utf-8编码
            if(filePath.startsWith("file:"))
                filePath = filePath.replaceFirst("file:","");
            filePath = filePath.concat(File.separator+excelOutputFileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filePath;
    }


    /**
     * 5.打印excelChannelList
     */
    private static  void printExcelChannelList(List<ExcelChannel> excelChannelList) {
        System.out.println("{");
        for (int i = 0; i < excelChannelList.size(); i++) {
            ExcelChannel excelChannel = excelChannelList.get(i);
            System.out.println(excelChannel.get序号()+"_"+excelChannel.get通道名称()+":"+ JSON.toJSONString(excelChannel)+",");
        }
        System.out.println("}");

    }




    /**
     * 处理FileMap
     */
    public static List<ExcelChannel>  processFileMap(Map<String,LinkedList<String>> fileMap) {
          List<ExcelChannel> excelChannelList = Lists.newLinkedList();  //ExclChannel对象
        int j=1;
        for (Map.Entry<String, LinkedList<String>> entry : fileMap.entrySet()) {
            String channelKey = entry.getKey();
           // String channelServCoName = unicode2String(getChannelCo(channelKey));   //1.第三方支付厂商名称
            String channelServCoName = unicodeToUtf8(getChannelCo(channelKey));   //1.第三方支付厂商名称
            LinkedList<String> channelValues = fileMap.get(channelKey);
            for (int i=0;i<channelValues.size();i++) {
                ExcelChannel excelChannel = new ExcelChannel(String.valueOf(j++));
                String channelValue = channelValues.get(i).trim();                //2.全部备注信息
                String channelName = getChannelName(channelValue).trim();        //3.第三方支付厂商-通道名称
                String ChannelComment = getChannelComment(channelValue,channelName).trim(); //4.第三方支付厂商-注释

                excelChannel = setExcelChannel(excelChannel,channelServCoName,channelName,ChannelComment); //设置ExcelChannel
                excelChannelList.add(excelChannel);
            }
        }
         return excelChannelList;
    }


    /**
     * 设置ExcelChannel
     */
    private static ExcelChannel setExcelChannel(ExcelChannel excelChannel,String channelServCoName,String channelName, String channelComment) {
        excelChannel = setChannelCompany(excelChannel,channelServCoName,channelName);   //1.设置,第三方ID,第三方名称,通道名称
        excelChannel = setChannelEnvironment(excelChannel,channelName);                 //2.设置,支持WEB,支持WAP,支持APP
        excelChannel = setChannelServsPartner(excelChannel,channelName,channelComment); //3.设置,微信二维码,支付宝二维码,QQ钱包二维码,财付通,百度钱包,京东钱包,银联钱包,快钱,银行名称,备注
        excelChannel = setChannelMastBeFillParam(excelChannel,channelComment);          //4.设置,商号必填否,商家密钥必填否,跳转网址必填,上传密钥文件必填,商家公钥必填,上传公钥文件必填,公钥口令必填,终端号必填,业务号必填




        return excelChannel;
    }


    /**
     * 4.设置,商号必填否,商家密钥必填否,跳转网址必填,上传密钥文件必填,商家公钥必填,上传公钥文件必填,公钥口令必填,终端号必填,业务号必填
     */
    private static ExcelChannel setChannelMastBeFillParam(ExcelChannel excelChannel, String channelComment) {
        String[] split = channelComment.split(channelCommentSplit); //备注参数(银行名称|备注,**必填,**必填,**必填,....)
        if(split.length>=2){
            for (int i = 1; i < split.length; i++) {
                // String mastBeFillParamName = unicode2String(split[i]);
                 String mastBeFillParamName = unicodeToUtf8(split[i]);
                 if("商号必填否".equals(mastBeFillParamName)){
                     excelChannel.set商号必填(NO);
                 }else if("商家密钥必填否".equals(mastBeFillParamName)){
                     excelChannel.set商家密钥必填(NO);
                 }else if("跳转网址必填".equals(mastBeFillParamName)){
                     excelChannel.set跳转网址必填(YES);
                 }else if("上传密钥文件必填".equals(mastBeFillParamName)){
                     excelChannel.set上传密钥文件必填(YES);
                 }else if("商家公钥必填".equals(mastBeFillParamName)){
                     excelChannel.set商家公钥必填(YES);
                 }else if("上传公钥文件必填".equals(mastBeFillParamName)){
                     excelChannel.set上传公钥文件必填(YES);
                 }else if("公钥口令必填".equals(mastBeFillParamName)){
                     excelChannel.set公钥口令必填(YES);
                 }else if("终端号必填".equals(mastBeFillParamName)){
                     excelChannel.set终端号必填(YES);
                 }else if("业务号必填".equals(mastBeFillParamName)){
                     excelChannel.set业务号必填(YES);
                 }
            }
        }
        return excelChannel;
    }


    /**
     * 3.设置,微信二维码,支付宝二维码,QQ钱包二维码,财付通,百度钱包,京东钱包,银联钱包,快钱,银行名称,备注
     */
    private static ExcelChannel setChannelServsPartner(ExcelChannel excelChannel, String channelName, String channelComment) {
        String channelServsPartner = channelName.split(channelNameSplit)[3];                     //第三方合作服务商名称
        //String bankNameOrComment  = unicode2String(channelComment.split(channelCommentSplit)[0]);//银行名称，或者备注信息
        String bankNameOrComment  = unicodeToUtf8(channelComment.split(channelCommentSplit)[0]);//银行名称，或者备注信息
        if( !channelName.contains("_WY_") && !channelName.contains("_DF_") && !channelComment.contains("(")   && !channelComment.contains(")")){
            if(channelName.contains("_WEBWAPAPP_"))
                bankNameOrComment =  bankNameOrComment.concat("(WEB+WAP+APP端)");
            if(channelName.contains("_WAP_"))
                bankNameOrComment =  bankNameOrComment.concat("(WAP端)");
        }

        switch (channelServsPartner) {
            case "WX":
                if(channelName.endsWith("_SM")){
                    excelChannel.set微信二维码(YES);
                }else if(channelName.endsWith("_GZH")){
                    excelChannel.set微信公众号(YES);
                }else if(channelName.endsWith("_FS")){
                    excelChannel.set微信反扫(YES);
                }
                break;
            case "ZFB":
                if(channelName.endsWith("_SM")){
                    excelChannel.set支付宝二维码(YES);
                }else if(channelName.endsWith("_GZH")){
                    excelChannel.set支付宝公众号(YES);
                }else if(channelName.endsWith("_FS")){
                    excelChannel.set支付宝反扫(YES);
                }
                break;
            case "QQ":
                if(channelName.endsWith("_SM")){
                   excelChannel.setQQ钱包二维码(YES);
                } else if(channelName.endsWith("_FS")){
                    excelChannel.setQQ钱包反扫(YES);
            }
                break;
            case "CFT":
                excelChannel.set财付通(YES);
                break;
            case "BD":
                excelChannel.set百度钱包(YES);
                break;
            case "JD":
                if(channelName.endsWith("_SM")){
                    excelChannel.set京东钱包(YES);
                }else if(channelName.endsWith("_KJZF")){
                    excelChannel.set京东快捷支付(YES);
                }
                break;
            case "YL":
                if(channelName.endsWith("_SM")){
                    excelChannel.set银联钱包(YES);
                }else if(channelName.endsWith("_KJZF")){
                    excelChannel.set银联快捷支付(YES);
                }
                break;
            case "KQ":
                excelChannel.set快钱(YES);
                break;
            case "WY":
                excelChannel.set网上银行(YES);
                excelChannel.set银行名称(bankNameOrComment); //设置银行名称
                break;
            case "DF":
                excelChannel.set代付通道(YES);
                excelChannel.set银行名称(bankNameOrComment);//设置银行名称
                break;
        }
        excelChannel.set备注(bankNameOrComment); //设置备注



        return excelChannel;
    }


    /**
     * 2.设置,支持WEB,支持WAP,支持APP
     */
    private static ExcelChannel setChannelEnvironment(ExcelChannel excelChannel, String channelName) {
        String environment = channelName.split(channelNameSplit)[2];
        if(environment.equals("WEB")){
           excelChannel.set支持WEB(YES);
        }else if(environment.equals("WAP")){
            excelChannel.set支持WAP(YES);
        }else if(environment.equals("APP")){
            excelChannel.set支持APP(YES);
        }else if(environment.equals("WEBWAP")){
            excelChannel.set支持WEB(YES);
            excelChannel.set支持WAP(YES);
        }else if(environment.equals("WEBWAPAPP")){
            excelChannel.set支持WEB(YES);
            excelChannel.set支持WAP(YES);
            excelChannel.set支持APP(YES);
        }else if(environment.equals("WEBAPP")){
            excelChannel.set支持WEB(YES);
            excelChannel.set支持APP(YES);
        }else if(environment.equals("WAPAPP")){
            excelChannel.set支持WAP(YES);
            excelChannel.set支持APP(YES);
        }
        return excelChannel;
    }




    /**
     * 1.设置,第三方ID,第三方名称,通道名称
     */
    private static ExcelChannel setChannelCompany(ExcelChannel excelChannel,String channelServCoName, String channelName) {
        excelChannel.set第三方名称(channelServCoName);
        excelChannel.set通道名称(channelName);
        excelChannel.set第三方ID(channelName.split(channelNameSplit)[0]);
        return excelChannel;
    }



    /**
     * 以行为单位读取文件，常用于读面向行的格式化文件
     */
    public static Map<String,LinkedList<String>>  readFileByLines(InputStream inputStream,Map<String,LinkedList<String>> fileMap ) {
        //BufferedReader reader = new BufferedReader(new FileReader(channelFile))
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String tempString = null;
            String tempKey = null;
            int line = 1;
            boolean newChannels=false;
            while ((tempString = reader.readLine()) != null) {
                if(StringUtils.isBlank(tempString) || tempString.contains("FOR_CALLBACK")){ //空行跳过&FOR_CALLBACK
                    line++;
                    continue;
                }
                if(line>3){
                  if(tempString.contains(lineSp) && !newChannels){ //新一轮通道
                      newChannels = true;
                      line++;
                      continue;
                  }
                  if(newChannels){ //新一轮第一行
                      tempKey = tempString;
                      LinkedList<String> channelParamsList = Lists.newLinkedList();
                      fileMap.put(tempKey,channelParamsList);
                      newChannels=false;
                  }else{
                      if(!tempString.startsWith("#"))
                         fileMap.get(tempKey).add(tempString);
                  }
                }
                line++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileMap;
    }




    /**
     * unicode 转字符串
     */
    public static String unicode2String(String unicode) {
        StringBuffer string = new StringBuffer();
        String[] hex = unicode.split("\\\\u");
        if(hex[0].equalsIgnoreCase("A")){
            System.out.println(hex);
        }
        for (int i = 0; i < hex.length; i++) {
            try {
                // 转换出每一个代码点
                int data = Integer.parseInt(hex[i], 16);
                // 追加成string
                string.append((char) data);
            }catch (Exception e){
                string.append(hex[i]);
            }
        }
        return string.toString();
    }


    /**
     * 字符串转换unicode
     */
    public static String string2Unicode(String string) {
        StringBuffer unicode = new StringBuffer();
        for (int i = 0; i < string.length(); i++) {
            // 取出每一个字符
            char c = string.charAt(i);
            // 转换为unicode
            unicode.append("\\u" + Integer.toHexString(c));
        }
        return unicode.toString();
    }



    /**
     * utf-8 转换成 unicode
     */
    public static String utf8ToUnicode(String inStr) {
        char[] myBuffer = inStr.toCharArray();

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < inStr.length(); i++) {
            Character.UnicodeBlock ub = Character.UnicodeBlock.of(myBuffer[i]);
            if(ub == Character.UnicodeBlock.BASIC_LATIN){
                //英文及数字等
                sb.append(myBuffer[i]);
            }else if(ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS){
                //全角半角字符
                int j = (int) myBuffer[i] - 65248;
                sb.append((char)j);
            }else{
                //汉字
                short s = (short) myBuffer[i];
                String hexS = Integer.toHexString(s);
                String unicode = "\\u"+hexS;
                sb.append(unicode.toLowerCase());
            }
        }
        return sb.toString();
    }

    /**
     * unicode 转换成 utf-8
     */
    public static String unicodeToUtf8(String theString) {
        char aChar;
        int len = theString.length();
        StringBuffer outBuffer = new StringBuffer(len);
        for (int x = 0; x < len;) {
            aChar = theString.charAt(x++);
            if (aChar == '\\') {
                aChar = theString.charAt(x++);
                if (aChar == 'u') {
                    // Read the xxxx
                    int value = 0;
                    for (int i = 0; i < 4; i++) {
                        aChar = theString.charAt(x++);
                        switch (aChar) {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                value = (value << 4) + aChar - '0';
                                break;
                            case 'a':
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                                value = (value << 4) + 10 + aChar - 'a';
                                break;
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                value = (value << 4) + 10 + aChar - 'A';
                                break;
                            default:
                                throw new IllegalArgumentException("Malformed   \\uxxxx   encoding.");
                        }
                    }
                    outBuffer.append((char) value);
                } else {
                    if (aChar == 't')
                        aChar = '\t';
                    else if (aChar == 'r')
                        aChar = '\r';
                    else if (aChar == 'n')
                        aChar = '\n';
                    else if (aChar == 'f')
                        aChar = '\f';
                    outBuffer.append(aChar);
                }
            } else
                outBuffer.append(aChar);
        }
        return outBuffer.toString();
    }





    /**
     * 过滤空通道配置(被注释的)
     */
    public static Map<String,LinkedList<String>> filterFileMap(Map<String,LinkedList<String>> fileMap){
        Iterator<Map.Entry<String, LinkedList<String>>> iterator = fileMap.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<String, LinkedList<String>> entry = iterator.next();
            String key = entry.getKey();
            LinkedList<String> values = fileMap.get(key);
            if(values.isEmpty())
                iterator.remove();        //OK
               // fileMap.remove(key);
      }
        return fileMap;
    }



    /**
     * 1.第三方支付厂商名称-获得[内容]
     * @param str
     * @return
     */
    public static String getChannelCo(String str){
        Pattern patternDomain = Pattern.compile("(?<=\\[).*?(?=\\])",Pattern.CASE_INSENSITIVE);
        Matcher matcherDomain = patternDomain.matcher(str);
        if(matcherDomain.find()){
            return matcherDomain.group();
        }
        return "";
    }

    /**
     * 获得第三方支付厂商-通道名称
     * @param str
     * @return
     */
    public static String getChannelName(String str){
        Pattern patternDomain = Pattern.compile("^[^=]*(?==)",Pattern.CASE_INSENSITIVE);
        Matcher matcherDomain = patternDomain.matcher(str);
        if(matcherDomain.find()){
            return matcherDomain.group();
        }
        return "";
    }

    /**
     * 获取第三方支付厂商-备注信息
     * @param str
     * @return
     */
    public static String getChannelComment(String str,String channelBankName){
        Pattern patternDomain = Pattern.compile("(?<=\\#-).*",Pattern.CASE_INSENSITIVE);
        Matcher matcherDomain = patternDomain.matcher(str);
        if(matcherDomain.find()){
            return  matcherDomain.group();
        }
        return "";
    }

    public static Map<String,String> getAllPayCo() {
            Map<String,String>  channelCoConfigParamAll = Maps.newHashMap();
            List<ExcelChannel> channelConfParamAll = ExcelHelper.getChannelConfParamAll();
            channelConfParamAll.forEach(excelChannel -> {
                channelCoConfigParamAll.put(excelChannel.get第三方ID(),excelChannel.get第三方名称());
            });
            return channelCoConfigParamAll;
    }





    public static Map<String,String> getAllPayChannelAndCoCname() {
        Map<String,String>  channelAndCoCname = Maps.newHashMap();
        List<ExcelChannel> channelConfParamAll = ExcelHelper.getChannelConfParamAll();
        channelConfParamAll.forEach(excelChannel -> {
            channelAndCoCname.put(excelChannel.get通道名称(),excelChannel.get第三方名称());
        });
        return channelAndCoCname;
    }



    public static Map<String,String> getAllPayChannelAndCoId() {
        Map<String,String>  channelAndCoId = Maps.newHashMap();
        List<ExcelChannel> channelConfParamAll = ExcelHelper.getChannelConfParamAll();
        channelConfParamAll.forEach(excelChannel -> {
            channelAndCoId.put(excelChannel.get通道名称(),excelChannel.get第三方ID());
        });
        return channelAndCoId;
    }




    //检查网银名称是否标准名称
    public static void testChannelBankCName(List<ExcelChannel>  excelChannelList){
        if(null!=excelChannelList &&  !excelChannelList.isEmpty()){

            String tk = getYjfkTK(); //登陆
            List<String> yjfkBankCnName = getYjfkBankCnName(tk); // 标准名称

            excelChannelList.forEach(excelChannel -> {
                if((excelChannel.get网上银行().equalsIgnoreCase(YES) || excelChannel.get代付通道().equalsIgnoreCase(YES)) && !yjfkBankCnName.contains(excelChannel.get银行名称()) && !excelChannel.get银行名称().equalsIgnoreCase("支持全部银行") ){
                        System.err.println(excelChannel.get银行名称()+"--------------->银行名称【不是】【标准银行名称】");
                        System.exit(0);
                }
            });

        }
    }


    //获取意见反馈登陆TK
    public static String getYjfkTK(){
        try {
            Map<String,String> loginParam = Maps.newHashMap();
            Map<String,String> headerMap = Maps.newHashMap();
            String loginUrl="https://a84i1mn9.iax6uc5du.com/apis/login";
            loginParam.put("userName","tony");
            loginParam.put("passWord","tony888888");
            headerMap.put("Content-Type","application/json");
            JSONObject jsonObject = HttpUtil.postJson(loginUrl, headerMap, loginParam);
            if(null!=jsonObject && jsonObject.containsKey("code") && jsonObject.getString("code").equalsIgnoreCase("200") ){
                return jsonObject.getJSONObject("data").getString("tk");
            }
        }catch (Exception e){
            System.err.println("获取【意见反馈】登陆tk失败。");
        }
        return null;
    }


    //获取银行列表
    public static  List<String> getYjfkBankCnName(String tk){
        try {
            Map<String,String> getBankCNameParam = Maps.newHashMap();
            Map<String,String> headerMap = Maps.newHashMap();
            String getBankCNameUrl="https://a84i1mn9.iax6uc5du.com/apis/bank/findByCondition";
            getBankCNameParam.put("deal","1");
            getBankCNameParam.put("pageNo","1");
            getBankCNameParam.put("pageSize","1000");
            getBankCNameParam.put("orderSort","desc");
            getBankCNameParam.put("orderField","createTime");
            headerMap.put("Content-Type","application/json");
            headerMap.put("tk",tk);
            JSONObject jsonObject = HttpUtil.postJson(getBankCNameUrl, headerMap, getBankCNameParam);
            if(null!=jsonObject && jsonObject.containsKey("code") && jsonObject.getString("code").equalsIgnoreCase("200") ){
                JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("resultList");

                List<String> collect = StreamSupport.stream(jsonArray.spliterator(), false)
                        .map(JSONObject.class::cast)
                        .filter(o -> StringUtils.isNotBlank(o.getString("bankName")))
                        .map(o -> o.getString("bankName"))
                        .map(String::trim)
                        .collect(Collectors.toList());

                if(null!=collect && !collect.isEmpty())  return collect;

            }
        }catch (Exception e){
            System.err.println("获取【意见反馈】银行列表失败。");
        }
        return null;
    }


}