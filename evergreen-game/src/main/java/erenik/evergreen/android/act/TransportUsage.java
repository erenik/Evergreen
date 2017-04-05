package erenik.evergreen.android.act;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.preference.Preference;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.ValueDependentColor;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import org.w3c.dom.Text;

import java.io.Serializable;
import java.sql.Array;
import java.text.NumberFormat;
import erenik.util.EList;
import java.util.Queue;

import erenik.evergreen.R;
import erenik.evergreen.android.App;
import erenik.evergreen.common.player.Transport;
import erenik.weka.transport.SensingFrame;
import erenik.weka.transport.TransportDetectionService;
import erenik.weka.transport.TransportOccurrence;
import erenik.weka.transport.TransportType;

/**
 * Created by Emil on 2017-03-08.
 */

public class TransportUsage  extends EvergreenActivity {

    Handler iterationHandler = new Handler();
    int updateDelayMs = 1000; // 20 fps update for the graphs when looking at them?
    GraphView graphTransportDurations = null;

    long secondsToDisplayInGraph = 300;
    BroadcastReceiver broadcastReceiver = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transport_usage);

        findViewById(R.id.buttonBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Spinner spinner = (Spinner) findViewById(R.id.spinnerTransportUsageDuration);
        SetSpinnerArray(spinner, R.array.transportDisplayPeriods);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                TextView tv = (TextView)view;
                if (tv != null) {
                    String text = (String) tv.getText();
                    System.out.println("Selected: " + tv.getText());
                    long unitInSeconds = 1;
                    if (text.contains("minute")) unitInSeconds = 60;
                    if (text.contains("hour")) unitInSeconds = 3600;
                    if (text.contains("day")) unitInSeconds = 86400;
                    if (text.contains("week")) unitInSeconds = 604800;
                    String p = text.split(" ")[0];
                    int durationUnits = Integer.parseInt(p);
                    long totalTimeSeconds = durationUnits * unitInSeconds;
                    System.out.println("New period of observation: "+totalTimeSeconds+" seconds");
                    secondsToDisplayInGraph = totalTimeSeconds;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        final SharedPreferences sp = App.GetPreferences();
        final String PREF_HISTORY_SET = "HistorySet";
        final String PREF_SLEEP_SESSIONS = "SleepSessions";
        int historySetSize = sp.getInt(PREF_HISTORY_SET, 12);
        int sleepSessions = sp.getInt(PREF_SLEEP_SESSIONS, 12);

        spinner = (Spinner) findViewById(R.id.spinnerHistorySetSize);
        SetSpinnerArray(spinner, R.array.historySetSizes);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                TextView tv = (TextView)view;
                if (tv == null) // Otherwise causes FATAL EXCEPTION when returning to the view after long periods of time / yeah.
                    return;
                int val = Integer.parseInt((String) tv.getText());
                SharedPreferences.Editor e = sp.edit();
                e.putInt(PREF_HISTORY_SET, val);
                e.commit();
                TransportDetectionService.GetInstance().SetHistorySetSize(val);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        String[] arr = getBaseContext().getResources().getStringArray(R.array.historySetSizes);
        for (int i = 0; i < arr.length; ++i){
            if (arr[i].equals(historySetSize+""))
                spinner.setSelection(i);
        }

        spinner = (Spinner) findViewById(R.id.spinnerSleepSessions);
        SetSpinnerArray(spinner, R.array.sleepSessions);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                TextView tv = (TextView)view;
                int val = Integer.parseInt((String) tv.getText());
                SharedPreferences.Editor e = sp.edit();
                e.putInt(PREF_SLEEP_SESSIONS, val);
                e.commit();
                TransportDetectionService.GetInstance().SetSleepSessions(val);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        arr = getBaseContext().getResources().getStringArray(R.array.sleepSessions);
        for (int i = 0; i < arr.length; ++i){
            if (arr[i].equals(sleepSessions+""))
                spinner.setSelection(i);
        }

        graphTransportDurations = (GraphView) findViewById(R.id.graphTransportDurations);

        /// Query next sampling.
        QueueUpdate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AddBroadcastReceiver(); // Add broadcast receiver for all upcoming updates.
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;

    }

    private void QueueUpdate() {
        iterationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Iterate();
            }
        }, updateDelayMs);
    }

    void Iterate(){
        QueueUpdate(); // Queue update before any return statements later.
        final TransportDetectionService service = TransportDetectionService.GetInstance();
        TextView tv = (TextView) findViewById(R.id.textView_sensorState);
        if (service == null){
            tv.setText("Service inactive");
            return;
        }
        else {
            tv.setText(service.GetStatus());
        }
        tv = (TextView) findViewById(R.id.textView_currentTransport);
        tv.setText(service.CurrentTransport());

        // Request last x sensing frames.
        Intent intent = new Intent(getBaseContext(), TransportDetectionService.class);
        intent.putExtra(TransportDetectionService.REQUEST_TYPE, TransportDetectionService.GET_LAST_SENSING_FRAMES);
        intent.putExtra(TransportDetectionService.NUM_FRAMES, 12 * 3);
        startService(intent);

        // Request last x sensing frames.
        intent = new Intent(getBaseContext(), TransportDetectionService.class);
        intent.putExtra(TransportDetectionService.REQUEST_TYPE, TransportDetectionService.GET_TOTAL_STATS_FOR_DATA_SECONDS);
        intent.putExtra(TransportDetectionService.DATA_SECONDS, secondsToDisplayInGraph);
        startService(intent);
    }

    private void AddBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_ATTACH_DATA); // Intent.ACTION_ATTACH_DATA
        if (broadcastReceiver == null)
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    //              System.out.println("BroadcastReceiver onReceive: "+intent);
                    int request = intent.getIntExtra(TransportDetectionService.REQUEST_TYPE, -1);
                    Serializable data = intent.getSerializableExtra(TransportDetectionService.SERIALIZABLE_DATA);
    //                System.out.println("Req/Res: "+request+" data: "+data);
                    switch (request){
                        case -1: break;
                        case TransportDetectionService.GET_LAST_SENSING_FRAMES:
                            EList<SensingFrame> sfs = (EList<SensingFrame>) data;
                            UpdateLog(sfs);
                            break;
                        case TransportDetectionService.GET_TOTAL_STATS_FOR_DATA_SECONDS:
                            EList<TransportOccurrence> to = (EList<TransportOccurrence>) data;
                            UpdateGraphWithStats(to);
                            break;
                    }
                }
            };
        getBaseContext().registerReceiver(broadcastReceiver, intentFilter);
    }

    private void UpdateGraphWithStats(final EList<TransportOccurrence> stats) {
        /// Update it with total time for now? or past 5 mins?
        graphTransportDurations.removeAllSeries(); // Remove old data from graph.
//        final EList<TransportOccurrence> stats = service.GetTotalStatsForDataSeconds(secondsToDisplayInGraph);
        System.out.println("Update graph with stats: "+stats.size());
        long max = 10;
        for (int i = 0; i < stats.size(); ++i){
            final BarGraphSeries<DataPoint> series = new BarGraphSeries<>();
            TransportOccurrence to = stats.get(i);
            if (to.DurationSeconds() <= 0)
                continue;
//            System.out.println("To: "+to.transport+" "+to.durationMs);
            // Add it?
            long seconds = to.DurationSeconds();
            if (seconds > max)
                max = seconds;
            series.appendData(new DataPoint(to.transport.ordinal(), seconds), false, 10);
            // legend
            series.setTitle(to.transport.name());
            series.setSpacing(50);
            series.setDrawValuesOnTop(true);
            series.setValuesOnTopColor(Color.RED);
            series.setValueDependentColor(new ValueDependentColor<DataPoint>() {
                @Override
                public int get(DataPoint data) {
                    TransportType trans = TransportType.GetFromString(TransportDetectionService.GetTransportString((int)data.getX()));
                    return Color.rgb(trans.r, trans.g, trans.b);
                }
            });
            graphTransportDurations.addSeries(series);
        }
        graphTransportDurations.getLegendRenderer().setVisible(true);
        graphTransportDurations.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);

        Viewport vp = graphTransportDurations.getViewport();
        vp.setYAxisBoundsManual(true);
        vp.setMaxY((int) max * 1.1f);
        vp.setMinY(0);
        vp.setXAxisBoundsManual(true);
        vp.setMaxX(10);
        vp.setMinX(-2);

        graphTransportDurations.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    // show normal x values
                    return super.formatLabel(value, isValueX);
                } else {
                    // show currency for y values
                    for (int i = 0; i < stats.size(); ++i){
                        if (value == stats.get(i).DurationSeconds()){
                            return stats.get(i).transport.name()+", "+super.formatLabel(value, isValueX)+"s";
                        }
                    }
                    return super.formatLabel(value, isValueX)+"s";
                }
            }
        });
    }

    void UpdateLog(EList<SensingFrame> sfs ){
        ViewGroup vg = (ViewGroup) findViewById(R.id.layoutTransportLogs);
        vg.removeAllViews();
//        = service.GetLastSensingFrames(12 * 3); // The past 3 minutes?
        for (int i = 0; i < sfs.size(); ++i){
            TextView tv2 = new TextView(getBaseContext());
            SensingFrame sf = sfs.get(i);
            long ms = sf.StartTimeSystemMs();
            long seconds = ms / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;
            long hour = hours % 24,
                    minute = minutes % 60,
                    second = seconds % 60;
            tv2.setText(hour+":"+minute+":"+second+" "+sf.shortString());
            vg.addView(tv2);
        }
    }
}
