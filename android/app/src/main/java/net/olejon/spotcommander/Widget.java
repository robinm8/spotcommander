package net.olejon.spotcommander;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

public class Widget extends AppWidgetProvider
{
	private final static String WIDGET_INTENT_EXTRA = "net.olejon.spotcommander.WIDGET_INTENT_EXTRA";

	// Update
	@Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        for(int appWidgetId : appWidgetIds)
        {
            String id = String.valueOf(appWidgetId);

            Intent launchIntent = new Intent(context, MainActivity.class);
            launchIntent.setAction("android.intent.action.MAIN");
            launchIntent.addCategory("android.intent.category.LAUNCHER");
            PendingIntent launchPendingIntent = PendingIntent.getActivity(context, appWidgetId, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent previousIntent = new Intent(context, Widget.class);
            previousIntent.setAction("previous");
            previousIntent.putExtra(WIDGET_INTENT_EXTRA, new String[] {id, "previous", ""});
            PendingIntent previousPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, previousIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent playPauseIntent = new Intent(context, Widget.class);
            playPauseIntent.setAction("play_pause");
            playPauseIntent.putExtra(WIDGET_INTENT_EXTRA, new String[] {id, "play_pause", ""});
            PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(context, appWidgetId, playPauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent nextIntent = new Intent(context, Widget.class);
            nextIntent.setAction("next");
            nextIntent.putExtra(WIDGET_INTENT_EXTRA, new String[] {id, "next", ""});
            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

            views.setOnClickPendingIntent(R.id.widget_launcher_button, launchPendingIntent);
            views.setOnClickPendingIntent(R.id.widget_previous_button, previousPendingIntent);
            views.setOnClickPendingIntent(R.id.widget_play_button, playPausePendingIntent);
            views.setOnClickPendingIntent(R.id.widget_next_button, nextPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
    
	// Receive
	@Override
	public void onReceive(@NonNull Context context, @NonNull Intent intent)
	{
		super.onReceive(context, intent);
		
		MyMethod mMethod = new MyMethod(context);

        String intentAction = intent.getAction();

        if(!intentAction.contains("android"))
        {
            String[] action = intent.getStringArrayExtra(WIDGET_INTENT_EXTRA);

            long computerId = mMethod.getSharedPreferencesLong("WIDGET_"+action[0]+"_COMPUTER_ID");

            mMethod.remoteControl(computerId, action[1], action[2]);
        }
	}
}