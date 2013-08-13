/***************************************
 * 
 * Android Bluetooth Oscilloscope
 * yus	-	projectproto.blogspot.com
 * September 2010
 *  
 ***************************************/

package org.projectproto.yuscope;

import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;

public class WaveformPlotThread extends Thread {
	//Debugging
	private final String TAG = "WaveformPlotThread";
	private final boolean D = true;
	
	private SurfaceHolder holder;
	private WaveformView plot_area;
	private boolean _run = false;
	
	public WaveformPlotThread(SurfaceHolder surfaceHolder, WaveformView view){
		holder = surfaceHolder;
		plot_area = view;
	}
	public void setRunning(boolean run){
		_run = run;
	}
	
	@Override
	public void run(){
		Canvas c;
		while(_run){
			c = null;
			try{
				c = holder.lockCanvas(null);
				synchronized (holder) {
					plot_area.PlotPoints(c);
				}
			} catch (Exception e) {
				if (D) Log.d(TAG, "exception occur", e);
			} finally {
				if(c!=null){
					holder.unlockCanvasAndPost(c);
				}
			}
		}
	}
}
