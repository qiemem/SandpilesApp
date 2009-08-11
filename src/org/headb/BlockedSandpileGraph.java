/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.headb;
import java.util.ArrayList;
import gnu.trove.TIntArrayList;
/**
 *
 * @author headb
 */
public class BlockedSandpileGraph {
	ArrayList<EdgeStructureBlock> blocks;
	TIntArrayList degrees;
	int numVertices;

	public void addVertex(Int2dArrayList edgeInfo) {
		numVertices++;
		for (EdgeStructureBlock b : blocks){
			if(b.tryAddVertex(numVertices()-1, edgeInfo))
				return;
		}
	}

	public int numVertices(){
		return numVertices;
	}
}
