package net.olejon.spotcommander;

import android.app.Activity;
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
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.webkit.CookieSyncManager;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
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
	public final static int NOTIFICATION_ID = 1;
	
	public static boolean activityIsPaused = false;
	
	private final MyMethod mMethod = new MyMethod(this);

	private PowerManager.WakeLock wakeLock;
	private NotificationCompat.Builder notificationBuilder;
	private NotificationManagerCompat notificationManager;
	private WebView webView;
	
	private String appVersion;
	private String currentNetwork;
	
	private boolean hasLongPressedBack = false;
	private boolean persistentNotification = false;
	private boolean persistentNotificationIsSupported = false;

    private int statusBarPrimaryColor = 0;
    private int statusBarCoverArtColor = 0;

	// Create activity
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		// Allow landscape?
		if(!mMethod.allowLandscape()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		// Hide the status bar?
		boolean hideStatusBar = mMethod.getDefaultSharedPreferencesBoolean("HIDE_STATUS_BAR");
		if(hideStatusBar) getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		// Power manager
		PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "wakeLock");
		
		// Settings
		mMethod.setSharedPreferencesBoolean("CAN_CLOSE_COVER", false);
		
		// Wi-Fi
		currentNetwork = mMethod.getCurrentNetwork();
		
		// Computer
		final long computerId = mMethod.getSharedPreferencesLong("LAST_COMPUTER_ID");
		final String[] computer = mMethod.getComputer(computerId);
		
		final String uri = computer[0];
		final String username = computer[1];
		final String password = computer[2];
		
		// Layout
		setContentView(R.layout.activity_webview);

        // Status bar color
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            statusBarPrimaryColor = getWindow().getStatusBarColor();
            statusBarCoverArtColor = statusBarPrimaryColor;
        }
		
		// Notification
		persistentNotificationIsSupported = (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN);
		
		if(persistentNotificationIsSupported)
		{
			notificationManager = NotificationManagerCompat.from(this);
			
			Intent launchActivityIntent = new Intent(this, MainActivity.class);
			launchActivityIntent.setAction("android.intent.action.MAIN");
			launchActivityIntent.addCategory("android.intent.category.LAUNCHER");
	        PendingIntent launchActivityPendingIntent = PendingIntent.getActivity(this, 0, launchActivityIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			
			Intent hideIntent = new Intent(this, RemoteControlIntentService.class);
			hideIntent.setAction("hide_notification");
			hideIntent.putExtra(RemoteControlIntentService.REMOTE_CONTROL_INTENT_SERVICE_EXTRA, computerId);
			PendingIntent hidePendingIntent = PendingIntent.getService(this, 0, hideIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			
			Intent playPauseIntent = new Intent(this, RemoteControlIntentService.class);
			playPauseIntent.setAction("play_pause");
			playPauseIntent.putExtra(RemoteControlIntentService.REMOTE_CONTROL_INTENT_SERVICE_EXTRA, computerId);
			PendingIntent playPausePendingIntent = PendingIntent.getService(this, 0, playPauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			
			Intent nextIntent = new Intent(this, RemoteControlIntentService.class);
			nextIntent.setAction("next");
			nextIntent.putExtra(RemoteControlIntentService.REMOTE_CONTROL_INTENT_SERVICE_EXTRA, computerId);
			PendingIntent nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			
			notificationBuilder = new NotificationCompat.Builder(this);

            Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);

			notificationBuilder.setOngoing(true)
            .setLargeIcon(largeIcon)
			.setSmallIcon(R.drawable.ic_play_arrow_white_24dp)
			.setContentTitle(getString(R.string.app_name))
			.setContentText(getString(R.string.notification_text))
			.setWhen(0)
			.setContentIntent(launchActivityPendingIntent)
			.addAction(R.drawable.ic_close_white_24dp, getString(R.string.notification_action_hide), hidePendingIntent)
			.addAction(R.drawable.ic_play_arrow_white_24dp, getString(R.string.notification_action_play_pause), playPausePendingIntent)
			.addAction(R.drawable.ic_skip_next_white_24dp, getString(R.string.notification_action_next), nextPendingIntent);
		}
		
		// Webview
		webView = (WebView) findViewById(R.id.webview_webview);
		webView.setBackgroundColor(getResources().getColor(R.color.background));
		webView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);

		webView.setWebViewClient(new WebViewClient()
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
					mMethod.showToast(getString(R.string.webview_authentication_failed), 1);
					finish();
				}
			}
			
			@Override
			public void onReceivedSslError(WebView view, @NonNull SslErrorHandler handler, SslError error)
			{
				handler.proceed();
			}
			
			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
			{
				mMethod.showToast(getString(R.string.webview_error), 1);
				finish();
			}
		});

		// Prepare user agent
		try
		{
			appVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		}
		catch(Exception e)
		{
			appVersion = "0.0";

            Log.e("getPackageName", Log.getStackTraceString(e));
		}

		boolean authenticationIsEnabled = (!username.equals("") && !password.equals(""));
		boolean hardwareAcceleratedAnimations = mMethod.getDefaultSharedPreferencesBoolean("HARDWARE_ACCELERATED_ANIMATIONS");

		String ua_option_1 = (authenticationIsEnabled) ? "AUTHENTICATION_ENABLED " : "";
		String ua_option_2 = (hardwareAcceleratedAnimations) ? "" : "DISABLE_CSSTRANSITIONS ";
		String ua_option_3 = (hardwareAcceleratedAnimations) ? "" : "DISABLE_CSSTRANSFORMS3D ";

		// Web settings
		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSupportZoom(false);
		webSettings.setUserAgentString(webSettings.getUserAgentString()+" "+getString(R.string.app_name)+"/"+appVersion+" "+ua_option_1+ua_option_2+ua_option_3);

		// Load app
		if(savedInstanceState != null)
		{
			webView.restoreState(savedInstanceState);
		}
		else
		{
			webView.loadUrl(uri);
		}

		// JavaScript interface
		webView.addJavascriptInterface(new JavaScriptInterface(this), "Android");
	}
	
	// Pause activity
	@Override
	public void onPause()
	{
		super.onPause();
		
		// Release wake lock
		if(wakeLock.isHeld()) wakeLock.release();
		
		// Notification
		persistentNotification = mMethod.getSharedPreferencesBoolean("PERSISTENT_NOTIFICATION");
		
		if(persistentNotificationIsSupported && persistentNotification && !hasLongPressedBack)
		{
			String nowplaying_artist = mMethod.getSharedPreferencesString("NOWPLAYING_ARTIST");
			String nowplaying_title = mMethod.getSharedPreferencesString("NOWPLAYING_TITLE");
			
			if(!nowplaying_artist.equals(getString(R.string.notification_no_music_is_playing_artist)))
			{	
				notificationBuilder.setTicker(nowplaying_artist+" - "+nowplaying_title);
				notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
			}
		}
		
		// Pause webview
		CookieSyncManager.getInstance().sync();
		webView.pauseTimers();
		
		activityIsPaused = true;
	}
	
	// Resume activity
	@Override
	public void onResume()
	{
		super.onResume();

		// Notification
		persistentNotification = mMethod.getSharedPreferencesBoolean("PERSISTENT_NOTIFICATION");
		if(persistentNotificationIsSupported && persistentNotification) notificationManager.cancel(NOTIFICATION_ID);
		
		// Resume webview
		if(currentNetwork.equals(mMethod.getCurrentNetwork()))
		{
			webView.resumeTimers();
			if(activityIsPaused) webView.loadUrl("javascript:nativeAppLoad(true)");
		}
		else
		{
			mMethod.showToast(getString(R.string.webview_network_changed), 1);
			finish();
		}
	}
	
	// Save activity
	@Override
	protected void onSaveInstanceState(@NonNull Bundle savedInstanceState)
	{
		super.onSaveInstanceState(savedInstanceState);
		
		// Save webview
		CookieSyncManager.getInstance().sync();
		webView.saveState(savedInstanceState);
	}
	
	// Destroy activity
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		
		activityIsPaused = false;
	}
	
	// Key down
	@Override
	public boolean onKeyDown(int keyCode, @NonNull KeyEvent event)
	{
		if(keyCode == KeyEvent.KEYCODE_BACK)
		{
			event.startTracking();
			return true;
		}
		else if(keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
		{
			return true;
		}
		
		return super.onKeyDown(keyCode, event);	
	}
	
	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event)
	{
		if(keyCode == KeyEvent.KEYCODE_BACK)
		{
			hasLongPressedBack = true;
			finish();
			return true;
		}
		
		return super.onKeyLongPress(keyCode, event);
	}
	
	// Key up
	@Override
	public boolean onKeyUp(int keyCode, @NonNull KeyEvent event)
	{	
		if(keyCode == KeyEvent.KEYCODE_MENU)
		{
			webView.loadUrl("javascript:nativeAppAction('menu')");
			return true;
		}
		else if(keyCode == KeyEvent.KEYCODE_SEARCH)
		{
			webView.loadUrl("javascript:nativeAppAction('search')");
			return true;
		}
		else if(keyCode == KeyEvent.KEYCODE_BACK)
		{
			if(hasLongPressedBack) return true;
			
			boolean canCloseCover = mMethod.getSharedPreferencesBoolean("CAN_CLOSE_COVER");
			
			if(webView.canGoBack() || canCloseCover)
			{
				webView.loadUrl("javascript:nativeAppAction('back')");
				return true;
			}
			else
			{
				mMethod.showToast(getString(R.string.webview_back), 1);
				return true;
			}
		}
		else if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
		{
			webView.loadUrl("javascript:nativeAppAction('volume_down')");
			return true;
		}
		else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP)
		{	
			webView.loadUrl("javascript:nativeAppAction('volume_up')");
			return true;
		}
		
		return super.onKeyUp(keyCode, event);
	}
	
	// JavaScript interface
	public class JavaScriptInterface
	{
	    private final Context mContext;

	    JavaScriptInterface(Context context)
	    {
	        mContext = context;
	    }

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

                Log.e("getApplicationInfo", Log.getStackTraceString(e));
	    	}

            return appFound;
	    }
	    
	    @JavascriptInterface
	    public void JSkeepScreenOn(boolean keepScreenOn)
	    {
	    	if(keepScreenOn)
	    	{
	    		if(!wakeLock.isHeld()) wakeLock.acquire();
	    	}
	    	else
	    	{
	    		if(wakeLock.isHeld()) wakeLock.release();
	    	}
	    }
	    
	    @JavascriptInterface
	    public void JSfinishActivity()
	    {
	    	hasLongPressedBack = true;
	    	finish();
	    }

        @JavascriptInterface
        public void JSsetStatusBarColor(String color)
        {
            final int intColor;

            if(color.equals("primary"))
            {
                intColor = statusBarPrimaryColor;
            }
            else if(color.equals("cover_art"))
            {
                intColor = statusBarCoverArtColor;
            }
            else
            {
                intColor = Color.parseColor(color);
            }

            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    {
                        getWindow().setStatusBarColor(intColor);
                    }
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
			return mMethod.getSharedPreferencesString(preference);
	    }
	    
	    @JavascriptInterface
	    public void JSsetSharedString(String preference, String string)
	    {	
			mMethod.setSharedPreferencesString(preference, string);
	    }
	    
	    @JavascriptInterface
	    public boolean JSgetSharedBoolean(String preference)
	    {
	    	return mMethod.getSharedPreferencesBoolean(preference);
	    }
	    
	    @JavascriptInterface
	    public void JSsetSharedBoolean(String preference, boolean bool)
	    {
			mMethod.setSharedPreferencesBoolean(preference, bool);
	    }
	    
	    @JavascriptInterface
	    public String JSgetVersions()
	    {
	    	String app_version = appVersion;
	    	String app_minimum_version = getString(R.string.app_minimum_version);
	    	
	    	return new JSONArray(Arrays.asList(app_version, app_minimum_version)).toString();
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
                    getWindow().setStatusBarColor(getResources().getColor(R.color.green));

                    statusBarCoverArtColor = R.color.black;
                }

                webView.loadUrl("javascript:setCoverArtFabColor('#689f38')");
            }
            else
            {
                Palette.generateAsync(bitmap, new Palette.PaletteAsyncListener()
                {
                    public void onGenerated(Palette palette)
                    {
                        final int color = palette.getVibrantColor(R.color.black);

                        final String hexColor = String.format("#%06X", (0xFFFFFF & color));

                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                                {
                                    getWindow().setStatusBarColor(color);

                                    statusBarCoverArtColor = color;
                                }

                                webView.loadUrl("javascript:setCoverArtFabColor('"+hexColor+"')");
                            }
                        });
                    }
                });
            }
        }

        @Override
        protected Bitmap doInBackground(String... strings)
        {
            Bitmap bitmap;

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
                bitmap = null;

                Log.e("GetStatusBarColorFromImageTask doInBackground", Log.getStackTraceString(e));
            }

            return bitmap;
        }
    }
}