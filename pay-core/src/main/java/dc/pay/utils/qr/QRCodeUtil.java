package dc.pay.utils.qr;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import dc.pay.base.processor.PayException;
import dc.pay.utils.qr.base64.ImageReader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Decoder;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * ************************
 * 二维码工具类
 * @author tony 3556239829
 */
@SuppressWarnings("unchecked")
public class QRCodeUtil {
    private static final Logger log =  LoggerFactory.getLogger(QRCodeUtil.class);
    private static final int BLACK = 0xff000000;
    private static final int WHITE = 0xFFFFFFFF;


    /**
     * 生成QRCode二维码<br>
     * 在编码时需要将com.google.zxing.qrcode.encoder.Encoder.java中的<br>
     * static final String DEFAULT_BYTE_MODE_ENCODING = "ISO8859-1";<br>
     * 修改为UTF-8，否则中文编译后解析不了<br>
     */
    public void encode(String contents, File file, BarcodeFormat format, int width, int height, Map<EncodeHintType, ?> hints) {
        try {
            //消除乱码
            contents = new String(contents.getBytes("UTF-8"), "ISO-8859-1");
            BitMatrix bitMatrix = new MultiFormatWriter().encode(contents, format, width, height);
            writeToFile(bitMatrix, "png", file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成二维码图片<br>
     *
     * @param matrix
     * @param format 图片格式
     * @param file   生成二维码图片位置
     * @throws IOException
     */
    public static void writeToFile(BitMatrix matrix, String format, File file) throws IOException {
        BufferedImage image = toBufferedImage(matrix);
        ImageIO.write(image, format, file);
    }

    /**
     * 生成二维码内容<br>
     *
     * @param matrix
     * @return
     */
    public static BufferedImage toBufferedImage(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) == true ? BLACK : WHITE);
            }
        }
        return image;
    }

    /**
     * 解析QRCode二维码-文件
     */
    @SuppressWarnings("unchecked")
    public static void decode(File file) {
        try {
            BufferedImage image;
            try {
                image = ImageIO.read(file);
                if (image == null) {
                    System.out.println("Could not decode image");
                }
                LuminanceSource source = new BufferedImageLuminanceSource(image);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                Result result;
                @SuppressWarnings("rawtypes")
                Hashtable hints = new Hashtable();
                //解码设置编码方式为：utf-8
                hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
                result = new MultiFormatReader().decode(bitmap, hints);
                String resultStr = result.getText();
                System.out.println("解析后内容：" + resultStr);
            } catch (IOException ioe) {
                System.out.println(ioe.toString());
            } catch (ReaderException re) {
                System.out.println(re.toString());
            }
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }


    /**
     * 解析QRCode二维码-URL
     */
    public static String decodeByUrl(String url) throws PayException {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) { }
        try {
            return QRUrlDecodeUtil.decodeQr(url);
        }catch (Exception e){
            log.error("解析QRCode二维码-URL出错，图片地址：".concat(url).concat(",错误消息："+e.getMessage()),e);
            return decodeByUrlOldVersion(url);
        }
    }


    /**
     * 解析QRCode二维码-URL
     */
    public static String decodeByUrlOldVersion(String url) throws PayException {
        if(StringUtils.isBlank(url)) throw new PayException("二维码地址为空");
        BufferedImage image;
        try{
            try{
                image = ImageIO.read(new URL(url)); }catch (Exception e){
                image = ImageIO.read(new MemoryCacheImageInputStream(new ByteArrayInputStream( getImageFromNetByUrl(url))) );
             }

            if (image == null) {
                throw new PayException("[解析二维码出错,无法获得图片]imgURL："+url);
            }

            image =  toGrayImage(image);

            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result;
            Hashtable hints = new Hashtable();
            //解码设置编码方式为：utf-8
            hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
            result = new MultiFormatReader().decode(bitmap, hints);
            String resultStr = result.getText();
            return resultStr.trim();
        } catch (Exception e) {
            throw new PayException("[解析二维码出错],解析出错，消息："+e.getMessage()+",图片地址："+url);
        }
    }




    //转换色阶
    private static BufferedImage toGrayImage(BufferedImage image) {
        BufferedImage result = image;
        if (BufferedImage.TYPE_BYTE_GRAY != image.getType()) {
            BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            newImage.getGraphics().drawImage(image, 0, 0, null);
            result = newImage;
        }
        Raster raster = result.getRaster();
        DataBufferByte buffer = (DataBufferByte) raster.getDataBuffer();
        byte[] data = buffer.getData();
        for (int i = 0; i < data.length; i++) {
            byte value = 0;
            if (data[i] < 32) {
                value = -1;
            }
            buffer.setElem(i, value);
        }
        return result;
    }




    public static byte[] getImageFromNetByUrl(String strUrl) {
        try {
            URL url = new URL(strUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5 * 1000);
            conn.setRequestProperty("User-agent", "  Mozilla/5.0 (Windows NT 6.1; WOW64; rv:33.0) Gecko/20100101 Firefox/33.0");
            InputStream inStream = conn.getInputStream();// 通过输入流获取图片数据
            byte[] btImg = readInputStream(inStream);// 得到图片的二进制数据
            return btImg;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据地址获得数据的字节流
     */
    public static byte[] getImageFromLocalByUrl(String strUrl) {
        try {
            File imageFile = new File(strUrl);
            InputStream inStream = new FileInputStream(imageFile);
            byte[] btImg = readInputStream(inStream);// 得到图片的二进制数据
            return btImg;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 从输入流中获取数据
     */
    public static byte[] readInputStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[10240];
        int len = 0;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        inStream.close();
        return outStream.toByteArray();
    }








    /**
     * 解析QRCode二维码-base64编码
     * imageStrData = “data:image/png;base64,iVBORw0KGg...."
     */
    public static String decodeByBase64(String imageStrData) {
        String imgStr = imageStrData;
        String resultStr=null;
        BufferedImage image = null;
        if(imageStrData.contains(",")) { imageStrData = imageStrData.substring(imageStrData.indexOf(",") + 1, imageStrData.length());  }
            try {
                BASE64Decoder decoder = new BASE64Decoder();
                byte[] bytes = decoder.decodeBuffer(imageStrData);
                for (int i = 0; i < bytes.length; ++i) {
                    if (bytes[i] < 0) {// 调整异常数据
                        bytes[i] += 256;
                    }
                }
               ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
               image = ImageIO.read(bais);
               resultStr =processImage(image);
               if(StringUtils.isBlank(resultStr)) throw new PayException("");
            } catch (Exception e) {
                try { image = ImageReader.readDataURIImage(new URI(imageStrData)); resultStr =processImage(image); }catch (Exception ee){ return null;}
            }finally {
                imgStr  =null;
                imageStrData = null;
            }
        return resultStr;
    }


    public static String processImage(BufferedImage image) throws PayException {
        return QRUrlDecodeUtil.processImage(image);
    }


}