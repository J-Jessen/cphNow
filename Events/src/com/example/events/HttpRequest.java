package com.example.events;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Kim on 18/05/2014.
 */
class HttpRequest extends AsyncTask<String, String, String> {

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d("onPreExecute: ", "in onPreExecute()");
    }

    @Override
    protected String doInBackground(String ... strParams) {

        boolean blnRequestSuccess = false;
        String strResponse = "";
        String strRequestConnector = "http://www.msk-webdesign.dk/cphnow/connectors/handleRequest.php";
        if(strRequestConnector.isEmpty()) {
            return strResponse;
        }
        try {
            JSONParser jsonParser = new JSONParser();
            List<NameValuePair> arrRequestParams = new ArrayList<NameValuePair>();
            arrRequestParams.add(new BasicNameValuePair("request", strParams[0]));
            arrRequestParams.add(new BasicNameValuePair("params", strParams[1]));

            JSONObject objJsonResponse = jsonParser.makeHttpRequest(strRequestConnector, "POST", arrRequestParams);
            if(null == objJsonResponse) {
                return strResponse;
            }
            blnRequestSuccess = objJsonResponse.getBoolean("status");
            if (blnRequestSuccess == true) {
            	if (strParams[0].equals("getEventList")) {
            		JSONArray resultObj = objJsonResponse.getJSONArray("result");					
            		strResponse = resultObj.toString();
				}
            	else {
            		JSONObject resultObj = objJsonResponse.getJSONObject("result");
            		strResponse = resultObj.toString();            		
            	}
                Log.d("Response: ", strResponse);
            }
            else {
                Log.d("Response: ", "Request failed");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return strResponse;
    }

    @Override
    protected void onPostExecute(String result) {
        Log.d("onPostExecute: ", result);
    }
}
