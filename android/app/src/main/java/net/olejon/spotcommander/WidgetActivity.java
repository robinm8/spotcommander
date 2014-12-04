package net.olejon.spotcommander;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class WidgetActivity extends ActionBarActivity
{	
	private final MyMethod mMethod = new MyMethod(this);
	
	private SQLiteDatabase mDatabase;
	private Cursor mCursor;
	private ListView listView;
	
	private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	
	// Create activity
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		// Allow landscape?
		if(!mMethod.allowLandscape()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		// Database
		mDatabase = new MySQLiteHelper(this).getWritableDatabase();
		
		// Intent
		setResult(RESULT_CANCELED);
		
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		
		if(extras != null) appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		
        if(appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) finish();
		
		// Layout
		setContentView(R.layout.activity_widget);

        // Toolbar
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
		
		// Listview
		listView = (ListView) findViewById(R.id.list);
		
		View listViewHeader = getLayoutInflater().inflate(R.layout.header_main, listView, false);
		listView.addHeaderView(listViewHeader, null, false);

        View listViewEmpty = findViewById(R.id.empty);
        listView.setEmptyView(listViewEmpty);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView< ?> parent, View view, int position, long id)
            {
                mMethod.setSharedPreferencesLong("WIDGET_"+appWidgetId+"_COMPUTER_ID", id);

                Intent result = new Intent();
                result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                setResult(RESULT_OK, result);

                finish();
            }
       });

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
}