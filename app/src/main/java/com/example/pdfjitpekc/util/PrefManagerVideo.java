package com.example.pdfjitpekc.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class PrefManagerVideo {

    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context _context;

    // shared pref mode
    int PRIVATE_MODE = 0;

    // Shared preferences file name
    private static final String PREF_NAME = "font_rcloudapp";

    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";

    public PrefManagerVideo(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void setBoolean(String PREF_NAME, Boolean val) {
        editor.putBoolean(PREF_NAME, val);
        editor.commit();
    }
    public void setString(String PREF_NAME, String VAL) {
        Log.d("TAGNEW", "setString: "+PREF_NAME+" "+VAL);
        editor.putString(PREF_NAME, VAL);
        editor.commit();
    }
    public void setInt(String PREF_NAME, int VAL) {
        editor.putInt(PREF_NAME, VAL);
        editor.commit();
    }
    public boolean getBoolean(String PREF_NAME) {
        return pref.getBoolean(PREF_NAME,true);
    }
    public void remove(String PREF_NAME){
        if(pref.contains(PREF_NAME)){
            editor.remove(PREF_NAME);
            editor.commit();
        }
    }
    public String getString(String PREF_NAME) {

        if(pref.contains(PREF_NAME)){
            Log.d("TAGNEW", "setString: "+PREF_NAME+" "+pref.getString(PREF_NAME, null));
            return pref.getString(PREF_NAME,null);
        }
        if (PREF_NAME.equals("LANGUAGE_DEFAULT")){
            return "0";
        }
        if(PREF_NAME.equals("ORDER_DEFAULT_IMAGE")){
            return "created";
        }
        if(PREF_NAME.equals("ORDER_DEFAULT_GIF")){
            return "created";
        }
        if(PREF_NAME.equals("ORDER_DEFAULT_VIDEO")){
            return "created";
        }
        if(PREF_NAME.equals("ORDER_DEFAULT_JOKE")){
            return "created";
        }
        if(PREF_NAME.equals("ORDER_DEFAULT_STATUS")){
            return "created";
        }
        return  "";
    }

    public int getInt(String key) {
        return pref.getInt(key,0);
    }
}
