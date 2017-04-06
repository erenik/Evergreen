package erenik.evergreen.common.logging;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Date;

import erenik.evergreen.common.Enumerator;
import erenik.util.EList;

/**
 * Created by Emil on 2016-10-31.
 */ /// For the player game-log. To be color-coded etc.?
public class Log implements Serializable {
    private static final long serialVersionUID = 1L;


    /** True for all Log-objects created earlier.
     * Those with this flag as false use the new Log-type where an enumerated text-ID is used
     * and arguments are provided to fill in the gaps, so to speak, depending on the locale
     * and language chosen by the end-user.
    */
    boolean stringBasicVersion = true;
    // 0 - Not displayed, 1 - Displayed at client, 2 - confirmed at server.
    public int displayedToEndUser = 0;

    public boolean BasicStringVersion() { return stringBasicVersion;};
    // Empty
    public Log(){}
    /// String and Type, type determines varous filtering and color-coding schemes.
    public Log(String s, LogType t) {
        logID = ++logIDEnumerator.value;
        text = s;
        type = t;
        date = new Date();
    }
    /// String and Type, type determines varous filtering and color-coding schemes.
    public Log(LogTextID ltid, LogType t) {
        logID = ++logIDEnumerator.value;
        this.ltid = ltid;
        this.type = t;
        date = new Date();
        stringBasicVersion = false;
    }
    /// String and Type, type determines varous filtering and color-coding schemes.
    public Log(LogTextID ltid, LogType t, String arg1) {
        logID = ++logIDEnumerator.value;
        this.ltid = ltid;
        this.type = t;
        date = new Date();
        this.args.add(arg1);
        stringBasicVersion = false;
    }
    /// String and Type, type determines varous filtering and color-coding schemes.
    public Log(LogTextID ltid, LogType t, String arg1, String arg2) {
        logID = ++logIDEnumerator.value;
        this.ltid = ltid;
        this.type = t;
        date = new Date();
        this.args.add(arg1);
        this.args.add(arg2);
        stringBasicVersion = false;
    }
    /// String and Type, type determines varous filtering and color-coding schemes.
    public Log(LogTextID ltid, LogType t, String arg1, String arg2, String arg3) {
        logID = ++logIDEnumerator.value;
        this.ltid = ltid;
        this.type = t;
        date = new Date();
        this.args.add(arg1);
        this.args.add(arg2);
        this.args.add(arg3);
        stringBasicVersion = false;
    }
    public Log(LogTextID ltid, LogType t, EList<String> args) {
        logID = ++logIDEnumerator.value;
        this.ltid = ltid;
        this.type = t;
        date = new Date();
        this.args = args;
        stringBasicVersion = false;
    }

    @Override
    public String toString() {
        if (stringBasicVersion)
            return this.text;
        String s = ltid.name();
        if (args.size() > 0)
            s += " args: ";
        for (int i = 0; i < args.size(); ++i){
            s += args.get(i)+", ";
        }
        return s;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(date);
        out.writeObject(text);
        out.writeInt(type.ordinal());
        out.writeInt(ltid.ordinal());
        out.writeInt(args.size());
        for (int i = 0; i < args.size(); ++i){
            out.writeObject(args.get(i));
        }
//        out.writeObject(args); // Write it as an array instead of EList! Supposedly safer. http://stackoverflow.com/questions/20275623/type-safety-unchecked-cast-from-object-to-arraylistmyvariable
        out.writeBoolean(stringBasicVersion);
        out.writeInt(displayedToEndUser);
        out.writeLong(logID);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        date = (Date) in.readObject();
        text = (String) in.readObject();
        type = LogType.values()[in.readInt()];
        ltid = LogTextID.values()[in.readInt()];
        int numArgs = in.readInt();
        args = new EList<>();
        for (int i = 0; i < numArgs; ++i){
            String str = (String) in.readObject();
            args.add(str);
        }
//        args = (EList<String>) in.readObject();
        stringBasicVersion = in.readBoolean();
        displayedToEndUser = in.readInt();
        logID = in.readLong();
    }
    private void readObjectNoData() throws ObjectStreamException
    {

    }
    /// Text-ID on the android-platform for cross-language performance.
    public LogTextID TextID() {
        return ltid;
    }
    public EList<String> Args(){return args;};
    /// New vars added for better cross-platform, cross-language logging.
    LogTextID ltid = LogTextID.undefined;
    EList<String> args = new EList<>();
    /// Main stats.
    Date date = new Date(); // Time-stamp of this log message.
    public String text = "";
    public LogType type = LogType.Undefined;
    private long logID = -1;     // The actual ID of this specific message, to synchronize properly between client and server and reduce bandwidth consumption for saving/loading procedures.

    public static Enumerator logIDEnumerator = null;

    public long LogID() {
        return logID;
    }

    public static void PrintLastLogMessages(EList<Log> log, int num) {
        EList<Log> logs = log;
        if (logs == null){
            System.out.println("Log.PrintLastLogMessages, null log, aborting");
            new Exception().printStackTrace();
            return;
        }
        for (int i = logs.size() - num; i < logs.size(); ++i){
            if (i < 0)
                continue;
            Log l = logs.get(i);
            System.out.println("Log msg "+l.LogID()+" index"+i+" "+l);
        }

    }

    public static EList<Log> ApplyFilters(EList<Log> list, EList<LogType> logTypesToFilter) {
        EList<Log> newList = new EList<>();
        for (int i = 0; i < list.size(); ++i) {
            Log l = list.get(i);
            boolean show = true;
            for (int j = 0; j < logTypesToFilter.size(); ++j) {
                if (l.type.ordinal() == logTypesToFilter.get(j).ordinal()) {
                    show = false;
                    continue;
                }
            }
            if (!show) {
                System.out.println("Skipping message "+l.LogID()+" "+l);
                continue;
            }
            newList.add(l);
        }
        return newList;
    }

    public void readFrom(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readObject(in);
    }

    public void writeTo(ObjectOutputStream out) throws IOException {
        writeObject(out);
    }
}
