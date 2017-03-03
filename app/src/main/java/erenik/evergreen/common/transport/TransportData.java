package erenik.evergreen.common.transport;

import java.util.ArrayList;

import erenik.evergreen.common.player.Transport;
import erenik.evergreen.util.Tuple;

/**
 * Created by Emil on 2017-03-02.
 */

public class TransportData {
    public ArrayList<Tuple<Transport, Integer>> data = new ArrayList<>();

    public int secondsPerEntry = 5; // Default 5 seconds per data-entry (integer-count).

    /// Returns ratio of time spent in a certain mode of transport for this data-set.
    public float Ratio(Transport t){
        int total = 0;
        for (int i = 0; i < data.size(); ++i){
            Tuple<Transport,Integer> d = data.get(i);
            total += d.y;
            if (d.x.ordinal() == t.ordinal()){
                return d.y / (float) total;
            }
        }
        return 0;
    }
}
