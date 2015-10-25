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
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

public class PlaylistsActivity extends Activity
{
    private final Context mContext = this;

    private final MyTools mTools = new MyTools(mContext);

    private ProgressBar mProgressBar;
    private ListView mListView;

    private final ArrayList<String> mPlaylistUris = new ArrayList<>();

    // Create activity
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Allow landscape?
        if(!mTools.allowLandscape()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Intent
        final Intent intent = getIntent();

        // Computer
        final long computerId = mTools.getSharedPreferencesLong("WIDGET_"+intent.getStringExtra(WidgetLarge.WIDGET_LARGE_INTENT_EXTRA)+"_COMPUTER_ID");

        final String[] computer = mTools.getComputer(computerId);

        // Layout
        setContentView(R.layout.activity_playlists);

        // Progress bar
        mProgressBar = (ProgressBar) findViewById(R.id.playlists_progressbar);
        mProgressBar.setVisibility(View.VISIBLE);

        // Listview
        mListView = (ListView) findViewById(R.id.playlists_list);

        // Get playlists
        RequestQueue requestQueue = Volley.newRequestQueue(mContext);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, computer[0]+"/playlists.php?get_playlists_as_json", new Response.Listener<JSONObject>()
        {
            @Override
            public void onResponse(JSONObject response)
            {
                try
                {
                    ArrayList<String> playlistNamesArrayList = new ArrayList<>();

                    Iterator<?> iterator = response.keys();

                    while(iterator.hasNext())
                    {
                        String key = (String) iterator.next();

                        playlistNamesArrayList.add(key);
                    }

                    Collections.sort(playlistNamesArrayList, new Comparator<String>()
                    {
                        @Override
                        public int compare(String string1, String string2)
                        {
                            return string1.compareToIgnoreCase(string2);
                        }
                    });

                    for(String playlistName : playlistNamesArrayList)
                    {
                        mPlaylistUris.add(response.getString(playlistName));
                    }

                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(mContext, R.layout.activity_playlists_list_item, playlistNamesArrayList);

                    mProgressBar.setVisibility(View.GONE);

                    mListView.setAdapter(arrayAdapter);
                    mListView.setVisibility(View.VISIBLE);

                    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                        {
                            mTools.remoteControl(computerId, "shuffle_play_uri", mPlaylistUris.get(position));
                        }
                    });
                }
                catch(Exception e)
                {
                    mProgressBar.setVisibility(View.GONE);

                    TextView textView = (TextView) findViewById(R.id.playlists_error);
                    textView.setVisibility(View.VISIBLE);
                }
            }
        }, new Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                mProgressBar.setVisibility(View.GONE);

                TextView textView = (TextView) findViewById(R.id.playlists_error);
                textView.setVisibility(View.VISIBLE);
            }
        })
        {
            @Override
            public HashMap<String, String> getHeaders()
            {
                HashMap<String, String> hashMap = new HashMap<>();

                if(!computer[1].equals("") && !computer[2].equals("")) hashMap.put("Authorization", "Basic "+Base64.encodeToString((computer[1]+":"+computer[2]).getBytes(), Base64.NO_WRAP));

                return hashMap;
            }
        };

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(2500, 0, 0));

        requestQueue.add(jsonObjectRequest);
    }
}