/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.headb;

import java.awt.Canvas;
import java.util.List;

/**
 *
 * @author headb
 */
public interface SandpileDrawer{
	public enum ColorMode {
		NUM_OF_GRAINS,
		STABILITY,
		FIRINGS
	}
	public Canvas getCanvas();
	public void setColorMode(ColorMode cm);
	public ColorMode getColorMode();
	public void paintSandpileGraph(SandpileGraph graph, List<float[]> vertexLocations, SandpileConfiguration config, List<Integer> firings, int selectedVertex);
	public float[] transformCanvasCoords(int x, int y);
}
