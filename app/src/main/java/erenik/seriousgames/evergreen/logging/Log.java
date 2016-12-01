package erenik.seriousgames.evergreen.logging;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import erenik.seriousgames.evergreen.App;
import erenik.seriousgames.evergreen.R;
import erenik.seriousgames.evergreen.logging.LogType;
import erenik.seriousgames.evergreen.player.Player;

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
