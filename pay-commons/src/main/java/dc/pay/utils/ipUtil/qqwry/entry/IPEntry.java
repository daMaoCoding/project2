package dc.pay.utils.ipUtil.qqwry.entry;



/**
 * 一条IP范围记录，不仅包括国家和区域，也包括起始IP和结束IP
 */
public class IPEntry {


    //类属性
    public String beginIp;//其实IP
    public String endIp;//终止IP
    public String country;//国家
    public String province;//省
    public String city;//市（州，盟，地区）
    public String district;//县（区）
    public String area;//原始的地址字段
    public String flage;//地址字段的类型

    /**
     * 构造函数
     */
    public IPEntry() {
        beginIp = endIp = country = province = city = district = area = flage = "";
    }

    public IPEntry(String country, String province, String city, String district, String flage, String area) {
        this.country = country;
        this.province = province;
        this.city = city;
        this.district = district;
        this.area = area;
        this.flage = flage;
    }

    public String getCountry() {
        return country;
    }

    public String getCity() {
        return city;
    }

    public String getArea() {
        return area;
    }

    public String getBeginIp() {
        return beginIp;
    }

    public String getDistrict() {
        return district;
    }

    public String getEndIp() {
        return endIp;
    }

    public String getFlage() {
        return flage;
    }

    public String getProvince() {
        return province;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public void setBeginIp(String beginIp) {
        this.beginIp = beginIp;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public void setEndIp(String endIp) {
        this.endIp = endIp;
    }

    public void setFlage(String flage) {
        this.flage = flage;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    /**
     * 默认显示方式是以“|”分隔
     * @return
     */
    public String getAreaSplited() {
        return country + "|" + province + "|" + city + "|" + district;
    }

    /**
     * 显示省市县字段拼接成的字符串，并指定分隔符
     * @param splitStr 指定分隔符
     * @return 返回由省市县字段拼接成的字符串，eg：中国|河北|保定|定州
     */
    public String getAreaSplited(String splitStr) {
        return country + splitStr + province + splitStr + city + splitStr + district;
    }
}