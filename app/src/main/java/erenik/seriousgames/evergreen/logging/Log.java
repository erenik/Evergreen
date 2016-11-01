package erenik.seriousgames.evergreen.logging;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

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

    /// No filter, all is shown.
    public static void UpdateLog(ViewGroup vg, Context context, int maxLinesToDisplay)
    {
        UpdateLog(vg, context, maxLinesToDisplay, Arrays.asList(LogType.values()));
    }
    // Specify filter.
    public static void UpdateLog(ViewGroup vg, Context context, int maxLinesToDisplay, List<LogType> typesToShow)
    {
        System.out.println("Log.UpdateLog");
        Player player = Player.getSingleton();
        ViewGroup v = vg;
        // Remove children.
        v.removeAllViews();
        // Add new ones?
        int numDisplay = player.log.size();
        numDisplay = numDisplay > maxLinesToDisplay ? maxLinesToDisplay : numDisplay;
        int startIndex = player.log.size() - numDisplay;
        System.out.println("Start index: "+startIndex+" log size: "+player.log.size());
        View lastAdded = null;
        for (int i = startIndex; i < player.log.size(); ++i)
        {
            Log l = player.log.get(i);
            boolean show = false;
            for (int j = 0; j < typesToShow.size(); ++j)
            {
                if (l.type.ordinal() == typesToShow.get(j).ordinal())
                    show = true;
            }
            if (!show)
                continue;
            String s = l.text;
            TextView t = new TextView(context);
            t.setText(s);
            int hex = ContextCompat.getColor(context, l.type.GetResourceColor());
            // System.out.println("Colorizing: "+Integer.toHexString(hex));
            t.setTextColor(hex);
            v.addView(t);
            t.setFocusable(true); // Focusable.
            t.setFocusableInTouchMode(true);
            lastAdded = t;
        }
        if (lastAdded != null)
            lastAdded.requestFocus(); // Request focus, make visible?
    }
}
