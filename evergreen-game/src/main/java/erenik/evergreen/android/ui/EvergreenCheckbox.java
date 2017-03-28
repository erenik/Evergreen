package erenik.evergreen.android.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.CheckBox;

/**
 * Created by Emil on 2016-10-31.
 */
public class EvergreenCheckbox extends CheckBox{
    public EvergreenCheckbox(Context context) {
        super(context);
    }
    public EvergreenCheckbox(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }
    public EvergreenCheckbox(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setButtonDrawable(@Nullable Drawable drawable)
    {
        // Take the drawable, re-scale it?
        drawable.setBounds(0, 40, 40, 0);
        super.setButtonDrawable(drawable);
        // Override minimum height that may have been set.
        setMinHeight(0);
        //                 setMinHeight(drawable.getIntrinsicHeight());
    }

}
