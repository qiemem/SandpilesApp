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

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import gnu.trove.TIntArrayList;
import java.awt.Dimension;

/**
 *
 * @author headb
 */
public class Sandpile3dDrawer implements SandpileDrawer, GLEventListener {

	private GLCanvas canvas;
	private ColorMode colorMode;
	private float[] lightAmbient = {0.5f, 0.5f, 0.5f, 1.0f};
	private float[] lightDiffuse = {0.2f, 0.2f, 0.2f, 1.0f};
	private float[] lightPosition = {5.0f, 0.0f, 0.0f, 1.0f};
	private DelaunayTriangulation tris;
	private SandpileConfiguration config;
	private TIntArrayList firings;
	private SandpileGraph graph;
	//private HashMap<float[], Integer> pointsToVerts;
	private Float2dArrayList colors;
	private Float2dArrayList inDebtColors;
	private int heightSmoothing = 3;
	private int colorSmoothing = 3;
	private float heightMultiplier = 3f;
	private boolean drawShape = true;
	private boolean drawWire = false;
	private boolean autoRotate = false;
	private float[] lastPoint;
	private float xRot = 0f, yRot = 0f;
	private float startingZ = 240f;
	private float cameraX = 0f, cameraY = 0f, cameraZ = startingZ;
	private float[] rotAxis = new float[3];
	private float rotAngle = 0f;
	private final float ROT_SCALE = 90f;
	private float[] rotMatrix = new float[16];
	private float[] backgroundColor = {0f, 0f, 0f};

	public Sandpile3dDrawer(GLCanvas canvas) {
		colorMode = ColorMode.NUM_OF_GRAINS;
		this.canvas = canvas;
		this.canvas.addGLEventListener(this);
		try{
			tris = new DelaunayTriangulation(new Float2dArrayList(0, 2));
		}catch(InterruptedException e){
			
		}
		config = new SandpileConfiguration();
		firings = new TIntArrayList();
		graph = new SandpileGraph();
		//pointsToVerts = new HashMap<float[], Integer>();
		final Sandpile3dDrawer me = this;
		canvas.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent evt) {
				me.mousePressed(evt);
			}
		});
		canvas.addMouseMotionListener(new MouseMotionAdapter() {

			@Override
			public void mouseDragged(MouseEvent evt) {
				me.mouseDragged(evt);
			}
		});
		canvas.addMouseWheelListener(new MouseWheelListener(){
			public void mouseWheelMoved(MouseWheelEvent evt){
				me.mouseWheelMoved(evt);
			}
		});
		canvas.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent evt) {
				me.keyPressed(evt);
			}
		});
	}

	public void mouseWheelMoved(MouseWheelEvent evt){
		float amount = 1f - 0.01f * evt.getUnitsToScroll();
		setZoom(getZoom() * amount);
		canvas.repaint();
	}

	public void mousePressed(MouseEvent evt) {
		lastPoint = trackBallPointMapping(evt.getX(), evt.getY());
	}

	public void mouseDragged(MouseEvent evt) {
		float[] curPoint = trackBallPointMapping(evt.getX(), evt.getY());
		float[] dir = new float[3];
		dir[0] = curPoint[0]-lastPoint[0];
		dir[1] = curPoint[1]-lastPoint[1];
		dir[2] = curPoint[2]-lastPoint[2];
		float velocity = (float) Math.sqrt(dir[0]*dir[0] + dir[1]*dir[1] + dir[2]*dir[2]);
		if(velocity<0.0001f){
			rotAngle = 0f;
			return;
		}
		rotAxis[0] = lastPoint[1]*curPoint[2] - lastPoint[2]*curPoint[1];
		rotAxis[1] = lastPoint[2]*curPoint[0] - lastPoint[0]*curPoint[2];
		rotAxis[2] = lastPoint[0]*curPoint[1] - lastPoint[1]*curPoint[0];
		rotAngle = velocity*ROT_SCALE;
		lastPoint = curPoint;
		canvas.repaint();
	}

	private float[] trackBallPointMapping(int x, int y){
		float[] v = new float[3];
		Dimension dim = canvas.getSize();
		v[0] = (2f*x - (float)dim.getWidth())/(float)dim.getWidth();
		v[1] = ((float)dim.getHeight() - 2f*y)/(float)dim.getHeight();
		float d = (float)Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
		d = d<1f ? d : 1f;
		v[2] = (float)Math.sqrt(1.001f - d*d);
		d = (float)Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
		v[0]/=d;
		v[1]/=d;
		v[2]/=d;
		return v;
	}

	public void setCamera(float x, float y) {
		cameraX = x;
		cameraY = y;
	}

	public float getCameraX(){
		return cameraX;
	}

	public float getCameraY(){
		return cameraY;
	}

	public float getZoom(){
		return startingZ/cameraZ;
	}

	public void setZoom(float amount) {
		cameraZ = startingZ / amount;
	}

	public void setDrawShape(boolean val){
		drawShape = val;
	}

	public void setDrawWire(boolean val){
		drawWire = val;
	}

	public void keyPressed(KeyEvent evt) {
		switch (evt.getKeyCode()) {
			case KeyEvent.VK_UP:
				cameraY += 1f;
				break;
			case KeyEvent.VK_DOWN:
				cameraY -= 1f;
				break;
			case KeyEvent.VK_LEFT:
				cameraX -= 1f;
				break;
			case KeyEvent.VK_RIGHT:
				cameraX += 1f;
				break;
		}
		canvas.repaint();
	}

	public void setHeightSmoothing(int n) {
		heightSmoothing = n;
	}

	public void setColorSmoothing(int n) {
		colorSmoothing = n;
	}

	public void setHeightScalar(float s) {
		heightMultiplier = s;
	}

	public GLCanvas getCanvas() {
		return canvas;
	}

	public void setColorMode(ColorMode cm) {
		colorMode = cm;
	}

	public void setColors(Float2dArrayList colors, Float2dArrayList inDebtColors, float[] backgroundColor) {
		this.colors = new Float2dArrayList(colors);
		this.inDebtColors = new Float2dArrayList(inDebtColors);
		this.backgroundColor = backgroundColor;
	}

	public ColorMode getColorMode() {
		return colorMode;
	}

	public void triangulate(Float2dArrayList vertexLocations) throws InterruptedException{
		if(vertexLocations.equals(tris.points()))
			return;
		tris = new DelaunayTriangulation(vertexLocations);
	}

	public void paintSandpileGraph(SandpileGraph graph, Float2dArrayList vertexLocations, SandpileConfiguration config, TIntArrayList firings, TIntArrayList selectedVertices) {
//		ArrayList<Float> heights = new ArrayList<Float>();
//		for(int v : config){
//			heights.add((float)v);
//		}
		this.config = config;
		this.firings = firings;
		this.graph = graph;
		//tris.assignHeights(heights);
		canvas.repaint();
	}

	public void setSelectionBox(float maxX, float maxY, float minX, float minY) {
	}

	public void clearSelectionBox() {
	}

	public float[] transformCanvasCoords(int x, int y) {
		throw (new UnsupportedOperationException());
	}

	public float[] normalizedCross(float x1, float y1, float z1, float x2, float y2, float z2) {
		float x = y1 * z2 - z1 * y2;
		float y = z1 * x2 - x1 * z2;
		float z = x1 * y2 - y1 * x2;
		float l = (float) Math.sqrt(x * x + y * y + z * z);
		x /= l;
		y /= l;
		z /= l;
		float[] normal = {x,y,z};
		return normal;
	}

	public void drawTriangulation(GL gl, DelaunayTriangulation tris) {
		//System.err.println("Drawing tris");
		float[] h = calcHeights();
		for (int i = 0; i < heightSmoothing; i++) {
			h = smoothHeights(h);
		}
		if (drawShape) {
			float[][] c = calcColors();
			for (int i = 0; i < colorSmoothing; i++) {
				c = smoothColors(c);
			}

			gl.glBegin(gl.GL_TRIANGLES);
			for (int i = 0; i<tris.triangles().rows(); i++) {
				int v0 = tris.triangles().get(i, 0);
				float x0 = tris.points().get(v0,0)-cameraX;
				float y0 = tris.points().get(v0,1)-cameraY;
				int v1 = tris.triangles().get(i, 1);
				float x1 = tris.points().get(v1,0)-cameraX;
				float y1 = tris.points().get(v1,1)-cameraY;
				int v2 = tris.triangles().get(i, 2);
				float x2 = tris.points().get(v2,0)-cameraX;
				float y2 = tris.points().get(v2,1)-cameraY;
				float[] n = normalizedCross(x1 - x0, y1 - y0, h[v1] - h[v0],
						x2 - x0, y2 - y0, h[v2] - h[v0]);
				//System.err.println(c[v0][0]+" "+c[v0][1]+" "+c[v0][2]);
				gl.glNormal3fv(n, 0);
				gl.glColor3fv(c[v0],0);
				gl.glVertex3f(x0, y0, h[v0]);
				gl.glColor3fv(c[v1],0);
				gl.glVertex3f(x1, y1, h[v1]);
				gl.glColor3fv(c[v2],0);
				gl.glVertex3f(x2, y2, h[v2]);
				//gl.glVertex3f(tri[0][0], tri[0][1], tri[0][2]);
				
			}
			gl.glEnd();
		}

		if (drawWire) {
			gl.glColor3f(0f, 1f, 0f);
			for (int i=0;i<tris.triangles().rows();i++) {
				int v0 = tris.triangles().get(i, 0);
				float x0 = tris.points().get(v0,0)-cameraX;
				float y0 = tris.points().get(v0,1)-cameraY;
				int v1 = tris.triangles().get(i, 1);
				float x1 = tris.points().get(v1,0)-cameraX;
				float y1 = tris.points().get(v1,1)-cameraY;
				int v2 = tris.triangles().get(i, 2);
				float x2 = tris.points().get(v2,0)-cameraX;
				float y2 = tris.points().get(v2,1)-cameraY;
				gl.glBegin(gl.GL_LINES);
				gl.glVertex3f(x0, y0, h[v0]);
				gl.glVertex3f(x1, y1, h[v1]);
				gl.glVertex3f(x1, y1, h[v1]);
				gl.glVertex3f(x2, y2, h[v2]);
				gl.glVertex3f(x2, y2, h[v2]);
				gl.glVertex3f(x0, y0, h[v0]);
				gl.glEnd();
			}
		}
	}

	public void init(GLAutoDrawable drawable) {
		// Use debug pipeline
		// drawable.setGL(new DebugGL(drawable.getGL()));

		GL gl = drawable.getGL();
		System.err.println("INIT GL IS: " + gl.getClass().getName());

		// Enable VSync
		gl.setSwapInterval(1);

		// Setup the drawing area and shading mode
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glShadeModel(GL.GL_SMOOTH); // try setting this to GL_FLAT and see what happens.

		gl.glEnable(gl.GL_DEPTH_TEST);
		gl.glDepthFunc(gl.GL_LEQUAL);

		gl.glLightfv(gl.GL_LIGHT0, gl.GL_AMBIENT, lightAmbient, 0);
		gl.glLightfv(gl.GL_LIGHT0, gl.GL_DIFFUSE, lightDiffuse, 0);
		gl.glLightfv(gl.GL_LIGHT0, gl.GL_POSITION, lightPosition, 0);
		gl.glEnable(gl.GL_LIGHT0);
		gl.glEnable(gl.GL_LIGHTING);

		gl.glColorMaterial(gl.GL_FRONT_AND_BACK, gl.GL_DIFFUSE);
		gl.glColorMaterial(gl.GL_FRONT_AND_BACK, gl.GL_AMBIENT);
		gl.glEnable(gl.GL_COLOR_MATERIAL);

		gl.glGetFloatv(gl.GL_MODELVIEW_MATRIX, rotMatrix,0);
	}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		GL gl = drawable.getGL();
		GLU glu = new GLU();

		if (height <= 0) { // avoid a divide by zero error!

			height = 1;
		}
		final float h = (float) width / (float) height;
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(45.0f, h, 1.0, 100000.0);
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	public void display(GLAutoDrawable drawable) {
		GL gl = drawable.getGL();
		gl.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], 0.0f);

		// Clear the drawing area
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		// Reset the current matrix to the "identity"
		gl.glLoadIdentity();
		gl.glRotatef(rotAngle, rotAxis[0], rotAxis[1], rotAxis[2]);
		gl.glMultMatrixf(rotMatrix, 0);
		gl.glGetFloatv(gl.GL_MODELVIEW_MATRIX,rotMatrix, 0);
		gl.glLoadIdentity();
		gl.glTranslatef(0f, 0f, -cameraZ);
		//gl.glTranslatef(-cameraX, -cameraY, 0);
		gl.glMultMatrixf(rotMatrix, 0);
		gl.glPushMatrix();
		drawTriangulation(gl, tris);
		gl.glPopMatrix();

		// Flush all drawing operations to the graphics card
		gl.glFlush();

		if(!autoRotate)
			rotAngle = 0f;
	}

	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
	}

	private float[] getColorForVertex(int vert) {
		int color = 0;
		boolean inDebt=false;
		switch (colorMode) {
			case NUM_OF_GRAINS:
				int sand = Math.max(config.get(vert), -1);
				if(sand<0){
					color = Math.min(-sand-1, inDebtColors.rows()-1);
					inDebt = true;
				}else
					color = Math.min(sand, colors.rows()-1);
				break;
			case STABILITY:
				if (config.get(vert) < graph.degree(vert)) {
					color = 0;
				} else {
					color = colors.rows()-1;
				}
				break;
			case FIRINGS:
				color = Math.min(firings.get(vert),colors.rows()-1);
				break;
		}
		if(inDebt){
			float[] theColor = {inDebtColors.get(color, 0), inDebtColors.get(color,1), inDebtColors.get(color,2)};
			return theColor;
		}else{
			float[] theColor = {colors.get(color, 0), colors.get(color,1), colors.get(color,2)};
			return theColor;
		}
	}

	private float getHeightForVertex(int vert) {
		if (graph.isSink(vert)) {
			return -2f*heightMultiplier;
		} else {
			return heightMultiplier * ((float) config.get(vert) - 2f);
		}
	}

	private float[] calcHeights() {
		float[] heights = new float[config.size()];
		for (int v = 0; v < config.size(); v++) {
			heights[v] = getHeightForVertex(v);
		}
		return heights;
	}

	private float[] smoothHeights(float[] heights) {
		float[] newHeights = new float[config.size()];

		for (int v = 0; v < config.size(); v++) {
			float avg = heights[v];
			EdgeList outEdges = graph.getOutgoingEdges(v);
			for (int i=0; i<outEdges.size(); i++) {
				avg += heights[outEdges.destQuick(i)]*outEdges.wtQuick(i);
			}
			avg /= (float) (graph.degree(v) + 1);
			//avg*=1.5f;
			newHeights[v] = avg;
		}
		return newHeights;
	}

	private float[][] calcColors() {
		float[][] clrs = new float[config.size()][3];
		for (int v = 0; v < config.size(); v++) {
			clrs[v] = getColorForVertex(v);
		}
		return clrs;
	}

	private float[][] smoothColors(float[][] clrs) {
		float[][] newClrs = new float[config.size()][3];
		for (int v = 0; v < config.size(); v++) {
			float[] avg = new float[3];
			avg[0] = clrs[v][0];
			avg[1] = clrs[v][1];
			avg[2] = clrs[v][2];
			EdgeList outEdges = graph.getOutgoingEdges(v);
			for (int i=0; i<outEdges.size(); i++) {
				int vert = outEdges.destQuick(i);
				int wt = outEdges.wtQuick(i);
				avg[0] += clrs[vert][0]*wt;
				avg[1] += clrs[vert][1]*wt;
				avg[2] += clrs[vert][2]*wt;
			}
			avg[0] /= (float) (graph.degree(v) + 1);
			avg[1] /= (float) (graph.degree(v) + 1);
			avg[2] /= (float) (graph.degree(v) + 1);
			//avg*=1.5f;
			newClrs[v] = avg;
		}
		return newClrs;
	}
}
