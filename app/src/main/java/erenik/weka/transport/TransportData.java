package erenik.weka.transport;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import erenik.evergreen.common.player.Transport;
import weka.filters.unsupervised.attribute.Add;

/**
 * Structure for containing and comparing transport data over longer periods of time?
 * Created by Emil on 2017-03-02.
 */

public class TransportData implements Serializable {

    private static final long SecondsInAYear = 31557600;
    private static final long MillisecondsInAYear = 31557600000L;
    private static final long MillisecondsPerHour = 3600000;
    private TransportOccurrence lastEntry = null;

    static TransportData BuildPrimaryTree(){
        TransportData td = new TransportData(TimePeriod.AllData);
        return td;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(timePeriodContained);
        out.writeLong(timePeriodID);
        out.writeLong(startTimeMs);
        out.writeLong(endTimeMs);
        out.writeObject(children);
        out.writeObject(data);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        // populate the fields of 'this' from the data in 'in'...
        timePeriodContained = (TimePeriod) in.readObject();
        timePeriodID = in.readLong();
        startTimeMs = in.readLong();
        endTimeMs = in.readLong();
        children = (ArrayList<TransportData>) in.readObject();
        data = (ArrayList<TransportOccurrence>) in.readObject();
        if (data.size() > 0)
            lastEntry = data.get(data.size() - 1); // Just point it to the last one, makes things easier...
    }

    public void PrintAllData() {
        if (this.timePeriodContained != TimePeriod.AllData)
            return;
        System.out.println("TransportData "+timePeriodContained.name()+" id "+timePeriodID);
        System.out.println(" startTimeMs"+startTimeMs+" endTimeMs "+endTimeMs);
        System.out.println(" children: "+children.size()+" dataPoints: "+data.size());
        System.out.println(" hours: "+Hours().size());
        for (int i = 0; i < children.size(); ++i){
            children.get(i).PrintAllData();
        }
    }

    private TransportData(TimePeriod tp){
        this.timePeriodContained = tp;
    };

    /** add data,
     - when have a day of data, put into a day aggregate - remove the old data.
     - when have next day, make a new day aggregate,
     - when have 7 days of data, make a week aggregate,
     - when have 14 days of data, remove 7 oldest day aggregates,
     - when have 13 weeks of data, make quarter aggregate,
     */
    void AddData(TransportOccurrence to, TransportData parent){
        this.data.add(to);
        lastEntry = to;
        if (data.size() < 2)
            return;
        /// Check the first and last data.
        TransportOccurrence first = data.get(0),
            last = data.get(data.size() - 1);

        long duration = last.startTimeMs + last.durationMs - first.startTimeMs; // end time - start time = duration.
        System.out.println(duration+" > "+MillisecondsPerHour+" = "+(duration > MillisecondsPerHour));
        if (duration > MillisecondsPerHour){
            System.out.println("Time to make an hour! MS duration: "+duration);
            // Make them into an hour.
            TransportData child = new TransportData(TimePeriod.Hour);
            child.startTimeMs = first.startTimeMs;
            child.endTimeMs = last.startTimeMs + last.durationMs;
            this.children.add(child);
            // Move the raw data into this hour as well. Remove raw data in the next step when the hours are turned into days, or when we have at least 2 hours of data.
            child.data.addAll(data); // Move into here.
            data.clear(); // Clear this one.

        }
        // Go from the 2nd newest hour and backwards.
        ArrayList<TransportData> hours = Hours();
        for (int i = hours.size() - 2; i >= 0; --i) {
            hours.get(i).Aggregate();
        }
        if (hours.size() > 25){
            // Make a day out of the hours.
            TransportData newDay = new TransportData(TimePeriod.Day);
            newDay.startTimeMs = hours.get(0).startTimeMs;
            newDay.endTimeMs = hours.get(23).endTimeMs;
            for (int i = 0; i < 24; ++i){
                TransportData hour = hours.get(i);
                newDay.data.addAll(hour.data);
                hour.data = null;
                children.remove(hour); // Remove the hour as it is no longer needed.
            }
            newDay.Aggregate(); // Sum it up.
            children.add(newDay); // Add it to the list of relevant children.
        }
    }

    private void Aggregate() {
        ArrayList<TransportOccurrence> newData = new ArrayList<>();
        for (int i = 0; i < TransportType.values().length; ++i){
            TransportType tt = TransportType.values()[i];
            long durationTimeMs = GetTimeMs(tt);
            TransportOccurrence newOcc = new TransportOccurrence();
            newOcc.transport = tt;
            newOcc.startTimeMs = this.startTimeMs;
            newOcc.durationMs = durationTimeMs;
            newData.add(newOcc);
        }
        data.clear();     // Clear the old data.
        data = newData;   // Replace with new aggregate to reduce memory/storage needed.
    }

    private ArrayList<TransportData> Hours() {
        ArrayList<TransportData> result = new ArrayList<>();
        for (int i = 0; i < children.size(); ++i){
            TransportData td = children.get(i);
            if (td.timePeriodContained == TimePeriod.Hour)
                result.add(td);
        }
        return result;
    }

    private void AddNewChild() {
        switch (this.timePeriodContained){
            case AllData:
                children.add(new TransportData(TimePeriod.Year));
                break;
            case Year:
                children.add(new TransportData(TimePeriod.Quarter));
                break;
            case Quarter:
                children.add(new TransportData(TimePeriod.Week));
                break;
            case Week:
                children.add(new TransportData(TimePeriod.Day));
                break;
            case Day:
                children.add(new TransportData(TimePeriod.Hour));
                break;
        }
        UpdateChildrenTimePeriodsAndTimestamps();
    }

    private void FigureOutTimePeriod(TransportData parentNode) {
        switch (timePeriodContained){
            case AllData:
                this.timePeriodID = 1;
                break; // Set default to 1.
            case Year:
                this.timePeriodID = System.currentTimeMillis() / MillisecondsInAYear;
                this.startTimeMs = this.timePeriodID * MillisecondsInAYear;
                this.endTimeMs = this.startTimeMs + MillisecondsInAYear;
                UpdateChildrenTimePeriodsAndTimestamps();
                break;
            default:
                return;
        }
        for (int i = 0; i < children.size(); ++i)
            children.get(i).FigureOutTimePeriod(this);
    }

    private void UpdateChildrenTimePeriodsAndTimestamps() {
        int divider = 1;
        switch (this.timePeriodContained){
            case Year: divider = 4; break; // 4 quarters in a year.
            case Quarter: divider = 13; break; // 13 weeks in a quarter
            case Week: divider = 7; break; // 7 days in a week.
            case Day: divider = 24; break;
            case Hour: divider = 0; break; // 0!
        }
        if (divider == 0)
            return;
        long timePerChildMs = (endTimeMs - startTimeMs) / divider;
        for (int i = 0; i < children.size(); ++i){
            TransportData td = children.get(i);
            td.startTimeMs = startTimeMs + i * timePerChildMs; // Start time depends on child #.
            td.endTimeMs = td.startTimeMs + timePerChildMs; // Duration is constant.
        }
    }

    private TransportData MostRecentChild() {
        if (children.size() == 0)
            return null;
        TransportData mostRecent = children.get(0);
        for (int i = 1; i < children.size(); ++i){
            TransportData d = children.get(i);
            if (d.startTimeMs > mostRecent.startTimeMs)
                mostRecent = d;
        }
        return mostRecent;
    }

    /// Returns data back up until the target threshold in milliseconds system time. Beyond an hour's usage, this may or may not work as intended. Granularity of seconds decays at 1 hours' limit.
    public ArrayList<TransportOccurrence> GetDataSeconds(long thresholdTimeMs) {
        ArrayList<TransportOccurrence> newList = new ArrayList<>();
        for (int i = 0; i < data.size(); ++i){   // Add from data here.
            TransportOccurrence to = data.get(i);
            if (to.startTimeMs > thresholdTimeMs)
                newList.add(to);
        }
        for (int i = 0; i < children.size(); ++i){ // Add from children.
            newList.addAll(children.get(i).GetDataSeconds(thresholdTimeMs));
        }
        return newList;
    }

    public TransportOccurrence LastEntry() {
        return lastEntry;
    }

    enum TimePeriod {
        Hour, // Not used atm.
        Day, // 24 hours
        Week, // 7 days
        Quarter, // 13 weeks
        Year, // 52 weeks
        AllData,
    };
    /// Holds the time-period contained within this data. Should be one of the enums above.
    TimePeriod timePeriodContained;
    /// The ID of this time period, which will be based on the total # of equivalent time periods, as calculated via System.currentTimeMillis() (unit time since 1980 probably?)
    long timePeriodID = 0;
    /// Start-time in milliseconds of this time-period, and its corresponding ending time.
    long startTimeMs, endTimeMs;
    /// The actual data. Should not be used directly here in this base class?
    /// Each occurrence holds data for transport ID, duration, start-time and possibly ratio compared to the total of this time period (not guaranteed, should be calculated before use).
    protected ArrayList<TransportOccurrence> data = new ArrayList<>();
    /// Contains data on the children of this data.
    protected ArrayList<TransportData> children = new ArrayList<>();

    /// Returns ratio of time spent in a certain mode of transport for this data-set.
    public float Ratio(TransportType t){
        long time = GetTimeMs(t);
        long totTime = GetTotalTimeMs();
        return time / (float) totTime;
    }

    /// Returns total time for target transport type in milliseconds.
    private long GetTotalTimeMs() {
        long totalMs = 0;
        for (int i = 0; i < data.size(); ++i) {
            totalMs += data.get(i).durationMs;
        }
        for (int i = 0; i < children.size(); ++i)
            totalMs += children.get(i).GetTotalTimeMs();
        return totalMs;
    }
    /// Returns total time for target transport type in milliseconds.
    public long GetTimeMs(TransportType tt){
        long totalMs = 0;
        for (int i = 0; i < data.size(); ++i) {
            TransportOccurrence to = data.get(i);
            if (to.transport.ordinal() == tt.ordinal())
                totalMs += to.durationMs;
        }
        for (int i = 0; i < children.size(); ++i)
            totalMs += children.get(i).GetTimeMs(tt);
        return totalMs;
    }
}
