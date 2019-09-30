package dc.pay.utils.excel.channelConfig;/**
 * Created by admin on 2017/7/13.
 */

import dc.pay.utils.HandlerUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

/**
 * ************************
 *  POI操作Excel
 * @author tony 3556239829
 */
public class PoiOperater {

    //标题文字

    private static final String [] titles = {"序号","第三方名称","第三方ID","通道名称","支持WEB","支持WAP","支持APP","代付通道","微信二维码","微信公众号","微信反扫","支付宝二维码","支付宝公众号","支付宝反扫","QQ钱包二维码","QQ钱包反扫","财付通","百度钱包","京东钱包","京东快捷支付","银联钱包","银联快捷支付","快钱","网上银行","银行名称","商号显示","商号必填","跳转网址显示","跳转网址必填","商家密钥显示","商家密钥必填","上传密钥文件显示","上传密钥文件必填","商家公钥显示","商家公钥必填","上传公钥文件显示","上传公钥文件必填","公钥口令显示","公钥口令必填","终端号显示","终端号必填","业务号显示","业务号必填","排序显示","排序必填","支付名称显示","支付名称必填","支付描述显示","支付描述必填","停用金额显示","停用金额必填","最小金额显示","最小金额必填","最大金额显示","最大金额必填","支付通道商家名称显示","支付通道商家名称必填","单笔最大转账额度显示","单笔最大转账额度必填","每天最大转账额度显示","每天最大转账额度必填","一般到账时间说明显示","一般到账时间说明必填","备注显示","备注必填","备注"};
   // private static final String [] titles = {"序号","第三方名称","第三方ID","通道名称","支持WEB","支持WAP","支持APP","微信二维码","微信公众号","微信反扫","支付宝二维码","支付宝公众号","支付宝反扫","QQ钱包二维码","QQ钱包反扫","财付通","百度钱包","京东钱包","京东快捷支付","银联钱包","银联快捷支付","快钱","网上银行","银行名称","商号显示","商号必填","跳转网址显示","跳转网址必填","商家密钥显示","商家密钥必填","上传密钥文件显示","上传密钥文件必填","商家公钥显示","商家公钥必填","上传公钥文件显示","上传公钥文件必填","公钥口令显示","公钥口令必填","终端号显示","终端号必填","业务号显示","业务号必填","排序显示","排序必填","支付名称显示","支付名称必填","支付描述显示","支付描述必填","停用金额显示","停用金额必填","最小金额显示","最小金额必填","最大金额显示","最大金额必填","支付通道商家名称显示","支付通道商家名称必填","单笔最大转账额度显示","单笔最大转账额度必填","每天最大转账额度显示","每天最大转账额度必填","一般到账时间说明显示","一般到账时间说明必填","备注显示","备注必填","备注"};


    /*0.入口：创建通道配置表Excel*/
    public static void createExcelFile(List<ExcelChannel> excelChannelList,String filePath) throws IOException, IllegalAccessException {
        String now= HandlerUtil.getDateTimeByMilliseconds(String.valueOf(System.currentTimeMillis()),"yyyy-MM-dd_HH.mm.ss");
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("通道配置_"+now);
        sheetEdit(workbook,sheet,excelChannelList); //编写工作表
        FileOutputStream fileOut = new FileOutputStream(filePath);
        workbook.write(fileOut);
        fileOut.close();
    }



    /*编写工作表*/
    public static void sheetEdit(Workbook workbook,Sheet sheet,List<ExcelChannel> excelChannelList) throws IOException, IllegalAccessException {
        createTitle(workbook,sheet);                    //1.创建标题
        fillContext(workbook,sheet,excelChannelList);   //2.填充内容
    }



    /*2.填充内容*/
    private static void fillContext(Workbook workbook, Sheet sheet, List<ExcelChannel> excelChannelList) throws IllegalAccessException {
        Row row = null;
        Cell cell = null;
        CreationHelper creationHelper = workbook.getCreationHelper();
        for (int i = 0; i < excelChannelList.size(); i++) {
            ExcelChannel excelChannel = excelChannelList.get(i);//通道配置数据
            row = sheet.createRow((short)i+1);
            for (int j = 0; j < titles.length; j++) {
                cell = row.createCell(j);
                cell.setCellValue(creationHelper.createRichTextString(getExcelChannelCellValue(j,titles,excelChannel)));
            }
        }
    }


    /*获取通道配置参数值*/
    private static String getExcelChannelCellValue(int j,String[] titles, ExcelChannel excelChannel) throws IllegalAccessException {
        String getFiledName = titles[j]; //要获取属性的名字 如：序号
        Class<? extends ExcelChannel> excelChannelClazz = excelChannel.getClass();
        Field[] declaredFields = excelChannelClazz.getDeclaredFields(); //属性
        for(int i = 0 ; i < declaredFields.length; i++){
            Field field = declaredFields[i];
            field.setAccessible(true); //设置些属性是可以访问的
           if(getFiledName.equals(field.getName())){
               String val = String.valueOf(field.get(excelChannel));//得到此属性的值
               if("null".equals(val)|| StringUtils.isBlank(val) ) //||"否".equals(val)
                   val="";
               //System.out.println("name:"+field.getName()+"\t value = "+val);
               return val;
           }
        }
        return "";
    }




    /*1.创建标题*/
    private static void createTitle(Workbook workbook, Sheet sheet) {
        Font font = getTitleFont(workbook);//标题字体
        CellStyle cellStyle = getTitelCellStyle(workbook, font);//标题样式
        Row row = createTitleRow(sheet); //标题行
        setSheetColumnWidth(sheet); //设置标题列(全部列)宽度
        createTitleCell(workbook, cellStyle, row);//创建标题列
    }

    /*创建标题列*/
    private static void createTitleCell(Workbook workbook, CellStyle cellStyle, Row row) {
        Cell cell = null;
        for (int i = 0; i < titles.length; i++) {
            cell = row.createCell(i);
            cell.setCellValue(workbook.getCreationHelper().createRichTextString(titles[i]));
            cell.setCellStyle(cellStyle);
        }
    }


    /*标题行*/
    private static Row createTitleRow(Sheet sheet) {
        Row row = sheet.createRow((short)0);
        row.setHeightInPoints(20);
        row.setHeight((short) (25 * 20));
        return row;
    }

    /*标题样式*/
    private static CellStyle getTitelCellStyle(Workbook workbook, Font font) {
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFillForegroundColor(IndexedColors.GOLD.getIndex());//设置背景颜色
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);     //设置背景颜色填充方式
        cellStyle.setBorderTop(BorderStyle.THICK);//上边框
        cellStyle.setBorderRight(BorderStyle.MEDIUM);//右边框
        cellStyle.setBorderLeft(BorderStyle.MEDIUM);//左边框
        cellStyle.setBorderBottom(BorderStyle.THICK); //下边框
        cellStyle.setAlignment(HorizontalAlignment.CENTER); // 居中
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);// 居中
        cellStyle.setFont(font);
        return cellStyle;
    }

    /*标题字体*/
    private static Font getTitleFont(Workbook workbook) {
        Font font = workbook.createFont();
        font.setFontName("幼圆");
        font.setFontHeightInPoints((short) 11);//设置字体大小
        font.setBold(false);//粗体显示
        return font;
    }

    /*1-设置表格列宽度*/
    private static void setSheetColumnWidth(Sheet sheet) {
        sheet.setColumnWidth(0,  6 * 256);  //设置列宽
        sheet.setColumnWidth(3,  40 * 256);  //设置列宽
        sheet.setColumnWidth(47, 30 * 256);
        sheet.setColumnWidth(48, 30 * 256);
        sheet.setColumnWidth(49, 30 * 256);
        sheet.setColumnWidth(50, 30 * 256);
        sheet.setColumnWidth(51, 30 * 256);
        sheet.setColumnWidth(52, 30 * 256);
        sheet.setColumnWidth(53, 30 * 256);
        sheet.setColumnWidth(54, 30 * 256);
        sheet.setColumnWidth(55, 30 * 256);
        sheet.setColumnWidth(57, 35 * 256);
        sheet.setColumnWidth(64, 40 * 256); //备注
        sheet.setDefaultColumnWidth(18);
        sheet.setDefaultRowHeightInPoints(18);
    }
}
