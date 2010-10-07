package com.example.android.maxpapers.lcars;

import android.graphics.Rect;

public class ShipHotSpot {
	Rect hotspot = new Rect();
	String name = "";
	/**
	 * @param hotspot
	 * @param name
	 */
	public ShipHotSpot(Rect hotspot, String name) {
		super();
		this.hotspot = hotspot;
		this.name = name;
	}
	
}