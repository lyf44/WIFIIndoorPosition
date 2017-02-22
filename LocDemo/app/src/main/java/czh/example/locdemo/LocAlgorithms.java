package czh.example.locdemo;

public abstract class LocAlgorithms {
	
	public abstract Position[] DoLoc(StepDetectionParameters stepDetectionParameters,Position WIFIPosition, Position[] particle, Boolean flag_WIFIchanged, Position PDRposition);

}
