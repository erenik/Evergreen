package erenik.seriousgames.evergreen;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.Display;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Emil on 2016-10-31.
 */
public class EvergreenButton extends Button
{
    public EvergreenButton(Context context) {
        super(context);
        // Just set background color and text color automagically.
        setBackgroundColor(BackgroundColor(context));
        setTextColor(ContextCompat.getColor(context, R.color.mainButtonTextColor));
    }
    LinearLayout.LayoutParams layoutParams;
    public static int BackgroundColor(Context context)
    {
        return ContextCompat.getColor(context, R.color.mainButtonBackground);
    }
}
