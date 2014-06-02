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
import android.location.Address;
import android.location.Geocoder;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Main extends FragmentActivity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

	public JSONArray eventListResults;
    private static final double VIBEVEJ_LAT = 55.697437, VIBEVEJ_LNG = 12.526206;
    private static final float DEFAULTZOOM = 5;
    private static final int GPS_ERRORDIALOG_REQUEST = 9001;
    private int lastExpandedPosition = -1;

    public ExpandableListView getAllEventsListView;
    public GetAllEventListViewAdapter adapter;

	public static String appToken;
    public static String username;
    public static final String PREFS_NAME = "CPHnowSettings";
	GoogleMap mMap;

    LocationClient mLocationClient;
    public List<Marker> mapMarkers = new ArrayList<Marker>();

    private boolean viewSettingsIsVisible = false;

    private View mViewSettings;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        appToken = settings.getString("strAppToken", "");
        username = settings.getString("strUsername", "");
        eventListResults = getEventList();
        setContentView(R.layout.map);

        mViewSettings = findViewById(R.id.settings);


        if (servicesOK()) {
//			getAllEventsListView = (ExpandableListView) findViewById(R.id.listView1);
//			setListAdapter();

			if (initMap()) {
				Toast.makeText(this, "ready to map", Toast.LENGTH_SHORT).show();
                gotoLocation(VIBEVEJ_LAT, VIBEVEJ_LNG, DEFAULTZOOM);
				mMap.setMyLocationEnabled(true);
                mLocationClient = new LocationClient(this, this, this);
                mLocationClient.connect();
                    try {
                        for (int key = 0; key < eventListResults.length(); key+=1) {
                            JSONObject eventData = eventListResults.getJSONObject(key);
                            mapMarkers.add(setMarker(eventData.getString("strEventName"),"", eventData.getDouble("dblLatitude"), eventData.getDouble("dblLongitude")));
                        }
                        if (savedInstanceState == null) {
                            getFragmentManager().beginTransaction()
                                    .add(R.id.container, new PlaceholderFragment()).commit();
                        }
//                        getAllEventsListView = (ExpandableListView)findViewById(R.id.expListView);
//
//                        getAllEventsListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
//                            @Override
//                            public void onGroupExpand(int groupPosition) {
//                                if (lastExpandedPosition != -1 && groupPosition != lastExpandedPosition) {
//                                    getAllEventsListView.collapseGroup(lastExpandedPosition);
//                                    mapMarkers.get(lastExpandedPosition).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
//                                }
//                                lastExpandedPosition = groupPosition;
//                                mapMarkers.get(groupPosition).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
//                                gotoLocation(mapMarkers.get(groupPosition).getPosition().latitude, mapMarkers.get(groupPosition).getPosition().longitude, 15);
//                            }
//                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
			}
			else {
				Toast.makeText(this, "Map not available", Toast.LENGTH_SHORT).show();				
			}
			
		}
		else {
			Toast.makeText(this, "Map not ready!!!", Toast.LENGTH_SHORT).show();
			setContentView(R.layout.main);
		}
			
	}

	// Go to the specified location
	public void gotoLocation(double lat, double lng, float zoom) {
        LatLng ll = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, zoom);
        mMap.animateCamera(update);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        Toast.makeText(getApplication(), "SHOW MENU", Toast.LENGTH_SHORT).show();
		// Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
//        menu.findItem(R.id.action_settings).setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
//            @Override
//            public boolean onMenuItemActionExpand(MenuItem item) {
//                toggleSettings(item.getActionView());
//                return false;
//            }
//
//            @Override
//            public boolean onMenuItemActionCollapse(MenuItem item) {
//                return false;
//            }
//        });
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		case R.id.mapTypeSatellite:
			mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);		
			break;
			
		case R.id.mapTypeTerrain:
			mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);		
			break;
			
		case R.id.mapTypeNormal:
			mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);		
			break;
			
		case R.id.mapTypeHybrid:
			mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);		
			break;

        case R.id.action_create_event:
            gotoCreateEvent(item.getActionView());
            break;

        case R.id.action_settings:
            toggleSettings(item.getActionView());
            break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	protected void onStop() {
		super.onStop();
		MapStateManager mgr = new MapStateManager(this);
		mgr.saveMapState(mMap);
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		MapStateManager mgr = new MapStateManager(this);
		CameraPosition position = mgr.getSavedCameraPosition();
		
		if (position != null) {
			CameraUpdate update = CameraUpdateFactory.newCameraPosition(position);
			mMap.moveCamera(update);
			mMap.setMapType(mgr.getSavedMapType());
		}
	}
	
	
	// Checking the device for Google Play Services
	public boolean servicesOK() {
		int isAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		
		if (isAvailable == ConnectionResult.SUCCESS) {
			return true;
		}
		else if (GooglePlayServicesUtil.isUserRecoverableError(isAvailable)) {
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(isAvailable, this, GPS_ERRORDIALOG_REQUEST);
			dialog.show();
		}
		else {
			Toast.makeText(this, "Can't connect to the Google Play services", Toast.LENGTH_SHORT).show();
		}
		return false;
	}


	// Getting a reference to the map object if it does not already exist
	private boolean initMap() {
		if (mMap == null) {
			SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
			mMap = mapFrag.getMap();
			
			if (mMap != null) {
				mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
					
					@Override
					public View getInfoWindow(Marker arg0) {
						// Leaving this empty will result in a call to the below getInfoContents method
						return null;
					}
					
					@Override
					public View getInfoContents(Marker marker) {
						View v = getLayoutInflater().inflate(R.layout.info_window, null);
						TextView tvLocality = (TextView) v.findViewById(R.id.tv_locality);
						TextView tvLat = (TextView) v.findViewById(R.id.tv_lat);
						TextView tvLng = (TextView) v.findViewById(R.id.tv_lng);
						TextView tvSnippet = (TextView) v.findViewById(R.id.tv_snippet);
						
						LatLng ll = marker.getPosition();
						
						tvLocality.setText(marker.getTitle());
						tvLat.setText("Latitude: " + ll.latitude);
						tvLng.setText("Longitude: " + ll.longitude);
						tvSnippet.setText(marker.getSnippet());
						
						return v;
					}
				});
			}
		}
		return (mMap != null);
	}
	

	private void gotoLocation(double lat, double lng) {
		LatLng ll = new LatLng(lat, lng);
		CameraUpdate update = CameraUpdateFactory.newLatLng(ll);
		mMap.moveCamera(update);
	}
	
	
	public void geoLocate(View v) throws IOException {
		hideSoftKeyboard(v);
		
		//EditText et = (EditText) findViewById(R.id.editText1);
		String location = "Copenhagen"; //et.getText().toString();
		
		if (location.length() == 0) {
			Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show();
			return;
		}
		
		hideSoftKeyboard(v);
		
		Geocoder gc = new Geocoder(this);
		List<Address> list = gc.getFromLocationName(location, 1);
		Address add = list.get(0);
		String locality = add.getLocality();
		Toast.makeText(this, locality, Toast.LENGTH_LONG).show();
		
		double lat = add.getLatitude();
		double lng = add.getLongitude();
		
		gotoLocation(lat, lng, DEFAULTZOOM);
		
		setMarker(locality, add.getCountryName(), lat, lng);		
	}
	

	private void hideSoftKeyboard(View v) {
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getWindowToken(), 0);		
	}
	
	
	protected void gotoCurrentLocation() {
		Location currentLocation = mLocationClient.getLastLocation();
		
		if (currentLocation == null) {
			Toast.makeText(this, "Current location isnt available", Toast.LENGTH_SHORT).show();
		}
		else {
			LatLng ll = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
			CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, DEFAULTZOOM);
			mMap.animateCamera(update);
		}
		
		setMarker("Current location", "", currentLocation.getLatitude(), currentLocation.getLongitude());
	}
	

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub
		
	}
	
	// updates current location
	@Override
	public void onConnected(Bundle arg0) {
		Toast.makeText(this, "Connected to location service", Toast.LENGTH_SHORT).show();
		
		LocationRequest request = LocationRequest.create();
		request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Google recommends 60 seconds, we have used 5 seconds for testing purposes
		request.setInterval(5000);
		request.setFastestInterval(1000);
		mLocationClient.requestLocationUpdates(request, this);
	}
	

	@Override
	public void onDisconnected() {	
	}
	

	@Override
	public void onLocationChanged(Location location) {
		String msg = "Location: " + location.getLatitude() + " , " + location.getLongitude();
 		//Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}
	
	
	private Marker setMarker(String locality, String country, double lat, double lng) {

		MarkerOptions options = new MarkerOptions()
			.title(locality)
			.position(new LatLng(lat, lng))
			.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
		// custom marker
			//.icon(BitmapDescriptorFactory.fromResource(R.drawable."ADD YOUR FILE NAME HERE"));
		
		if (country.length() > 0) {
			options.snippet(country);
		}
		
		Marker marker =  mMap.addMarker(options);
        return marker;
	}
	
	public JSONArray getEventList() {
		String strRequestMethod = "getEventList";
        try {
            JSONObject objJsonParams = new JSONObject();
            objJsonParams.put("strUsername", username);
            objJsonParams.put("strAppToken", appToken);

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
	
//	public void setListAdapter() {
//
//		adapter = new GetAllEventListViewAdapter(eventListResults, this);
//		getAllEventsListView.setAdapter(adapter);
//
//        DisplayMetrics displaymetrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
//        int screenWidth = displaymetrics.widthPixels;
//
//        getAllEventsListView.setIndicatorBoundsRelative(screenWidth-200, screenWidth);
//
//	}

    public void toggleSettings(View view) {
        if (viewSettingsIsVisible) {
            mViewSettings.setVisibility(View.GONE);
        } else {
            mViewSettings.setVisibility(View.VISIBLE);
        }

        viewSettingsIsVisible = !viewSettingsIsVisible;
    }

    public void gotoCreateEvent(View view) {
        Intent createEvent = new Intent("com.example.events.CREATE");
        startActivityForResult(createEvent, 1);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 1) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Event activity closed", Toast.LENGTH_SHORT).show();
                updateAllEvents();
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.

                // Do something with the contact here (bigger example below)
            }
            else {
                Toast.makeText(this, "Event activity crashed?", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void updateAllEvents() {
        eventListResults = null;
        eventListResults = getEventList();
        adapter = new GetAllEventListViewAdapter(eventListResults, this);
        getAllEventsListView.setAdapter(adapter);
        mapMarkers.clear();
        mMap.clear();
        lastExpandedPosition = -1;

        try {
            for (int key = 0; key < eventListResults.length(); key+=1) {
                JSONObject eventData = eventListResults.getJSONObject(key);
                mapMarkers.add(setMarker(eventData.getString("strEventName"),"", eventData.getDouble("dblLatitude"), eventData.getDouble("dblLongitude")));
            }
        } catch (JSONException e) {
            e.printStackTrace();

        }
    }

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

            getAllEventsListView = new ExpandableListView(getActivity());
            getAllEventsListView.setId(R.id.expListView);
            setListAdapter();
            swipeRefreshLayout.addView(getAllEventsListView);
            swipeRefreshLayout.setColorScheme(android.R.color.holo_orange_dark,
                    android.R.color.holo_green_light,
                    android.R.color.holo_blue_dark,
                    android.R.color.holo_green_light);
            swipeRefreshLayout.setOnRefreshListener(this);

            setEventClickListeners();
        }

        public void setEventClickListeners() {
            getAllEventsListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
                @Override
                public void onGroupExpand(int groupPosition) {
                    if (lastExpandedPosition != -1 && groupPosition != lastExpandedPosition) {
                        getAllEventsListView.collapseGroup(lastExpandedPosition);
                        mapMarkers.get(lastExpandedPosition).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                    }
                    lastExpandedPosition = groupPosition;
                    mapMarkers.get(groupPosition).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    gotoLocation(mapMarkers.get(groupPosition).getPosition().latitude, mapMarkers.get(groupPosition).getPosition().longitude, 15);
                }
            });
        }

        public void setListAdapter() {

            adapter = new GetAllEventListViewAdapter(eventListResults, getActivity());
            getAllEventsListView.setAdapter(adapter);

            DisplayMetrics displaymetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            int screenWidth = displaymetrics.widthPixels;

            getAllEventsListView.setIndicatorBoundsRelative(screenWidth-200, screenWidth);

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



    }
}
