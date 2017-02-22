package czh.example.locdemo;

import java.util.ArrayList;
import java.util.List;

public class StepDetectionParameters {
	
	public boolean possible_start = false;
	public boolean step_flag = false; //detect one step based on n_threshold
	public boolean oneStepComplete = false; // one step complete flag
	public double storePastSmoothAccZ = 0f;
	public int step_count = 0;
	public int counting = 0;
	//public double turn_angle = 0f;
	//public boolean turn_flag = false;
	public double walking_direction = 0f;
	public double realtimeDirction = 0;
	public double stepLength = 0.65f;
	//public double[] store_walking_direction = {1000 , 0 , 0 , 0}; // in order to identify initial point, set 1000
	public double heading_offset = 35f;
	public double z_R = 5f;
    public double z_R_Delay = 2f;
	public int particle_radius =5;
    public int particle_radius_delayPF = 10;
	//public List<Double> list_walking_direction = new ArrayList<Double>();
	//public List<Double> list_step_length = new ArrayList<Double>();
	public List<PDRPosition> list_PDRPosition = new ArrayList<PDRPosition>();
	public float wifiVelocity = 0.03f;
	public int initialDelay = 47;
}
