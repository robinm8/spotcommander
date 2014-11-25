package net.olejon.spotcommander;

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

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.List;

final class MyMethod
{
	private final Context mContext;
	
	public MyMethod(Context context)
	{
		mContext = context;
	}
	
	// Allow landscape?
    public boolean allowLandscape()
    {
    	return ((mContext.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE || (mContext.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE);
    }
	
    // Toast
    public void showToast(String toast, int length)
    {
        Toast.makeText(mContext, toast, length).show();
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
    	SQLiteDatabase mDatabase = new MySQLiteHelper(mContext).getWritableDatabase();
	    
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
    
    private class RemoteControlTask extends AsyncTask<String, Void, String[]>
    {
	    @Override
	    protected void onPostExecute(String[] response)
	    {
            if(response[0].equals("error"))
    		{
    			showToast(mContext.getString(R.string.remote_control_error)+" "+response[1], 1);
    		}
    		else if(response[0].equals("timeout"))
    		{
    			showToast(mContext.getString(R.string.remote_control_timeout), 1);
    		}
    		else if(response[0].equals("authentication_failed"))
    		{
    			showToast(mContext.getString(R.string.remote_control_authentication_failed), 1);
    		}
	    }

	    @Override
	    protected String[] doInBackground(String... strings)
	    {
		    String uri = strings[0];
		    String username = strings[1];
		    String password = strings[2];
		    String action = strings[3];
            String data = strings[4];
		
		    HttpParams httpParameters = new BasicHttpParams();
		    HttpConnectionParams.setConnectionTimeout(httpParameters, 2000);
		    HttpConnectionParams.setSoTimeout(httpParameters, 20000);
		    HttpClient httpClient = new DefaultHttpClient(httpParameters);
		    HttpPost httpPost = new HttpPost(uri+"/main.php");
		    httpPost.setHeader("Authorization", "Basic "+Base64.encodeToString((username+":"+password).getBytes(), Base64.NO_WRAP));
		
		    String[] response = {"", ""};
		
		    try
		    {
			    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			    nameValuePairs.add(new BasicNameValuePair("action", action));
			    nameValuePairs.add(new BasicNameValuePair("data", data));
			    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			    HttpResponse httpResponse = httpClient.execute(httpPost);
			
			    int httpStatusCode = httpResponse.getStatusLine().getStatusCode();
			    
			    response[1] = String.valueOf(httpStatusCode);
			
			    if(httpStatusCode == 200)
			    {
			    	response[0] = EntityUtils.toString(httpResponse.getEntity(), HTTP.UTF_8);
			    }
			    else if(httpStatusCode == 401)
			    {
			    	response[0] = "authentication_failed";
			    }
			    else
			    {
			    	response[0] = "error";
			    }
			}
		    catch(Exception e)
		    {
		    	response[0] = "timeout";

                Log.e("RemoteControlTask doInBackground", Log.getStackTraceString(e));
		    }
			
			return response;
	    }
    }
}