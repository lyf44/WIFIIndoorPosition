package czh.example.locdemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Random;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.ActionBarActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanSettings;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class MainActivity extends ActionBarActivity implements SensorEventListener, LeScanCallback {

	//for UI
	private SurfaceView surface_view;
	private SurfaceHolder sfh;
	private Canvas canvas;
	private Paint paint;
	private PointF screenSize = new PointF();
	private Bitmap map;
	private Bitmap user;
	private Bitmap user_rotate;
	private Bitmap dot;
	private double mapWidth = 35.65;
	private double mapHeight = 16.6;
	private boolean initialPositionReceived = false;
	private boolean flag_WIFIchanged = false;
	private String macAddress;

	//Sensors
	SensorManager smManager;
	Sensor accSensor,oriSensor;
	public double[] accData_z = new double [100];
	public double[] oriData_a = new double [100];

	//bluetooth
	private BluetoothAdapter mBluetoothAdapter;
	private SparseArray<BluetoothDevice> mDevices;
	private BluetoothGatt mConnectedGatt;
	private InitiBeacon mInitiBeacon = new InitiBeacon();;
	ConfigiBeacon configiBeacon = new ConfigiBeacon();
	CalibrationiBeacon calibrationiBeacon = new CalibrationiBeacon();

	//
	private boolean isFirstPoint = true;
	Position finalPosition = new Position();
	Position PDRPosition = new Position();
	Position iBeaconPosition = new Position();

	//initialization of step detection parameters
	StepDetectionParameters SDparameters = new StepDetectionParameters();
	StepDetection sDfunction = new StepDetection();

	//wifi position
	//String path = "http://172.28.220.94/ipsapi/api/GetTargetByMAC?mac=10:FE:ED:AF:52:B5";
	Position WIFIPos = new Position();
	Position[] particle = new Position[500];
	Position[] particleNew = new Position[500];

	//Localization Algorithms
	LocAlgorithms_PDR pdrAlgorithm = new LocAlgorithms_PDR();
	InitialPointEstimation initialPointEstimation = new InitialPointEstimation();

	//handle
	public Handler Handler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, // full screen
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getSupportActionBar().hide();
		setContentView(R.layout.activity_main);

		//Bluetooth initialization
		BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		mBluetoothAdapter = manager.getAdapter();
        mDevices = new SparseArray<BluetoothDevice>();
        //configiBeacon.ReadWifiConfig();

		//get device's MAC address
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		WifiInfo wInfo = wifiManager.getConnectionInfo();
		//macAddress = wInfo.getMacAddress().toUpperCase();
		macAddress = "08:57:00:87:FC:9D";
		//
		InitSurface();
		InitSensors();
		InitHandle();
		mInitiBeacon.InitiBeacon();
		int i;
		for (i=0;i<500;i++){
			particle[i] = new Position();
			particleNew[i] = new Position();
		}

		//scan for bluetooth device
		startScan();

		new Thread(new WifiThread()).start();
	}

	/**************************handle***********/
	private void InitHandle() {
		Handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				String text = (String) msg.obj;
				if (text.contains("Finish")) {
					//PFalgorithm();
					//finalPosition=kalmanFilter.DoFusion(observationPosition, finalPosition, SDparameters);
					//GlabalData.currentPosition.x = finalPosition.x;
					//GlabalData.currentPosition.y = finalPosition.y;
					//Run Particle Filter
					particle = pdrAlgorithm.DoLoc(SDparameters,WIFIPos,particle,flag_WIFIchanged);
					PDRPosition.x = PDRPosition.x + SDparameters.stepLength*Math.sin(SDparameters.walking_direction*Math.PI/180.0f);
					PDRPosition.y = PDRPosition.y + SDparameters.stepLength*Math.cos(SDparameters.walking_direction*Math.PI/180.0f);
					int i;
					finalPosition.x = 0.0;
					finalPosition.y = 0.0;
					for(i=0; i<500; i++){
						finalPosition.x += particle[i].x/500;
						finalPosition.y += particle[i].y/500;
					}

					if (finalPosition.x <0.3) {
						finalPosition.x = 0.3;
					}else if (finalPosition.x > mapWidth - 0.3) {
						finalPosition.x = mapWidth-0.3;
					}
					if (finalPosition.y <0.3) {
						finalPosition.y = 0.3;
					}else if (finalPosition.y > mapHeight - 0.3) {
						finalPosition.y = mapHeight-0.3;
					}

					//clear the flag
					if(flag_WIFIchanged){
						flag_WIFIchanged = false;
					}
					//startScan();// scan after each step

				}else if (text.contains("Time")) {
					//Log.e("Timer", finalPosition.x+","+finalPosition.y);
					Draw(finalPosition, SDparameters, WIFIPos, PDRPosition);
				}else if (text.equals("Error")) {
					//Use toast to show error
					Context context = getApplicationContext();
					CharSequence t = "Error getting WIFI data!";
					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(context,t, duration);
					toast.show();
				}
			}
		};
	}

	//send message to main thread.
	public void updatetrack(String s) {
		Message msg = new Message();
		String textTochange = s;
		msg.obj = textTochange;
		Handler.sendMessage(msg);
	}

	//refresh screen every 200ms.
	public class MyThread implements Runnable {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while (true) {
				try {
					Thread.sleep(200);
					updatetrack("Time");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	//WifiThread this thread get wifiPosition every 1 second
	public class WifiThread implements Runnable{
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while (true) {
				try {
					boolean result = getWifiPosition("GetTargetByMAC","mac",macAddress);
					if (result == false){
						updatetrack("Error");
					}
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	/**************************handle***********/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	//this runs on main thread.
	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub

		double realtimeDirection_temp=0;
		double[] diffrealdirection = new double[9];

		if(event.sensor.getName() == accSensor.getName()) // acceleration
		{
			//update array
			for (int i = 0; i < accData_z.length-1; i++) {
				accData_z[i]=accData_z[i+1];
			}
			accData_z[99]=event.values[2]; // z-axis acceleration
			if (SDparameters.possible_start) {
				SDparameters.counting++;
			}

			if (isFirstPoint==true)
			{
				sDfunction.stepDetection(accData_z, oriData_a, SDparameters);
			}
		}
		else // orientation
		{
			//update array
			for (int i = 0; i < oriData_a.length-1; i++) {
				oriData_a[i]=oriData_a[i+1];
			}
			oriData_a[99]=event.values[0]; // azimuth
			for (int i = 90; i < 100-1; i++) {
				//realtimeDirection_temp += oriData_a[i];
				double difftemp;
				difftemp= oriData_a[i]-oriData_a[i+1]; //0--360 gap
				if (difftemp > 180) {
					difftemp = difftemp - 360;
				}else if (difftemp < -180) {
					difftemp = difftemp + 360;
				}
				diffrealdirection[i-90] = difftemp;
			}
			for (int i = 0; i < diffrealdirection.length; i++) {
				realtimeDirection_temp += diffrealdirection[i]*(9-i);
			}
			realtimeDirection_temp /=10.0f;
			realtimeDirection_temp +=oriData_a[90];
			if (realtimeDirection_temp < 0) {
				realtimeDirection_temp +=360;
			}else if (realtimeDirection_temp >360) {
				realtimeDirection_temp -=360;
			}

			realtimeDirection_temp = 48 + realtimeDirection_temp; // transfer
			if (realtimeDirection_temp < 0) {
				realtimeDirection_temp += 360;
			}
			SDparameters.realtimeDirction = realtimeDirection_temp;
		}


		if (SDparameters.oneStepComplete) {
			SDparameters.oneStepComplete = false;
			updatetrack("Finish");
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

	private void InitSensors()
	{
		smManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		accSensor = smManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		oriSensor = smManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

		//initialization
		for (int i = 0; i < accData_z.length; i++) {
			accData_z[i]=0;
		}
		for (int i = 0; i < oriData_a.length; i++) {
			oriData_a[i]=0;
		}
		//open sensor
		smManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_GAME);
		smManager.registerListener(this, oriSensor, SensorManager.SENSOR_DELAY_GAME);

	}


	private void InitSurface() {

		surface_view = (SurfaceView) findViewById(R.id.surfaceView1);
		sfh = surface_view.getHolder();
		sfh.addCallback(new DisplaySurfaceView());
		//surface_view.setOnTouchListener(listener);

		float x = this.getWindowManager().getDefaultDisplay().getWidth();
		float y = this.getWindowManager().getDefaultDisplay().getHeight();
		screenSize.set(x, y);
		map = BitmapFactory.decodeResource(getResources(), R.drawable.lab2);
		user = BitmapFactory.decodeResource(getResources(), R.drawable.user2);
		dot = BitmapFactory.decodeResource(getResources(), R.drawable.user);
	}

    private Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            stopScan();
            //ad.show();
        }
    };
    private Runnable mStartRunnable = new Runnable() {
        @Override
        public void run() {
            startScan();
        }
    };



	private void startScan(){
		mBluetoothAdapter.startLeScan(this);
	}


	private void stopScan(){
		mBluetoothAdapter.stopLeScan(this);
	}


	private OnTouchListener listener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent e) {

			if (isFirstPoint == false)
			{
				//int x = (int) e.getX();
				//int y = (int) e.getY();
				//finalPosition.x=(float)x*mapWidth/screenSize.x;
				//finalPosition.y=(float)y*mapHeight/screenSize.y;

				isFirstPoint = true;

			}
			//Log.e(TAG, x+","+y);
//            canvas = sfh.lockCanvas();
//			canvas.drawBitmap(
//					map,
//					null,
//					new Rect(0, 0, Math.round(screenSize.x), Math
//							.round(screenSize.y)), paint);
//    		int py = user.getHeight() / 2;
//    		int px = user.getWidth() / 2;
//            canvas.drawBitmap(user, x - px, y - py, paint);
//            sfh.unlockCanvasAndPost(canvas);
			return false;
		}
	};

	private void Draw(Position FinalPosition, StepDetectionParameters sdpara,Position wifiPosition, Position PDRPosition) {

		canvas = sfh.lockCanvas();
		paint = new Paint();
		canvas.drawBitmap(map, null, new Rect(0, 0, Math.round(screenSize.x),
				Math.round(screenSize.y)), paint);

		int final_py = dot.getHeight() / 2;
		int final_px = dot.getWidth() / 2;
		int wifi_py = dot.getHeight()/2;
		int wifi_px = dot.getWidth()/2;
		int PDR_py = user.getHeight()/2;
		int PDR_px = user.getWidth()/2;
		Float xx = new Float(screenSize.x);
		Float yy = new Float(screenSize.y);
		Double final_xxx = FinalPosition.x / mapWidth * (xx.doubleValue());
		Double final_yyy = FinalPosition.y / mapHeight * (yy.doubleValue());
		Double wifi_xxx = wifiPosition.x / mapWidth * (xx.doubleValue());
		Double wifi_yyy = wifiPosition.y / mapHeight * (yy.doubleValue());
		Double PDR_xxx = PDRPosition.x / mapWidth * (xx.doubleValue());
		Double PDR_yyy = PDRPosition.y / mapHeight * (yy.doubleValue());
		int final_x = final_xxx.intValue();
		int final_y = final_yyy.intValue();
		int wifi_x = wifi_xxx.intValue();
		int wifi_y = wifi_yyy.intValue();
		int PDR_x = PDR_xxx.intValue();
		int PDR_y = PDR_yyy.intValue();
		DecimalFormat df = new DecimalFormat("0.00");

		//Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.user1);
		Matrix matrix = new Matrix();
		//matrix.postTranslate(px, py);
		matrix.postRotate((float) sdpara.realtimeDirction);
		//matrix.setRotate((float) sdpara.realtimeDirction, px, py);
		//matrix.postTranslate(x-px, y-py);
		//canvas.drawBitmap(user,matrix,null);
		user_rotate = Bitmap.createBitmap(user, 0, 0, user.getWidth(), user.getHeight(), matrix, true);
		canvas.drawBitmap(user_rotate, PDR_x-PDR_px, PDR_y-PDR_py , paint);
		canvas.drawBitmap(dot, wifi_x-wifi_px, wifi_y-wifi_py , paint);
		canvas.drawBitmap(dot, final_x-final_px, final_y-final_py , paint);

		Paint textPaint = new Paint();
		textPaint.setTextSize(40f);
		canvas.drawText(
				"F(" + df.format(FinalPosition.x) + " , " + df.format(FinalPosition.y)
						+ ")", final_x - final_px + 50, final_y - final_py + 50, textPaint);
		canvas.drawText(
				"W(" + df.format(wifiPosition.x) + " , " + df.format(wifiPosition.y)
						+ ")", wifi_x - wifi_px + 50, wifi_y - wifi_py + 50, textPaint);
		canvas.drawText(
				"P(" + df.format(PDRPosition.x) + " , " + df.format(PDRPosition.y)
						+ ")", PDR_x - PDR_px + 50, PDR_y - PDR_py + 50, textPaint);

		sfh.unlockCanvasAndPost(canvas);
	}

	class DisplaySurfaceView implements SurfaceHolder.Callback {

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			canvas = sfh.lockCanvas();
			canvas.drawColor(Color.WHITE);
			canvas.drawRect(0, 0, screenSize.x, screenSize.y, new Paint());
			canvas.drawBitmap(
					map,
					null,
					new Rect(0, 0, Math.round(screenSize.x), Math
							.round(screenSize.y)), paint);
			sfh.unlockCanvasAndPost(canvas);
		}

		@Override
		public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2,
								   int arg3) {
			//

		}

		@Override
		public void surfaceDestroyed(SurfaceHolder arg0) {
			//

		}

	}

	@Override
	public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
		// TODO Auto-generated method stub

		String str = device.getAddress();
		if(GlabalData.iBeaconSensorList.containsKey(str) && rssi > -80){
			iBeaconPosition = GlabalData.iBeaconSensorList.get(str);
			int i,j;
			double orientation;
			double radius;
			double z_R=1;
			Random r = new Random(1);
			for(i=0;i<500;i++){
				orientation = Math.random()*2*Math.PI;
				radius = z_R/2*r.nextGaussian();
				particle[i].x = iBeaconPosition.x + radius*Math.cos(orientation);
				particle[i].y = iBeaconPosition.y + radius*Math.sin(orientation);
			}
			PDRPosition.x = iBeaconPosition.x;
			PDRPosition.y = iBeaconPosition.y;

			z_R = 0.5f;
			double sum_P_w = 0;
			double[] P_w = new double[500];
			double[] accum_P_w = new double[500];


			for (i = 0; i < 500; i++) {
				P_w[i] = (1 / (2 * Math.PI * z_R * z_R)) * Math.exp(-0.5 * z_R * z_R * ((particle[i].x - WIFIPos.x) * (particle[i].x - WIFIPos.x) + (particle[i].y - WIFIPos.y) * (particle[i].y - WIFIPos.y)));
				sum_P_w += P_w[i];
				accum_P_w[i] = 0;
			}
			P_w[0] = P_w[0] / sum_P_w;
			accum_P_w[0] = P_w[0];

			for (i = 1; i < 500; i++) {
				P_w[i] = P_w[i] / sum_P_w;
				accum_P_w[i] = accum_P_w[i - 1] + P_w[i];
			}
			for (i = 0; i < 400; i++) {
				double value = Math.random();
				for (j = 0; j < 500; j++) {
					if (accum_P_w[j] > value) {
						break;
					}
				}
				if (j < 500) {
					particleNew[i].x = particle[j].x;
					particleNew[i].y = particle[j].y;
				}
				else {
					particleNew[i].x = particle[499].x;
					particleNew[i].y = particle[499].y;
				}
			}
			for (i = 400; i < 500; i++) {
				particleNew[i].x = 36 * Math.random();
				particleNew[i].y = 15 * Math.random();
			}
			for(i=0;i<500;i++){
				particle[i].x = particleNew[i].x;
				particle[i].y = particleNew[i].y;
			}
			finalPosition.x = 0.0;
			finalPosition.y = 0.0;
			for(i=0; i<500; i++){
				finalPosition.x += particle[i].x/500;
				finalPosition.y += particle[i].y/500;
			}
		}


		/*if (calibrationiBeacon.caliBeacon(str, rssi)) {
			finalPosition.x = GlabalData.iBeaconSensorList.get(str).position.x;
			finalPosition.y = GlabalData.iBeaconSensorList.get(str).position.y;
		}*/
		/*
		if (GlabalData.iBeaconSensorList.containsKey(str)) {
			for (int i = 0; i < GlabalData.iBeaconSensorList.get(str).rssi.length-1; i++) { // update rss array
				GlabalData.iBeaconSensorList.get(str).rssi[i] = GlabalData.iBeaconSensorList.get(str).rssi[i+1];
			}
			GlabalData.iBeaconSensorList.get(str).rssi[GlabalData.iBeaconSensorList.get(str).rssi.length-1] = rssi;
			GlabalData.iBeaconSensorList.get(str).isUpdate = true;

			double smooth_rss = 0;
			for (int i = 10; i < GlabalData.iBeaconSensorList.get(str).rssi.length; i++) {
				smooth_rss += GlabalData.iBeaconSensorList.get(str).rssi[i];
			}
			for (int i = 0; i < GlabalData.iBeaconSensorList.get(str).smoothRss.length-1; i++) {
				GlabalData.iBeaconSensorList.get(str).smoothRss[i] = GlabalData.iBeaconSensorList.get(str).smoothRss[i+1];
			}
			GlabalData.iBeaconSensorList.get(str).smoothRss[GlabalData.iBeaconSensorList.get(str).smoothRss.length-1] = smooth_rss/10.0;// smooth rss
			double [] smooth_array = new double[5];
			for (int i = 0; i < smooth_array.length; i++) {
				smooth_array[i] = GlabalData.iBeaconSensorList.get(str).smoothRss[i];
			}
			//

			if (smooth_array[0] > GlabalData.rssThreshold && smooth_array[4] > GlabalData.rssThreshold) {
				GlabalData.iBeaconSensorList.get(str).flag_start = true;
			}else {
				GlabalData.iBeaconSensorList.get(str).flag_start = false;
				GlabalData.iBeaconSensorList.get(str).peakValue = -100.0;
			}

			if (GlabalData.iBeaconSensorList.get(str).flag_start) {
				if (smooth_array[2] >= smooth_array[0] && smooth_array[2] >= smooth_array[1] && smooth_array[2] >= smooth_array[3] && smooth_array[2] >= smooth_array[4]) {
					if (smooth_array[2] > GlabalData.iBeaconSensorList.get(str).peakValue) {
						//peak
						GlabalData.iBeaconSensorList.get(str).peakValue = smooth_array[2];
						finalPosition.x = GlabalData.iBeaconSensorList.get(str).position.x;
						finalPosition.y = GlabalData.iBeaconSensorList.get(str).position.y;
						//flag_detection = true;
					}
				}
			}
		}*/
	};

	private boolean getWifiPosition(String... params){
		StringBuilder urlString = new StringBuilder();
		urlString.append("http://172.28.220.94/ipsapi/api/");
		urlString.append(params[0]);
		urlString.append("?");
		urlString.append(params[1]).append("=");
		urlString.append(params[2]);

		HttpURLConnection urlConnection = null;
		URL url = null;
		InputStream inStream = null;

		try {
			url = new URL(urlString.toString());
			urlConnection = (HttpURLConnection) url.openConnection();
			//urlConnection.setRequestMethod("GET");
			//urlConnection.setDoOutput(true);
			//urlConnection.setDoInput(true);
			urlConnection.connect();
			inStream = urlConnection.getInputStream();
			BufferedReader bReader = new BufferedReader(new InputStreamReader(inStream));
			String temp;
			StringBuilder stringBuilder = new StringBuilder();
			while ((temp = bReader.readLine()) != null)
				stringBuilder.append(temp).append("\n");
			bReader.close();
			String response = stringBuilder.toString();
			if(response == null) {
				return false;
			}
			JSONObject jsonResponse = (JSONObject) new JSONTokener(response).nextValue();
			//String responseMAC = jsonResponse.getString("mac");
			Double temp_x = Double.parseDouble(jsonResponse.getString("x"));
			Double temp_y = Double.parseDouble(jsonResponse.getString("y"));
			if((temp_x - WIFIPos.x)> 0.001 || (temp_y-WIFIPos.y)> 0.001){
				flag_WIFIchanged = true;
				WIFIPos.x = temp_x;
				WIFIPos.y = temp_y;
			}
			if(initialPositionReceived == false) {
				if (WIFIPos.x !=  0.0) {
					initialPositionReceived = true;
					finalPosition.x = WIFIPos.x;
					finalPosition.y = WIFIPos.y;
					PDRPosition.x = WIFIPos.x;
					PDRPosition.y = WIFIPos.y;
					int i;
					double orientation;
					double radius;
					double z_R=1;
					Random r = new Random(1);
					for(i=0;i<500;i++){
						orientation = Math.random()*2*Math.PI;
						radius = z_R/2*r.nextGaussian();
						particle[i].x = WIFIPos.x + radius*Math.cos(orientation);
						particle[i].y = WIFIPos.y + radius*Math.sin(orientation);
					}
				}
				else{
					return false;
				}
				new Thread(new MyThread()).start(); // start real time display
			}
		} catch (Exception e) {
			Log.e("ERROR", e.getMessage(), e);
			return false;
		} finally {
			if (inStream != null) {
				try {
					// this will close the bReader as well
					inStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}
		return true;
	}
}

