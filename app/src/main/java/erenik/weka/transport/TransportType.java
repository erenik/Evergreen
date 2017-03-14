package erenik.weka.transport;

/**
 * Created by Emil on 2017-03-09.
 */

public enum TransportType {
    Unknown,
    Idle,
    Foot,
    Bike,
    Car,
    Train,
    Bus,
    Tram,
    Plane;

    static TransportType GetForString(String s){
        for (int i = 0; i < values().length; ++i){
            if (s.equals(values()[i].name()))
                return values()[i];
        }
        return TransportType.Unknown;
    }
}