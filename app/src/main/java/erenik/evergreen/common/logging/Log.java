package erenik.evergreen.common.logging;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    /// String and Type, type determines varous filtering and color-coding schemes.
    public Log(String s, LogType t) {
        text = s;
        type = t;
        date = new Date();
    }
    /// String and Type, type determines varous filtering and color-coding schemes.
    public Log(LogTextID ltid, LogType t, ArrayList<String> args) {
        this.ltid = ltid;
        this.type = t;
        date = new Date();
        this.args = args;
    }
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(date);
        out.writeObject(text);
        out.writeObject(type);
        out.writeObject(ltid);
        out.writeObject(args);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        date = (Date) in.readObject();
        text = (String) in.readObject();
        type = (LogType) in.readObject();
        ltid = (LogTextID) in.readObject();
        args = (ArrayList<String>) in.readObject();
    }
    private void readObjectNoData() throws ObjectStreamException
    {

    }
    /// New vars added for better cross-platform, cross-language logging.
    LogTextID ltid;
    ArrayList<String> args = new ArrayList<>();
    /// Main stats.
    Date date; // Time-stamp of this log message.
    public String text;
    public LogType type;
}
