package com.example.android.maxpapers.lcars;

import java.util.Date;
import java.util.GregorianCalendar;

public class DateCalc {
	private static long CONTACT = 2942996400l; 
	public static double stardate(){
		
		Date StardateOriginToday = new Date("July 15, 1987 00:00:00");
		Date StardateInputToday = new Date();
		
		long stardateToday = StardateInputToday.getTime() - StardateOriginToday.getTime();
		double dStardateToday = stardateToday / (1000 * 60 * 60 * 24 * 0.036525);
		dStardateToday = Math.floor(dStardateToday + 410000);
		dStardateToday = dStardateToday / 10;
		
		return dStardateToday;

	}
	
	public static double roundToDecimals(double d, int c) {
		int temp=(int)((d*Math.pow(10,c)));
		return (((double)temp)/Math.pow(10,c));
		}
	
	public static int getDay(){
		return new Date().getDate() + 1;
	}
	public static int getDecDay(){
		GregorianCalendar cal = new GregorianCalendar(getYear(), getMonth(), getDay());
		return Math.round(((float)(getDay() - 1) / (float)cal.getActualMaximum(GregorianCalendar.DAY_OF_MONTH)) * 10f);
	}
	
	public static int getYear(){
		return new Date().getYear() + 1900;
	}
	public static int getMonth(){
		return new Date().getMonth() + 1;
	}
	public static int getHours(){
		return new Date().getHours();
	}
	public static double getDaysToFirstContact(){
		long seconds = CONTACT - ((new Date().getTime()) / 1000);
		return roundToDecimals(((double)seconds * 100 / 86400d) / 100, 1);
	}
}
