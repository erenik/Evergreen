package erenik.weka.transport;

import erenik.util.EList;

/**
 * Created by Emil on 2017-03-09.
 */

public enum TransportType {
    Unknown(100, 100, 100),
    Idle(150, 150, 150),
    Foot(0, 255, 0),
    Bike(0, 150, 150),
    Bus(50, 200, 200),
    Tram(75, 255, 255), // Same as bus? Similar at least.
    Train(100, 150, 255),
    Car(175, 100, 50), // Higher chance, typical commuter?
    Boat(0, 50, 255), // Will probably not be used, but eh...?
    Plane(255, 50, 50); // Only triggerable by user analysis?

    TransportType(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /// For representations of the transports, colors for the graphs, charts, etc.
    public int r, g, b;

    public static TransportType GetFromString(String s) {
        return GetForString(s);
    }
    static TransportType GetForString(String s){
        for (int i = 0; i < values().length; ++i){
            if (s.equals(values()[i].name()))
                return values()[i];
        }
        return TransportType.Unknown;
    }

    // Used...?
    public static EList<String> GetStrings() {
        EList<String> list = new EList<String>();
        for (int i = 0; i < values().length; ++i) {
            list.add(values()[i].toString());
        }
        return list;
    }

}