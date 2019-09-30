package dc.pay.utils.qr.base64;
import java.lang.reflect.InvocationTargetException;
final class Java8Base64Decoder extends Base64Decoder {
  @Override
  byte[] decode(String s) {
    try {
      Object decoder = Class.forName("java.util.Base64")
          .getMethod("getDecoder").invoke(null);
      return (byte[]) Class.forName("java.util.Base64$Decoder")
          .getMethod("decode", String.class).invoke(decoder, s);
    } catch (IllegalAccessException | NoSuchMethodException | ClassNotFoundException e) {
      throw new IllegalStateException(e);
    } catch (InvocationTargetException ite) {
      throw new IllegalStateException(ite.getCause());
    }
  }
}
