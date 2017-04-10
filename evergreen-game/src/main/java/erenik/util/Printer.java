package erenik.util;

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
        System.out.println(msg);
        if (printToFile)
            FileUtil.AppendWithTimeStampToFile("logs", "Printer", msg);
    }
}
