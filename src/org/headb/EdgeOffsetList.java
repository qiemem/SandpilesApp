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
