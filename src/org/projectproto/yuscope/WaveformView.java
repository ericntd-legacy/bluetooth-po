/***************************************
 * 
 * Android Bluetooth Oscilloscope
 * yus	-	projectproto.blogspot.com
 * September 2010
 *  
 ***************************************/

package org.projectproto.yuscope;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
//import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.ViewGroup;

public class WaveformView extends SurfaceView implements SurfaceHolder.Callback{
	//Debugging
	private final String TAG = "WaveformView";
	private final boolean D = false;
	
	private WaveformPlotThread plot_thread;
	
//	private final int width = 320;
//	private final int height = 240;
	private int width = 500;//dp or px?
	private int height = 350;
	
//	private static int[] waveformArray = new int[320];
//	private static int[] ch2_data = new int[320];
	private static int[] waveformArray = null;
	private static int[] ch2_data = null;
//	private static int ch1_pos = 120, ch2_pos = 120;
	//private static int ch1_pos = 175, ch2_pos = 175;
	private static int ch1_pos = 350, ch2_pos = 350;//what are these?
	
	private Paint ch1_color = new Paint();
	private Paint ch2_color = new Paint();
	private Paint grid_paint = new Paint();
	private Paint cross_paint = new Paint();
	private Paint outline_paint = new Paint();
	
	public WaveformView(Context context, AttributeSet attrs) {  

		super(context, attrs);  
		//super(context);
		getHolder().addCallback(this);
		
		width = 500;
		waveformArray = new int[width];
		ch2_data = new int[width];
		
		int i;
		for(i=0; i<width; i++){
			waveformArray[i] = ch1_pos;
			ch2_data[i] = ch2_pos;
		}
		
		plot_thread = new WaveformPlotThread(getHolder(), this);
		//setFocusable(true);
		ch1_color.setColor(Color.YELLOW);
		ch2_color.setColor(Color.RED);
		grid_paint.setColor(Color.rgb(100, 100, 100));
		cross_paint.setColor(Color.rgb(152, 152, 152));
		//cross_paint.setColor(Color.LTGRAY);
		outline_paint.setColor(Color.GREEN);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		plot_thread = new WaveformPlotThread(getHolder(), this);
		plot_thread.setRunning(true);
		try {
			plot_thread.start();
		} catch (IllegalThreadStateException e) {
			if (D) Log.d(TAG, "exception occur", e);
		} catch (Exception e) {
			if (D) Log.d(TAG, "exception occur", e);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
		plot_thread.setRunning(false);
		while (retry){
			try{
				plot_thread.join();
				retry = false;
			} catch(InterruptedException e){
				if (D) Log.d(TAG, "exception occur", e);
			} catch (Exception e) {
				if (D) Log.d(TAG, "exception occur", e);
			}
		}
		
	}
	
	@Override
	public void onDraw(Canvas canvas){
		PlotPoints(canvas);
		
	}
	
	public void setSize(int w, int h) {
		if (w!=0) this.width = w;
		if (h!=0) this.height = h;
		waveformArray = new int[width];
		ch2_data = new int[width];
	}
	
	public void setData(int[] data1 ){
		/*int x;
		plot_thread.setRunning(false);
		x = 0;
		while(x<width){
			if(x<(data1.length)){
				//waveformArray[x] = data1[x];
				waveformArray[x] = height-data1[x]+1;
			}else{
				waveformArray[x] = ch1_pos;
			}
			x++;
		}
		x = 0;
		while(x<width){
			if(x<(data2.length)){
				//ch2_data[x] = data2[x];				
				ch2_data[x] = height-data2[x]+1;
			}else{
				ch2_data[x] = ch2_pos;
			}
			x++;
		}*/
		
		for (int i=0; i<width-1; i++) {
			waveformArray[i] = height-data1[i]+1;
			//waveformArray[i] = data1[i];
			//if (D) Log.d(TAG, "data "+waveformArray[i]+" - "+(waveformArray[i]==waveformArray[i+1])+" - height is "+height+" - count is "+i);
			//ch2_data[i] = height-data2[i]+1;
		}
		plot_thread.setRunning(true);
	}
	
	public void PlotPoints(Canvas canvas){//obviously canvas would be null here!
		if (D) Log.d(TAG, "width is "+width+" length of the data array "+waveformArray.length);
		// clear screen
		canvas.drawColor(Color.rgb(20, 20, 20));
		
		// draw grids
	    for(int vertical = 1; vertical<10; vertical++){
	    	if (vertical == 5) canvas.drawLine(
	    			vertical*(width/10)+1, 1,
	    			vertical*(width/10)+1, height+1,
	    			cross_paint);
	    	else canvas.drawLine(
	    			vertical*(width/10)+1, 1,
	    			vertical*(width/10)+1, height+1,
	    			grid_paint);
	    }	    	
	    for(int horizontal = 1; horizontal<10; horizontal++){
	    	if (horizontal == 5) canvas.drawLine(
	    			1, horizontal*(height/10)+1,
	    			width+1, horizontal*(height/10)+1,
	    			cross_paint);
	    	else canvas.drawLine(
	    			1, horizontal*(height/10)+1,
	    			width+1, horizontal*(height/10)+1,
	    			grid_paint);
	    }	    	
	    
	    // draw center cross
		//canvas.drawLine(0, (height/2)+1, width+1, (height/2)+1, cross_paint);
		//canvas.drawLine((width/2)+1, 0, (width/2)+1, height+1, cross_paint);
		
		// draw outline
		canvas.drawLine(0, 0, (width+1), 0, outline_paint);	// top
		//canvas.drawLine((width+1), 0, (width+1), (height+1), outline_paint); //right
		canvas.drawLine((width-1), 0, (width-1), (height-1), outline_paint); //right
		//canvas.drawLine(0, (height+1), (width+1), (height+1), outline_paint); // bottom
		canvas.drawLine(0, (height-1), (width-1), (height-1), outline_paint); // bottom
		canvas.drawLine(0, 0, 0, (height+1), outline_paint); //left
		
		// plot data
		for(int x=0; x<(width-1); x++){
			Point start = new Point(x+1, waveformArray[x]);
			Point stop = new Point(x+2, waveformArray[x+1]);
			//canvas.drawLine(x+1, ch2_data[x], x+2, ch2_data[x+1], ch2_color);
			canvas.drawLine(x+1, waveformArray[x], x+2, waveformArray[x+1], ch1_color);
			if (D) {
				if (waveformArray[x]==waveformArray[x+1]) Log.d(TAG, "flat "+waveformArray[x]);
				else Log.d(TAG, "not flat");
			}
			//Log.d(TAG, "point #"+x+1);
		}
	}
	
	@Override
	public void onMeasure(int w, int h) {
		super.onMeasure(w, h);
		
		int width;
		int height;
		
		int widthMode = MeasureSpec.getMode(w);
	    int widthSize = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
	    int heightMode = MeasureSpec.getMode(h);
	    int heightSize = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY);
	    
	    width = widthSize;
	    height = heightSize;
	    
		setMeasuredDimension(width, height);
	}

}
