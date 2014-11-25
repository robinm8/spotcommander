package net.olejon.spotcommander;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

public class MainActivity extends ActionBarActivity
{	
	private final MyMethod mMethod = new MyMethod(this);
	
	private SQLiteDatabase mDatabase;
	private Cursor mCursor;
	private ListView listView;
	private ActionMode actionMode;
	
	private String currentNetwork;
	
	// Create activity
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		// Allow landscape?
		if(!mMethod.allowLandscape()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		// Settings
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);
		
		// Database
		mDatabase = new MySQLiteHelper(this).getWritableDatabase();
		
		// Open last computer
		currentNetwork = mMethod.getCurrentNetwork();
		
		boolean openAutomatically = mMethod.getDefaultSharedPreferencesBoolean("OPEN_AUTOMATICALLY");
		boolean networkHasChanged = (!currentNetwork.equals(mMethod.getSharedPreferencesString("LAST_NETWORK_ID")));
		
		long lastComputerId = mMethod.getSharedPreferencesLong("LAST_COMPUTER_ID");
		
		if(openAutomatically && !networkHasChanged && savedInstanceState == null) openApp(lastComputerId, false);
		
		// Layout
		setContentView(R.layout.activity_main);

        // Toolbar
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(getString(R.string.main_title));
        setSupportActionBar(mToolbar);
		
		// Listview
		listView = (ListView) findViewById(R.id.list);
		
		View listViewHeader = getLayoutInflater().inflate(R.layout.header_main, listView, false);
		listView.addHeaderView(listViewHeader, null, false);

        View listViewEmpty = findViewById(R.id.empty);
        listView.setEmptyView(listViewEmpty);

        listView.setLongClickable(true);

        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView< ?> parent, View view, int position, long id)
            {
                if(actionMode != null)
                {
                    startMyActionMode(id);
                    return;
                }

                openApp(id, true);
            }
        });

        listView.setOnItemLongClickListener(new OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id)
            {
                if(actionMode != null) return false;

                startMyActionMode(id);
                return true;
            }
        });

        ImageButton fab = (ImageButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(getApplicationContext(), AddComputerActivity.class);
                startActivity(intent);
            }
        });

        Button makeDonationButton = (Button) findViewById(R.id.make_donation_button);

        makeDonationButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(getApplicationContext(), DonateActivity.class);
                startActivity(intent);
            }
        });

        // Dialog
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
        {
            boolean skipDialog = mMethod.getSharedPreferencesBoolean("SKIP_INFORMATION_DIALOG");

            if(!skipDialog)
            {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

                alertDialogBuilder.setTitle(R.string.main_dialog_title).setMessage(R.string.main_dialog_message).setPositiveButton(R.string.main_dialog_positive_button, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        mMethod.setSharedPreferencesBoolean("SKIP_INFORMATION_DIALOG", true);

                        dialogInterface.dismiss();
                    }
                }).create().show();
            }
        }
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
		
		mCursor.close();
		mDatabase.close();
    }

	// Menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
        getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if(item.getItemId() == R.id.action_settings)
		{
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);

			return true;
		}
        else if(item.getItemId() == R.id.action_troubleshooting)
        {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.olejon.net/code/spotcommander/?troubleshooting"));
            startActivity(intent);

            return true;
        }
        else if(item.getItemId() == R.id.action_report_issue)
        {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.olejon.net/code/spotcommander/?issues"));
            startActivity(intent);

            return true;
        }
        else if(item.getItemId() == R.id.action_make_donation)
        {
            Intent intent = new Intent(this, DonateActivity.class);
            startActivity(intent);

            return true;
        }
		
		return super.onOptionsItemSelected(item);
	}
	
	// Action mode
	private void startMyActionMode(long id)
	{	
		String[] queryColumns = {MySQLiteHelper.COLUMN_ID, MySQLiteHelper.COLUMN_NAME};
		mCursor = mDatabase.query(MySQLiteHelper.TABLE_COMPUTERS, queryColumns, MySQLiteHelper.COLUMN_ID+" = "+id, null, null, null, null);

		if(mCursor.moveToFirst())
		{
			String name = mCursor.getString(mCursor.getColumnIndexOrThrow(MySQLiteHelper.COLUMN_NAME));
			actionMode = startSupportActionMode(new MyActionMode(id, name));
		}
	}
	
	private final class MyActionMode implements ActionMode.Callback
	{
		final long itemId;
		final String itemName;
		
		public MyActionMode(long id, String name)
		{
			itemId = id;
			itemName = name;
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu)
		{
			getMenuInflater().inflate(R.menu.main_actionmode, menu);
			return true;
		}
	
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu)
		{
			mode.setTitle(itemName);
			return true;
		}
	
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item)
		{
			removeComputer(itemId);
			mode.finish();
			listComputers();
			return true;
		}
	
		@Override
		public void onDestroyActionMode(ActionMode mode)
		{
			actionMode = null;
		}
	}

    // Open app
    private void openApp(long id, boolean animation)
    {
        mMethod.setSharedPreferencesLong("LAST_COMPUTER_ID", id);
        mMethod.setSharedPreferencesString("LAST_NETWORK_ID", currentNetwork);

        Intent intent = new Intent(this, WebViewActivity.class);
        if(!animation) intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    // Computers
    private void listComputers()
    {
        String[] queryColumns = {MySQLiteHelper.COLUMN_ID, MySQLiteHelper.COLUMN_NAME};
        mCursor = mDatabase.query(MySQLiteHelper.TABLE_COMPUTERS, queryColumns, null, null, null, null, null);

        String[] fromColumns = {MySQLiteHelper.COLUMN_NAME};
        int[] toViews = {R.id.list_item_textview};

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.list_item_computer, mCursor, fromColumns, toViews, 0);
        listView.setAdapter(adapter);
    }

    private void removeComputer(long id)
    {
        mDatabase = new MySQLiteHelper(this).getWritableDatabase();
        mDatabase.delete(MySQLiteHelper.TABLE_COMPUTERS, MySQLiteHelper.COLUMN_ID + " = " + id, null);
    }
}