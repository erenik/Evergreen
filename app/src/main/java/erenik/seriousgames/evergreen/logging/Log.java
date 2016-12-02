package erenik.seriousgames.evergreen.logging;

import java.util.Date;

import erenik.seriousgames.evergreen.logging.LogType;

/**
 * Created by Emil on 2016-10-31.
 */ /// For the player game-log. To be color-coded etc.?
public class Log
{
    public Log(String s, LogType t)
    {
        text = s;
        type = t;
        date = new Date();
    }
    Date date; // Time-stamp of this log message.
    public String text;
    public LogType type;
}
