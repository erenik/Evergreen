package erenik.evergreen.android.act;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import erenik.evergreen.R;
import erenik.evergreen.android.App;
import erenik.evergreen.common.Player;
import erenik.evergreen.common.logging.Log;
import erenik.evergreen.common.logging.LogTextID;

/**
 * Created by Emil on 2017-03-03.
 */

public class ResultsScreen extends EvergreenActivity  {

    /// Main init function
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results_screen);

        Button b = (Button) findViewById(R.id.buttonOK);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ViewGroup vg = (ViewGroup) findViewById(R.id.layoutContents); // Reset layout with contents.
        vg.removeAllViews();
        // Populate it with those log messages which we are interested in viewing here in the results screen? All log messages which haven't been flagged as "displayed" yet.
        Player p = App.GetPlayer();
        for (int i = 0; i < p.log.size(); ++i){
            Log l = p.log.get(i);
            if (l.displayedToEndUser)
                continue;
            l.displayedToEndUser = true;
            View viewToAdd = null;
            switch (l.TextID()){
                /// Messages to be displayed as they are.
                case reduceEmissionsSuccessful:
                case reduceEmissionsMostlySuccessful:
                case reduceEmissionsNotSoSuccessful:
                case reduceEmissionsFailed:
                case scoutingSuccess:
                case scoutingFailure:
                case fledFromCombat:  // 1 arg, name of the fleer.
                case playerFledFromCombat:
                case playerTriedToFlee: // 1 arg, name of fleer
                case triedToFlee: // 2 args, quantity and name of attacking monster type.
                case shelterAttacked:
                    // Max 1 encounter per scouting sessions? Number determines strength?
                case scoutRandomEncounter:
                case playerPlayerAttack:case playerMonsterAttack:case monsterPlayerAttack:case playerPlayerAttackMiss:case monsterPlayerAttackMiss:case playerMonsterAttackMiss:
                case playerVanquishedMonster:case monsterKnockedOutPlayer:
                case secondLife:
                    // Print it as they are.
                    viewToAdd = GetViewForLogMessage(l);
                    break;
                default:
                    // Print a bad thingy? Cause crash?
                    String s = "Old text version: "+l.text;
                    if (l.BasicStringVersion() == false)
                        s = "Not configured new text: "+App.GetLogText(l.TextID(), l.Args());
                    TextView t = new TextView(getBaseContext());
                    t.setText(s);
                    int hex = ContextCompat.getColor(getBaseContext(), App.GetColorForLogType(l.type));
                    // System.out.println("Colorizing: "+Integer.toHexString(hex));
                    t.setTextColor(hex);
                    alternateLogMessageColor = !alternateLogMessageColor;
                    int colorID = alternateLogMessageColor? R.color.logColor1 : R.color.logColor2;
                    int bgHex = ContextCompat.getColor(getBaseContext(), colorID);
                    t.setBackgroundColor(bgHex);
//            t.setBackgroundResource(R.drawable.chatlogbg);
                    t.setFocusable(true); // Focusable.
                    t.setFocusableInTouchMode(true);
                    viewToAdd = t;
                    break;
            }
            if (viewToAdd != null)
                vg.addView(viewToAdd);
        }

    }

}
