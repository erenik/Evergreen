package erenik.evergreen.android.act;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import erenik.evergreen.android.App;
import erenik.evergreen.common.Player;
import erenik.evergreen.R;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class IntroScreen extends EvergreenActivity
{
    Player player = App.GetPlayer();

    int clicked = 0;
    private final View.OnClickListener buttonClicked = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            System.out.println("ontouchListener view: "+v.toString());
            ImageView iv = (ImageView) findViewById(R.id.imageViewIntroOverlay);
            int id = v.getId();
            if (id == R.id.nextButton)
            {
                // Update text?
                TextView textView = (TextView) findViewById(R.id.fullscreen_text);
                ++clicked;
                switch(clicked) {
                    case 1:
                        textView.setText(getString(R.string.introText1));
                        break;
                    case 2:
                        textView.setText(getString(R.string.introText2));
                        // Glass window!
//                        Drawable d = ContextCompat.getDrawable(getBaseContext(), R.drawable.intro_glass_crash);
                        iv.setBackgroundResource(R.drawable.intro_glass_crash);
                        break;
                    case 3:
                        // Remove it?
//                        iv.setBackgroundColor(0x00);
                        ((ImageView) findViewById(R.id.imageViewIntroOverlay2)).setBackgroundColor(0x88000000);
                        textView.setText(getString(R.string.introText3));
                        break;
                    default: // Load next view.
                    {
                        Save();
                        System.out.println("Starting new activity");
                        Intent i = new Intent(getBaseContext(), CreateCharacter.class);
                        startActivity(i);
                        break;
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_screen);
        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.nextButton).setOnClickListener(buttonClicked);
    }
}
