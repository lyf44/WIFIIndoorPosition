package czh.example.locdemo;

import java.util.Random;

/**
 * Created by luyun on 27/3/2017.
 */

public class LocAlgorithms_PFDelay {
    public Position[] DoLoc(StepDetectionParameters stepDetectionParameters, Position WIFIPosition,PDRPosition PDRposition) {
        Position[] particleNew = new Position[500];
        int i, j;
        for (i = 0; i < 500; i++) {
            particleNew[i] = new Position();
        }

        //distribute particles around PDRPosition_Delay
        Position[] particleDelay = new Position[500];
        for (i = 0; i < 500; i++) {
            particleDelay[i] = new Position();
            particleDelay[i].initializeParticle(PDRposition.time,PDRposition.x,PDRposition.y,stepDetectionParameters.particle_radius_delayPF);
        }

        double z_R = stepDetectionParameters.z_R; // vairance of x and y
        double sum_P_w = 0;
        double[] P_w = new double[500];
        double[] accum_P_w = new double[500];

        for (i = 0; i < 500; i++) {
            //3D normal distribution, with covariance=0.and variance_y = variance_x = z_R
            P_w[i] = (1 / (2 * Math.PI * z_R * z_R)) *
                    Math.exp( ((-1) / (2*z_R * z_R)) * ((particleDelay[i].x - WIFIPosition.x) * (particleDelay[i].x - WIFIPosition.x)
                            + (particleDelay[i].y - WIFIPosition.y) * (particleDelay[i].y - WIFIPosition.y)));
            sum_P_w += P_w[i];
            accum_P_w[i] = 0;
        }
        P_w[0] = P_w[0] / sum_P_w;
        accum_P_w[0] = P_w[0];

        //normalize
        for (i = 1; i < 500; i++) {
            P_w[i] = P_w[i] / sum_P_w;
            //accum_P_w contains the sum of all values from index 0 to current i;
            accum_P_w[i] = accum_P_w[i - 1] + P_w[i];
        }
        for (i = 0; i < 500; i++) {
            double value = Math.random(); //from 0 to 1
            //select particle based on cumulative distribution function
            for (j = 0; j < 500; j++) {
                if (accum_P_w[j] > value) {
                    break;
                }
            }
            if (j < 500) {
                particleNew[i].x = particleDelay[j].x;
                particleNew[i].y = particleDelay[j].y;
            } else {
                //the overall sum is not larger than random value.
                particleNew[i].x = particleDelay[499].x;
                particleNew[i].y = particleDelay[499].y;
            }
        }
            /*
            for (i = 400; i < 500; i++) {
                //redistribute random particle around the map
                //particleNew[i].x = 36 * Math.random();
                //particleNew[i].y = 15 * Math.random();
				// redistribute around PDR position
                Random r = new Random(1);
                double orientation = Math.random() * 2 * Math.PI;
                double radius = stepDetectionParameters.particle_radius * Math.random();// r.nextGaussian();
                particleNew[i].x = PDRposition.x + radius * Math.cos(orientation);
                particleNew[i].y = PDRposition.y + radius * Math.sin(orientation);
            }*/

        return particleNew;
    }
}
