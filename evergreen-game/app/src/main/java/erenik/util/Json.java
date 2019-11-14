package erenik.util;

import erenik.util.EList;
import erenik.util.EList;

/**
 * Created by Emil on 2016-12-18.
 */
public class Json {
//    @Test
    public void testIt()
    {
        String json2 = "{\"type\":\"Sword\",\"Damage\":\"5\"}";
        String json3 = "{\"type\":\"Axe\",\"Damage\":\"2\"}";
        String json = "{\"name\":\"Verycool\",\"sBad\":\"2423\", \"Inventory\":["+json2+", "+json3+"]}";
        Json j = new Json();
        j.str = json;
        j.Parse();
        for (int i = 0; i < j.Tuples().size(); ++i)
        {
            Printer.out("Tuples:"+j.Tuples().get(i));
        }
        j.Keys();
    }
    public EList<Tuple<String, String>> Tuples()
    {
        return tuples;
    }
    public void Parse(String str)
    {
        this.str = str;
        Parse();
    }
    void Parse()
    {
        tuples.clear();
        keys.clear();
        values.clear();
        if (keys.size() > 0) // Already parsed earlier.
            return;
        boolean inQuotes = false,
                argTime = false;
        int inObject = 0, inArray = 0;
        int startKey = 0;
        int startValue = 0;
        for (int i = 0; i < str.length(); ++i)
        {
            char c = str.charAt(i);
            if (c == '[') {
                ++inArray;
                if (inArray == 1)
                    startValue = i;
            }
            if (c == ']') {
                --inArray;
                if (inArray == 0) {
                    String value = str.substring(startValue, i+1);
          //          Printer.out("value: " + value);
                    values.add(value);
                }
            }
            if (c == '{')
                ++inObject;
            if (c == '}')
                --inObject;
            if (inArray != 0 || inObject > 1)
                continue;
            if (c == '\"') {
                inQuotes = !inQuotes;
                if (!inQuotes && !argTime) // Just finished a quote? And before argument? Is key.
                {
                    String key = str.substring(startKey+1, i);
        //            Printer.out("key: "+key);
                    keys.add(key);
                }
                else if (!inQuotes && argTime)
                {
                    String value = str.substring(startKey+1, i);
          //          Printer.out("value: "+value);
                    values.add(value);
                }
                startKey = i;
            }
            if (c == ':')
                argTime = true;
            if (c == ',') {
                argTime = false;
            }
        }
        // Build tuples.
        for (int i = 0; i < keys.size() && i < values.size(); ++i)
        {
            Tuple<String, String> t = new Tuple<String, String>(keys.get(i), values.get(i));
            tuples.add(t);
        }
    }
    EList<String> Keys()
    {
        return keys;
    }
    EList<String> Values()
    {
        return values;
    }
    EList<Tuple<String,String>> tuples = new EList<>();
    EList<String> keys = new EList<>();
    EList<String> values = new EList<>();
    private String str;
}
