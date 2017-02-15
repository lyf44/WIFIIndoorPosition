package czh.example.locdemo;


import android.graphics.Paint;

public class LocAlgorithms_PDR extends LocAlgorithms{

	@Override
	public Position[] DoLoc(StepDetectionParameters stepDetectionParameters, Position WIFIPosition, Position[] particle, Boolean flag_WIFIchanged) {
		// TODO Auto-generated method stub
		//StepDetectionParameters para;
		Position[] particleNew = new Position[500];
		int i, j;
		for (i = 0; i < 500; i++) {
			particleNew[i] = new Position();
		}
		if(flag_WIFIchanged) {
			float z_R = 0.5f;
			double sum_P_w = 0;
			double[] P_w = new double[500];
			double[] accum_P_w = new double[500];


			for (i = 0; i < 500; i++) {
				particle[i].x = particle[i].x + stepDetectionParameters.stepLength * Math.sin(stepDetectionParameters.walking_direction * Math.PI / 180.0f);
				particle[i].y = particle[i].y + stepDetectionParameters.stepLength * Math.cos(stepDetectionParameters.walking_direction * Math.PI / 180.0f);
				P_w[i] = (1 / (2 * Math.PI * z_R * z_R)) * Math.exp(-0.5 * z_R * z_R * ((particle[i].x - WIFIPosition.x) * (particle[i].x - WIFIPosition.x) + (particle[i].y - WIFIPosition.y) * (particle[i].y - WIFIPosition.y)));
				sum_P_w += P_w[i];
				accum_P_w[i] = 0;
			}
			P_w[0] = P_w[0] / sum_P_w;
			accum_P_w[0] = P_w[0];

			for (i = 1; i < 500; i++) {
				P_w[i] = P_w[i] / sum_P_w;
				accum_P_w[i] = accum_P_w[i - 1] + P_w[i];
			}
			for (i = 0; i < 400; i++) {
				double value = Math.random();
				for (j = 0; j < 500; j++) {
					if (accum_P_w[j] > value) {
						break;
					}
				}
				if (j < 500) {
					particleNew[i].x = particle[j].x;
					particleNew[i].y = particle[j].y;
				} else {
					particleNew[i].x = particle[499].x;
					particleNew[i].y = particle[499].y;
				}
			}
			for (i = 400; i < 500; i++) {
				particleNew[i].x = 36 * Math.random();
				particleNew[i].y = 15 * Math.random();
			}
		}
		else{
			for(i=1;i<500;i++){
				particleNew[i].x=particle[i].x+stepDetectionParameters.stepLength * Math.sin(stepDetectionParameters.walking_direction * Math.PI / 180.0f);
				particleNew[i].y=particle[i].y+stepDetectionParameters.stepLength * Math.cos(stepDetectionParameters.walking_direction * Math.PI / 180.0f);
			}
		}
		return particleNew;
	}

}
