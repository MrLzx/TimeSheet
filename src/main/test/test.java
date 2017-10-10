import java.text.ParseException;
import java.text.SimpleDateFormat;

public class test {
    public static void main(String[] args) {
        String date = "09:16";
        String date1 = "09:30";
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");



        try {
            long l = format.parse(date1).getTime() - format.parse(date).getTime();
            System.out.println(l/1000/60);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
