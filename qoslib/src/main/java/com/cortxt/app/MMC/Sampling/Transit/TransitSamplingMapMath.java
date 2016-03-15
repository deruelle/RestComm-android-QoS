package com.cortxt.app.MMC.Sampling.Transit;

import android.location.Location;

import com.google.android.maps.GeoPoint;

public class TransitSamplingMapMath {
	
	float EPSILON = 0.001f;
	
	public boolean isPointOnLine(GeoPoint y, GeoPoint x, GeoPoint n) {
		// m = (y2-y1)/(x2-x1)
		try {
			float m = (y.getLatitudeE6() - x.getLatitudeE6()) / (y.getLongitudeE6() - x.getLongitudeE6());
			
		    float b = y.getLatitudeE6()  - m * x.getLatitudeE6();
		    // y = mx + b
		    if ( Math.abs(n.getLatitudeE6() - (m * n.getLongitudeE6() + b)) < EPSILON) {
		        return true;
		    }
		} catch(Exception e) {
			return false;
		}
	    return false;
	}
	
	public float findSlope(long latY, long longY, long latX, long longX) {
		float m = (latY - latX) / (longY - longX);
	    return m;
	}
	
	public static double distanceTo(GeoPoint p1, GeoPoint p2) {
		if(p1 == null || p2 == null)
			return 0;
		double Nx = (p2.getLongitudeE6() - p1.getLongitudeE6()) / 1000000.0;
		double Ny = (p2.getLatitudeE6() - p1.getLatitudeE6()) / 1000000.0;
		double lNl = Math.sqrt(Nx*Nx + Ny*Ny);
		return lNl;
	}
	
	public float distanceToInMeters(GeoPoint start, GeoPoint end) {
		double E6 = 1000000.0;
		double latA = start.getLatitudeE6()/E6;
		double longA = start.getLongitudeE6()/E6;
		double latB = end.getLatitudeE6()/E6;
		double longB = end.getLongitudeE6()/E6;
		
        Location location1 = new Location("");
        location1.setLatitude(latA);
        location1.setLongitude(longA);
        
        Location location2 = new Location("");
        location2.setLatitude(latB);
        location2.setLongitude(longB);
        
        float distance = location1.distanceTo(location2);
        return distance;
	}
	
/*	public GeoPoint findNextSample(GeoPoint start, GeoPoint end, double distanceInMeters) {
		// Reference: http://math.stackexchange.com/questions/25286/2d-coordinates-of-a-point-along-a-line-based-on-d-and-m-where-am-i-messing
		// delta y = (y2 - y1), y2 = start long, y1 = end long
		// delta x = (x2 - x1), x2 = start lat, x1 = end lat
		// m = delta y / delta x
		// a = arctan m
		// --- this is the GeoPoint between start and end d distance along the line --- //
		// yn = d sin a, n long
		// xn = d cos a, n lat
	
		try {
			int y1 = end.getLongitudeE6();
			int y2 = start.getLongitudeE6();
			double deltaY =  y2 - y1;
			int x1 = end.getLatitudeE6();
			int x2 = start.getLatitudeE6();
			int deltaX =  x2 - x1;
			//Avoid a divide by 0 error, also this would mean start and end are the same which is also an error
			if(deltaY == 0 || deltaX == 0)
				return null;
			double m = deltaY / deltaX;
			double a = (int) Math.atan(m);
			
			double yn = (int) (distanceInMeters * Math.sin(a));
			double xn = (int) (distanceInMeters * Math.cos(a));
			
			GeoPoint nextSample = new GeoPoint((int)(x1 + xn), (int) (y1 + yn));
			
			return nextSample;
		} catch(Exception e) {
			return null;
		}
	}  */
	
	public GeoPoint findNextGeoPoint(GeoPoint start, double m, double distance) {
		
		if(0 == m || null == start || 0 == distance)
			return null;
		
		double x2 = start.getLongitudeE6()/1000000.0;
		double y2 = start.getLatitudeE6()/1000000.0;

//		(x2−x1)^2 + (y2−y1)^2 = d^2
//		(y2−y1) / (x2−x1) = m;
		
		distance = 0.000001 * distance; //To make the same unit as y2 and x2
		double u = distance * Math.sqrt((m*m) +1);
		double v = (m * distance) / Math.sqrt((m*m) +1);
		
		// u = x2-x1
		// v = y2-y1
		
		double newX = x2 - u;
		double newY = y2 - v;
		
		GeoPoint newPoint = new GeoPoint((int) (newY*1000000.0), (int) (newX * 1000000.0));
		return newPoint;
	}
	
	public static GeoPoint findNextGeoPoint(GeoPoint X, GeoPoint Y, double lCl) {
		
		double x1 = X.getLongitudeE6()/1000000.0;
		double x2 = Y.getLongitudeE6()/1000000.0;
		double y1 = X.getLatitudeE6()/1000000.0;
		double y2 = Y.getLatitudeE6()/1000000.0;
		double dy = y2-y1;
		double dx = x2-x1;
		double lBl = Math.sqrt(dx*dx+dy*dy);
		if(lBl == 0)
			return null;
		
		double y = y1 + (y2 - y1) * lCl / lBl;
		double x = x1 + (x2 - x1) * lCl / lBl;
	
		GeoPoint newPoint = new GeoPoint((int) (y*1000000.0), (int) (x * 1000000.0));
		return newPoint;
	}
	
//	public double findlCl(GeoPoint A, GeoPoint B, double lBl) {
//		//A.(B/|B|)
//		//A = vector pointing to the station
//		//B = Segment of the polyline we're testing
//		//|B| (lBl) = The distance along B where the station projects = (Ax*Bx + Ay*By)/(lBl)
//		
//		int Ax = A.getLatitudeE6(); //GeoPoints are just storing the vectors
//		int Ay = A.getLongitudeE6();
//		int Bx = B.getLatitudeE6();
//		int By = B.getLongitudeE6();
////		double E6 = 1000000.0;
////		double Ax = A.getLatitudeE6()/E6;
////		double Ay = A.getLongitudeE6()/E6;
////		double Bx = B.getLatitudeE6()/E6;
////		double By = B.getLongitudeE6()/E6;
//		
//		//|C| (lCl) = distance on polyline to where station intersects it
//		double lCl = (Ax*Bx + Ay*By)/(B);
//		return lCl;
//	}
	
	public double findlCl(GeoPoint p1, GeoPoint p2, GeoPoint s, double lBl) {
//		A = S - P1
//		B = P2 - P1

		double Ax = (s.getLongitudeE6() - p1.getLongitudeE6()) / 1000000.0; 
		double Ay = (s.getLatitudeE6() - p1.getLatitudeE6()) / 1000000.0;
		double Bx = (p2.getLongitudeE6() - p1.getLongitudeE6()) / 1000000.0;
		double By = (p2.getLatitudeE6() - p1.getLatitudeE6()) / 1000000.0;
		
		double lCl = (Ax*Bx + Ay*By)/(lBl);
		return lCl;
	}
	
	public double findlDl(double lCl, double lAl) {
		//the offset distance to the station 
		//|C|^2 + |D|^2 = |A|^2
		//lCl = |C| or distance on polyline previous or last polyline point to where station is perpendicular to polyline
		//lAl = |A| distance from previous or last polyline point to station
		//lDl = distance of line that is from station to intersection (perpendicular line) on polyline
		
		double lDl = Math.sqrt((lAl*lAl - lCl*lCl));
		return lDl;
	}
	
	
	public GeoPoint getGeoPointOnLineDDistanceAway(double lCl, double lBl, GeoPoint B) {
		//X = X0 + Bx * |C | / |B|
		
		//B = Segment of the polyline we're testing
		
		return null;
	}
		
	public double findAngle(GeoPoint vectorB, GeoPoint vectorA) {
//		   / A
//		  /
//		 /
//		/  angle: theta
//	   /_\_________ B
		double angle = 0;
		
		double latA = vectorA.getLatitudeE6()/1000000.0;
		double lonA = vectorA.getLongitudeE6()/1000000.0;
		double latB = vectorA.getLatitudeE6()/1000000.0;
		double lonB = vectorA.getLongitudeE6()/1000000.0;
		
		double magnitudeOfA = Math.sqrt(latA * latA) + Math.sqrt(lonA * lonA);
		double magnitudeOfB = Math.sqrt(latB * latB) + Math.sqrt(lonB * lonB);
		double magnitude = magnitudeOfA * magnitudeOfB;
		
		double dotProduct = (latA * latB) + (lonA + lonB);
		if(dotProduct == 0) //This is 90 degrees
			return 90;
		//cos theta = dotProduct/magnitude
		angle = dotProduct/magnitude;
		angle = Math.acos(Math.toRadians(angle));
		return angle;
	}
	
//			Right Triangle:
//				Station			|N| = length
//			      /|			 N  = vector
//			    /  |
//		|A|	  /	   | |D|
//		    /	   |
//		  /		   |
//		/	|C|	   |
//	---------------------- Polyline
//		 		|B|
}
