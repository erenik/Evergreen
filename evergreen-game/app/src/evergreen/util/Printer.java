package evergreen.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Emil on 2017-04-09.
 */

public class Printer {
    public static boolean printToFile = false;
    public static void out(){
        System.out.println();
    }
    public static void out(int iV){
        out(""+iV);
    }
    public static void out(String msg){
        String dateStrNow = new SimpleDateFormat("MM/dd, HH:mm,").format(new Date());
        System.out.println(dateStrNow+" "+msg);
        if (printToFile)
            FileUtil.AppendWithTimeStampToFile("MM/dd, HH:mm," , "logs", "Printer", msg);
    }
}
