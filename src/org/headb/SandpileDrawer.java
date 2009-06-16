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
	public Canvas getCanvas();
	public void paintSandpileGraph(SandpileGraph graph, List<float[]> vertexLocations, SandpileConfiguration config);
}
