package erenik.evergreen.util;

import java.util.Random;

/**
 * Created by Emil on 2016-12-18.
 */

public class NameGenerator {
    static Random r = new Random(System.currentTimeMillis());
    public static void main(String[] args)
    {
        for (int i = 0; i < 10; ++i)
            System.out.println("Name: "+New());
    }
    public static String New()
    {
        String s = "";
        int length = r.nextInt(10)+2;
        int vowel = (r.nextBoolean()? +1 : -1) * (r.nextInt(5) + 1);
        for (int i = 0; i < length; ++i)
        {
            // Choose
            if (vowel == 0){
                int rr = r.nextInt(20);
                if (rr > 5)
                    vowel = -3;
                if (rr > 2)
                    s += " ";
                else
                    s += "'";
            }
            else if (vowel > 0)
                s += vowel();
            else
                s += consonant();
            /// Next one?
            if (vowel == 0)
                vowel = (r.nextBoolean()? +1 : -1) * (r.nextInt(5) + 1);
            else if (vowel > 0)
                vowel -= r.nextInt(3) + 2;
            else if (vowel < 0)
                vowel += r.nextInt(3) + 2;
        }
        s = capitalize(s, 0);
//        System.out.println("Random name: "+s);
        return s;
    }

    private static String capitalize(String s, int i) {
        char c = s.charAt(i);
        char n;
        n = capital(c);
        String newString = s.substring(0, i) + n + s.substring(i+1);
        return newString;
    }
    public static char capital(char c)
    {
        switch (c)
        {
            case 'a': return 'A';
            case 'b': return 'B';
            case 'c': return 'C';
            case 'd': return 'D';
            case 'e': return 'E';
            case 'f': return 'F';
            case 'g': return 'G';
            case 'h': return 'H';
            case 'i': return 'I';
            case 'j': return 'J';
            case 'k': return 'K';
            case 'l': return 'L';
            case 'm': return 'M';
            case 'n': return 'N';
            case 'o': return 'O';
            case 'p': return 'P';
            case 'q': return 'Q';
            case 'r': return 'R';
            case 's': return 'S';
            case 't': return 'T';
            case 'u': return 'U';
            case 'v': return 'V';
            case 'w': return 'W';
            case 'x': return 'X';
            case 'y': return 'Y';
            case 'z': return 'Z';
            default: return '-';
        }
    }

    public static String vowel()
    {
        String[] vowels = new String[]{"a", "e", "i", "o", "u", "y"};
        String vowel = vowels[r.nextInt(vowels.length+5) % vowels.length];
        return vowel;
    }
    public static String consonant()
    {
        String[] cs = new String[]{"b", "c", "d", "f", "g", "h", "j", "k", "l", "m", "n", "p", "q", "r", "s", "t", "v", "w", "x", "z"};
        String c = cs[r.nextInt(cs.length+5) % cs.length];
        return c;
    }
}
