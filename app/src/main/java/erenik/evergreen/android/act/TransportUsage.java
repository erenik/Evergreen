package erenik.evergreen.android.act;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
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

import java.sql.Array;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Queue;

import erenik.evergreen.R;
import erenik.evergreen.common.player.Transport;
import erenik.weka.transport.SensingFrame;
import erenik.weka.transport.TransportDetectionService;
import erenik.weka.transport.TransportOccurrence;

/**
 * Created by Emil on 2017-03-08.
 */

public class TransportUsage  extends EvergreenActivity {

    Handler iterationHandler = new Handler();
    int updateDelayMs = 1000; // 20 fps update for the graphs when looking at them?
    GraphView graphTransportDurations = null;

    long secondsToDisplayInGraph = 300;

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

        spinner = (Spinner) findViewById(R.id.spinnerHistorySetSize);
        SetSpinnerArray(spinner, R.array.historySetSizes);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                TextView tv = (TextView)view;
                TransportDetectionService.GetInstance().SetHistorySetSize(Integer.parseInt((String) tv.getText()));
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        spinner = (Spinner) findViewById(R.id.spinnerSleepSessions);
        SetSpinnerArray(spinner, R.array.sleepSessions);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                TextView tv = (TextView)view;
                TransportDetectionService.GetInstance().SetSleepSessions(Integer.parseInt((String) tv.getText()));
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        graphTransportDurations = (GraphView) findViewById(R.id.graphTransportDurations);

        /// Query next sampling.
        QueueUpdate();

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

        // Update the log - only when button is pressed?
        ViewGroup vg = (ViewGroup) findViewById(R.id.layoutTransportLogs);
        vg.removeAllViews();
        ArrayList<SensingFrame> sfs = service.GetLastSensingFrames(12 * 3); // The past 3 minutes?
        for (int i = 0; i < sfs.size(); ++i){
            TextView tv2 = new TextView(getBaseContext());
            SensingFrame sf = sfs.get(i);
            long ms = sf.StartTimeMs();
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


        /// Update it with total time for now? or past 5 mins?
        graphTransportDurations.removeAllSeries(); // Remove old data from graph.
        final ArrayList<TransportOccurrence> stats = service.GetTotalStatsForDataSeconds(secondsToDisplayInGraph);

        int max = 10;
        for (int i = 0; i < stats.size(); ++i){
            final BarGraphSeries<DataPoint> series = new BarGraphSeries<>();
            TransportOccurrence to = stats.get(i);
            if (to.durationMs <= 0)
                continue;
//            System.out.println("To: "+to.transport+" "+to.durationMs);
            // Add it?
            int seconds = (int) (to.durationMs / 1000);
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
                    Transport trans = Transport.GetFromString(service.GetTransportString((int)data.getX()));
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
                        if (value == stats.get(i).durationMs / 1000){
                            return stats.get(i).transport.name()+", "+super.formatLabel(value, isValueX)+"s";
                        }
                    }
                    return super.formatLabel(value, isValueX)+"s";
                }
            }
        });
    }
}
