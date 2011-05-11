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

/**
 * A list of edges that all have the same source.
 * @author Bryan Head
 */
public class SingleSourceEdgeList extends GeneralEdgeList {

    int source = -1;

    public SingleSourceEdgeList(int source) {
        edgeData = new Int2dArrayList(2);
        this.source = source;
    }

    public SingleSourceEdgeList(EdgeList other, int source) {
        this.source = source;
        edgeData = new Int2dArrayList(2);
        for (int i = 0; i < other.size(); i++) {
            this.add(source, other.destQuick(i), other.wtQuick(i));
        }
    }

    public SingleSourceEdgeList(SingleSourceEdgeList other) {
        source = other.source;
        edgeData = new Int2dArrayList(other.edgeData);
    }

    @Override
    public int source(int i) {
        return source;
    }

    public int source() {
        return source;
    }

    @Override
    public int sourceQuick(int i) {
        return source;
    }

    @Override
    public void setSource(int i, int s) {
        source = s;
    }

    @Override
    public void setSourceQuick(int i, int s) {
        source = s;
    }

    public void setSource(int s) {
        source = s;
    }

    @Override
    public int dest(int i) {
        return edgeData.get(i, 0);
    }

    @Override
    public int destQuick(int i) {
        return edgeData.getQuick(i, 0);
    }

    @Override
    public void setDest(int i, int d) {
        edgeData.set(i, 0, d);
    }

    @Override
    public void setDestQuick(int i, int d) {
        edgeData.setQuick(i, 0, d);
    }

    @Override
    public int wt(int i) {
        return edgeData.get(i, 1);
    }

    @Override
    public int wtQuick(int i) {
        return edgeData.getQuick(i, 1);
    }

    @Override
    public void setWt(int i, int w) {
        edgeData.set(i, 1, w);
    }

    @Override
    public void setWtQuick(int i, int w) {
        edgeData.setQuick(i, 1, w);
    }

    @Override
    public void add(int s, int d, int w) {
        if (s == source) {
            add(d, w);
        } else {
            throw new IllegalArgumentException("Tried to add an edge with the " +
                    "wrong source to a SingleSourceEdge. Edge's source was " + s + " while it needed to be " + source + ".");
        }
    }

    public void add(int d, int w) {
        edgeData.addRow(d, w);
    }

    @Override
    public void add(Edge e) {
        if (e.source() == source) {
            edgeData.addRow(e.dest(), e.wt());
        } else {
            throw new IllegalArgumentException("Tried to add an edge with the wrong source to a SingleSourceEdge. Edge's source was " + e.source() + " while it needed to be " + source + ".");
        }
    }

    public EdgeOffsetList getEdgeOffsetList() {
        EdgeOffsetList offsetList = new EdgeOffsetList();
        for (int i = 0; i < this.size(); i++) {
            offsetList.addEdge(destQuick(i) - source(), wtQuick(i));
        }
        return offsetList;
    }
}
