package net.olejon.spotcommander;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class RemoteControlService extends Service implements SensorEventListener
{
	private final MyMethod mMethod = new MyMethod(this);

	private PhoneStateListener phoneStateListener;
	private TelephonyManager telephonyManager;
	private SensorManager sensorManager;
	private Sensor sensor;

	private String currentNetwork;
	
	private boolean pauseOnIncomingCall = false;
	private boolean pauseOnOutgoingCall = false;
	private boolean flipToPause = false;
	private boolean shakeToSkip = false;
	private boolean isIncomingCall = false;
	private boolean deviceHasAccelerometer = false;
	private boolean isFlipped = false;
	private boolean isShaked = false;
	
	private int shakeToSkipSensitivityInt;
	
	private float shakeToSkipChange;
	private float shakeToSkipCurrent;
	private float shakeToSkipLast;
	
	// Create service
	@Override
	public void onCreate()
	{
		// Calls
		phoneStateListener = new PhoneStateListener()
		{
		    @Override
		    public void onCallStateChanged(int state, String incomingNumber)
		    {
		    	pauseOnIncomingCall = mMethod.getSharedPreferencesBoolean("PAUSE_ON_INCOMING_CALL");
		    	pauseOnOutgoingCall = mMethod.getSharedPreferencesBoolean("PAUSE_ON_OUTGOING_CALL");
		    	
		    	long computerId = mMethod.getSharedPreferencesLong("LAST_COMPUTER_ID");
		    	
		    	if(state == TelephonyManager.CALL_STATE_RINGING)
		    	{	
		    		if(pauseOnIncomingCall && currentNetwork.equals(mMethod.getCurrentNetwork())) mMethod.remoteControl(computerId, "pause", "");
		    		
		    		isIncomingCall = true;
		    	}
		    	else if(state == TelephonyManager.CALL_STATE_OFFHOOK && !isIncomingCall)
		    	{
		    		if(pauseOnOutgoingCall && currentNetwork.equals(mMethod.getCurrentNetwork())) mMethod.remoteControl(computerId, "pause", "");
		    	}
		    	else if(state == TelephonyManager.CALL_STATE_IDLE)
		    	{
		    		isIncomingCall = false;
		    	}
		        
		        super.onCallStateChanged(state, incomingNumber);
		    }
		};
		
		telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		
		// Accelerometer
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		if(sensor != null) deviceHasAccelerometer = true;
	}
	
	// Start service
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		// Current network
		currentNetwork = mMethod.getCurrentNetwork();
		
		// Calls
		pauseOnIncomingCall = mMethod.getSharedPreferencesBoolean("PAUSE_ON_INCOMING_CALL");
		pauseOnOutgoingCall = mMethod.getSharedPreferencesBoolean("PAUSE_ON_OUTGOING_CALL");
		
		if(pauseOnIncomingCall || pauseOnOutgoingCall)
		{
			telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		}
		else
		{
			telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
		}
		
		// Accelerometer
		if(deviceHasAccelerometer)
		{
			flipToPause = mMethod.getSharedPreferencesBoolean("FLIP_TO_PAUSE");	
			shakeToSkip = mMethod.getSharedPreferencesBoolean("SHAKE_TO_SKIP");
			
			if(flipToPause || shakeToSkip)
			{
				sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
				
				if(shakeToSkip)
				{
					String shakeToSkipSensitivity = mMethod.getSharedPreferencesString("SHAKE_TO_SKIP_SENSITIVITY");

					shakeToSkipSensitivityInt = 14;

					if(shakeToSkipSensitivity.equals("higher"))
					{
						shakeToSkipSensitivityInt = 10;
					}
					else if(shakeToSkipSensitivity.equals("high"))
					{
						shakeToSkipSensitivityInt = 12;
					}
					else if(shakeToSkipSensitivity.equals("low"))
					{
						shakeToSkipSensitivityInt = 16;
					}
					else if(shakeToSkipSensitivity.equals("lower"))
					{
						shakeToSkipSensitivityInt = 18;
					}
					
					shakeToSkipChange = 0.00f;
					shakeToSkipCurrent = SensorManager.GRAVITY_EARTH;
					shakeToSkipLast = SensorManager.GRAVITY_EARTH;
				}
			}
			else
			{
				sensorManager.unregisterListener(this);
			}
		}
		
		return START_STICKY;
	}
	
	// RPC
	@Override
	public IBinder onBind(Intent arg0)
	{
		return null;
	}
	
	// Destroy service
	@Override
	public void onDestroy()
	{
		// Calls
		telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
		
		// Accelerometer
		if(deviceHasAccelerometer) sensorManager.unregisterListener(this);
	}
	
	// Accelerometer
	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		
	}
	
	@Override
	public final void onSensorChanged(SensorEvent event)
	{	
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];

		// Flip to pause
		if(flipToPause)
		{
			if(z < -9.5)
			{
				if(!isFlipped && currentNetwork.equals(mMethod.getCurrentNetwork()))
				{
					Runnable isFlippedRunnable= new Runnable()
					{
						public void run()
						{
							if(isFlipped)
							{
								long computerId = mMethod.getSharedPreferencesLong("LAST_COMPUTER_ID");
								
								mMethod.remoteControl(computerId, "pause", "");
							}
						}			
					};
					
					Handler isFlippedHandler = new Handler();			
					isFlippedHandler.removeCallbacks(isFlippedRunnable);	
					isFlippedHandler.postDelayed(isFlippedRunnable, 1000);
				}
			
				isFlipped = true;
			}
			else if(z > -7)
			{	
				isFlipped = false;
			}
		}
		
		// Shake to skip
		if(shakeToSkip)
		{
			shakeToSkipLast = shakeToSkipCurrent;
			shakeToSkipCurrent = (float) Math.sqrt((double) (x*x + y*y + z*z));
		
			float shakeToSkipDelta = shakeToSkipCurrent - shakeToSkipLast;
			
			shakeToSkipChange = shakeToSkipChange * 0.9f + shakeToSkipDelta;
			
			if(shakeToSkipChange > shakeToSkipSensitivityInt)
			{
				if(!isShaked && currentNetwork.equals(mMethod.getCurrentNetwork()))
				{
					isShaked = true;
					
					mMethod.showToast("Shake detected, playing next track", 0);
					
					long computerId = mMethod.getSharedPreferencesLong("LAST_COMPUTER_ID");
					
					mMethod.remoteControl(computerId, "next", "");
				}
				
				Runnable isShakedRunnable= new Runnable()
				{
					public void run()
					{
						isShaked = false;
					}			
				};
				
				Handler isShakedHandler = new Handler();			
				isShakedHandler.removeCallbacks(isShakedRunnable);	
				isShakedHandler.postDelayed(isShakedRunnable, 500);
			}
		}
	}
}