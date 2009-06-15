/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.headb;

import javax.media.opengl.*;
import javax.media.opengl.glu.GLU;
import java.awt.Canvas;
import java.util.List;

/**
 *
 * @author headb
 */
public class SandpileGLDrawer implements SandpileDrawer, GLEventListener{
	private GLCanvas canvas = new GLCanvas();

	private List<float[]> vertexLocations;
	private SandpileGraph graph;
	private SandpileConfiguration config;


	public SandpileGLDrawer() {
		canvas.addGLEventListener(this);
	}

	public void init(GLAutoDrawable drawable) {
		System.err.println("init");
        // Use debug pipeline
        // drawable.setGL(new DebugGL(drawable.getGL()));

        GL gl = drawable.getGL();
        System.err.println("INIT GL IS: " + gl.getClass().getName());

        // Enable VSync
        gl.setSwapInterval(1);

        // Setup the drawing area and shading mode
        gl.glClearColor(0.0f, 0.0f, 1.0f, 0.0f);
        gl.glShadeModel(GL.GL_SMOOTH); // try setting this to GL_FLAT and see what happens.

		//gl.glEnable(gl.GL_DEPTH_TEST);
		//gl.glDepthFunc(gl.GL_LEQUAL);
	}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		System.err.println("reshape");
        GL gl = drawable.getGL();
        GLU glu = new GLU();

        if (height <= 0) { // avoid a divide by zero error!

            height = 1;
		}
        final float h = (float) width / (float) height;
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        //glu.gluPerspective(45.0f, h, 1.0, 1000.0);
		glu.gluOrtho2D(-10.0, 10.0, 10.0, -10.0);
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();
	}

	public void display(GLAutoDrawable drawable) {
		GL gl = drawable.getGL();
		GLU glu = new GLU();

		// Clear the drawing area
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        // Reset the current matrix to the "identity"
        gl.glLoadIdentity();
		gl.glColor3f(1f,0f,0f);
		gl.glBegin(gl.GL_QUADS);
			gl.glVertex2f(-1f, 1f);
			gl.glVertex2f(1f,1f);
			gl.glVertex2f(1f,-1f);
			gl.glVertex2f(-1f, -1f);
        gl.glFlush();
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

	public Canvas getCanvas(){
		return canvas;
	}

	public void paintSandpileGraph(SandpileGraph graph, List<float[]> vertexLocations, SandpileConfiguration config){
		this.graph = graph;
		this.vertexLocations = vertexLocations;
		this.config = config;
		canvas.display();
	}
}
