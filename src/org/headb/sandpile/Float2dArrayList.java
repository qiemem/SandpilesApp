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

import gnu.trove.list.array.TFloatArrayList;
import java.io.Serializable;

/**
 * Represents a list of arrays of the same size. This is far more efficient than
 * ArrayList<ArrayList<Float>> for instance or even float[][].
 * @author Bryan Head
 */
public class Float2dArrayList implements Serializable {

    static private final long serialVersionUID = -21312181081L;
    private int cols;
    private TFloatArrayList data;

    public Float2dArrayList() {
        data = new TFloatArrayList();
        cols = 1;
    }

    public Float2dArrayList(int cols) {
        data = new TFloatArrayList();
        this.cols = cols;
    }

    public Float2dArrayList(int rows, int cols) {
        data = new TFloatArrayList(new float[rows * cols]);
        this.cols = cols;
    }

    public Float2dArrayList(Float2dArrayList other) {
        data = new TFloatArrayList(other.toArray());
        cols = other.cols;
    }

    public Float2dArrayList(Float2dArrayList other, int cols) {
        data = new TFloatArrayList(other.toArray());
        if (other.data.size() % cols != 0) {
            throw (new IndexOutOfBoundsException("Incompatible number of columns," + cols + ", to convert Float2dArrayList of size " + other.data.size() + "."));
        }
        this.cols = cols;
    }

    public Float2dArrayList(float[] array, int cols) {
        data = new TFloatArrayList(array);
        this.cols = cols;
    }

    public float[] toArray() {
        return data.toArray();
    }

    public float get(int r, int c) {
        return data.get(r * cols + c);
    }

    public float getQuick(int r, int c) {
        return data.getQuick(r * cols + c);
    }

    public void set(int r, int c, float val) {
        data.set(r * cols + c, val);
    }

    public void setQuick(int r, int c, float val) {
        data.setQuick(r * cols + c, val);
    }

    public void insertRow(int r, float... row) {
        data.insert(r * cols, row);
    }

    public void setRow(int r, float... row) {
        data.set(r * cols, row);
    }

    public int addRow() {
        for (int i = 0; i < cols; i++) {
            data.add(0f);
        }
        return rows() - 1;
    }

    public int addRow(float val) {
        for (int i = 0; i < cols; i++) {
            data.add(val);
        }
        return rows() - 1;
    }

    public int addRow(float... row) {
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

    public boolean equals(Float2dArrayList other) {
        return this.cols() == other.cols() && this.data.equals(other.data);
    }
}
