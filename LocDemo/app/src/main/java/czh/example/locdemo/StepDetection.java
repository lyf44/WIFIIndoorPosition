package czh.example.locdemo;

import java.util.ArrayList;
import java.util.List;

public class StepDetection {
	
	private float p_threshold = 0.8f;
	private float n_threshold = -0.5f; //step detection, threshold 
	
	public void stepDetection(double[] accData_z, double[] oriData_a, StepDetectionParameters SDParameters) {
 		double smooth_accZ = 0f;
 		double walking_directionTemp = 0f;
 		double maxAcc = 0;
 		double minAcc = 0;
		List<Double> stepAccData = new ArrayList<Double>();


 		for (int i = 90; i < 99; i++) { // 10 order smooth
 			smooth_accZ +=accData_z[i];
 		}
 		smooth_accZ/=10; //obtain smooth output

 		if ((!SDParameters.possible_start) && (smooth_accZ > p_threshold)) { // one step is detected, waiting to the step ending point.
			SDParameters.possible_start = true;
 			SDParameters.counting = 0;
 		}

        //step started, store data
		if(SDParameters.possible_start){
			stepAccData.add(smooth_accZ);
			walking_directionTemp += oriData_a[99];
		}

		if (stepAccData.size()>10000){ // too many data, something is wrong.==> reset
			stepAccData.clear();
			SDParameters.possible_start=false;
		}

 		if (SDParameters.possible_start && (smooth_accZ < n_threshold)) { //step is valid, wait for step complete
 			SDParameters.step_flag = true;
 		}

		//if (SDParameters.step_flag && (SDParameters.storePastSmoothAccZ*smooth_accZ<0f)) { //steo is complete when the acceleration becomes positive again.
		if (SDParameters.step_flag && (smooth_accZ>0f)) { //steo is complete when the acceleration becomes positive again.
			SDParameters.step_flag = false;
			SDParameters.possible_start = false;
 			SDParameters.step_count++;
 			maxAcc = stepAccData.get(0);
			minAcc = stepAccData.get(0);

			/*obtain max and min acceleration values*/
			for (int i = 0; i<stepAccData.size(); i++) {
				if (stepAccData.get(i) > maxAcc) {
					maxAcc = stepAccData.get(i);
				}else if (stepAccData.get(i) < minAcc) {
					minAcc = stepAccData.get(i);
				}
 			}

			SDParameters.stepLength = 0.4*Math.pow(maxAcc-minAcc, 0.25);
			walking_directionTemp /= (double)(stepAccData.size()); // walking direction
			SDParameters.walking_direction = SDParameters.heading_offset + walking_directionTemp; // changed based on test environment**********************important************50.0f********

			/*original code*/
			//find the step starting point
 			/*for (int i = accData_z.length - SDParameters.counting-1 ; i > 0; i--) {  // in order to obtain walking direction
			    maxAcc = accData_z[i]; // obtain walking length
			    minAcc = accData_z[i];
			    for (int j = i; j < oriData_a.length; j++) {
				    walking_directionTemp += oriData_a[j];
				    if (accData_z[j] > maxAcc) { //obtain max and min acceleration values
					    maxAcc = accData_z[j];
					}else if (accData_z[j] < minAcc) {
						minAcc = accData_z[j];
					}
				}
				SDParameters.stepLength = 0.4*Math.pow(maxAcc-minAcc, 0.25);
				walking_directionTemp /= (double)(oriData_a.length-i); // walking direction
				SDParameters.walking_direction = 132.0-walking_directionTemp; // changed based on test environment**********************important************50.0f********

				break;
			}
			*/
 			//nstepsTV.setText(String.valueOf(step_count)+"/n"+String.valueOf(walking_direction));

			/*store walking direction*/
 			if (SDParameters.store_walking_direction[0] == 1000) {
				for (int j = 0; j < SDParameters.store_walking_direction.length; j++) {
					SDParameters.store_walking_direction[j] = SDParameters.walking_direction;
				}
			}else {
				for (int j = 0; j < SDParameters.store_walking_direction.length -1; j++) {
					SDParameters.store_walking_direction[j] = SDParameters.store_walking_direction[j+1];
				}
				SDParameters.store_walking_direction[3] = SDParameters.walking_direction;
			}
 			
 			SDParameters.oneStepComplete = true;
 		}
 		
 		SDParameters.storePastSmoothAccZ = smooth_accZ; //store past value
 	}
	
	public void AnchorDetection(StepDetectionParameters SDParameters) {
		SDParameters.trun_angle = 0f;
		for (int i = 0; i < SDParameters.store_walking_direction.length -1; i++) {
			SDParameters.trun_angle += (SDParameters.store_walking_direction[i+1]-SDParameters.store_walking_direction[i]);
		}
		if (SDParameters.trun_angle > 60) {
			SDParameters.trun_flag = true;
		}else {
			SDParameters.trun_flag = false;
		}
	}

}
