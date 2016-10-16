package erenik.seriousgames.evergreen;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;


public class SelectActivity extends AppCompatActivity
{
    static final int SELECT_ACTIVE_ACTION = 2;
    static final int SELECT_DAILY_ACTION = 0;
    static final int SELECT_SKILL = 1;

    private final View.OnClickListener confirm = new View.OnClickListener() {
        @Override
        public void onClick(View v)
        {
            // Finalize activity, return to the main screen?
            Spinner spinner = (Spinner) findViewById(R.id.spinnerSelect);
            int selectedItem = spinner.getSelectedItemPosition();
            System.out.println("Selecting: "+selectedItem);
            // Return it?
            setResult(selectedItem);
            finish();
        }
    };
    private final View.OnClickListener cancel = new View.OnClickListener() {
        @Override
        public void onClick(View v)
        {
            // Return it?
            System.out.println("Canceling");
            setResult(-1);
            finish();
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);

        int type = getIntent().getIntExtra("Type", 0);

        int arrayId = -1;
        switch(type)
        {
            case SELECT_DAILY_ACTION: arrayId = R.array.dailyActions; break;
            case SELECT_ACTIVE_ACTION: arrayId = R.array.activeActions; break;
            case SELECT_SKILL: arrayId = R.array.skills; break;
        }
        /// Populate the spinner!
        Spinner spinner = (Spinner) findViewById(R.id.spinnerSelect);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                arrayId, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        findViewById(R.id.buttonConfirm).setOnClickListener(confirm);
        findViewById(R.id.buttonCancel).setOnClickListener(cancel);

    }
}
