package dc.pay.utils;

import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Seconds;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.crypto.Data;

/**
 * 日期工具类
 */
public class DateUtil {

    private static final Logger logger = LoggerFactory.getLogger(DateUtil.class);

	public static final String dateString = "yyyy-MM-dd";
	public static final String timeString = "HH:mm:ss";
	public static final String dateTimeString = "yyyy-MM-dd HH:mm:ss";
	public static final String dateTimeString2 = "yyyyMMddHHmmss";

	public static final String zeroClock = " 00:00:00";
	public static final String sevenClock = " 07:00:00";
	public static final String sixFiftyNineClock = " 06:59:59";
	public static final String sixFiftyNineEightClock = " 06:59:58";


	public static final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(dateTimeString);

	/**
	 * 由开始日期转换为开始时间，一般在以时间为条件的查询中使用
	 */
	public static Date date2StartTime(Date startDate) {
		if (startDate == null)
			return null;
		SimpleDateFormat sdf = new SimpleDateFormat(dateString);
		String dateStr = sdf.format(startDate);
		sdf = new SimpleDateFormat(dateTimeString);
		Date startTime = null;
		try {
			startTime = sdf.parse(dateStr + " 00:00:00");
		} catch (ParseException e) {
			logger.error(e.getMessage(),e);
		}
		return startTime;
	}

	/**
	 * 由结束日期转换为结束时间，一般在以时间为条件的查询中使用
	 */
	public static Date date2EndTime(Date endDate) {
		if (endDate == null)
			return null;
		SimpleDateFormat sdf = new SimpleDateFormat(dateString);
		String dateStr = sdf.format(endDate);
		sdf = new SimpleDateFormat(dateTimeString);
		Date endTime = null;
		try {
			endTime = sdf.parse(dateStr + " 23:59:59");
		} catch (ParseException e) {
			logger.error(e.getMessage(),e);
		}
		return endTime;
	}

	/**
	 * 获得当前日期
	 */
	public static Date curDate() {
		return new Date();
	}

	/**
	 * 获得当前日期，SQL日期类型
	 */
	public static java.sql.Date curSqlDate() {
		return new java.sql.Date(System.currentTimeMillis());
	}

	/**
	 * 获得当前日期字符串: yyyy-MM-dd
	 */
	public static String curDateStr() {
		SimpleDateFormat sdf = new SimpleDateFormat(dateString);
		return sdf.format(new Date());
	}

	/**
	 * 获得当前日期字符串：yyyy-MM-dd HH:mm:ss
	 */
	public static String formatDateTimeStrByParam(String pattern) {
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		return sdf.format(new Date());
	}


	/**
	 * 获得当前日期字符串：
	 */
	public static String curDateTimeStr() {
		SimpleDateFormat sdf = new SimpleDateFormat(dateTimeString);
		return sdf.format(new Date());
	}




	/**
	 * 格式化日期
	 * 
	 * @param date
	 * @return
	 */
	public static String formatDate(Date date) {
		if (date == null) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(dateString);
		return sdf.format(date);
	}

	/**
	 * 格式化时间
	 * 
	 * @param date
	 * @return
	 */
	public static String formatTime(Date date) {
		if (date == null) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(timeString);
		return sdf.format(date);
	}

	/**
	 * 格式化日期时间
	 * 
	 * @param date
	 * @return
	 */
	public static String formatDateTime(Date date) {
		if (date == null) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(dateTimeString);
		return sdf.format(date);
	}

	/**
	 * 字符串转换为日期
	 * 
	 * @param dateStr
	 * @return
	 */
	public static Date parseDate(String dateStr) {
		if (StringUtils.isEmpty(dateStr)) {
			return null;
		}
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(dateString);
			return sdf.parse(dateStr);
		} catch (ParseException e) {
			return null;
		}
	}

	public static String getCurrentWeekFirst() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, -(c.get(Calendar.DAY_OF_WEEK) - 2));
		SimpleDateFormat sdf = new SimpleDateFormat(dateString);
		return sdf.format(c.getTime());
	}

	public static String getCurrentWeekLast() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, -(c.get(Calendar.DAY_OF_WEEK) - 2));
		c.add(Calendar.DATE, 6);
		SimpleDateFormat sdf = new SimpleDateFormat(dateString);
		return sdf.format(c.getTime());
	}

	/**
	 * 字符串转换为SQL日期对象，如果出错，则返回null
	 * 
	 * @param dateStr
	 * @return
	 */
	public static java.sql.Date parseSqlDate(String dateStr) {
		Date d = parseDate(dateStr);
		if (d != null) {
			return new java.sql.Date(d.getTime());
		}
		return null;
	}

	public static Date parseDateTime(String datetimeStr) {
		if (StringUtils.isEmpty(datetimeStr)) {
			return null;
		}
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(dateTimeString);
			return sdf.parse(datetimeStr);
		} catch (ParseException e) {
			return null;
		}
	}

	public static java.sql.Timestamp parseSqlDateTime(String datetimeStr) {
		Date d = parseDateTime(datetimeStr);
		if (d != null) {
			return new java.sql.Timestamp(d.getTime());
		}
		return null;
	}

	public static void main(String[] args) {
		logger.info(DateUtil.getCurrentWeekFirst());
		logger.info(DateUtil.getCurrentWeekLast());
	}

	/*****************************************************************
	 * copy from summer
	 *****************************************************************/
	public final static int MAX_YEAR = 3000;

	public final static int MIN_YEAR = 1900;

	public final static String DATE_SEPERATOR = "-";

	public final static String DF_EN_DATE = "yyyy-MM-dd";

	public final static String DF_EN_DATETIME = "yyyy-MM-dd HH:mm:ss";

	public final static String DF_CN_DATE = "yyyy年MM月dd日";

	public final static String DF_CN_DATETIME = "yyyy年MM月dd日 HH时mm分ss秒";

	/**
	 * 按照[yyyy-MM-dd]格式生成当前日期字符串（例如：2001年1月12日为“2001-01-12”）
	 * 
	 * @return String 当前日期字符串[yyyy-MM-dd]
	 */
	public static final String getCurDate() {
		Calendar today = Calendar.getInstance();

		SimpleDateFormat formatDateTime = new SimpleDateFormat("yyyy-MM-dd");

		return formatDateTime.format(today.getTime());
	}

	/**
	 * 按照[yyyy-MM-dd]格式转换日期字符串到Date对象（例如：2001年1月12日为“2001-01-12”）
	 * 
	 * @return Date 日期对象,如果日期字符创不合法，返回当前日期
	 * @throws ParseException
	 */
	public static final Date getDateFromString(String sDate) throws ParseException {
		SimpleDateFormat formatDateTime = new SimpleDateFormat("yyyy-MM-dd");
		return formatDateTime.parse(sDate);
	}

	/**
	 * 按照[yyyy年MM月dd日]格式转换日期字符串到Date对象（例如：2001年1月12日为“2001年01月12日”）
	 * 
	 * @return Date 日期对象,如果日期字符创不合法，返回当前日期
	 * @throws ParseException
	 */
	public static final Date getDateFromCHString(String sDate) throws ParseException {
		SimpleDateFormat formatDateTime = new SimpleDateFormat("yyyy年MM月dd日");
		return formatDateTime.parse(sDate);
	}

	/**
	 * 按照[yyyy年MM月dd日]格式转换日期字符串到Date对象（例如：2001年1月12日为“2001年01月12日”）
	 *
	 * @return Date 日期对象,如果日期字符创不合法，返回当前日期
	 * @throws ParseException
	 */
	public static final Date getDateFromPartStr(String sDate,String part) throws ParseException {
		SimpleDateFormat formatDateTime = new SimpleDateFormat(part);
		return formatDateTime.parse(sDate);
	}



	/**
	 * 两个日期之间天数
	 */
	public static final int daysBetween(String startStr,String endStr,String dataFormatPart) {
		try{
			DateTime start = new DateTime(DateUtil.getDateFromPartStr(startStr,dataFormatPart));
			DateTime end = new DateTime(DateUtil.getDateFromPartStr(endStr,dataFormatPart)); //.getTime()+1000
			int days = Days.daysBetween(start, end).getDays();
			return days;
		}catch (Exception ex){
			return 0;
		}

	}





	/**
	 * 按照[yyyymmdd]格式生成当前日期字符串（例如：2001年1月12日为“20010112”）
	 * 
	 * @return String 当前日期字符串[yyyymmdd]
	 */
	public static final String getCurDateString() {
		StringBuffer result = new StringBuffer();

		int iYear, iMonth, iDate;

		Calendar today = Calendar.getInstance();

		iYear = today.get(Calendar.YEAR);
		result.append(iYear);

		iMonth = today.get(Calendar.MONTH) + 1;
		if (iMonth < 10)
			result.append("0");
		result.append(iMonth);

		if ((iDate = today.get(Calendar.DATE)) < 10)
			result.append("0");
		result.append(iDate);

		return result.toString();
	}

	/**
	 * 判断日期时间1是否等于日期时间2
	 * 
	 * @param sDateTime1
	 *            String 日期时间1[yyyy-MM-dd HH:mm:ss]
	 * @param sDateTime2
	 *            String 日期时间2[yyyy-MM-dd HH:mm:ss]
	 * @return int 比较结果
	 *         <ul>
	 *         <li>0：相等/失败；
	 *         <li>小于0：日期时间1小于日期时间2；
	 *         <li>大于0：日期时间1大于日期时间2
	 *         </ul>
	 */
	public static final int equalDateTime(final String sDateTime1, final String sDateTime2) {
		SimpleDateFormat formatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		try {
			Date dateTime1 = formatDateTime.parse(sDateTime1);
			Date dateTime2 = formatDateTime.parse(sDateTime2);

			if (dateTime1.equals(dateTime2))
				return 0;
			else if (dateTime1.after(dateTime2))
				return 1;
			else
				return -1;
		} catch (Exception e) {
			return 0;
		}
	}

	/**
	 * 判断日期1是否晚于日期2
	 * 
	 * @param sDate1
	 *            String 日期1[yyyy-MM-dd]
	 * @param sDate2
	 *            String 日期2[yyyy-MM-dd]
	 * @return int 比较结果
	 *         <ul>
	 *         <li>true：日期1晚于日期2
	 *         <li>false：日期1早于或者等于日期2
	 *         </ul>
	 */
	public static final boolean after(final String sDate1, final String sDate2) {
		int iYear1, iMonth1, iDay1;
		int iYear2, iMonth2, iDay2;

		iYear1 = getYear(sDate1);
		iMonth1 = getMonth(sDate1);
		iDay1 = getDay(sDate1);

		iYear2 = getYear(sDate2);
		iMonth2 = getMonth(sDate2);
		iDay2 = getDay(sDate2);

		Calendar dtDate1 = Calendar.getInstance();
		dtDate1.set(iYear1, iMonth1 - 1, iDay1);
		Calendar dtDate2 = Calendar.getInstance();
		dtDate2.set(iYear2, iMonth2 - 1, iDay2);

		if (dtDate1.after(dtDate2))
			return true;
		else
			return false;
	}

	/**
	 * 判断日期1是否等于日期2
	 * 
	 * @param sDate1
	 *            String 日期1[yyyy-MM-dd]
	 * @param sDate2
	 *            String 日期2[yyyy-MM-dd]
	 * @return int 比较结果
	 *         <ul>
	 *         <li>true：等于
	 *         <li>false：不等于
	 *         </ul>
	 */
	public static final boolean equals(final String sDate1, final String sDate2) {
		int iYear1, iMonth1, iDay1;
		int iYear2, iMonth2, iDay2;

		iYear1 = getYear(sDate1);
		iMonth1 = getMonth(sDate1);
		iDay1 = getDay(sDate1);

		iYear2 = getYear(sDate2);
		iMonth2 = getMonth(sDate2);
		iDay2 = getDay(sDate2);

		Calendar dtDate1 = Calendar.getInstance();
		dtDate1.set(iYear1, iMonth1 - 1, iDay1);
		Calendar dtDate2 = Calendar.getInstance();
		dtDate2.set(iYear2, iMonth2 - 1, iDay2);

		if (dtDate1.equals(dtDate2))
			return true;
		else
			return false;
	}

	/**
	 * 判断指定日期是否晚于当前月
	 * 
	 * @param iYear
	 *            int 年份
	 * @param iMonth
	 *            int 月份
	 * @return int 比较结果
	 *         <ul>
	 *         <li>true：指定日期晚于当前月
	 *         <li>false：指定日期等于或者小于当前月
	 *         </ul>
	 */
	public static final boolean laterMonth(final int iYear, final int iMonth) {
		Calendar today = Calendar.getInstance();

		int iCurY = today.get(Calendar.YEAR);
		int iCurM = today.get(Calendar.MONTH) + 1;

		if (iYear == iCurY) {
			if (iMonth > iCurM)
				return true;
			else
				return false;
		} else if (iYear < iCurY)
			return false;
		else
			return true;
	}

	/**
	 * 得到当前的月份
	 * 
	 * @return int 当前的月份
	 */
	public static final int getMonth() {
		Calendar today = Calendar.getInstance();

		return today.get(Calendar.MONTH) + 1;
	}

	/**
	 * 得到当前的年份
	 * 
	 * @return int 当前的年份
	 */
	public static final int getYear() {
		Calendar today = Calendar.getInstance();
		return today.get(Calendar.YEAR);
	}

	/**
	 * 得到指定日期的年份
	 * 
	 * @param sDate
	 *            String 日期[yyyy-MM-dd]
	 * @return int 指定日期的年份
	 */
	public static final int getYear(String sDate) {
		sDate = getDate(sDate);

		int iIdx1 = sDate.indexOf('-');

		int iYear = Integer.parseInt(sDate.substring(0, iIdx1));

		return iYear;
	}

	/**
	 * @return 获得当前年份的字符串
	 */
	public static String getCurYearString() {
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		return String.valueOf(year);
	}

	/**
	 * 得到指定日期的月份
	 * 
	 * @param sDate
	 *            String 日期[yyyy-MM-dd]
	 * @return int 指定日期的月份
	 */
	public static final int getMonth(String sDate) {
		sDate = getDate(sDate);

		int iIdx1 = sDate.indexOf('-');
		int iIdx2 = sDate.lastIndexOf('-');

		int iMonth = Integer.parseInt(sDate.substring(iIdx1 + 1, iIdx2));

		return iMonth;
	}

	/**
	 * 得到指定日期的日子
	 * 
	 * @param sDate
	 *            String 日期[yyyy-MM-dd]
	 * @return int 指定日期的日子
	 */
	public static final int getDay(String sDate) {
		sDate = getDate(sDate);

		int iIdx2 = sDate.lastIndexOf('-');

		int iDay = Integer.parseInt(sDate.substring(iIdx2 + 1));

		return iDay;
	}

	/**
	 * 得到指定日期时间中的不带秒的日期时间
	 * 
	 *            String 日期时间[yyyy-MM-dd HH:mm:ss]
	 * @return String 指定日期时间的不带秒的日期时间[yyyy-MM-dd HH:mm]
	 */
	public static final String getDateTimeWithoutSecond(final String sDateTime) {
		if (sDateTime.indexOf(':') > 0 && sDateTime.indexOf(':') != sDateTime.lastIndexOf(':'))
			return sDateTime.substring(0, sDateTime.lastIndexOf(':'));
		else
			return sDateTime;
	}

	/**
	 * 得到指定日期时间中的不带秒和微秒的日期时间
	 * 
	 *            String 日期时间[yyyy-MM-dd HH:mm:ss.ms]
	 * @return String 指定日期时间的不带秒的日期时间[yyyy-MM-dd HH:mm]
	 */
	public static final String getDateTime(final String sDateTime) {
		if (sDateTime.indexOf('.') > 0)
			return sDateTime.substring(0, sDateTime.lastIndexOf('.'));
		else
			return sDateTime;
	}

	/**
	 * 得到指定日期时间中的日期
	 * 
	 *            String 日期时间[yyyy-MM-dd HH:mm:ss]
	 * @return String 指定日期时间的日期[yyyy-MM-dd]
	 */
	public static final String getDate(final String sDateTime) {
		int iEndIdx = sDateTime.indexOf(" ");

		if (iEndIdx != -1)
			return sDateTime.substring(0, iEndIdx);
		else
			return sDateTime;
	}

	/**
	 * 得到指定日期的星期索引，以星期一为一周的首日期，索引从“1”开始
	 * 
	 * @param sDate
	 *            String 日期[yyyy-MM-dd]
	 * @return int 指定日期的星期
	 */
	public static final int getWeek(final String sDate) {
		int iYear, iMonth, iDate;

		iYear = getYear(sDate);
		iMonth = getMonth(sDate);
		iDate = getDay(sDate);

		Calendar calendar = Calendar.getInstance();
		calendar.set(iYear, iMonth - 1, iDate);

		int iWeekDay = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;

		if (iWeekDay == 0) // Sunday
			iWeekDay = 7;

		return iWeekDay;
	}

	/**
	 * 得到指定日期的星期名称
	 * 
	 * @param sDate
	 *            String 日期[yyyy-MM-dd | yyyy-MM-dd HH:mm:ss]
	 * @return int 指定日期的星期
	 */
	public static final String getWeekName(String sDate, final Locale locale) {
		MessageFormat formatMsg = new MessageFormat("{0,Date,EEE}");

		if (locale != null)
			formatMsg.setLocale(locale);

		Object[] aobjArg = { convertString2Date(getDate(sDate)) };
		sDate = formatMsg.format(aobjArg);

		return sDate;
	}

	/**
	 * 将日期字符串转换成Date类型的对象
	 * 
	 * @param sDate
	 *            String 日期字符串[yyyy-MM-dd]
	 * 
	 * @return Date Date类型对象
	 */
	private static Date convertString2Date(String sDate) {
		if (sDate == null || sDate.length() == 0) {
			return null;
		}
		SimpleDateFormat formatDateTime = new SimpleDateFormat("yyyy-MM-dd");
		try {
			return formatDateTime.parse(sDate);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 得到指定月的天数
	 * 
	 * @param iYear
	 *            int 年份
	 * @param iMonth
	 *            int 月份
	 * @return int 指定月的天数
	 */
	public static final int getMonthDays(final int iYear, final int iMonth) {
		if (iMonth == 2) {
			if ((iYear % 4) == 0 && (iYear % 100 > 0 || iYear % 400 == 0)) {
				return 29;
			} else {
				return 28;
			}
		}
		if (iMonth > 0) {
			if (iMonth <= 7) {
				if ((iMonth % 2) == 1) {
					return 31;
				} else {
					return 30;
				}
			} else {
				if ((iMonth % 2) == 1) {
					return 30;
				} else {
					return 31;
				}
			}
		}
		return 0;
	}

	/**
	 * 得到指定日期所在星期的星期一的日期（首日）
	 * 
	 * @param sDate
	 *            String 日期[yyyy-MM-dd]
	 * @return String 指定日期所在星期的星期一的日期（首日）[yyyy-MM-dd]
	 */
	public static final String getWeekFirstDate(String sDate) {
		String sRetDate = null;

		if (sDate == null || sDate.length() == 0)
			sDate = getCurDate();

		int iMonth, iYear, iDay;
		int iIdx1 = sDate.indexOf('-');
		int iIdx2 = sDate.lastIndexOf('-');
		int iIdx3 = sDate.length();
		try {
			iYear = Integer.parseInt(sDate.substring(0, iIdx1));
			iMonth = Integer.parseInt(sDate.substring(iIdx1 + 1, iIdx2));
			iDay = Integer.parseInt(sDate.substring(iIdx2 + 1, iIdx3));
			Calendar calendar = Calendar.getInstance();
			calendar.set(iYear, iMonth - 1, iDay);

			if (calendar != null) {
				int iWeekDay = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;

				if (iWeekDay != 0) // Not Sunday
					iDay = iDay - iWeekDay + 1;
				else
					// Sunday
					iDay = iDay - 6;

				if (iDay <= 0) {
					if (--iMonth < 1) {
						iMonth = 12;
						iYear--;
					}
					iDay = getMonthDays(iYear, iMonth) + iDay;
				}
			}
			sRetDate = iYear + "-" + iMonth + "-" + iDay;
		} catch (Exception e) {
			sRetDate = null;
		}

		return sRetDate;
	}

	/**
	 * 得到指定日期所在星期的星期日的日期（末日）
	 * 
	 * @param sDate
	 *            String 日期[yyyy-MM-dd]
	 * @return String 指定日期所在星期的星期日的日期（末日）[yyyy-MM-dd]
	 */
	public static final String getWeekLastDate(String sDate) {
		String sEndDate = null;

		if (sDate == null || sDate.length() == 0)
			sDate = getCurDate();

		int iMonth, iYear, iDay;
		int iIdx1 = sDate.indexOf('-');
		int iIdx2 = sDate.lastIndexOf('-');
		int iIdx3 = sDate.length();
		try {
			iYear = Integer.parseInt(sDate.substring(0, iIdx1));
			iMonth = Integer.parseInt(sDate.substring(iIdx1 + 1, iIdx2));
			iDay = Integer.parseInt(sDate.substring(iIdx2 + 1, iIdx3));
			Calendar calendar = Calendar.getInstance();
			calendar.set(iYear, iMonth - 1, iDay);

			if (calendar != null) {
				int iWeekDay = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
				if (iWeekDay != 0)
					iDay = iDay + (7 - iWeekDay);
				// else
				// iDay = iDay ;

				if (iDay > getMonthDays(iYear, iMonth)) {
					iDay = iDay - getMonthDays(iYear, iMonth);
					if (++iMonth > 12) {
						iMonth = 1;
						iYear++;
					}
				}
			}
			sEndDate = iYear + "-" + iMonth + "-" + iDay;
		} catch (Exception e) {
			sEndDate = null;
		}

		return sEndDate;
	}

	/**
	 * 根据传入的日期，得到该周第一时间点(星期一00:00:00.000)
	 * 
	 * @param date
	 *            传入的日期
	 * @return 该周第一时间点
	 */
	public static Date firstDateOfWeek(Date date) {
		Calendar calendar = firstTimeWeek(date);
		return calendar.getTime();
	}

	/**
	 * 根据传入的日期，得到该周最后时间点(星期日23:59:59.999)
	 * 
	 * @param date
	 *            传入的日期
	 * @return 该周最后时间点
	 */
	public static Date lastDateOfWeek(Date date) {
		Calendar calendar = lastTimeWeek(date);
		return calendar.getTime();
	}

	/**
	 * 根据传入的日期，得到该周最后时间点(星期日23:59:59.999)
	 * 
	 * @param date
	 *            日期
	 * @return 该周最后时间点
	 */
	public static Calendar lastTimeWeek(Date date) {
		Calendar calendar = firstTimeWeek(date);
		calendar.add(Calendar.DAY_OF_YEAR, 7);
		calendar.add(Calendar.MILLISECOND, -1);
		return calendar;
	}

	/**
	 * 根据传入的日期，得到该周第一时间点(星期一00:00:00.000)
	 * 
	 * @param date
	 *            传入的日期
	 * @return 该周第一时间点
	 */
	public static Calendar firstTimeWeek(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.DAY_OF_WEEK, 2);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar;
	}

	/**
	 * 得到指定日期在前一个星期中的对应日期
	 * 
	 * @param sDate
	 *            String 日期[yyyy-MM-dd]
	 * @return String 指定日期在前一个星期中的对应日期[yyyy-MM-dd]
	 */
	public static final String getPrevWeekDate(String sDate) {
		String sRetDate = null;

		if (sDate == null || sDate.length() == 0)
			sDate = getCurDate();

		int iMonth, iYear, iDay;
		int iIdx1 = sDate.indexOf('-');
		int iIdx2 = sDate.lastIndexOf('-');
		int iIdx3 = sDate.length();
		try {
			iYear = Integer.parseInt(sDate.substring(0, iIdx1));
			iMonth = Integer.parseInt(sDate.substring(iIdx1 + 1, iIdx2));
			iDay = Integer.parseInt(sDate.substring(iIdx2 + 1, iIdx3)) - 7;

			if (iDay <= 0) {
				if (--iMonth < 1) {
					iMonth = 12;
					iYear--;
				}
				iDay = getMonthDays(iYear, iMonth) + iDay;
			}
			if (iYear < 1900)
				sRetDate = null;
			else
				sRetDate = iYear + "-" + iMonth + "-" + iDay;
		} catch (Exception e) {
			sRetDate = null;
		}

		return sRetDate;
	}

	/**
	 * 得到指定日期在后一个星期中的对应日期
	 * 
	 * @param sDate
	 *            String 日期[yyyy-MM-dd]
	 * @return String 指定日期在后一个星期中的对应日期[yyyy-MM-dd]
	 */
	public static String getNextWeekDate(String sDate) {
		String sRetDate = null;

		if (sDate == null || sDate.length() == 0)
			sDate = getCurDate();

		int iMonth, iYear, iDay;
		int iIdx1 = sDate.indexOf('-');
		int iIdx2 = sDate.lastIndexOf('-');
		int iIdx3 = sDate.length();
		try {
			iYear = Integer.parseInt(sDate.substring(0, iIdx1));
			iMonth = Integer.parseInt(sDate.substring(iIdx1 + 1, iIdx2));
			iDay = Integer.parseInt(sDate.substring(iIdx2 + 1, iIdx3)) + 7;

			int nDays;
			if (iDay > (nDays = getMonthDays(iYear, iMonth))) {
				iDay = iDay - nDays;
				if (++iMonth > 12) {
					iMonth = 1;
					iYear++;
				}
			}

			if (iYear < MAX_YEAR)
				sRetDate = iYear + "-" + iMonth + "-" + iDay;
			else
				return null;
		} catch (Exception e) {
			sRetDate = null;
		}

		return sRetDate;
	}

	/**
	 * 得到指定日期所在月的首日的日期
	 * 
	 * @param sDate
	 *            String 日期[yyyy-MM-dd]
	 *            int 每月的起始日期
	 * @return String 指定日期所在星期的星期一的日期（首日）[yyyy-MM-dd]
	 */
	public static String getMonthFirstDate(String sDate, int iBeginDay) {
		String sRetDate;
		try {
			if (getDay(sDate) < iBeginDay)
				sDate = getPrevMonthDate(sDate);

			sRetDate = getYear(sDate) + "-" + getMonth(sDate) + "-" + iBeginDay;
		} catch (Exception e) {
			sRetDate = null;
		}
		return sRetDate;
	}

	/**
	 * 得到指定日期所在月的末日的日期
	 * 
	 * @param sDate
	 *            String 日期[yyyy-MM-dd]
	 *            int 每月的起始日期
	 * @return String 指定日期所在星期的星期一的日期（首日）[yyyy-MM-dd]
	 */
	public static String getMonthLastDate(String sDate, int iBeginDay) {
		String sRetDate;
		try {
			int iYear = getYear(sDate);
			int iMonth = getMonth(sDate);

			if (iBeginDay == 1)
				sRetDate = iYear + "-" + iMonth + "-" + getMonthDays(iYear, iMonth);
			else {
				sRetDate = getNextMonthDate(getMonthFirstDate(sDate, iBeginDay));
				sRetDate = getPrevDate(sRetDate);
			}
		} catch (Exception e) {
			sRetDate = null;
		}
		return sRetDate;
	}

	/**
	 * 得到指定日期在前一个月中的对应日期
	 * 
	 * @param sDate
	 *            String 日期[yyyy-MM-dd]
	 * @return String 指定日期在前一个月中的对应日期[yyyy-MM-dd]
	 */
	public static String getPrevMonthDate(String sDate) {
		String sRetDate = null;

		if (sDate == null || sDate.length() == 0)
			sDate = getCurDate();

		int iMonth, iYear, iDay;
		try {
			iYear = getYear(sDate);
			iMonth = getMonth(sDate);
			iDay = getDay(sDate);

			iMonth--;

			if (iMonth <= 0) {
				iYear--;
				iMonth = 12;
			}

			if (iYear < 1900)
				sRetDate = null;
			else
				sRetDate = iYear + "-" + iMonth + "-" + iDay;
		} catch (Exception e) {
			sRetDate = null;
		}

		return sRetDate;
	}

	/**
	 * 得到指定日期在后一个月中的对应日期
	 * 
	 * @param sDate
	 *            String 日期[yyyy-MM-dd]
	 * @return String 指定日期在后一个月中的对应日期[yyyy-MM-dd]
	 */
	public static String getNextMonthDate(String sDate) {
		String sRetDate = null;

		if (sDate == null || sDate.length() == 0)
			sDate = getCurDate();

		int iMonth, iYear, iDay;
		try {
			iYear = getYear(sDate);
			iMonth = getMonth(sDate);
			iDay = getDay(sDate);

			iMonth++;
			if (iMonth > 12) {
				iYear++;
				iMonth = 1;
			}

			if (iYear >= MAX_YEAR)
				return null;

			sRetDate = iYear + "-" + iMonth + "-" + iDay;
		} catch (Exception e) {
			sRetDate = null;
		}

		return sRetDate;
	}

	/**
	 * 得到指定日期的前一天日期
	 * 
	 * @param sDate
	 *            String 指定日期[yyyy-MM-dd]
	 * @return String 指定日期的前一天日期[yyyy-MM-dd]
	 */
	public static String getPrevDate(String sDate) {
		String sRetDate;

		if (sDate == null || sDate.length() == 0)
			sDate = getCurDate();

		int iYear, iMonth, iDay;
		int iIdx1 = sDate.indexOf('-');
		int iIdx2 = sDate.lastIndexOf('-');
		try {
			iYear = Integer.parseInt(sDate.substring(0, iIdx1));
			iMonth = Integer.parseInt(sDate.substring(iIdx1 + 1, iIdx2));
			iDay = Integer.parseInt(sDate.substring(iIdx2 + 1));
		} catch (Exception e) {
			return null;
		}

		if ((--iDay) < 1) {
			if ((--iMonth) < 1) {
				iMonth = 12;
				iYear--;
			}
			iDay = getMonthDays(iYear, iMonth);
		}

		if (iYear < 1900)
			sRetDate = null;
		else
			sRetDate = iYear + "-" + iMonth + "-" + iDay;

		return sRetDate;
	}

	/**
	 * 得到指定日期的后一天日期
	 * 
	 * @param sDate
	 *            String 指定日期[yyyy-MM-dd]
	 * @return String 指定日期的后一天日期[yyyy-MM-dd]
	 */
	public static String getNextDate(String sDate, boolean bExist) {
		String sRetDate;

		if (sDate == null || sDate.length() == 0) {
			if (!bExist)
				sDate = getCurDate();
			else
				return null;
		}

		int iYear, iMonth, iDay;
		int iIdx1 = sDate.indexOf('-');
		int iIdx2 = sDate.lastIndexOf('-');
		try {
			iYear = Integer.parseInt(sDate.substring(0, iIdx1));
			iMonth = Integer.parseInt(sDate.substring(iIdx1 + 1, iIdx2));
			iDay = Integer.parseInt(sDate.substring(iIdx2 + 1));
		} catch (Exception e) {
			return null;
		}

		if (bExist) {
			if (!haveNextDay(iYear, iMonth, iDay))
				return null;
		}

		if (++iDay > getMonthDays(iYear, iMonth)) {
			iDay = 1;
			if (++iMonth > 12) {
				iMonth = 1;
				iYear++;
			}
		}

		if (iYear >= MAX_YEAR)
			return null;

		sRetDate = iYear + "-" + iMonth + "-" + iDay;
		return sRetDate;
	}

	/**
	 * 查看指定日期的下一天是否已经存在
	 * 
	 * @param iYear
	 *            int 年份
	 * @param iMonth
	 *            int 月份
	 * @param iDay
	 *            int 日期
	 * @return boolean true：存在；false：不存在；
	 */
	public static boolean haveNextDay(int iYear, int iMonth, int iDay) {
		Calendar today = Calendar.getInstance();

		Calendar tdCompared = Calendar.getInstance();
		tdCompared.set(iYear, iMonth, iDay);

		if (tdCompared.after(today))
			return false;
		else
			return true;
	}

	/**
	 * 得到当前时间
	 * 
	 * @return String 当前时间[HH:mm:ss]
	 */
	public static String getCurTime() {
		Calendar today = Calendar.getInstance();

		SimpleDateFormat formatDateTime = new SimpleDateFormat("HH:mm:ss");

		String sTime = formatDateTime.format(today.getTime());
		sTime = sTime.substring(0, sTime.length() - 2) + "00";

		return sTime;
	}

	/**
	 * 得到当前日期时间
	 * 
	 * @return String 当前日期时间[yyyy-MM-dd HH:mm:ss]
	 */
	public static String getCurDateTime() {
		Calendar today = Calendar.getInstance();

		SimpleDateFormat formatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		String sTime = formatDateTime.format(today.getTime());
		sTime = sTime.substring(0, sTime.length() - 2) + "00";
		return sTime;
	}


	//解析时间戳
	public static String parseTimestamp(long m){
		SimpleDateFormat formatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return  formatDateTime.format(m);
	}



	/**
	 * 得到当前日期时间，包括秒数
	 * 
	 * @return String 当前日期时间，包括秒数[yyyy-MM-dd HH:mm:ss]
	 */
	public static String getCurDateTimeWithSecond() {
		Calendar today = Calendar.getInstance();

		SimpleDateFormat formatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		String sTime = formatDateTime.format(today.getTime());

		return sTime;
	}

	/**
	 * 得到指定年份的下一年
	 * 
	 * @param iYear
	 *            int 指定年份
	 * @param bExist
	 *            boolean 是否必须是已存在的年份
	 * @return int 指定年份的下一年
	 */
	public static int getNextYear(int iYear, boolean bExist) {
		int iCurYear = getYear();

		if (bExist) {
			if (iYear >= iCurYear)
				return -1;
		}

		iYear++;

		if (iYear >= MAX_YEAR)
			return -1;

		return iYear;
	}

	/**
	 * 得到指定年份的上一年，小于1900年，则返回[-1]
	 * 
	 * @param iYear
	 *            int 指定年份
	 * @return int 指定年份的上一年
	 */
	public static int getPrevYear(int iYear) {
		if (iYear <= 1900)
			return -1;

		return --iYear;
	}

	/**
	 * 转换日期显示格式，由[YYYY-MM-DD]转换成[YYYY年MM月DD日]
	 * 
	 * @param sDate
	 *            String 待转换日期[YYYY-MM-DD]
	 * @return String 已转换日期[YYYY年MM月DD日]
	 */
	public static String reformDate(String sDate) {
		if (sDate == null || sDate.length() == 0)
			return null;

		return getYear(sDate) + "年" + getMonth(sDate) + "月" + getDay(sDate) + "日";
	}

	/**
	 * 得到指定日期加上指定天数之后的日期
	 * 
	 * @param sDate
	 *            String 指定日期[YYYY-MM-DD]
	 * @param nDay
	 *            int 指定天数
	 * @return String 指定日期加上指定天数之后的日期
	 */
	public static String addDays(String sDate, int nDay) {
		String sRetDate = sDate;

		for (int i = 0; i < nDay; i++)
			sRetDate = getNextDate(sRetDate, false);

		return sRetDate;
	}

	/**
	 * 计算生日
	 * 
	 * @param sDate
	 *            String 指定日期[YYYY-MM-DD]或时间
	 * @return int 年龄
	 */
	public static int getAge(String sDate) {
		// 提取字符串中的年、月、日
		int i1stMin = sDate.indexOf("-", 0);
		int i2stMin = sDate.indexOf("-", i1stMin + 1);
		int i3stMin = i2stMin + 3;
		if (i2stMin + 3 < sDate.length()) {
			i3stMin = i2stMin + 2;
		} else {
			if (sDate.charAt(i2stMin + 2) == ' ') {
				i3stMin = i2stMin + 2;
			}
		}
		int iYear = Integer.parseInt(sDate.substring(0, i1stMin));
		int iMonth = Integer.parseInt(sDate.substring(i1stMin + 1, i2stMin));
		int iDay = Integer.parseInt(sDate.substring(i2stMin + 1, i3stMin));

		// 取得当前年份，和当天是年内第N天
		Calendar calendar = Calendar.getInstance();
		int iSysYear = calendar.get(Calendar.YEAR);
		int iSysMonth = calendar.get(Calendar.MONTH) + 1;
		int iSysDay = calendar.get(Calendar.DATE);

		// 计算年龄
		int iAge = iSysYear - iYear;
		if (iSysDay - iDay < 0) {
			// 月份数借位
			iSysMonth--;
		}
		if (iSysMonth - iMonth < 0) {
			iAge--;
		}
		return iAge;
	}

	/**
	 * 计算生日
	 * 
	 * @param sDate
	 *            String 指定日期[YYYY-MM-DD]或时间
	 * @param pattern
	 *            String 指定日期格式
	 * @return int 年龄
	 */
	public static int getAge(String sDate, String pattern) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);

		Date d = sdf.parse(sDate);

		return getAge(d);
	}

	/**
	 * 计算生日
	 * 
	 * @param date
	 *            date 指定日期或时间
	 * @return int 年龄
	 */
	public static int getAge(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		int iYear = cal.get(Calendar.YEAR);
		int iMonth = cal.get(Calendar.MONTH) + 1;
		int iDay = cal.get(Calendar.DATE);

		// 取得当前年份，和当天是年内第N天
		Calendar calendar = Calendar.getInstance();
		int iSysYear = calendar.get(Calendar.YEAR);
		int iSysMonth = calendar.get(Calendar.MONTH) + 1;
		int iSysDay = calendar.get(Calendar.DATE);

		// 计算年龄
		int iAge = iSysYear - iYear;
		if (iSysDay - iDay < 0) {
			// 月份数借位
			iSysMonth--;
		}
		if (iSysMonth - iMonth < 0) {
			iAge--;
		}
		return iAge;
	}

	/**
	 * 根据出生日期计算某人的年龄，如果没有过生日，则减1
	 * 
	 * @param birthday
	 *            出生日期
	 * @param effectiveDate
	 *            生效日期（计算时的时间）
	 * @return 年龄
	 */
	public static int computeAge(Date birthday, Date effectiveDate) {
		return computeAge(birthday.getTime(), effectiveDate.getTime());
	}

	/**
	 * 根据出生日期计算某人的年龄，如果没有过生日，则减1
	 * 
	 * @param birthday
	 *            出生日期
	 * @return 年龄
	 */
	public static int computeAge(Date birthday) {
		return computeAge(birthday.getTime(), System.currentTimeMillis());
	}

	/**
	 * 根据出生日期计算某人的年龄，如果没有过生日，则减1
	 * 
	 * @param birthday
	 *            出生日期
	 * @param effective
	 *            生效日期（计算日期）
	 * @return 年龄
	 */
	public static int computeAge(long birthday, long effective) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(birthday);
		Calendar cal2 = Calendar.getInstance();
		cal2.setTimeInMillis(effective);

		int ret = cal2.get(Calendar.YEAR) - cal.get(Calendar.YEAR);
		if (cal.get(Calendar.DAY_OF_YEAR) > cal2.get(Calendar.DAY_OF_YEAR)) {
			ret = ret - 1;
		}
		return ret;
	}


	/**
	 * 启动时输出当前时间
	 */
	public static void printNow(){
		logger.error("=========================================================");
		Date date = Calendar.getInstance().getTime();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		logger.error("当前系统时间："+dateFormat.format(date));

		//ZonedDateTime now1 = ZonedDateTime.now( ZoneOffset.UTC );
		//System.out.println(now1);

		org.joda.time.DateTime now = new org.joda.time.DateTime();//Default time zone.
		org.joda.time.DateTime zulu = now.toDateTime( org.joda.time.DateTimeZone.UTC );

		logger.error("当前系统ISO-8601时间:" + now );
		//System.out.println("Same moment in UTC (Zulu):" + zulu );
		logger.error("=========================================================");

	}


	/**
	 * @return 当前时间与 n秒 之前拼接的字符串,2147483647
	 */
	public static String printFromNowMinuteAgo(int seconds){
		DateTime  now= new DateTime();
		DateTime beforFive = now.minusSeconds(seconds);
		return dateTimeFormatter.print(beforFive).concat(" - ").concat(dateTimeFormatter.print(now));
	}

	/**
	 * 减去多少天
	 */
	public static String minusDays(String dataStr,String formater,int days){
		DateTimeFormatter format = DateTimeFormat.forPattern(formater);
		DateTime dateTime = DateTime.parse(dataStr, format);
		dateTime = dateTime.minusDays(days);
	 	return  dateTime.toString(formater);
	}

	/**
	 * 加上多少天
	 */
	public static String plusDays(String dataStr,String formater,int days){
		DateTimeFormatter format = DateTimeFormat.forPattern(formater);
		DateTime dateTime = DateTime.parse(dataStr, format);
		dateTime = dateTime.plusDays(days);
		return  dateTime.toString(formater);
	}


	/**
	 * 格式化
	 */
	public static String formaterDate(String dataStr,String inFormater,String outFormater){
		DateTimeFormatter format = DateTimeFormat.forPattern(inFormater);
		DateTime dateTime = DateTime.parse(dataStr, format);
		return  dateTime.toString(outFormater);
	}


	/**
	 * 输出 DATE对象
	 */
	public static Date getDate(String dataStr,String formater){
		DateTimeFormatter format = DateTimeFormat.forPattern(formater);
		DateTime dateTime = DateTime.parse(dataStr, format);
		return dateTime.toDate();
	}



	/**
	 * 格式化
	 */
	public static String formatDate(String dataStr,String formater){
		DateTimeFormatter format = DateTimeFormat.forPattern(formater);
		DateTime dateTime = DateTime.parse(dataStr, format);
		return dateTime.toString(formater);
	}



	/**
	 * 格式化
	 */
	public static String formatDateInParttern(String dataStr,String inFormater,String outFormater){
		DateTimeFormatter format = DateTimeFormat.forPattern(inFormater);
		DateTime dateTime = DateTime.parse(dataStr, format);
		return dateTime.toString(outFormater);
	}



	//日期之间
	public static int  betweenDateTime(String xdate,String ydate,String formater){
		//logger.info("[日期之间]:{}-{},format:{}",xdate,ydate,formater);
		if(StringUtils.isBlank(formater)) formater = dateTimeString;
		if(ValidateUtil.valdateRiQi(xdate,formater) && ValidateUtil.valdateRiQi(ydate,formater)){
			DateTimeFormatter format = DateTimeFormat.forPattern(formater);
			DateTime xDateTime = DateTime.parse(xdate, format);
			DateTime yDateTime = DateTime.parse(ydate, format);
			return Seconds.secondsBetween(xDateTime, yDateTime).getSeconds();
		}
		return -1;
	}

	//等于7.00->0;大于7点-->1;小于7点——>-1
	public static int isSevenClock(String xdateTime,String formater){
		DateTimeFormatter format = DateTimeFormat.forPattern(formater);
		DateTime xDateTime = DateTime.parse(xdateTime, format);
		return  Integer.compare(xDateTime.secondOfDay().get(),25200);
	}


	//等于6.59->0;大于6.59-->1;小于6.59——>-1
	public static int isSixFiftyNineClock(String xdateTime,String formater){
		DateTimeFormatter format = DateTimeFormat.forPattern(formater);
		DateTime xDateTime = DateTime.parse(xdateTime, format);
		return  Integer.compare(xDateTime.secondOfDay().get(),25200-1);
	}


	//1年前的今日0:0：0 -> 明天：0:0:0
	public static String getDateTimeOneYearAgo(){
		DateTime todayDate = new DateTime( DateUtil.formatDateTimeStrByParam(DateUtil.dateString));
		String tomorrowDateStr =   DateUtil.plusDays(todayDate.toString(DateUtil.dateTimeString), DateUtil.dateTimeString, 1);
		String beforeYearDateStr = DateUtil.minusDays(tomorrowDateStr, DateUtil.dateTimeString, 366);
		return  beforeYearDateStr.replaceAll(zeroClock,sevenClock).concat(" - ").concat(tomorrowDateStr.replaceAll(zeroClock,sixFiftyNineClock));
	}


	//比较日期时间，相等=0，大于1，小于-1,错误-2
	public static int  compareDateTime(String xdate,String ydate, String formater){
		int secondes = betweenDateTime(xdate, ydate, formater);
		if(secondes==0) return 0;
		if(secondes>0) return 1;
		if(secondes<0) return -1;
		return -2;
	}



}
