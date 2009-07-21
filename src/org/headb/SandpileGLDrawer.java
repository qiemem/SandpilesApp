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
	private List<Integer> selectedVertices = new ArrayList<Integer>();
	private float startingWidth = 200.0f, startingHeight = 200.0f, zoom = 1f;
	private float originX = 0.0f, originY = 0.0f, width = 200.0f, height = 200.0f;
	private int canvasX, canvasY, canvasW, canvasH;
	private boolean needsReshape = true;
	private float mouseX = 0f, mouseY = 0f;
	private float vertSize = 1f;
	private ColorMode mode = SandpileDrawer.ColorMode.NUM_OF_GRAINS;
	private ArrayList<float[]> colors;
	private ArrayList<float[]> inDebtColors;
	private List<Integer> firings = new ArrayList<Integer>();
	public boolean drawEdgeLabels = false;
	public boolean drawVertexLabels = false;
	public boolean drawEdges = true;
	public boolean drawVertices = true;
	public boolean printFPS = false;
	public boolean repaint = true;
	public boolean changingVertexSize = true;
	public boolean drawCircles = false;
	public boolean scrollOnDrag = true;
	private float[] selectionBox = {0f, 0f, 0f, 0f};
	private boolean drawSelectionBox = false;
	private long timeOfLastDisplay = 0;
	private ArrayList<ReshapeListener> repaintListeners = new ArrayList<ReshapeListener>();

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

	public void setColors(List<float[]> colors, List<float[]> inDebtColors){
		this.colors = new ArrayList<float[]>(colors);
		this.inDebtColors = new ArrayList<float[]>(inDebtColors);
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
		for (ReshapeListener r : repaintListeners) {
			r.onReshape();
		}
	}

	public void display(GLAutoDrawable drawable) {
		if (!repaint) {
			return;
		}

		if (printFPS) {
			long curTime = System.currentTimeMillis();
			System.err.println(1000f / (curTime - timeOfLastDisplay));
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
		if (drawEdges) {
			drawEdges(gl);
		}
		if (drawVertices) {
			drawVertices(gl, glu);
		}
		if (drawVertexLabels) {
			drawVertexLabels(tr);
		}
		if (drawEdgeLabels) {
			drawEdgeLabels(tr);
		}
		if (!selectedVertices.isEmpty()) {
			drawSelected(gl);
		}
		if (drawSelectionBox) {
			drawSelectionBox(gl);
		}

		gl.glFlush();
	}

	public void setColorMode(ColorMode cm) {
		mode = cm;
	}

	public ColorMode getColorMode() {
		return mode;
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
				//if (Math.sqrt((dx - sx) * (dx - sx) + (dy - sy) * (dy - sy)) > vertSize * 2f + 0.01f) {
				gl.glVertex2f(sx, sy);
				gl.glVertex2f(dx, dy);
				//}
			}
		}
		gl.glEnd();
	}

	private void drawEdgeLabels(TextRenderer tr) {
		float textPlacement = 0.8f;
		tr.setColor(.8f, .5f, .6f, 1f);
		tr.begin3DRendering();
		for (int source = 0; source < vertexLocations.size(); source++) {
			for (int dest : graph.getOutgoingVertices(source)) {
				float sx = vertexLocations.get(source)[0];
				float sy = vertexLocations.get(source)[1];
				float dx = vertexLocations.get(dest)[0];
				float dy = vertexLocations.get(dest)[1];
				//Only draw the edges that aren't covered by vertices
				//if (Math.sqrt((dx - sx) * (dx - sx) + (dy - sy) * (dy - sy)) > vertSize * 2f + 0.01f) {
				float x = (1f - textPlacement) * sx + textPlacement * dx;
				float y = (1f - textPlacement) * sy + textPlacement * dy;
				String str = Integer.toString(graph.weight(source, dest));
				tr.draw3D(str, x, y, 0f, .15f * vertSize / str.length());
				//}
			}
		}
		tr.end3DRendering();
	}

	private void drawVertices(GL gl, GLU glu) {
		if (drawCircles) {
			GLUquadric quadric = glu.gluNewQuadric();
			for (int vert = 0; vert < vertexLocations.size(); vert++) {
				float x = vertexLocations.get(vert)[0];
				float y = vertexLocations.get(vert)[1];
				float size = vertSize;
				int sand = Math.max(config.get(vert), 0);
				if (changingVertexSize && !graph.isSink(vert)) {
					size = Math.min(((float) sand + 1f) / ((float) graph.degree(vert)), vertSize);
				}
				setColorForVertex(gl, vert);
				gl.glTranslatef(x, y, 0f);
				glu.gluDisk(quadric, 0.0, size, 8, 1);
				gl.glLoadIdentity();
			}
		} else {
			gl.glBegin(gl.GL_QUADS);
			for (int vert = 0; vert < vertexLocations.size(); vert++) {
				float x = vertexLocations.get(vert)[0];
				float y = vertexLocations.get(vert)[1];
				float size = vertSize;
				int sand = Math.max(config.get(vert), 0);
				if (changingVertexSize && !graph.isSink(vert)) {
					size = Math.min(((float) sand + 1f) / ((float) graph.degree(vert)), vertSize);
				}
				setColorForVertex(gl, vert);
				gl.glVertex2f(x - size, y + size);
				gl.glVertex2f(x + size, y + size);
				gl.glVertex2f(x + size, y - size);
				gl.glVertex2f(x - size, y - size);
			}
			gl.glEnd();
		}
	}

	private void drawVertexLabels(TextRenderer tr) {
		tr.setColor(.8f, .5f, .6f, 1f);
		tr.begin3DRendering();
		for (int vert = 0; vert < vertexLocations.size(); vert++) {
			int amount = 0;
			switch(mode){
				case NUM_OF_GRAINS:
					amount = config.get(vert);
					break;
				case STABILITY:
					if (config.get(vert) < graph.degree(vert)) {
						amount = 0;
					} else {
						amount = 1;
					}
					break;
				case FIRINGS:
					amount = firings.get(vert);
					break;
			}
			String str = Integer.toString(amount);
			if(amount!=0)
				tr.draw3D(str, vertexLocations.get(vert)[0] - vertSize, vertexLocations.get(vert)[1] - .9f * vertSize, 0f, .15f * vertSize / str.length());

		}
		tr.end3DRendering();
	}

	private void drawSelected(GL gl) {
		for (Integer selectedVertex : selectedVertices) {
			if (selectedVertex >= 0) {
				gl.glBegin(gl.GL_LINES);
				float x = vertexLocations.get(selectedVertex)[0];
				float y = vertexLocations.get(selectedVertex)[1];
				//GLUquadric quadric = glu.gluNewQuadric();
				//glu.gluDisk(quadric, 0.0, 1.0, 10, 1);
				gl.glColor3f(0f, 1f, .5f);
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
	}

	private void drawSelectionBox(GL gl) {
		gl.glColor3f(0f, 1f, .5f);
		//System.err.println(selectionBox[0]+" "+selectionBox[1]+" "+selectionBox[2]+" "+selectionBox[3]);
		gl.glBegin(gl.GL_LINES);
		//top
		gl.glVertex2f(selectionBox[2], selectionBox[1]);
		gl.glVertex2f(selectionBox[0], selectionBox[1]);
		//right
		gl.glVertex2f(selectionBox[0], selectionBox[1]);
		gl.glVertex2f(selectionBox[0], selectionBox[3]);
		//bottom
		gl.glVertex2f(selectionBox[0], selectionBox[3]);
		gl.glVertex2f(selectionBox[2], selectionBox[3]);
		//left
		gl.glVertex2f(selectionBox[2], selectionBox[3]);
		gl.glVertex2f(selectionBox[2], selectionBox[1]);
		gl.glEnd();
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

	public GLCanvas getCanvas() {
		return canvas;
	}

	public void paintSandpileGraph(SandpileGraph graph, List<float[]> vertexLocations, SandpileConfiguration config, List<Integer> firings, List<Integer> selectedVertices) {
		this.graph = graph;
		this.vertexLocations = vertexLocations;
		this.config = config;
		this.selectedVertices = selectedVertices;
		this.firings = firings;
		canvas.display();
	}

	public void setSelectionBox(float maxX, float maxY, float minX, float minY) {
		selectionBox[0] = maxX;
		selectionBox[1] = maxY;
		selectionBox[2] = minX;
		selectionBox[3] = minY;
		drawSelectionBox = true;
	}

	public void clearSelectionBox() {
		drawSelectionBox = false;
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

	public float getZoom() {
		return zoom;
	}

	public void setZoom(float zoom) {
		this.zoom = Math.max(zoom, 0.00001f);
		height = startingHeight / zoom;
		width = startingWidth / zoom;
		needsReshape = true;
		canvas.repaint();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		float[] coords = transformCanvasCoords(e.getX(), e.getY());
		mouseX = coords[0];
		mouseY = coords[1];
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (!scrollOnDrag) {
			return;
		}
		float[] coords = transformCanvasCoords(e.getX(), e.getY());
		float deltaX = mouseX - coords[0];
		float deltaY = mouseY - coords[1];
		setGLDimensions(getOriginX() + deltaX, getOriginY() + deltaY, getWidth(), getHeight());
		coords = transformCanvasCoords(e.getX(), e.getY());
		mouseX = coords[0];
		mouseY = coords[1];
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		float amount = 1f - 0.01f * e.getUnitsToScroll();
		setZoom(getZoom() * amount);
		//setGLDimensions(getOriginX(), getOriginY(), getWidth() + amount, getHeight() + amount);
	}

	private void setColorForVertex(GL gl, int vert) {
		float[] color = {0f, 0f, 0f};
		switch (mode) {
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

	public void addReshapeListener(ReshapeListener r) {
		repaintListeners.add(r);
	}
}
