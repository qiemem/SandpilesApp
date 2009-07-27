/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.headb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Collection;

import gnu.trove.TIntArrayList;

/**
 *
 * @author headb
 */
public class DelaunayTriangulation {
	//private Float2dArrayList points;

	private Float2dArrayList triangles;
	private int parentTri;
	private ArrayList<TIntArrayList> pointsToTris;
	private Int2dArrayList trisToPoints;
	private final float PI = (float) Math.PI;
	private ArrayList<int[]> triTree;
	private final float ERROR_TOLERANCE = 0.00001f;

	private Float2dArrayList points;

	public DelaunayTriangulation(Float2dArrayList points) {
		//System.err.println("init");
		//this.points = new Float2dArrayList(pts);
		trisToPoints = new Int2dArrayList(0, 3);
		pointsToTris = new ArrayList<TIntArrayList>();
		triTree = new ArrayList<int[]>();
		if(points.isEmpty())
			return;

		float trX = points.get(0,0);
		float trY = points.get(0,1);

		for (int i=1; i<points.rows(); i++) {
			float x = points.get(i, 0);
			float y = points.get(i, 1);
			if (y > trY) {
				trX = points.get(i,0);
				trY = points.get(i,1);
			} else if (y == trY && x > trX) {
				trX = x;
				trY = y;
			}
		}
		float startX = trX+1f;
		float startY = trY+1f;
		//Calculate the bounding triangle with top right at startPt.
		float largestAngle = PI;
		float smallestAngle = 2 * PI;
		float largestDist = 0f;
		for (int i=0; i<points.rows(); i++) {
			//if(p==startPt)
			//	continue;
			float px = points.get(i,0);
			float py = points.get(i,1);
			largestAngle = Math.max(atan2(px, py, startX, startY), largestAngle);
			smallestAngle = Math.min(atan2(px, py, startX, startY), smallestAngle);
			largestDist = Math.max(largestDist, dist(startX, startY, px, py));
		}
		//System.err.println(largestAngle);
		// For simiplicity's sake, we make the bounding triangle iso.
		// We make the sides long enough so that the base of the triangle is
		// further from startPt than any element of points. Thus, we guarantee
		// that the triangle is in fact bounding.
		// We bump the tl point up just a touch so that all points as high as
		// startPt are contained in the circle.
		float brAngle = largestAngle + (2 * PI - largestAngle) / 2f;
		float tlAngle = smallestAngle == PI ? PI - (2 * PI - brAngle) / 2f : smallestAngle - (smallestAngle - PI) / 2f;
		float length = 2f * (float) (largestDist / Math.cos((brAngle - tlAngle) / 2f));
		float tlX = startX + length * (float) Math.cos(tlAngle);
		float tlY = startY + length * (float) Math.sin(tlAngle);
		float brX = startX + length * (float) (Math.cos(brAngle));
		float brY = startY + length * (float) (Math.sin(brAngle));

		int startPt = points.rows();
		int tl = startPt+1;
		int br = tl+1;
		points.addRow(startX, startY);
		points.addRow(tlX, tlY);
		points.addRow(brX, brY);
		parentTri = addTriangle(startPt, tl, br);
		for (int p = 0; p < points.rows(); p++) {
			try {
				addPoint(p);
			} catch (RuntimeException e) {
				System.err.println("WARNING: Couldn't add a point. Throwing it out.");
				System.err.println("Point: " + points.get(p,0) + " " + points.get(p,1));
				System.err.println("Largest angle: " + largestAngle);
				System.err.println("Smallest angle: " + smallestAngle);
				System.err.println("startPt: " + startX + " " + startY);
				System.err.println("brAngle: " + brAngle);
				System.err.println("tlAngle: " + tlAngle);
				System.err.println("length: " + length);
			}
		}
		removePoint(tl);
		removePoint(br);
		removePoint(startPt);/*
		triangles = new Float2dArrayList(0, 9);
		for(int t=0; t<triangles.size();t++){
			if(triTree.get(t)==null){
				for(int c=0; c<3; c++){
					float x = x(trisToPoints.get(t,c));
					float y = y(trisToPoints.get(t,c));
					triangles.addRow(x,y);
				}
			}
		}*/
		triTree = null;
	}

	protected float x(int p){
		return points.get(p,0);
	}

	protected float y(int p){
		return points.get(p,1);
	}

	protected int p1(int tri){
		return trisToPoints.get(tri,0);
	}

	protected int p2(int tri){
		return trisToPoints.get(tri,1);
	}

	protected int p3(int tri){
		return trisToPoints.get(tri,2);
	}

	private float dist(float x1, float y1, float x2, float y2) {
		return (float) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
	}

	private float dist(int p1, int p2){
		return dist(points.get(p1, 0), points.get(p1, 1), points.get(p2, 0), points.get(p2, 1));
	}

	public final Int2dArrayList triangles() {
		return trisToPoints;
	}

	public Float2dArrayList points() {
		return points;
	}

	private float fixAngle(float angle) {
		while (angle >= 2 * PI) {
			angle -= 2 * PI;
		}
		while (angle < 0) {
			angle += 2 * PI;
		}
		return angle;
	}

	private float atan2(float x1, float y1, float x2, float y2){
		return fixAngle((float) Math.atan2(y1-y2,x1-x2));
	}

	private float atans2(int p1, int p2){
		return atan2(points.get(p1,0), points.get(p1, 1), points.get(p2, 0), points.get(p2, 1));
	}

	protected boolean sameSide(int p1, int p2, int a, int b) {
		float lineX = x(b) - x(a);
		float lineY = y(b) - y(a);
		float x1 = x(p1) - x(a);
		float y1 = y(p1) - y(a);
		float x2 = x(p2) - x(a);
		float y2 = y(p2) - y(a);

		return get2dCross(lineX, lineY, x1, y1) * get2dCross(lineX, lineY, x2, y2) >= 0;
	}

	protected boolean triangleContains(int tri, int p) {
		return sameSide(p, p1(tri), p2(tri), p3(tri)) && sameSide(p, p2(tri), p3(tri), p1(tri)) && sameSide(p, p3(tri), p1(tri),p2(tri));
	}

	protected int getContainingTriangle(int p) {
		int curTri = parentTri;

		while (triTree.get(curTri)!=null) {
			int lastTri = curTri;
			for (int tri : triTree.get(curTri)) {
				if (triangleContains(tri, p)) {
					curTri = tri;
					break;
				}
			}
			if (lastTri == curTri) {
				throw (new RuntimeException("Couldn't find triangle for point: " + x(p) + " " + y(p)));
			}
		}
		return curTri;
	}

	protected float get2dCross(float x1, float y1, float x2, float y2) {
		return x1 * y2 - y1 * x2;
	}

	protected boolean lineContains(int p1, int p2, int p) {
		float minX = Math.min(x(p1), x(p2));
		float maxX = Math.max(x(p1), x(p2));
		float minY = Math.min(y(p1), y(p2));
		float maxY = Math.max(y(p1), y(p2));
		if (x(p) < minX - ERROR_TOLERANCE || x(p) > maxX + ERROR_TOLERANCE || y(p) < minY - ERROR_TOLERANCE || y(p) < maxY + ERROR_TOLERANCE) {
			return false;
		}

		return (Math.abs(get2dCross(x(p) - x(p1), y(p) - y(p1), x(p2) - x(p1), y(p2) - y(p1))) < ERROR_TOLERANCE);
	}

	protected int addTriangle(int p1, int p2, int p3) {
		int tri = trisToPoints.addRow(p1, p2, p3);
		pointsToTris.get(p1).add(tri);
		pointsToTris.get(p2).add(tri);
		pointsToTris.get(p3).add(tri);
		return tri;
	}

	protected void removeTriangle(int tri) {
		int p1 = p1(tri);
		int p2 = p2(tri);
		int p3 = p3(tri);
		int t1 = pointsToTris.get(p1).binarySearch(tri);
		pointsToTris.get(p1).remove(t1);
		int t2 = pointsToTris.get(p2).binarySearch(tri);
		pointsToTris.get(p2).remove(t2);
		int t3 = pointsToTris.get(p3).binarySearch(tri);
		pointsToTris.get(p3).remove(t3);
	}

	protected TIntArrayList getIncidentTriangles(int p1, int p2) {
		TIntArrayList incTris = new TIntArrayList();
		for (int i=0; i< pointsToTris.get(p1).size(); i++) {
			int tri = pointsToTris.get(p1).get(i);
			if (pointsToTris.get(p2).contains(tri)) {
				incTris.add(tri);
			}
		}
		return incTris;
	}

	private void splitTrisWithPointOnLine(int p1, int p2, int p) {
		TIntArrayList tris = getIncidentTriangles(p1, p2);
		int tri1 = tris.get(0);
		int p3 = -1;
		for (int i=0; i<3; i++) {
			if (trisToPoints.get(tri1, i)!=p1 && trisToPoints.get(tri1, i)!=p2) {
				p3 = trisToPoints.get(tri1, i);
				break;
			}
		}
		int[] children1 = {addTriangle(p, p1, p3), addTriangle(p, p2, p3)};
		triTree.set(tri1, children1);
		removeTriangle(tri1);
		legalizeEdge(p, p1, p3);
		legalizeEdge(p, p2, p3);

		int tri2 = tris.get(0);
		int p4 = -1;
		for (int i=0; i<3; i++) {
			if (trisToPoints.get(tri2, i)!=p1 && trisToPoints.get(tri2, i)!=p2) {
				p4 = trisToPoints.get(tri2, i);
				break;
			}
		}
		int[] children2 = {addTriangle(p, p1, p4), addTriangle(p, p2, p4)};
		triTree.set(tri2, children1);
		removeTriangle(tri2);
		legalizeEdge(p, p1, p4);
		legalizeEdge(p, p2, p4);

	}

	protected float[] calcCenter(int p1, int p2, int p3) {
		float ma = (y(p2) - y(p1)) / (x(p2) - x(p1));
		float mb = (y(p3) - y(p2)) / (x(p3) - x(p2));
		float x = (ma * mb * (y(p1) - y(p3)) + mb * (x(p1) + x(p2)) - ma * (x(p2) + x(p3))) / (2f * (mb - ma));
		float y = -1 / ma * (x - (x(p1) + x(p2)) / 2f) + (y(p1) + y(p2)) / 2f;
		float[] c = {x, y};
		return c;
	}

	protected void legalizeEdge(int p, int p1, int p2) {
		TIntArrayList incTris = getIncidentTriangles(p1, p2);
		//System.err.println(incTris.size());
		if (incTris.size() <= 1) {
			return;
		}
		int otherPt = -1;
		for (int i=0; i<incTris.size(); i++) {
			int tri = incTris.get(i);
			for (int j = 0; j<3; j++) {
				int pt = trisToPoints.get(tri,j);
				if (!(pt == p || pt == p1 || pt == p2)) {
					otherPt = pt;
					break;
				}
			}
		}
		if (otherPt == -1) {
			return;
		}
		float[] c = calcCenter(p, p1, p2);
		float r = dist(c[0], c[1], x(p), y(p));
		if (dist(c[0], c[1], x(otherPt), y(otherPt)) < r) {
			//System.err.println("Swapping");
			int newTri1 = addTriangle(p, p1, otherPt);
			int newTri2 = addTriangle(p, otherPt, p2);
			int[] children = {newTri1, newTri2};
			triTree.set(incTris.get(0), children);
			triTree.set(incTris.get(1), children);
			removeTriangle(incTris.get(1));
			removeTriangle(incTris.get(0));
			legalizeEdge(p, p1, otherPt);
			legalizeEdge(p, otherPt, p2);
		}
	}

	protected void addPoint(int p) {
		//System.err.println("Adding point");
//		System.err.println("Adding point: "+toString(p));
		int tri = getContainingTriangle(p);
//		if (tri == null) {
//			System.err.println("Couldn't find a triangle containing point: " + p[0] + ", " + p[1]);
//			System.err.println("Current triangles are:");
//			for (float[][] t : triangles) {
//				System.err.println("(" + t[0][0] + ", " + t[0][1] + ") " +
//						"(" + t[1][0] + ", " + t[1][1] + ") " +
//						"(" + t[2][0] + ", " + t[2][1] + ")");
//			}
//		}

		if (lineContains(p1(tri), p2(tri), p)) {
			splitTrisWithPointOnLine(p1(tri), p2(tri), p);
		} else if (lineContains(p2(tri), p3(tri), p)) {
			splitTrisWithPointOnLine(p2(tri), p3(tri), p);
		} else if (lineContains(p3(tri), p1(tri), p)) {
			splitTrisWithPointOnLine(p3(tri), p1(tri), p);
		} else {
			int[] children = {addTriangle(p, p1(tri), p2(tri)),
				addTriangle(p, p2(tri), p3(tri)),
				addTriangle(p, p3(tri), p1(tri))};
			triTree.set(tri, children);
			removeTriangle(tri);
			legalizeEdge(p, p1(tri), p2(tri));
			legalizeEdge(p, p2(tri), p3(tri));
			legalizeEdge(p, p3(tri), p1(tri));
		}
	}

	protected void removePoint(int p) {
		TIntArrayList tris = new TIntArrayList(pointsToTris.get(p).toNativeArray());
		for (int i=0; i<tris.size(); i++) {
			int tri = tris.get(i);
			removeTriangle(tri);
		}
	}
}
