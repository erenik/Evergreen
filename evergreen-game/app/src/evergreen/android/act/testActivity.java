package evergreen.android.act;

import android.app.Activity;
import android.os.Bundle;

import evergreen.R;

public class testActivity extends EvergreenActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
    }
}
