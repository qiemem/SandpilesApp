/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.headb.sandpile;

import java.util.Iterator;

/**
 *
 * @author headb
 */
public abstract class EdgeList implements Iterable<Edge> {

    /**
     * A subclass of Edge that points to a certain location in edgeData. The
     * GeneralEdgeList iterator uses these so that it can keep the list of edges in the
     * more efficient form of the Int2dArrayList rather a list of Edges without
     * having to convert the information for each edge into an Edge. Note that this also
     * allows write access to the edges.
     */
    protected class MyEdge extends Edge {

        int edgeNum;

        public MyEdge(int edgeNum) {
            this.edgeNum = edgeNum;
        }

        @Override
        public int source() {
            return sourceQuick(edgeNum);
        }

        @Override
        public void setSource(int s) {
            setSourceQuick(edgeNum, s);
        }

        @Override
        public int dest() {
            return destQuick(edgeNum);
        }

        @Override
        public void setDest(int d) {
            setDestQuick(edgeNum, d);
        }

        @Override
        public int wt() {
            return wtQuick(edgeNum);
        }

        @Override
        public void setWt(int w) {
            setWtQuick(edgeNum, w);
        }
    }

    abstract public int size();

    public Edge getEdge(int i) {
        if (i < size()) {
            return new MyEdge(i);
        } else {
            throw new IndexOutOfBoundsException("Size: " + size() + ". Edge requested: " + i);
        }
    }

    protected void sizeCheck(int i) {
        assert i < size() :
                new IndexOutOfBoundsException("Size: " + size() + ". Edge requested: " + i);
    }

    public int source(int i) {
        sizeCheck(i);
        return sourceQuick(i);
    }

    abstract public int sourceQuick(int i);

    public void setSource(int i, int s) {
        sizeCheck(i);
        setSourceQuick(i, s);
    }

    abstract public void setSourceQuick(int i, int s);

    public int dest(int i) {
        sizeCheck(i);
        return destQuick(i);
    }

    abstract public int destQuick(int i);

    public void setDest(int i, int d) {
        sizeCheck(i);
        setDestQuick(i, d);
    }

    abstract public void setDestQuick(int i, int d);

    public int wt(int i) {
        sizeCheck(i);
        return wtQuick(i);
    }

    abstract public int wtQuick(int i);

    public void setWt(int i, int w) {
        sizeCheck(i);
        setWtQuick(i, w);
    }

    abstract public void setWtQuick(int i, int w);

    abstract public void add(int s, int d, int w);

    public void add(Edge e) {
        add(e.source(), e.dest(), e.wt());
    }

    public int find(Edge e) {
        int i = 0;
        for (Edge f : this) {
            if (f.equals(e)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public boolean remove(Edge e) {
        int i = find(e);
        if (i == -1) {
            return false;
        }
        remove(i);
        return true;
    }

    abstract public void remove(int i);

    public Iterator<Edge> iterator() {
        final EdgeList me = this;
        return new Iterator<Edge>() {

            private int i = -1;

            public boolean hasNext() {
                return i + 1 < size();
            }

            public Edge next() {
                i++;
                return getEdge(i);
            }

            public void remove() {
                me.remove(i);
                i--;
            }
        };
    }
}
