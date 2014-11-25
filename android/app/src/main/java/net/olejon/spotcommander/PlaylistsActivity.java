package net.olejon.spotcommander;

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
    private final MyMethod mMethod = new MyMethod(this);

    private ProgressBar mProgress;
    private ListView listView;

    private final ArrayList<String> playlistUris = new ArrayList<String>();

    // Create activity
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Allow landscape?
        if(!mMethod.allowLandscape()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Intent
        Intent intent = getIntent();
        String stringIntentExtra = intent.getStringExtra(WidgetLarge.WIDGET_LARGE_INTENT_EXTRA);

        // Computer
        final long computerId = mMethod.getSharedPreferencesLong("WIDGET_"+stringIntentExtra+"_COMPUTER_ID");
        final String[] computer = mMethod.getComputer(computerId);

        // Layout
        setContentView(R.layout.activity_playlists);

        // Progress bar
        mProgress = (ProgressBar) findViewById(R.id.progressbar);
        mProgress.setVisibility(View.VISIBLE);

        // Listview
        listView = (ListView) findViewById(R.id.list);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                mMethod.remoteControl(computerId, "shuffle_play_uri", playlistUris.get(position));
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
                mProgress.setVisibility(View.GONE);

                TextView textView = (TextView) findViewById(R.id.error);
                textView.setVisibility(View.VISIBLE);
            }
            else
            {
                try
                {
                    JSONObject playlists = new JSONObject(response);
                    ArrayList<String> playlistNames = new ArrayList<String>();
                    Iterator<?> keys = playlists.keys();

                    while(keys.hasNext())
                    {
                        String key = (String) keys.next();

                        playlistNames.add(key);
                    }

                    Collections.sort(playlistNames, new Comparator<String>()
                    {
                        @Override
                        public int compare(String string1, String string2)
                        {
                            return string1.compareToIgnoreCase(string2);
                        }
                    });

                    Collections.swap(playlistNames, playlistNames.indexOf("Starred"), 0);

                    for(String playlistName : playlistNames) playlistUris.add(playlists.getString(playlistName));

                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.list_item_playlist, playlistNames);

                    mProgress.setVisibility(View.GONE);

                    listView.setAdapter(arrayAdapter);
                }
                catch(Exception e)
                {
                    mProgress.setVisibility(View.GONE);

                    TextView textView = (TextView) findViewById(R.id.error);
                    textView.setVisibility(View.VISIBLE);

                    Log.e("GetPlaylistsTask onPostExecute", Log.getStackTraceString(e));
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
            httpGet.setHeader("Authorization", "Basic "+ Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP));

            String response;

            try
            {
                HttpResponse httpResponse = httpClient.execute(httpGet);

                response = EntityUtils.toString(httpResponse.getEntity(), HTTP.UTF_8);
            }
            catch(Exception e)
            {
                response = "";

                Log.e("GetPlaylistsTask doInBackground", Log.getStackTraceString(e));
            }

            return response;
        }
    }
}