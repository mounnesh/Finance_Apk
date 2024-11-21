package com.talhaatif.financeapk.firebase;

import android.content.Context;
import android.content.SharedPreferences;

public class Util {
    public void saveLocalData(Context activity, String key, String value) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply(); // apply changes
    }

    public String getLocalData(Context activity, String key) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences("my_preferences", Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, "");
    }
}
