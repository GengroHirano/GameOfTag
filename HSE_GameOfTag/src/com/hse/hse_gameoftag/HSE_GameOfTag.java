package com.hse.hse_gameoftag;

import android.app.Application;

public class HSE_GameOfTag extends Application {

	private int riskLevel ;
	
	@Override
	public void onCreate() {
		riskLevel = 0 ;
		super.onCreate();
	}
	
	public void setRiskLevel(int riskLevel){
		this.riskLevel = riskLevel ;
	}
	
	public void addRiskLevel(int riskLevel){
		this.riskLevel += riskLevel ;
	}
	
	public int getRiskLevel(){
		return riskLevel ;
	}
}
