package dc.pay.utils.qr.base64;

abstract class Base64Decoder {

  private static final Base64Decoder INSTANCE;
  static {
    Base64Decoder instance;
    try {
      Class.forName("java.util.Base64");
      // If succeeds, then:
      instance = new Java8Base64Decoder();
    } catch (ClassNotFoundException cnfe) {
      instance = new JAXBBase64Decoder();
    }
    INSTANCE = instance;
  }

  abstract byte[] decode(String s);

  static Base64Decoder getInstance() {
    return INSTANCE;
  }

}
