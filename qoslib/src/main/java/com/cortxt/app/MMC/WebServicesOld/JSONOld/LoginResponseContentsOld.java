package com.cortxt.app.MMC.WebServicesOld.JSONOld;

public class LoginResponseContentsOld {
	private int iUser;
	private String apiKey;
	public LoginResponseContentsOld() {
		
	}
	public LoginResponseContentsOld(int iUser, String apiKey){
		this.iUser = iUser;
		this.apiKey = apiKey;
	}
	public int getIUser(){
		return iUser;
	}
	public void setIUser(int iUser){
		this.iUser = iUser;
	}
	public String getApiKey(){
		return apiKey;
	}
	public void setApiKey (String apiKey){
		this.apiKey = apiKey;
	}
}
