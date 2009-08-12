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
import java.util.Iterator;

/**
 * Represents an adjacency list. Note that using this class over something like
 * ArrayList<Edge> will usually be better.
 * @author Bryan Head
 */
public class EdgeList implements Iterable<Edge>{
	private Int2dArrayList edgeData;

	/**
	 * A subclass of Edge that points to a certain lovation in edgeData. The
	 * EdgeList iterator uses these so that it can keep the list of edges in the
	 * more efficient form of the Int2dArrayList rather a list of Edges without
	 * having to the information for each edge into an Edge. Note that this also
	 * allows write access to the edges.
	 */
	private class MyEdge extends Edge{
		int edgeNum;
		public MyEdge(int edgeNum){
			this.edgeNum = edgeNum;
		}
		@Override public int source(){
			return edgeData.getQuick(edgeNum,0);
		}
		@Override public void setSource(int s) {
			edgeData.setQuick(edgeNum, 0, s);
		}
		@Override public int dest() {
			return edgeData.getQuick(edgeNum, 1);
		}
		@Override public void setDest(int d) {
			edgeData.setQuick(edgeNum, 1, d);
		}
		@Override public int wt() {
			return edgeData.getQuick(edgeNum, 2);
		}
		@Override public void setWt(int w) {
			edgeData.setQuick(edgeNum, 2, w);
		}
	}

	public EdgeList(){
		edgeData = new Int2dArrayList(3);
	}

	public EdgeList(EdgeList other){
		this.edgeData = new Int2dArrayList(other.edgeData);
	}

	public int size(){
		return edgeData.rows();
	}

	public Edge getEdge(int i){
		if(i<size())
			return new MyEdge(i);
		else
			throw new IndexOutOfBoundsException("Size: "+size()+". Edge requested: "+i);
	}

	public int source(int i){
		return edgeData.get(i, 0);
	}
	public int sourceQuick(int i){
		return edgeData.getQuick(i, 0);
	}
	public void setSource(int i, int s){
		edgeData.set(i, 0, s);
	}
	public int dest(int i){
		return edgeData.get(i,1);
	}
	public int destQuick(int i){
		return edgeData.getQuick(i,1);
	}
	public void setDest(int i, int d){
		edgeData.set(i, 1, d);
	}
	public int wt(int i){
		return edgeData.get(i, 2);
	}
	public int wtQuick(int i){
		return edgeData.getQuick(i,2);
	}
	public void setWt(int i, int w){
		edgeData.set(i, 2, w);
	}

	public Edge get(int i){
		return new MyEdge(i);
	}

	public void add(int s, int d, int w){
		edgeData.add(s);
		edgeData.add(d);
		edgeData.add(w);
	}

	public void add(Edge e){
		edgeData.add(e.source());
		edgeData.add(e.dest());
		edgeData.add(e.wt());
	}

	public int find(Edge e){
		int i=0;
		for(Edge f : this){
			if(f.equals(e)){
				return i;
			}
		}
		return -1;
	}

	public boolean remove(Edge e){
		int i = find(e);
		if(i==-1)
			return false;
		edgeData.removeRow(find(e));
		return true;
	}

	/**
	 * Allows one to iterate over the EdgeList using the Edge class.
	 * Although this method is provided for convenience, it is faster to do
	 * manual iteration such as:
	 * for(int i = 0; i < edgeList.size(); i++){
	 *     edgeList.destQuick(i);
	 *     // or even
	 *     edgeList.dest(i);
	 * }
	 * Using the iterator actually creates new objects; manual iteration does
	 * not.
	 * @return
	 */
	public Iterator<Edge> iterator(){
		return new Iterator<Edge>() {
			private int i = -1;
			public boolean hasNext() {
				return i+1<size();
			}

			public Edge next() {
				i++;
				return getEdge(i);
			}

			public void remove() {
				edgeData.removeRow(i);
				i--;
			}
		};
	}
}
