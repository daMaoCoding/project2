package dc.pay.config;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;

public class PayHttpRequestRetryHandler implements HttpRequestRetryHandler {
    @Override
    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
        if(1==1) return false;
        if (executionCount <= 5){
            return true;
        }
        if (exception instanceof InterruptedIOException || exception instanceof NoHttpResponseException) {
            // Timeout or 服务端断开连接
            return true;
        }
        // Unknown host
        if (exception instanceof UnknownHostException) {
            return true;
        }
        // SSL handshake exception
        if (exception instanceof SSLException) {
            return true;
        }

        final HttpClientContext clientContext = HttpClientContext.adapt(context);
        final HttpRequest request = clientContext.getRequest();
        boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
        if (idempotent) {
            // Retry if the request is considered idempotent
            return true;
        }
        return false;
    }
}