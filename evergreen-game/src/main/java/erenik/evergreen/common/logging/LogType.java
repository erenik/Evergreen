package erenik.evergreen.common.logging;

/**
 * Created by Emil on 2016-10-31.
 */

public enum LogType
{
    Undefined,
    INFO, // General info
    PROBLEM_NOTIFICATION, // Warning/problem notifications.
    PROGRESS,
    ATTACKED, // For when taking damage.
    EVENT,
    ATTACK_MISS, ATTACK, EXP, ATTACKED_MISS,
    ACTION_NO_PROGRESS, // Uses same color as Attack Miss, but will not be filtered the same way.
    ACTION_FAILURE, // Uses same color as Attacked, but will not be filtered the same way.
    SUCCESS, OtherDamage,
    ENC_INFO, // Other info in encounters. status effects, etc.
    Error,
    PLAYER_ATTACK, PLAYER_ATTACK_MISS,
    DEFEATED_ENEMY, DEFEATED;

    /*
    int HexColor()
    {
        return getColor(getContext(), GetResourceColor());
    };*/ // Text font color for this message.
};

;
