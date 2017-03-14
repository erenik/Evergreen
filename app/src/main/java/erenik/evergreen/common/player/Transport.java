package erenik.evergreen.common.player;

import java.io.IOException;
import java.io.InvalidClassException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Emil on 2016-11-11.
 */
public enum Transport {
    Unknown(0, 100, 100, 100),
    Idle(0, 150, 150, 150),
    Walking(1, 0, 255, 0),
    Bike(1, 0, 150, 150),
    Bus(1, 50, 200, 200),
    Tram(0, 75, 255, 255), // Same as bus? Similar at least.
    Train(1, 100, 150, 255),
    Car(2, 175, 100, 50), // Higher chance, typical commuter?
    Plane(0, 255, 50, 50); // Only triggerable by user analysis?

    Transport(float defProb, int r, int g, int b) {
        defaultProbability = defProb;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /// For representations of the transports, colors for the graphs, charts, etc.
    public int r, g, b;

    public float Probability() { return Get(TransportStat.RandomProbability);};
    public float defaultProbability = 0;
    float[] stats = new float[TransportStat.values().length];

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(stats);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException, InvalidClassException {
        stats = (float[]) in.readObject();
    }

    /// CAll after creation of an instance of a transport.
    public void SetDefaults() {
        for (int i = 0; i < TransportStat.values().length; ++i)
            stats[i] = TransportStat.values()[i].DefaultValue();
        Set(TransportStat.RandomProbability, defaultProbability);
        switch (this)
        {
            case Walking:
                Set(TransportStat.ForagingBonus, 2);
                Set(TransportStat.AmountEnemiesEncounteredRatio, 0.75f);
                Set(TransportStat.EmissionsPerDay, -2);
                break;
            case Bike:
                Set(TransportStat.ForagingBonus, 1);
                Set(TransportStat.MaterialGatheringBonus, 1);
                Set(TransportStat.SpeedBonus, 1);
                Set(TransportStat.FleeBonus, 1);
                Set(TransportStat.EmissionsPerDay, -1);
                break;
            case Bus:
            case Tram:
                Set(TransportStat.ForagingBonus, -2);
                Set(TransportStat.MaterialGatheringBonus, 1);
                Set(TransportStat.AmountEnemiesEncounteredRatio, 1.5f);
                Set(TransportStat.SocialSupport, 1);
                Set(TransportStat.SpeedBonus, 2);
                Set(TransportStat.FleeBonus, 2);
                Set(TransportStat.EmissionsPerDay, 3);
                break;
            case Train:
                Set(TransportStat.ForagingBonus, -3);
                Set(TransportStat.AmountEnemiesEncounteredRatio, 1.25f);
                Set(TransportStat.SocialSupport, 2);
                Set(TransportStat.SpeedBonus, 2);
                Set(TransportStat.FleeBonus, 1);
                Set(TransportStat.EmissionsPerDay, 1);
                break;
            case Car:
                Set(TransportStat.MaterialGatheringBonus, +2);
                Set(TransportStat.AmountEnemiesEncounteredRatio, 1.75f);
                // No social support unless taking active action to ensure you have co-passengers.
                Set(TransportStat.SpeedBonus, 4);
                Set(TransportStat.FleeBonus, 3);
                Set(TransportStat.EmissionsPerDay, 7);
                break;
            case Plane: // Nothing good, pretty much.
                Set(TransportStat.MaterialGatheringBonus, -4);
                Set(TransportStat.ForagingBonus, -4);
                Set(TransportStat.AmountEnemiesEncounteredRatio, 2.f);
                Set(TransportStat.EmissionsPerDay, 15);
                break;
        }
    }

    public void Set(TransportStat s, float value)
    {
        stats[s.ordinal()] = value;
    }
    public float Get(TransportStat s)
    {
        return stats[s.ordinal()];
    }
    public void Adjust(TransportStat s, float adjustment)
    {
        stats[s.ordinal()] += adjustment;
    }
    public static List<String> GetStrings() {
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < values().length; ++i)
        {
            list.add(values()[i].toString());
        }
        return list;
    }

    public static Transport RandomOf(List<Transport> transports) {
        float total = 0;
        for (int i = 0; i < transports.size(); ++i)
            total += transports.get(i).Get(TransportStat.RandomProbability);
        Random transportRandom = new Random();
        float r = transportRandom.nextFloat() * total;
    //    System.out.println("Randomed " + r + " out of " + total);
        for (int i = 0; i < transports.size(); ++i) {
            Transport t = transports.get(i);
            r -= t.Get(TransportStat.RandomProbability);
      //      System.out.println("Total " + total);
            if (r < 0) {
      //          System.out.println("Yo " + t.name());
                return t;
            }
        }
        return transports.get(transports.size() - 1);
    }

    public static Transport GetFromString(String s) {
        if (s.contains("Idle"))
            return Transport.Idle;
        if (s.contains("Foot"))
            return Transport.Walking;
        if (s.contains("Bike"))
            return Transport.Bike;
        return Transport.Unknown;
    }
}
