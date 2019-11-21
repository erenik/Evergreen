/**
 * PS C:\Windows\system32> $payload = @{ name = "Arvid"} | ConvertTo-Json;  Invoke-WebRequest -Method POST -Uri "http://localhost:8080/register/" -Body $payload -Verbose -Headers @{ "Content-Type" = "application/json"}
VERBOSE: POST http://localhost:8080/register/ with -1-byte payload
VERBOSE: received -1-byte response of content type application/json;charset=UTF-8


StatusCode        : 200
StatusDescription :
Content           : {"userName":"Hello, Arvid!","success":true}
RawContent        : HTTP/1.1 200
                    Transfer-Encoding: chunked
                    Content-Type: application/json;charset=UTF-8
                    Date: Mon, 18 Nov 2019 20:45:37 GMT

                    {"userName":"Hello, Arvid!","success":true}
Forms             : {}
Headers           : {[Transfer-Encoding, chunked], [Content-Type, application/json;charset=UTF-8], [Date, Mon, 18 Nov 2019 20:45:37 G
                    MT]}
Images            : {}
InputFields       : {}
Links             : {}
ParsedHtml        : System.__ComObject
RawContentLength  : 43
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
public class RegisterController {

    @RequestMapping("/isNameFree")
    public Boolean isNameFree(@RequestParam(value="name") String name) {
        Game game = Game.DefaultGame();
        Player player = game.GetPlayer(name);
        return (player != null);
    }

//    @RequestMapping(value = "/register", method = RequestMethod.POST, produces = "application/json")
    @PostMapping("/register")
    public RegisterResult register(@RequestBody RegisterPlayer newRegistration) throws PlayerAlreadyExistsException {
        String name = newRegistration.name;
        System.out.println("Register request received for name: "+name);
        if (name == null){
            System.out.println("Bad name!");
            return new RegisterResult(name, false);
        }
        Game game = Game.DefaultGame();
        Player player = game.GetPlayer(name);
        if (player != null){
            String reason = "Declined, "+name+" already exists in the game.";
            System.out.println(reason);
            throw new ResponseStatusException(HttpStatus.CONFLICT, reason);
        }
        System.out.println("Player name not taken, registering.");

        player = new Player();
        player.name = name;
        player.Set(Config.StartingBonus, newRegistration.startingBonus);
        player.Set(Config.Difficulty, newRegistration.difficulty);
        player.Set(Config.Avatar, newRegistration.avatar);

        game.AddPlayer(player);
        System.out.println("Player "+name+" joined the game.");
        return new RegisterResult(name, true);
    }
}