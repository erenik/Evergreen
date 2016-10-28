package erenik.seriousgames.evergreen;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainScreen extends AppCompatActivity
{
    Player player = Player.getSingleton();
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnClickListener selectActionSkill = new View.OnClickListener() {

        @Override
        public void onClick(View view)
        {
            findViewById(R.id.buttonChooseActiveAction).setOnClickListener(selectActionSkill);
            findViewById(R.id.buttonChooseAction).setOnClickListener(selectActionSkill);
            findViewById(R.id.buttonChooseSkill).setOnClickListener(selectActionSkill);

            int selection = -1;
            switch(view.getId())
            {
                case R.id.buttonChooseSkill: selection = SelectActivity.SELECT_SKILL; break;
                case R.id.buttonChooseAction: selection = SelectActivity.SELECT_DAILY_ACTION; break;
                case R.id.buttonChooseActiveAction: selection = SelectActivity.SELECT_ACTIVE_ACTION; break;
            }
            Intent i = new Intent(getBaseContext(), SelectActivity.class);
            i.putExtra("Type", selection);
            startActivityForResult(i, selection);
        }
    };
    View.OnClickListener nextDay = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                NextDay();
            } catch (NotAliveException e) {
                e.printStackTrace();
            }
        }
    };

    /// o-o
    void NextDay() throws NotAliveException {
        player.NextDay();
        // Finally, update gui.
        UpdateGUI();
    }
    /// Main init function
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        App.mainScreenActivity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        mVisible = true;
        // Load stuff as needed.
        player.LoadLocally();
        player.SaveLocally(); // Save copy?
        /// Update GUI.
        UpdateGUI();


        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.buttonChooseActiveAction).setOnClickListener(selectActionSkill);
        findViewById(R.id.buttonChooseAction).setOnClickListener(selectActionSkill);
        findViewById(R.id.buttonChooseSkill).setOnClickListener(selectActionSkill);
        findViewById(R.id.nextDay).setOnClickListener(nextDay);

    }

    void UpdateGUI()
    {
        SetText(R.id.textViewHP, player.GetInt(Stat.HP)+"/"+player.GetInt(Stat.MAX_HP));
        SetText(R.id.textViewFood, player.GetInt(Stat.FOOD)+"");
        SetText(R.id.textViewMaterials, player.GetInt(Stat.MATERIALS)+"");
        SetText(R.id.textViewAttack, player.Attack()+"");
        SetText(R.id.textViewDefense, player.Defense()+"");
        SetText(R.id.textViewEmissions, player.GetInt(Stat.EMISSIONS) + "");

        UpdateActiveActionButton();
        UpdateDailyActionButton();
        UpdateSkillButton();
        // Update log.
        ViewGroup v = (ViewGroup) findViewById(R.id.layoutLog);
        // Remove children.
        v.removeAllViews();
        // Add new ones?
        int numDisplay = player.log.size();
        final int MAX_DISPLAY = 10;
        numDisplay = numDisplay > MAX_DISPLAY ? MAX_DISPLAY : numDisplay;
        for (int i = player.log.size() - numDisplay; i < player.log.size(); ++i)
        {
            Log l = player.log.get(i);
            String s = l.text;
            TextView t = new TextView(getBaseContext());
            t.setText(s);
            int hex = ContextCompat.getColor(getBaseContext(), l.type.GetResourceColor());
            System.out.println("Colorizing: "+Integer.toHexString(hex));
            t.setTextColor(hex);
            v.addView(t);
            t.setFocusable(true); // Focusable.
            t.setFocusableInTouchMode(true);
            t.requestFocus(); // Request focus, make visible?
        }
        // Scroll down?
//        ScrollView sv = (ScrollView) findViewById(R.id.scrollViewLog);
  //      sv.fullScroll(ScrollView.FOCUS_DOWN);
    }

    void SetText(int viewID, String text)
    {
        ((TextView) findViewById(viewID)).setText(text);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
       // delayedHide(100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        int type = requestCode;
        System.out.println("onActivityResult called, req: "+requestCode+" code: " + resultCode);
        if (resultCode < 0)
            return;
        switch(type)
        {
            case SelectActivity.SELECT_ACTIVE_ACTION:
                player.activeAction = resultCode;
                UpdateActiveActionButton();
                break;
            case SelectActivity.SELECT_DAILY_ACTION:
                player.dailyAction = resultCode;
                UpdateDailyActionButton();
                break;
            case SelectActivity.SELECT_SKILL:
                player.skill = resultCode;
                UpdateSkillButton();
                break;
        }
        player.SaveLocally(); // Save copy?
    }
    private void UpdateActiveActionButton() {
        int idBtn = R.id.buttonChooseActiveAction;
        TextView tv = (TextView) findViewById(idBtn);
        if (player.activeAction >= 0)
            tv.setText("Change Active Action: " + getResources().getStringArray(R.array.activeActions)[player.activeAction]);
        else
            tv.setText("Active action");
    }
    private void UpdateDailyActionButton() {
        TextView tv = ((TextView) findViewById(R.id.buttonChooseAction));
        if (player.dailyAction >= 0)
            tv.setText("Change Action: " + getResources().getStringArray(R.array.dailyActions)[player.dailyAction]);
        else
            tv.setText(R.string.chooseAction);
    }
    private void UpdateSkillButton()
    {
        TextView tv = ((TextView) findViewById(R.id.buttonChooseSkill));
        if (player.skill >= 0)
            tv.setText("Change Skill: "+getResources().getStringArray(R.array.skills)[player.skill]);
        else
            tv.setText(R.string.chooseSkill);
    }
    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
