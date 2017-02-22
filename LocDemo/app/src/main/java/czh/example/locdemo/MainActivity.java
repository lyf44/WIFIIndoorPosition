package czh.example.locdemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;

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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.json.JSONTokener;

import static java.lang.Math.abs;


//TODO test DelayedPF Algorithm
//TODO 1. Draw no F_D done
//TODO 2. Instant update finish doen
//TODO 3. PDRVelocity infinity dome
//TODO 4. FinalPosition delay boundary
//TODO　final total update
//TODO initial divergence

//TODO 1. add rotation button to prompt user to correct initial heading    ignored
//TODO 3. add a constant internet stream.   ignored
//TODO fix map ignored

//TODO test wifi lag done
//TODO write delayed particle filter => valid step start and stop time!!!!!!!!!!!!! done
//TODO a step should start when DPRPosition defers from previous PDRPosition and ends when WIFI catches up with PDRPosition.
//TODO write automatic reinitialization done
//TODO improve particle filter,configure radius done
//TODO minimize action bar/action bar app button on the right/listview whole column clickable done
//TODO 5. add functionality if internet comes back done with refresh  done
//TODO  add toggle buttons for wifi PDR and Final done
//TODO 2. check how to heading comes into step detection  done
//TODO 5. prevent screen from becoming black   done
public class MainActivity extends ActionBarActivity implements SensorEventListener{// implements LeScanCallback

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
	private Bitmap reddot;
	final private double mapWidth = 35.65;
	final private double mapHeight = 16.6;
	private String macAddress;
	private PopupWindow mPopupWindow;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private View popupView;
	private TextView textView;
	private TextView textView_retryCounter;
	private TextView textView_timeCounter;
	private TextView textView_stayCounter;
	private TextView textView_walkStartTime;
	private TextView textView_walkEndTime;
	private TextView textView_WIFIUpdateTime;

	//counter
	private int wifiRetryCounter;
	private int wifiCounter;
	private int timeCounter;
	private int autoReIniCounter;
	private int velocityCounter =0;
	private int stayCounter;

	//flags
	private boolean flag_initialPositionReceived = false;
	private boolean flag_WIFIchanged = false;
	private boolean flag_WIFIAvailable = false;
	private boolean flag_showPDR = true;
	private boolean flag_showWifi = true;
	private boolean flag_showFinal = true;
	private boolean flag_showFinalDelay = false;
	private boolean flag_runWifi = true;
	private boolean flag_walkStart = false;
	private boolean flag_useDelayPF = false;
	private boolean flag_WIFIDelayUpdateFinished = true;

	//thread
	private Thread wifiThread;
	private Thread refreshThread;

	//Sensors
	SensorManager smManager;
	Sensor accSensor, oriSensor;
	public double[] accData_z = new double[100];
	public double[] oriData_a = new double[100];

	//bluetooth
	/*
	private BluetoothAdapter mBluetoothAdapter;
	private SparseArray<BluetoothDevice> mDevices;
	private BluetoothGatt mConnectedGatt;
	private InitiBeacon mInitiBeacon = new InitiBeacon();
	;
	ConfigiBeacon configiBeacon = new ConfigiBeacon();
	CalibrationiBeacon calibrationiBeacon = new CalibrationiBeacon();
    */

	//Position
	Position finalPosition = new Position();
	PDRPosition PDRPosition = new PDRPosition();
	Position finalPosition_Delay = new Position();
	PDRPosition PDRPosition_Delay = new PDRPosition();
	//Position iBeaconPosition = new Position();

	//initialization of step detection parameters
	StepDetectionParameters SDparameters = new StepDetectionParameters();
	StepDetection sDfunction = new StepDetection();

	//wifi position
	Position WIFIPos = new Position();
	Position[] particle = new Position[500];
	//Position[] particleNew = new Position[500];
	Position[] particleDelay = new Position[500];

	//Localization Algorithms
	LocAlgorithms_PF ParticleFilter = new LocAlgorithms_PF();
	LocAlgorithms_PFDelay ParticleFilter_Delay = new LocAlgorithms_PFDelay();
	//InitialPointEstimation initialPointEstimation = new InitialPointEstimation();

	//dealyed particle filter
	double PDRVelocity;
	double PDRVelocity_sum=0;
	int walkStartTime;
	int walkEndTime;
	int PDRTimeStamp;
	int delayIndex; //index of the PDRPosition being updated

	//handler
	public Handler Handler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, // full screen
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		//getSupportActionBar().hide();//hide action bar
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		/**************UI**********************/
		LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);// Initialize a new instance of LayoutInflater service
		popupView = inflater.inflate(R.layout.popupwindow_layout,null);// Inflate the custom layout/view
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		//mDrawerToggle = new ActionBarDrawerToggle(this,mDrawerLayout,R.string.drawer_open,R.string.drawer_close);
        //mDrawerLayout.setDrawerListener(mDrawerToggle);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		textView = (TextView) findViewById(R.id.textView);
		textView_retryCounter = (TextView) findViewById(R.id.textView_retryCounter);
		textView_timeCounter = (TextView) findViewById(R.id.textView_timeCounter);
		textView_stayCounter = (TextView) findViewById(R.id.textView_stayCounter);
		textView_walkStartTime = (TextView) findViewById(R.id.textView_walkStartTime);
		textView_walkEndTime = (TextView) findViewById(R.id.textView_walkEndTime);
		textView_WIFIUpdateTime = (TextView) findViewById(R.id.textView_WIFIUpdateTime);
		//this.getSupportActionBar().hide();

		/*******************Flags and counters********************/
		wifiRetryCounter=0;
		flag_initialPositionReceived= false;

		/*********************Utilities*******************************/
		//get device's MAC address
		WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		WifiInfo wInfo = wifiManager.getConnectionInfo();
		macAddress = wInfo.getMacAddress().toUpperCase();
		//macAddress = "08:57:00:87:FC:9D";

		/*********************initialize functions***********************/
		InitPopupView();
		InitDrawer();
		InitSurface();
		InitSensors();
		InitHandle();
		InitParticle();

		/**********************Thread****************************/
		wifiThread = new Thread(new WifiThread());
		refreshThread = new Thread(new RefreshThread());

		/**********************Toast*****************************/
		/*use toast to show progress*/
		Context context = getApplicationContext();
		CharSequence t = "getting WIFI data!";
		int duration = Toast.LENGTH_SHORT;
		Toast toast;
		toast = Toast.makeText(context, t, duration);
		toast.show();

		/****************Bluetooth*************************/
		//Bluetooth initialization
		/*
		BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		mBluetoothAdapter = manager.getAdapter();
        mDevices = new SparseArray<BluetoothDevice>();
        */
		//configiBeacon.ReadWifiConfig();

		//mInitiBeacon.InitiBeacon();

		//scan for bluetooth device
		//startScan();
	}

	@Override
	protected void onStart(){
		super.onStart();
		/*start wifi and refresh thread*/
		wifiThread.start();
		refreshThread.start();
	}

	/****************************
	 * Initialization functions
	 *************************************************/
	private void InitPopupView(){
		SeekBar seekBar = (SeekBar) popupView.findViewById(R.id.popup_seekBar);
		final TextView textView1 = (TextView) popupView.findViewById(R.id.popup_textView);
		Button popup_button = (Button) popupView.findViewById(R.id.popup_button);
		textView1.setText(String.valueOf(SDparameters.particle_radius));

		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			int value;
			@Override
			public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
				textView1.setText(String.valueOf(i));
				value = i;
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				SDparameters.particle_radius = value;
			}
		});

		popup_button.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View view){
				mPopupWindow.dismiss();
				Toast.makeText(getApplicationContext(), "particle radius updated", Toast.LENGTH_SHORT).show();
			}
		});
	}
	private void InitDrawer(){

		List<String> values = new ArrayList<String>();
		values.add("Refresh");
		values.add("showPDR");
		values.add("showWifi");
		values.add("showFinal");
		values.add("setRadius");
		values.add("useDelayPF");
		values.add("showF_D");

		CustomDrawerAdapter customDrawerAdapter = new CustomDrawerAdapter(this,R.layout.drawer_list_item,values);
		// Set the adapter for the list view
		mDrawerList.setAdapter(customDrawerAdapter);
		/*
		mDrawerList.setAdapter(new ArrayAdapter<String>(this,
				R.layout.drawer_list_item, values));
		*/
	}
	private void InitParticle(){
		int i;
		for (i = 0; i < 500; i++) {
			particle[i] = new Position();
			//particleNew[i] = new Position();
			particleDelay[i] = new Position();
		}
	}
	private void InitSurface() {

		surface_view = (SurfaceView) findViewById(R.id.surfaceView1);
		sfh = surface_view.getHolder();
		sfh.addCallback(new DisplaySurfaceView());

		float x = this.getWindowManager().getDefaultDisplay().getWidth();
		float y = this.getWindowManager().getDefaultDisplay().getHeight();
		screenSize.set(x, y);
		map  = BitmapFactory.decodeResource(getResources(), R.drawable.lab);
		//Bitmap map_temp = BitmapFactory.decodeResource(getResources(), R.drawable.lab);
		//map = Bitmap.createScaledBitmap(map_temp,600,280,false);
		//map = BitmapFactory.decodeFile("D:\\MyLife\\work\\FYP\\WIFI\\WIFIIndoorPosition\\LocDemo\\app\\src\\main\\res\\drawable-hdpi\\lab.png");
		Bitmap user_temp;
		user_temp = BitmapFactory.decodeResource(getResources(), R.drawable.user1);
		Matrix matrix = new Matrix();
		//matrix.postTranslate(px, py);
		matrix.postRotate(90f);
		user = Bitmap.createBitmap(user_temp, 0, 0, user_temp.getWidth(), user_temp.getHeight(), matrix, true);
		dot = BitmapFactory.decodeResource(getResources(), R.drawable.user);
		reddot = BitmapFactory.decodeResource(getResources(),R.drawable.dot);
		paint = new Paint();
	}
	private void InitSensors() {
		smManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accSensor = smManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		oriSensor = smManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

		//initialization
		for (int i = 0; i < accData_z.length; i++) {
			accData_z[i] = 0;
		}
		for (int i = 0; i < oriData_a.length; i++) {
			oriData_a[i] = 0;
		}
		//open sensor
		smManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_GAME);
		smManager.registerListener(this, oriSensor, SensorManager.SENSOR_DELAY_GAME);

	}
	private void InitHandle() {
		Handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				String text = (String) msg.obj;
				if (text.contains("PDRFinish")) {
					//a step has happened,clear stayCounter;
					stayCounter = 0;
					autoReIniCounter = 0;
					//timeCounter++;

					PDRPosition.x = PDRPosition.x
                            + SDparameters.stepLength * Math.cos(SDparameters.walking_direction * Math.PI / 180.0f);
					PDRPosition.y = PDRPosition.y
                            + SDparameters.stepLength * Math.sin(SDparameters.walking_direction * Math.PI / 180.0f);
					for (int i=0;i<500;i++) {
						particle[i].x = particle[i].x
                                + SDparameters.stepLength * Math.cos(SDparameters.walking_direction * Math.PI / 180.0f);
						particle[i].y = particle[i].y
                                + SDparameters.stepLength * Math.sin(SDparameters.walking_direction * Math.PI / 180.0f);
					}

					if (flag_useDelayPF){
						//detect walkStart
						if((!flag_walkStart) && flag_WIFIDelayUpdateFinished){
							flag_walkStart = true;
							walkStartTime = timeCounter;//store walkStart time
							showTost("walk start");
						}

						if(flag_walkStart) {
							//store PDRposition into the list along with the stepDirection and stepLength that led to this position
                            //Log.i("timeCounter",String.valueOf(timeCounter));
							PDRPosition.setTime(timeCounter);
							PDRPosition.setStepDirection(SDparameters.walking_direction);
							PDRPosition.setStepLength(SDparameters.stepLength);
							PDRPosition tempPosition = new PDRPosition(PDRPosition);//copy constructor. JAVA pass object by reference. cannnot just add PDRPosition to list.
							if (SDparameters.list_PDRPosition.size()>=1000){
								showTost("list_PDRPosition has too many data,reset");
								flag_useDelayPF = false; //when user toggle this flag, everything will be reset.
							}else {
								SDparameters.list_PDRPosition.add(tempPosition);
								Log.i("tempPosition.x",String.valueOf(tempPosition.x));
								Log.i("tempPosition.y",String.valueOf(tempPosition.y));
								Log.i("tempPosition.time",String.valueOf(tempPosition.time));

								//set the current PDRVelocity
								double gradient;
								int prev_time;
								int tmp = SDparameters.list_PDRPosition.size();
								if (tmp == 1) { // there is only 1 PDRPosition in the list
									PDRVelocity = 0;
								} else {
									prev_time = SDparameters.list_PDRPosition.get(tmp - 2).time;
									gradient = SDparameters.stepLength / (timeCounter - prev_time);
									PDRVelocity_sum += gradient;
									velocityCounter++;
									PDRVelocity = PDRVelocity_sum / velocityCounter; //PDRVelocity is the average of all registered velocity
									//Log.i("prev_time",String.valueOf(prev_time));
									//Log.i("gradient",String.valueOf(gradient));
								}
							}
						}
					}

				} else if (text.contains("Time")) {

					//Draw positions on map
					Draw(finalPosition, SDparameters, WIFIPos, PDRPosition,finalPosition_Delay,PDRPosition_Delay);

					if (!flag_useDelayPF) { // disable this function when using delayPF
						//automatic  reinitialization.
						// If PDRPosition is not updated for 60 seconds. PDR Position is reinitialized as final position
						autoReIniCounter++;
						// autoReIniCounter is reset in OnSensorChanged function when one_step_complete flag is true;
						if (autoReIniCounter > 600) {
							PDRPosition.x = finalPosition.x;
							PDRPosition.y = finalPosition.y;
							autoReIniCounter = 0;
							Context context = getApplicationContext();
							CharSequence t = "PDRPosition reinitialized";
							int duration = Toast.LENGTH_SHORT;
							Toast toast = Toast.makeText(context, t, duration);
							toast.show();
						}
					}

					//increment stayCounter, stayCouter is cleared when PDRFinish is received
					// when that counter reaches 50, meaning the PDRPosition has not changed for 5 seconds,
					// the flag_walkStart will be false
					if(flag_useDelayPF) {
						if (flag_walkStart) {
							stayCounter++;
							if (stayCounter > 50) {
								flag_walkStart = false;
								walkEndTime = timeCounter;
								showTost("walk finished, waiting for WIFI update");
								stayCounter = 0;
							}
						}
						textView_stayCounter.setText(String.valueOf(stayCounter));
					}

					//display time counter
					textView_timeCounter.setText(String.valueOf(timeCounter));
					timeCounter++;
					if (timeCounter>30000 || timeCounter<0) {
						timeCounter = 0;
					}


					if (flag_useDelayPF){
						textView_WIFIUpdateTime.setText(String.valueOf(PDRTimeStamp));
						textView_walkStartTime.setText(String.valueOf(walkStartTime));
						textView_walkEndTime.setText(String.valueOf(walkEndTime));
					}

					//test log purpose
					/*
					Log.i("timeCounter",String.valueOf(timeCounter));

					StringBuilder stringBuilder = new StringBuilder();
					stringBuilder.append(WIFIPos.x);
					stringBuilder.append(" ");
					stringBuilder.append(WIFIPos.y);
					String logData = stringBuilder.toString();
					Log.i("WifiData",logData);

					StringBuilder stringBuilder2 = new StringBuilder();
					stringBuilder2.append(PDRPosition.x);
					stringBuilder2.append(" ");
					stringBuilder2.append(PDRPosition.y);
					String logData2 = stringBuilder2.toString();
					Log.i("PDRData",logData2);*/
					/*
					StringBuilder stringBuilder = new StringBuilder();
					stringBuilder.append(finalPosition.x);
					stringBuilder.append(" ");
					stringBuilder.append(finalPosition.y);
					String logData = stringBuilder.toString();
					Log.i("final",logData);
					*/
				} else if (text.equals("WifiError")) {
					//Use toast to show error
					if (wifiRetryCounter == 1){
						Context context = getApplicationContext();
						CharSequence t = "Error getting WIFI data!retrying";
						int duration = Toast.LENGTH_SHORT;
						Toast toast = Toast.makeText(context, t, duration);
						toast.show();
					}
					textView_retryCounter.setText(String.valueOf(wifiRetryCounter));
				}else if (text.equals("WifiLost")){
					Context context = getApplicationContext();
					CharSequence t = "getting WIFI data failed";
					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(context, t, duration);
					toast.show();
					if (!flag_initialPositionReceived){
						InitPosition(false);
						flag_initialPositionReceived= true;
					}
					/*stop wifi thread*/
					wifiThread.interrupt();
					flag_runWifi = false;
				}else if (text.equals("WifiGet")){
					textView.setText(String.valueOf(wifiCounter));

					int i;
					//Run Particle Filter
					if (!flag_useDelayPF) {
						particle = ParticleFilter.DoLoc(SDparameters, WIFIPos, particle, flag_WIFIchanged, PDRPosition);
						finalPosition.x = 0.0;
						finalPosition.y = 0.0;

					/*use the average of first 400 particles to determine final position*/
						for (i = 0; i < 400; i++) {
							finalPosition.x += particle[i].x;
							finalPosition.y += particle[i].y;
						}
						finalPosition.x /= 400;
						finalPosition.y /= 400;

					/*;limit output*/
						if (finalPosition.x < 0.3) {
							finalPosition.x = 0.3;
						} else if (finalPosition.x > mapWidth - 0.3) {
							finalPosition.x = mapWidth - 0.3;
						}
						if (finalPosition.y < 0.3) {
							finalPosition.y = 0.3;
						} else if (finalPosition.y > mapHeight - 0.3) {
							finalPosition.y = mapHeight - 0.3;
						}
					}
					if  (flag_useDelayPF) {
						Log.i("PDRTimeStamp",String.valueOf(PDRTimeStamp));
						Log.i("PDRPosition_Delay.x",String.valueOf(PDRPosition_Delay.x));
						Log.i("PDRPosition_Delay.y",String.valueOf(PDRPosition_Delay.y));
						if ((walkEndTime != 0) && (PDRTimeStamp >= walkEndTime)) { //WIFI update finished
							//reset
							flag_WIFIDelayUpdateFinished = true;
							SDparameters.list_PDRPosition.clear();
							PDRVelocity = 0;
							PDRVelocity_sum = 0;
							velocityCounter = 0;
							walkStartTime = 0;
							walkEndTime = 0;
							PDRTimeStamp = 0;
							delayIndex = 0;

							//notify
							showTost("WIFI update finished");
							PDRPosition_Delay.x = PDRPosition.x;
							PDRPosition_Delay.y = PDRPosition.y;

							PDRPosition.x = finalPosition.x;
							PDRPosition.y = finalPosition.y;
						}
						if(flag_WIFIDelayUpdateFinished){
							finalPosition.x = PDRPosition.x;
							finalPosition.y = PDRPosition.y;
						}
						if (!flag_WIFIDelayUpdateFinished) {
							if (flag_WIFIchanged) { // only execute filter if wifi has given a new measurement.
								finalPosition.x = 0;
								finalPosition.y = 0;
								//Log.i("delayIndex",String.valueOf(delayIndex));
								//Run Particle Delay Filter
								//particleDelay contains the updated particle around a previous position
								particleDelay = ParticleFilter_Delay.DoLoc(SDparameters, WIFIPos, PDRPosition_Delay);

								//debug
								finalPosition_Delay.x = 0.0;
								finalPosition_Delay.y = 0.0;
								for (i = 0; i < 500; i++) {
									finalPosition_Delay.x += particleDelay[i].x;
									finalPosition_Delay.y += particleDelay[i].y;
								}
								finalPosition_Delay.x /= 500;
								finalPosition_Delay.y /= 500;


								//get finalposition_delay from particleDelay
								//stepLength and stepDirection is stored inside PDRPosition
								int j;
                                int counter=0;
								for (i = 0; i < 500; i++) {
									int tmp = delayIndex + (particleDelay[i].time);//tmp is the index of the particle in the list PDRPosition
									if ((tmp<0) || (tmp>= SDparameters.list_PDRPosition.size())){
                                        counter++;
										break;
									}
									Position tempPosition = new Position();
									tempPosition.x = particleDelay[i].x;
									tempPosition.y = particleDelay[i].y;
									for (j = tmp; j < SDparameters.list_PDRPosition.size(); j++) {
										tempPosition.x = tempPosition.x
												+ SDparameters.list_PDRPosition.get(j).stepLength
												* Math.cos(SDparameters.list_PDRPosition.get(j).stepDirection * Math.PI / 180.0f);
										tempPosition.y = tempPosition.y
												+ SDparameters.list_PDRPosition.get(j).stepLength
												* Math.sin(SDparameters.list_PDRPosition.get(j).stepDirection * Math.PI / 180.0f);
									}
									finalPosition.x += tempPosition.x;
									finalPosition.y += tempPosition.y;
								}
								finalPosition.x /= (500-counter);
								finalPosition.y /= (500-counter);
							}


						}
					}

					//clear the flag
					if (flag_WIFIchanged) {
						flag_WIFIchanged = false;
					}
				}
			}
		};
	}
	private void InitPosition(boolean flag) {
		int i;
		double orientation;
		double radius;
		Random r = new Random(1);
		if(flag) {
			finalPosition.x = WIFIPos.x;
			finalPosition.y = WIFIPos.y;
			finalPosition_Delay.x = WIFIPos.x;
			finalPosition_Delay.y = WIFIPos.y;
			PDRPosition.x = WIFIPos.x;
			PDRPosition.y = WIFIPos.y;
		}else{
			Context context = getApplicationContext();
			CharSequence t = "initial position at lyf's workbench";
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(context, t, duration);
			toast.show();
			PDRPosition.x = 7.5d;
			PDRPosition.y = 4.5d;
			finalPosition_Delay.x = 7.5d;
			finalPosition_Delay.y = 4.5d;
			finalPosition.x = 7.5d;
			finalPosition.y = 4.5d;
		}

		for (i = 0; i < 500; i++) {
			orientation = Math.random() * 2 * Math.PI;
			radius = SDparameters.particle_radius * Math.random();
			particle[i].x = PDRPosition.x + radius * Math.cos(orientation);
			particle[i].y = PDRPosition.y + radius * Math.sin(orientation);
		}

		return;
	}

	/*****************************
	 * threads
	 ***************************************/
	//send message to main thread.
	public void updatetrack(String s) {
		Message msg = new Message();
		msg.obj = s;
		Handler.sendMessage(msg);
	}

	//send message to ui
	public void showTost(CharSequence s){
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, s, duration);
		toast.show();
	}

	//refreshthread refreshes screen every 100ms.
	public class RefreshThread implements Runnable {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			while (true) {
				try {
					Thread.sleep(100);
					updatetrack("Time");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	//WifiThread this thread get wifiPosition every 1 second
	public class WifiThread implements Runnable {
		@Override
		public void run() {
			while (true) {
				if (!flag_runWifi){
					continue;
				}
				try {
					boolean result = getWifiPosition("GetTargetByMAC", "mac", macAddress);
					if ((!result) && (wifiRetryCounter<5)) {
						wifiRetryCounter++;
						updatetrack("WifiError");
					}
					if (result){
                        wifiCounter++;
                        if (wifiCounter==10000){
                            wifiCounter=0;
                        }
						updatetrack("WifiGet");
						wifiRetryCounter=0;
						if(!flag_initialPositionReceived){
							InitPosition(true);
							flag_initialPositionReceived=true;
							flag_WIFIAvailable=true;
						}
					}
					if (wifiRetryCounter==5){
						updatetrack("WifiLost");
						flag_WIFIAvailable=false;
					}
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	/******************************
	 * IMU method
	 *******************************/
	//this runs on main thread.
	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub

		double realtimeDirection_temp = 0;
		//double realDirectiondiff_temp=0;
		//double[] diffrealdirection = new double[9];

		if (event.sensor.getName() == accSensor.getName()) // acceleration
		{
			//update array, array contains the z acceleration over time
			for (int i = 0; i < accData_z.length - 1; i++) {
				accData_z[i] = accData_z[i + 1];
			}
			accData_z[99] = event.values[2];// - gravity; // z-axis acceleration
			sDfunction.stepDetection(accData_z, oriData_a, SDparameters);
		} else {
			/*orientation*/
			//update array
			for (int i = 0; i < oriData_a.length - 1; i++) {
				oriData_a[i] = oriData_a[i + 1];
			}

			/*software low pass filter to resisit abrupt changes in the device's heading*/
			if (abs(oriData_a[99]) < 1e-4) {
				realtimeDirection_temp = event.values[0];//azimuth/yaw
			} else {
				/*LPF*/
				realtimeDirection_temp = 0.2 * oriData_a[98] + 0.8 * event.values[0];// azimuth/yaw
			}
			oriData_a[99] = realtimeDirection_temp+SDparameters.heading_offset;
			SDparameters.realtimeDirction = realtimeDirection_temp+SDparameters.heading_offset;

			/*original code*/
			/*
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
			realDirectiondiff_temp /=10.0f;
			realtimeDirection_temp = oriData_a[90] + realDirectiondiff_temp;
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
			*/

		}
		if (SDparameters.oneStepComplete) {
			SDparameters.oneStepComplete = false;
			updatetrack("PDRFinish");
		}
	}

	private void Draw(Position FinalPosition, StepDetectionParameters sdpara, Position wifiPosition, Position PDRPosition, Position FinalPosition_Delay, Position PDRPosition_Delay) {

		canvas = sfh.lockCanvas();
		if (canvas==null){
			return;
		}
		//canvas.drawColor(Color.WHITE);
		canvas.drawBitmap(map, null, new Rect(0, 0, Math.round(screenSize.x),
				Math.round(screenSize.y)), paint);

		if(flag_initialPositionReceived) {
			int final_py = dot.getHeight() / 2;
			int final_px = dot.getWidth() / 2;
			int wifi_py = dot.getHeight() / 2;
			int wifi_px = dot.getWidth() / 2;
			int PDR_py = user.getHeight() / 2;
			int PDR_px = user.getWidth() / 2;
			int PDR_red_px = reddot.getWidth() /2;
			int PDR_red_py = reddot.getHeight() /2;
			Float xx = Float.valueOf(screenSize.x);
			Float yy = Float.valueOf(screenSize.y);
			Double final_xxx = FinalPosition.x / mapWidth * (xx.doubleValue());
			Double final_yyy = FinalPosition.y / mapHeight * (yy.doubleValue());
			Double final_delay_xxx = FinalPosition_Delay.x / mapWidth * (xx.doubleValue());
			Double final_delay_yyy = FinalPosition_Delay.y / mapHeight * (yy.doubleValue());
			Double wifi_xxx = wifiPosition.x / mapWidth * (xx.doubleValue());
			Double wifi_yyy = wifiPosition.y / mapHeight * (yy.doubleValue());
			Double PDR_xxx = PDRPosition.x / mapWidth * (xx.doubleValue());
			Double PDR_yyy = PDRPosition.y / mapHeight * (yy.doubleValue());
			Double PDR_red_xxx = PDRPosition_Delay.x / mapWidth * (xx.doubleValue());
			Double PDR_red_yyy = PDRPosition_Delay.y / mapHeight * (yy.doubleValue());
			int final_x = final_xxx.intValue();
			int final_y = final_yyy.intValue();
			int final_delay_x = final_delay_xxx.intValue();
			int final_delay_y = final_delay_yyy.intValue();
			int wifi_x = wifi_xxx.intValue();
			int wifi_y = wifi_yyy.intValue();
			int PDR_x = PDR_xxx.intValue();
			int PDR_y = PDR_yyy.intValue();
			int PDR_red_x = PDR_red_xxx.intValue();
			int PDR_red_y = PDR_red_yyy.intValue();
			DecimalFormat df = new DecimalFormat("0.00");

			//Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.user1);
			Matrix matrix = new Matrix();
			//matrix.postTranslate(px, py);
			matrix.postRotate((float) sdpara.realtimeDirction);
			//matrix.setRotate((float) sdpara.realtimeDirction, px, py);
			//matrix.postTranslate(x-px, y-py);
			//canvas.drawBitmap(user,matrix,null);
			user_rotate = Bitmap.createBitmap(user, 0, 0, user.getWidth(), user.getHeight(), matrix, true);
			//user_rotate = user;
			if (flag_showPDR) {
				canvas.drawBitmap(user_rotate, PDR_x - PDR_px, PDR_y - PDR_py, paint);
			}
			if (flag_WIFIAvailable && flag_showWifi) {
				canvas.drawBitmap(dot, wifi_x - wifi_px, wifi_y - wifi_py, paint);
			}
			if (flag_showFinal){
				canvas.drawBitmap(dot, final_x - final_px, final_y - final_py, paint);
			}
			if (flag_showFinalDelay){
				canvas.drawBitmap(dot, final_delay_x - final_px, final_delay_y - final_py, paint);
			}
			if (flag_useDelayPF){
				canvas.drawBitmap(reddot, PDR_red_x - PDR_red_px, PDR_red_y - PDR_red_py, paint);
			}
			//for static accuracy test
			/*
			int test_x = (int)(2 / mapWidth * (xx.doubleValue()));
			int test_y = (int)(1 / mapHeight * (yy.doubleValue()));
            canvas.drawBitmap(dot, test_x- final_px, test_y - final_py, paint);
			test_x = (int)(2 / mapWidth * (xx.doubleValue()));
			test_y = (int)(7 / mapHeight * (yy.doubleValue()));
			canvas.drawBitmap(dot, test_x- final_px, test_y - final_py, paint);
			test_x = (int)(8 / mapWidth * (xx.doubleValue()));
			test_y = (int)(10 / mapHeight * (yy.doubleValue()));
			canvas.drawBitmap(dot, test_x- final_px, test_y - final_py, paint);
			test_x = (int)(11 / mapWidth * (xx.doubleValue()));
			test_y = (int)(12 / mapHeight * (yy.doubleValue()));
			canvas.drawBitmap(dot, test_x- final_px, test_y - final_py, paint);
			test_x = (int)(14 / mapWidth * (xx.doubleValue()));
			test_y = (int)(8 / mapHeight * (yy.doubleValue()));
			canvas.drawBitmap(dot, test_x- final_px, test_y - final_py, paint);
			test_x = (int)(20 / mapWidth * (xx.doubleValue()));
			test_y = (int)(5 / mapHeight * (yy.doubleValue()));
			canvas.drawBitmap(dot, test_x- final_px, test_y - final_py, paint);
			test_x = (int)(18 / mapWidth * (xx.doubleValue()));
			test_y = (int)(11 / mapHeight * (yy.doubleValue()));
			canvas.drawBitmap(dot, test_x- final_px, test_y - final_py, paint);
			test_x = (int)(24 / mapWidth * (xx.doubleValue()));
			test_y = (int)(2 / mapHeight * (yy.doubleValue()));
			canvas.drawBitmap(dot, test_x- final_px, test_y - final_py, paint);
			test_x = (int)(27 / mapWidth * (xx.doubleValue()));
			test_y = (int)(7 / mapHeight * (yy.doubleValue()));
			canvas.drawBitmap(dot, test_x- final_px, test_y - final_py, paint);
			test_x = (int)(33 / mapWidth * (xx.doubleValue()));
			test_y = (int)(10 / mapHeight * (yy.doubleValue()));
			canvas.drawBitmap(dot, test_x- final_px, test_y - final_py, paint);
			*/

			Paint textPaint = new Paint();
			textPaint.setTextSize(40f);
			if (flag_showPDR) {
				canvas.drawText(
						"P(" + df.format(PDRPosition.x) + " , " + df.format(PDRPosition.y)
								+ ")", PDR_x - PDR_px + 50, PDR_y - PDR_py + 50, textPaint);
			}
			if (flag_WIFIAvailable && flag_showWifi) {
				canvas.drawText(
						"W(" + df.format(wifiPosition.x) + " , " + df.format(wifiPosition.y)
								+ ")", wifi_x - wifi_px + 50, wifi_y - wifi_py + 50, textPaint);
			}
			if (flag_showFinal){
				canvas.drawText(
						"F(" + df.format(FinalPosition.x) + " , " + df.format(FinalPosition.y)
								+ ")", final_x - final_px + 50, final_y - final_py + 50, textPaint);
			}
			if (flag_showFinalDelay){
				canvas.drawText(
						"D(" + df.format(FinalPosition_Delay.x) + " , " + df.format(FinalPosition_Delay.y)
								+ ")", final_delay_x - final_px + 50, final_delay_y - final_py + 50, textPaint);
			}
		}
		sfh.unlockCanvasAndPost(canvas);
	}

	private boolean getWifiPosition(String... params) {
		StringBuilder urlString = new StringBuilder();
		urlString.append("http://172.28.220.94/ipsapi/api/");
		//urlString.append("http://172.28.220.94/ipsapi/api/");
		urlString.append(params[0]);
		urlString.append("?");
		urlString.append(params[1]).append("=");
		urlString.append(params[2]);

		HttpURLConnection urlConnection = null;
		InputStream inStream = null;

		try {
			URL url = new URL(urlString.toString());
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");
			//urlConnection.setDoOutput(true);
			//urlConnection.setDoInput(true);
			urlConnection.connect();
			inStream = urlConnection.getInputStream();
			if (inStream == null) {
				return false;
			}
			BufferedReader bReader = new BufferedReader(new InputStreamReader(inStream));
			String temp;
			StringBuilder stringBuilder = new StringBuilder();
			while ((temp = bReader.readLine()) != null)
				stringBuilder.append(temp).append("\n");
			bReader.close();
			String response = stringBuilder.toString();
			if (response == null) {
				return false;
			}
			JSONObject jsonResponse = (JSONObject) new JSONTokener(response).nextValue();
			//String responseMAC = jsonResponse.getString("mac");
			Double temp_x = Double.parseDouble(jsonResponse.getString("x"));
			Double temp_y = Double.parseDouble(jsonResponse.getString("y"));
			if ((abs(temp_x - WIFIPos.x) > 0.001) || (abs(temp_y - WIFIPos.y) > 0.001)) {
				flag_WIFIchanged = true;
				WIFIPos.time = timeCounter;
				if (flag_useDelayPF){
					if(abs(PDRVelocity)>0.001 && (walkStartTime!=0)) {
						//calculate PDRTimeStamp
						PDRTimeStamp = (int) ((timeCounter - walkStartTime - SDparameters.initialDelay)
								* SDparameters.wifiVelocity / PDRVelocity + walkStartTime);

						if (PDRTimeStamp>=walkStartTime){
							flag_WIFIDelayUpdateFinished = false;// start WIFI Update
							int tmp = SDparameters.list_PDRPosition.size(); //Get the total number of PDRPosition stored
							for (int i = 0; i < tmp; i++) {
								if (SDparameters.list_PDRPosition.get(i).time > PDRTimeStamp) { //get the first index of PDRPosition that has a time larger than PDRTimestamp.
									if (i == 0){
										delayIndex = 0;
									}else if (i == (tmp-1)) {
										delayIndex = i;
									}else {
										delayIndex = i - 1;
									}
									PDRPosition_Delay = SDparameters.list_PDRPosition.get(delayIndex);//get the PDRPosition which the current wifi is measuring.
									//Log.i("PDRPosition_Delay.x",String.valueOf(PDRPosition_Delay.x));
									//Log.i("PDRPosition_Delay.y",String.valueOf(PDRPosition_Delay.y));
									break;
								}
							}
						}
					}
				}
			}else{
				flag_WIFIchanged= false;
			}
			WIFIPos.x = temp_x;
			WIFIPos.y = temp_y;

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

	/*this class is for surface holder callback*/
	class DisplaySurfaceView implements SurfaceHolder.Callback {

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			canvas = sfh.lockCanvas();
			canvas.drawColor(Color.WHITE);
			//canvas.drawRect(0, 0, screenSize.x, screenSize.y, new Paint());
			//canvas.drawRect(0,0,canvas.getWidth(),canvas.getHeight(),new Paint());
			/*canvas.drawBitmap(
					map,
					null,
					new Rect(0, 0, 600,200),
					paint);*/
			Matrix matrix = new Matrix();
			canvas.drawBitmap(map,matrix,paint);
			//Math.round(screenSize.x) Math.round(screenSize.y))
			sfh.unlockCanvasAndPost(canvas);
		}

		@Override
		public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2,
								   int arg3) {
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder arg0) {
		}
	}

	/*this class is for navigation drawer*/
	private class CustomDrawerAdapter extends ArrayAdapter<String> {

		Context context;
		List<String> drawerItemList;
		int layoutResID;

		public CustomDrawerAdapter(Context context, int layoutResourceID,
								   List<String> listItems) {
			super(context, layoutResourceID, listItems);
			this.context = context;
			this.drawerItemList = listItems;
			this.layoutResID = layoutResourceID;

		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub

			View view = convertView;

			if (view == null) {
				LayoutInflater inflater = ((Activity) context).getLayoutInflater();
				view = inflater.inflate(R.layout.drawer_list_item, parent, false);
			}

			TextView textView = (TextView) view.findViewById(R.id.drawer_TextView);
			final String mText = this.drawerItemList.get(position);
			textView.setText(mText);

			switch(position){
				case 0:
					textView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							flag_runWifi = true;
							flag_showWifi = true;
							flag_WIFIchanged = false;
							flag_showFinal = true;
							flag_showPDR = true;
							flag_initialPositionReceived = false;//force wifi thread to give initial position again.
							WIFIPos.x = 0d;
							WIFIPos.y = 0d;
							PDRPosition.x = 0d;
							PDRPosition.y = 0d;
							finalPosition.x = 0d;
							finalPosition.y = 0d;
							//if thread is interrupted, rerun the thread.
							if (wifiThread.isInterrupted()){
								wifiThread.interrupt();
							}
						}
					});
					break;
				case 1:
					textView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							if(flag_showPDR){
								flag_showPDR = false;
							}else{
								flag_showPDR = true;
							}
						}
					});
					break;
				case 2:
					textView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							if(flag_showWifi){
								flag_showWifi = false;
							}else{
								flag_showWifi = true;
							}
						}
					});
					break;
				case 3:
					textView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							if(flag_showFinal){
								flag_showFinal = false;
							}else{
								flag_showFinal = true;
							}
						}
					});
					break;
				case 4:
					textView.setOnClickListener(new View.OnClickListener(){
						@Override
						public void onClick(View view){

                /*
                    public PopupWindow (View contentView, int width, int height)
                        Create a new non focusable popup window which can display the contentView.
                        The dimension of the window must be passed to this constructor.

                        The popup does not provide any background. This should be handled by
                        the content view.

                    Parameters
                        contentView : the popup's content
                        width : the popup's width
                        height : the popup's height
                */
							// Initialize a new instance of popup window
							mPopupWindow = new PopupWindow(
									popupView,
									ViewGroup.LayoutParams.WRAP_CONTENT,
									ViewGroup.LayoutParams.WRAP_CONTENT
							);

							// Set an elevation value for popup window
							// Call requires API level 21
							if(Build.VERSION.SDK_INT>=21){
								mPopupWindow.setElevation(5.0f);
							}
							mPopupWindow.showAtLocation(mDrawerLayout, Gravity.CENTER,0,0);
						}
					});
					break;
				case 5:
					textView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							if(flag_useDelayPF){
								flag_useDelayPF = false;
								textView_walkEndTime.setVisibility(View.INVISIBLE);
								textView_walkStartTime.setVisibility(View.INVISIBLE);
								textView_WIFIUpdateTime.setVisibility(View.INVISIBLE);
								flag_showFinalDelay = false;
							}else{
								flag_useDelayPF = true;
								textView_walkStartTime.setVisibility(View.VISIBLE);
								textView_walkEndTime.setVisibility(View.VISIBLE);
								textView_WIFIUpdateTime.setVisibility(View.VISIBLE);
								flag_showFinalDelay = true;
							}
							//clear everything
							SDparameters.list_PDRPosition.clear();
							walkStartTime = 0;
							walkEndTime = 0;
							PDRTimeStamp = 0;
							PDRVelocity_sum=0;
							PDRVelocity = 0;
							velocityCounter = 0;
							delayIndex = 0;
						}
					});
					break;
				case 6:
					textView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							if(flag_showFinalDelay){
								flag_showFinalDelay = false;
							}else{
								if(flag_useDelayPF){
									flag_showFinalDelay = true;//this flag can only be set to true if flag_useDelayPF is true;
								}else{
									showTost("flag_useDelayPF is false, can't toggle");
								}
							}
						}
					});
					break;
			}


			return view;
		}
	}

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
			if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)){
				mDrawerLayout.closeDrawer(Gravity.LEFT);
			}else{
				mDrawerLayout.openDrawer(Gravity.LEFT);
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

}
	/*******************************bluetooth**********************************/
	/*
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
	*/
/************************************bluetooth******************************************/
	/*
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
		}
	};*/