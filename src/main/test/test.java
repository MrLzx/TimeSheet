import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class test {
    public static void main(String[] args) {
        String date = "2017/10/1";
        date = date.replace("/","-");
        System.out.println(date);
    }
}
