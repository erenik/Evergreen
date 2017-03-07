package erenik.evergreen.android.act;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import erenik.evergreen.R;
import erenik.evergreen.android.App;
import erenik.evergreen.android.auth.NetworkTask;
import erenik.evergreen.android.auth.NetworkTaskListener;
import erenik.evergreen.common.Invention.Invention;
import erenik.evergreen.common.Player;
import erenik.evergreen.common.logging.LogTextID;
import erenik.evergreen.common.player.Stat;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class CreateCharacter extends EvergreenActivity implements LoaderCallbacks<Cursor> {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private NetworkTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    int avatarID = 0;
    String bonus = "";
    private String difficulty = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_character);

        Spinner spinnerStartingBonus = (Spinner) findViewById(R.id.spinnerStartingBonus);
        spinnerStartingBonus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView b = (TextView) view;
                System.out.println("Bonus selected: "+b.getText());
                bonus = (String) b.getText();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        // Populate spinner for starting bonus
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.starting_bonus, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStartingBonus.setAdapter(adapter);

        SetupDifficultySpinner();

        ImageButton ib = (ImageButton) findViewById(R.id.buttonSelectAvatar);
        ib.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), SelectAvatar.class);
                startActivityForResult(intent, SELECT_AVATAR);
                return;
            }
        });

        /// If local game, hide the e-mail and password annoying shits.
        if (App.isLocalGame) {
            ViewGroup vg = (ViewGroup) findViewById(R.id.layoutEmailPassword);
            vg.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
            vg.setVisibility(View.INVISIBLE);
        }
        // Online-specific actions.
        else {
            // Set up the login form.
            mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
            populateAutoComplete();

            mPasswordView = (EditText) findViewById(R.id.password);
            mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                    if (id == R.id.login || id == EditorInfo.IME_NULL) {
                        registerCharacter();
                        return true;
                    }
                    return false;
                }
            });
        }

        findViewById(R.id.buttonGenerateName).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText et = (EditText) findViewById(R.id.textEditName);
                String[] firstNames = getResources().getStringArray(R.array.firstNames);
                Random nameRand = new Random(System.nanoTime());
                String[] lastNames = getResources().getStringArray(R.array.surnames);
                String fullName = firstNames[nameRand.nextInt(firstNames.length)%firstNames.length] + " " +
                        lastNames[nameRand.nextInt(lastNames.length) % lastNames.length];
                et.setText(fullName);
            }
        });


        Button registerButton = (Button) findViewById(R.id.registerButton);
        registerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                registerCharacter();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        /// Request games list, should maybe fetch it even earlier..?
        GetGamesList();
    }

    private void SetupDifficultySpinner() {
        Spinner spinnerDifficulty = (Spinner) findViewById(R.id.spinnerDifficulty);
        spinnerDifficulty.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView b = (TextView) view;
                System.out.println("Difficulty selected: "+b.getText());
                difficulty = (String) b.getText();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        // Populate spinner for starting bonus
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.difficulty, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(adapter);
    }

    int SELECT_AVATAR = 314;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == SELECT_AVATAR) {
            if (resultCode > 0) {
                avatarID = resultCode;
                // Update the profile pic?
                ImageButton ib = (ImageButton) findViewById(R.id.buttonSelectAvatar);
                ib.setImageResource(App.GetDrawableForAvatarID(avatarID));
            }
        }
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }

    // Attempts to sign in or register the account specified by the login form. * If there are form errors (invalid email, missing fields, etc.), the * errors are presented and no actual login attempt is made.
    private void registerCharacter() {
        EditText et = (EditText) findViewById(R.id.textEditName);
        if (et.getText().toString().length() < 1) {
            Toast("Please give your character at least 1 letter... Poor thing.");
            return;
        }
        Player player = new Player();
        player.SetName(et.getText().toString());
        player.Set(Stat.Avatar, avatarID);
        String[] difficulties = getResources().getStringArray(R.array.difficulty);
        for (int i = 0; i < difficulties.length; ++i) {
            if (difficulty.equals(difficulties[i])) {
                player.Set(Stat.Difficulty, i);
                System.out.println("Difficulty set to: "+i);
            }
        }
        player.AssignResourcesBasedOnDifficulty();
        // Check the requested starting item.
        if (bonus.equals("Food supply")) {
            player.LogInfo(LogTextID.startingBonusFood, ""+20);
            player.Adjust(Stat.FOOD, 20);
        } else if (bonus.equals("Materials supply")) {
            player.LogInfo(LogTextID.startingBonusMaterials, ""+10);
            player.Adjust(Stat.MATERIALS, 10);
        } else if (bonus.equals("A weapon")) {
            Invention randomWeapon = Invention.RandomWeapon(player.BonusFromDifficulty()/3);
            player.LogInfo(LogTextID.startingBonusItem, randomWeapon.name);
            player.inventory.add(randomWeapon);
            player.Equip(randomWeapon); // Equip it from start?
        } else if (bonus.equals("A body armor")) {
            Invention armor = Invention.RandomArmor(player.BonusFromDifficulty()/3);
            player.LogInfo(LogTextID.startingBonusItem, armor.name);
            player.inventory.add(armor);
            player.Equip(armor); // Equip it from start?
        } else if (bonus.equals("A tool")) {
            Invention tool = Invention.RandomTool(player.BonusFromDifficulty()/3);
            player.LogInfo(LogTextID.startingBonusItem, tool.name);
            player.inventory.add(tool);
            player.Equip(tool);
        } else if (bonus.equals("2 inventions")) {
            player.LogInfo(LogTextID.startingBonusInventions, ""+2);
            player.inventions.add(Invention.Random(player.BonusFromDifficulty()/3));
            player.inventions.add(Invention.Random(player.BonusFromDifficulty()/3));
        }
        if (App.isLocalGame) {
            /// Just make a player using the chosen config on this screen and go to the main menu without waiting.
            App.RegisterPlayer(player);
            App.MakeActivePlayer(player);
            GoToMainScreen();
            finish(); // Finish this activity.
            return;
        }

        if (mAuthTask != null)
            return;
        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first            // form field with an error.
            focusView.requestFocus();
            return;
        }

        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        showProgress(true);
        mAuthTask = new NetworkTask(email, password);
        mAuthTask.AddListener(new NetworkTaskListener(){
            @Override
            public void OnTaskCompleted(boolean success) {
                mAuthTask = null;
                showProgress(false);
                if (success) {
                    // finish();
                    // Go to main screen? Show success window first?
                    GoToMainScreen();
                } else {
                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                    mPasswordView.requestFocus();
                }
            }

            @Override
            public void OnTaskCanceled() {
                mAuthTask = null;
                showProgress(false);

            }
        });
        mAuthTask.execute((Void) null);
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
            // Retrieve data rows for the device user's 'profile' contact.
            Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }
        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(CreateCharacter.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }


    private interface ProfileQuery
    {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }
}

