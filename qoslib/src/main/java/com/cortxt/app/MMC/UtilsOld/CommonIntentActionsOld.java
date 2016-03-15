package com.cortxt.app.MMC.UtilsOld;

/**
 * There are some intent actions that are used by multiple recipients.
 * These cannot be defined in the recipients themselves because we would
 * have to define them in multiple places and that would lead to a maintenance
 * hassle. We cannot cross-use the action strings because that would be a 
 * very bad practice and would haphazard code. So we declare them here and then
 * reference them in the respective recipients.
 * 
 * <b>
 * Note: As a design principle, all recipients of these actions must reference
 * the corresponding strings as public static final String variables.
 * </b>
 * @author abhin
 *
 */
public class CommonIntentActionsOld {
	/**
	 * This action string is used when letting the recipient know of the latest location
	 */
	public static final String LOCATION_UPDATE_ACTION = "com.cortxt.app.MMC.utils.CommonIntentActions.LOCATION_UPDATE";
	
	/**
	 * This action string is used when letting the recipient know when the gps has been turned ON/OFF
	 */
	public static final String GPS_STATUS_UPDATE_ACTION = "com.cortxt.app.MMC.utils.CommonIntentActions.GPS_STATUS_UPDATE";
}
