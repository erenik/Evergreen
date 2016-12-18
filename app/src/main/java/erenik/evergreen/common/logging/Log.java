package erenik.evergreen.common.logging;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by Emil on 2016-10-31.
 */ /// For the player game-log. To be color-coded etc.?
public class Log implements Serializable
{
    public Log(String s, LogType t)
    {
        text = s;
        type = t;
        date = new Date();
    }
    private void writeObject(java.io.ObjectOutputStream out) throws IOException
    {
        out.writeObject(date);
        out.writeObject(text);
        out.writeObject(type);
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        date = (Date) in.readObject();
        text = (String) in.readObject();
        type = (LogType) in.readObject();
    }
    private void readObjectNoData() throws ObjectStreamException
    {

    }

    Date date; // Time-stamp of this log message.
    public String text;
    public LogType type;
}
