package com.example.events;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
public class Splash extends Activity {

    private boolean blnShowSplash;
    public static final String PREFS_NAME = "CPHnowSettings";

    // Function called when activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        blnShowSplash = settings.getBoolean("showSplash", true);

        if (blnShowSplash) {
            setContentView(R.layout.splash);
            getActionBar().hide();
            Thread splashTimer = new Thread(){
                public void run() {
                    try {
                        sleep(3000);
                        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putBoolean("showSplash", false);
                        editor.commit();
                        String strAppToken = settings.getString("strAppToken", "");
                        String strUsername = settings.getString("strUsername", "");

                        if (strAppToken.isEmpty() || strUsername.isEmpty()) {
                            Intent registerIntent = new Intent("com.example.events.REGISTER");
                            startActivity(registerIntent);
                        }
                        else {
                            Intent mainIntent = new Intent("com.example.events.MAIN");
                            startActivity(mainIntent);
                        }
                    }
                    catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                    finally{
                        finish();
                    }
                }
            };
            splashTimer.start();
        }
        else {
            Intent mainIntent = new Intent("com.example.events.MAIN");
            startActivity(mainIntent);
            finish();
        }
    }

    // Runs when the activity is destroyed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("showSplash", true);
        editor.commit();
    }

    // Runs when the activity is suspended in memory
    @Override
    protected void onPause() {
        super.onPause();
    }


}
