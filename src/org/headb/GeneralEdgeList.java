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
public class GeneralEdgeList extends EdgeList{
	protected Int2dArrayList edgeData;

	public GeneralEdgeList(){
		edgeData = new Int2dArrayList(3);
	}

	public GeneralEdgeList(GeneralEdgeList other){
		this.edgeData = new Int2dArrayList(other.edgeData);
	}

	public int size(){
		return edgeData.rows();
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
	public void setSourceQuick(int i, int s){
		edgeData.setQuick(i, 0, s);
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
	public void setDestQuick(int i, int d){
		edgeData.setQuick(i, 1, d);
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
	public void setWtQuick(int i, int w){
		edgeData.setQuick(i, 2, w);
	}
	public Edge get(int i){
		return new MyEdge(i);
	}

	public void add(int s, int d, int w){
		edgeData.addRow(s,d,w);
	}

	public void add(Edge e){
		edgeData.addRow(e.source(), e.dest(), e.wt());
	}

	public int find(Edge e){
		int i=0;
		for(Edge f : this){
			if(f.equals(e)){
				return i;
			}
			i++;
		}
		return -1;
	}

	public void remove(int i){
		edgeData.removeRow(i);
	}
}
