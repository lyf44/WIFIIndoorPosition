package czh.example.locdemo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;
import android.util.Xml;



public class ConfigiBeacon {
	/*
    public void ReadWifiConfig() {
		XmlPullParser parser = Xml.newPullParser();
		try {
			File xmlFlie = new File(GlabalData.path + "/iBeaconConfig.conf");
			InputStream inputStream = new FileInputStream(xmlFlie);
			parser.setInput(inputStream, "gb2312");
			int eventType = parser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.START_DOCUMENT:
					break;
				case XmlPullParser.START_TAG:
					String name = parser.getName();
					if ("Sensor".equals(name)) {
						String major = parser.getAttributeValue(0).toString();
						String minor = parser.getAttributeValue(1).toString();
						String mac = parser.getAttributeValue(2).toString();
						String posx = parser.getAttributeValue(3).toString();
						String posy = parser.getAttributeValue(4).toString();
						SensoriBeacon sensor = new SensoriBeacon();
						sensor.major = Integer.valueOf(major);
						sensor.minor = Integer.valueOf(minor);
						sensor.BTaddress = mac;
						sensor.position.x = Double.valueOf(posx);
						sensor.position.y = Double.valueOf(posy);
						GlabalData.iBeaconSensorList.put(mac, sensor);

					}
					break;
				}
				eventType = parser.next();
			}
		} catch (XmlPullParserException e) {
			Log.e("error", e.getMessage());
		} catch (IOException e) {
			Log.e("error", e.getMessage());
		}
	}
*/
}
