package erenik.evergreen.common.player;

import erenik.evergreen.common.Player;

/**
 * Created by Emil on 2016-12-18.
 */

public interface PlayerListener {
    void OnPlayerDied(Player player);
    void OnPlayerNewDay(Player player);
}
