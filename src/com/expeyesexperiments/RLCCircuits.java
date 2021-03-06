package com.expeyesexperiments;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import explib.ejMath;
import explib.ejPlot;
 
public class RLCCircuits extends Activity {
 
	Button button,fit_bt;
	expeyesCommon ej;
	int[] channel_colors = {Color.DKGRAY,Color.RED,Color.rgb(0, 155, 0),Color.BLUE};
	ejPlot ejplot;
	TextView timebase_label,msg,fit_msg;
	SeekBar timebase;
	String filename = new String();
    private File dataDirectory;
    private double[] msPerDiv = {0.100, 0.200, 0.500, 1.0, 2.0, 5.0, 10.0, 20.0, 40.0, 50.0};	
    private Builder reconnect_message;
    DecimalFormat df1 = new DecimalFormat("#.###");
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rlc_experiment);
		Toast.makeText(getBaseContext(),"RLC TRANSIENT RESPONSE MEASUREMENTS",Toast.LENGTH_SHORT).show();

		
		dataDirectory = new File(Environment.getExternalStorageDirectory()+"/expeyes/RLC/");
		//Log.e("DIR",dataDirectory.getName());
		dataDirectory.mkdirs();

		
		
		ej=expeyesCommon.getInstance();
     	setTitle(ej.title+"RLC Circuits");
     	reconnect_message = ej.makeReconnectDialog(this);
     	
     	msg = (TextView)findViewById(R.id.msg);
     	fit_msg = (TextView)findViewById(R.id.RLC_msg);
		LinearLayout plot=(LinearLayout)findViewById(R.id.plot);
		ejplot = new ejPlot(this, plot);
		timebase_label = (TextView) findViewById(R.id.timebase_label);
		timebase = (SeekBar) findViewById(R.id.timebase);
		fit_bt = (Button) findViewById(R.id.fit_bt);
		setScale();
		
	    timebase.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
				
				timebase_label.setText(msPerDiv[progress]+"mS/div");
			}
		});

     	
	}
	
	
	@Override public void onResume(){
		super.onResume();
		fit_bt.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				show_data(true);
			}
	    	
	    });

	}
	@Override
	public void onDestroy(){
		super.onDestroy();
		Toast.makeText(getBaseContext(),"RETURNING TO MAIN MENU",Toast.LENGTH_SHORT).show();
	}
 

	public void discharge(View v){
		setScale();
		ej.ej.set_state(10,1);
		ej.ej.enable_set_low(10);
		ej.ej.capture_hr(1,ejplot.Par.NS,ejplot.Par.TG);
		
		if(ej.ej.commandStatus!=ej.ej.SUCCESS){
			//Toast.makeText(getBaseContext(),"FAILED. Check connections.",Toast.LENGTH_SHORT).show();
			if(!ej.ej.connected)reconnect_message.show();
			return;
			}
		
		show_data(false);
		//if(!ej.ej.connected)reconnect_message.show();	
	}


	public void show_data(boolean showfit){
		
		int MAX_PTS=1000,MAX_PAR=25;
		double[] P=new double[MAX_PAR],Err=new double[MAX_PAR];
		boolean Ok;
		Ok=ejMath.DoFitDSine(ej.ej.ejdata.length,5,ej.ej.ejdata.t1,ej.ej.ejdata.ch1,P,Err);
		//P[1]=P[1]/(2*3.1415); //change frequency from angular to normal
		//P[2]=P[2]*180/3.1415; //change phase to degrees
		Log.e("FITTING",Ok+":"+P[0]+","+P[1]+","+P[2]+","+P[3]+","+P[4]);
		
		ejplot.clearPlots();
		ejplot.line(ej.ej.ejdata.t1,ej.ej.ejdata.ch1,ej.ej.ejdata.length,0);
		if(Ok){
			
			fit_msg.setText("Frequency : "+df1.format(P[1]/(2*3.1415))+" kHz\nDecay constant : "+df1.format(P[4]/(P[1]) ));
			if(showfit){
				for(int n=0;n<ej.ej.ejdata.length;n++){
					ej.ej.ejdata.ch2[n]=(float) ejMath.Func_Dsine(P, ej.ej.ejdata.t1[n]);
					}
				ejplot.line(ej.ej.ejdata.t1,ej.ej.ejdata.ch2,ej.ej.ejdata.length,1);
				}
		}
		else{
			fit_msg.setText("Couldn't fit");
		}
		ejplot.updatePlots();
		
	}

	// Changing the Time Base
		public void setScale() {
			int selectedTimebase = timebase.getProgress();    				// Slider returns a number from 0 to 9
			ejplot.setTimebase(msPerDiv[selectedTimebase]);
			timebase_label.setText(msPerDiv[selectedTimebase]+"mS/div");	
			 
		}
		
	
	
private void appendToFile(OutputStreamWriter writer,float[] x,float[] y,int length) throws IOException{
	for(int i=0;i<length;i++){writer.append(x[i]+" "+y[i]+"\n");}
	writer.append("\n");
	
}

public void dumpToFile(View v){
	SimpleDateFormat s = new SimpleDateFormat("dd-MM_hh-mm-ss");
	String format = s.format(new Date());
	//Log.e("FILENAME",format+"");
	filename = format+".txt";
	//Log.e("SAVING",""+filename);
	msg.setText("Saved to:"+filename);
	try {
    	File outputFile = new File(dataDirectory, filename);
  		outputFile.createNewFile();
		FileOutputStream fOut = new FileOutputStream(outputFile);
		OutputStreamWriter myOutWriter =  new OutputStreamWriter(fOut);
        appendToFile(myOutWriter,ej.ej.ejdata.t1,ej.ej.ejdata.ch1,ej.ej.ejdata.length);
    	
		myOutWriter.close();
        fOut.close();
        
        Toast.makeText(getBaseContext(), "Done writing SD " + filename + "",Toast.LENGTH_SHORT).show();
    } catch (Exception e) {
        Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
    }
	
	
}


}
