package erenik.seriousgames.evergreen.player;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Emil on 2016-11-06.
 */
public enum Transport
{
    Walking,
    Biking,
    Bus,
    Tram,
    Train,
    Car;

    public static List<String> GetStrings()
    {
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < values().length; ++i)
        {
            list.add(values()[i].toString());
        }
        return list;
    }
}
