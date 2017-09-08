/*
 * This software was developed by employees of the National Institute of 
 * Standards and Technology (NIST), an agency of the Federal Government. 
 * Pursuant to title 17 United States Code Section 105, works of NIST employees 
 * are not subject to copyright protection in the United States and are considered 
 * to be in the public domain. Permission to freely use, copy, modify, and distribute 
 * this software and its documentation without fee is hereby granted, provided that 
 * this notice and disclaimer of warranty appears in all copies.
 * THE SOFTWARE IS PROVIDED 'AS IS' WITHOUT ANY WARRANTY OF ANY KIND, EITHER EXPRESSED, 
 * IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, ANY WARRANTY THAT THE SOFTWARE 
 * WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE, AND FREEDOM FROM INFRINGEMENT, AND ANY WARRANTY THAT THE 
 * DOCUMENTATION WILL CONFORM TO THE SOFTWARE, OR ANY WARRANTY THAT THE SOFTWARE WILL 
 * BE ERROR FREE. IN NO EVENT SHALL NIST BE LIABLE FOR ANY DAMAGES, INCLUDING, BUT NOT 
 * LIMITED TO, DIRECT, INDIRECT, SPECIAL OR CONSEQUENTIAL DAMAGES, ARISING OUT OF, 
 * RESULTING FROM, OR IN ANY WAY CONNECTED WITH THIS SOFTWARE, WHETHER OR NOT BASED 
 * UPON WARRANTY, CONTRACT, TORT, OR OTHERWISE, WHETHER OR NOT INJURY WAS SUSTAINED BY 
 * PERSONS OR PROPERTY OR OTHERWISE, AND WHETHER OR NOT LOSS WAS SUSTAINED FROM, OR 
 * AROSE OUT OF THE RESULTS OF, OR USE OF, THE SOFTWARE OR SERVICES PROVIDED HEREUNDER.
 */
package pipeline3D;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author pnb
 */

public class Im2DLine {
    protected Im2DPoint pts1 = null; //pts [row,col]
    protected Im2DPoint pts2 = null; //pts [row,col]
    protected double slope;
    protected double q;
    
    // critical cases
    private double _minPtsSepar = 1.0;

    // these should become static (maybe)
    private double _maxAllowedSlope = 50000.0;//Integer.MAX_VALUE;
    private double _epsilon3 = 1.0E-6; 

    private static Log logger = LogFactory.getLog(Im2DLine.class);

    //constructor
    public Im2DLine() {
        pts1 = new Im2DPoint();
        pts2 = new Im2DPoint();
        slope = 0.0;
        q = 0.0;
    }

    public Im2DLine(Im2DPoint ptsIn1, Im2DPoint ptsIn2) {
        pts1 = new Im2DPoint(ptsIn1);
        pts2 = new Im2DPoint(ptsIn2);
        computeSlopeFromPts();
    }

    /**
     * computes slope and offset of a line given two points
     * @return
     */
    public boolean computeSlopeFromPts() {
        if (pts1 == null || pts2 == null) {
            logger.error("Error: points have not been defined");
            return false;
        }

        if (Math.abs(pts2.x - pts1.x) < _minPtsSepar) {
            if (Math.abs(pts2.y - pts1.y) < _minPtsSepar) {
                slope = 0.0;
                q = 0.0;
                return false;
            } else {
                slope = _maxAllowedSlope;
                q = pts1.x;
            }
        } else {
            if (Math.abs(pts2.y - pts1.y) < _minPtsSepar) {
                slope = 0.0;
                q = pts1.y;
            } else {
                slope = (pts2.y - pts1.y) / (pts2.x - pts1.x);
                q = pts1.y - slope * pts1.x;
            }
        }
        return true;
    }

    /**
     *compute all line points given two end points 
     * @param line
     * @param pts
     * @param numpix
     * @return
     */
    public boolean LinePoints(Im2DLine line, Im2DPoint[] pts, int numpix) {
        double x, y, x1, y1, x2, y2, deltax;
        int i;

        x1 = line.pts1.x;
        y1 = line.pts1.y;
        x2 = line.pts2.x;
        y2 = line.pts2.y;

        if (x1 == x2 && y1 == y2) {
            logger.error("Error: two identical points cannot create a line \n");
            return false;
        }
        if (!computeSlopeFromPts()) {
            return false;
        }
        if (Math.abs(line.slope) < _maxAllowedSlope) {
            deltax = (x2 - x1) / (float) numpix;

            if (Math.abs(deltax) <  _epsilon3) { 
                logger.error(" Error: slope\n");
                return false;
            }

            for (i = 0; i < numpix; i++) {
                x = x1 + deltax * i;
                pts[i].x = x;
                pts[i].y = line.slope * x + line.q;
            }
        } else {
            deltax = (y2 - y1) / (float) numpix;
            if (Math.abs(deltax) < _epsilon3) {
                logger.error(" Error: slope\n");

                return false;
            }

            for (i = 0; i < numpix; i++) {
                y = y1 + deltax * i;
                pts[i].x = line.q;
                pts[i].y = y;
            }
        }

        return true;
    } // end of LinePoints

    ////////////////////////////////////////////////////////////////////
    //setters
    public boolean setImLine(Im2DPoint ptsIn1, Im2DPoint ptsIn2) {
        pts1.setImPoint(ptsIn1);
        pts2.setImPoint(ptsIn2);
        return (computeSlopeFromPts());
    }

    public boolean setMinPtsSepar(double val) {
        if (val <= 0.0) {
            logger.error("ERROR: Min point separation must be positive");
            return false;
        }
        _minPtsSepar = val;
        return true;
    }

    public boolean setMaxAllowedSlope(double val) {
        if (val <= 0.0) {
            logger.error("ERROR: Max allowed slope should correspond to an absolute value");
            return false;
        }
        _maxAllowedSlope = val;
        return true;
    }
    public boolean setMinAllowedSlope(double val) {
        if (val <= 0.0) {
            logger.error("ERROR: Min allowed slope should correspond to an absolute value");
            return false;
        }
        _epsilon3 = val;
        return true;
    }

    /**
     * @return Returns the slope.
     */
    public double getSlope() {
        return slope;
    }

    /**
     * @param slope The slope to set.
     */
    public void setSlope(double slope) {
        this.slope = slope;
    }

    /**
     * @return Returns the q.
     */
    public double getQ() {
        return q;
    }

    /**
     * @param q The q to set.
     */
    public void setQ(double q) {
        this.q = q;
    }

    /**
     * @return Returns the pts1.
     */
    public Im2DPoint getPts1() {
        return pts1;
    }

    /**
     * @param pts1 The pts1 to set.
     */
    public void setPts1(Im2DPoint pts1) {
        this.pts1 = pts1;
    }

    /**
     * @return Returns the pts2.
     */
    public Im2DPoint getPts2() {
        return pts2;
    }

    /**
     * @param pts2 The pts2 to set.
     */
    public void setPts2(Im2DPoint pts2) {
        this.pts2 = pts2;
    }
    
    public String toString() {
        //logger.info("Im2DLine: slope=" + slope + ", q=" + q);
        String str = new String();
        str = pts1.toString();
        str+= pts2.toString();
        str+="slope=" + slope + ", q=" + q;
        return str;
    }

    /**
	 * This method computes a distance between 
	 * a point (x1,y1) and a line 
	 * dist = (a*x1 +b*y1 + c)/sqrt(a^2+b^2)
	 * 
	 * @param line
	 *            Im2DLine = line definition
	 * @param pts
	 *            Im2DPoint = point definition
	 * @return double =  distance
	 */
	public double distLineToPoint2(Im2DLine line, Im2DPoint pts) {
		double dist;

		if (Math.abs(line.getSlope()) == _maxAllowedSlope) {
			dist = Math.abs(pts.x - line.getQ());
		} else {
			if (Math.abs(line.getSlope()) > _epsilon3) {
				dist = Math.abs((line.getSlope() * pts.x - pts.y + line.getQ()) / (Math.sqrt(line.getSlope() * line.getSlope() + 1)));
			} else {
				dist = Math.abs(line.getQ() - pts.y);
			}
		}
		return dist;
	}

	public double distLineToPoint(Im2DLine line, Im2DPoint pts) {
		double dist;

		// formula from https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
		if (Math.abs(line.getSlope()) == _maxAllowedSlope) {
			dist = Math.abs(pts.x - line.getQ());
		} else {
			if (Math.abs(line.getSlope()) > _epsilon3) {
				dist = Math.abs((line.pts2.y - line.pts1.y) * pts.x - (line.pts2.x - line.pts1.x)* pts.y + line.pts2.x * line.pts1.y - line.pts2.y * line.pts1.x);
				double denom =Math.sqrt( (line.pts2.y - line.pts1.y)*(line.pts2.y - line.pts1.y) + (line.pts2.x - line.pts1.x)*(line.pts2.x - line.pts1.x) );
				if(denom > _epsilon3){
					dist = dist/denom;
				}else{
					dist = Double.MAX_VALUE;
				}
			} else {
				dist = Math.abs(line.getQ() - pts.y);
			}
		}
		return dist;
	}

	public Im2DPoint closestLinePointToPoint(Im2DLine line, Im2DPoint pts) {


		Im2DPoint res = null;
		// formula from https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
		if (Math.abs(line.getSlope()) == _maxAllowedSlope) {
			res = new Im2DPoint(line.pts1.x, pts.y);
		} else {
			if (Math.abs(line.getSlope()) > _epsilon3) {
				double x = -1*(-pts.x - line.getSlope()*pts.y) - line.getSlope()* line.getQ();
				x = x/(line.getSlope() * line.getSlope() + 1);
				double y =  line.getSlope()*(pts.x  + line.getSlope()*pts.y) + line.getQ();
				y = y/(line.getSlope() * line.getSlope() + 1);
				res = new Im2DPoint(x, y);
			} else {
				res = new Im2DPoint(pts.x, line.pts1.y);
			}
		}
		return res;
	}
	
	public static boolean isPointBetweenLineEndPoints(Im2DLine line, Im2DPoint pts) {

		//sanity check
		if(line == null || pts == null){
			System.err.println("Missing inputs");
			return false;
		}
		if( (pts.x >= line.pts1.x && pts.x <= line.pts2.x) || (pts.x <= line.pts1.x && pts.x >= line.pts2.x) ){
			if( (pts.y >= line.pts1.y && pts.y <= line.pts2.y) || (pts.y <= line.pts1.y && pts.y >= line.pts2.y) ){
				return true;
			}
		}
		return false;
	}
}
