package evergreen.util;

import java.util.Random;

/**
 * Created by Emil on 2016-12-18.
 */

public class NameGenerator {
    static Random r = new Random(System.currentTimeMillis());
    public static void main(String[] args)
    {
        for (int i = 0; i < 10; ++i)
            Printer.out("Name: "+New());
    }
    public static String New()
    {
        String s = "";
        int length = r.nextInt(4)+2;
        boolean vowel = r.nextBoolean();
        boolean wasSpaceOrApostrophe = false;
        for (int i = 0; i < length; ++i)
        {
            // Choose
            if (vowel){
                s += anyVowelCombination();
            }
            else // Consonant
            {
                if (i == 0)
                    s += consonant();
                else
                    s += anyConsonantCombination();
            }
            if (!wasSpaceOrApostrophe) {
                int rr = r.nextInt(50);
                int dashChance = 2;
                int apostropheChance = 2;
                if (rr < dashChance) {
                    s += "-";
                    wasSpaceOrApostrophe = true;
                }
                else {
                    rr -= dashChance;
                    if (rr < apostropheChance) {
                        s += "'";
                        wasSpaceOrApostrophe = true;
                    }
                }
            }
            else
                wasSpaceOrApostrophe = false;

            vowel = !vowel; // Flip vowel to consonant.
        }

        String[] replacements = new String[] {"f", "ph",
                "k", "c",
        "c", "k",
        "k", "ck",
        "y", "i",
        "z", "ts",
        "z", "s",
        "x", "s",
        "x", "ks",
        "q", "k"};
        for (int i = 0; i < replacements.length - 2; i += 2) {
            String fromS = replacements[i], toS = replacements[i+1];
            if (s.contains(fromS) && (r.nextInt(100) < 50)) {
                // Replace
                s = s.replace(fromS, toS); // Replace and update.
//                Printer.out(" replaced "+fromS+" with "+toS);
            }
        }
        s = capitalize(s, 0);



//        Printer.out("Random name: "+s);
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

    public static String anyVowelCombination()
    {
        // Add double vowels later?
        return r.nextInt(100) < 10? vowel3() : r.nextInt(100) < 25? vowel() + vowel() : vowel();
    }
    public static String vowel3()
    {
        // Triple vowels. Steal from french!
        String[] v = new String[] {"eau", "aio", "aou", "oua"};
        String vowel = v[r.nextInt(v.length*2) % v.length];
        return vowel;
    }
    public static String vowel()
    {
        String[] vowels = new String[]{"a", "e", "i", "o", "u", "y"};
        String vowel = vowels[r.nextInt(vowels.length*2) % vowels.length];
        return vowel;
    }
    public static String anyConsonantCombination()
    {
        return r.nextInt(100) < 30? consonantPair() : consonant();
       // return consonant();
    }
    public static String consonant()
    {
        String[] cs = new String[]{"b", "c", "d", "f", "g", "h", "j", "k", "l", "m", "n", "p", "q", "r", "s", "t", "v", "w", "x", "z"};
        String c = cs[r.nextInt(cs.length*2) % cs.length];
        return c;
    }
    public static String consonantPair()
    {
        while(true) {
            String s = consonant() + consonant();
            if (s.equals("jw"))
                continue;
            return s;
        }
    }
}
