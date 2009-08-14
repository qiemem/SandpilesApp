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
 * Represents a list of arrays of the same size. This is far more efficient than
 * ArrayList<ArrayList<Integer>> for instance or even int[][].
 * @author Bryan Head
 */
public class Int2dArrayList extends TIntArrayList{
	private static final int DEFAULT_ROW_CAPACITY = 4;
	private int cols;
//	private Int2dArrayList me = this;
//
//	private class Row{
//		int row;
//		public Row(int r){
//			row = r;
//		}
//		public int get(int c){
//			if(c<me.cols())
//				return me.getQuick(row, c);
//			else
//				throw new IndexOutOfBoundsException();
//		}
//		public int getQuick(int c){
//			return me.getQuick(row, c);
//		}
//		public void set(int c, int val){
//			if(c<me.cols())
//				me.setQuick(row, c, val);
//			else
//				throw new IndexOutOfBoundsException();
//		}
//		public void setQuick(int c, int val){
//			me.setQuick(row, c, val);
//		}
//	}

	public Int2dArrayList(int cols){
		super(cols*DEFAULT_ROW_CAPACITY);
		this.cols = cols;
	}
	public Int2dArrayList(int rows, int cols){
		super(new int[rows*cols]);
		this.cols = cols;

	}
	public Int2dArrayList(Int2dArrayList other){
		super(other.toNativeArray());
		cols = other.cols;
	}
	public Int2dArrayList(int[] array, int cols){
		super(array);
		this.cols = cols;
	}
	private boolean checkIndices(int r, int c){
		return c<cols() && r<rows();
	}
	public int get(int r, int c){
		if(!checkIndices(r,c))
			throw new IndexOutOfBoundsException();
		return super.get(r*cols + c);
	}
	public int getQuick(int r, int c){
		return _data[r*cols+c];
	}
	public void set(int r, int c, int val){
		if(!checkIndices(r,c))
			throw new IndexOutOfBoundsException();
		super.set(r*cols+c, val);
	}
	public void setQuick(int r, int c, int val){
		_data[r*cols + c] = val;
	}
	
	public int addRow(){
		for(int i=0; i<cols; i++)
			super.add(0);
		return rows()-1;
	}
	public int addRow(int val){
		for(int i=0; i<cols; i++)
			super.add(val);
		return rows()-1;
	}
	public int addRow(int... row){
		if(row.length==cols){
			super.add(row);
			return rows() - 1;
		}else
			throw(new IndexOutOfBoundsException("Tried to add a row of the wrong length. Row length was "+row.length+". It should have been "+cols+"."));
	}
	public int rows(){
		return size()/cols;
	}
	public int cols(){
		return cols;
	}

	public void removeRow(int r){
		super.remove(r*cols, cols);
	}
//
//	public boolean equals(Int2dArrayList other){
//		if(this==other)
//			return true;
//		if(this.rows()!=other.rows() || this.cols()!=other.cols())
//			return false;
//		for(int i=0; i<this.size(); i++){
//			if(this.get(i)!=other.get(i))
//				return false;
//		}
//		return true;
//	}
}
