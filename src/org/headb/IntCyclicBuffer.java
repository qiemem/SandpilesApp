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
