package dc.pay.utils.ipUtil.qqwry.qqwry3.util;


import dc.pay.utils.ipUtil.qqwry.entry.CityDirectly;
import dc.pay.utils.ipUtil.qqwry.entry.IPEntry;
import dc.pay.utils.ipUtil.qqwry.entry.Province;
import dc.pay.utils.ipUtil.qqwry.entry.ProvinceSuffix;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.Set;

/**
 * 根据给定的字符转切分出省市县字段
 */
public class AreaSplit {
    private StringBuffer stringBuffer = new StringBuffer();
    public StringBuffer getStringBuffer() {
        return stringBuffer;
    }
    public void setStringBuffer(StringBuffer stringBuffer) {
        this.stringBuffer = stringBuffer;
    }


    /**
     * 按照指定格式（encoding）读取文件
     * @param fileName 要读取的文件地址
     * @param encoding  按照指定编码格式读取文件避免乱码
     */
    public void readFileByLines(String fileName,String encoding) {
        InputStreamReader inputStreamReader = null;
        BufferedReader reader = null;
        try {
            inputStreamReader= new InputStreamReader(new FileInputStream(fileName), encoding);
            reader = new BufferedReader(inputStreamReader);
            String tempString = null;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                IPEntry ipEntry = new IPEntry();
                tempString = tempString.replaceAll("[ ][ ]*",";");
                String[] lines = tempString.split(";");
                if(lines.length>=3){
//                    System.out.println("area: "+lines[2]);
                    ipEntry.setBeginIp(lines[0]);
                    ipEntry.setEndIp(lines[1]);
                    String areaStr = lines[2];
                    ipEntry = getArea(areaStr);
//                    System.out.println("\t aim:: "+ipEntry.getAreaString());
                    stringBuffer.append(ipEntry.getAreaSplited(";")+"\n");
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
//            return stringBuffer;
        }
    }

    /**
     * 字符缓冲写入
     * @param stringBuffer 要写入的数据
     * @param filePath 要写入的文件地址
     * @param encoding 写文件时的文件编码
     */
    public void bufferedWriteAndFileWriterTest(StringBuffer stringBuffer,String filePath,String encoding) {
        File file = new File(filePath);
//        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding));
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding));
//            bufferedWriter = new BufferedWriter( new FileWriter(file));
            bufferedWriter.write(stringBuffer.toString());
            bufferedWriter.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bufferedWriter.close();
//                if(file.exists()){
//                    file.delete();
//                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 判断文件的编码格式
     * @param fileName :file
     * @return 文件编码格式
     * @throws Exception
     */
    public static String codeString(String fileName) throws Exception{
        BufferedInputStream bin = new BufferedInputStream(new FileInputStream(fileName));
        int p = (bin.read() << 8) + bin.read();
        String code = null;
        switch (p) {
            case 0xefbb:
                code = "UTF-8";
                break;
            case 0xfffe:
                code = "Unicode";
                break;
            case 0xfeff:
                code = "UTF-16BE";
                break;
            default:
                code = "GBK";
        }
        return code;
    }



    /**
     * 将字符串切分，获取省，市，县字段，三个字段，没有值的使用""填充
     * 0、为了显示准确，市级字段除了后缀"市"被过滤，其他的：“州”“地区”"盟"等后缀被保留。
     * 1、对于**市**市 的情况，例如："河北省保定市定州市" 和 "内蒙古**市**市"。在获取市子弹，因为使用"市"对字段切分，所以会导致最后的一个县区（如果有县区字段）字段丢失最后的后缀"市"：
     *      例如"河北省保定市定州市" --》中国|河北|保定|定州 ；为避免误导，对最后的县区字段手动添加后缀"市"使结果为：中国|河北|保定|定州市
     *      若获取市字段时使用的是其他后缀如：州,盟,区域,则无需另行为县区字段添加后缀"市"
     * 2、对于直辖市：
     *      北京  -> 中国|北京|北京|
     *      北京市 -> 中国|北京|北京|
     *      北京市海淀区 -> 中国|北京|北京|海淀区
     *      北京海淀区 -> 中国|北京|北京|海淀区
     *
     * @param areaStr
     * @return
     */
    public IPEntry getArea(String areaStr){
        Boolean isChina = Boolean.FALSE;
        String countryStr = "";    //国家
        String provinceStr = "";// 省
        String cityStr = "";    // 市
        String districtStr = "";// 县
        String flage = "";//记录类型

        String[] tempProvinceArr = null;
        String tempProvinceStr = null;
        String[] tempCityArr = null;
        String tempCityStr = null;
        if(isContains(areaStr,"省")){//包含“省”,且不以“省”开头
            tempProvinceArr = areaStr.split("省");
            isChina = Boolean.TRUE;
            countryStr = "中国";
            ProvinceSuffix.counterGN++;
            provinceStr = tempProvinceArr[0];
            if(tempProvinceArr.length >1 && !StringUtils.isEmpty(tempProvinceArr[1]) && !StringUtils.isBlank(tempProvinceArr[1])){//"省"之后仍有内容
//                if (isContains(tempProvinceArr[1],"地区,市,州")) {//包含“市”，且不以“市”开头：如“**省**市**县（区，市）”
                int indexSuffix = ProvinceSuffix.isContainsOf(tempProvinceArr[1]);//tempProvinceArr[1]为截取省字段之后的字符串
                if(indexSuffix >= 0 ){
//                    tempCityArr = tempProvinceArr[1].split("地区|市|州");

                    tempCityArr = tempProvinceArr[1].split(ProvinceSuffix.SUFFIX_ARR.get(indexSuffix));//判断之后的字段是：州,市,地区,盟中的哪个，并按照该字段切分tempProvinceArr[1]
                    //因为存在"黑龙江省大庆市肇州县"和"河北省石家庄市赵州县"这种情况（会导致：黑龙江|大庆市肇州|县  中国|河北|石家庄市赵州|县），导致切分错误。
                    Boolean isZX = "州".equals(ProvinceSuffix.SUFFIX_ARR.get(indexSuffix)) && tempProvinceArr[1].endsWith("州县") && tempProvinceArr[1].contains("市");
                    Boolean isZQ = "州".equals(ProvinceSuffix.SUFFIX_ARR.get(indexSuffix)) && tempProvinceArr[1].endsWith("州区") && tempProvinceArr[1].contains("市");
                    if(isZX | isZQ){
                        ProvinceSuffix.counterZAX++;
                        tempCityArr = tempProvinceArr[1].split("市");
                    }
                    //为避免误解，若后缀为地区或州（如：贵州省黔南州荔波县），后缀不去除（结果：贵州;黔南州;荔波县）
                    cityStr = "地区".equals(ProvinceSuffix.SUFFIX_ARR.get(indexSuffix)) | ("州".equals(ProvinceSuffix.SUFFIX_ARR.get(indexSuffix)) && !isZX && !isZQ)  ? tempCityArr[0]+ProvinceSuffix.SUFFIX_ARR.get(indexSuffix) : tempCityArr[0];

                    if(tempCityArr.length>1 && !StringUtils.isEmpty(tempCityArr[1]) && !StringUtils.isBlank(tempCityArr[1])){//市之后有内容
//                        districtStr = tempProvinceArr[1].endsWith("市") || tempCityArr[1].endsWith("县") || tempCityArr[1].endsWith("区") || tempCityArr[1].endsWith("旗") ? tempCityArr[1] : "";
                        //如果切分字段为"市"且类型为：**市**市，例如："河北省保定市定州市" ;切分后最后的"定州市"为"定州",为其补充"市"最为后缀
                        districtStr = ("市".equals(ProvinceSuffix.SUFFIX_ARR.get(indexSuffix)) && tempProvinceArr[1].endsWith("市")) ? tempCityArr[1]+ProvinceSuffix.SUFFIX_ARR.get(indexSuffix) : tempCityArr[1];
                        if(("市".equals(ProvinceSuffix.SUFFIX_ARR.get(indexSuffix)) && tempProvinceArr[1].endsWith("市")))
                            ProvinceSuffix.counterSSS++;
                        flage = "省-市(地区,州)-市县区";
                        ProvinceSuffix.counterSSX++;
                    }else{
                        flage = "省-市(地区,州)";
                        ProvinceSuffix.counterSS++;
                    }
                }else {
                    districtStr = tempProvinceArr[1].endsWith("县") || tempProvinceArr[1].endsWith("区") || tempProvinceArr[1].endsWith("旗") ? tempProvinceArr[1] : "";
                    //输出到文件。查看
                    flage = "省-县区旗";
                    ProvinceSuffix.counterSX++;
                }
            }else{
                //省之后没有内容，eg:浙江省。什么也不做
                flage = "省";
                ProvinceSuffix.counterS++;
            }
        }else{
            //不包含“省”，1、国外，2、内蒙古，3、北京，上海等直辖市，4、港澳台地区。
            //1、遍历所有省份，
            tempProvinceStr = getInSet(Province.getProvinceSet(),areaStr);//
            if(!StringUtils.isEmpty(tempProvinceStr)){//字符串中包含省名，说明是国内的地址
                isChina = true;
                countryStr = "中国";
                ProvinceSuffix.counterGN++;
                provinceStr = tempProvinceStr;
                String areaStrSuffix = "地区,盟,市";
                if(CityDirectly.getCitySet().contains(tempProvinceStr)){//是否是直辖市：“北京市西城区”
                    if(isContains(areaStr,areaStrSuffix)){//直辖市之后跟地区,盟,市
                        int indexSuffix = indexContains(areaStr,areaStrSuffix);
                        tempCityArr = areaStr.split(areaStrSuffix.split(",")[indexSuffix]);
                        cityStr = "地区".equals(areaStrSuffix.split(",")[indexSuffix]) | "州".equals(areaStrSuffix.split(",")[indexSuffix]) |"盟".equals(areaStrSuffix.split(",")[indexSuffix]) ? tempCityArr[0]+areaStrSuffix.split(",")[indexSuffix] : tempCityArr[0];
//                        cityStr = tempCityArr[0];
                        if(tempCityArr.length>1 && !StringUtils.isEmpty(tempCityArr[1]) && !StringUtils.isBlank(tempCityArr[1])){//市之后有内容
                            //一般市之后的所有内容都无论以什么结尾，都统一作为districtStr的内容
                            districtStr = tempCityArr[1];
                            flage = "直-市县区旗";
                            ProvinceSuffix.counterZS ++;
                        }else{
                            //是直辖市，但仅仅只有市：“北京市”
                            flage = "直";
                            ProvinceSuffix.counterZ ++;
                        }
                    }else{
                        //是直辖市，但是却没有包含“市”字，暂时简单处理，需统计类似数据量
                        //自动将市字段=直辖市
                        cityStr = provinceStr;
                        districtStr = areaStr.replaceAll("北京市|北京","").trim().length()>0 ? areaStr.replaceAll("北京市|北京","") : "";
                        flage = "直!市";
                        ProvinceSuffix.counterZNS++;
                    }

                } else{//不是直辖市，说明就是内蒙古，港澳台等
                    tempCityStr = areaStr.replaceAll(tempProvinceStr,"");//把省从地址字符串中去除掉
                    if(isContains(tempCityStr,areaStrSuffix)){//省之后为地区,盟,市：内蒙古乌兰察布市,内蒙古通辽市霍林郭勒市
                        int indexSuffix = indexContains(tempCityStr,areaStrSuffix);
//                        System.out.println(areaStrSuffix.split(",")[indexSuffix]);
                        tempCityArr = tempCityStr.split(areaStrSuffix.split(",")[indexSuffix]);
                        cityStr = "地区".equals(areaStrSuffix.split(",")[indexSuffix]) | "州".equals(areaStrSuffix.split(",")[indexSuffix]) |"盟".equals(areaStrSuffix.split(",")[indexSuffix]) ? tempCityArr[0]+areaStrSuffix.split(",")[indexSuffix] : tempCityArr[0];
                        if(tempCityArr.length>1 && !StringUtils.isEmpty(tempCityArr[1]) && !StringUtils.isBlank(tempCityArr[1])){//获取县（区，旗）
                            //一般市之后的所有内容都无论以什么结尾，都统一作为districtStr的内容
//                            districtStr = tempCityStr.endsWith("市") || tempCityArr[1].endsWith("县") || tempCityArr[1].endsWith("区") || tempCityArr[1].endsWith("旗") ? tempCityArr[1] : "";
                            districtStr = tempCityArr[1];
                            flage = "!直-市盟-市县区旗";
                            ProvinceSuffix.counterNZSX++;
                        }else{//只到市没有县区旗
                            flage = "!直-市盟";
                            ProvinceSuffix.counterNZS ++;
                        }
//                    }else if(isContains(tempCityStr,"盟")){//省之后为盟：内蒙古锡林郭勒盟
//                        tempCityArr = tempCityStr.split("盟");
//                        cityStr = tempCityArr[0];
//                        if(tempCityArr.length>1 && !StringUtils.isEmpty(tempCityArr[1]) && !StringUtils.isBlank(tempCityArr[1])){//获取县（区，旗）
//                            //盟之后会有内容，但却不包含：市县区旗等字段：内蒙古锡林郭勒盟二连浩特
//                            //故，盟之后的内容不再做处理直接作为districtStr字段。
//                            districtStr = tempCityArr[1];
//                            flage = "!直-盟-市县区旗";
//                        }else {//盟之后没有了
//                            flage = "!直-盟";
//                        }
                    }else {//港澳台，单独没有市县
                        flage = "!直";
                        ProvinceSuffix.counterNZ++;
                    }
                }
            }else {
                //不包含中国的省，说明是国外地址
                countryStr = areaStr;
                flage = "国外";
                ProvinceSuffix.counterGW ++;
            }
        }
        IPEntry ipEntry = new IPEntry(countryStr,provinceStr,cityStr,districtStr,flage,areaStr);
        return ipEntry;
    }



    /**
     * areaStr包含provinceSuffix中字符串,但不能以其开头
     * provinceSuffix中字符元素顺序固定
     * @param areaStr
     * @return
     */
    public int indexContains(String areaStr,String str) {
        String[] strTempArr = str.contains(",") ? str.split(",") : new String[]{str};
        String strTmp = "";
        for(int index =0;index<strTempArr.length;index++){
            strTmp = strTempArr[index];
            if(areaStr.contains(strTmp) && !areaStr.trim().startsWith(strTmp)){
                return index;
            }
        }
        return -1;
    }

    /**
     * areaStr中是否含有str中的字符
     * str中字符以逗号分隔：
     * @param areaStr
     * @param str
     * @return
     */
    public Boolean isContains(String areaStr, String str) {
        String[] strTempArr = str.contains(",") ? str.split(",") : new String[]{str};
        for(String strTmp : strTempArr){
            if(areaStr.contains(strTmp) && !areaStr.trim().startsWith(strTmp)){
                return true;
            }
        }
        return false;
    }

    /**
     * 字符串str中是否包含set集合中的元素，包含则返回该元素，否则为""
     * @param set
     * @param str
     * @return
     */
    public String getInSet(Set<String> set, String str){
        String aimStr = "";
        for(String tmpStr : set ){
           if(str.contains(tmpStr) && str.startsWith(tmpStr)) {
               aimStr = tmpStr;
               break;
           }
        }
        return aimStr;
    }

    /**
     * 字符串str中是否包含set集合中的元素，包含则返回true，否则为false
     * @param set
     * @param str
     * @return
     */
    public Boolean isInSet(Set<String> set, String str){
        Boolean result = false;
        for(String tmpStr : set ){
            if(str.contains(tmpStr) && str.startsWith(tmpStr)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * 调用时的入口函数
     * encoding ：解析之后文件的编码格式
     * @param args
     */
    public static void main(String[] args) throws Exception {
            AreaSplit areaSplit = new AreaSplit();
            String inputFilePath = "./QQWry.txt";
            String outputFilePath = "./ipoutput.txt";
            String inputEncoding = areaSplit.codeString(inputFilePath);
            String encoding =inputEncoding;
            System.out.println("输入文件inputFilePath: "+inputFilePath);
            System.out.println("输出文件outputFilePath: "+outputFilePath);
            System.out.println("输出文件编码encoding: "+encoding);
            System.out.println("输入文件编码inputEncoding: "+inputEncoding);
            areaSplit.readFileByLines(inputFilePath,inputEncoding);
            areaSplit.bufferedWriteAndFileWriterTest(areaSplit.getStringBuffer(),outputFilePath,encoding);
            System.out.println("数据统计如下：");
            ProvinceSuffix.getCounter();
        }


}
