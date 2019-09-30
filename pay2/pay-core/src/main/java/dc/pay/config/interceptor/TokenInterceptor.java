package dc.pay.config.interceptor;

import com.alibaba.fastjson.JSON;
import dc.pay.dao.pay.RequestPayDao;
import dc.pay.config.RunTimeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.InetAddress;

public class TokenInterceptor implements ClientHttpRequestInterceptor
{
    private static final Logger log =  LoggerFactory.getLogger(RequestPayDao.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException
    {
        //请求地址
        String checkTokenUrl = request.getURI().toString();
        //token有效时间
        int ttTime = (int) (System.currentTimeMillis() / 1000 + 1800);
        String methodName = request.getMethod().name();
        //请求内容
        String requestBody = new String(body);
       // String token = TokenHelper.generateToken(checkTokenUrl, ttTime, methodName, requestBody);
        //request.getHeaders().add("X-Auth-Token",token);
       // request.getHeaders().set("Accept","text/plain, application/json, application/*+json,  */*");  //设置头,可取消
        ClientHttpResponse response = execution.execute(request, body);
        HttpStatus statusCode = response.getStatusCode();
        if(statusCode.value()!=200){
            log.info("######################################################################################################");
            log.info("响应代码："+statusCode);
            log.info("服务器："+   InetAddress.getLocalHost());
            log.info("HEAD大小："+(request.getHeaders().size()));
            log.info("REQUEST:"+JSON.toJSONString(request));
            log.info("RESPONSE:"+JSON.toJSONString(response));
            log.info("请求地址："+checkTokenUrl);
            log.info("请求方法名："+methodName);
            log.info("请求内容："+requestBody);
            RunTimeInfo.printMemoryInfo();
            log.error("######################################################################################################");
        }




        return response;
    }
}
