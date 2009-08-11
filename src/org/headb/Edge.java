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
