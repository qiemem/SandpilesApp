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
import gnu.trove.TFloatArrayList;

/**
 *
 * @author headb
 */
public class Float2dArrayList extends TFloatArrayList{
	static private final long serialVersionUID = -21312181081L;
	private int cols;

	public Float2dArrayList(){
		super();
		cols = 1;
	}

	public Float2dArrayList(int cols){
		super();
		this.cols = cols;
	}

	public Float2dArrayList(int rows, int cols){
		super(new float[rows*cols]);
		this.cols = cols;
	}
	public Float2dArrayList(Float2dArrayList other){
		super(other.toNativeArray());
		cols = other.cols;
	}
	public Float2dArrayList(float[] array, int cols){
		super(array);
		this.cols = cols;
	}
	public float get(int r, int c){
		return super.get(r*cols + c);
	}
	public float getQuick(int r, int c){
		return super.getQuick(r*cols+c);
	}
	public void set(int r, int c, float val){
		super.set(r*cols+c, val);
	}
	public void setQuick(int r, int c, float val){
		super.setQuick(r*cols+c, val);
	}
	public void insertRow(int r, float... row){
		super.insert(r*cols, row);
	}
	public void setRow(int r, float... row){
		super.set(r*cols, row);
	}
	public int addRow(){
		for(int i=0; i<cols; i++)
			super.add(0f);
		return rows()-1;
	}
	public int addRow(float val){
		for(int i=0; i<cols; i++)
			super.add(val);
		return rows()-1;
	}
	public int addRow(float... row){
		if(row.length==cols){
			super.add(row);
			return rows()-1;
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

	public boolean equals(Float2dArrayList other){
		if(this==other)
			return true;
		if(this.rows()!=other.rows() || this.cols()!=other.cols())
			return false;
		for(int i=0; i<this.size(); i++){
			if(this.get(i)!=other.get(i))
				return false;
		}
		return true;
	}

}
