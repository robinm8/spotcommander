package net.olejon.spotcommander;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

public class WidgetLarge extends AppWidgetProvider
{
    public final static String WIDGET_LARGE_INTENT_EXTRA = "net.olejon.spotcommander.WIDGET_LARGE_INTENT_EXTRA";

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

            Intent previousIntent = new Intent(context, WidgetLarge.class);
            previousIntent.setAction("previous");
            previousIntent.putExtra(WIDGET_LARGE_INTENT_EXTRA, new String[] {id, "previous", ""});
            PendingIntent previousPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, previousIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent playPauseIntent = new Intent(context, WidgetLarge.class);
            playPauseIntent.setAction("play_pause");
            playPauseIntent.putExtra(WIDGET_LARGE_INTENT_EXTRA, new String[] {id, "play_pause", ""});
            PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(context, appWidgetId, playPauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent nextIntent = new Intent(context, WidgetLarge.class);
            nextIntent.setAction("next");
            nextIntent.putExtra(WIDGET_LARGE_INTENT_EXTRA, new String[] {id, "next", ""});
            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent launchQuitIntent = new Intent(context, WidgetLarge.class);
            launchQuitIntent.setAction("launch_quit");
            launchQuitIntent.putExtra(WIDGET_LARGE_INTENT_EXTRA, new String[] {id, "launch_quit", ""});
            PendingIntent launchQuitPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, launchQuitIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent volumeMuteIntent = new Intent(context, WidgetLarge.class);
            volumeMuteIntent.setAction("volume_mute");
            volumeMuteIntent.putExtra(WIDGET_LARGE_INTENT_EXTRA, new String[] {id, "adjust_spotify_volume", "mute"});
            PendingIntent volumeMutePendingIntent = PendingIntent.getBroadcast(context, appWidgetId, volumeMuteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent volumeDownIntent = new Intent(context, WidgetLarge.class);
            volumeDownIntent.setAction("volume_down");
            volumeDownIntent.putExtra(WIDGET_LARGE_INTENT_EXTRA, new String[] {id, "adjust_spotify_volume", "down"});
            PendingIntent volumeDownPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, volumeDownIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent volumeUpIntent = new Intent(context, WidgetLarge.class);
            volumeUpIntent.setAction("volume_up");
            volumeUpIntent.putExtra(WIDGET_LARGE_INTENT_EXTRA, new String[] {id, "adjust_spotify_volume", "up"});
            PendingIntent volumeUpPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, volumeUpIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Intent playlistsIntent = new Intent(context, PlaylistsActivity.class);
            playlistsIntent.putExtra(WIDGET_LARGE_INTENT_EXTRA, id);
            PendingIntent playlistsPendingIntent = PendingIntent.getActivity(context, appWidgetId, playlistsIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_large);

            views.setOnClickPendingIntent(R.id.widget_launcher_button, launchPendingIntent);
            views.setOnClickPendingIntent(R.id.widget_previous_button, previousPendingIntent);
            views.setOnClickPendingIntent(R.id.widget_play_button, playPausePendingIntent);
            views.setOnClickPendingIntent(R.id.widget_next_button, nextPendingIntent);
            views.setOnClickPendingIntent(R.id.widget_power_button, launchQuitPendingIntent);
            views.setOnClickPendingIntent(R.id.widget_volume_mute_button, volumeMutePendingIntent);
            views.setOnClickPendingIntent(R.id.widget_volume_down_button, volumeDownPendingIntent);
            views.setOnClickPendingIntent(R.id.widget_volume_up_button, volumeUpPendingIntent);
            views.setOnClickPendingIntent(R.id.widget_playlists_button, playlistsPendingIntent);

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
            String[] action = intent.getStringArrayExtra(WIDGET_LARGE_INTENT_EXTRA);

            long computerId = mMethod.getSharedPreferencesLong("WIDGET_"+action[0]+"_COMPUTER_ID");

            mMethod.remoteControl(computerId, action[1], action[2]);
        }
    }
}