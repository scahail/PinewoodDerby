package com.rocket.car;

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
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
	  /** Button for adding entered data to the current series. */
	  private Button mAdd;
	  /** Edit text field for entering the X value of the data to be added. */
	  private EditText mX;
	  /** Edit text field for entering the Y value of the data to be added. */
	  private EditText mY;
	  /** The chart view that displays the data. */
	  private GraphicalView mChartView;

	  private SensorManager mSensorManager;
	  private Sensor mAccelerometer;
	  
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
	    	mAccelerometer =  mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    	mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	    }
	    else
	    {
	    	Toast.makeText(RocketCar.this, "No Sensor Service", Toast.LENGTH_SHORT).show();
	    }
	    // the top part of the UI components for adding new data points
	    mX = (EditText) findViewById(R.id.xValue);
	    mY = (EditText) findViewById(R.id.yValue);
	    mAdd = (Button) findViewById(R.id.add);

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
	        mCurrentRenderer = renderer;
	        setSeriesWidgetsEnabled(true);
	        mChartView.repaint();
	      }
	    });

	    mAdd.setOnClickListener(new View.OnClickListener() {
	      public void onClick(View v) {
	        double x = 0;
	        double y = 0;
	        try {
	          x = Double.parseDouble(mX.getText().toString());
	        } catch (NumberFormatException e) {
	          mX.requestFocus();
	          return;
	        }
	        try {
	          y = Double.parseDouble(mY.getText().toString());
	        } catch (NumberFormatException e) {
	          mY.requestFocus();
	          return;
	        }
	        // add a new data point to the current series
	        mCurrentSeries.add(x, y);
	        mX.setText("");
	        mY.setText("");
	        mX.requestFocus();
	        // repaint the chart such as the newly added point to be visible
	        mChartView.repaint();
	      }
	    });
	  }

	  protected void onResume() {
	    super.onResume();
	    //Sensor Management
	    mSensorManager.registerListener(this, mAccelerometer,SensorManager.SENSOR_DELAY_NORMAL);
	    if (mChartView == null) {
	      LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
	      mChartView = ChartFactory.getLineChartView(this, mDataset, mRenderer);
	      // enable the chart click events
	      mRenderer.setClickEnabled(true);
	      mRenderer.setSelectableBuffer(10);
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
	    mX.setEnabled(enabled);
	    mY.setEnabled(enabled);
	    mAdd.setEnabled(enabled);
	  }

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		
	}
}