package edu.fsu.cs.fbcalendarsync;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONObject;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.facebook.Response;

import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Reminders;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseBooleanArray;

public class MainActivity extends Activity {
	
	private Button submitbutton;
	ListView listView1;
	UiLifecycleHelper uiHelper;
	private LoginButton loginButton;
	static ArrayList<String> listItems;
	String[] list;
	
	/////////////////////////////
	//Search Constant
	/////////////////////////////
	private static final String[] COLS = new String[] {
	CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART,
	CalendarContract.Events.DESCRIPTION,
	CalendarContract.Events.CALENDAR_ID, CalendarContract.Events._ID };
	/////////////////////////////
	//end Search Constant
	/////////////////////////////
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		uiHelper = new UiLifecycleHelper(MainActivity.this, callback);
	    uiHelper.onCreate(savedInstanceState);    	     
	    
	    loginButton = (LoginButton) findViewById(R.id.login);
	    listItems = new ArrayList<String>();
	    LoginHandler(loginButton);
	    				
				// *** Submit Button ***
			    submitbutton = (Button) findViewById(R.id.button2);
			    submitbutton.setOnClickListener(new OnClickListener() {
			    	public void onClick(View v) {
			    		
			    		// Array of checked items and their positions taken
			    		SparseBooleanArray checkedItems = listView1.getCheckedItemPositions();
			    	  
			    		if (checkedItems != null) {
			    			for (int i = 0; i < checkedItems.size(); i++) {
			    				if (checkedItems.valueAt(i)) {
			    					
			    					// If the item is checked, grab the string
			    					String friendInfo = listView1.getAdapter()
			    							.getItem(checkedItems.keyAt(i)).toString();
			    					
			    					// Parse the string for name and date
			    					String[] info = friendInfo.split(",");
			    				  
			    					// Enter into calendar (Default 10 minutes)
			    					createEvent(info[0].trim( ), info[1].trim( ), 10);
			    				}
			    			}
			    		}
			    	}
			    });	
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private Session.StatusCallback callback = new Session.StatusCallback() {
	    @Override
	    public void call(Session session, SessionState state, Exception exception) {
	        onSessionStateChange(session, state, exception);
	    }
	};
	
	private void onSessionStateChange(Session session, SessionState state, Exception exception) {
	    if (state.isOpened()) {
	        Log.d("facebook", "Logged in...");
	       
	     // Request user data and show the results
	        Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {

	            @Override
	            public void onCompleted(GraphUser user, Response response) {
	                if (user != null) {
	                    // Display the parsed user info
	                    //userInfoTextView.setText(buildUserInfoDisplay(user));
	                	Log.d("facebook", "Result: " + response.toString());
	                	
	                }                
	            }            
	        
	        });
	     
	        String fqlQuery = "SELECT uid, name, birthday_date FROM user WHERE uid IN " +
	        		"(SELECT uid2 FROM friend WHERE uid1 = me())";
	        		Bundle params = new Bundle();
	        	
	        		params.putString("q", fqlQuery);
	        		//session = Session.getActiveSession();
	        		Request request = new Request(session,"/fql",params, HttpMethod.GET,	new Request.Callback(){ 
	        			public void onCompleted(Response response) {
	        				Log.d("facebook", "Result: " + response.toString());
	        				
	        				try
	        			    {
	        			        GraphObject go  = response.getGraphObject();
	        			        JSONObject  jso = go.getInnerJSONObject();
	        			        JSONArray   arr = jso.getJSONArray( "data" );
	        			        Log.d("facebook", "length: " + arr.length());


	        			        for ( int i = 0; i < arr.length(); i++ )
	        			        {
	        			            JSONObject json_obj = arr.getJSONObject( i );

	        			            String name   = json_obj.getString( "name" );
	        			            String dob = json_obj.getString("birthday_date");
	        			            
	        			            if(!dob.equals("null"))
	        			            {
	        			            	Log.d("facebook", "name: " + name + " dob: " + dob);
	        			            	String info = format(name, dob);
	        			            	listItems.add(info);
	        			            	Log.d("facebook","size:" + listItems.size());	        			            	 
	        			            }
	        			            
	        			        }
	        			        
	        			        list = new String[listItems.size()];
	        			        for(int i = 0; i < listItems.size(); i++)
	        			        {
	        			        	list[i] = listItems.get(i);
	        			        	Log.d("facebook", list[i]);
	        			        }
        			            listView1 = (ListView) findViewById(R.id.listView);
        			    		listView1.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_multiple_choice, list));
        			    		listView1.setItemsCanFocus(false);
        			    		listView1.setBackgroundColor(android.graphics.Color.BLACK);
        			    		listView1.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        			    		// Check or Uncheck the box on click
        			    		listView1.setOnItemClickListener(new OnItemClickListener() {
        			    			@Override
        			    			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
        			    					long arg3) {
        			    				CheckedTextView ctv = (CheckedTextView) arg1;
        			    			}
        			    		});
        			    		
	        			    }
	        			    catch ( Throwable t )
	        			    {
	        			        t.printStackTrace();
	        			    }
	        			} 
	        		}); 
	        		
	        		Request.executeBatchAsync(request);
	    }
 
	    else if (state.isClosed()) {
	        Log.d("facebook", "Logged out...");
	    }
    
	}
	        	
	public void createEvent(String name, String birthday, int reminder_minutes) {

			String[] calendarIds = new String[] { "_id", "name" };
			Uri calendars = Uri.parse("content://com.android.calendar/calendars");

			Cursor mCursor = managedQuery(calendars, calendarIds, null, null, null);
			if (mCursor.moveToFirst()) {
				String calName;
				String calId;
				int nameColumn = mCursor.getColumnIndex("name");
				int idColumn = mCursor.getColumnIndex("_id");
				// do {
				calName = mCursor.getString(nameColumn);
				calId = mCursor.getString(idColumn);

				long theDate = 0;
				try {
					Date date = new SimpleDateFormat("MM-dd").parse(birthday);
					theDate = date.getTime();
				} catch (Exception e) {
				}

				// //////////////////////////////////////
				// search for event
				// //////////////////////////////////////
				String mSelectionClause = CalendarContract.Events.TITLE
						+ " = ? AND " + CalendarContract.Events.DESCRIPTION
						+ " = ?";// AND " + CalendarContract.Events.DTSTART + " = ?
									// AND " + CalendarContract.Events.CALENDAR_ID;
				String[] mSelectionArgs = new String[] { name + "'s Birthday",
						"Facebook Birthday", };// birthday, calId};
				String mOrderBy;
				int[] mListItems;

				mCursor = getContentResolver().query(
						CalendarContract.Events.CONTENT_URI, COLS,
						mSelectionClause, mSelectionArgs, null);
				mCursor.moveToFirst();
				String title = "N/A";
				Long start = 0L;
				Format df = DateFormat.getDateFormat(this);
				Format tf = DateFormat.getTimeFormat(this);
				try {
					title = mCursor.getString(0);
					start = mCursor.getLong(1);
				} catch (Exception e) {
					// ignore
				}
				Log.i("find event",
						title + " on " + df.format(start) + " at "
								+ tf.format(start));
				// //////////////////////////////////////
				// end search for event
				// //////////////////////////////////////

				////////////////////////////////////////
				// add event
				///////////////////////////////////////
				if (title.equals("N/A")) {
					// new birthday
				
					ContentResolver cr = this.getContentResolver();
					ContentValues event = new ContentValues();

					event.put(CalendarContract.Events.CALENDAR_ID, calId);
					event.put(CalendarContract.Events.TITLE, name + "'s Birthday");
					event.put(CalendarContract.Events.DESCRIPTION,
							"Facebook Birthday");
					event.put(CalendarContract.Events.DTSTART, theDate);
					event.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone
							.getDefault().getID());
					event.put(CalendarContract.Events.DURATION, "P1D");
					event.put(CalendarContract.Events.ALL_DAY, 1);
					event.put(CalendarContract.Events.RRULE, "FREQ=YEARLY");
					event.put(CalendarContract.Events.HAS_ALARM, 1);

					// set a 10 minute reminder
					ContentValues reminders = new ContentValues();
					Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, event);
					long eventID = Long.parseLong(uri.getLastPathSegment());
					reminders.put(Reminders.EVENT_ID, eventID);
					reminders.put(Reminders.METHOD, Reminders.METHOD_ALERT);
					reminders.put(Reminders.MINUTES, reminder_minutes);
					Uri uri2 = cr.insert(Reminders.CONTENT_URI, reminders);
				} else {
					//birthday already exists
				}
				////////////////////////////////////////
				// end add event
				///////////////////////////////////////
			}
			mCursor.close();
		
	}

	   public void LoginHandler(View v)
	    {
		
		   Session.StatusCallback statusCallback =  new Session.StatusCallback() {
           
			@Override
            public void call(Session session, SessionState state, Exception exception){

                   if(session.isOpened()){ //do something}
                   	Log.d("facebook","here");
             	
                   }
            }
			};
			
			Session session = new Session(this);
			session.openForRead(new Session.OpenRequest(this)
			                       .setCallback(statusCallback)
			                       .setPermissions(Arrays.asList("friends_birthday")));
	    }
	
	//Gets the correct format
	 public static String format(String name, String bday)
	 {
		 char[] middlebd = new char[5];
		 String all;
		 
		 char[] charbday = bday.toCharArray();
		
		 middlebd[0] = charbday[0];
		 middlebd[1] = charbday[1];
		 middlebd[2] = '-';
		 middlebd[3] = charbday[3];
		 middlebd[4] = charbday[4];
		 
		 //newbday = middlebd.toString();
		 
		 all = name + ", " + String.valueOf(middlebd); 
		 
		 return all;
	 }
	 
	 @Override
	 public void onResume() {
	     super.onResume();
	     uiHelper.onResume();
	 }

	 @Override
	 public void onActivityResult(int requestCode, int resultCode, Intent data) {
	     super.onActivityResult(requestCode, resultCode, data);
	     uiHelper.onActivityResult(requestCode, resultCode, data);
	     Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	 }

	 @Override
	 public void onPause() {
	     super.onPause();
	     uiHelper.onPause();
	 }

	 @Override
	 public void onDestroy() {
	     super.onDestroy();
	     uiHelper.onDestroy();
	 }

	 @Override
	 public void onSaveInstanceState(Bundle outState) {
	     super.onSaveInstanceState(outState);
	     uiHelper.onSaveInstanceState(outState);
	 }
}


