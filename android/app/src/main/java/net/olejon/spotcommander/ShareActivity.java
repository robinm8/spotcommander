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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class ShareActivity extends Activity
{
    private final Context mContext = this;

	private final MyTools mTools = new MyTools(mContext);

	private String mUri;

	// Create activity
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Intent
		Intent intent = getIntent();
		String intentAction = intent.getAction();

		if(intentAction != null && intentAction.equals(Intent.ACTION_SEND))
		{
            mUri = intent.getStringExtra(Intent.EXTRA_TEXT);
		}
		else if(intentAction != null && intentAction.equals(Intent.ACTION_VIEW))
		{
            mUri = intent.getData().toString();
		}

		// Share uri
		if(mUri == null)
        {
            finish();
        }
        else
        {
            mTools.setSharedPreferencesString("SHARE_URI", mUri);
			
			if(WebViewActivity.ACTIVITY_IS_PAUSED)
			{
				Intent launchActivityIntent = new Intent(mContext, WebViewActivity.class);
				launchActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				startActivity(launchActivityIntent);
			}
			else
			{
				Intent launchActivityIntent = new Intent(mContext, MainActivity.class);
				launchActivityIntent.setAction("android.intent.action.MAIN");
				launchActivityIntent.addCategory("android.intent.category.LAUNCHER");
				launchActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION);
				startActivity(launchActivityIntent);
			}
		}
	}
}