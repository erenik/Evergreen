package erenik.evergreen;
import erenik.util.EList;

/**
 * Created by Emil on 2016-11-06.
 */
public class StringUtils {
    public static String join(EList<String> list, String glue)
    {
        String s = "";
        for (int i = 0; i < list.size(); ++i)
        {
            s += list.get(i);
            if (i < list.size() - 1)
                s += glue;
        }
        System.out.println("join: "+s);
        return s;
    };
}
