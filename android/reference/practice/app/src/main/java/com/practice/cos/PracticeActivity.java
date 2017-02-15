package com.practice.cos;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class PracticeActivity extends Activity {
    /** Called when the activity is first created. */
	
	//设置LOG标签
	private  SensorManager sm;
	private TextView XT,YT,ZT,SCT,ACT,G;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        XT = (TextView)findViewById(R.id.XT);
        YT = (TextView)findViewById(R.id.YT);
        ZT = (TextView)findViewById(R.id.ZT);
        G=(TextView)findViewById(R.id.G);
        SCT = (TextView)findViewById(R.id.SensorChanged);
        ACT = (TextView)findViewById(R.id.onAccuracyChanged);
        
      //创建一个SensorManager来获取系统的传感器服务
        sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        //选取加速度感应器
        int sensorType = Sensor.TYPE_ACCELEROMETER;
        
        /*
         * 最常用的一个方法 注册事件
         * 参数1 ：SensorEventListener监听器
         * 参数2 ：Sensor 一个服务可能有多个Sensor实现，此处调用getDefaultSensor获取默认的Sensor
         * 参数3 ：模式 可选数据变化的刷新频率
         * */
        sm.registerListener(myAccelerometerListener,sm.getDefaultSensor(sensorType),SensorManager.SENSOR_DELAY_NORMAL);
        

    }
    /*
     * SensorEventListener接口的实现，需要实现两个方法
     * 方法1 onSensorChanged 当数据变化的时候被触发调用
     * 方法2 onAccuracyChanged 当获得数据的精度发生变化的时候被调用，比如突然无法获得数据时
     * */
    final SensorEventListener myAccelerometerListener = new SensorEventListener(){
    	
    	//复写onSensorChanged方法
    	public void onSensorChanged(SensorEvent sensorEvent){
    		if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
    			
    			//图解中已经解释三个值的含义
    			float X_lateral = sensorEvent.values[0];
    			float Y_longitudinal = sensorEvent.values[1];
    			float Z_vertical = sensorEvent.values[2];
                double G_value = Math.sqrt(X_lateral*X_lateral+Y_longitudinal*Y_longitudinal+Z_vertical*Z_vertical);
    			XT.setText("x=\n"+X_lateral);
    			YT.setText("y=\n"+Y_longitudinal);
    			ZT.setText("z=\n"+Z_vertical);
                G.setText("g=\n"+G_value);
    		}
    	}
    	//复写onAccuracyChanged方法
    	public void onAccuracyChanged(Sensor sensor , int accuracy){
    		ACT.setText("onAccuracyChanged被触发");
    	}
    };
    
    public void onPause(){
    	/*
    	 * 很关键的部分：注意，说明文档中提到，即使activity不可见的时候，感应器依然会继续的工作，测试的时候可以发现，没有正常的刷新频率
    	 * 也会非常高，所以一定要在onPause方法中关闭触发器，否则讲耗费用户大量电量，很不负责。
    	 * */
    	sm.unregisterListener(myAccelerometerListener);
    	super.onPause();
    }

}