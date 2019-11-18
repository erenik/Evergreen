package evergreen.common.logging;

import evergreen.common.Player;
import evergreen.common.encounter.Encounter;

/**
 * Created by Emil on 2017-01-28.
 */

public interface LogListener {
    /// When a log messages is logged for target player and you want to do something more with it as well...
    public void OnLog(Log l, Player player);
//    public void OnLog(Log l, Encounter enc);
}
