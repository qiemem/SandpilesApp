/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.headb;
import gnu.trove.TIntArrayList;

/**
 *
 * @author headb
 */
public class EdgeStructureBlock {
	TIntArrayList vertices;
	EdgeOffsetList edgeInfo;

	public EdgeStructureBlock(EdgeOffsetList edgeInfo){
		this.edgeInfo = edgeInfo;
	}

	public boolean tryAddVertex(int v, Int2dArrayList edgeInfo){
		if(this.edgeInfo.equals(edgeInfo)){
			vertices.add(v);
			return true;
		}
		return false;
	}

	public void removeVertex(int v) {
		vertices.remove(v);
	}

	public int numEdges(){
		return edgeInfo.size();
	}

	public int numVertices(){
		return vertices.size();
	}

	public int getVert(int vertIndex){
		return vertices.get(vertIndex);
	}

	public int getVertQuick(int vertIndex){
		return vertices.getQuick(vertIndex);
	}

	public int getDestVert(int vertIndex, int edgeIndex){
		return edgeInfo.destOffset(edgeIndex) + vertices.get(vertIndex);
	}

	public int getDestVertQuick(int vertIndex, int edgeIndex){
		return edgeInfo.destOffsetQuick(edgeIndex) + vertices.getQuick(vertIndex);
	}

	public int getWt(int edgeIndex) {
		return edgeInfo.wt(edgeIndex);
	}

	public int getWtQuick(int edgeIndex){
		return edgeInfo.wtQuick(edgeIndex);
	}

	public EdgeOffsetList getEdgeOffsetInfo(){
		return edgeInfo;
	}
}
