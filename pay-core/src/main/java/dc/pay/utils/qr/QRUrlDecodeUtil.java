package dc.pay.utils.qr;

import com.google.common.net.HttpHeaders;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.ImageReader;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.multi.MultipleBarcodeReader;
import dc.pay.base.processor.PayException;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.color.CMMException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;


public final class QRUrlDecodeUtil {

    private static final Logger log = Logger.getLogger(QRUrlDecodeUtil.class.getName());

    // No real reason to let people upload more than ~64MB
    private static final long MAX_IMAGE_SIZE = 1L << 26;
    // No real reason to deal with more than ~32 megapixels
    private static final int MAX_PIXELS = 1 << 25;
    private static final byte[] REMAINDER_BUFFER = new byte[1 << 16];
    private static final Map<DecodeHintType, Object> HINTS;
    private static final Map<DecodeHintType, Object> HINTS_PURE;

    static {
        HINTS = new EnumMap<>(DecodeHintType.class);
        HINTS.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        HINTS.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.allOf(BarcodeFormat.class));
        HINTS_PURE = new EnumMap<>(HINTS);
        HINTS_PURE.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
    }


    public static final String decodeQr(String imageURIString) throws PayException {
        if (imageURIString == null || imageURIString.isEmpty()) {
            throw new PayException("图片地址不能为空");
        }
        imageURIString = imageURIString.trim();
        URI imageURI;
        try {
            imageURI = new URI(imageURIString);
            // Assume http: if not specified
            if (imageURI.getScheme() == null) {
                imageURI = new URI("http://" + imageURIString);
            }
        } catch (URISyntaxException e) {
            throw new PayException("二维码图片地址不正确,".concat(imageURIString));
        }

        // Shortcut for data URI
        if ("data".equals(imageURI.getScheme())) {
            BufferedImage image = null;
            try {
                image = ImageReader.readDataURIImage(imageURI);
            } catch (IOException | IllegalStateException e) {
                log.info("Error " + e + " while reading data URI: " + imageURIString);
                throw new PayException("二维码读取出错,".concat(imageURIString));
            }
            if (image == null) {
                throw new PayException("二维码读取出错,".concat(imageURIString));
            }
            try {
                return processImage(image);
            } catch (Exception e) {
                throw new PayException("处理二维码数据出错,".concat(imageURIString).concat(", ").concat(e.getMessage()));
            } finally {
                image.flush();
            }
        }

        URL imageURL;
        try {
            imageURL = imageURI.toURL();
        } catch (MalformedURLException ignored) {
            throw new PayException("二维码URL不正确,".concat(imageURIString));
        }

        String protocol = imageURL.getProtocol();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            throw new PayException("二维码网址协议不支持,".concat(imageURIString));
        }


        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) imageURL.openConnection();
        } catch (Exception e) {
            throw new PayException("二维码URL无法访问,".concat(imageURIString));
        }

        connection.setAllowUserInteraction(false);
        connection.setInstanceFollowRedirects(true);
        connection.setReadTimeout(5000);
        connection.setConnectTimeout(5000);
        connection.setRequestProperty(HttpHeaders.USER_AGENT, "zxing.org");
        connection.setRequestProperty(HttpHeaders.CONNECTION, "close");

        try {
            connection.connect();
        } catch (IOException | IllegalArgumentException e) {
            // Encompasses lots of stuff, including
            //  java.net.SocketException, java.net.UnknownHostException,
            //  javax.net.ssl.SSLPeerUnverifiedException,
            //  org.apache.http.NoHttpResponseException,
            //  org.apache.http.client.ClientProtocolException,
            throw new PayException("二维码URL无法访问,".concat(imageURIString).concat(e.getMessage()));
        }

        try (InputStream is = connection.getInputStream()) {
            try {
                if (connection.getResponseCode() != HttpServletResponse.SC_OK) {
                    throw new PayException("无法获取二维码URL图片，服务器返回：,".concat(String.valueOf(connection.getResponseCode())));
                }
                if (connection.getHeaderFieldInt(HttpHeaders.CONTENT_LENGTH, 0) > MAX_IMAGE_SIZE) {
                    throw new PayException("二维码URL图片过大,".concat(imageURIString).concat("超过最大大小：").concat(String.valueOf(MAX_IMAGE_SIZE)));
                }
                // Assume we'll only handle image/* content types,去掉这里的判断，完全是兼容一些奇葩的第三方，比如高通，返回图片，但响应头却不是图片的。Content-Type: text/html; charset=utf-8
//                String contentType = connection.getContentType();
//                if (contentType != null &&   !contentType.startsWith("image/")     ) {
//                    throw new PayException("二维码解析失败，响应类型错误 " + contentType + ": " + imageURIString);
//                }
                return processStream(is);
            } catch (Exception e) {
                throw new PayException(e.getMessage());
            } finally {
                consumeRemainder(is);
            }
        } catch (Exception e) {
            throw new PayException(e.getMessage());
        } finally {
            connection.disconnect();
        }

    }

    private static void consumeRemainder(InputStream is) {
        try {
            while (is.read(REMAINDER_BUFFER) > 0) {
                // don't care about value, or collision
            }
        } catch (IOException | IndexOutOfBoundsException ioe) {
            // sun.net.www.http.ChunkedInputStream.read is throwing IndexOutOfBoundsException
            // continue
        }
    }


    private static String processStream(InputStream is) throws PayException {
        BufferedImage image;
        try {
            image = ImageIO.read(is);
        } catch (IOException | CMMException | IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            // Have seen these in some logs, like an AIOOBE from certain GIF images
            // https://github.com/zxing/zxing/issues/862#issuecomment-376159343
            // log.info(e.toString());
            throw new PayException("处理二维码流错误,".concat(e.getMessage()));
        }
        if (image == null) {
            throw new PayException("处理二维码流错误,二维码图片无数据");
        }
        try {
            int height = image.getHeight();
            int width = image.getWidth();
            if (height <= 1 || width <= 1 || height * width > MAX_PIXELS) {
                //log.info("Dimensions out of bounds: " + width + 'x' + height);
            }
            return processImage(image);
        } catch (Exception e) {
            throw new PayException(e.getMessage());
        } finally {
            image.flush();
        }
    }

    public static String processImage(BufferedImage image) throws PayException {
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
        Collection<Result> results = new ArrayList<>(1);

        try {
            Reader reader = new MultiFormatReader();
            ReaderException savedException = null;
            try {
                // Look for multiple barcodes
                MultipleBarcodeReader multiReader = new GenericMultipleBarcodeReader(reader);
                Result[] theResults = multiReader.decodeMultiple(bitmap, HINTS);
                if (theResults != null) {
                    results.addAll(Arrays.asList(theResults));
                }
            } catch (ReaderException re) {
                savedException = re;
            }

            if (results.isEmpty()) {
                try {
                    // Look for pure barcode
                    Result theResult = reader.decode(bitmap, HINTS_PURE);
                    if (theResult != null) {
                        results.add(theResult);
                    }
                } catch (ReaderException re) {
                    savedException = re;
                }
            }

            if (results.isEmpty()) {
                try {
                    // Look for normal barcode in photo
                    Result theResult = reader.decode(bitmap, HINTS);
                    if (theResult != null) {
                        results.add(theResult);
                    }
                } catch (ReaderException re) {
                    savedException = re;
                }
            }

            if (results.isEmpty()) {
                try {
                    // Try again with other binarizer
                    BinaryBitmap hybridBitmap = new BinaryBitmap(new HybridBinarizer(source));
                    Result theResult = reader.decode(hybridBitmap, HINTS);
                    if (theResult != null) {
                        results.add(theResult);
                    }
                } catch (ReaderException re) {
                    savedException = re;
                }
            }

            if (results.isEmpty()) {
                try {
                    throw savedException == null ? NotFoundException.getNotFoundInstance() : savedException;
                } catch (FormatException | ChecksumException e) {
                    log.info(e.toString());
                } catch (ReaderException e) { // Including NotFoundException
                    log.info(e.toString());
                }
            }

        } catch (RuntimeException e) {
            throw new PayException("处理二维码出错".concat(e.getMessage()));
        }

        if (null != results && !results.isEmpty() && results.size() == 1) {
            return ((ArrayList<Result>) results).get(0).getText();
        }
        return null;
    }


}
