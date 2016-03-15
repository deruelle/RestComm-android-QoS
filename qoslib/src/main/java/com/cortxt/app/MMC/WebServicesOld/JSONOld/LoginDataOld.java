package com.cortxt.app.MMC.WebServicesOld.JSONOld;

/**
 * @author abhin
 *
 */
public class LoginDataOld {
	private String email;
	private String password;
	
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}	
	
	public LoginDataOld() {
	}
	/**
	 * @param email
	 * @param password
	 */
	public LoginDataOld(String email, String password) {
		this.email = email;
		this.password = password;
	}
}
