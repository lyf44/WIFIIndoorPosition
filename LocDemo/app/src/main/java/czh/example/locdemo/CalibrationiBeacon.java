package czh.example.locdemo;

public class CalibrationiBeacon {
/*
	public Boolean caliBeacon(String str, Integer rssi) {

		Boolean flag_detection = false;
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
						//position.x = GlabalData.iBeaconSensorList.get(str).position.x;
						//position.y = GlabalData.iBeaconSensorList.get(str).position.y;
						flag_detection = true;
					}
				}
			}
		}
		
		return flag_detection;
		
	}
	*/
}
