package dc.pay.entity.admin;

/**
 * <p>
 * 部门表
 * </p>
 *
 */
public class Dept extends Base {

    private static final long serialVersionUID = 1L;
    /**
     * 排序
     */
    private Integer num;
    /**
     * 父部门id
     */
    private Integer pid;
    /**
     * 父级ids
     */
    private String  pids;
    /**
     * 简称
     */
    private String  simplename;
    /**
     * 全称
     */
    private String  fullname;
    /**
     * 提示
     */
    private String  tips;
    /**
     * 版本（乐观锁保留字段）
     */
    private Integer version;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public Integer getPid() {
        return pid;
    }

    public void setPid(Integer pid) {
        this.pid = pid;
    }

    public String getPids() {
        return pids;
    }

    public void setPids(String pids) {
        this.pids = pids;
    }

    public String getSimplename() {
        return simplename;
    }

    public void setSimplename(String simplename) {
        this.simplename = simplename;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getTips() {
        return tips;
    }

    public void setTips(String tips) {
        this.tips = tips;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Dept{" +
                "id=" + id +
                ", num=" + num +
                ", pid=" + pid +
                ", pids=" + pids +
                ", simplename=" + simplename +
                ", fullname=" + fullname +
                ", tips=" + tips +
                ", version=" + version +
                "}";
    }
}
