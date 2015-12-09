package net.olejon.spotcommander;

/*

Copyright 2015 Ole Jon Bjørkum

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see http://www.gnu.org/licenses/.

*/

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.webkit.CookieSyncManager;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class WebViewActivity extends Activity
{
    public static boolean ACTIVITY_IS_PAUSED = false;

	public final static int NOTIFICATION_ID = 1;

    private final Activity mActivity = this;

    private final Context mContext = this;

	private final MyTools mTools = new MyTools(mContext);

	private PowerManager.WakeLock mWakeLock;
	private NotificationCompat.Builder mNotificationBuilder;
	private NotificationManagerCompat mNotificationManager;
	private WebView mWebView;

	private String mProjectVersionName;
	private String mCurrentNetwork;

	private boolean mHasLongPressedBack = false;
	private boolean mPersistentNotification = false;
	private boolean mPersistentNotificationIsSupported = false;

    private int mStatusBarPrimaryColor = 0;
    private int mStatusBarCoverArtColor = 0;

	// Create activity
    protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Allow landscape?
		if(!mTools.allowLandscape()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// Hide status bar?
		if(mTools.getDefaultSharedPreferencesBoolean("HIDE_STATUS_BAR")) getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// Power manager
		PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        //noinspection deprecation
        mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "wakeLock");

		// Settings
        mTools.setSharedPreferencesBoolean("CAN_CLOSE_COVER", false);

		// Current network
        mCurrentNetwork = mTools.getCurrentNetwork();

		// Computer
		final long computerId = mTools.getSharedPreferencesLong("LAST_COMPUTER_ID");

		final String[] computer = mTools.getComputer(computerId);

		final String uri = computer[0];
		final String username = computer[1];
		final String password = computer[2];

		// Layout
		setContentView(R.layout.activity_webview);

        // Status bar color
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            mStatusBarPrimaryColor = getWindow().getStatusBarColor();
            mStatusBarCoverArtColor = mStatusBarPrimaryColor;
        }

		// Notification
        mPersistentNotificationIsSupported = (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN);

		if(mPersistentNotificationIsSupported)
		{
			Intent launchActivityIntent = new Intent(mContext, MainActivity.class);
			launchActivityIntent.setAction("android.intent.action.MAIN");
			launchActivityIntent.addCategory("android.intent.category.LAUNCHER");
	        PendingIntent launchActivityPendingIntent = PendingIntent.getActivity(mContext, 0, launchActivityIntent, PendingIntent.FLAG_CANCEL_CURRENT);

			Intent hideIntent = new Intent(mContext, RemoteControlIntentService.class);
			hideIntent.setAction("hide_notification");
			hideIntent.putExtra(RemoteControlIntentService.REMOTE_CONTROL_INTENT_SERVICE_EXTRA, computerId);
			PendingIntent hidePendingIntent = PendingIntent.getService(mContext, 0, hideIntent, PendingIntent.FLAG_CANCEL_CURRENT);

			Intent playPauseIntent = new Intent(mContext, RemoteControlIntentService.class);
			playPauseIntent.setAction("play_pause");
			playPauseIntent.putExtra(RemoteControlIntentService.REMOTE_CONTROL_INTENT_SERVICE_EXTRA, computerId);
			PendingIntent playPausePendingIntent = PendingIntent.getService(mContext, 0, playPauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);

			Intent nextIntent = new Intent(mContext, RemoteControlIntentService.class);
			nextIntent.setAction("next");
			nextIntent.putExtra(RemoteControlIntentService.REMOTE_CONTROL_INTENT_SERVICE_EXTRA, computerId);
			PendingIntent nextPendingIntent = PendingIntent.getService(mContext, 0, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);

            mNotificationManager = NotificationManagerCompat.from(mContext);
            mNotificationBuilder = new NotificationCompat.Builder(mContext);

            mNotificationBuilder.setWhen(0)
                    .setOngoing(true)
                    .setLargeIcon(largeIcon)
                    .setSmallIcon(R.drawable.ic_play_arrow_white_24dp)
                    .setContentTitle(getString(R.string.project_name))
                    .setContentText(getString(R.string.notification_text))
                    .setContentIntent(launchActivityPendingIntent)
                    .addAction(R.drawable.ic_close_white_24dp, getString(R.string.notification_action_hide), hidePendingIntent)
                    .addAction(R.drawable.ic_play_arrow_white_24dp, getString(R.string.notification_action_play_pause), playPausePendingIntent)
                    .addAction(R.drawable.ic_skip_next_white_24dp, getString(R.string.notification_action_next), nextPendingIntent);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) mNotificationBuilder.setPriority(Notification.PRIORITY_MAX);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
                mNotificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
                mNotificationBuilder.setCategory(Notification.CATEGORY_TRANSPORT);
            }
		}

		// Web view
        mWebView = (WebView) findViewById(R.id.webview_webview);

        mWebView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.background));
        mWebView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);

        mWebView.setWebViewClient(new WebViewClient()
		{
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url)
			{
				if(url != null && !url.contains(uri) && !url.contains("olejon.net/code/spotcommander/api/1/spotify/") && !url.contains("accounts.spotify.com/") && !url.contains("facebook.com/"))
				{
					view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

					return true;
				}

				return false;
			}

			@Override
			public void onReceivedHttpAuthRequest(WebView view, @NonNull HttpAuthHandler handler, String host, String realm)
			{	
				if(handler.useHttpAuthUsernamePassword())
				{
					handler.proceed(username, password);
				}
				else
				{
					handler.cancel();

                    mTools.showToast(getString(R.string.webview_authentication_failed), 1);

                    NavUtils.navigateUpFromSameTask(mActivity);
				}
			}

			@Override
			public void onReceivedSslError(WebView view, @NonNull SslErrorHandler handler, SslError error)
			{
				handler.proceed();
			}

			@Override
			public void onReceivedError(WebView view, WebResourceRequest webResourceRequest, WebResourceError webResourceError)
			{
                mTools.showToast(getString(R.string.webview_error), 1);

                NavUtils.navigateUpFromSameTask(mActivity);
			}
		});

		// Prepare user agent
        mProjectVersionName = mTools.getProjectVersionName();

		boolean authenticationIsEnabled = (!username.equals("") && !password.equals(""));

		String ua_append_1 = (authenticationIsEnabled) ? "AUTHENTICATION_ENABLED " : "";
        String ua_append_2 = "";
        String ua_append_3 = "";

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
        {
            ua_append_2 = (mTools.getDefaultSharedPreferencesBoolean("HARDWARE_ACCELERATED_ANIMATIONS")) ? "" : "DISABLE_CSSTRANSITIONS ";
            ua_append_3 = (mTools.getDefaultSharedPreferencesBoolean("HARDWARE_ACCELERATED_ANIMATIONS")) ? "" : "DISABLE_CSSTRANSFORMS3D ";
        }

		// Web settings
		WebSettings webSettings = mWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSupportZoom(false);
		webSettings.setUserAgentString(webSettings.getUserAgentString()+" "+getString(R.string.project_name)+"/"+mProjectVersionName+" "+ua_append_1+ua_append_2+ua_append_3);

		// Load app
		if(savedInstanceState != null)
		{
            mWebView.restoreState(savedInstanceState);
		}
		else
		{
            mWebView.loadUrl(uri);
		}

		// JavaScript interface
        mWebView.addJavascriptInterface(new JavaScriptInterface(), "Android");
	}

	// Pause activity
	@Override
	public void onPause()
	{
		super.onPause();

		if(mWakeLock.isHeld()) mWakeLock.release();

        mPersistentNotification = mTools.getSharedPreferencesBoolean("PERSISTENT_NOTIFICATION");

		if(mPersistentNotificationIsSupported && mPersistentNotification && !mHasLongPressedBack)
		{
			String nowplaying_artist = mTools.getSharedPreferencesString("NOWPLAYING_ARTIST");
			String nowplaying_title = mTools.getSharedPreferencesString("NOWPLAYING_TITLE");

			if(!nowplaying_artist.equals(getString(R.string.notification_no_music_is_playing_artist)))
			{
                mNotificationBuilder.setTicker(nowplaying_artist+" - "+nowplaying_title);
                mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build());
			}
		}

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
        {
            //noinspection deprecation
            CookieSyncManager.getInstance().sync();
        }

        mWebView.pauseTimers();

        ACTIVITY_IS_PAUSED = true;
	}

	// Resume activity
	@Override
	public void onResume()
	{
		super.onResume();

        mPersistentNotification = mTools.getSharedPreferencesBoolean("PERSISTENT_NOTIFICATION");

		if(mPersistentNotificationIsSupported && mPersistentNotification) mNotificationManager.cancel(NOTIFICATION_ID);

		if(mCurrentNetwork.equals(mTools.getCurrentNetwork()))
		{
            mWebView.resumeTimers();

			if(ACTIVITY_IS_PAUSED) mWebView.loadUrl("javascript:nativeAppLoad(true)");
		}
		else
		{
            mTools.showToast(getString(R.string.webview_network_changed), 1);

            NavUtils.navigateUpFromSameTask(mActivity);
		}
	}

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);

        mWebView.saveState(outState);
    }

    // Destroy activity
	@Override
	protected void onDestroy()
	{
		super.onDestroy();

        ACTIVITY_IS_PAUSED = false;
	}

	// Key down
	@Override
	public boolean onKeyDown(int keyCode, @NonNull KeyEvent event)
	{
        switch(keyCode)
        {
            case KeyEvent.KEYCODE_BACK:
            {
                event.startTracking();
                return true;
            }
            case KeyEvent.KEYCODE_MENU:
            {
                return true;
            }
            case KeyEvent.KEYCODE_SEARCH:
            {
                return true;
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            {
                return true;
            }
            case KeyEvent.KEYCODE_VOLUME_UP:
            {
                return true;
            }
            default:
            {
                return super.onKeyDown(keyCode, event);
            }
        }
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event)
	{
        switch(keyCode)
        {
            case KeyEvent.KEYCODE_BACK:
            {
                mHasLongPressedBack = true;

                NavUtils.navigateUpFromSameTask(mActivity);

                return true;
            }
            default:
            {
                return super.onKeyLongPress(keyCode, event);
            }
        }
	}

	// Key up
	@Override
	public boolean onKeyUp(int keyCode, @NonNull KeyEvent event)
	{
        switch(keyCode)
        {
            case KeyEvent.KEYCODE_MENU:
            {
                mWebView.loadUrl("javascript:nativeAppAction('menu')");
                return true;
            }
            case KeyEvent.KEYCODE_SEARCH:
            {
                mWebView.loadUrl("javascript:nativeAppAction('search')");
                return true;
            }
            case KeyEvent.KEYCODE_BACK:
            {
                if(mHasLongPressedBack) return true;

                if(mWebView.canGoBack() || mTools.getSharedPreferencesBoolean("CAN_CLOSE_COVER"))
                {
                    mWebView.loadUrl("javascript:nativeAppAction('back')");

                    return true;
                }

                mTools.showToast(getString(R.string.webview_back), 1);

                return true;
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            {
                mWebView.loadUrl("javascript:nativeAppAction('volume_down')");
                return true;
            }
            case KeyEvent.KEYCODE_VOLUME_UP:
            {
                mWebView.loadUrl("javascript:nativeAppAction('volume_up')");
                return true;
            }
            default:
            {
                return super.onKeyUp(keyCode, event);
            }
        }
	}

	// JavaScript interface
	private class JavaScriptInterface
	{
        @JavascriptInterface
        public void JSmakeDonation()
        {
            Intent intent = new Intent(mContext, DonateActivity.class);
            startActivity(intent);
        }

	    @JavascriptInterface
	    public void JSshare(String title, String uri)
	    {
	    	Intent intent = new Intent();
	    	intent.setAction(Intent.ACTION_SEND);
	    	intent.putExtra(Intent.EXTRA_TEXT, uri);
	    	intent.setType("text/plain");
	    	Intent chooser = Intent.createChooser(intent, title);
	    	startActivity(chooser);
	    }

	    @JavascriptInterface
	    public void JSopenUri(String uri)
	    {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(uri));
            startActivity(intent);
	    }

	    @JavascriptInterface
	    public int JSsearchApp(String app, String string)
	    {
            int appFound;

	    	try
	    	{
				getPackageManager().getApplicationInfo(app, 0);

		    	Intent intent = new Intent(Intent.ACTION_SEARCH);
		    	intent.setPackage(app);
		    	intent.putExtra("query", string);
		    	startActivity(intent);

                appFound = 1;
	    	}
	    	catch(Exception e)
	    	{
                appFound = 0;

                Log.e("WebViewActivity", Log.getStackTraceString(e));
	    	}

            return appFound;
	    }

	    @JavascriptInterface
	    public void JSkeepScreenOn(boolean keepScreenOn)
	    {
	    	if(keepScreenOn)
	    	{
	    		if(!mWakeLock.isHeld()) mWakeLock.acquire();
	    	}
	    	else
	    	{
	    		if(mWakeLock.isHeld()) mWakeLock.release();
	    	}
	    }

	    @JavascriptInterface
	    public void JSfinishActivity()
	    {
            mHasLongPressedBack = true;

            NavUtils.navigateUpFromSameTask(mActivity);
	    }

        @JavascriptInterface
        public void JSsetStatusBarColor(String color)
        {
            final int intColor;

            switch(color)
            {
                case "primary":
                {
                    intColor = mStatusBarPrimaryColor;
                    break;
                }
                case "cover_art":
                {
                    intColor = mStatusBarCoverArtColor;
                    break;
                }
                default:
                {
                    intColor = Color.parseColor(color);
                    break;
                }
            }

            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) getWindow().setStatusBarColor(intColor);
                }
            });
        }

        @JavascriptInterface
        public void JSsetStatusBarColorFromImage(String uri)
        {
            GetStatusBarColorFromImageTask getStatusBarColorFromImageTask = new GetStatusBarColorFromImageTask();
            getStatusBarColorFromImageTask.execute(uri);
        }

	    @JavascriptInterface
	    public void JSstartService()
	    {
			Intent intent = new Intent(mContext, RemoteControlService.class);
			startService(intent);
	    }

	    @JavascriptInterface
	    public String JSgetSharedString(String preference)
	    {
			return mTools.getSharedPreferencesString(preference);
	    }

	    @JavascriptInterface
	    public void JSsetSharedString(String preference, String string)
	    {
            mTools.setSharedPreferencesString(preference, string);
	    }

	    @JavascriptInterface
	    public boolean JSgetSharedBoolean(String preference)
	    {
	    	return mTools.getSharedPreferencesBoolean(preference);
	    }

	    @JavascriptInterface
	    public void JSsetSharedBoolean(String preference, boolean bool)
	    {
            mTools.setSharedPreferencesBoolean(preference, bool);
	    }

	    @JavascriptInterface
	    public String JSgetVersions()
	    {
	    	String project_version = mProjectVersionName;
	    	String project_minimum_version = getString(R.string.project_minimum_version);

	    	return new JSONArray(Arrays.asList(project_version, project_minimum_version)).toString();
	    }

	    @JavascriptInterface
	    public String JSgetPackageName()
	    {
			return mContext.getPackageName();
	    }
	}

    private class GetStatusBarColorFromImageTask extends AsyncTask<String, String, Bitmap>
    {
        @Override
        protected void onPostExecute(Bitmap bitmap)
        {
            if(bitmap == null)
            {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                {
                    getWindow().setStatusBarColor(ContextCompat.getColor(mContext, R.color.green));

                    mStatusBarCoverArtColor = R.color.black;
                }

                mWebView.loadUrl("javascript:setCoverArtFabColor('#689f38')");
            }
            else
            {
                Palette.from(bitmap).generate(new Palette.PaletteAsyncListener()
                {
                    public void onGenerated(Palette palette)
                    {
                        final int vibrantColor = palette.getVibrantColor(ContextCompat.getColor(mContext, R.color.black));

                        final String vibrantColorHex = String.format("#%06X", (0xFFFFFF & vibrantColor));

                        final float[] colorHsv = new float[3];

                        Color.colorToHSV(vibrantColor, colorHsv);
                        colorHsv[2] *= 0.8f;

                        final int darkVibrantColor = Color.HSVToColor(colorHsv);

                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                                {
                                    getWindow().setStatusBarColor(darkVibrantColor);

                                    mStatusBarCoverArtColor = darkVibrantColor;
                                }

                                mWebView.loadUrl("javascript:setCoverArtFabColor('"+vibrantColorHex+"')");
                            }
                        });
                    }
                });
            }
        }

        @Override
        protected Bitmap doInBackground(String... strings)
        {
            Bitmap bitmap = null;

            try
            {
                URL uri = new URL(strings[0]);

                HttpURLConnection httpURLConnection = (HttpURLConnection) uri.openConnection();

                httpURLConnection.setDoInput(true);
                httpURLConnection.setConnectTimeout(2000);
                httpURLConnection.setReadTimeout(10000);
                httpURLConnection.connect();

                InputStream inputStream = httpURLConnection.getInputStream();

                bitmap = BitmapFactory.decodeStream(inputStream);
            }
            catch(Exception e)
            {
                Log.e("WebViewActivity", Log.getStackTraceString(e));
            }

            return bitmap;
        }
    }
}