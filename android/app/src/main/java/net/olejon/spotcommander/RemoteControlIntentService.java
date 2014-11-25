package net.olejon.spotcommander;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

public class RemoteControlIntentService extends IntentService
{
	public final static String REMOTE_CONTROL_INTENT_SERVICE_EXTRA = "net.olejon.spotcommander.REMOTE_CONTROL_INTENT_SERVICE_EXTRA";
	
	private final MyMethod mMethod = new MyMethod(this);

	public RemoteControlIntentService()
	{
		super("RemoteControlIntentService");
	}
	
	// Intent
	@Override
	protected void onHandleIntent(Intent intent)
	{
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		long computerId = intent.getLongExtra(REMOTE_CONTROL_INTENT_SERVICE_EXTRA, 0);
		
		String action = intent.getAction();
		
		if(action.equals("hide_notification"))
		{
			notificationManager.cancel(WebViewActivity.NOTIFICATION_ID);
		}
		else
		{
			mMethod.remoteControl(computerId, action, "");
		}
	}
}