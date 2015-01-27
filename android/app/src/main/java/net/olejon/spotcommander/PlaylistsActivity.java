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
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class PlaylistsActivity extends Activity
{
    private final MyTools mTools = new MyTools(this);

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
        Intent intent = getIntent();

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

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                mTools.remoteControl(computerId, "shuffle_play_uri", mPlaylistUris.get(position));
            }
        });

        // Get playlists
        GetPlaylistsTask getPlaylistsTask = new GetPlaylistsTask();
        getPlaylistsTask.execute(computer[0], computer[1], computer[2]);
    }

    private class GetPlaylistsTask extends AsyncTask<String, Void, String>
    {
        @Override
        protected void onPostExecute(String response)
        {
            if(response.equals(""))
            {
                mProgressBar.setVisibility(View.GONE);

                TextView textView = (TextView) findViewById(R.id.playlists_error);
                textView.setVisibility(View.VISIBLE);
            }
            else
            {
                try
                {
                    JSONObject playlistsJsonObject = new JSONObject(response);

                    ArrayList<String> playlistNamesArrayList = new ArrayList<>();

                    Iterator<?> iterator = playlistsJsonObject.keys();

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

                    Collections.swap(playlistNamesArrayList, playlistNamesArrayList.indexOf("Starred"), 0);

                    for(String playlistName : playlistNamesArrayList)
                    {
                        mPlaylistUris.add(playlistsJsonObject.getString(playlistName));
                    }

                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.playlists_list_item, playlistNamesArrayList);

                    mProgressBar.setVisibility(View.GONE);

                    mListView.setAdapter(arrayAdapter);
                    mListView.setVisibility(View.VISIBLE);
                }
                catch(Exception e)
                {
                    mProgressBar.setVisibility(View.GONE);

                    TextView textView = (TextView) findViewById(R.id.playlists_error);
                    textView.setVisibility(View.VISIBLE);

                    Log.e("PlaylistsActivity", Log.getStackTraceString(e));
                }
            }
        }

        @Override
        protected String doInBackground(String... strings)
        {
            String uri = strings[0];
            String username = strings[1];
            String password = strings[2];

            HttpParams httpParameters = new BasicHttpParams();

            HttpConnectionParams.setConnectionTimeout(httpParameters, 2000);
            HttpConnectionParams.setSoTimeout(httpParameters, 20000);

            HttpClient httpClient = new DefaultHttpClient(httpParameters);

            HttpGet httpGet = new HttpGet(uri+"/playlists.php?get_playlists_including_starred_as_json");
            httpGet.setHeader("Authorization", "Basic "+ Base64.encodeToString((username+":"+password).getBytes(), Base64.NO_WRAP));

            String response;

            try
            {
                HttpResponse httpResponse = httpClient.execute(httpGet);

                response = EntityUtils.toString(httpResponse.getEntity(), HTTP.UTF_8);
            }
            catch(Exception e)
            {
                response = "";

                Log.e("PlaylistsActivity", Log.getStackTraceString(e));
            }

            return response;
        }
    }
}