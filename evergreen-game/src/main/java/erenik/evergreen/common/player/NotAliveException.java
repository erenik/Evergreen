package erenik.evergreen.common.player;

/**
 * Created by Emil on 2016-10-31.
 */
public class NotAliveException extends Exception
{
    NotAliveException() {
        super("HP reached below 0.");
    }
}
