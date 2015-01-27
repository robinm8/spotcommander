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
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class SettingsActivity extends PreferenceActivity
{
    private final Activity mActivity = this;

	private final MyTools mTools = new MyTools(this);
	
	// Create activity
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		// Allow landscape?
		if(!mTools.allowLandscape()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		// Layout
		addPreferencesFromResource(R.xml.settings);
		setContentView(R.layout.activity_settings);

        // Toolbar
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        toolbar.setTitle(getString(R.string.settings_title));

        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));

        toolbar.setNavigationOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                NavUtils.navigateUpFromSameTask(mActivity);
            }
        });
	}
}