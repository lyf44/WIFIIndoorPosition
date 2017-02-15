package czh.example.locdemo;

import java.io.File;
import java.util.Hashtable;

import android.os.Environment;

public class GlabalData {
	// WIFIsensor map, <macAddress,WiFiSensor>
	public static Hashtable<String, Position> iBeaconSensorList = new Hashtable<String, Position>();
	
	public static Position wifiPos = new Position();
	// Current position
	public static Position currentPosition = new Position();
	
	private static File sd = Environment.getExternalStorageDirectory();
	public static String path = sd.getPath() + "/iBeaconSensor";
	
	public static double rssThreshold = -83;

}
