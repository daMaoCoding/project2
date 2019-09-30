package dc.pay.business.caifubao;

import java.io.DataInputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class DataHelper {

	public final static String UTFEncode="UTF-8";
	public final static String GBKEncode="GBK";


	//获取得到的参数值。保存在stirng中
	public static String GetQueryString(Map<String, String> map){
		Iterator<Entry<String, String>> iter=map.entrySet().iterator();
		StringBuilder sb=new StringBuilder();
		while(iter.hasNext()){
			Entry<String, String> entry=iter.next();
			Object key=entry.getKey().toString();
			Object value=entry.getValue().toString();
			sb.append(key+"="+value).append("&");
		}
		if(sb.length()==0) return "";
		return sb.substring(0,sb.length()-1);
	}
	//对值进行转码，转换成自己的编码
	public static void TranferCharsetEncode(Map<String,String> map) throws UnsupportedEncodingException{
		for(Entry<String,String> entry : map.entrySet()){
			if(entry.getValue()==null) continue;
			String UTFEncode=URLEncoder.encode(entry.getValue(),DataHelper.UTFEncode);
			entry.setValue(UTFEncode);
		}
	}
	//得到排序后的值并转换为小写
	public static String GetSortQueryToLowerString(Map<String,String> map, boolean removeempty){
		List<Entry<String,String>> keyValue=new ArrayList<Entry<String,String>>(map.entrySet());
		//根据keyValue的大小进行牌勋
		Collections.sort(keyValue,new Comparator<Entry<String,String>>() {
			//�Ƚ�����ֵ��С��0=��ȣ�С��0=01<02,������0=01>02,��
			@Override
			public int compare(Entry<String, String> o1,
					Entry<String, String> o2) {
				// TODO Auto-generated method stub
				return (o1.getValue()).toString().compareTo(o2.getKey());
			}

		});
		StringBuilder sb=new StringBuilder();
		for(int i=0;i<keyValue.size();i++){
            String value = keyValue.get(i).getValue();
            if ( removeempty && StringUtil.isBlankOrEmpty(value) ){
                continue;
            }
			if(value==null){
				sb.append(keyValue.get(i).getKey()+"=");
			}else{
				sb.append(keyValue.get(i).getKey()+"="+value.toLowerCase());
			}
			sb.append("&");
		}
		return sb.substring(0,sb.length()-1);
	}
	//得到排序后的值
		public static String GetSortQueryString(Map<String,String> map, boolean removeempty){
			List<Entry<String,String>> keyValue=new ArrayList<Entry<String,String>>();
			//根据keyValue的大小进行牌勋
			Collections.sort(keyValue,new Comparator<Entry<String,String>>() {
				//�Ƚ�����ֵ��С��0=��ȣ�С��0=01<02,������0=01>02,��
				@Override
				public int compare(Entry<String, String> o1,
						Entry<String, String> o2) {
					// TODO Auto-generated method stub
					return (o1.getValue()).toString().compareTo(o2.getKey());
				}

			});
			StringBuilder sb=new StringBuilder();
			for(int i=0;i<keyValue.size();i++){
                String value = keyValue.get(i).getValue();
                if ( removeempty && StringUtil.isBlankOrEmpty(value) ){
                    continue;
                }
				sb.append(keyValue.get(i).getKey()+"="+value);
				sb.append("&");
			}
			return sb.substring(0,sb.length()-1);
		}

		public static String RequestGetUrl(String getUrl)
		{
			return GetPostUrl(null,getUrl,"GET");
		}

		public static String RequestPostUrl(String getUrl,String postData)
		{
			return GetPostUrl(postData,getUrl,"POST");
		}

		public static String GetPostUrl(String postData,String postUrl,String submitMethod){
			URL url=null;
			HttpURLConnection huconn=null;
			try{
				url=new URL(postUrl);
				huconn=(HttpURLConnection) url.openConnection();
				huconn.setRequestMethod(submitMethod.toUpperCase());
				huconn.setRequestProperty("Conntent-Type", "application/x-www-form-urlencoded");
				huconn.setDoInput(true);
				huconn.setDoOutput(true);
				if(submitMethod.equalsIgnoreCase("POST")){
					huconn.getOutputStream().write(postData.getBytes(GBKEncode));
					huconn.getOutputStream().flush();
					huconn.getOutputStream().close();
				}
				int code=huconn.getResponseCode();
				if(code==200){
					DataInputStream in=new DataInputStream(huconn.getInputStream());
					int len=in.available();
					byte[] by=new byte[len];
					in.readFully(by);
					String rev=new String(by,GBKEncode);
					in.close();
					return rev;
				}
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				if(huconn!=null){
					huconn.disconnect();
				}
			}
			return null;
		}
}