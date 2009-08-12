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
import gnu.trove.TIntArrayList;
/**
 *
 * @author headb
 */
public class SandpileGLDrawer extends MouseInputAdapter implements MouseWheelListener, SandpileDrawer, GLEventListener {

	private GLCanvas canvas;
	private Float2dArrayList vertexLocations = new Float2dArrayList(0, 2);
	private SandpileGraph graph = new SandpileGraph();
	private SandpileConfiguration config = new SandpileConfiguration();
	private TIntArrayList selectedVertices = new TIntArrayList();
	private float startingWidth = 200.0f, startingHeight = 200.0f, zoom = 1f;
	private float originX = 0.0f, originY = 0.0f, width = 200.0f, height = 200.0f;
	private int canvasX, canvasY, canvasW, canvasH;
	private boolean needsReshape = true;
	private float mouseX = 0f, mouseY = 0f;
	private float vertSize = 1f;
	private ColorMode mode = SandpileDrawer.ColorMode.NUM_OF_GRAINS;
	private Float2dArrayList colors;
	private Float2dArrayList inDebtColors;
	private TIntArrayList firings = new TIntArrayList();
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
	private float[] backgroundColor = {0f, 0f, 1f};

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

	public void setColors(Float2dArrayList colors, Float2dArrayList inDebtColors, float[] backgroundColor){
		this.colors = new Float2dArrayList(colors);
		this.inDebtColors = new Float2dArrayList(inDebtColors);
		this.backgroundColor = backgroundColor;
	}

	public void init(GLAutoDrawable drawable) {
		// Use debug pipeline
		// drawable.setGL(new DebugGL(drawable.getGL()));

		GL gl = drawable.getGL();
		System.err.println("INIT GL IS: " + gl.getClass().getName());

		// Enable VSync
		gl.setSwapInterval(1);

		// Setup the drawing area and shading mode
		gl.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], 0.0f);
		gl.glShadeModel(GL.GL_FLAT);

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
//		for (ReshapeListener r : repaintListeners) {
//			r.onReshape();
//		}
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
		

		GL gl = drawable.getGL();
		if (needsReshape) {
			reshape(canvas, canvasX, canvasY, canvasW, canvasH);
		}
		//GLU glu = new GLU();
		// Clear the drawing area
		gl.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], 0.0f);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		// Reset the current matrix to the "identity"
		gl.glLoadIdentity();
		if (drawEdges) {
			drawEdges(gl);
		}
		if (drawVertices) {
			drawVertices(gl);
		}
		if (drawVertexLabels) {
			TextRenderer tr = new TextRenderer(new java.awt.Font("Courier", java.awt.Font.PLAIN, 12));
			drawVertexLabels(tr);
		}
		if (drawEdgeLabels) {
			TextRenderer tr = new TextRenderer(new java.awt.Font("Courier", java.awt.Font.PLAIN, 8));
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
		for (int source = 0; source < graph.numVertices(); source++) {
			EdgeList outEdges = graph.getOutgoingEdges(source);
			for (int i=0; i<outEdges.size(); i++) {
				int dest = outEdges.destQuick(i);
				float sx = vertexLocations.get(source,0);
				float sy = vertexLocations.get(source,1);
				float dx = vertexLocations.get(dest,0);
				float dy = vertexLocations.get(dest,1);
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
		for (int source = 0; source < graph.numVertices(); source++) {
			EdgeList outEdges = graph.getOutgoingEdges(source);
			for (int i=0; i<outEdges.size(); i++) {
				int dest = outEdges.destQuick(i);
				int wt = outEdges.wtQuick(i);
				float sx = vertexLocations.get(source,0);
				float sy = vertexLocations.get(source,1);
				float dx = vertexLocations.get(dest,0);
				float dy = vertexLocations.get(dest,1);
				//Only draw the edges that aren't covered by vertices
				//if (Math.sqrt((dx - sx) * (dx - sx) + (dy - sy) * (dy - sy)) > vertSize * 2f + 0.01f) {
				float x = (1f - textPlacement) * sx + textPlacement * dx;
				float y = (1f - textPlacement) * sy + textPlacement * dy;
				String str = String.valueOf(wt);
				tr.draw3D(str, x, y, 0f, .15f * vertSize / str.length());
				//}
			}
		}
		tr.end3DRendering();
	}

	private void drawVertices(GL gl) {
		gl.glBegin(gl.GL_QUADS);
		for (int vert = 0; vert < graph.numVertices(); vert++) {
			float x = vertexLocations.getQuick(vert,0);
			float y = vertexLocations.getQuick(vert,1);
			float size = vertSize;
			int d = graph.degree(vert);
			if (changingVertexSize && d!=0) {
				int sand = Math.max(config.get(vert), 0);
				size = Math.min(((float) sand + 1f) / ((float) d), vertSize);
			}
			setColorForVertex(gl, vert);
			gl.glVertex2f(x - size, y + size);
			gl.glVertex2f(x + size, y + size);
			gl.glVertex2f(x + size, y - size);
			gl.glVertex2f(x - size, y - size);
		}
		gl.glEnd();
	}

	private void drawVertexLabels(TextRenderer tr) {
		tr.setColor(.8f, .5f, .6f, 1f);
		tr.begin3DRendering();
		for (int vert = 0; vert < graph.numVertices(); vert++) {
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
				tr.draw3D(str, vertexLocations.get(vert,0) - vertSize, vertexLocations.get(vert,1) - .9f * vertSize, 0f, .15f * vertSize / str.length());

		}
		tr.end3DRendering();
	}

	private void drawSelected(GL gl) {
		for (int i=0; i<selectedVertices.size(); i++) {
			int selectedVertex = selectedVertices.get(i);
			if (selectedVertex >= 0) {
				gl.glBegin(gl.GL_LINES);
				float x = vertexLocations.get(selectedVertex,0);
				float y = vertexLocations.get(selectedVertex,1);
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

	public void paintSandpileGraph(SandpileGraph graph, Float2dArrayList vertexLocations, SandpileConfiguration config, TIntArrayList firings, TIntArrayList selectedVertices) {
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
	}

	private void setColorForVertex(GL gl, int vert) {
		int color = 0;
		boolean inDebt=false;
		switch (mode) {
			case NUM_OF_GRAINS:
				int sand = Math.max(config.getQuick(vert), -1);
				if(sand<0){
					color = Math.min(-sand-1, inDebtColors.rows()-1);
					inDebt = true;
				}else
					color = Math.min(sand, colors.rows()-1);
				break;
			case STABILITY:
				if (config.getQuick(vert) < graph.degree(vert)) {
					color = 0;
				} else {
					color = colors.rows()-1;
				}
				break;
			case FIRINGS:
				color = Math.min(firings.getQuick(vert),colors.rows()-1);
				break;
		}
		if(inDebt)
			gl.glColor3f(inDebtColors.getQuick(color, 0),inDebtColors.getQuick(color, 1), inDebtColors.getQuick(color, 2));
		else
			gl.glColor3f(colors.getQuick(color, 0), colors.getQuick(color,1), colors.getQuick(color, 2));
	}

//	public void addReshapeListener(ReshapeListener r) {
//		repaintListeners.add(r);
//	}
}
