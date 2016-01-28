package net.olejon.spotcommander;

/*

Copyright 2016 Ole Jon Bjørkum

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
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity
{
    private final Context mContext = this;

	private final MyTools mTools = new MyTools(mContext);

	private SQLiteDatabase mDatabase;
	private Cursor mCursor;

    private FloatingActionButton mFloatingActionButton;
	private ListView mListView;

	private String mCurrentNetwork;

	// Create activity
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Allow landscape?
		if(!mTools.allowLandscape()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// Settings
		PreferenceManager.setDefaultValues(mContext, R.xml.settings, false);

		// Database
		mDatabase = new MySQLiteHelper(mContext).getWritableDatabase();

		// Open last computer
        mCurrentNetwork = mTools.getCurrentNetwork();

		if(mTools.getDefaultSharedPreferencesBoolean("OPEN_AUTOMATICALLY") && mCurrentNetwork.equals(mTools.getSharedPreferencesString("LAST_NETWORK_ID")) && savedInstanceState == null) openApp(mTools.getSharedPreferencesLong("LAST_COMPUTER_ID"), false);

		// Layout
		setContentView(R.layout.activity_main);

        // Toolbar
        final Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.setTitle(getString(R.string.main_title));

        setSupportActionBar(toolbar);

		// Listview
        mListView = (ListView) findViewById(R.id.main_list);

		View listViewHeader = getLayoutInflater().inflate(R.layout.activity_main_subheader, mListView, false);
        mListView.addHeaderView(listViewHeader, null, false);

        View listViewEmpty = findViewById(R.id.main_empty);
        mListView.setEmptyView(listViewEmpty);

        mListView.setLongClickable(true);

        mListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView< ?> parent, View view, int position, long id)
            {
                openApp(id, true);
            }
        });

        mListView.setOnItemLongClickListener(new OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, final long id)
            {
                new MaterialDialog.Builder(mContext).title(getString(R.string.main_remove_computer_dialog_title)).content(getString(R.string.main_remove_computer_dialog_message)).positiveText(getString(R.string.main_remove_computer_dialog_positive_button)).negativeText(getString(R.string.main_remove_computer_dialog_negative_button)).onPositive(new MaterialDialog.SingleButtonCallback()
                {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction)
                    {
                        removeComputer(id);

                        listComputers();
                    }
                }).contentColorRes(R.color.black).negativeColorRes(R.color.black).show();

                return true;
            }
        });

        // Floating action button
        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.main_fab);

        mFloatingActionButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(mContext, AddComputerActivity.class);
                startActivity(intent);
            }
        });

        // Make donation button
        Button button = (Button) findViewById(R.id.main_make_donation_button);

        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(mContext, DonateActivity.class);
                startActivity(intent);
            }
        });

        // Dialog
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
        {
            if(!mTools.getSharedPreferencesBoolean("SKIP_INFORMATION_DIALOG"))
            {
                new MaterialDialog.Builder(mContext).title(getString(R.string.main_information_dialog_title)).content(getString(R.string.main_information_dialog_message)).positiveText(getString(R.string.main_information_dialog_positive_button)).onPositive(new MaterialDialog.SingleButtonCallback()
                {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction)
                    {
                        mTools.setSharedPreferencesBoolean("SKIP_INFORMATION_DIALOG", true);
                    }
                }).contentColorRes(R.color.black).show();
            }
        }

        // Message
        RequestQueue requestQueue = Volley.newRequestQueue(mContext);

        String projectVersionName = mTools.getProjectVersionName();

        String device = "";

        try
        {
            device = (Build.MANUFACTURER == null || Build.MODEL == null || Build.VERSION.SDK_INT < 1) ? "" : URLEncoder.encode(Build.MANUFACTURER+" "+Build.MODEL+" "+Build.VERSION.SDK_INT, "utf-8");
        }
        catch(Exception e)
        {
            Log.e("MainActivity", Log.getStackTraceString(e));
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, getString(R.string.project_website)+"api/1/android/message/?version_name="+projectVersionName+"&device="+device, new Response.Listener<JSONObject>()
        {
            @Override
            public void onResponse(JSONObject response)
            {
                try
                {
                    final long id = response.getLong("id");
                    final String title = response.getString("title");
                    final String message = response.getString("message");

                    final long lastId = mTools.getSharedPreferencesLong("MESSAGE_LAST_ID");

                    if(lastId == 0)
                    {
                        mTools.setSharedPreferencesLong("MESSAGE_LAST_ID", id);
                    }
                    else if(id != lastId)
                    {
                        new MaterialDialog.Builder(mContext).title(title).content(message).positiveText(getString(R.string.main_message_dialog_positive_button)).onPositive(new MaterialDialog.SingleButtonCallback()
                        {
                            @Override
                            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction)
                            {
                                mTools.setSharedPreferencesLong("MESSAGE_LAST_ID", id);
                            }
                        }).contentColorRes(R.color.black).show();
                    }
                }
                catch(Exception e)
                {
                    Log.e("MainActivity", Log.getStackTraceString(e));
                }
            }
        }, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                Log.e("MainActivity", error.toString());
            }
        });

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(jsonObjectRequest);
	}

	// Resume activity
	@Override
	protected void onResume()
	{
		super.onResume();

		listComputers();
	}

	// Destroy activity
	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		if(mCursor != null && !mCursor.isClosed()) mCursor.close();
		if(mDatabase != null && mDatabase.isOpen()) mDatabase.close();
    }

	// Menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
        getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
        switch(item.getItemId())
        {
            case R.id.main_menu_settings:
            {
                Intent intent = new Intent(mContext, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.main_menu_troubleshooting:
            {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.olejon.net/code/spotcommander/?troubleshooting"));
                startActivity(intent);
                return true;
            }
            case R.id.main_menu_report_issue:
            {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.olejon.net/code/spotcommander/?issues"));
                startActivity(intent);
                return true;
            }
            case R.id.main_menu_make_donation:
            {
                Intent intent = new Intent(mContext, DonateActivity.class);
                startActivity(intent);
                return true;
            }
            default:
            {
                return super.onOptionsItemSelected(item);
            }
        }
	}

    // Open app
    private void openApp(long id, boolean animation)
    {
        mTools.setSharedPreferencesLong("LAST_COMPUTER_ID", id);
        mTools.setSharedPreferencesString("LAST_NETWORK_ID", mCurrentNetwork);

        Intent intent = new Intent(mContext, WebViewActivity.class);
        if(!animation) intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    // Computers
    private void listComputers()
    {
        String[] queryColumns = {MySQLiteHelper.COLUMN_ID, MySQLiteHelper.COLUMN_NAME};
        mCursor = mDatabase.query(MySQLiteHelper.TABLE_COMPUTERS, queryColumns, null, null, null, null, MySQLiteHelper.COLUMN_NAME);

        String[] fromColumns = {MySQLiteHelper.COLUMN_NAME};
        int[] toViews = {R.id.main_list_item};

        mListView.setAdapter(new SimpleCursorAdapter(mContext, R.layout.activity_main_list_item, mCursor, fromColumns, toViews, 0));

        Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.fab);

        mFloatingActionButton.startAnimation(animation);
        mFloatingActionButton.setVisibility(View.VISIBLE);
    }

    private void removeComputer(long id)
    {
        mDatabase.delete(MySQLiteHelper.TABLE_COMPUTERS, MySQLiteHelper.COLUMN_ID+" = "+id, null);
    }
}