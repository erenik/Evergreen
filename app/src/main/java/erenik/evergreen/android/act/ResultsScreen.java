package erenik.evergreen.android.act;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import erenik.evergreen.R;
import erenik.evergreen.android.App;
import erenik.evergreen.common.Player;
import erenik.evergreen.common.logging.Log;

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
            boolean centered = false;
            int textSize = 18;
            int drawableLeftSide = 0;
            Log l = p.log.get(i);
            if (l.displayedToEndUser)
                continue;
            l.displayedToEndUser = true;
            TextView viewToAdd = null;
            switch (l.TextID()){
                case transportOfTheDay:
                    String trans = l.Args().get(0);
                    if (trans.contains("Bus")) drawableLeftSide = R.drawable.ri_bus;
                    if (trans.contains("Foot") || trans.contains("Walking")) drawableLeftSide = R.drawable.ri_foot;
                    if (trans.contains("Train")) drawableLeftSide = R.drawable.ri_train;
                    if (trans.contains("Car")) drawableLeftSide = R.drawable.ri_car;
                    if (trans.contains("Bike")) drawableLeftSide = R.drawable.ri_bike;
//                    if (trans.contains("Bus")) drawableLeftSide = R.drawable.ri_bus;
                    break;
                case recoverRecovered: drawableLeftSide = R.drawable.ri_recover; break;
                case monsterKnockedOutPlayer: drawableLeftSide = R.drawable.ri_defeated; break;
                case studiesEXP: // Same exp sign for battles and studying.
                case expGained: drawableLeftSide = R.drawable.ri_exp; break;
                case shelterAttacked: drawableLeftSide = R.drawable.ri_shelter_attacked; break;
                case newDayPlayerTurnPlayed: centered = true; drawableLeftSide = R.drawable.ri_day; textSize = 26; break; // Title!
                case playerMonsterAttack: drawableLeftSide = R.drawable.ri_attack; break;
                case monsterPlayerAttack: drawableLeftSide = R.drawable.ri_attacked; break;
                case encounterSurvived: // Encounter survived and monster vanquished get to share the same icon for now?
                case playerVanquishedMonster: drawableLeftSide = R.drawable.ri_monster_vanquished; break;
                case encounterNumMonsters: drawableLeftSide = R.drawable.ri_monster_encounter; break;
                case scoutingSuccess: drawableLeftSide = R.drawable.ri_scout; break;
                case reduceEmissionsSuccessful:
                case reduceEmissionsMostlySuccessful:
                case reduceEmissionsNotSoSuccessful: drawableLeftSide = R.drawable.ri_emissions; break;
                case reduceEmissionsFailed: drawableLeftSide = R.drawable.ri_emissions_failed; break;
                case foragingFood: drawableLeftSide = R.drawable.ri_forage; break;
                case gatherMaterials: drawableLeftSide = R.drawable.ri_gathmats; break;
                case buildDefensesProgress: drawableLeftSide = R.drawable.ri_build_defenses; break;
                case defensesReachedLevel: drawableLeftSide = R.drawable.ri_defenses_level_up; break;
                default: // Nothing special.
                    break;
            }
            switch (l.TextID()){
                /// Messages to be displayed as they are.
                default:
                    // Print it as they are.
                    viewToAdd = GetViewForLogMessage(l);
                    break;
                case monsterPlayerAttackMiss: // Those messages to skip (e.g. too much to show).

                    break;
                case debug:
                case undefined:
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
            LinearLayout ll = new LinearLayout(getBaseContext());
            LinearLayout.LayoutParams lllp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lllp.leftMargin = 10;
            lllp.rightMargin = 10;
            if (centered)
                ll.setGravity(Gravity.CENTER);
            vg.addView(ll);
            // Add contents.
            if (drawableLeftSide > 0){
                ImageView iv = new ImageView(getBaseContext());
                int pixelSize =  App.GetScreenSize().x / 7;
                lllp = new LinearLayout.LayoutParams(pixelSize,pixelSize);
                lllp.setMargins(10, 0, 10, 0);
//                lllp.width = ;
  //              lllp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                lllp.gravity = Gravity.CENTER;
                iv.setLayoutParams(lllp);
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER); // Scale to fit?
                // Set max width/height.
                iv.setImageResource(drawableLeftSide);
                iv.setBackgroundColor(0xFFFFFFFF);
                ll.addView(iv);
            }
            if (viewToAdd != null) {
                lllp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lllp.setMargins(10, 0, 10, 0);
                viewToAdd.setLayoutParams(lllp);
                viewToAdd.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
                ll.addView(viewToAdd);
            }
        }

    }

}