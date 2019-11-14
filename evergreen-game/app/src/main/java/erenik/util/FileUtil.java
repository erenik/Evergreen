package erenik.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Emil on 2017-04-03.
 */

public class FileUtil {
    /// e.g. "yyyy-MM-dd_HH:mm:ss"
    public static void AppendWithTimeStampToFile(String timeFormat, String folderStrng, String fileName, String s){
        String dateStrNow = new SimpleDateFormat(timeFormat).format(new Date());
        AppendToFile(folderStrng, fileName, dateStrNow+" "+s);
    }
    public static void AppendWithTimeStampToFile(String folderStrng, String fileName, String s){
        String dateStrNow = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
        AppendToFile(folderStrng, fileName, dateStrNow+" "+s);
    }
    public static void AppendToFile(String folderString, String fileName, String s) {
        new File(folderString).mkdirs();
        try {
            FileWriter fw = new FileWriter(folderString+"/"+fileName+".txt", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);
            out.println(s);
            out.flush();
            fw.close(); // Close it.
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }
    }

}
