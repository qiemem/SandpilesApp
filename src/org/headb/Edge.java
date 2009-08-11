/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.headb;

/**
 *
 * @author headb
 */
public class Edge {
	private int source;
	private int dest;
	private int wt;

	protected Edge(){
		
	}
	public Edge(int s, int d, int w){
		source = s;
		dest = d;
		wt = w;
	}
	public Edge(Edge other){
		setSource(other.source());
		setDest(other.dest());
		setWt(other.wt());
	}
	public int source(){
		return source;
	}
	public void setSource(int s){
		source = s;
	}
	public int dest(){
		return dest;
	}
	public void setDest(int d){
		dest = d;
	}
	public int wt(){
		return wt;
	}
	public void setWt(int w){
		wt=w;
	}

	public boolean equals(Edge other){
		return (source() == other.source()) && (dest() == other.dest()) && (wt() == other.wt());
	}
}
