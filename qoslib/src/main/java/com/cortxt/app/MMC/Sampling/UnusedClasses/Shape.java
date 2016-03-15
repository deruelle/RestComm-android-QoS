package com.cortxt.app.MMC.Sampling.UnusedClasses;

import com.google.gson.annotations.SerializedName;

public class Shape {
	@SerializedName("shape_id") 
	private String shapeId;
	
	@SerializedName("lat_arr") 
	private double[] latArr;
	
	@SerializedName("long_arr") 
	private double[] longArr;
	
	@SerializedName("pt_seq_arr") 
	private int[] ptSeqArr;
	
	public Shape() {}
	
	public Shape(String shapeId, double[] latArr, double[] longArr, int[] ptSeqArr) {
		this.shapeId = shapeId;
		this.latArr = latArr;
		this.longArr = longArr;
		this.ptSeqArr = ptSeqArr;
	}
	
	public String getShapeId() {
		return shapeId;
	}
	
	public double[] getLatArr() {
		return latArr;
	}
	
	public double[] getLonArr() {
		return longArr;
	}
	
	public int[] getPtSeqArr() {
		return ptSeqArr;
	}
	
	public void setShapeId(String shapeId) {
		this.shapeId = shapeId;
	}
	
	public void setLatArr(double[] latArr) {
		this.latArr = latArr;
	}
	
	public void setLongArr(double[] longArr) {
		this.longArr = longArr;
	}
	
	public void setPtSeqArr(int[] ptSeqArr) {
		this.ptSeqArr = ptSeqArr;
	}
}