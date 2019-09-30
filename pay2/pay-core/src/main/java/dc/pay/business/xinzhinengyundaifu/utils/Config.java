package dc.pay.business.xinzhinengyundaifu.utils;

public class Config {

	// 商户编号
	public static String MERCHANTID = "1D313241B1";


	// 商户RSA私钥 (长度2048)
	public static final String PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDlkBbzJ8eLHvpANMamyPGzXn+nOQl5PWaduEaA/p/abS4l26diKQfDtwxIgtzByr4cSKoZvnw6t3eetK8A07x1rwQhx4LUY5PRatAGKRvm9vI41+YEC0eMT5uW4NtuNi2IUoW4WHKb9MiOjfEupl0zgmhmZBbfAX6FYAHzjzHY+h1qjGvuwFFHt0B8bYFYXkb2l+Y3abYNYHxB2F/DRjSw7pcbdM5NT+DW3Sg8FddL3CuP8uh6+BZIt7v5mOgR0btTGljtPX6fRzBUl1Blulr7Uyz7UyUjLKQz2XR51T4p7fzNS90sPbhnXvwwbaTpuQUqoEG4RSKBFiAiiKnAG+PvAgMBAAECggEBAMCdhvQdA4wSwO1QmwHkhxD2kGtyKyXERC7AKMAhZWUzyM3RIP/252HrW+4Xhz0/bQZ0Xe1d7ASkXkUW2+P4xR+FaxUgvwCQUL4dPlB8+8FuoMzbMk9gW3c6cJVHNDakVM4WV8bWx8tdt04NgTmgM4F9wTVwc9RH/63PYbPICY5Ar7Qos1YLgDuTOI8wPvfmhswtnJr4XhGNBooPVmdXQmyxMhtIOc9lIARg1OQ2ugZAlblYovwBfk721Xa+1ZvW7CsZl6mFZgjwL1Vr3ATMlHaPkC5zd1dpmKTlxDIyn2Y0F0lb6iI3HDYLIpuZPDwq2WIRXKZ77wzbNlo9Zbo0wukCgYEA+Aa102kzGD0GUdTroAoZjvpKmN3N3z0L5oldOiYhkifioC1zgp6sopD0gU/tIqzN7meeqlsxNUskGsCjadIuiafgnC7Ib2yHlme2xdFFBiFdlGDwnwe22aVyx/g3M8MIWunk58nK+3KZadpXDgmNdDG/nPnrjrdM4W888CuBMeMCgYEA7PFsY1pIzy43CFnENennQuuZ0hDOeM5euVufnHJFu1XVZ0AK1ZMSqS3ZmYFU4Vi49cG+dLFQbGfs/Cjtw29lQez4pnGXJOjpXBCudddjJ75sDk/jum91jIPb8eMV0sF+tPIabfTF+d4gO+do4Cyc2tFW+Rxeg6dAJRMUKEVcc4UCgYB41vwbhnOoaYL8t5odHQ6axIM3u0kkbJ+xsFdFj3JnvRsDI2HVz+0YPzuBDSnh7QNgFggNw+RTrMpoZla3pfmag7pBHeH1t5DPzLQV6QGSHQt00U4qjR2fKKalmt96TplxgedOrthAbfMkyYYEhs7uP5a1qmYD4HaKlyG/edK2NQKBgFlkF8U/6ZooUGDZJPpGHZaUtjqXVp4jFX+Ovp/SSNaNYm6Krcu3qeGjx6VvMqu1wkoNZyI1Rycu6iyT8Ge8HhKKKaD519D4AhpWvMr+kI0M/U1E/KB4rnttv8v5JWGW2IvBjxEOuKY1FColet6gVikDs3FWL54xujSChydOS7vVAoGAQV2tupMpvqsjs/ZX7DaHGrTXhSnOGdgEGseM1H/oqg3IDeBK+Jyi2j40qMdLpQu1Vq9B/AsavJBfVPMI1QF/xLkaT8eyQhRcaBaZ2jhlWcB52SAG8S2y5cIkMjSjUjZlCZ1VvgGUEXmkfJisBNSK2kNpZQpRybNmXquRyUrJMKs=";

	//平台RSA公钥
	public static final String PLATFORM_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAz4RovsOpH2URK+tYbOEiJFRG3xTOfIclbsFHLNCRA8Yu3I0NqahCvgSAIdIWQ9Inok7KOfCjhIJXFVmPmIWO3pawy+7Vt12lG280UcKJuu+kLAHiGkgLWp+VWPwFEDE43zAoK+0lVVvgPxxseJ8sWTBPZWz+ese5Cdqzlgku7jIg7suMV5QRV3ew6WJyAP+aZAfjlAnSnq8DWboHVdZ+LawQ7alZi79h5rcsMS7TX95/FR+ikZvugMhIwxdCv8P53LKgkzLNZYtvGCsacpxglArSZ7MAeSzWyiSC039Bw7rigo67PEXNBGooRxVOYJtD3TMrqwG0Osiq61GOZgiPvQIDAQAB";


	//版本代码 
	public static String VERSION = "v1";
	
	//编码
	public static String CHARSET = "UTF-8";
	
	//签名类型  目前只支持 RSA
	public static String SIGNTYPE = "RSA"; 
	
	//异步通知地址
	public static String notifyUrl = "http://127.0.0.1:8898/notifyUrl";
	
//	//提现接口地址
	public static String API_PAY = "https://www.yefupay.cn/gateway/service/pay";

	//支付结果查询地址
	public static String API_PAY_QUERY = "https://www.yefupay.cn/gateway/service/query";

	//查询余额地址
	public static String API_QUERY_BALANCE = "https://www.yefupay.cn/gateway/service/balance";

	//提现接口地址
//	public static String API_PAY = "http://127.0.0.1:8898/gateway/service/pay";
//
//	//支付结果查询地址
//	public static String API_PAY_QUERY = "http://127.0.0.1:8898/gateway/service/query";
//
//	//查询余额地址
//	public static String API_QUERY_BALANCE = "http://127.0.0.1:8898/gateway/service/balance";
}
