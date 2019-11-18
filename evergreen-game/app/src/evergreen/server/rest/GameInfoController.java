/**
*/

package evergreen.server.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import evergreen.Game;
import evergreen.common.Player;
import evergreen.common.logging.Log;
import evergreen.common.player.Config;

@RestController
public class GameInfoController {

	
    public class GameStatus {
        public int players;
        public int day;
    }

    @RequestMapping("/status")
    public GameStatus status() {
        Game game = Game.DefaultGame();
        GameStatus gs = new GameStatus();
        gs.players = game.players.size();
        gs.day = game.Day();
        return gs;
	}
	
}
