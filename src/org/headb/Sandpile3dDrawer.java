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
public class Sandpile3dDrawer implements SandpileDrawer, GLEventListener  {

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


	private int mouseX, mouseY;
	private float xRot = 0f, yRot = 0f;
	private float cameraX = 0f, cameraY = 0f, cameraZ = 100f;

	public Sandpile3dDrawer(GLCanvas canvas){
		colorMode = ColorMode.NUM_OF_GRAINS;
		this.canvas = canvas;
		this.canvas.addGLEventListener(this);
		tris = new DelaunayTriangulation(new ArrayList<float[]>());
		config = new SandpileConfiguration();
		firings = new ArrayList<Integer>();
		graph = new SandpileGraph();
		pointsToVerts = new HashMap<float[], Integer>();
		final Sandpile3dDrawer me = this;
		canvas.addMouseListener(new MouseAdapter(){
			@Override public void mousePressed(MouseEvent evt){
				me.mousePressed(evt);
			}
		});
		canvas.addMouseMotionListener(new MouseMotionAdapter(){
			@Override public void mouseDragged(MouseEvent evt){
				me.mouseDragged(evt);
			}
		});
		canvas.addKeyListener(new KeyAdapter(){
			@Override public void keyPressed(KeyEvent evt){
				me.keyPressed(evt);
			}
		});
	}

	public void mousePressed(MouseEvent evt){
		mouseX = evt.getX();
		mouseY = evt.getY();
	}

	public void mouseDragged(MouseEvent evt){
		yRot += 0.1f*(evt.getX() - mouseX);
		xRot += 0.1f*(evt.getY() - mouseY);
		mouseX = evt.getX();
		mouseY = evt.getY();
		canvas.repaint();
	}

	public void keyPressed(KeyEvent evt){
		switch(evt.getKeyCode()){
			case KeyEvent.VK_UP:
				cameraY+=1f;
				break;
			case KeyEvent.VK_DOWN:
				cameraY-=1f;
				break;
			case KeyEvent.VK_LEFT:
				cameraX-=1f;
				break;
			case KeyEvent.VK_RIGHT:
				cameraX+=1f;
				break;
		}
		canvas.repaint();
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

	public void triangulate(List<float[]> vertexLocations){
		pointsToVerts = new HashMap<float[], Integer>();
		ArrayList<float[]> locations3d = new ArrayList<float[]>();
		int v = 0;
		for(float[] p : vertexLocations){
			float[] newP = {p[0], p[1], 0f};
			locations3d.add(newP);
			pointsToVerts.put(newP, v);
			v++;
		}
		tris = new DelaunayTriangulation(locations3d);
		System.err.println(tris.points().size());
	}

	public void paintSandpileGraph(SandpileGraph graph, List<float[]> vertexLocations, SandpileConfiguration config, List<Integer> firings, List<Integer> selectedVertices) {
		ArrayList<Float> heights = new ArrayList<Float>();
		for(int v : config){
			heights.add((float)v);
		}
		this.config = config;
		this.firings = firings;
		this.graph = graph;
		tris.assignHeights(heights);
		canvas.repaint();
	}

	public void setSelectionBox(float maxX, float maxY, float minX, float minY) {
	}

	public void clearSelectionBox() {
	}

	public float[] transformCanvasCoords(int x, int y) {
		throw (new UnsupportedOperationException());
	}

	public float[] normalizedCross(float x1, float y1, float z1, float x2, float y2, float z2){
		float x = y1*z2 - z1*y2;
		float y = z1*x2 - x1*z2;
		float z = x1*y2 - y1*x2;
		float l = (float) Math.sqrt(x*x + y*y + z*z);
		x /= l;
		y /= l;
		z /= l;
		float[] normal = new float[3];
		normal[0]=x;
		normal[1]=y;
		normal[2]=z;
		return normal;
	}

	public void drawTriangulation(GL gl, DelaunayTriangulation tris){
		//System.err.println("Drawing tris");
		for(float[][] tri : tris.triangles()){
			int v0 = pointsToVerts.get(tri[0]);
			int v1 = pointsToVerts.get(tri[1]);
			int v2 = pointsToVerts.get(tri[2]);
			float h0 = getHeightForVertex(v0);
			float h1 = getHeightForVertex(v1);
			float h2 = getHeightForVertex(v2);
			float[] n = normalizedCross(tri[1][0]-tri[0][0], tri[1][1]-tri[0][1], h1-h0,
										tri[2][0]-tri[0][0], tri[2][1]-tri[0][1], h2-h0);

			gl.glBegin(gl.GL_TRIANGLES);
			gl.glNormal3f(n[0], n[1], n[2]);
			setColorForVertex(gl,pointsToVerts.get(tri[0]));
			gl.glVertex3f(tri[0][0], tri[0][1], h0);
			setColorForVertex(gl,pointsToVerts.get(tri[1]));
			gl.glVertex3f(tri[1][0], tri[1][1], h1);
			setColorForVertex(gl, pointsToVerts.get(tri[2]));
			gl.glVertex3f(tri[2][0], tri[2][1], h2);
			//gl.glVertex3f(tri[0][0], tri[0][1], tri[0][2]);
			gl.glEnd();
		}
		for(float[][] tri : tris.triangles()){
			int v0 = pointsToVerts.get(tri[0]);
			int v1 = pointsToVerts.get(tri[1]);
			int v2 = pointsToVerts.get(tri[2]);
			float h0 = getHeightForVertex(v0);
			float h1 = getHeightForVertex(v1);
			float h2 = getHeightForVertex(v2);
			gl.glColor3f(0f,1f,0f);
			gl.glBegin(gl.GL_LINES);
			gl.glVertex3f(tri[0][0], tri[0][1], h0);
			gl.glVertex3f(tri[1][0], tri[1][1], h1);
			gl.glVertex3f(tri[1][0], tri[1][1], h1);
			gl.glVertex3f(tri[2][0], tri[2][1], h2);
			gl.glVertex3f(tri[2][0], tri[2][1], h2);
			gl.glVertex3f(tri[0][0], tri[0][1], h0);
			gl.glEnd();
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
		drawTriangulation(gl,tris);

		// Flush all drawing operations to the graphics card
		gl.glFlush();
	}

	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
	}

	private void setColorForVertex(GL gl, int vert) {
		//System.err.println(vert);
		float[] color = {0f, 0f, 0f};
		switch (colorMode) {
			case NUM_OF_GRAINS:
				int sand = Math.max(config.get(vert), -1);
				if(sand<0)
					color = inDebtColors.get(Math.min(-sand-1, inDebtColors.size()-1));
				else
					color = colors.get(Math.min(sand, colors.size()-1));
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
				color = colors.get(Math.min(firings.get(vert),colors.size()-1));
				break;
		}
		gl.glColor3f(color[0], color[1], color[2]);
	}

	private float getHeightForVertex(int vert) {
		if(graph.isSink(vert))
			return 0f;
		else
			return 2f*(float) config.get(vert);
	}
}
