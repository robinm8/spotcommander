package net.olejon.spotcommander;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

public class AddComputerActivity extends ActionBarActivity
{
	private final MyMethod mMethod = new MyMethod(this);
	
	private NetworkScanTask networkScanTask;
	private PowerManager.WakeLock wakeLock;
	private MenuItem scanMenuItem;
	private ProgressBar mProgress;
	
	// Create activity
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Allow landscape?
		if(!mMethod.allowLandscape()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// Power manager
		PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "wakeLock");

		// Layout
		setContentView(R.layout.activity_add_computer);

        // Toolbar
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);

		// Progress bar
		mProgress = (ProgressBar) findViewById(R.id.progressbar);

        // Clickable link
        TextView textView = (TextView) findViewById(R.id.add_computer_information);
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        // Dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle(R.string.add_computer_dialog_title).setMessage(R.string.add_computer_dialog_message).setPositiveButton(R.string.add_computer_dialog_positive_button, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                scanNetwork();

                dialogInterface.dismiss();
            }
        }).setNegativeButton(R.string.add_computer_dialog_negative_button, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                dialogInterface.dismiss();
            }
        }).create().show();
	}
	
	// Pause activity
	@Override
	protected void onPause()
	{
		super.onPause();
		
		// Cancel network scan
		if(networkScanTask != null && networkScanTask.getStatus() == AsyncTask.Status.RUNNING) networkScanTask.cancel(true);
		
		// Release wake lock
		if(wakeLock.isHeld()) wakeLock.release();
	}
	
	// Menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.add_computer, menu);

		scanMenuItem = menu.findItem(R.id.action_scan_network);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(item.getItemId() == android.R.id.home)
		{
			finish();
			return true;
		}
		else if(item.getItemId() == R.id.action_scan_network)
		{
			scanNetwork();
			return true;
		}
		else if(item.getItemId() == R.id.action_add_computer)
		{
			addComputer();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	// Add computer
	private void addComputer()
	{
    	EditText nameEditText = (EditText) findViewById(R.id.name);
    	EditText uriEditText = (EditText) findViewById(R.id.uri);
    	EditText usernameEditText = (EditText) findViewById(R.id.username);
    	EditText passwordEditText = (EditText) findViewById(R.id.password);
    	
    	String name = nameEditText.getText().toString();
    	String uri = uriEditText.getText().toString();
    	String username = usernameEditText.getText().toString();
    	String password = passwordEditText.getText().toString();

        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(nameEditText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		
    	if(networkScanTask != null && networkScanTask.getStatus() == AsyncTask.Status.RUNNING)
    	{
    		mMethod.showToast(getString(R.string.add_computer_scanning_network), 0);
    	}
    	else if(name.equals(""))
    	{
            nameEditText.setError(getString(R.string.add_computer_invalid_name));
    	}
        else if(!uri.startsWith("http://") && !uri.startsWith("https://"))
        {
            uriEditText.setError(getString(R.string.add_computer_invalid_uri));
        }
    	else
    	{
            ContentValues contentValues = new ContentValues();
    		
    		contentValues.put(MySQLiteHelper.COLUMN_NAME, name);
    		contentValues.put(MySQLiteHelper.COLUMN_URI, uri);
    		contentValues.put(MySQLiteHelper.COLUMN_USERNAME, username);
    		contentValues.put(MySQLiteHelper.COLUMN_PASSWORD, password);
    		
    		SQLiteDatabase database = new MySQLiteHelper(this).getWritableDatabase();
    		
    		database.insert(MySQLiteHelper.TABLE_COMPUTERS, null, contentValues);
    		database.close();

            finish();
    	}
	}
	
	// Scan network
	private void scanNetwork()
	{
		if(networkScanTask != null && networkScanTask.getStatus() == AsyncTask.Status.RUNNING)
		{
			networkScanTask.cancel(true);
		}
		else
		{
			WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
	
			if(wifiManager.isWifiEnabled())
			{	
				WifiInfo wifiInfo = wifiManager.getConnectionInfo();
				int wifiIpAddress = wifiInfo.getIpAddress();
				String wifiSubnet = String.format("%d.%d.%d", (wifiIpAddress & 0xff), (wifiIpAddress >> 8 & 0xff), (wifiIpAddress >> 16 & 0xff));
				
				if(wifiSubnet.equals("0.0.0"))
				{
					mMethod.showToast(getString(R.string.add_computer_wifi_not_connected), 0);
				}
				else
				{	
					networkScanTask = new NetworkScanTask();
					networkScanTask.execute(wifiSubnet);
				}
			}
			else
			{
				mMethod.showToast(getString(R.string.add_computer_wifi_not_connected), 0);
			}
		}
	}
	
    public class NetworkScanTask extends AsyncTask<String, String, String[]>
	{
    	final EditText nameEditText = (EditText) findViewById(R.id.name);
    	final EditText uriEditText = (EditText) findViewById(R.id.uri);
		
        @Override
        protected void onPreExecute()
        {
        	if(!wakeLock.isHeld()) wakeLock.acquire();
        	
        	scanMenuItem.setTitle(getString(R.string.add_computer_stop));

        	mProgress.setVisibility(View.VISIBLE);
        	
        	mMethod.showToast(getString(R.string.add_computer_scanning_network), 0);
        	
			nameEditText.setEnabled(false);
			nameEditText.setText(getString(R.string.add_computer_scanning)+getString(R.string.ellipsis), TextView.BufferType.EDITABLE);
			
			uriEditText.setEnabled(false);
        }
        
        @Override
        protected void onProgressUpdate(String... strings)
        {
        	uriEditText.setText(getString(R.string.add_computer_trying)+" "+strings[0]+getString(R.string.ellipsis), TextView.BufferType.EDITABLE);
        }
        
        @Override
        protected void onPostExecute(String[] string)
        {
        	scanMenuItem.setTitle(getString(R.string.add_computer_scan));
        	
        	mProgress.setVisibility(View.GONE);
        	
        	String computerIpAddress = string[0];
        	String computerHostname = (string[1].equals("")) ? "Unknown" : string[1];
        	
        	nameEditText.setEnabled(true);
        	uriEditText.setEnabled(true);
        	
        	if(computerIpAddress.equals(""))
        	{
        		mMethod.showToast(getString(R.string.add_computer_installation_not_found), 1);

        		nameEditText.setText(getString(R.string.add_computer_name_edittext_text), TextView.BufferType.EDITABLE);
        		uriEditText.setText(getString(R.string.add_computer_uri_edittext_text), TextView.BufferType.EDITABLE);
        	}
        	else
        	{	
        		String foundOn = (computerHostname.equals("Computer")) ? getString(R.string.add_computer_computer_with_authentication_enabled) : computerHostname;
  
        		mMethod.showToast(getString(R.string.add_computer_installation_found)+" "+foundOn, 1);

        		nameEditText.setText(computerHostname, TextView.BufferType.EDITABLE);
        		uriEditText.setText("http://"+computerIpAddress+"/spotcommander/", TextView.BufferType.EDITABLE);
        	}
        }
        
        @Override
        protected void onCancelled()
        {
        	if(wakeLock.isHeld()) wakeLock.release();
        	
        	scanMenuItem.setTitle(getString(R.string.add_computer_scan));
        	
        	mProgress.setVisibility(View.GONE);
        	
			nameEditText.setEnabled(true);
			nameEditText.setText(getString(R.string.add_computer_name_edittext_text), TextView.BufferType.EDITABLE);
			
			uriEditText.setEnabled(true);
			uriEditText.setText(getString(R.string.add_computer_uri_edittext_text), TextView.BufferType.EDITABLE);
        }
    	
        @Override
        protected String[] doInBackground(String... strings)
        {
        	String wifiSubnet = strings[0];
        	
            HttpParams httpParameters = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, 250);
            HttpConnectionParams.setSoTimeout(httpParameters, 2500);
            HttpClient httpClient = new DefaultHttpClient(httpParameters);

            String[] networkScanResult = {"", ""};
            
            outerLoop: for(int i = 1; i <= 254; i++)
            {
            	if(isCancelled()) break;
            	
            	String computerIpAddress = wifiSubnet+"."+i;
            	
            	String[] progress = {computerIpAddress, String.valueOf(i)};
            	
            	publishProgress(progress);
            	
                HttpGet httpGet = new HttpGet("http://"+computerIpAddress+"/spotcommander/main.php?hostname");

                try
                {
    				HttpResponse httpResponse = httpClient.execute(httpGet);
    				Header[] headers = httpResponse.getAllHeaders();
    				int httpStatusCode = httpResponse.getStatusLine().getStatusCode();
    				
    				String headerString;
    				String computerHostname = "Computer";
    				
    				if(httpStatusCode == 200)
    				{	
    					computerHostname = EntityUtils.toString(httpResponse.getEntity(), HTTP.UTF_8);
    					
        				for(Header header : headers)
        				{
        					headerString = header.toString();
        				    if(headerString.contains("TP-LINK") || headerString.contains("ZyXEL")) continue outerLoop;
        				}
        				
        				if(computerHostname.contains("html")) continue;
    				}
    				else if(httpStatusCode == 401)
    				{
    					boolean isApp = false;
    					
        				for(Header header : headers)
        				{
        					headerString = header.toString();
        				    if(headerString.contains(getString(R.string.app_name))) isApp = true;
        				}
        				
        				if(!isApp) continue;
    				}
    				else
    				{
    					continue;
    				}
    				
					networkScanResult[0] = computerIpAddress;
					networkScanResult[1] = computerHostname;
					
					break;
                }
                catch(Exception e)
                {
                    Log.v("NetworkScanTask", getString(R.string.add_computer_installation_not_found)+": "+computerIpAddress);
                }
            }
            
            return networkScanResult;
        }
    }
}