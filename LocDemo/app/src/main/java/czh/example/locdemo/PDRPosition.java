package czh.example.locdemo;

import java.util.Random;

/**
 * Created by luyun on 5/4/2017.
 */

public class PDRPosition extends Position {
    public double stepDirection;
    public double stepLength;

    public void setStepDirection(double value){
        stepDirection = value;
    }

    public void setStepLength(double value){
        stepLength = value;
    }

    public PDRPosition (){}

    public PDRPosition(PDRPosition aPDRPosition){
        this.x = aPDRPosition.x;
        this.y = aPDRPosition.y;
        this.time = aPDRPosition.time;
        this.stepLength = aPDRPosition.stepLength;
        this.stepDirection = aPDRPosition.stepDirection;
    }


}
