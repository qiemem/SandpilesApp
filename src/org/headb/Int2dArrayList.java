/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.headb;
import gnu.trove.TIntArrayList;

/**
 *
 * @author headb
 */
public class Int2dArrayList extends TIntArrayList{
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
	public int get(int r, int c){
		return super.get(r*cols + c);
	}
	public int getQuick(int r, int c){
		return super.getQuick(r*cols+c);
	}
	public void set(int r, int c, int val){
		super.set(r*cols+c, val);
	}
	public void setQuick(int r, int c, int val){
		super.setQuick(r*cols+c, val);
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
