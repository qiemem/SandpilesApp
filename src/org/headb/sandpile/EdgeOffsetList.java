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
 * This class is under development and is not currently used. Represents a list
 * of edges by indicating the difference in index between the source and
 * destination vertex. This allows many vertices to share the same
 * EdgeOffsetList rather than each having their own EdgeList.
 * @author Bryan Head
 */
public class EdgeOffsetList {

    private Int2dArrayList edgeOffsetData;
    private int degree;

    private class PersonalizedEdgeList extends EdgeList {

        private int vert;
        private EdgeOffsetList owner;

        private UnsupportedOperationException manipulationException() {
            return new UnsupportedOperationException("Can't alter the edges of a vertex in an EdgeStructureBlock through an intermediary EdgeList");
        }

        public PersonalizedEdgeList(EdgeOffsetList owner, int vert) {
            this.owner = owner;
            this.vert = vert;
        }

        public int size() {
            return owner.size();
        }

        public int source(int i) {
            return vert;
        }

        public int sourceQuick(int i) {
            return vert;
        }

        public int destQuick(int i) {
            return vert + destOffsetQuick(i);
        }

        public int wtQuick(int i) {
            return owner.wtQuick(i);
        }

        public void remove(int i) {
            throw manipulationException();
        }

        public void add(int s, int d, int w) {
            throw manipulationException();
        }

        public void setWtQuick(int i, int w) {
            throw manipulationException();
        }

        public void setDestQuick(int i, int d) {
            throw manipulationException();
        }

        public void setSourceQuick(int i, int s) {
            throw manipulationException();
        }
    }

    public EdgeOffsetList() {
        degree = 0;
        edgeOffsetData = new Int2dArrayList(2);
    }

    public EdgeOffsetList(EdgeOffsetList other) {
        this.edgeOffsetData = new Int2dArrayList(other.edgeOffsetData);
        degree = other.degree;
    }

    public int destOffset(int i) {
        return edgeOffsetData.get(i, 0);
    }

    public int destOffsetQuick(int i) {
        return edgeOffsetData.getQuick(i, 0);
    }

    public void setDestOffset(int i, int d) {
        edgeOffsetData.set(i, 0, d);
    }

    public int wt(int i) {
        return edgeOffsetData.get(i, 1);
    }

    public int wtQuick(int i) {
        return edgeOffsetData.getQuick(i, 1);
    }

    public void setWt(int i, int w) {
        edgeOffsetData.set(i, 1, w);
        recalcDegree();
    }

    public int size() {
        return edgeOffsetData.rows();
    }

    public void addEdge(int destOffset, int wt) {
        if (wt <= 0) {
            return;
        }
        edgeOffsetData.addRow(destOffset, wt);
        degree += wt;
    }

    public int wtForOffset(int offset) {
        int i = find(offset);
        if (i < 0) {
            return 0;
        }
        return wtQuick(i);
    }

    public int find(int offset) {
        for (int i = 0; i < size(); i++) {
            if (destOffsetQuick(i) == offset) {
                return i;
            }
        }
        return -1;
    }

    public void remove(int i) {
        edgeOffsetData.removeRow(i);
        recalcDegree();
    }

    public int degree() {
        return degree;
    }

    public boolean equals(EdgeOffsetList that) {
        if (this.size() != that.size() || this.degree() != that.degree()) {
            return false;
        }
        if (this.edgeOffsetData.equals(that.edgeOffsetData)) {
            return true;
        }
        for (int i = 0; i < size(); i++) {
            if (wtQuick(i) != that.wtForOffset(destOffsetQuick(i))) {
                return false;
            }
        }
        return true;
    }

    private void recalcDegree() {
        degree = 0;
        for (int i = 0; i < size(); i++) {
            degree += wtQuick(i);
        }
    }

    public EdgeList getOutgoingEdges(int vert) {
        return new PersonalizedEdgeList(this, vert);
    }
}
