/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.headb;
import java.util.Iterator;

/**
 *
 * @author headb
 */
public class EdgeList implements Iterable<Edge>{
	private Int2dArrayList edgeData;

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
