/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.headb;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

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
	private List<Integer> firings;
	private SandpileGraph graph;
	private HashMap<float[], Integer> pointsToVerts;
	private ArrayList<float[]> colors;
	private ArrayList<float[]> inDebtColors;
	private int heightSmoothing = 3;
	private int colorSmoothing = 3;
	private float heightMultiplier = 3f;
	private boolean drawShape = true;
	private boolean drawWire = false;
	private int mouseX, mouseY;
	private float xRot = 0f, yRot = 0f;
	private float startingZ = 200f;
	private float cameraX = 0f, cameraY = 0f, cameraZ = startingZ;

	public Sandpile3dDrawer(GLCanvas canvas) {
		colorMode = ColorMode.NUM_OF_GRAINS;
		this.canvas = canvas;
		this.canvas.addGLEventListener(this);
		tris = new DelaunayTriangulation(new ArrayList<float[]>());
		config = new SandpileConfiguration();
		firings = new ArrayList<Integer>();
		graph = new SandpileGraph();
		pointsToVerts = new HashMap<float[], Integer>();
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
		canvas.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent evt) {
				me.keyPressed(evt);
			}
		});
	}

	public void mousePressed(MouseEvent evt) {
		mouseX = evt.getX();
		mouseY = evt.getY();
	}

	public void mouseDragged(MouseEvent evt) {
		yRot += 0.1f * (evt.getX() - mouseX);
		xRot += 0.1f * (evt.getY() - mouseY);
		mouseX = evt.getX();
		mouseY = evt.getY();
		canvas.repaint();
	}

	public void setCamera(float x, float y) {
		cameraX = x;
		cameraY = y;
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

	public void setColors(List<float[]> colors, List<float[]> inDebtColors) {
		this.colors = new ArrayList<float[]>(colors);
		this.inDebtColors = new ArrayList<float[]>(inDebtColors);
	}

	public ColorMode getColorMode() {
		return colorMode;
	}

	public void triangulate(List<float[]> vertexLocations) {
		if(vertexLocations.equals(tris.points()))
			return;
		pointsToVerts = new HashMap<float[], Integer>();
		int v = 0;
		for (float[] p : vertexLocations) {
			pointsToVerts.put(p, v);
			v++;
		}
		tris = new DelaunayTriangulation(vertexLocations);
	}

	public void paintSandpileGraph(SandpileGraph graph, List<float[]> vertexLocations, SandpileConfiguration config, List<Integer> firings, List<Integer> selectedVertices) {
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
			for (float[][] tri : tris.triangles()) {
				int v0 = pointsToVerts.get(tri[0]);
				int v1 = pointsToVerts.get(tri[1]);
				int v2 = pointsToVerts.get(tri[2]);
				float[] n = normalizedCross(tri[1][0] - tri[0][0], tri[1][1] - tri[0][1], h[v1] - h[v0],
						tri[2][0] - tri[0][0], tri[2][1] - tri[0][1], h[v2] - h[v0]);
				//System.err.println(c[v0][0]+" "+c[v0][1]+" "+c[v0][2]);
				gl.glNormal3fv(n, 0);
				gl.glColor3fv(c[v0],0);
				gl.glVertex3f(tri[0][0], tri[0][1], h[v0]);
				gl.glColor3fv(c[v1],0);
				gl.glVertex3f(tri[1][0], tri[1][1], h[v1]);
				gl.glColor3fv(c[v2],0);
				gl.glVertex3f(tri[2][0], tri[2][1], h[v2]);
				//gl.glVertex3f(tri[0][0], tri[0][1], tri[0][2]);
				
			}
			gl.glEnd();
		}

		if (drawWire) {
			gl.glColor3f(0f, 1f, 0f);
			for (float[][] tri : tris.triangles()) {
				int v0 = pointsToVerts.get(tri[0]);
				int v1 = pointsToVerts.get(tri[1]);
				int v2 = pointsToVerts.get(tri[2]);
				gl.glBegin(gl.GL_LINES);
				gl.glVertex3f(tri[0][0], tri[0][1], h[v0]);
				gl.glVertex3f(tri[1][0], tri[1][1], h[v1]);
				gl.glVertex3f(tri[1][0], tri[1][1], h[v1]);
				gl.glVertex3f(tri[2][0], tri[2][1], h[v2]);
				gl.glVertex3f(tri[2][0], tri[2][1], h[v2]);
				gl.glVertex3f(tri[0][0], tri[0][1], h[v0]);
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
		glu.gluPerspective(45.0f, h, 1.0, 500.0);
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	public void display(GLAutoDrawable drawable) {
		GL gl = drawable.getGL();

		// Clear the drawing area
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		// Reset the current matrix to the "identity"
		gl.glLoadIdentity();
		gl.glTranslatef(-cameraX, -cameraY, -cameraZ);
		gl.glRotatef(xRot, 1f, 0f, 0f);
		gl.glRotatef(yRot, 0f, 1f, 0f);
		drawTriangulation(gl, tris);

		// Flush all drawing operations to the graphics card
		gl.glFlush();
	}

	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
	}

	private float[] getColorForVertex(int vert) {
		//System.err.println(vert);
		float[] color = {0f, 0f, 0f};
		switch (colorMode) {
			case NUM_OF_GRAINS:
				int sand = Math.max(config.get(vert), -1);
				if (sand < 0) {
					color = inDebtColors.get(Math.min(-sand - 1, inDebtColors.size() - 1));
				} else {
					color = colors.get(Math.min(sand, colors.size() - 1));
				}
				break;
			case STABILITY:
				if (config.get(vert) < graph.degree(vert)) {
					color = colors.get(0);
				} else {
					color[0] = 1f;
					color[1] = 1f;
				}
				break;
			case FIRINGS:
				color = colors.get(Math.min(firings.get(vert), colors.size() - 1));
				break;
		}
		return color;
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
			for (int vert : graph.getOutgoingVertices(v)) {
				avg += heights[vert];
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
			for (int vert : graph.getOutgoingVertices(v)) {
				avg[0] += clrs[vert][0];
				avg[1] += clrs[vert][1];
				avg[2] += clrs[vert][2];
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
