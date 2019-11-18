package evergreen.android.act;

import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;

import evergreen.R;
import evergreen.android.App;
import evergreen.util.EList;
import evergreen.util.Printer;

/**
 * Created by Emil on 2017-02-23.
 */

public class SelectAvatar  extends EvergreenActivity {

    class AvatarButton {
        int index;
        ImageButton button;
    }
    EList<AvatarButton> buttons = new EList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_avatar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // fill it up.
        LinearLayout ll = (LinearLayout) findViewById(R.id.layoutAvatars);
        if (ll == null)
            return;
        ll.removeAllViews();
        LinearLayout llRow = null;
        int imagesInRow = 0;
        int imagesPerRow = 2;
        buttons.clear();
        for (int i = 0; i < App.AvatarIDs(); ++i){
            Point p = App.GetScreenSize();
            int height = p.y / 3;
            int width = (p.x - 30) / imagesPerRow;
            Printer.out("height: "+height+" width: "+width);

            if (llRow == null) {
                imagesInRow = 0;
                llRow = new LinearLayout(getBaseContext()); // Width, height.
                llRow.setLayoutParams(new LinearLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, height));
                ll.addView(llRow);
            }

            ImageButton ib = new ImageButton(getBaseContext());
            AvatarButton ab = new AvatarButton();
            ab.button = ib;
            ab.index = i;
            buttons.add(ab);
            int padding = 35;
            ib.setPadding(padding,padding,padding,padding);
            LinearLayout.LayoutParams lllp = new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT);
//            int margins = 14;
            //          lllp.setMargins(margins,margins,margins,margins);
            ib.setLayoutParams(lllp);
            ib.setScaleType(ImageView.ScaleType.FIT_CENTER);
            ib.setImageResource(App.GetDrawableForAvatarID(i));
            ib.setBackgroundResource(R.drawable.icon);
//            ib.setBackgroundResource(R.color.transparent);
            llRow.addView(ib);
            ib.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Return
                    ImageButton ib = (ImageButton) v;
                    Drawable d = ib.getDrawable();
                    Printer.out("Drawable: "+d.toString());
                    int result = -1;
                    for (int i = 0; i < buttons.size(); ++i){
                        AvatarButton ab = buttons.get(i);
                        if (v == ab.button)
                            result = ab.index;
                    }
                    Printer.out("Result: "+result);
                    setResult(result);
                    finish(); // Go back, yo.
                }
            });
            ++imagesInRow;
            if (imagesInRow >= imagesPerRow)
                llRow = null; // New row.
        }
    }
}