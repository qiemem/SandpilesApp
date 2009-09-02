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
import gnu.trove.TIntArrayList;

/**
 * This class is under development and is not currently used.
 * This class contains a group of vertices that have a common edge structure in
 * that they each have identical EdgeOffsetLists, which is also contained.
 * @author Bryan Head
 */
public class EdgeStructureBlock {
	TIntArrayList vertices;
	EdgeOffsetList edgeInfo;

	private class PersonalizedEdgeList extends EdgeList{
		private int vert;

		private UnsupportedOperationException manipulationException() {
			return new UnsupportedOperationException("Can't alter the edges of a vertex in an EdgeStructureBlock through an intermediary EdgeList");
		}
		public PersonalizedEdgeList(int vert){
			this.vert = vert;
		}
		public int size() {
			return edgeInfo.size();
		}
		public int source(int i){
			return vert;
		}
		public int sourceQuick(int i){
			return vert;
		}
		public int destQuick(int i){
			return vert + edgeInfo.destOffsetQuick(i);
		}
		public int wtQuick(int i){
			return edgeInfo.wtQuick(i);
		}
		public void remove(int i){
			throw manipulationException();
		}
		public void add(int s, int d, int w){
			throw manipulationException();
		}
		public void setWtQuick(int i, int w){
			throw manipulationException();
		}
		public void setDestQuick(int i, int d){
			throw manipulationException();
		}
		public void setSourceQuick(int i, int s){
			throw manipulationException();
		}
	}

	public EdgeStructureBlock(EdgeOffsetList edgeInfo){
		this.edgeInfo = edgeInfo;
		this.vertices = new TIntArrayList();
	}

	public EdgeStructureBlock(EdgeStructureBlock other){
		this.edgeInfo = new EdgeOffsetList(other.edgeInfo);
		this.vertices = new TIntArrayList(other.vertices.toNativeArray());
	}

	public boolean tryAddVertex(int v, EdgeOffsetList edgeInfo){
		if(this.edgeInfo.equals(edgeInfo)){
			vertices.add(v);
			return true;
		}
		return false;
	}

	public boolean removeVertex(int v) {
		int i = vertices.indexOf(v);
		if(i>=0){
			vertices.remove(i);
			return true;
		}
		return false;
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
		return edgeInfo.destOffset(edgeIndex) + getVert(vertIndex);
	}

	public int getDestVertQuick(int vertIndex, int edgeIndex){
		return edgeInfo.destOffsetQuick(edgeIndex) + getVert(vertIndex);
	}

	public int getWt(int edgeIndex) {
		return edgeInfo.wt(edgeIndex);
	}

	public int getWtQuick(int edgeIndex){
		return edgeInfo.wtQuick(edgeIndex);
	}

	public int degree(){
		return edgeInfo.degree();
	}

	public EdgeOffsetList getEdgeOffsetInfo(){
		return edgeInfo;
	}

	public EdgeList getOutgoingEdgesQuick(int vert){
		return new PersonalizedEdgeList(vert);
	}
	public EdgeList getOutgoingEdges(int vert){
		if(vertices.contains(vert))
			return new PersonalizedEdgeList(vert);
		else
			throw new IndexOutOfBoundsException("Tried to get the outgoing edges of a vertex not in this block.");
	}
}
