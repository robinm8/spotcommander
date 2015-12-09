package net.olejon.spotcommander;

/*

Copyright 2015 Ole Jon Bjørkum

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see http://www.gnu.org/licenses/.

*/

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;

final class MyTools
{
	private final Context mContext;

	public MyTools(Context context)
	{
		mContext = context;
	}

    // Default shared preferences
    public boolean getDefaultSharedPreferencesBoolean(String preference)
    {
    	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    	return sharedPreferences.getBoolean(preference, false);
    }

    // Shared preferences
    public String getSharedPreferencesString(String preference)
    {
    	SharedPreferences sharedPreferences = mContext.getSharedPreferences("PREFERENCES", 0);
    	return sharedPreferences.getString(preference, "");
    }

    public void setSharedPreferencesString(String preference, String string)
    {
    	SharedPreferences sharedPreferences = mContext.getSharedPreferences("PREFERENCES", 0);
    	SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
    	sharedPreferencesEditor.putString(preference, string);
    	sharedPreferencesEditor.apply();
    }

    public boolean getSharedPreferencesBoolean(String preference)
    {
    	SharedPreferences sharedPreferences = mContext.getSharedPreferences("PREFERENCES", 0);
    	return sharedPreferences.getBoolean(preference, false);
    }

    public void setSharedPreferencesBoolean(String preference, boolean bool)
    {
    	SharedPreferences sharedPreferences = mContext.getSharedPreferences("PREFERENCES", 0);
    	SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
    	sharedPreferencesEditor.putBoolean(preference, bool);
    	sharedPreferencesEditor.apply();
    }

    public long getSharedPreferencesLong(String preference)
    {
    	SharedPreferences sharedPreferences = mContext.getSharedPreferences("PREFERENCES", 0);
    	return sharedPreferences.getLong(preference, 0);
    }

    public void setSharedPreferencesLong(String preference, long l)
    {
    	SharedPreferences sharedPreferences = mContext.getSharedPreferences("PREFERENCES", 0);
    	SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
    	sharedPreferencesEditor.putLong(preference, l);
    	sharedPreferencesEditor.apply();
    }

    // Project version
    public String getProjectVersionName()
    {
        try
        {
            return mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
        }
        catch(Exception e)
        {
            Log.e("MyTools", Log.getStackTraceString(e));
        }

        return "0.0";
    }

    // Current network
    public String getCurrentNetwork()
    {
    	WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

    	if(wifiManager.isWifiEnabled()) return wifiManager.getConnectionInfo().getSSID();

    	return "";
    }

    // Computer
    public String[] getComputer(long id)
    {
    	SQLiteDatabase mDatabase = new MySQLiteHelper(mContext).getReadableDatabase();

		String[] queryColumns = {MySQLiteHelper.COLUMN_URI, MySQLiteHelper.COLUMN_USERNAME, MySQLiteHelper.COLUMN_PASSWORD};
		Cursor mCursor = mDatabase.query(MySQLiteHelper.TABLE_COMPUTERS, queryColumns, MySQLiteHelper.COLUMN_ID+" = "+id, null, null, null, null);

		String uri = "";
		String username = "";
		String password = "";

		if(mCursor.moveToFirst())
		{	
			uri = mCursor.getString(mCursor.getColumnIndexOrThrow(MySQLiteHelper.COLUMN_URI));
			username = mCursor.getString(mCursor.getColumnIndexOrThrow(MySQLiteHelper.COLUMN_USERNAME));
			password = mCursor.getString(mCursor.getColumnIndexOrThrow(MySQLiteHelper.COLUMN_PASSWORD));
		}

		mCursor.close();
		mDatabase.close();

		return new String[] {uri, username, password};
    }

    // Allow landscape?
    public boolean allowLandscape()
    {
        int size = mContext.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        return ((size) == Configuration.SCREENLAYOUT_SIZE_LARGE || (size) == Configuration.SCREENLAYOUT_SIZE_XLARGE);
    }

    // Toast
    public void showToast(String toast, int length)
    {
        Toast.makeText(mContext, toast, length).show();
    }

    // Remote control
    public void remoteControl(long id, String action, String data)
    {
    	String[] computer = getComputer(id);

    	if(computer[0].equals(""))
    	{
    		showToast(mContext.getString(R.string.remote_control_computer_not_found), 1);
    	}
    	else
    	{
    		RemoteControlTask remoteControlTask = new RemoteControlTask();
    		remoteControlTask.execute(computer[0], computer[1], computer[2], action, data);
    	}
    }

    private class RemoteControlTask extends AsyncTask<String, Void, Void>
    {
	    @Override
	    protected Void doInBackground(String... strings)
	    {
		    final String uri = strings[0];
            final String username = strings[1];
            final String password = strings[2];
            final String action = strings[3];
            final String data = strings[4];

            RequestQueue requestQueue = Volley.newRequestQueue(mContext);

            StringRequest stringRequest = new StringRequest(Request.Method.POST, uri+"/", new Response.Listener<String>()
            {
                @Override
                public void onResponse(String response) { }
            },new Response.ErrorListener()
            {
                @Override
                public void onErrorResponse(VolleyError error)
                {
                    NetworkResponse networkResponse = error.networkResponse;

                    if(networkResponse != null)
                    {
                        int statusCode = networkResponse.statusCode;

                        if(statusCode == 401)
                        {
                            showToast(mContext.getString(R.string.remote_control_authentication_failed), 1);
                        }
                        else
                        {
                            showToast(mContext.getString(R.string.remote_control_error)+" "+statusCode, 1);
                        }
                    }
                }
            })
            {
                @Override
                protected HashMap<String, String> getParams()
                {
                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put("action", action);
                    hashMap.put("data", data);
                    return hashMap;
                }

                @Override
                public HashMap<String, String> getHeaders()
                {
                    HashMap<String, String> hashMap = new HashMap<>();

                    if(!username.equals("") && !password.equals("")) hashMap.put("Authorization", "Basic "+Base64.encodeToString((username+":"+password).getBytes(), Base64.NO_WRAP));

                    return hashMap;
                }
            };

            stringRequest.setRetryPolicy(new DefaultRetryPolicy(2500, 0, 0));

            requestQueue.add(stringRequest);

            return null;
	    }
    }
}