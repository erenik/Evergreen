package erenik.evergreen.logging;

import java.util.Date;

/**
 * Created by Emil on 2016-10-31.
 */

public enum LogType
{
    INFO, // General info
    PROBLEM_NOTIFICATION, // Warning/problem notifications.
    PROGRESS,
    ATTACKED, // For when taking damage.
    EVENT,
    ATTACK_MISS, ATTACK, EXP, ATTACKED_MISS,
    SUCCESS, OtherDamage;
    /*
    int HexColor()
    {
        return getColor(getContext(), GetResourceColor());
    };*/ // Text font color for this message.
};

;
