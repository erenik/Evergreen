package evergreen.common.logging;

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
    ENC_INFO_FAILED, // When stuff tries to happen but failes.
    Error,
    PLAYER_ATTACK, // Attacking player, attacked player, damage
    PLAYER_ATTACK_MISS, // Attacking player, attacked player
    PLAYER_ATTACKED_ME, // Same as those 2 above, but other colors later.
    PLAYER_ATTACKED_ME_BUT_MISSED, // Same as those 2 above, but other colors later.
    DEFEATED_ENEMY, DEFEATED,
    ;
    /*
    int HexColor()
    {
        return getColor(getContext(), GetResourceColor());
    };*/ // Text font color for this message.
};

;
