package dc.pay.entity.tj;

import dc.pay.base.BaseEntity;

import javax.persistence.Column;
import java.util.Date;

public class TjStatus extends BaseEntity {

      @Column(name = "tj_time_stmp")
      Date tjTimeStmp;  //统计日期

      @Column(name = "tj_name")
      String tjName;

      @Column(name = "tj_status")
      String tjStatus;

      @Column(name = "tj_is_locked")
      int tjIsLocked;

      @Column(name = "tj_locker")
      String tjLocker;

      @Column(name = "time_stmp")
      Date timeStmp;    //统计操作时间

    @Column(name = "tj_count")
      int tjCount;


    public Date getTjTimeStmp() {
        return tjTimeStmp;
    }

    public void setTjTimeStmp(Date tjTimeStmp) {
        this.tjTimeStmp = tjTimeStmp;
    }

    public String getTjName() {
        return tjName;
    }

    public void setTjName(String tjName) {
        this.tjName = tjName;
    }

    public String getTjStatus() {
        return tjStatus;
    }

    public void setTjStatus(String tjStatus) {
        this.tjStatus = tjStatus;
    }

    public int getTjIsLocked() {
        return tjIsLocked;
    }

    public void setTjIsLocked(int tjIsLocked) {
        this.tjIsLocked = tjIsLocked;
    }

    public String getTjLocker() {
        return tjLocker;
    }

    public void setTjLocker(String tjLocker) {
        this.tjLocker = tjLocker;
    }

    public Date getTimeStmp() {
        return timeStmp;
    }

    public void setTimeStmp(Date timeStmp) {
        this.timeStmp = timeStmp;
    }

    public int getTjCount() {
        return tjCount;
    }

    public void setTjCount(int tjCount) {
        this.tjCount = tjCount;
    }
}
