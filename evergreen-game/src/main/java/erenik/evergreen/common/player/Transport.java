package erenik.evergreen.common.player;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Random;

import erenik.util.EList;
import erenik.util.ESerializable;
import erenik.weka.transport.TransportType;

/**
 * Created by Emil on 2016-11-11.
 */
public class Transport implements Serializable {
    Transport() {}
    Transport(TransportType tt, float defProb) {
        this.tt = tt;
        defaultProbability = defProb;
        SetDefaults();
    }

    public static EList<Transport> DefaultTransports(){
        EList<Transport> alt = new EList<>();
        alt.add(new Transport(TransportType.Idle, 0));
        alt.add(new Transport(TransportType.Foot, 1));
        alt.add(new Transport(TransportType.Bike, 1));
        alt.add(new Transport(TransportType.Bus, 1));
        alt.add(new Transport(TransportType.Tram, 1));
        alt.add(new Transport(TransportType.Train, 1));
        alt.add(new Transport(TransportType.Car, 3));
        alt.add(new Transport(TransportType.Boat, 1));
        alt.add(new Transport(TransportType.Plane, 0.5f));
        alt.add(new Transport(TransportType.Subway, 1));
        return alt;
    }

    public TransportType tt = TransportType.Unknown;
    public String name() { return tt.name(); };
    public float defaultProbability = 0;
    /// Seconds this transport has been used/identified/classified as being used.
    public long secondsUsed = 0;

    float[] stats = new float[TransportStat.values().length];

    public void writeTo(java.io.ObjectOutputStream out) throws IOException {
        writeObject(out);
    }
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(tt);
        out.writeFloat(defaultProbability);
        out.writeLong(secondsUsed);
        out.writeObject(stats);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException, InvalidClassException {
        tt = (TransportType) in.readObject();
        defaultProbability = in.readFloat();
        secondsUsed = in.readLong();
        stats = (float[]) in.readObject();
    }

    /// CAll after creation of an instance of a transport.
    public void SetDefaults() {
        for (int i = 0; i < TransportStat.values().length; ++i)
            stats[i] = TransportStat.values()[i].DefaultValue();
        if (tt == null)
            return;
        Set(TransportStat.Weight, 1); // Default weight of 1 for non-IDLE.
        switch (tt)
        {
            default:
                System.out.println("Not implemented transport: "+name());
                break;
            case Idle:
                Set(TransportStat.Weight, 0);
                break;
            case Boat:
                Set(TransportStat.ForagingBonus, 5); // Fishing!
                Set(TransportStat.MaterialGatheringBonus, -5);
                Set(TransportStat.AmountEnemiesEncounteredRatio, 0.5f);
                Set(TransportStat.EmissionsPerDay, 5);
                break;
            case Foot:
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
                Set(TransportStat.ForagingBonus, -2);
                Set(TransportStat.MaterialGatheringBonus, -2);
                Set(TransportStat.AmountEnemiesEncounteredRatio, 2.0f);
                Set(TransportStat.SocialSupport, 1);
                Set(TransportStat.SpeedBonus, 2);
                Set(TransportStat.FleeBonus, 2);
                Set(TransportStat.EmissionsPerDay, 3);
                break;
            case Tram:
            case Subway:
                Set(TransportStat.ForagingBonus, -3);
                Set(TransportStat.MaterialGatheringBonus, -3);
                Set(TransportStat.AmountEnemiesEncounteredRatio, 1.25f);
                Set(TransportStat.SocialSupport, 1);
                Set(TransportStat.SpeedBonus, 2);
                Set(TransportStat.FleeBonus, 1);
                Set(TransportStat.EmissionsPerDay, 1);
                break;
            case Train:
                Set(TransportStat.ForagingBonus, -3);
                Set(TransportStat.MaterialGatheringBonus, -3);
                Set(TransportStat.AmountEnemiesEncounteredRatio, 1.25f);
                Set(TransportStat.SocialSupport, 2);
                Set(TransportStat.SpeedBonus, 2);
                Set(TransportStat.FleeBonus, 1);
                Set(TransportStat.EmissionsPerDay, 1);
                break;
            case Car:
                Set(TransportStat.ForagingBonus, -1);
                Set(TransportStat.MaterialGatheringBonus, +2);
                Set(TransportStat.AmountEnemiesEncounteredRatio, 2.5f);
                // No social support unless taking active action to ensure you have co-passengers.
                Set(TransportStat.SpeedBonus, 4);
                Set(TransportStat.FleeBonus, 3);
                Set(TransportStat.EmissionsPerDay, 7);
                break;
            case Plane: // Nothing good, pretty much.
                Set(TransportStat.MaterialGatheringBonus, -4);
                Set(TransportStat.ForagingBonus, -4);
                Set(TransportStat.AmountEnemiesEncounteredRatio, 3.5f);
                Set(TransportStat.SpeedBonus, 4);
                Set(TransportStat.FleeBonus, -3);
                Set(TransportStat.EmissionsPerDay, 15);
                break;
        }
    }

    public void Set(TransportStat s, float value)
    {
        stats[s.ordinal()] = value;
    }
    public float Get(TransportStat s) {
        return stats[s.ordinal()];
    }
    public void Adjust(TransportStat s, float adjustment)
    {
        stats[s.ordinal()] += adjustment;
    }

    public static Transport RandomOf(EList<Transport> transports) {
        float total = 0;
        for (int i = 0; i < transports.size(); ++i)
            total += transports.get(i).defaultProbability;
        Random transportRandom = new Random();
        float r = transportRandom.nextFloat() * total;
    //    System.out.println("Randomed " + r + " out of " + total);
        for (int i = 0; i < transports.size(); ++i) {
            Transport t = transports.get(i);
            r -= t.defaultProbability;
      //      System.out.println("Total " + total);
            if (r < 0) {
      //          System.out.println("Yo " + t.name());
                return t;
            }
        }
        return transports.get(transports.size() - 1);
    }

    public static Transport readFrom(ObjectInputStream in) {
        Transport t = new Transport();
        try {
            t.readObject(in);
            return t;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

/*    public static Transport GetFromString(String s) {
        if (s.equals("Foot"))
            return Transport.Walking;
        for (int i = 0; i < values().length; ++i){
            Transport t = values()[i];
            if (t.name().equals(s))
                return t;
            if (s.contains(t.name()))
                return t;
        }
        System.out.println("ERROR: Unknown transport: "+s);
        new Exception().printStackTrace();
        System.exit(15);
        return Transport.Unknown;
    }
                */
}
