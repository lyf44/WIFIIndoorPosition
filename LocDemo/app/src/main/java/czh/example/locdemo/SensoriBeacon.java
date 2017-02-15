package czh.example.locdemo;

public class SensoriBeacon {
	
	public String BTaddress=""; // AP mac address
	public Integer major = 0;
	public Integer minor = 0;
	public Integer[] rssi={-100,-100,-100,-100,-100,-100,-100,-100,-100,-100,-100,-100,-100,-100,-100,-100,-100,-100,-100,-100};// AP rssi
	public Double[] smoothRss = {-100.0,-100.0,-100.0,-100.0,-100.0};
	public Position position = new Position(); // AP position;
	public Boolean isUpdate = false; // is AP rssi update?
	
	public Boolean flag_start = false;
	public Boolean flag_possibleMax = false;
	public Boolean flag_peak = false;
	public Double peakValue = -100.0;

}
