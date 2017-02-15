package czh.example.locdemo;

import java.util.Iterator;
import java.util.Map.Entry;

public class InitialPointEstimation {
/*
	public Position initialPosition() {
		Double sumDef=0.0;
		Double rssi_max=-77.39;
		Position position = new Position();
		Iterator<Entry<String, SensoriBeacon>> iter = GlabalData.iBeaconSensorList.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, SensoriBeacon> entry = (Entry<String, SensoriBeacon>) iter.next();
			SensoriBeacon sensor = entry.getValue();
			Double tmprssi = rssi_max-sensor.rssi[sensor.rssi.length-1];
			//if (tmprssi>rssi_max-rssi_min)
			//	tmprssi=rssi_max-rssi_min;
			Double def=1.0/Math.pow(10, (tmprssi/21.529));
			sumDef=sumDef+def;
			position.x=position.x+def*sensor.position.x;
			position.y=position.y+def*sensor.position.y;
		}
		// i changed it  position.x=position.x/sumDef;
		// i changed it  position.y=position.y/sumDef;
		position.x=10.0;
		position.y=10.0;
		return position;
	}
*/
}
