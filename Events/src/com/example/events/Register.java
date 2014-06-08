package com.example.events;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;



public class Register extends Activity {

    public static final String PREFS_NAME = "CPHnowSettings";

    private EditText fieldUsername;
    private boolean blnTextColorChanged = false;
    private String strChosenUsername = "";

    // Function called when activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        fieldUsername = (EditText)findViewById(R.id.strUsername);
        fieldUsername.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if(blnTextColorChanged) {
                    fieldUsername.setTextColor(Color.BLACK);
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    // Creates the menu (inflates the menu)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {return true;}

    // Listens for clicks in the menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {return super.onOptionsItemSelected(item);}

    // Combines duplicate username check and user registration
    public void doRegisterUser(View view) {
        boolean blnRegistered = false;
        strChosenUsername = "";
        boolean blnUsernameAvailable = checkUsername();
        if(blnUsernameAvailable && !strChosenUsername.isEmpty()) {
            blnRegistered = registerNewUser();
            if(false == blnRegistered) {
                Toast.makeText(this, "Something went wrong! Try again.", Toast.LENGTH_SHORT).show();
            }
            else {
                Intent mainIntent = new Intent("com.example.events.MAIN");
                startActivity(mainIntent);
                finish();
            }
        }
    }

    // Checks for duplicate usernames in DB
    private boolean checkUsername() {

        String strRequestMethod = "checkExistingUsername";
        String strUsername = fieldUsername.getText().toString();

        if(!strUsername.isEmpty()) {
            try {

                JSONObject objJsonParams = new JSONObject();
                objJsonParams.put("strUsername", strUsername);

                String strRequestResponse = new HttpRequest().execute(strRequestMethod, objJsonParams.toString()).get();
                if(strRequestResponse.isEmpty()) {
                    throw new Exception();
                }
                JSONObject objResponse = new JSONObject(strRequestResponse);
                boolean blUsernameTaken = objResponse.getBoolean("blnUsernameTaken");

                if(blUsernameTaken) {
                    // Username is taken!
                    Toast.makeText(this, "Username is taken!", Toast.LENGTH_SHORT).show();
                    fieldUsername.setTextColor(Color.RED);
                    blnTextColorChanged = true;
                    return false;
                }
                else {
                    // Username is available!
                    strChosenUsername = strUsername;
                    return true;
                }

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Connection Error!", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        else {
            Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    // Creates the new user and saves app-token to local storage
    private boolean registerNewUser() {
        String strRequestMethod = "registerNewUser";
        try {
            JSONObject objJsonParams = new JSONObject();
            objJsonParams.put("strUsername", strChosenUsername);

            String strRequestResponse = new HttpRequest().execute(strRequestMethod, objJsonParams.toString()).get();
            if(strRequestResponse.isEmpty()) {
                throw new Exception();
            }
            JSONObject objResponse = new JSONObject(strRequestResponse);
            boolean blnUserCreated = objResponse.getBoolean("blnUserCreated");

            if(blnUserCreated) {
                String strAppToken = objResponse.getString("strAppToken");

                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("strAppToken", strAppToken);
                editor.putString("strUsername", strChosenUsername);
                editor.commit();
                return true;
            }
            else {
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Connection Error!", Toast.LENGTH_LONG).show();
            return false;
        }
    }
}