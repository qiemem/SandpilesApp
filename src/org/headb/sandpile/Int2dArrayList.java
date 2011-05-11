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
package org.headb.sandpile;

import gnu.trove.list.array.TIntArrayList;
import java.io.Serializable;

/**
 * Represents a list of arrays of the same size. This is far more efficient than
 * ArrayList<ArrayList<Int>> for instance or even int[][].
 * @author Bryan Head
 */
public class Int2dArrayList implements Serializable {

    static private final long serialVersionUID = -21312181081L;
    private int cols;
    private TIntArrayList data;

    public Int2dArrayList() {
        data = new TIntArrayList();
        cols = 1;
    }

    public Int2dArrayList(int cols) {
        data = new TIntArrayList();
        this.cols = cols;
    }

    public Int2dArrayList(int rows, int cols) {
        data = new TIntArrayList(new int[rows * cols]);
        this.cols = cols;
    }

    public Int2dArrayList(Int2dArrayList other) {
        data = new TIntArrayList(other.toArray());
        cols = other.cols;
    }

    public Int2dArrayList(Int2dArrayList other, int cols) {
        data = new TIntArrayList(other.toArray());
        if (other.data.size() % cols != 0) {
            throw (new IndexOutOfBoundsException("Incompatible number of columns," + cols + ", to convert Int2dArrayList of size " + other.data.size() + "."));
        }
        this.cols = cols;
    }

    public Int2dArrayList(int[] array, int cols) {
        data = new TIntArrayList(array);
        this.cols = cols;
    }

    public int[] toArray() {
        return data.toArray();
    }

    public int get(int r, int c) {
        return data.get(r * cols + c);
    }

    public int getQuick(int r, int c) {
        return data.getQuick(r * cols + c);
    }

    public void set(int r, int c, int val) {
        data.set(r * cols + c, val);
    }

    public void setQuick(int r, int c, int val) {
        data.setQuick(r * cols + c, val);
    }

    public void insertRow(int r, int... row) {
        data.insert(r * cols, row);
    }

    public void setRow(int r, int... row) {
        data.set(r * cols, row);
    }

    public int addRow() {
        for (int i = 0; i < cols; i++) {
            data.add(0);
        }
        return rows() - 1;
    }

    public int addRow(int val) {
        for (int i = 0; i < cols; i++) {
            data.add(val);
        }
        return rows() - 1;
    }

    public int addRow(int... row) {
        if (row.length == cols) {
            data.add(row);
            return rows() - 1;
        } else {
            throw (new IndexOutOfBoundsException("Tried to add a row of the wrong length. Row length was " + row.length + ". It should have been " + cols + "."));
        }
    }

    public int rows() {
        return data.size() / cols;
    }

    public int cols() {
        return cols;
    }

    public void removeRow(int r) {
        data.remove(r * cols, cols);
    }

    public void clear() {
        data.clear();
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public boolean equals(Int2dArrayList other) {
        return this.cols() == other.cols() && this.data.equals(other.data);
    }
}
