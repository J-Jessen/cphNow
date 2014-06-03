package com.example.events;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class GetAllEventListViewAdapter extends BaseExpandableListAdapter {

	JSONArray dataArray;
    private Context context;
    private String[] selectionPeople = {"1-5","5-10","10-20", "20-50", "50+"};
    private String[] selectionTime = {"½ Hour","1 Hour","2 Hours","2-5 Hours", "5-10 Hours", "10+ Hours"};

	public GetAllEventListViewAdapter(JSONArray dataArray , Context context)
	{
		this.dataArray = dataArray;
		this.context = context;
	}

    public String convertTime(Long timestamp) {
        Calendar mydate = Calendar.getInstance();
        mydate.setTimeInMillis(timestamp*1000);
        return new SimpleDateFormat("HH:mm").format(mydate.getTime());
    }

    public String convertDate(Long timestamp) {
        Calendar mydate = Calendar.getInstance();
        mydate.setTimeInMillis(timestamp*1000);
        return new SimpleDateFormat("d. MMM").format(mydate.getTime());
    }



    @Override
    public int getGroupCount() {
        return dataArray.length();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 1;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return groupPosition;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        ListCell cell;

        if(convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.event_list_group, null);

            cell = new ListCell();

            cell.eventName = (TextView) convertView.findViewById(R.id.event_name);
            cell.eventDistance = (TextView) convertView.findViewById(R.id.event_distance);
            cell.eventDate = (TextView) convertView.findViewById(R.id.event_date);
            cell.eventTime = (TextView) convertView.findViewById(R.id.event_time);
            cell.eventImage = (ImageView) convertView.findViewById(R.id.event_image);

            convertView.setTag(cell);
        }

        else
        {
            cell = (ListCell) convertView.getTag();
        }

        // Changing data of cell
        try{
            JSONObject jobject = dataArray.getJSONObject(groupPosition);

            cell.eventName.setText(jobject.getString("strEventName"));
            cell.eventDate.setText(convertDate(jobject.getLong("intEventTime")));
            cell.eventTime.setText(convertTime(jobject.getLong("intEventTime")));
            cell.eventDistance.setText(jobject.getInt("intEventDistance") + "m");


            switch (jobject.getInt("intEventType")) {
                case 0:
                    cell.eventImage.setImageResource(R.drawable.party);
                    break;

                case 1:
                    cell.eventImage.setImageResource(R.drawable.market);
                    break;

                case 2:
                    cell.eventImage.setImageResource(R.drawable.show);
                    break;

                case 3:
                    cell.eventImage.setImageResource(R.drawable.action);
                    break;

                default:
                    break;
            }

        }

        catch(JSONException e)
        {
            e.printStackTrace();
        }

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ListChild child;

        if(convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.event_list_item, null);

            child = new ListChild();

            child.eventUsername = (TextView) convertView.findViewById(R.id.event_username);
            child.eventCreated = (TextView) convertView.findViewById(R.id.event_created);
            child.eventDescription = (TextView) convertView.findViewById(R.id.event_description);
            child.eventFee = (TextView) convertView.findViewById(R.id.event_fee);
            child.feeValue = (TextView) convertView.findViewById(R.id.fee_value);
            child.foodValue = (ImageView) convertView.findViewById(R.id.food_value);
            child.drinksValue = (ImageView) convertView.findViewById(R.id.drinks_value);
            child.musicValue = (ImageView) convertView.findViewById(R.id.music_value);
            child.peopleValue = (TextView) convertView.findViewById(R.id.people_value);
            child.eventDuration = (TextView) convertView.findViewById(R.id.event_duration);
            child.durationValue = (TextView) convertView.findViewById(R.id.duration_value);

            convertView.setTag(child);
        }
        else
        {
            child = (ListChild) convertView.getTag();
        }

        // Changing data of cell
        try{
            JSONObject jobject = dataArray.getJSONObject(groupPosition);

            child.eventCreated.setText("Created: " + convertDate(jobject.getLong("intTimePosted")) + " " + convertTime(jobject.getLong("intTimePosted")));
            child.eventUsername.setText(jobject.getString("strUsername"));
            child.eventDescription.setText(jobject.getString("strEventDescription"));
            child.feeValue.setText(jobject.getInt("intEventFee") + " kr.");
            child.peopleValue.setText(selectionPeople[jobject.getInt("intPeople")]);
            child.durationValue.setText(selectionTime[jobject.getInt("intEventDuration")]);


            if(jobject.getBoolean("blnFood")) {
                child.musicValue.setImageResource(R.drawable.check);
            } else {
                child.musicValue.setImageResource(R.drawable.delete);
            }

            if(jobject.getBoolean("blnDrinks")) {
                child.drinksValue.setImageResource(R.drawable.check);
            } else {
                child.drinksValue.setImageResource(R.drawable.delete);
            }

            if(jobject.getBoolean("blnMusic")) {
                child.foodValue.setImageResource(R.drawable.check);
            } else {
                child.foodValue.setImageResource(R.drawable.delete);
            }
        }

        catch(JSONException e)
        {
            e.printStackTrace();
        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }


    private class ListCell
	{
		public TextView eventName;
        public ImageView eventImage;
        public TextView eventDate;
        public TextView eventTime;
        public TextView eventDistance;
    }

    private class ListChild {

        public TextView eventUsername;
        public TextView eventCreated;
        public TextView eventDescription;
        public TextView eventFee;
        public TextView feeValue;
        public TextView peopleValue;
        public TextView eventDuration;
        public TextView durationValue;
        public ImageView musicValue;
        public ImageView foodValue;
        public ImageView drinksValue;
    }

}
