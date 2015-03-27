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

import android.appwidget.AppWidgetManager;
import android.content.Context;
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
    private final Context mContext = this;

	private final MyTools mTools = new MyTools(mContext);

	private SQLiteDatabase mDatabase;
	private Cursor mCursor;

	private ListView mListView;

	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	// Create activity
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Allow landscape?
		if(!mTools.allowLandscape()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// Database
		mDatabase = new MySQLiteHelper(mContext).getReadableDatabase();

		// Intent
		setResult(RESULT_CANCELED);

		final Intent intent = getIntent();

		if(intent.getExtras() != null) mAppWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

        if(mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) finish();

		// Layout
		setContentView(R.layout.activity_widget);

        // Toolbar
        final Toolbar toolbar = (Toolbar) findViewById(R.id.widget_toolbar);

        setSupportActionBar(toolbar);

		// Listview
        mListView = (ListView) findViewById(R.id.widget_list);

		View listViewHeader = getLayoutInflater().inflate(R.layout.activity_main_subheader, mListView, false);
        mListView.addHeaderView(listViewHeader, null, false);

        View listViewEmpty = findViewById(R.id.widget_empty);
        mListView.setEmptyView(listViewEmpty);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                mTools.setSharedPreferencesLong("WIDGET_"+mAppWidgetId+"_COMPUTER_ID", id);

                Intent result = new Intent();
                result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
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

		if(mCursor != null && !mCursor.isClosed()) mCursor.close();
		if(mDatabase != null && mDatabase.isOpen()) mDatabase.close();
	}

	// Computers
	private void listComputers()
	{	
		String[] queryColumns = {MySQLiteHelper.COLUMN_ID, MySQLiteHelper.COLUMN_NAME};
		mCursor = mDatabase.query(MySQLiteHelper.TABLE_COMPUTERS, queryColumns, null, null, null, null, null);

		String[] fromColumns = {MySQLiteHelper.COLUMN_NAME};
		int[] toViews = {R.id.main_list_item};

        mListView.setAdapter(new SimpleCursorAdapter(mContext, R.layout.activity_main_list_item, mCursor, fromColumns, toViews, 0));
	}
}