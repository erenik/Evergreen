package erenik.evergreen.common.player;

/**
 * Created by Emil on 2017-04-08.
 */

public class Difficulty {
    /*  0 - Easiest, 10% progress lost upon death (all stats n skills).
        1 - Easy, 20%,
        2 - Medium, 35% loss,
        3 - Hard, 50% loss,
        4 - Unforgiving, 75% loss.
        5 - Wipeout, 100%, hardcore
     */
    public static final int Easiest = 0;
    public static final int Easy = 1;
    public static final int Medium = 2;
    public static final int Hard = 3;
    public static final int Unforgiving = 4;
    public static final int Wipeout = 5;

    public static int Lives(int diff){
        switch (diff){
            case Easiest: return 100;           // 100 lives, basically living forever compared to the others.
            case Easy: return 25;              // Can die a lot without too much hard.
            case Medium: return 10;             // Now you need to play slightly defensively.
            case Hard: return 5;               // 5 minutes.
            case Unforgiving: return 3;        // 3 steps.
            case Wipeout: return 1; // 1 breath.
        }
        new Exception().printStackTrace();
        System.exit(1);
        return -1;
    }
    public static float LossRatio(int diff){
        switch (diff){
            case Easiest: return 0.01f;        // 1% loss? is almost nothing..
            case Easy: return 0.05f;              // 5%, can be felt.
            case Medium: return 0.10f;            // 10%, Oi vey.
            case Hard: return 0.25f;               // 25%, That hurts.
            case Unforgiving: return 0.50f;        // 50%, half to rebuild, quite a lot...
            case Wipeout: return 1; // 1 breath.
        }
        new Exception().printStackTrace();
        System.exit(1);
        return -1;
    }

    public static String String(int i) {
        switch (i){
            case Easiest: return "Easiest";
            case Easy: return "Easy";
            case Medium: return "Medium";
            case Hard: return "Hard";
            case Unforgiving: return "Unforgiving";
            case Wipeout: return "Wipeout";
        }
        return "Bad difficulty";
    }
}
