package erenik.evergreen.common.logging;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import erenik.evergreen.common.Enumerator;

/**
 * Created by Emil on 2016-10-31.
 */ /// For the player game-log. To be color-coded etc.?
public class Log implements Serializable {

    /** True for all Log-objects created earlier.
     * Those with this flag as false use the new Log-type where an enumerated text-ID is used
     * and arguments are provided to fill in the gaps, so to speak, depending on the locale
     * and language chosen by the end-user.
    */
    boolean stringBasicVersion = true;
    // 0 - Not displayed, 1 - Displayed at client, 2 - confirmed at server.
    public int displayedToEndUser = 0;

    public boolean BasicStringVersion() { return stringBasicVersion;};
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
    public Log(LogTextID ltid, LogType t, ArrayList<String> args) {
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
        out.writeObject(args);
        out.writeBoolean(stringBasicVersion);
        out.writeInt(displayedToEndUser);
        out.writeLong(logID);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        date = (Date) in.readObject();
        text = (String) in.readObject();
        type = LogType.values()[in.readInt()];
        ltid = LogTextID.values()[in.readInt()];
        args = (ArrayList<String>) in.readObject();
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
    public List<String> Args(){return args;};
    /// New vars added for better cross-platform, cross-language logging.
    LogTextID ltid = LogTextID.undefined;
    ArrayList<String> args = new ArrayList<>();
    /// Main stats.
    Date date = new Date(); // Time-stamp of this log message.
    public String text = "";
    public LogType type = LogType.Undefined;
    private long logID = -1;     // The actual ID of this specific message, to synchronize properly between client and server and reduce bandwidth consumption for saving/loading procedures.

    public static Enumerator logIDEnumerator = null;

    public long LogID() {
        return logID;
    }
}
