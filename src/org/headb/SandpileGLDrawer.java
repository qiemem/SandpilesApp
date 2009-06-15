/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.headb;

import javax.media.opengl.*;
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

	}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

	}

	public void display(GLAutoDrawable drawable) {

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
