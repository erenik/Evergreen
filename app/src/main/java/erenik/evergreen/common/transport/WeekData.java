package erenik.evergreen.common.transport;

import java.util.ArrayList;

import erenik.evergreen.common.player.Transport;
import erenik.evergreen.util.Tuple;

/**
 * Created by Emil on 2017-03-02.
 */

public class WeekData extends TransportData {
    public ArrayList<DayData> days = new ArrayList<>();
    public ArrayList<Tuple<Transport, Integer>> data = new ArrayList<>();



}
