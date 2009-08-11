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
