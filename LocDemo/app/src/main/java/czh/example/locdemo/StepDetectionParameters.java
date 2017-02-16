package czh.example.locdemo;

public class StepDetectionParameters {
	
	public boolean possible_start = false;
	public boolean step_flag = false; //detect one step based on n_threshold
	public boolean oneStepComplete = false; // one step complete flag
	public double storePastSmoothAccZ = 0f;
	public int step_count = 0;
	public int counting = 0;
	public double trun_angle = 0f;
	public boolean trun_flag = false;
	public double walking_direction = 0f;
	public double realtimeDirction = 0;
	public double stepLength = 0.65f;
	public double[] store_walking_direction = {1000 , 0 , 0 , 0}; // in order to identify initial point, set 1000
	public double heading_offset = 0f;
}
