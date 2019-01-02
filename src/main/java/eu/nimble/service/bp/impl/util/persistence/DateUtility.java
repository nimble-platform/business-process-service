package eu.nimble.service.bp.impl.util.persistence;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Created by suat on 29-Jun-18.
 */
public class DateUtility {
    public static String transformInputDateToDbDate(String inputDate) {
        DateTimeFormatter inputFormat = DateTimeFormat.forPattern("dd-MM-yyyy");
        DateTimeFormatter dbFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

        DateTime date = inputFormat.parseDateTime(inputDate);
        return dbFormat.print(date);
    }

    public static String transformInputDateToMaxDbDate(String inputDate) {
        DateTimeFormatter inputFormat = DateTimeFormat.forPattern("dd-MM-yyyy");
        DateTimeFormatter dbFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

        DateTime date = inputFormat.parseDateTime(inputDate);
        date = date.plusDays(1).minusMillis(1);
        return dbFormat.print(date);
    }
}
