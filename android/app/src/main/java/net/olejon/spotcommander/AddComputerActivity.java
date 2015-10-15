package net.olejon.spotcommander;

/*

Copyright 2015 Ole Jon Bjørkum

This file is part of SpotCommander.

SpotCommander is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SpotCommander is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SpotCommander.  If not, see <http://www.gnu.org/licenses/>.

*/

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
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

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AddComputerActivity extends AppCompatActivity
{
    private final Context mContext = this;

	private final MyTools mTools = new MyTools(mContext);

	private PowerManager.WakeLock mWakeLock;

	private MenuItem mMenuItem;
	private ProgressBar mProgressBar;

    private NetworkScanTask mNetworkScanTask;

	// Create activity
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Allow landscape?
		if(!mTools.allowLandscape()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// Power manager
		PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        //noinspection deprecation
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "wakeLock");

		// Layout
		setContentView(R.layout.activity_add_computer);

        // Toolbar
        final Toolbar toolbar = (Toolbar) findViewById(R.id.add_computer_toolbar);

        setSupportActionBar(toolbar);

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Progress bar
        mProgressBar = (ProgressBar) findViewById(R.id.add_computer_progressbar);

        // Information
        TextView textView = (TextView) findViewById(R.id.add_computer_information);
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        // Dialog
        new MaterialDialog.Builder(mContext).title(getString(R.string.add_computer_scan_dialog_title)).content(getString(R.string.add_computer_scan_dialog_message)).positiveText(getString(R.string.add_computer_scan_dialog_positive_button)).negativeText(getString(R.string.add_computer_scan_dialog_negative_button)).onPositive(new MaterialDialog.SingleButtonCallback()
        {
            @Override
            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction)
            {
                scanNetwork();
            }
        }).contentColorRes(R.color.black).negativeColorRes(R.color.black).show();
	}

	// Pause activity
	@Override
	protected void onPause()
	{
		super.onPause();

		if(mNetworkScanTask != null && mNetworkScanTask.getStatus() == AsyncTask.Status.RUNNING) mNetworkScanTask.cancel(true);

		if(mWakeLock.isHeld()) mWakeLock.release();
	}

	// Menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu_add_computer, menu);

        mMenuItem = menu.findItem(R.id.add_computer_menu_scan_network);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
        switch(item.getItemId())
        {
            case android.R.id.home:
            {
                NavUtils.navigateUpFromSameTask(this);
                return true;
            }
            case R.id.add_computer_menu_scan_network:
            {
                scanNetwork();
                return true;
            }
            case R.id.add_computer_menu_add_computer:
            {
                addComputer();
                return true;
            }
            default:
            {
                return super.onOptionsItemSelected(item);
            }
        }
	}

	// Add computer
	private void addComputer()
	{
    	EditText nameEditText = (EditText) findViewById(R.id.add_computer_name);
    	EditText uriEditText = (EditText) findViewById(R.id.add_computer_uri);
    	EditText usernameEditText = (EditText) findViewById(R.id.add_computer_username);
    	EditText passwordEditText = (EditText) findViewById(R.id.add_computer_password);

    	String name = nameEditText.getText().toString();
    	String uri = uriEditText.getText().toString();
    	String username = usernameEditText.getText().toString();
    	String password = passwordEditText.getText().toString();

        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(nameEditText.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);

    	if(mNetworkScanTask != null && mNetworkScanTask.getStatus() == AsyncTask.Status.RUNNING)
    	{
    		mTools.showToast(getString(R.string.add_computer_scanning_network), 0);
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
            contentValues.put(MySQLiteHelper.COLUMN_NETWORK_NAME, "");
            contentValues.put(MySQLiteHelper.COLUMN_NETWORK_DEFAULT, 0);

    		SQLiteDatabase database = new MySQLiteHelper(mContext).getWritableDatabase();

    		database.insert(MySQLiteHelper.TABLE_COMPUTERS, null, contentValues);

    		database.close();

            finish();
    	}
	}

	// Scan network
	private void scanNetwork()
	{
		if(mNetworkScanTask != null && mNetworkScanTask.getStatus() == AsyncTask.Status.RUNNING)
		{
            mNetworkScanTask.cancel(true);
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
					mTools.showToast(getString(R.string.add_computer_wifi_not_connected), 0);
				}
				else
				{
                    mNetworkScanTask = new NetworkScanTask();
                    mNetworkScanTask.execute(wifiSubnet);
				}
			}
			else
			{
				mTools.showToast(getString(R.string.add_computer_wifi_not_connected), 0);
			}
		}
	}

    public class NetworkScanTask extends AsyncTask<String, String, String[]>
	{
    	final EditText nameEditText = (EditText) findViewById(R.id.add_computer_name);
    	final EditText uriEditText = (EditText) findViewById(R.id.add_computer_uri);

        @Override
        protected void onPreExecute()
        {
        	if(!mWakeLock.isHeld()) mWakeLock.acquire();

            mMenuItem.setTitle(getString(R.string.add_computer_menu_stop));

            mProgressBar.setVisibility(View.VISIBLE);

        	mTools.showToast(getString(R.string.add_computer_scanning_network), 0);

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
            mMenuItem.setTitle(getString(R.string.add_computer_menu_scan));

            mProgressBar.setVisibility(View.GONE);

        	String computerIpAddress = string[0];
        	String computerHostname = (string[1].equals("")) ? "Unknown" : string[1];

        	nameEditText.setEnabled(true);
        	uriEditText.setEnabled(true);

        	if(computerIpAddress.equals(""))
        	{
        		mTools.showToast(getString(R.string.add_computer_installation_not_found), 1);

        		nameEditText.setText(getString(R.string.add_computer_name_text), TextView.BufferType.EDITABLE);
        		uriEditText.setText(getString(R.string.add_computer_uri_text), TextView.BufferType.EDITABLE);
        	}
        	else
        	{
        		String foundOn = (computerHostname.equals("Computer")) ? getString(R.string.add_computer_computer_with_authentication_enabled) : computerHostname;

        		mTools.showToast(getString(R.string.add_computer_installation_found)+" "+foundOn, 1);

        		nameEditText.setText(computerHostname, TextView.BufferType.EDITABLE);
        		uriEditText.setText("http://"+computerIpAddress+"/spotcommander/", TextView.BufferType.EDITABLE);
        	}
        }

        @Override
        protected void onCancelled()
        {
        	if(mWakeLock.isHeld()) mWakeLock.release();

            mMenuItem.setTitle(getString(R.string.add_computer_menu_scan));

            mProgressBar.setVisibility(View.GONE);

			nameEditText.setEnabled(true);
			nameEditText.setText(getString(R.string.add_computer_name_text), TextView.BufferType.EDITABLE);

			uriEditText.setEnabled(true);
			uriEditText.setText(getString(R.string.add_computer_uri_text), TextView.BufferType.EDITABLE);
        }

        @Override
        protected String[] doInBackground(String... strings)
        {
        	String wifiSubnet = strings[0];

            String[] networkScanResult = {"", ""};

            outerLoop: for(int i = 1; i <= 254; i++)
            {
                if(isCancelled()) break;

                String computerIpAddress = wifiSubnet+"."+i;

                publishProgress(computerIpAddress);

                HttpURLConnection httpURLConnection = null;

                try
                {
                    URL url = new URL("http://"+computerIpAddress+"/spotcommander/main.php?hostname");

                    httpURLConnection = (HttpURLConnection) url.openConnection();

                    httpURLConnection.setConnectTimeout(500);
                    httpURLConnection.setReadTimeout(2500);

                    InputStream inputStream = new BufferedInputStream(httpURLConnection.getInputStream());

                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                    StringBuilder stringBuilder = new StringBuilder();

                    String line;

                    while((line = bufferedReader.readLine()) != null)
                    {
                        stringBuilder.append(line);
                    }

                    String computerHostname = stringBuilder.toString();

                    Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
                    Set<String> headerFieldsKeys = headerFields.keySet();

                    for(String headerKey : headerFieldsKeys)
                    {
                        String headerString = headerFields.get(headerKey).toString();

                        if(headerString.contains("TP-LINK") || headerString.contains("ZyXEL")) continue outerLoop;
                    }

                    if(computerHostname.contains("html")) continue;

                    networkScanResult[0] = computerIpAddress;
                    networkScanResult[1] = computerHostname;

                    break;
                }
                catch(Exception e1)
                {
                    try
                    {
                        if(httpURLConnection != null)
                        {
                            int responseCode = httpURLConnection.getResponseCode();

                            if(responseCode == 401)
                            {
                                String computerHostname = "Computer";

                                String headerField = httpURLConnection.getHeaderField("WWW-Authenticate");

                                if(!headerField.contains(getString(R.string.project_name))) continue;

                                networkScanResult[0] = computerIpAddress;
                                networkScanResult[1] = computerHostname;

                                break;
                            }
                        }
                    }
                    catch(Exception e2)
                    {
                        Log.w("AddComputerActivity", getString(R.string.add_computer_installation_not_found)+": "+computerIpAddress);
                    }
                }
                finally
                {
                    if(httpURLConnection != null) httpURLConnection.disconnect();
                }
            }

            return networkScanResult;
        }
    }
}