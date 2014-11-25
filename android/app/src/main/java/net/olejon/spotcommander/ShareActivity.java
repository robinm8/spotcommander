package net.olejon.spotcommander;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ShareActivity extends Activity
{
	private final MyMethod mMethod = new MyMethod(this);
	
	private String uri;
	
	// Create activity
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		// Intent
		Intent intent = getIntent();
		String intentAction = intent.getAction();
		String intentType = intent.getType();
		
		if(Intent.ACTION_SEND.equals(intentAction) && intentType != null)
		{
			uri = intent.getStringExtra(Intent.EXTRA_TEXT);
		}
		else if(Intent.ACTION_VIEW.equals(intentAction))
		{
			uri = intent.getData().toString();
		}

		// Share uri
		if(uri != null)
		{
			mMethod.setSharedPreferencesString("SHARE_URI", uri);
			
			if(WebViewActivity.activityIsPaused)
			{
				Intent launchActivityIntent = new Intent(this, WebViewActivity.class);
				launchActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				startActivity(launchActivityIntent);
			}
			else
			{
				Intent launchActivityIntent = new Intent(this, MainActivity.class);
				launchActivityIntent.setAction("android.intent.action.MAIN");
				launchActivityIntent.addCategory("android.intent.category.LAUNCHER");
				launchActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
				startActivity(launchActivityIntent);
			}
		}
		else
		{
			finish();
		}
	}
}