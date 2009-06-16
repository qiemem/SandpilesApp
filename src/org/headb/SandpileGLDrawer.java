/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.headb;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.j2d.TextRenderer;
import java.awt.Canvas;
import java.util.List;
import java.util.ArrayList;
import javax.swing.event.MouseInputAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;

/**
 *
 * @author headb
 */
public class SandpileGLDrawer extends MouseInputAdapter implements MouseWheelListener, SandpileDrawer, GLEventListener {

	private GLCanvas canvas;
	private List<float[]> vertexLocations = new ArrayList<float[]>();
	private SandpileGraph graph = new SandpileGraph();
	private SandpileConfiguration config = new SandpileConfiguration();
	private int selectedVertex = -1;
	private float originX = 0.0f,  originY = 0.0f,  width = 200.0f,  height = 200.0f;
	private int canvasX,  canvasY,  canvasW,  canvasH;
	private boolean needsReshape = true;
	private float mouseX = 0f,  mouseY = 0f;
	private float vertSize = 1f;

	public boolean drawLabels = false;
	public boolean drawEdges = true;
	public boolean drawVertices = true;
	public boolean printFPS = false;
	public boolean repaint = true;

	private long timeOfLastDisplay = 0;

	public SandpileGLDrawer() {
		canvas = new GLCanvas();
		canvas.addGLEventListener(this);
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
	}

	public SandpileGLDrawer(GLCanvas canvas) {
		this.canvas = canvas;
		this.canvas.addGLEventListener(this);
		this.canvas.addMouseWheelListener(this);
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

	//gl.glEnable(gl.GL_DEPTH_TEST);
	//gl.glDepthFunc(gl.GL_LEQUAL);
	}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		canvasX = x;
		canvasY = y;
		canvasW = width;
		canvasH = height;
		GL gl = drawable.getGL();
		GLU glu = new GLU();

		if (height <= 0) { // avoid a divide by zero error!

			height = 1;
		}
		final float ratio = (float) width / (float) height;
		this.width = this.height * ratio;
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		//glu.gluPerspective(45.0f, h, 1.0, 1000.0);
		glu.gluOrtho2D(originX - this.width / 2f, originX + this.width / 2f, originY - this.height / 2f, originY + this.height / 2f);
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
		needsReshape = false;
	}

	public void display(GLAutoDrawable drawable) {
		if(!repaint)	return;

		if(printFPS){
			long curTime = System.currentTimeMillis();
			System.err.println(1000f/(curTime-timeOfLastDisplay));
			timeOfLastDisplay = curTime;
		}
		TextRenderer tr = new TextRenderer(new java.awt.Font("Courier", java.awt.Font.PLAIN, 12));

		GL gl = drawable.getGL();
		if (needsReshape) {
			reshape(canvas, canvasX, canvasY, canvasW, canvasH);
		}
		GLU glu = new GLU();
		// Clear the drawing area
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		// Reset the current matrix to the "identity"
		gl.glLoadIdentity();
		if(drawEdges)
			drawEdges(gl);
		if(drawVertices)
			drawVertices(gl,glu);
		if(drawLabels){
			drawVertexLabels(tr);
			drawEdgeLabels(tr);
		}
		drawSelected(gl);

		gl.glFlush();
	}

	private void drawEdges(GL gl) {
		gl.glColor3f(1.0f, 1.0f, 1.0f);
		gl.glBegin(gl.GL_LINES);
		for (int source = 0; source < vertexLocations.size(); source++) {
			for (int dest : graph.getOutgoingVertices(source)) {
				float sx = vertexLocations.get(source)[0];
				float sy = vertexLocations.get(source)[1];
				float dx = vertexLocations.get(dest)[0];
				float dy = vertexLocations.get(dest)[1];
				//Only draw the edges that aren't covered by vertices
				if(Math.sqrt((dx - sx) * (dx - sx) + (dy - sy) * (dy - sy)) > vertSize * 2f) {
					gl.glVertex2f(sx, sy);
					gl.glVertex2f(dx, dy);
				}
			}
		}
		gl.glEnd();
	}
	private void drawEdgeLabels(TextRenderer tr) {
		float textPlacement = 0.8f;
		tr.begin3DRendering();
		for (int source = 0; source < vertexLocations.size(); source++) {
			for (int dest : graph.getOutgoingVertices(source)) {
				float sx = vertexLocations.get(source)[0];
				float sy = vertexLocations.get(source)[1];
				float dx = vertexLocations.get(dest)[0];
				float dy = vertexLocations.get(dest)[1];
				//Only draw the edges that aren't covered by vertices
				if(Math.sqrt((dx - sx) * (dx - sx) + (dy - sy) * (dy - sy)) > vertSize * 2f) {
					float x = (1f-textPlacement)*sx + textPlacement*dx;
					float y = (1f-textPlacement)*sy + textPlacement*dy;
					tr.draw3D(Integer.toString(graph.weight(source, dest)), x, y, 0f, .2f*vertSize);
				}
			}
		}
		tr.end3DRendering();
	}

	private void drawVertices(GL gl, GLU glu) {
		gl.glBegin(gl.GL_QUADS);
		for (int vert = 0; vert < vertexLocations.size(); vert++) {
			float x = vertexLocations.get(vert)[0];
			float y = vertexLocations.get(vert)[1];
			setColorForVertex(gl, config.get(vert));
			//GLUquadric quadric = glu.gluNewQuadric();
			//gl.glTranslatef(x, y, 0f);
			//glu.gluDisk(quadric, 0.0, 1.0, 10, 1);
			//gl.glLoadIdentity();
			gl.glVertex2f(x - vertSize, y + vertSize);
			gl.glVertex2f(x + vertSize, y + vertSize);
			gl.glVertex2f(x + vertSize, y - vertSize);
			gl.glVertex2f(x - vertSize, y - vertSize);
		}
		gl.glEnd();
	}

	private void drawVertexLabels(TextRenderer tr){
		tr.begin3DRendering();
			for (int vert = 0; vert < vertexLocations.size(); vert++) {
				tr.draw3D(Integer.toString(config.get(vert)), vertexLocations.get(vert)[0]-vertSize, vertexLocations.get(vert)[1]-.9f*vertSize, 0f, .2f*vertSize);
			}
		tr.end3DRendering();
	}

	private void drawSelected(GL gl) {
		if (selectedVertex >= 0) {
			gl.glBegin(gl.GL_LINES);
				float x = vertexLocations.get(selectedVertex)[0];
				float y = vertexLocations.get(selectedVertex)[1];
				//GLUquadric quadric = glu.gluNewQuadric();
				//glu.gluDisk(quadric, 0.0, 1.0, 10, 1);
				gl.glColor3f(0f,1f,.5f);
				gl.glVertex2f(x - vertSize, y + vertSize);
				gl.glVertex2f(x + vertSize, y + vertSize);

				gl.glVertex2f(x + vertSize, y + vertSize);
				gl.glVertex2f(x + vertSize, y - vertSize);

				gl.glVertex2f(x + vertSize, y - vertSize);
				gl.glVertex2f(x - vertSize, y - vertSize);

				gl.glVertex2f(x - vertSize, y - vertSize);
				gl.glVertex2f(x - vertSize, y + vertSize);
			gl.glEnd();
		}
	}

	/**
	 * It is my understanding that this function is required by the GLEventListener interface but is not used anywhere yet.
	 *
	 * @param drawable
	 * @param modeChanged
	 * @param deviceChanged
	 */
	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
	}

	public Canvas getCanvas() {
		return canvas;
	}

	public void paintSandpileGraph(SandpileGraph graph, List<float[]> vertexLocations, SandpileConfiguration config, int selectedVertex) {
		this.graph = graph;
		this.vertexLocations = vertexLocations;
		this.config = config;
		this.selectedVertex = selectedVertex;
		canvas.display();
	}

	public float[] transformCanvasCoords(int x, int y) {
		float topLeftX = originX - width / 2f;
		float topLeftY = originY + height / 2f;
		float widthScale = width / (float) canvas.getWidth();
		float heightScale = height / (float) canvas.getHeight();
		float[] coords = {topLeftX + x * widthScale, topLeftY - y * heightScale};
		return coords;
	}

	public void setGLDimensions(float x, float y, float w, float h) {
		width = Math.max(w, 1f);
		height = Math.max(h, 1f);
		originX = x;
		originY = y;
		needsReshape = true;
		canvas.display();
	}

	public float getOriginX() {
		return originX;
	}

	public float getOriginY() {
		return originY;
	}

	public float getWidth() {
		return width;
	}

	public float getHeight() {
		return height;
	}

	@Override
	public void mousePressed(MouseEvent e) {
		float[] coords = transformCanvasCoords(e.getX(), e.getY());
		mouseX = coords[0];
		mouseY = coords[1];
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		float[] coords = transformCanvasCoords(e.getX(), e.getY());
		float deltaX = mouseX - coords[0];
		float deltaY = mouseY - coords[1];
		setGLDimensions(getOriginX() + deltaX, getOriginY() + deltaY, getWidth(), getHeight());
		coords = transformCanvasCoords(e.getX(), e.getY());
		mouseX = coords[0];
		mouseY = coords[1];
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		float amount = e.getUnitsToScroll();
		setGLDimensions(getOriginX(), getOriginY(), getWidth() + amount, getHeight() + amount);
	}

	private void setColorForVertex(GL gl, int sand) {
		switch (sand) {
			case 0:
				gl.glColor3f(0.2f, 0.2f, 0.2f);
				break;
			case 1:
				gl.glColor3f(0.0f, 0.0f, 1.0f);
				break;
			case 2:
				gl.glColor3f(0.0f, 1.0f, 1.0f);
				break;
			case 3:
				gl.glColor3f(0.0f, 1.0f, 0.0f);
				break;
			case 4:
				gl.glColor3f(1.0f, 0.0f, 0.0f);
				break;
			case 5:
				gl.glColor3f(1.0f, 1.0f, 0.0f);
				break;
			default:
				gl.glColor3f(1.0f, 1.0f, 1.0f);
		}
	}
}
