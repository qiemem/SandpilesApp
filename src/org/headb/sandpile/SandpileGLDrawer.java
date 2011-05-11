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
package org.headb.sandpile;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.j2d.TextRenderer;
import java.nio.FloatBuffer;

import com.sun.opengl.util.BufferUtil;
import javax.swing.event.MouseInputAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;
import gnu.trove.list.array.TIntArrayList;
import java.util.concurrent.locks.Lock;

/**
 * A SandpileDrawer that represents the sandpile as a graph in 2d using OpenGL.
 * This is the standard visual representation of a sandpile.
 * Does all the mouse view control work.
 * @author Bryan Head
 */
public class SandpileGLDrawer extends MouseInputAdapter implements MouseWheelListener, SandpileDrawer, GLEventListener {

    private static final int[] LOCATION_BUFFER = {0};
    private static final int[] COLOR_BUFFER = {1};

    private FloatBuffer locationBuffer = BufferUtil.newFloatBuffer(0);
    private FloatBuffer colorBuffer = BufferUtil.newFloatBuffer(0);

    private GLAutoDrawable canvas;
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
    private SandpileConfiguration baseConfig;
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
    private FloatBuffer colorArray;
    private int numInDebtColors = 0;

    public SandpileGLDrawer() {
	this(new GLJPanel());
        canvas = new GLJPanel();
    }

    public SandpileGLDrawer(GLAutoDrawable canvas) {
        this.canvas = canvas;
        this.canvas.addGLEventListener(this);
        this.canvas.addMouseWheelListener(this);
    }

    public void setColors(Float2dArrayList colors, Float2dArrayList inDebtColors, float[] backgroundColor) {
        this.colors = new Float2dArrayList(colors);
        this.inDebtColors = new Float2dArrayList(inDebtColors);
        this.backgroundColor = backgroundColor;


        numInDebtColors = inDebtColors.rows();
        colorArray = BufferUtil.newFloatBuffer((numInDebtColors + colors.rows()) * 3);
        for ( int i = 0; i < numInDebtColors; i++) {
            colorArray.put(inDebtColors.getQuick(i, 0));
            colorArray.put(inDebtColors.getQuick(i, 1));
            colorArray.put(inDebtColors.getQuick(i, 2));
        }
        for (int i = 0; i < colors.rows(); i++) {
            colorArray.put(colors.getQuick(i, 0));
            colorArray.put(colors.getQuick(i, 1));
            colorArray.put(colors.getQuick(i, 2));
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
        gl.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], 0.0f);
        gl.glShadeModel(GL.GL_FLAT);

        gl.glGenBuffersARB(1, LOCATION_BUFFER, 0);
        gl.glGenBuffersARB(1, COLOR_BUFFER, 0);
        
        //gl.glEnableClientState(gl.GL_COLOR_ARRAY);

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
        //colorArray.rewind();
        //gl.glColorPointer(3, gl.GL_FLOAT, 0, colorArray);
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
            renderVBOs(gl);
        }
        if (drawVertexLabels) {
            TextRenderer tr = new TextRenderer(new java.awt.Font("Courier", java.awt.Font.PLAIN, 12));
            drawVertexLabels(gl, tr);
        }
        if (drawEdgeLabels) {
            TextRenderer tr = new TextRenderer(new java.awt.Font("Courier", java.awt.Font.PLAIN, 6));
            drawEdgeLabels(gl, tr);
        }
        if (!selectedVertices.isEmpty()) {
            drawSelected(gl);
        }
        if (drawSelectionBox) {
            drawSelectionBox(gl);
        }


        
        gl.glFlush();
    
    }

    public void renderVBOs(GL gl) {
        gl.glBindBufferARB(gl.GL_ARRAY_BUFFER_ARB, LOCATION_BUFFER[0]);
        gl.glVertexPointer(2, gl.GL_FLOAT, 0, 0);

        gl.glBindBufferARB(gl.GL_ARRAY_BUFFER_ARB, COLOR_BUFFER[0]);
        gl.glColorPointer(3, gl.GL_FLOAT, 0, 0);

        gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
        gl.glEnableClientState(gl.GL_COLOR_ARRAY);

        gl.glDrawArrays(gl.GL_QUADS, 0, locationBuffer.limit()/2);

        gl.glDisableClientState(gl.GL_VERTEX_ARRAY);
        gl.glDisableClientState(gl.GL_COLOR_ARRAY);
        
    }

    public void updateLocationBuffer(GL gl) {
        int numVertices = graph.numVertices();
        if(numVertices*2*4 != locationBuffer.limit()) {
            locationBuffer = BufferUtil.newFloatBuffer(numVertices*2*4);
            locationBuffer.limit(numVertices*2*4);
        }
        
        for (int vert = 0; vert < numVertices; vert++) {
            float x = vertexLocations.getQuick(vert, 0);
            float y = vertexLocations.getQuick(vert, 1);
            float size = vertSize;
            int d = graph.degreeQuick(vert);
            if (changingVertexSize && d != 0) {
                int sand = Math.max(config.getQuick(vert), 0);
                size = Math.min(((float) sand + 1f) / ((float) d), vertSize);
            }

            locationBuffer.put(x-size);
            locationBuffer.put(y+size);
            locationBuffer.put(x+size);
            locationBuffer.put(y+size);
            locationBuffer.put(x+size);
            locationBuffer.put(y-size);
            locationBuffer.put(x-size);
            locationBuffer.put(y-size);
        }
        locationBuffer.flip();
        gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, LOCATION_BUFFER[0]);
        gl.glBufferDataARB(gl.GL_ARRAY_BUFFER_ARB, locationBuffer.limit()*BufferUtil.SIZEOF_FLOAT, locationBuffer, gl.GL_STREAM_DRAW_ARB);
    }

    public void updateColorBuffer(GL gl) {
        int numVertices = graph.numVertices();
        if(numVertices*3*4 != colorBuffer.limit()) {
            colorBuffer = BufferUtil.newFloatBuffer(numVertices*3*4);
            colorBuffer.limit(numVertices*3*4);
        }
        float[] colorComp = new float[3];
        for (int vert = 0; vert < numVertices; vert++) {
            int color = getColorForVertex(vert);
            float r = colorArray.get(color*3);
            float g = colorArray.get(color*3+1);
            float b = colorArray.get(color*3+2);
            float[] array = {r,g,b,r,g,b,r,g,b,r,g,b};
//            colorComp[0] = colorArray.get(color*3);
//            colorComp[1] = colorArray.get(color*3+1);
//            colorComp[2] = colorArray.get(color*3+2);
//            colorBuffer.put(colorComp);
//            colorBuffer.put(colorComp);
//            colorBuffer.put(colorComp);
//            colorBuffer.put(colorComp);
//            colorBuffer.put(r);
//            colorBuffer.put(g);
//            colorBuffer.put(b);
//            colorBuffer.put(r);
//            colorBuffer.put(g);
//            colorBuffer.put(b);
//            colorBuffer.put(r);
//            colorBuffer.put(g);
//            colorBuffer.put(b);
//            colorBuffer.put(r);
//            colorBuffer.put(g);
//            colorBuffer.put(b);
            colorBuffer.put(array);
        }
        colorBuffer.flip();
        gl.glBindBufferARB(gl.GL_ARRAY_BUFFER_ARB, COLOR_BUFFER[0]);
        gl.glBufferDataARB(gl.GL_ARRAY_BUFFER_ARB, colorBuffer.limit()*BufferUtil.SIZEOF_FLOAT, colorBuffer, gl.GL_STREAM_DRAW_ARB);
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
        int numVertices = graph.numVertices();
        for (int source = 0; source < numVertices; source++) {
            EdgeList outEdges = graph.getOutgoingEdges(source);
            int numOutEdges = outEdges.size();
            for (int i = 0; i < numOutEdges; i++) {
                int dest = outEdges.destQuick(i);
                float sx = vertexLocations.getQuick(source, 0);
                float sy = vertexLocations.getQuick(source, 1);
                float dx = vertexLocations.getQuick(dest, 0);
                float dy = vertexLocations.getQuick(dest, 1);
                //Only draw the edges that aren't covered by vertices
                //if (Math.sqrt((dx - sx) * (dx - sx) + (dy - sy) * (dy - sy)) > vertSize * 2f + 0.01f) {
                gl.glVertex2f(sx, sy);
                gl.glVertex2f(dx, dy);
                //}
            }
        }
        gl.glEnd();
    }

    private void drawEdgeLabels(GL gl, TextRenderer tr) {
        // If the GL_COLOR_ARRAY isn't disabled, the colors on text go all
        // crazy. This is kind of a hack, but it works.
        gl.glDisableClientState(gl.GL_COLOR_ARRAY);
        float textPlacement = 0.8f;
        tr.setColor(.8f, .5f, .6f, 1f);
        tr.begin3DRendering();
        int numVertices = graph.numVertices();
        for (int source = 0; source < numVertices; source++) {
            EdgeList outEdges = graph.getOutgoingEdges(source);
            int numOutEdges = outEdges.size();
            for (int i = 0; i < numOutEdges; i++) {
                int dest = outEdges.destQuick(i);
                int wt = outEdges.wtQuick(i);
                float sx = vertexLocations.get(source, 0);
                float sy = vertexLocations.get(source, 1);
                float dx = vertexLocations.get(dest, 0);
                float dy = vertexLocations.get(dest, 1);
                float x = (1f - textPlacement) * sx + textPlacement * dx;
                float y = (1f - textPlacement) * sy + textPlacement * dy;
                String str = String.valueOf(wt);
                tr.draw3D(str, x, y, 0f, .15f * vertSize / str.length());
            }
        }
        tr.end3DRendering();
        gl.glEnableClientState(gl.GL_COLOR_ARRAY);
    }

    private void drawVertices(GL gl) {
        updateColorBuffer(gl);
        updateLocationBuffer(gl);
    }

    private void drawVertexLabels(GL gl, TextRenderer tr) {
        // If the GL_COLOR_ARRAY isn't disabled, the colors on text go all
        // crazy. This is kind of a hack, but it works.
        gl.glDisableClientState(gl.GL_COLOR_ARRAY);
        tr.setColor(.8f, .5f, .6f, 1f);
        tr.begin3DRendering();
        int numVertices = graph.numVertices();
        for (int vert = 0; vert < numVertices; vert++) {
            int amount = 0;
            switch (mode) {
                case NUM_OF_GRAINS:
                    amount = config.getQuick(vert);
                    break;
                case STABILITY:
                    if (config.getQuick(vert) < graph.degreeQuick(vert)) {
                        amount = 0;
                    } else {
                        amount = 1;
                    }
                    break;
                case FIRINGS:
                    amount = firings.getQuick(vert);
                    break;
                case DIFFERENCE:
                    amount = config.getQuick(vert) - baseConfig.getQuick(vert);
                    break;
            }
            String str = Integer.toString(amount);
            if (amount != 0) {
                tr.draw3D(str, vertexLocations.get(vert, 0) - vertSize, vertexLocations.get(vert, 1) - .9f * vertSize, 0f, .15f * vertSize / str.length());

            }

        }
        tr.end3DRendering();
        gl.glEnableClientState(gl.GL_COLOR_ARRAY);
    }

    private void drawSelected(GL gl) {
        int numVertices = selectedVertices.size();
        for (int i = 0; i < numVertices; i++) {
            int selectedVertex = selectedVertices.get(i);
            if (selectedVertex >= 0) {
                gl.glBegin(gl.GL_LINES);
                float x = vertexLocations.get(selectedVertex, 0);
                float y = vertexLocations.get(selectedVertex, 1);
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

    public GLAutoDrawable getCanvas() {
        return canvas;
    }

    public void paintSandpileGraph(SandpileGraph graph, Float2dArrayList vertexLocations, SandpileConfiguration config, TIntArrayList firings, TIntArrayList selectedVertices, Lock configLock) {
        this.graph = graph;
        this.vertexLocations = vertexLocations;
        //this.config = config;
        configLock.lock();
        try{
            this.config.setTo(config);
        } finally {
            configLock.unlock();
        }
        this.selectedVertices = selectedVertices;
        this.firings = firings;
        canvas.display();
    }

    public void setBaseConfig(SandpileConfiguration baseConfig) {
        this.baseConfig = new SandpileConfiguration(baseConfig);
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
        width = Math.max(w, 0.00001f);
        height = Math.max(h, 0.00001f);
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

    private int getColorForVertex(int vert) {
        int color = 0;
        switch (mode) {
            case NUM_OF_GRAINS:
                int sand = config.getQuick(vert);
                if (sand < 0) {
                    color = Math.min(-sand - 1, inDebtColors.rows() - 1);
                } else {
                    color = Math.min(sand, colors.rows() - 1);
                }
                break;
            case STABILITY:
                if (config.getQuick(vert) < graph.degreeQuick(vert)) {
                    color = 0;
                } else {
                    color = colors.rows() - 1;
                }
                break;
            case FIRINGS:
                color = Math.min(firings.getQuick(vert), colors.rows() - 1);
                break;
            case DIFFERENCE:
                sand = config.getQuick(vert) - baseConfig.getQuick(vert);
                if (sand < 0) {
                    color = Math.min(-sand - 1, inDebtColors.rows() - 1);
                } else {
                    color = Math.min(sand, colors.rows() - 1);
                }
                break;
        }
        return color+numInDebtColors;
    }
//	public void addReshapeListener(ReshapeListener r) {
//		repaintListeners.add(r);
//	}
}
