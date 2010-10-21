package com.example.android.maxpapers.lcars;

import android.graphics.Path;

public class LCARSPath {

	/**
	 * Creates a path representing the top-right corner of a typical LCARS frame
	 * 
	 * @param x1
	 * @param y1
	 * @param h
	 * @param x2
	 * @param y2
	 * @param w
	 * @return
	 */
	public static Path getTopRightCorner(float x1, float y1, float h, float x2,
			float y2, float w) {
		float insideCurve = (w > h)?h:w;
		Path path = new Path();
		path.moveTo(x1, y1);
		float cx1 = x2 - (w * .75f);
		path.lineTo(cx1, y1);
		path.cubicTo(cx1 + w * .5f, y1, x2, y1 + h * .5f, x2, y1 + h);
		path.lineTo(x2, y2);
		float x3 = x2 - w;
		float y3 = y1 + h;
		float y4 = y3 + insideCurve*.5f;
		path.lineTo(x3, y2);
		path.lineTo(x3, y3);
		path.cubicTo(x3, y4, x3 - insideCurve * .5f, y3, x3-insideCurve, y3);
		path.lineTo(x1, y3);
		path.close();
		return path;
	}

}
