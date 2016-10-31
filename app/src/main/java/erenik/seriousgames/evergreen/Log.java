package erenik.seriousgames.evergreen;

import java.util.Date;

/**
 * Created by Emil on 2016-10-31.
 */

enum LogType
{
    INFO, // General info
    PROBLEM_NOTIFICATION, // Warning/problem notifications.
    PROGRESS,
    ATTACKED, // For when taking damage.
    EVENT,
    ATTACK_MISS, ATTACK, EXP, ATTACKED_MISS;
    /*
    int HexColor()
    {
        return getColor(getContext(), GetResourceColor());
    };*/ // Text font color for this message.
    int GetResourceColor()
    {
        switch(this)
        {
            case ATTACK: return R.color.attack;
            case ATTACKED_MISS:
            case ATTACK_MISS: return R.color.attackMiss;
            case INFO: return R.color.info;
            case PROGRESS: return R.color.progress;
            case EXP: return R.color.exp;
            case ATTACKED: return R.color.attacked;
            case EVENT: return R.color.event;
            case PROBLEM_NOTIFICATION: return R.color.problemNotification;
        }
        return R.color.black;

    }
};

/// For the player game-log. To be color-coded etc.?
public class Log
{
    Log(String s, LogType t)
    {
        text = s;
        type = t;
        date = new Date();
    }
    Date date; // Time-stamp of this log message.
    String text;
    LogType type;
};
