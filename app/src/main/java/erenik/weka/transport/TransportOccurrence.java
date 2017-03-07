package erenik.weka.transport;

/**
 * Created by Emil on 2017-03-06.
 */

public class TransportOccurrence {
    TransportOccurrence(){}
    TransportOccurrence(int trans, long timeStamp){ transport = trans; timeStampMs = timeStamp;}
    int transport;
    long timeStampMs; // Time stamp when the measuring window started.
}
