package czh.example.locdemo;

/**
 * Created by USER on 2016/10/16.
 */
public class InitiBeacon {
    public void InitiBeacon(){
        String mac;
        Position []iBeaconPosition = new Position[2];
        int i;
        for(i=0;i<2;i++){
            iBeaconPosition[i] = new Position();
        }
        iBeaconPosition[0].x = 5.0;
        iBeaconPosition[0].y = 5.0;
        mac = "D4:F5:13:4C:84:E5";
        GlabalData.iBeaconSensorList.put(mac,iBeaconPosition[0]);

        iBeaconPosition[1].x = 10.0;
        iBeaconPosition[1].y = 5.0;
        mac = "D4:F5:13:4C:86:45";
        GlabalData.iBeaconSensorList.put(mac,iBeaconPosition[1]);

    }
}
 