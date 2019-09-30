package dc.pay.payrest;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import dc.pay.entity.jpa.ReqPayList;
import dc.pay.services.jpaRepository.ReqPayListRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;


@Controller
@RequestMapping(path="/wy")
public class JumpController {
	@Autowired
	private ReqPayListRepository resPayListRepository;
	

	@RequestMapping(value = {"/jmp/{orderId}"},method = {RequestMethod.POST, RequestMethod.GET},produces = {"text/plain;charset=UTF-8" })
	public void  jumpToBank(@PathVariable(value = "orderId",required = true) String orderId, HttpServletResponse response) {
		String resPMsg = "Error";

		ReqPayList reqPayList = resPayListRepository.findbyOrOrderId(orderId);
		if(reqPayList!=null && StringUtils.isNotBlank( reqPayList.getRequestPayResult())){
			String str = reqPayList.getRequestPayResult().replaceAll("/dc.pay.business.RequestPayResult","");
			str=str.replaceAll("timestampB-timestampC>90000","timestampB-timestampC>3600000").replaceAll("1分半钟","60分钟");
			JSONObject jsonObject = JSON.parseObject(str);
			if(null!=jsonObject && jsonObject.containsKey("requestPayHtmlContent")){
				resPMsg =jsonObject.getString("requestPayHtmlContent");
			}
		}
		writeResponse(response,resPMsg);
	}




	public  void writeResponse(HttpServletResponse response, String result){
		response.setContentType("text/html; charset=utf-8");
		PrintWriter out = null;
		try {
			out = response.getWriter();
			if(null!=result && !"".equalsIgnoreCase(result)){
				out.println("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
				out.print(result);
				out.println("</html>");
				out.flush();
			}else{
				out.print("ERROR");
				out.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			if(null!=out){
				out.close();
			}
		}

	}

}
