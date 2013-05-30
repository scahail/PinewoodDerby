package com.rocket.car;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Calendar;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class RocketCar extends Activity implements SensorEventListener, LocationListener{
	
	public static final boolean Chartflag = false;
	
	  /** Button for creating a new series of data. */
	  private Button mNewSeries;
	  
	  private TextView mSocketOut;
	  /** The chart view that displays the data. */
	  //private GraphicalView mChartView;
	  
	  private static int[] COLORS = new int[] { Color.GREEN, Color.BLUE, Color.MAGENTA, Color.CYAN };


	  private SensorManager mSensorManager;
	  private Sensor mAccelerometer;
	  private TextView mXG;
	  private TextView mYG;
	  private TextView mZG;
	  
	  private long timecount = 0;
	  private String filename = Environment.getExternalStorageDirectory()+"/rocketCar";
	  
	  private boolean mInitializedOld = false;
	  
	  //Launch Detection Params
	  private float mNominalVal = -115;
	  private int mAngleCount = 0;
	  private int mLevelCount = 0;
	  private boolean mReadyToDrop = false;
	  private boolean mRocketFired = false;
	  
	  //Socket Listening Params
	  private NetworkTask mNetTask;
	  private Button mConnectButton;
	  private boolean mSocketOpened = false;
	  private boolean mRocketArmed = false;
	  
	  //GPS
	  private LocationManager mLocationManager;
	  
	  //Velocity Calculation info
	  //private Sensor mLinearAcceleration;
	  //mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	  //mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
	  @Override
	  protected void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	  }

	  @Override
	  protected void onRestoreInstanceState(Bundle savedState) {
	    super.onRestoreInstanceState(savedState);
	  }
	  
	  protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.xy_chart);
	    //Sensor Management
	    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	    if (mSensorManager != null)
	    {
	    	mAccelerometer =  mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
	    	mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);
	    	mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_NORMAL);
	    }
	    else
	    {
	    	Toast.makeText(RocketCar.this, "No Sensor Service", Toast.LENGTH_SHORT).show();
	    }
	    Calendar c = Calendar.getInstance();
	    filename += String.valueOf(c.get(Calendar.HOUR))+"_"+String.valueOf(c.get(Calendar.MINUTE))+"_"+String.valueOf(c.get(Calendar.SECOND))+".txt";
	    filename ="rocketcar.txt";
	    mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
	    
	    //Setup Network Task
	    mConnectButton = (Button) findViewById(R.id.sockConnect);
	    mConnectButton.setOnClickListener(new View.OnClickListener() {
		      public void onClick(View v) {
		    	  mNetTask = new NetworkTask();
		    	  getIP();
		  	      mNetTask.execute();
		  	      mSocketOpened =true;
		  	      mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, RocketCar.this);
		      }
		    });
	    mSocketOut = (TextView) findViewById(R.id.sockOut);
	    //Gravity output texts
	    mXG = (TextView) findViewById(R.id.xG);
	    mYG = (TextView) findViewById(R.id.yG);
	    mZG = (TextView) findViewById(R.id.zG);
	    
	    // the button that handles the new series of data creation
	    mNewSeries = (Button) findViewById(R.id.new_series);
	    mNewSeries.setOnClickListener(new View.OnClickListener() {
	      public void onClick(View v) {
	    	  mNetTask.SendDataToNetwork("Hello Moto");
	      }
	    });
	  }

	  protected void onResume() {
	    super.onResume();
	    //Sensor Management
	    mSensorManager.registerListener(this, mAccelerometer,SensorManager.SENSOR_DELAY_NORMAL);
	      //Initializing File
	      //writeData("x,y,z\r\n",filename);
	  }
	  protected void playSound(int duration,int type)
	  {
		  final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
	      switch(type)
	      {
	    	  case 0:
	    		  tg.startTone(ToneGenerator.TONE_PROP_BEEP,duration);
	    		  break;
	    	  case 1:
	    		  tg.startTone(ToneGenerator.TONE_CDMA_DIAL_TONE_LITE,duration);
	    		  break;
	    	  case 2:
	    		  tg.startTone(ToneGenerator.TONE_CDMA_LOW_L,duration);
	    		  break;
	    	  case 3:
	    		  tg.startTone(ToneGenerator.TONE_CDMA_HIGH_L,duration);
	    		  break;
	    	  default:
	    		  tg.startTone(ToneGenerator.TONE_PROP_BEEP,duration);
	    		  break;
	      }
		  
	  }
	  protected void onPause() {
		  super.onPause();
		  //Sensor Management
		  mSensorManager.unregisterListener(this);
	  }

	  public void writeData(String data,String strFilePath)
      {
          PrintWriter csvWriter;
          try
          {
              File file = new File(strFilePath);
              if(!file.exists()){
                  file = new File(strFilePath);
              }
              csvWriter = new  PrintWriter(new FileWriter(file,true));
              csvWriter.print(data);
              csvWriter.close();
          }
          catch (Exception e)
          {
              e.printStackTrace();
          }
      }

	  public String getIP()
	  {
		  EditText mIPView = (EditText) findViewById(R.id.mIpAddr);
		  String mIP = mIPView.getText().toString();
	  	  mSocketOut.setText("IP: "+mIP);
	  	  mNetTask.SetSocketIP(mIP);
	  	  return mIP;
	  }

	public void mLog(String s)
	{
		writeData(s+"\r\n",filename);
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		// TODO Auto-generated method stub
	}
	public void armRocket()
	{
		Toast.makeText(RocketCar.this, "Rocket Armed", Toast.LENGTH_SHORT).show();
		mRocketArmed = true;
	}
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION)
		{
			float x = event.values[0];
			float y = (-event.values[1]);
			mXG.setText(String.valueOf(mAngleCount)+"  ");
			mYG.setText(String.valueOf(y)+"  ");
			mZG.setText(String.valueOf(mLevelCount)+"  ");
			float z = event.values[2];
			if (!mInitializedOld)
			{
				mInitializedOld = true;
			}
			//Get the steady State of Car before drop
			if(y > (mNominalVal-15) && y < (mNominalVal+15) && !mReadyToDrop && mAngleCount < 0)
			{
				mAngleCount = 0;
			}
			if (Math.abs(y-mNominalVal) < 5)
			{
				mAngleCount++;
			}
			else
			{
				mAngleCount--;
			}
			if (mAngleCount > 20)
			{
				if(!mReadyToDrop)
				{
					if(mSocketOpened)
						mNetTask.SendDataToNetwork("READY TO DROP");
					//playSound(200,0);
					mReadyToDrop = true;
					if(mRocketFired)
						mRocketFired = false;
				}
			}
			if (mReadyToDrop)
			{
				if((mNominalVal-y) > 20)
				{
						mLevelCount++;
				}
				else
				{
					mLevelCount--;
					if(mLevelCount < 0)
						mLevelCount = 0;
				}
				if(mLevelCount > 10)
				{
					//Check for primed
					if(!mRocketFired && mRocketArmed)
					{
						playSound(3000,0);
						mRocketFired = true;
				        mAngleCount = 0;
				        mLevelCount = 0;
						mReadyToDrop = false;
						mRocketArmed = false;
					}
				}
		        timecount++;
			}
		}
		if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
		{
			if(mSocketOpened)
				mNetTask.SendDataToNetwork(String.format("ACC:%+5.2f", event.values[1]));
		}
	}
	
	//Network Socket Task
	public class NetworkTask extends AsyncTask<Void, byte[], Boolean> {
        Socket nsocket; //Network Socket
        InputStream nis; //Network Input Stream
        OutputStream nos; //Network Output Stream
        private String mIP = (String) getText(R.string.ipaddr);
        @Override
        protected void onPreExecute() {
            mLog("onPreExecute");
        }

        @Override
        protected Boolean doInBackground(Void... params) { //This runs on a different thread
            boolean result = false;
            try {
            	mLog("doInBackground: Creating socket ");
            	mLog("IP: "+mIP);
                SocketAddress sockaddr = new InetSocketAddress(mIP, 50007);
                nsocket = new Socket();
                nsocket.connect(sockaddr, 10000); //10 second connection timeout
                if (nsocket.isConnected()) { 
                    nis = nsocket.getInputStream();
                    nos = nsocket.getOutputStream();
                    mLog("doInBackground: Socket created, streams assigned");
                    mLog("doInBackground: Waiting for inital data...");
                    byte[] buffer = new byte[4096];
                    int read = nis.read(buffer, 0, 4096); //This is blocking
                    while(read != -1){
                        byte[] tempdata = new byte[read];
                        System.arraycopy(buffer, 0, tempdata, 0, read);
                        publishProgress(tempdata);
                        read = nis.read(buffer, 0, 4096); //This is blocking
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                mLog("doInBackground: IOException");
                result = true;
            } catch (Exception e) {
                e.printStackTrace();
                mLog("doInBackground: Exception");
                result = true;
            } finally {
                try {
                    nis.close();
                    nos.close();
                    nsocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mLog("doInBackground: Finished");
            }
            return result;
        }
        public void SetSocketIP(String ip)
        {
        	mIP = ip;
        }
        public void SendDataToNetwork(String cmd) { //You run this from the main thread.
            try {
                if (nsocket.isConnected()) {
                	//mLog("SDTN: Write:"+cmd);
                    nos.write(cmd.getBytes());
                } else {
                	mLog("SendDataToNetwork: Cannot send message. Socket is closed");
                }
            } catch (Exception e) {
            	mLog("SendDataToNetwork: Message send failed. Caught an exception");
            }
        }

        @Override
        protected void onProgressUpdate(byte[]... values) {
            if (values.length > 0) {
            	mLog("onProgressUpdate: " + values[0].length + " bytes received.");
                String data = null;
				try {
					data = new String(values[0],"UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					mLog("Encoding Exception");
				}
				if(data != null)
				{
	                mLog(data);
	                if (data.contains("ARM"))
	                {
	                	armRocket();
	                	//playSound(2000,0);
	                	SendDataToNetwork("CONFIRM ARMED");
	                }
				}
            }
        }
        @Override
        protected void onCancelled() {
        	mLog("Cancelled.");
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
            	mLog("onPostExecute: Completed with an Error.");
            } else {
            	mLog("onPostExecute: Completed.");
            }
        }
    }
	protected void onDestroy()
	{
		super.onDestroy();
		mNetTask.cancel(true);
	}

	public void onLocationChanged(Location location) {
		//String newLatitude = Double.toString(location.getLatitude());
		//String newLongitude = Double.toString(location.getLongitude());
		//if(mSocketOpened)
			//mNetTask.SendDataToNetwork(String.format("LAT:%+5.2f LON:%+5.2f", newLatitude,newLongitude));
	}

	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
}