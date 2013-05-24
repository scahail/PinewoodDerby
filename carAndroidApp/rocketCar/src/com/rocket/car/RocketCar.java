package com.rocket.car;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Calendar;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.SeriesSelection;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class RocketCar extends Activity implements SensorEventListener {
	/** The main dataset that includes all the series that go into a chart. */
	  private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
	  /** The main renderer that includes all the renderers customizing a chart. */
	  private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
	  /** The most recently added series. */
	  private XYSeries mCurrentSeries;
	  /** The most recently created renderer, customizing the current series. */
	  private XYSeriesRenderer mCurrentRenderer;
	  /** Button for creating a new series of data. */
	  private Button mNewSeries;
	  
	  /** The chart view that displays the data. */
	  private GraphicalView mChartView;
	  
	  private static int[] COLORS = new int[] { Color.GREEN, Color.BLUE, Color.MAGENTA, Color.CYAN };


	  private SensorManager mSensorManager;
	  private Sensor mAccelerometer;
	  private TextView mXG;
	  private TextView mYG;
	  private TextView mZG;
	  private float grav_x = SensorManager.GRAVITY_EARTH;
	  private float grav_y = SensorManager.GRAVITY_EARTH;
	  private float grav_z = SensorManager.GRAVITY_EARTH;
	  private long timecount = 0;
	  private String filename = Environment.getExternalStorageDirectory()+"/rocketCar";
	  private float old_value = 0;
	  private boolean mInitializedOld = false;
	  
	  //Launch Detection Params
	  private float mNominalVal = 115;
	  private int mAngleCount = 0;
	  private int mLevelCount = 0;
	  private boolean mLevel = false;
	  private boolean mReadyToDrop = false;
	  private boolean mRocketFired = false;
	  
	  @Override
	  protected void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	    // save the current data, for instance when changing screen orientation
	    outState.putSerializable("dataset", mDataset);
	    outState.putSerializable("renderer", mRenderer);
	    outState.putSerializable("current_series", mCurrentSeries);
	    outState.putSerializable("current_renderer", mCurrentRenderer);
	  }

	  @Override
	  protected void onRestoreInstanceState(Bundle savedState) {
	    super.onRestoreInstanceState(savedState);
	    // restore the current data, for instance when changing the screen
	    // orientation
	    mDataset = (XYMultipleSeriesDataset) savedState.getSerializable("dataset");
	    mRenderer = (XYMultipleSeriesRenderer) savedState.getSerializable("renderer");
	    mCurrentSeries = (XYSeries) savedState.getSerializable("current_series");
	    mCurrentRenderer = (XYSeriesRenderer) savedState.getSerializable("current_renderer");
	  }
	  
	  protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.xy_chart);
	    //Sensor Management
	    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	    if (mSensorManager != null)
	    {
	    	mAccelerometer =  mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
	    	mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
	    }
	    else
	    {
	    	Toast.makeText(RocketCar.this, "No Sensor Service", Toast.LENGTH_SHORT).show();
	    }
	    Calendar c = Calendar.getInstance();
	    filename += String.valueOf(c.get(Calendar.HOUR))+"_"+String.valueOf(c.get(Calendar.MINUTE))+"_"+String.valueOf(c.get(Calendar.SECOND))+".txt";
	    
	    //Gravity output texts
	    mXG = (TextView) findViewById(R.id.xG);
	    mYG = (TextView) findViewById(R.id.yG);
	    mZG = (TextView) findViewById(R.id.zG);
	    
	    // set some properties on the main renderer
	    mRenderer.setApplyBackgroundColor(true);
	    mRenderer.setBackgroundColor(Color.argb(100, 50, 50, 50));
	    mRenderer.setAxisTitleTextSize(16);
	    mRenderer.setChartTitleTextSize(20);
	    mRenderer.setLabelsTextSize(15);
	    mRenderer.setLegendTextSize(15);
	    mRenderer.setMargins(new int[] { 20, 30, 15, 0 });
	    mRenderer.setZoomButtonsVisible(true);
	    mRenderer.setPointSize(5);
        
	    // the button that handles the new series of data creation
	    mNewSeries = (Button) findViewById(R.id.new_series);
	    mNewSeries.setOnClickListener(new View.OnClickListener() {
	      public void onClick(View v) {
	        String seriesTitle = "Series " + (mDataset.getSeriesCount() + 1);
	        // create a new series of data
	        XYSeries series = new XYSeries(seriesTitle);
	        mDataset.addSeries(series);
	        mCurrentSeries = series;
	        // create a new renderer for the new series
	        XYSeriesRenderer renderer = new XYSeriesRenderer();
	        mRenderer.addSeriesRenderer(renderer);
	        // set some renderer properties
	        renderer.setPointStyle(PointStyle.CIRCLE);
	        renderer.setFillPoints(true);
	        renderer.setDisplayChartValues(true);
	        renderer.setDisplayChartValuesDistance(10);
	        renderer.setColor(Color.GREEN);
	        mCurrentRenderer = renderer;
	        setSeriesWidgetsEnabled(true);
	        mChartView.repaint();
	      }
	    });
	  }

	  protected void onResume() {
	    super.onResume();
	    //Sensor Management
	    mSensorManager.registerListener(this, mAccelerometer,SensorManager.SENSOR_DELAY_GAME);
	    if (mChartView == null) {
	      LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
	      mChartView = ChartFactory.getLineChartView(this, mDataset, mRenderer);
	      // enable the chart click events
	      mRenderer.setClickEnabled(true);
	      mRenderer.setSelectableBuffer(10);
	      
	      for(int i=0;i<3;i++){
	      //Add Initial Series
	      String seriesTitle = "Series " + (mDataset.getSeriesCount() + 1);
	        // create a new series of data
	        XYSeries series = new XYSeries(seriesTitle);
	        mDataset.addSeries(series);
	        mCurrentSeries = series;
	        // create a new renderer for the new series
	        XYSeriesRenderer renderer = new XYSeriesRenderer();
	        renderer.setColor(COLORS[i]);
	        mRenderer.addSeriesRenderer(renderer);
	        // set some renderer properties
	        renderer.setPointStyle(PointStyle.CIRCLE);
	        renderer.setFillPoints(true);
	        renderer.setDisplayChartValues(true);
	        renderer.setDisplayChartValuesDistance(10);
	        mCurrentRenderer = renderer;
	        setSeriesWidgetsEnabled(true);
	        mChartView.repaint();
	      }
	      //Initializing File
	      //writeData("x,y,z\r\n",filename);

	      //Adding Chart Click Listener
	      mChartView.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	          // handle the click event on the chart
	          SeriesSelection seriesSelection = mChartView.getCurrentSeriesAndPoint();
	          if (seriesSelection == null) {
	            Toast.makeText(RocketCar.this, "No chart element", Toast.LENGTH_SHORT).show();
	          } else {
	            // display information of the clicked point
	            Toast.makeText(
	            		RocketCar.this,
	                "Chart element in series index " + seriesSelection.getSeriesIndex()
	                    + " data point index " + seriesSelection.getPointIndex() + " was clicked"
	                    + " closest point value X=" + seriesSelection.getXValue() + ", Y="
	                    + seriesSelection.getValue(), Toast.LENGTH_SHORT).show();
	          }
	        }
	      
	      });
	      layout.addView(mChartView, new LayoutParams(LayoutParams.FILL_PARENT,
	          LayoutParams.FILL_PARENT));
	      boolean enabled = mDataset.getSeriesCount() > 0;
	      setSeriesWidgetsEnabled(enabled);
	    } else {
	      mChartView.repaint();
	    }
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

	  /**
	   * Enable or disable the add data to series widgets
	   * 
	   * @param enabled the enabled state
	   */
	  private void setSeriesWidgetsEnabled(boolean enabled) {

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

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub


		  // Isolate the force of gravity with the low-pass filter.
		/*
		grav_x = alpha * grav_x + (1 - alpha) * event.values[0];
		grav_y = alpha * grav_y + (1 - alpha) * event.values[1];
		grav_z = alpha * grav_z + (1 - alpha) * event.values[2];
		mXG.setText(String.valueOf(grav_x)+"  ");
		mYG.setText(String.valueOf(grav_y)+"  ");
		mZG.setText(String.valueOf(grav_z)+"  ");
		  // Remove the gravity contribution with the high-pass filter.
		 float x = event.values[0] - grav_x;
		 float y = event.values[1] - grav_y;
		 float z = event.values[2] - grav_z;
		 */
		float x = event.values[0];
		float y = (-event.values[1]);
		mXG.setText(String.valueOf(mAngleCount)+"  ");
		mYG.setText(String.valueOf(y)+"  ");
		mZG.setText(String.valueOf(mLevelCount)+"  ");
		float z = event.values[2];
		if (!mInitializedOld)
		{
			old_value = y;
			mInitializedOld = true;
		}
		//Get the steady State of Car before drop
		if(y > 100 && y < 130 && !mReadyToDrop && mAngleCount < 0)
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
				playSound(200,0);
				mReadyToDrop = true;
				mCurrentSeries = mDataset.getSeriesAt(2);
		        mCurrentSeries.add(timecount, y);
				if(mRocketFired)
					mRocketFired = false;
			}
		}
		if (mReadyToDrop)
		{
			if((y-mNominalVal) > 20)
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
				mLevel = true;
				//Check for primed
				if(!mRocketFired)
				{
					playSound(3000,3);
					mRocketFired = true;
					mCurrentSeries = mDataset.getSeriesAt(1);
			        mCurrentSeries.add(timecount, y);
			        mAngleCount = 0;
			        mLevelCount = 0;
					mReadyToDrop = false;
				}
			}
		}
		
		// add a new data point to the current series
		mCurrentSeries = mDataset.getSeriesAt(0);
        mCurrentSeries.add(timecount, y);
        /*
        mCurrentSeries = mDataset.getSeriesAt(1);
        mCurrentSeries.add(timecount, y);
        mCurrentSeries = mDataset.getSeriesAt(2);
        mCurrentSeries.add(timecount, z);
       	*/
        //writeData(String.valueOf(x)+","+String.valueOf(y)+","+String.valueOf(z)+"\r\n",filename);
        // repaint the chart such as the newly added point to be visible
        mChartView.repaint();
        timecount++;
	}
}