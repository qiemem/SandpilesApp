/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.headb;
import gnu.trove.TIntArrayList;
import java.util.Arrays;

/**
 *
 * @author headb
 */
public class IntCyclicBuffer {

	private int nextBegin = 0, nextLength = 0, curIndex = -1;
	private int maxSize;
	private int[] buffer;
	
	public IntCyclicBuffer(int maxSize){
		this.maxSize = maxSize;
		buffer = new int[maxSize];
	}

	public int maxSize(){
		return maxSize;
	}

	public int nextItem() {
		if(hasNextItem()){
			return nextItemUnsafe();
		} else {
			throw new IndexOutOfBoundsException("Went beyond the end of the current cycle.");
		}
	}

	public int nextItemUnsafe() {
		curIndex = nextIndex();
		return buffer[curIndex];
	}

	public boolean hasNextItem() {
		return nextIndex()!=nextBegin;
	}

	public int remaining(){
		return (nextBegin - nextIndex() + maxSize())%maxSize();
	}

	private int nextIndex() {
		return (curIndex+1)%maxSize();
	}

	public boolean hasNextCycle() {
		return nextCycleLength()>0;
	}

	public void add(int val){
		int i = (nextBegin+nextCycleLength())%maxSize();
		if(hasNextItem() && i==nextIndex()){
			throw new IndexOutOfBoundsException("Tried to add at: "+i+" though nextIndex() is "+nextIndex());
		}else {
			buffer[i] = val;
			nextLength++;
		}
	}

	public void addUnsafe(int val){
		buffer[(nextBegin+nextCycleLength())%maxSize()]=val;
		nextLength++;
	}

	public void addAll(TIntArrayList seq){
		for(int i = 0; i<seq.size(); i++){
			add(seq.get(i));
		}
	}

	public void goToNextCycle(){
		curIndex = nextBegin-1;
		nextBegin = nextBegin+nextCycleLength();
		nextLength = 0;
	}

	public int nextCycleLength(){
		return nextLength;
	}
}
