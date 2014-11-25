package net.olejon.spotcommander;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

final class MySQLiteHelper extends SQLiteOpenHelper
{
	public static final String TABLE_COMPUTERS = "computers";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_URI = "uri";
	public static final String COLUMN_USERNAME = "username";
	public static final String COLUMN_PASSWORD = "password";
	private static final String COLUMN_NETWORK_NAME = "network_name";
	private static final String COLUMN_NETWORK_DEFAULT = "network_default";
	
	public MySQLiteHelper(Context context)
	{
		super(context, "database.db", null, 9);
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL("CREATE TABLE "+TABLE_COMPUTERS+"("+COLUMN_ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+COLUMN_NAME+" TEXT, "+COLUMN_URI+" TEXT, "+COLUMN_USERNAME+" TEXT, "+COLUMN_PASSWORD+" TEXT, "+COLUMN_NETWORK_NAME+" TEXT, "+COLUMN_NETWORK_DEFAULT+" INTEGER);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
	    db.execSQL("DROP TABLE IF EXISTS "+TABLE_COMPUTERS);
	    onCreate(db);
	}
}