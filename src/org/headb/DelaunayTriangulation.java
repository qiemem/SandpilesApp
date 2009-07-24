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

/**
 *
 * @author headb
 */
public class DelaunayTriangulation {
	//private ArrayList<float[]> points;

	private Collection<float[][]> triangles;
	private float[][] parentTri;
	private HashMap<float[], ArrayList<float[][]>> pointsToTris;
	private final float PI = (float) Math.PI;
	//private Iterator<float[]> ptsToAdd;
	private HashMap<float[][], float[][][]> triTree;
	private final float ERROR_TOLERANCE = 0.00001f;

	private ArrayList<float[]> points;

	public DelaunayTriangulation(List<float[]> points) {
		//System.err.println("init");
		//this.points = new ArrayList<float[]>(pts);
		triangles = new ArrayList<float[][]>();
		pointsToTris = new HashMap<float[], ArrayList<float[][]>>();
		triTree = new HashMap<float[][], float[][][]>();
		if(points.isEmpty())
			return;

		float[] trPt = points.get(0);

		for (float[] p : points) {
			if (p[1] > trPt[1]) {
				trPt = p;
			} else if (p[1] == trPt[1] && p[0] > trPt[0]) {
				trPt = p;
			}
		}
		float[] startPt = {trPt[0]+1f, trPt[1]+1f};
		//Calculate the bounding triangle with top right at startPt.
		float largestAngle = PI;
		float smallestAngle = 2 * PI;
		float largestDist = 0f;
		for (float[] p : points) {
			//if(p==startPt)
			//	continue;
			largestAngle = Math.max(atan2(p, startPt), largestAngle);
			smallestAngle = Math.min(atan2(p, startPt), smallestAngle);
			largestDist = Math.max(largestDist, dist(startPt, p));
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
		float[] tl = {startPt[0] + length * (float) Math.cos(tlAngle), startPt[1] + length * (float) Math.sin(tlAngle), 0f};
		float[] br = {startPt[0] + length * (float) (Math.cos(brAngle)),
			startPt[1] + length * (float) (Math.sin(brAngle)), 0f};

		parentTri = addTriangle(startPt, tl, br);
		for (float[] p : points) {
			try {
				addPoint(p);
			} catch (RuntimeException e) {
				System.err.println("WARNING: Couldn't add a point. Throwing it out.");
				System.err.println("Point: " + toString(p));
				System.err.println("Largest angle: " + largestAngle);
				System.err.println("Smallest angle: " + smallestAngle);
				System.err.println("startPt: " + toString(startPt));
				System.err.println("brAngle: " + brAngle);
				System.err.println("tlAngle: " + tlAngle);
				System.err.println("length: " + length);
			}
		}
		removePoint(tl);
		removePoint(br);
		removePoint(startPt);
		triangles = new ArrayList<float[][]>(triangles);
		triTree = null;
	}
//
//	public boolean addNext(){
//		if(ptsToAdd.hasNext()){
//			addPoint(ptsToAdd.next());
//			return true;
//		}
//		return false;
//	}

	private String toString(float[] p) {
		return "(" + p[0] + ", " + p[1] + ")";
	}

//	public void assignHeights(List<Float> heights){
//		int i = 0;
//		for(float h : heights){
//			points.get(i)[2]=h;
//			i++;
//		}
//	}

	private float dist(float[] p1, float[] p2) {
		float d = (float) Math.sqrt((p1[0] - p2[0]) * (p1[0] - p2[0]) + (p1[1] - p2[1]) * (p1[1] - p2[1]));
		return d;
	}

	private boolean equals(float[] p1, float[] p2) {
		return p1[0] == p2[0] && p1[1] == p2[1];
	}

	public final Collection<float[][]> triangles() {
		return triangles;
	}

	public List<float[]> points() {
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

	private float atan2(float[] p1, float[] p2) {
		float[] newP = {p1[0] - p2[0], p1[1] - p2[1]};
		return atan2(newP);
	}

	private float atan2(float[] p) {
		return fixAngle((float) Math.atan2(p[1], p[0]));
	}

	protected boolean sameSide(float[] p1, float[] p2, float[] a, float[] b) {
		float[] lineVec = {b[0] - a[0], b[1] - a[1]};
		float[] p1Vec = {p1[0] - a[0], p1[1] - a[1]};
		float[] p2Vec = {p2[0] - a[0], p2[1] - a[1]};

		return get2dCross(lineVec, p1Vec) * get2dCross(lineVec, p2Vec) >= 0;
	}

	protected boolean triangleContains(float[][] tri, float[] p) {
		return sameSide(p, tri[0], tri[1], tri[2]) && sameSide(p, tri[1], tri[2], tri[0]) && sameSide(p, tri[2], tri[0],tri[1]);
	}

	protected float[][] getContainingTriangle(float[] p) {
		float[][] curTri = parentTri;

		while (triTree.containsKey(curTri)) {
			float[][] lastTri = curTri;
			for (float[][] tri : triTree.get(curTri)) {
				if (triangleContains(tri, p)) {
					curTri = tri;
					break;
				}
			}
			if (lastTri == curTri) {
				throw (new RuntimeException("Couldn't find triangle for point: " + toString(p)));
			}
		}
		return curTri;
	}

	protected float get2dCross(float[] vec1, float[] vec2) {
		return vec1[0] * vec2[1] - vec1[1] * vec2[0];
	}

	protected boolean lineContains(float[] p1, float[] p2, float[] p) {
		float minX = Math.min(p1[0], p2[0]);
		float maxX = Math.max(p1[0], p2[0]);
		float minY = Math.min(p1[1], p2[1]);
		float maxY = Math.max(p1[1], p2[1]);
		if (p[0] < minX - ERROR_TOLERANCE || p[0] > maxX + ERROR_TOLERANCE || p[1] < minY - ERROR_TOLERANCE || p[1] < maxY + ERROR_TOLERANCE) {
			return false;
		}
		float[] vec1 = {p[0] - p1[0], p[1] - p1[1]};
		float[] vec2 = {p2[0] - p1[0], p2[1] - p1[1]};

		return (Math.abs(get2dCross(vec1, vec2)) < ERROR_TOLERANCE);
	}

	protected float[][] addTriangle(float[] p1, float[] p2, float[] p3) {
		float[][] tri = {p1, p2, p3};
		triangles.add(tri);
		if (!pointsToTris.containsKey(p1)) {
			pointsToTris.put(p1, new ArrayList<float[][]>());
		}
		if (!pointsToTris.containsKey(p2)) {
			pointsToTris.put(p2, new ArrayList<float[][]>());
		}
		if (!pointsToTris.containsKey(p3)) {
			pointsToTris.put(p3, new ArrayList<float[][]>());
		}
		pointsToTris.get(p1).add(tri);
		pointsToTris.get(p2).add(tri);
		pointsToTris.get(p3).add(tri);
		//triTree.put(tri, new ArrayList<float[][]>());
		return tri;
	}

	protected boolean removeTriangle(float[][] tri) {
		if (triangles.remove(tri)) {
			pointsToTris.get(tri[0]).remove(tri);
			if (pointsToTris.get(tri[0]).isEmpty()) {
				pointsToTris.remove(tri[0]);
			}
			pointsToTris.get(tri[1]).remove(tri);
			if (pointsToTris.get(tri[1]).isEmpty()) {
				pointsToTris.remove(tri[1]);
			}
			pointsToTris.get(tri[2]).remove(tri);
			if (pointsToTris.get(tri[2]).isEmpty()) {
				pointsToTris.remove(tri[2]);
			}
			return true;
		} else {
			return false;
		}
	}

	protected ArrayList<float[][]> getIncidentTriangles(float[] p1, float p2[]) {
		ArrayList<float[][]> incTris = new ArrayList<float[][]>();
		for (float[][] tri : pointsToTris.get(p1)) {
			if (pointsToTris.get(p2).contains(tri)) {
				incTris.add(tri);
			}
		}
		return incTris;
	}

	private void splitTrisWithPointOnLine(float[] p1, float[] p2, float[] p) {
		ArrayList<float[][]> tris = getIncidentTriangles(p1, p2);
		float[][] tri1 = tris.get(0);
		float[] p3 = null;
		for (float[] pt : tri1) {
			if (!Arrays.equals(pt, p1) && !Arrays.equals(pt, p2)) {
				p3 = pt;
				break;
			}
		}
		float[][][] children1 = {addTriangle(p, p1, p3), addTriangle(p, p2, p3)};
		triTree.put(tri1, children1);
		removeTriangle(tri1);
		legalizeEdge(p, p1, p3);
		legalizeEdge(p, p2, p3);

		float[][] tri2 = tris.get(1);
		float[] p4 = null;
		for (float[] pt : tri2) {
			if (!Arrays.equals(pt, p1) && !Arrays.equals(pt, p2)) {
				p4 = pt;
				break;
			}
		}
		float[][][] children2 = {addTriangle(p, p1, p4), addTriangle(p, p2, p4)};
		triTree.put(tri2, children2);
		removeTriangle(tri2);
		legalizeEdge(p, p1, p4);
		legalizeEdge(p, p2, p4);

	}

	protected float[] calcCenter(float[] p1, float[] p2, float[] p3) {
		float ma = (p2[1] - p1[1]) / (p2[0] - p1[0]);
		float mb = (p3[1] - p2[1]) / (p3[0] - p2[0]);
		float x = (ma * mb * (p1[1] - p3[1]) + mb * (p1[0] + p2[0]) - ma * (p2[0] + p3[0])) / (2f * (mb - ma));
		float y = -1 / ma * (x - (p1[0] + p2[0]) / 2f) + (p1[1] + p2[1]) / 2f;
		float[] c = {x, y};
		return c;
	}

	protected void legalizeEdge(float[] p, float[] p1, float[] p2) {
		ArrayList<float[][]> incTris = getIncidentTriangles(p1, p2);
		//System.err.println(incTris.size());
		if (incTris.size() <= 1) {
			return;
		}
		float[] otherPt = null;
		for (float[][] tri : incTris) {
			for (float[] pt : tri) {
				if (!(equals(pt, p) || equals(pt, p1) || equals(pt, p2))) {
					otherPt = pt;
					break;
				}
			}
		}
		if (otherPt == null) {
			return;
		}
		float[] c = calcCenter(p, p1, p2);
		float r = dist(c, p);
		if (dist(c, otherPt) < r) {
			//System.err.println("Swapping");
			float[][] newTri1 = addTriangle(p, p1, otherPt);
			float[][] newTri2 = addTriangle(p, otherPt, p2);
			float[][][] children = {newTri1, newTri2};
			triTree.put(incTris.get(0), children);
			triTree.put(incTris.get(1), children);
			removeTriangle(incTris.get(1));
			removeTriangle(incTris.get(0));
			legalizeEdge(p, p1, otherPt);
			legalizeEdge(p, otherPt, p2);
		}
	}

	protected void addPoint(float[] p) {
		//System.err.println("Adding point");
//		System.err.println("Adding point: "+toString(p));
		float[][] tri = getContainingTriangle(p);
//		if (tri == null) {
//			System.err.println("Couldn't find a triangle containing point: " + p[0] + ", " + p[1]);
//			System.err.println("Current triangles are:");
//			for (float[][] t : triangles) {
//				System.err.println("(" + t[0][0] + ", " + t[0][1] + ") " +
//						"(" + t[1][0] + ", " + t[1][1] + ") " +
//						"(" + t[2][0] + ", " + t[2][1] + ")");
//			}
//		}

		if (lineContains(tri[0], tri[1], p)) {
			splitTrisWithPointOnLine(tri[0], tri[1], p);
		} else if (lineContains(tri[1], tri[2], p)) {
			splitTrisWithPointOnLine(tri[1], tri[2], p);
		} else if (lineContains(tri[2], tri[0], p)) {
			splitTrisWithPointOnLine(tri[2], tri[0], p);
		} else {
			float[][][] children = {addTriangle(p, tri[0], tri[1]),
				addTriangle(p, tri[1], tri[2]),
				addTriangle(p, tri[2], tri[0])};
			triTree.put(tri, children);
			removeTriangle(tri);
			legalizeEdge(p, tri[0], tri[1]);
			legalizeEdge(p, tri[1], tri[2]);
			legalizeEdge(p, tri[2], tri[0]);
		}
	}

	protected void removePoint(float[] p) {
		ArrayList<float[][]> tris = new ArrayList<float[][]>(pointsToTris.get(p));
		for (float[][] tri : tris) {
			removeTriangle(tri);
		}
	}
}
