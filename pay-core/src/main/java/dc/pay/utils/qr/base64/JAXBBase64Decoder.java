package dc.pay.utils.qr.base64;
import java.lang.reflect.InvocationTargetException;
final class JAXBBase64Decoder extends Base64Decoder {
  @Override
  byte[] decode(String s) {
    try {
      return (byte[]) Class.forName("javax.xml.bind.DatatypeConverter")
          .getMethod("parseBase64Binary", String.class).invoke(null, s);
    } catch (IllegalAccessException | InvocationTargetException |
             NoSuchMethodException | ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }
}
