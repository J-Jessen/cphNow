package com.example.events;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Dialog;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;


public class Main extends FragmentActivity implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

    public JSONArray arrJsonEventList;
    private static final float DEFAULT_ZOOM = 15;
    private static final int GPS_ERROR_DIALOG_REQUEST = 9001;

    public ExpandableListView viewEventList;
    public GetAllEventListViewAdapter adapterEventList;
    public int intLastExpandedGroup = -1;

    public static String strAppToken;
    public static String strUsername;
    public static final String PREFS_NAME = "CPHnowSettings";
    SharedPreferences settings;
    GoogleMap objGoogleMap;
    Boolean blnShowParty;
    Boolean blnShowMarket;
    Boolean blnShowShow;
    Boolean blnShowAction;
    Integer intSelectionDistance;
    Integer intSelectionDays;

    LocationClient objLocationClient;
    public List<Marker> arrMapMarkers = new ArrayList<Marker>();

    private boolean blnSettingsVisible = false;
    private HashMap<String, Integer> arrMarkerType = new HashMap<String, Integer>();
    private String[] selectionDistance = {"250m","500m","750m","1km", "5km", "All"};
    private String[] selectionDays = {"Today","2 Days","5 Days","1 Week", "2 Weeks", "All"};

    private View viewSettings;
    private boolean blnSwitchListenersSet = false;
    private boolean blnSpinnerListenersSet = false;
    SharedPreferences.Editor editor;
    boolean blnSavedInstance = true;

    // Function called when activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if(savedInstanceState == null) {
            blnSavedInstance = false;
        }
        setContentView(R.layout.map);

        // Set required variables
        settings = getSharedPreferences(PREFS_NAME, 0);
        editor = settings.edit();
        strAppToken = settings.getString("strAppToken", "");
        strUsername = settings.getString("strUsername", "");

        viewSettings = findViewById(R.id.settings);
        blnShowParty = settings.getBoolean("blnShowParty", true);
        blnShowMarket = settings.getBoolean("blnShowMarket", true);
        blnShowShow = settings.getBoolean("blnShowShow", true);
        blnShowAction = settings.getBoolean("blnShowAction", true);
        intSelectionDistance = settings.getInt("intSelectionDistance", 5);
        intSelectionDays = settings.getInt("intSelectionDays", 5);
        setPreviousSettings();

        if (checkGoogleServices() && initiateGoogleMap()) {
            objGoogleMap.setMyLocationEnabled(true);
            objLocationClient = new LocationClient(this, this, this);
            objLocationClient.connect();
        }
        else {
            Toast.makeText(this, "Error in Google Play Services", Toast.LENGTH_SHORT).show();
        }

    }

    // Creates the menu (inflates the menu)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu. This adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // Listens for clicks in the menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
        case R.id.mapTypeSatellite:
            objGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            break;

        case R.id.mapTypeTerrain:
            objGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
            break;

        case R.id.mapTypeNormal:
            objGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            break;

        case R.id.mapTypeHybrid:
            objGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            break;

        case R.id.action_create_event:
            openCreateEvent(item.getActionView());
            break;

        case R.id.action_settings:
            toggleSettings(item.getActionView());
            break;

        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    // When activity is paused
    @Override
    protected void onStop() {
        super.onStop();
        MapStateManager ctxStateManager = new MapStateManager(this);
        ctxStateManager.saveMapState(objGoogleMap);
    }

    // When the activity is resumed
    @Override
    protected void onResume() {
        super.onResume();
        MapStateManager ctxStateManager = new MapStateManager(this);
        CameraPosition objPosition = ctxStateManager.getSavedCameraPosition();

        if (objPosition != null) {
            CameraUpdate update = CameraUpdateFactory.newCameraPosition(objPosition);
            objGoogleMap.moveCamera(update);
            objGoogleMap.setMapType(ctxStateManager.getSavedMapType());
        }
    }

    // Requests an updated event list from the server
    public JSONArray getEventList() {
        String strRequestMethod = "getEventList";
        try {
            JSONObject objJsonParams = new JSONObject();
            objJsonParams.put("strUsername", strUsername);
            objJsonParams.put("strAppToken", strAppToken);
            objJsonParams.put("blnShowParty", blnShowParty);
            objJsonParams.put("blnShowMarket", blnShowMarket);
            objJsonParams.put("blnShowShow", blnShowShow);
            objJsonParams.put("blnShowAction", blnShowAction);
            objJsonParams.put("intMaxDistance", intSelectionDistance);
            objJsonParams.put("intMaxDays", intSelectionDays);

            Location objCurrentLocation = objLocationClient.getLastLocation();
            objJsonParams.put("dblCurrentLatitude", objCurrentLocation.getLatitude());
            objJsonParams.put("dblCurrentLongitude", objCurrentLocation.getLongitude());

            String strRequestResponse = new HttpRequest().execute(strRequestMethod, objJsonParams.toString()).get();
            if(strRequestResponse.isEmpty()) {
                throw new Exception();
            }
            return new JSONArray(strRequestResponse);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Connection Error!", Toast.LENGTH_LONG).show();
            return new JSONArray();
        }
    }

    // Updates all elements with the newest events
    public void updateAllEvents() {
        arrJsonEventList = null;
        arrJsonEventList = getEventList();
        adapterEventList = new GetAllEventListViewAdapter(arrJsonEventList, this);
        viewEventList.setAdapter(adapterEventList);
        arrMapMarkers.clear();
        objGoogleMap.clear();
        intLastExpandedGroup = -1;

        try {
            for (int key = 0; key < arrJsonEventList.length(); key+=1) {
                JSONObject objJsonEventData = arrJsonEventList.getJSONObject(key);

                String strEventName = objJsonEventData.getString("strEventName");
                String strEventInfoBox = objJsonEventData.getInt("intEventDistance") + "m";
                Long intTimestamp = objJsonEventData.getLong("intEventTime");
                strEventInfoBox += " - "+ convertDate(intTimestamp) + " " + convertTime(intTimestamp);
                arrMapMarkers.add(setMarker(strEventName, strEventInfoBox
                        , objJsonEventData.getDouble("dblLatitude")
                        , objJsonEventData.getDouble("dblLongitude")
                        , objJsonEventData.getInt("intEventType")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Sets the user selected settings
    private void setPreviousSettings() {
        Switch switchParty = (Switch) findViewById(R.id.switchParty);
        Switch switchMarket = (Switch) findViewById(R.id.switchMarket);
        Switch switchShow = (Switch) findViewById(R.id.switchShow);
        Switch switchAction = (Switch) findViewById(R.id.switchAction);

        Spinner eventSettingsDistance = (Spinner) findViewById(R.id.eventSettingsDistance);
        ArrayAdapter<String> distanceAdapter = new ArrayAdapter<String>(this,R.layout.spinner_custom, selectionDistance);
        distanceAdapter.setDropDownViewResource(R.layout.spinner_custom_item);
        eventSettingsDistance.setAdapter(distanceAdapter);

        Spinner eventSettingsDays = (Spinner) findViewById(R.id.eventSettingsDays);
        ArrayAdapter<String> daysAdapter = new ArrayAdapter<String>(this,R.layout.spinner_custom, selectionDays);
        daysAdapter.setDropDownViewResource(R.layout.spinner_custom_item);
        eventSettingsDays.setAdapter(daysAdapter);

        switchParty.setChecked(blnShowParty);
        switchMarket.setChecked(blnShowMarket);
        switchShow.setChecked(blnShowShow);
        switchAction.setChecked(blnShowAction);

        eventSettingsDistance.setSelection(intSelectionDistance);
        eventSettingsDays.setSelection(intSelectionDays);

        setSwitchOnChangeEvents();
        setSpinnersOnChangeEvents();
    }

    // Listeners for switches in settings
    private void setSwitchOnChangeEvents() {
        if(blnSwitchListenersSet) {
            return;
        }
        Switch switchParty = (Switch) findViewById(R.id.switchParty);
        Switch switchMarket = (Switch) findViewById(R.id.switchMarket);
        Switch switchShow = (Switch) findViewById(R.id.switchShow);
        Switch switchAction = (Switch) findViewById(R.id.switchAction);

        switchParty.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                editor.putBoolean("blnShowParty", isChecked);
                blnShowParty = isChecked;
                editor.commit();
            }
        });

        switchMarket.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                editor.putBoolean("blnShowMarket", isChecked);
                blnShowMarket = isChecked;
                editor.commit();
            }
        });

        switchShow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                editor.putBoolean("blnShowShow", isChecked);
                blnShowShow = isChecked;
                editor.commit();
            }
        });

        switchAction.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                editor.putBoolean("blnShowAction", isChecked);
                blnShowAction = isChecked;
                editor.commit();
            }
        });
        blnSwitchListenersSet = true;
    }

    // Listeners for spinners in settings
    private void setSpinnersOnChangeEvents() {
        if(blnSpinnerListenersSet) {
            return;
        }

        Spinner eventSettingsDistance = (Spinner) findViewById(R.id.eventSettingsDistance);
        Spinner eventSettingsDays = (Spinner) findViewById(R.id.eventSettingsDays);

        eventSettingsDistance.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                editor.putInt("intSelectionDistance", position);
                editor.commit();
                intSelectionDistance = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        eventSettingsDays.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                editor.putInt("intSelectionDays", position);
                editor.commit();
                intSelectionDays = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        blnSpinnerListenersSet = true;
    }

    // Changes time from unix to readable string
    public String convertTime(Long timestamp) {
        Calendar mydate = Calendar.getInstance();
        mydate.setTimeInMillis(timestamp*1000);
        return new SimpleDateFormat("HH:mm").format(mydate.getTime());
    }

    // Changes date from unix to readable string
    public String convertDate(Long timestamp) {
        Calendar mydate = Calendar.getInstance();
        mydate.setTimeInMillis(timestamp*1000);
        return new SimpleDateFormat("d. MMM").format(mydate.getTime());
    }

    // Google functions - Custom and Overrides

    // Puts a custom marker on the map
    private Marker setMarker(String strEventName, String strEventInfo
            , double dblLatitude, double dblLongitude, int intEventType) {

        MarkerOptions options = new MarkerOptions()
                .title(strEventName)
                .position(new LatLng(dblLatitude, dblLongitude))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));

        if (strEventInfo.length() > 0) {
            options.snippet(strEventInfo);
        }

        Marker objMarker =  objGoogleMap.addMarker(options);
        arrMarkerType.put(objMarker.getId(), intEventType);
        return objMarker;
    }

    // Go to the specified location with given zoom
    public void animateToLocation(double dblLatitude, double dblLongitude, float fltZoom) {
        LatLng objLatLng = new LatLng(dblLatitude, dblLongitude);
        CameraUpdate objCameraUpdate = CameraUpdateFactory.newLatLngZoom(objLatLng, fltZoom);
        objGoogleMap.animateCamera(objCameraUpdate);
    }

    // Go to the device's current location
    private void gotoCurrentLocation() {

        Location objCurrentLocation = objLocationClient.getLastLocation();
        if (objCurrentLocation == null) {
            Toast.makeText(this, "Current location isn't available, turn on GPS!", Toast.LENGTH_SHORT).show();
        }
        else {
            LatLng objLatLng = new LatLng(objCurrentLocation.getLatitude(), objCurrentLocation.getLongitude());
            CameraUpdate objCameraUpdate = CameraUpdateFactory.newLatLngZoom(objLatLng, DEFAULT_ZOOM);
            objGoogleMap.moveCamera(objCameraUpdate);
        }

    }

    // Checking the device for Google Play Services
    public boolean checkGoogleServices() {
        int isAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplication());

        if (isAvailable == ConnectionResult.SUCCESS) {
            return true;
        }
        else if (GooglePlayServicesUtil.isUserRecoverableError(isAvailable)) {
            Dialog objDialog = GooglePlayServicesUtil.getErrorDialog(isAvailable, this, GPS_ERROR_DIALOG_REQUEST);
            objDialog.show();
        }
        else {
            Toast.makeText(this, "Can't connect to the Google Play services", Toast.LENGTH_SHORT).show();
        }
        return false;
    }


    // Getting a reference to the map object if it does not already exist
    private boolean initiateGoogleMap() {
        if (objGoogleMap == null) {
            SupportMapFragment objMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            objGoogleMap = objMapFragment.getMap();

            if (objGoogleMap != null) {
                objGoogleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

                    @Override
                    public View getInfoWindow(Marker arg0) {
                        // Leaving this empty will result in a call to the below getInfoContents method
                        return null;
                    }

                    @Override
                    public View getInfoContents(Marker marker) {
                        View viewInfoWindow = getLayoutInflater().inflate(R.layout.info_window, null);
                        TextView tvEventName = (TextView) viewInfoWindow.findViewById(R.id.eventInfoName);
                        TextView tvEventInfo = (TextView) viewInfoWindow.findViewById(R.id.eventInfo);
                        ImageView tvEventIcon = (ImageView) viewInfoWindow.findViewById(R.id.eventTypeIcon);

                        int intEventType = arrMarkerType.get(marker.getId());
                        switch(intEventType) {
                            case 0:
                                tvEventIcon.setImageResource(R.drawable.party);
                                break;
                            case 1:
                                tvEventIcon.setImageResource(R.drawable.market);
                                break;
                            case 2:
                                tvEventIcon.setImageResource(R.drawable.show);
                                break;
                            case 3:
                                tvEventIcon.setImageResource(R.drawable.action);
                                break;
                        }
                        tvEventName.setText(marker.getTitle());
                        tvEventInfo.setText(marker.getSnippet());

                        return viewInfoWindow;
                    }
                });
            }
        }
        return (objGoogleMap != null);
    }

    // Runs when Google Maps connection is made and triggers the first event update
    @Override
    public void onConnected(Bundle arg0) {
        LocationRequest objLocationRequest = LocationRequest.create();
        objLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Google recommends 60 seconds, we have used 5 seconds for testing purposes
        objLocationRequest.setInterval(5000);
        objLocationRequest.setFastestInterval(1000);
        objLocationClient.requestLocationUpdates(objLocationRequest, this);
        gotoCurrentLocation();

        arrJsonEventList = getEventList();
        try {
            for (int intKey = 0; intKey < arrJsonEventList.length(); intKey += 1) {
                JSONObject objJsonEventData = arrJsonEventList.getJSONObject(intKey);
                String strEventName = objJsonEventData.getString("strEventName");
                String strEventInfoBox = objJsonEventData.getInt("intEventDistance") + "m";
                Long intTimestamp = objJsonEventData.getLong("intEventTime");
                strEventInfoBox += " - "+ convertDate(intTimestamp) + " " + convertTime(intTimestamp);
                arrMapMarkers.add(setMarker(strEventName, strEventInfoBox
                                , objJsonEventData.getDouble("dblLatitude")
                                , objJsonEventData.getDouble("dblLongitude")
                                , objJsonEventData.getInt("intEventType"))
                );
            }
            if (!blnSavedInstance) {
                getFragmentManager().beginTransaction()
                        .add(R.id.container, new PlaceholderFragment()).commit();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Used if device locations changes (not in use)
    @Override
    public void onLocationChanged(Location location) {}

    // Triggered if connection to Google Play Services is disconnected (not in use)
    @Override
    public void onDisconnected() {}

    // Triggered if no connection to Google Play Services (not in use)
    @Override
    public void onConnectionFailed(ConnectionResult arg0) {}

    // OTHER VIEWS / ACTIVITIES

    // Show or hide the settings view
    public void toggleSettings(View view) {

        if (blnSettingsVisible) {
            viewSettings.setVisibility(View.GONE);
        } else {
            viewSettings.setVisibility(View.VISIBLE);
        }
        blnSettingsVisible = !blnSettingsVisible;

    }

    // Starts the create event activity
    public void openCreateEvent(View view) {
        Intent createEvent = new Intent("com.example.events.CREATE");
        startActivityForResult(createEvent, 1);
    }

    // Waits for response from Create event
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 1) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // Event activity closed - Refresh list
                updateAllEvents();
            }
            else {
                // Event activity cancelled or crashed
            }
        }
    }

    // Class to create a SwipeRefreshLayout combined with an ExpandableListView
    public class PlaceholderFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

        private SwipeRefreshLayout swipeRefreshLayout;

        public PlaceholderFragment() { }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            swipeRefreshLayout = new SwipeRefreshLayout(getActivity());
            return swipeRefreshLayout;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            viewEventList = new ExpandableListView(getActivity());
            viewEventList.setId(R.id.expListView);
            setListAdapter();
            swipeRefreshLayout.addView(viewEventList);
            swipeRefreshLayout.setColorScheme(android.R.color.holo_orange_dark,
                    android.R.color.holo_green_light,
                    android.R.color.holo_blue_dark,
                    android.R.color.holo_green_light);
            swipeRefreshLayout.setOnRefreshListener(this);

            setEventClickListeners();
        }

        @Override
        public void onRefresh() {

            new Thread() {
                public void run() {
                    SystemClock.sleep(2000);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateAllEvents();
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                };
            }.start();
        }

        public void setEventClickListeners() {
            viewEventList.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
                @Override
                public void onGroupExpand(int intGroupPosition) {
                    if (intLastExpandedGroup != -1 && intGroupPosition != intLastExpandedGroup) {
                        Marker objLastMarker = arrMapMarkers.get(intLastExpandedGroup);
                        viewEventList.collapseGroup(intLastExpandedGroup);
                        objLastMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                        if(objLastMarker.isInfoWindowShown()) {
                            objLastMarker.hideInfoWindow();
                        }
                    }
                    intLastExpandedGroup = intGroupPosition;
                    arrMapMarkers.get(intGroupPosition).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    animateToLocation(arrMapMarkers.get(intGroupPosition).getPosition().latitude
                            , arrMapMarkers.get(intGroupPosition).getPosition().longitude, 15);
                    arrMapMarkers.get(intGroupPosition).showInfoWindow();

                }
            });
        }

        public void setListAdapter() {

            adapterEventList = new GetAllEventListViewAdapter(arrJsonEventList, getActivity());
            viewEventList.setAdapter(adapterEventList);

            DisplayMetrics objDisplayMetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(objDisplayMetrics);
            int intScreenWidth = objDisplayMetrics.widthPixels;

            viewEventList.setIndicatorBoundsRelative(intScreenWidth-200, intScreenWidth);

        }
    }
}
