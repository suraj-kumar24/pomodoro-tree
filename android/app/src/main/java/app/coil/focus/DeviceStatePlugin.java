package app.coil.focus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Tells the web layer what happened while Coil was in the background, so a session can be
 * paused for a call, kept running when the phone was only locked, or ended after 60 seconds away.
 * No permissions needed: call state comes from the audio mode, locking from SCREEN_OFF.
 */
@CapacitorPlugin(name = "DeviceState")
public class DeviceStatePlugin extends Plugin {

    // Kept in SharedPreferences too, so a locked phone still counts if Android kills the app meanwhile.
    private static final String PREFS = "coil_device_state";
    private long stoppedAt = 0;
    private boolean callAtStop = false;
    private volatile long lastScreenOff = 0;
    private BroadcastReceiver screenReceiver;

    private SharedPreferences prefs() {
        return getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private void save() {
        prefs().edit().putLong("stoppedAt", stoppedAt).putBoolean("callAtStop", callAtStop).putLong("lastScreenOff", lastScreenOff).apply();
    }

    @Override
    public void load() {
        SharedPreferences p = prefs();
        stoppedAt = p.getLong("stoppedAt", 0);
        callAtStop = p.getBoolean("callAtStop", false);
        lastScreenOff = p.getLong("lastScreenOff", 0);
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                    lastScreenOff = System.currentTimeMillis();
                    save();
                }
            }
        };
        // System broadcasts still arrive with RECEIVER_NOT_EXPORTED.
        ContextCompat.registerReceiver(
            getContext(),
            screenReceiver,
            new IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED
        );
        // The app scales its own type from the system font size (see info()), so stop the WebView doing it too.
        getActivity().runOnUiThread(() -> getBridge().getWebView().getSettings().setTextZoom(100));
    }

    @Override
    protected void handleOnStop() {
        stoppedAt = System.currentTimeMillis();
        callAtStop = inCall();
        PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isInteractive()) {
            lastScreenOff = Math.max(lastScreenOff, stoppedAt);
        }
        save();
    }

    @Override
    protected void handleOnDestroy() {
        if (screenReceiver != null) {
            try {
                getContext().unregisterReceiver(screenReceiver);
            } catch (IllegalArgumentException ignored) {}
            screenReceiver = null;
        }
    }

    private boolean inCall() {
        AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return false;
        int mode = am.getMode();
        return mode == AudioManager.MODE_RINGTONE || mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION;
    }

    @PluginMethod
    public void awayInfo(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("call", callAtStop || inCall());
        // SCREEN_OFF can be delivered just before or after onStop.
        ret.put("screenOff", stoppedAt > 0 && lastScreenOff >= stoppedAt - 2000);
        call.resolve(ret);
    }

    @PluginMethod
    public void info(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("fontScale", getContext().getResources().getConfiguration().fontScale);
        float anim = Settings.Global.getFloat(getContext().getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        ret.put("animationsOff", anim == 0f);
        call.resolve(ret);
    }

    @PluginMethod
    public void openNotificationSettings(PluginCall call) {
        String pkg = getContext().getPackageName();
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg);
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
        call.resolve();
    }
}
