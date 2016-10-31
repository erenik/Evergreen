package erenik.seriousgames.evergreen;

import java.util.Random;

/**
 * Created by Emil on 2016-10-30.
 */
public class Dice
{
    Dice(int type, int num, int bonus)
    {
        diceType = type;
        dice = num;
        this.bonus = bonus;
    }
    int Roll()
    {
        int total = bonus;
        for (int i = 0; i < dice; ++i)
            total += r.nextInt(diceType) + 1;
        System.out.println(dice+"D"+diceType+""+(bonus > 0? "+"+bonus : bonus < 0? ""+bonus : "")+" = "+total);
        return total;
    }
    int diceType; // Sides of the dice. E.g. 6 for D6, 3 for D3. etc.
    int dice;
    int bonus;
    static Random r = new Random();

    static void InitSeed()
    {
        r = new Random(System.nanoTime());
    }
    /// E.g. 2, 3 (2D6+3) may yield anything from 5 to 15, with larger distributions near 10.
    static int RollD6(int numDice)
    {
        int total = 0;
        for (int i = 0; i < numDice; ++i)
            total += r.nextInt(6) + 1;
        System.out.println(numDice+"D6 = "+total);
        return total;
    }
    static int RollD3(int numDice)
    {
        int total = 0;
        for (int i = 0; i < numDice; ++i)
            total += r.nextInt(3) + 1;
        System.out.println(numDice+"D3 = "+total);
        return total;
    }
}
