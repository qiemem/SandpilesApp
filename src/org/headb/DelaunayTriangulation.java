/*
Copyright (c) 2008-2009 Bryan Head
All Rights Reserved

[This software is released under the "MIT License"]

Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the
Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall
be included in all copies or substantial portions of the
Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package org.headb;

import java.util.ArrayList;

import gnu.trove.TIntArrayList;

/**
 * This class, when given a list of 2 dimensional points, will calculate their
 * delauney triangulation. The only public methods are the constructor, a get
 * method for the points, and a get method for the resulting triangles.
 * @author Bryan Head
 */
public class DelaunayTriangulation {
	//private Float2dArrayList points;

	private Int2dArrayList triangles;
	private int parentTri;
	private ArrayList<TIntArrayList> pointsToTris;
	private Int2dArrayList trisToPoints;
	private final float PI = (float) Math.PI;
	private ArrayList<int[]> triTree;
	private final float ERROR_TOLERANCE = 0.00001f;

	private Float2dArrayList points;

	public DelaunayTriangulation(Float2dArrayList points) throws InterruptedException{
		trisToPoints = new Int2dArrayList(0, 3);
		pointsToTris = new ArrayList<TIntArrayList>();
		triTree = new ArrayList<int[]>();
		triangles = new Int2dArrayList(0,3);
		this.points = new Float2dArrayList(points);
		if(this.points.isEmpty())
			return;


		// Get the bounding box
		float maxX = this.points.get(0, 0), minX = this.points.get(0, 0);
		float maxY = this.points.get(0, 1), minY = this.points.get(0, 1);
		for(int i=1; i<this.points.rows(); i++){
			float x = points.get(i, 0);
			float y = points.get(i, 1);
			if(x>maxX)
				maxX = x;
			else if(x<minX)
				minX = x;
			if(y>maxY)
				maxY = y;
			else if(y<minY)
				minY = y;
		}

		//get a bounding triangle:
		float trX = (maxX - minX)/2f;
		float trY = maxY+(maxY-minY);
		float blX = minX - 8f*(maxX - minX);
		float blY = minY - (maxY-minY);
		float brX = maxX + 8f*(maxX - minX);
		float brY = minY - (maxY-minY);

		int tr = this.points.rows();
		int l = tr+1;
		int b = l+1;
		this.points.addRow(trX, trY);
		this.points.addRow(blX, blY);
		this.points.addRow(brX, brY);

		for(int i=0; i<this.points.rows(); i++){
			pointsToTris.add(new TIntArrayList());
		}

		parentTri = addTriangle(tr, l, b);
		for (int p = 0; p < this.points.rows()-3; p++) {
			try {
			addPoint(p);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
		}
		removePoint(b);
		removePoint(l);
		removePoint(tr);
		triangles = new Int2dArrayList(0, 3);
		for (int t = 0; t < trisToPoints.rows(); t++) {
			boolean ok = triTree.get(t)==null;
			for(int c = 0; c<3; c++){
				if (trisToPoints.get(t, c)==l)
					ok = false;
				else if (trisToPoints.get(t, c)==b)
					ok = false;
				else if(trisToPoints.get(t, c)==tr)
					ok = false;
			}
			if(ok){
				triangles.addRow(p1(t), p2(t), p3(t));
			}
		}
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
		return triangles;
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
				//throw (new RuntimeException("Couldn't find triangle for point: " + x(p) + " " + y(p)));
				for(int i=0; i<trisToPoints.rows(); i++){
					if(triTree.get(i)==null && triangleContains(i, p)){
						return i;
					}
				}
				throw (new RuntimeException("Couldn't find triangle for point: " + x(p) + " " + y(p)));
			}
		}
		return curTri;
//		for(int tri = 0; tri<trisToPoints.rows(); tri++){
//			if(triangleContains(tri, p) && triTree.get(tri)==null){
//				return tri;
//			}
//		}
//		throw(new RuntimeException());
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
		triTree.add(null);
		return tri;
	}

	protected void removeTriangle(int tri) {
		int p1 = p1(tri);
		int p2 = p2(tri);
		int p3 = p3(tri);
		int t1 = pointsToTris.get(p1).indexOf(tri);
		if(t1>=0)
			pointsToTris.get(p1).remove(t1);
		int t2 = pointsToTris.get(p2).indexOf(tri);
		if(t2>=0)
			pointsToTris.get(p2).remove(t2);
		int t3 = pointsToTris.get(p3).indexOf(tri);
		if(t3>=0)
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
		//tri1 and tri2 form the qudralateral with diagonal p1-p2 containing new
		//point p.
		//Split quad into four triangles.

		TIntArrayList tris = getIncidentTriangles(p1, p2);
		int tri1 = tris.get(0);

		//find point opposite to edge p1-p2 for tri1.
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
		int tri2 = tris.get(0);

		//find point opposite to edge p1-p2 for tri2.
		int p4 = -1;
		for (int i=0; i<3; i++) {
			if (trisToPoints.get(tri2, i)!=p1 && trisToPoints.get(tri2, i)!=p2) {
				p4 = trisToPoints.get(tri2, i);
				break;
			}
		}
		int[] children2 = {addTriangle(p, p1, p4), addTriangle(p, p2, p4)};
		triTree.set(tri2, children2);
		removeTriangle(tri2);
		
		legalizeEdge(p, p1, p3, 0);
		legalizeEdge(p, p2, p3, 0);
		legalizeEdge(p, p1, p4, 0);
		legalizeEdge(p, p2, p4, 0);

	}

	protected float[] calcCenter(int p1, int p2, int p3) {
		float ma = (y(p2) - y(p1)) / (x(p2) - x(p1));
		float mb = (y(p3) - y(p2)) / (x(p3) - x(p2));
		float x = (ma * mb * (y(p1) - y(p3)) + mb * (x(p1) + x(p2)) - ma * (x(p2) + x(p3))) / (2f * (mb - ma));
		float y = -1 / ma * (x - (x(p1) + x(p2)) / 2f) + (y(p1) + y(p2)) / 2f;
		float[] c = {x, y};
		return c;
	}

	protected void legalizeEdge(int p, int p1, int p2, int n) {
		//if(n>40)
			//System.err.println(n);
		TIntArrayList incTris = getIncidentTriangles(p1, p2);
		//System.err.println(incTris.size());
		if (incTris.size() <= 1) {
			return;
		}

		// Find the point opposite to p across p1-p2
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
			legalizeEdge(p, p1, otherPt, n+1);
			legalizeEdge(p, otherPt, p2, n+1);
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
			legalizeEdge(p, p1(tri), p2(tri), 0);
			legalizeEdge(p, p2(tri), p3(tri), 0);
			legalizeEdge(p, p3(tri), p1(tri), 0);
		}
	}

	protected void removePoint(int p) {
		TIntArrayList tris = new TIntArrayList(pointsToTris.get(p).toNativeArray());
		tris.sort();
		tris.reverse();
		for (int i=0; i<tris.size(); i++) {
			int tri = tris.get(i);
			removeTriangle(tri);
		}
		points.removeRow(p);
	}
}
