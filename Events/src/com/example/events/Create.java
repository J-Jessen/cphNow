package com.example.events;

import android.app.Activity;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;


public class Create extends Activity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {
    public static final String PREFS_NAME = "CPHnowSettings";

    private String[] selectionPeople = {"1-5","5-10","10-20", "20-50", "50+"};
    private String[] selectionTime = {"½ Hour","1 Hour","2 Hours","2-5 Hours", "5-10 Hours", "10+ Hours"};
    private String[] selectionType = {"Party","Market","Show","Action"};

    private HashMap<String, Integer> peopleMap = new HashMap<String, Integer>();
    private HashMap<String, Integer> timeMap = new HashMap<String, Integer>();
    private HashMap<String, Integer> typeMap = new HashMap<String, Integer>();

    LocationClient objLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        setMapValues();

        objLocationClient = new LocationClient(this, this, this);
        objLocationClient.connect();

        Calendar dateNow = Calendar.getInstance();
        Integer intMinute = dateNow.get(Calendar.MINUTE);
        int intHour = dateNow.get(Calendar.HOUR_OF_DAY);
        int intDay = dateNow.get(Calendar.DAY_OF_MONTH);
        int intMonth = dateNow.get(Calendar.MONTH);
        int intPickerPosition = (int)Math.ceil((double)intMinute / (double)15);

        if(intPickerPosition > 3) {
            intPickerPosition = 0;
            intHour += 1;
            if(intHour > 23) {
                intHour = 0;
                dateNow.add(Calendar.DATE, 1);
                intDay = dateNow.get(Calendar.DAY_OF_MONTH);
                intMonth = dateNow.get(Calendar.MONTH);
            }
        }

        DatePicker datePicker = (DatePicker) findViewById(R.id.datePicker);
        try {
            Field f[] = datePicker.getClass().getDeclaredFields();
            for (Field field : f) {
                if (field.getName().equals("mYearSpinner")) {
                    field.setAccessible(true);
                    Object yearPicker = field.get(datePicker);
                    ((View) yearPicker).setVisibility(View.GONE);
                }

                if (field.getName().equals("mMonthSpinner")) {
                    field.setAccessible(true);
                    NumberPicker monthPicker = (NumberPicker) field.get(datePicker);
                    monthPicker.setValue(intMonth);
                }

                if (field.getName().equals("mDaySpinner")) {
                    field.setAccessible(true);
                    NumberPicker dayPicker = (NumberPicker) field.get(datePicker);
                    dayPicker.setValue(intDay);
                }

            }
        }
        catch (SecurityException e) {
            Log.d("ERROR", e.getMessage());
        }
        catch (IllegalArgumentException e) {
            Log.d("ERROR", e.getMessage());
        }
        catch (IllegalAccessException e) {
            Log.d("ERROR", e.getMessage());
        }

        TimePicker timePicker = (TimePicker) findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);
        Field f[] = timePicker.getClass().getDeclaredFields();
        for (Field field : f) {
            if (field.getName().equals("mMinuteSpinner")) {
                field.setAccessible(true);
                NumberPicker minutePicker = null;
                try {
                    minutePicker = (NumberPicker) field.get(timePicker);
                    minutePicker.setMinValue(0);
                    minutePicker.setMaxValue(3);
                    minutePicker.setDisplayedValues(new String[]{"00", "15", "30", "45"});
                    minutePicker.setValue(intPickerPosition);
                } catch(IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            if (field.getName().equals("mHourSpinner")) {
                field.setAccessible(true);
                NumberPicker hourPicker = null;
                try {
                    hourPicker = (NumberPicker) field.get(timePicker);
                    hourPicker.setValue(intHour);
                } catch(IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        Spinner numberPeople = (Spinner) findViewById(R.id.numberPeople);
        ArrayAdapter<String> peopleAdapter = new ArrayAdapter<String>(this,R.layout.spinner_custom, selectionPeople);
        peopleAdapter.setDropDownViewResource(R.layout.spinner_custom_item);
        numberPeople.setAdapter(peopleAdapter);

        Spinner eventType = (Spinner) findViewById(R.id.eventType);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>(this,R.layout.spinner_custom, selectionType);
        typeAdapter.setDropDownViewResource(R.layout.spinner_custom_item);
        eventType.setAdapter(typeAdapter);

        Spinner eventDuration = (Spinner) findViewById(R.id.eventDuration);
        ArrayAdapter<String> durationAdapter = new ArrayAdapter<String>(this,R.layout.spinner_custom, selectionTime);
        durationAdapter.setDropDownViewResource(R.layout.spinner_custom_item);
        eventDuration.setAdapter(durationAdapter);

    }

    private void setMapValues() {
        for(Integer value = 0; value < selectionPeople.length; value += 1) {
            peopleMap.put(selectionPeople[value], value);
        }

        for(Integer value = 0; value < selectionTime.length; value += 1) {
            timeMap.put(selectionTime[value], value);
        }

        for(Integer value = 0; value < selectionType.length; value += 1) {
            typeMap.put(selectionType[value], value);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public Double[] getGpsLocation() {
        Location currentLocation = objLocationClient.getLastLocation();
        Double latitude = currentLocation.getLatitude();
        Double longitude = currentLocation.getLongitude();
        return new Double[] {latitude, longitude};
    }

    public void doCreateEvent(View view) {
        EditText eventName = (EditText) findViewById(R.id.eventName);
        if(eventName.getText().toString().trim().equals("")) {
            eventName.setError("Name is required!");
            eventName.setHint("Please enter a name");
            return;
        }

        EditText eventDescription = (EditText) findViewById(R.id.eventDescription);
        if(eventDescription.getText().toString().trim().equals("")) {
            eventDescription.setError( "Description is required!" );
            eventDescription.setHint("Please write a description");
            return;
        }

        String strRequestMethod = "createNewEvent";

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String strUsername = settings.getString("strUsername", "");
        String strAppToken = settings.getString("strAppToken", "");

        try {
            JSONObject objJsonParams = new JSONObject();
            objJsonParams.put("strUsername", strUsername);
            objJsonParams.put("strAppToken", strAppToken);

            String strEventName = eventName.getText().toString().trim();
            String strEventDescription = eventDescription.getText().toString().trim();
            objJsonParams.put("strEventName", strEventName);
            objJsonParams.put("strEventDescription", strEventDescription);

            Switch eventMusic = (Switch) findViewById(R.id.switchMusic);
            boolean blnMusic = eventMusic.isChecked();
            objJsonParams.put("blnMusic", blnMusic);

            Switch eventFood = (Switch) findViewById(R.id.switchFood);
            boolean blnFood = eventFood.isChecked();
            objJsonParams.put("blnFood", blnFood);

            Switch eventDrinks = (Switch) findViewById(R.id.switchDrinks);
            boolean blnDrinks = eventDrinks.isChecked();
            objJsonParams.put("blnDrinks", blnDrinks);


            EditText eventFee = (EditText) findViewById(R.id.eventFee);
            Integer intEventFee = Integer.parseInt(eventFee.getText().toString());
            objJsonParams.put("intEventFee", intEventFee);

            Spinner eventType = (Spinner) findViewById(R.id.eventType);
            Integer intEventType = typeMap.get(eventType.getSelectedItem().toString());
            objJsonParams.put("intEventType", intEventType);

            Spinner eventPeople = (Spinner) findViewById(R.id.numberPeople);
            Integer intPeople = peopleMap.get(eventPeople.getSelectedItem().toString());
            objJsonParams.put("intPeople", intPeople);

            Spinner eventDuration = (Spinner) findViewById(R.id.eventDuration);
            Integer intEventDuration = timeMap.get(eventDuration.getSelectedItem().toString());
            objJsonParams.put("intEventDuration", intEventDuration);


            DatePicker datePicker = (DatePicker) findViewById(R.id.datePicker);
            TimePicker timePicker = (TimePicker) findViewById(R.id.timePicker);

            Integer hour = timePicker.getCurrentHour();
            Integer minute = timePicker.getCurrentMinute() * 15;

            Calendar calendar = new GregorianCalendar(datePicker.getYear(), datePicker.getMonth(),
                    datePicker.getDayOfMonth(), hour, minute);
            long intEventTime = calendar.getTimeInMillis() / 1000;
            objJsonParams.put("intEventTime", intEventTime);

            Double[] location = getGpsLocation();

            objJsonParams.put("dblLatitude", location[0]);
            objJsonParams.put("dblLongitude", location[1]);

            String strRequestResponse = new HttpRequest().execute(strRequestMethod, objJsonParams.toString()).get();
            if(strRequestResponse.isEmpty()) {
                throw new Exception();
            }
            JSONObject objResponse = new JSONObject(strRequestResponse);
            boolean blnEventCreated = objResponse.getBoolean("blnEventCreated");

            if(blnEventCreated) {
                Toast.makeText(this, "Event has been created!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            else {
                Toast.makeText(this, "Failed to create Event", Toast.LENGTH_SHORT).show();
            }

        }
        catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Connection Error!", Toast.LENGTH_LONG).show();
        }

    }


    @Override
    public void onConnectionFailed(ConnectionResult arg0) {}

    @Override
    public void onConnected(Bundle arg0) {
        // updates current location
        Toast.makeText(this, "Connected to location service", Toast.LENGTH_SHORT).show();

        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Google recommends 60 seconds, we have used 5 seconds for testing purposes
        request.setInterval(5000);
        request.setFastestInterval(1000);
        objLocationClient.requestLocationUpdates(request, this);
    }


    @Override
    public void onDisconnected() {}


    @Override
    public void onLocationChanged(Location location) {}

}
