/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.headb;

/**
 *
 * @author headb
 */
public class EdgeOffsetList {
	Int2dArrayList edgeOffsetData;
	public int destOffset(int i) {
		return edgeOffsetData.get(i,1);
	}
	public int destOffsetQuick(int i) {
		return edgeOffsetData.getQuick(i,1);
	}
	public void setDestOffset(int i, int d){
		edgeOffsetData.set(i, 1, d);
	}
	public int wt(int i){
		return edgeOffsetData.get(i, 2);
	}
	public int wtQuick(int i){
		return edgeOffsetData.getQuick(i, 2);
	}
	public void setWt(int i, int w){
		edgeOffsetData.set(i, 2, w);
	}
	public int size(){
		return edgeOffsetData.rows();
	}
	public void addEdge(int destOffset, int wt) {
		edgeOffsetData.addRow(destOffset, wt);
	}
	public int wtForOffset(int offset){
		for(int i=0; i<size(); i++){
			if(destOffsetQuick(i)==offset){
				return wtQuick(i);
			}
		}
		return -1;
	}
	public boolean equals(EdgeOffsetList that){
		if(this.size()!=that.size())
			return false;
		for(int i=0; i<size(); i++){
			if(wtQuick(i)!=that.wtForOffset(destOffsetQuick(i))){
				return false;
			}
		}
		return true;
	}
}
