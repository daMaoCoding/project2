package dc.pay.utils;

import org.apache.http.ProtocolException;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Locale;

/**
 * ************************
 *
 * @author tony 3556239829
 */

public class PayLaxRedirectStrategy extends LaxRedirectStrategy {

    @Override
    protected URI createLocationURI(final String location) throws ProtocolException {
        try {
            String url = new String(location.getBytes("ISO-8859-1"),"utf-8"); //youzhifu,并没使用，因为接的时候第三方更改了
            final URIBuilder b = new URIBuilder(new URI(url).normalize());
            final String host = b.getHost();
            if (host != null) {
                b.setHost(host.toLowerCase(Locale.ROOT));
            }
            final String path = b.getPath();
            if (TextUtils.isEmpty(path)) {
                b.setPath("/");
            }
            return b.build();
        } catch (final URISyntaxException ex) {
            throw new ProtocolException("Invalid redirect URI: " + location, ex);
        }catch (UnsupportedEncodingException ex) {
            throw new ProtocolException("Invalid redirect URI: " + location, ex);
        }
    }




}
