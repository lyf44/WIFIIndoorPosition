package czh.example.locdemo;


import android.graphics.Paint;

import java.util.Random;

public class LocAlgorithms_PF extends LocAlgorithms{

	@Override
	public Position[] DoLoc(StepDetectionParameters stepDetectionParameters, Position WIFIPosition, Position[] particle, Boolean flag_WIFIchanged,Position PDRposition) {
		// TODO Auto-generated method stub
		//StepDetectionParameters para;
		Position[] particleNew = new Position[500];
		int i, j;
		for (i = 0; i < 500; i++) {
			particleNew[i] = new Position();
		}
		if(flag_WIFIchanged) {
			double z_R = stepDetectionParameters.z_R_Delay; // vairance of x and y
			double sum_P_w = 0;
			double[] P_w = new double[500];
			double[] accum_P_w = new double[500];

			for (i = 0; i < 500; i++) {
				//3D normal distribution, with covariance=0.and variance_y = variance_x = z_R
				P_w[i] = (1 / (2 * Math.PI * z_R * z_R)) *
                        Math.exp( ((-1) / (2*z_R * z_R)) * ((particle[i].x - WIFIPosition.x) * (particle[i].x - WIFIPosition.x)
                                + (particle[i].y - WIFIPosition.y) * (particle[i].y - WIFIPosition.y)));
				sum_P_w += P_w[i];
				accum_P_w[i] = 0;
			}
			P_w[0] = P_w[0] / sum_P_w;
			accum_P_w[0] = P_w[0];

			//normalize
			for (i = 1; i < 500; i++) {
				P_w[i] = P_w[i] / sum_P_w;
                //accum_P_w containst the sum of all values from index 0 to current i;
				accum_P_w[i] = accum_P_w[i - 1] + P_w[i];
			}
			for (i = 0; i < 400; i++) {
				double value = Math.random(); //from 0 to 1
				//select particle based on cumulative distributuon function
				for (j = 0; j < 500; j++) {
					if (accum_P_w[j] > value) {
						break;
					}
				}
				if (j < 500) {
					particleNew[i].x = particle[j].x;
					particleNew[i].y = particle[j].y;
				} else {
					//the overall sum is not larger than random value.
					particleNew[i].x = particle[499].x;
					particleNew[i].y = particle[499].y;
				}
			}
			for (i = 400; i < 500; i++) {
				//redistribute random particle around the map
				//particleNew[i].x = 36 * Math.random();
				//particleNew[i].y = 15 * Math.random();
				/* redistribute around PDR position*/
				Random r = new Random(1);
				double orientation = Math.random() * 2 * Math.PI;
				double radius = stepDetectionParameters.particle_radius * Math.random();// r.nextGaussian();
				particleNew[i].x = PDRposition.x + radius * Math.cos(orientation);
				particleNew[i].y = PDRposition.y + radius * Math.sin(orientation);
			}
		}
		else{
			//else if wifi signal is not found, no update is performed
			for (i = 0; i < 500; i++) {
				particleNew[i] = particle[i];
			}
		}
		return particleNew;
	}

}
