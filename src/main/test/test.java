import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class test {
    public static void main(String[] args) throws ParseException {
        Calendar instance = Calendar.getInstance();
        instance.setTime(new SimpleDateFormat("yyyy-MM-dd").parse("2017-01-01"));
        instance.set(Calendar.MONTH, instance.get(Calendar.MONTH) - 1);

        String sYear = String.valueOf(instance.get(Calendar.YEAR));
        String sMonth = String.valueOf(instance.get(Calendar.MONTH) + 1);

        System.out.println(sYear);
        System.out.println(sMonth);
    }
}
