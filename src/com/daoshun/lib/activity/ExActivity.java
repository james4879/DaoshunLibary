package com.daoshun.lib.activity;

import java.lang.reflect.Method;
import java.util.HashMap;

import android.app.Activity;
import android.app.LocalActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ExActivity extends Activity {

    private static final String TAG = ExActivity.class.getName();

    private static final String STATES_KEY = "android:states";
    static final String PARENT_NON_CONFIG_INSTANCE_KEY = "android:parent_non_config_instance";

    protected LocalActivityManager mLocalActivityManager;

    public void initActivityManager(Bundle savedInstanceState) {
        mLocalActivityManager = new LocalActivityManager(this, true);

        Bundle states =
                savedInstanceState != null
                        ? (Bundle) savedInstanceState.getBundle(STATES_KEY) : null;
        mLocalActivityManager.dispatchCreate(states);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mLocalActivityManager != null)
            mLocalActivityManager.dispatchResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mLocalActivityManager != null) {
            Bundle state = mLocalActivityManager.saveInstanceState();
            if (state != null) {
                outState.putBundle(STATES_KEY, state);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mLocalActivityManager != null)
            mLocalActivityManager.dispatchPause(isFinishing());
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mLocalActivityManager != null)
            mLocalActivityManager.dispatchStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mLocalActivityManager != null)
            mLocalActivityManager.dispatchDestroy(isFinishing());
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, Object> onRetainNonConfigurationChildInstances() {
        HashMap<String, Object> instanceMap = null;
        if (mLocalActivityManager != null) {
            Method dispatchRetainNonConfigurationInstanceMethod = null;
            try {
                dispatchRetainNonConfigurationInstanceMethod =
                        LocalActivityManager.class
                                .getDeclaredMethod("dispatchRetainNonConfigurationInstance");
                dispatchRetainNonConfigurationInstanceMethod.setAccessible(true);
                instanceMap =
                        (HashMap<String, Object>) dispatchRetainNonConfigurationInstanceMethod
                                .invoke(mLocalActivityManager);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return instanceMap;
    }

    public Activity getCurrentActivity() {
        if (mLocalActivityManager != null)
            return mLocalActivityManager.getCurrentActivity();
        else
            return this;
    }

    public final LocalActivityManager getLocalActivityManager() {
        return mLocalActivityManager;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mLocalActivityManager != null) {
            Activity activity = mLocalActivityManager.getCurrentActivity();
            if (activity != null) {
                try {
                    Method onActivityResultMethod =
                            Activity.class.getDeclaredMethod("onActivityResult", int.class,
                                    int.class, Intent.class);
                    onActivityResultMethod.setAccessible(true);
                    onActivityResultMethod.invoke(activity, requestCode, resultCode, data);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if (getParent() == null) {
            super.startActivityForResult(intent, requestCode);
        } else {
            getParent().startActivityForResult(intent, requestCode);
        }
    }
}