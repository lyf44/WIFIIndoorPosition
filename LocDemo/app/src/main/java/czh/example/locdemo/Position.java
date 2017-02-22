package czh.example.locdemo;


import java.util.Random;

import static java.lang.Math.abs;

public class Position {
	public double x=0.0;
	public double y=0.0;
	//public Integer floor=0;
		
	//for wifi filter
	/*
	public Double wifi_dx=0.0;
	public Double wifi_dy=0.0;
	public Double wifi_vx=0.0;
	public Double wifi_vy=0.0;
	public Double wifi_ax=0.0;
	public Double wifi_ay=0.0;
	
	//for map info
	public String hisNearIdString="";
	public String nearId="";
    public String nearNextId="";
    public Double minDis1=0.0;
    public Double minDis2=0.0;
    public Double minAng=0.0;
    */


	//for WIFI delay
	public int time = 0;

	public void setTime(int time){
		this.time = time;
	}

	public void initializeParticle(int timestamp,double x, double y,int particle_radius){
		double timeVar,orientation,radius;
		Random random = new Random();
		timeVar = random.nextGaussian();
		this.time = (int) (timeVar);// + timestamp
		orientation = Math.random() * 2 * Math.PI;
		radius = particle_radius * Math.random();
		this.x = x + radius * Math.cos(orientation);
		this.y = y + radius * Math.sin(orientation);
	}

}
