/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.headb;

/**
 *
 * @author headb
 */
public interface SandpileChangeListener {
	public void onGraphChange(SandpileGraph graph);
	public void onConfigChange(SandpileConfiguration config);
}
