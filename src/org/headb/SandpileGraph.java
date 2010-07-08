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

import java.util.*;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntStack;

/**
 * Represents a weighted, directed graph with methods for updating a sandpile
 * configuration on that graph. Contains methods for calculating several
 * important configuration. The most efficient method of updating a config
 * repeatedly is to use an iterator returned by inPlaceParallelUpdater(config). Calling
 * the iterator's next() method will transform config into the next
 * configuration. This class is quite optimized for updating, and not as much
 * for adding and removing vertices, and doing non-iterating reads. However,
 * the performance should be more than sufficient for manual creation of graphs.
 * @author Bryan Head
 */
public class SandpileGraph {

    // Edges are represented by Edge arrays
    // where int[0] = source
    // int[1] = dest
    // int[2] = weight
    private ArrayList<EdgeOffsetList> offsetLists;
    private HashMap<EdgeOffsetList, Integer> vertexCounts;
    //private TIntArrayList degrees;
    private ArrayList<EdgeOffsetList> vertsToOffsetLists;
    private int[] degrees;

    /**
     * Creates a new, empty graph.
     */
    public SandpileGraph() {
        this.offsetLists = new ArrayList<EdgeOffsetList>();
        this.vertsToOffsetLists = new ArrayList<EdgeOffsetList>();
        this.vertexCounts = new HashMap<EdgeOffsetList, Integer>();
        degrees = new int[0];
    }

    public SandpileGraph(SandpileGraph graph) {
        this.offsetLists = new ArrayList<EdgeOffsetList>();
        this.vertsToOffsetLists = new ArrayList<EdgeOffsetList>();
        this.vertexCounts = new HashMap<EdgeOffsetList, Integer>();
        degrees = new int[graph.numVertices()];

        for (int v = 0; v < graph.numVertices(); v++) {
            addVertex();
            placeVertexWithOffsets(v, graph.getOffsetList(v));
            degrees[v] = graph.degreeQuick(v);
        }
    }

    private EdgeOffsetList getOffsetList(int vert) {
        return vertsToOffsetLists.get(vert);
    }

    /**
     * Returns the number of vertices.
     * @return An int of the number of vertices.
     */
    public int numVertices() {
        return vertsToOffsetLists.size();
    }

    /**
     * Retrieves the sum of the weights of the outgoing edges of the given
     * vertex.
     *
     * @param vert The index of the vertex.
     * @return The sum of the weights of the outgoing edges of the given vertex.
     */
    public int degree(int vert) {
        //return this.vertices.get(vert).degree();
        return getOffsetList(vert).degree();
    }

    /**
     * Retrieves the sum of the weights of the outgoing edges of the given
     * vertex. Does not check to see if the index is within range, thus
     * creating unpredictable behavior if it is not.
     *
     * @param vert The index of the vertex.
     * @return The sum of the weights of the outgoing edges of the given vertex.
     */
    public int degreeQuick(int vert) {
        return degrees[vert];
    }

    /**
     * Retrieves the list of edges which begin with the source vertex.
     */
    public final EdgeList getOutgoingEdges(int vert) {
        return vertsToOffsetLists.get(vert).getOutgoingEdges(vert);
    }

    private int getBlockSize(EdgeOffsetList offsetList) {
        return vertexCounts.get(offsetList);
    }

    private void incBlockSize(EdgeOffsetList offsetList) {
        vertexCounts.put(offsetList, getBlockSize(offsetList) + 1);
    }

    private void decBlockSize(EdgeOffsetList offsetList) {
        vertexCounts.put(offsetList, getBlockSize(offsetList) + 1);
    }

    private void addOffsetList(EdgeOffsetList offsetList) {
        offsetLists.add(offsetList);
        vertexCounts.put(offsetList, 0);
    }

    private boolean removeOffsetListIfEmpty(EdgeOffsetList offsetList) {
        if (getBlockSize(offsetList) == 0) {
            offsetLists.remove(offsetList);
            return true;
        }
        return false;
    }

    private void removeVertexFromOffsetList(int v) {
        EdgeOffsetList offsetList = getOffsetList(v);
        decBlockSize(offsetList);
        removeOffsetListIfEmpty(offsetList);
    }

    private EdgeOffsetList getMatchingOffsetList(EdgeOffsetList offsetList) {
        for (EdgeOffsetList ol : offsetLists) {
            if (ol.equals(offsetList)) {
                return ol;
            }
        }
        return null;
    }

    public void setOutgoingEdges(SingleSourceEdgeList edges) {
        int vert = edges.source();
        removeVertexFromOffsetList(vert);
        EdgeOffsetList offsetList = edges.getEdgeOffsetList();
        this.placeVertexWithOffsets(vert, offsetList);
    }

    public GeneralEdgeList getIncomingEdges(int vert) {
        GeneralEdgeList edges = new GeneralEdgeList();
        for (int v = 0; v < this.numVertices(); v++) {
            for (Edge e : getOutgoingEdges(v)) {
                if (e.dest() == vert) {
                    edges.add(e);
                }
            }
        }
        return edges;
    }

    /**
     * Determines if the given offset data currently resides in the graph. If so
     * it links the vertex to that data. Otherwise, it adds the offset data and
     * links the vertex to it.
     */
    private void placeVertexWithOffsets(int vert, EdgeOffsetList offsets) {
        EdgeOffsetList offsetList = this.getMatchingOffsetList(offsets);
        if (offsetList == null) {
            offsetList = new EdgeOffsetList(offsets);
            addOffsetList(offsetList);
        }
        //assert !offsetList.tryAddVertex(vert, offsets):
        //	new RuntimeException("This should be impossible");
        //offsetList.tryAddVertex(vert, offsets);
        incBlockSize(offsetList);

        vertsToOffsetLists.set(vert, offsetList);
        degrees[vert] = offsetList.degree();
    }

    public void addVertex() {
        vertsToOffsetLists.add(null);
        if (numVertices() > degrees.length) {
            int[] newDegrees = new int[numVertices() * 2];
            for (int i = 0; i < degrees.length; i++) {
                newDegrees[i] = degrees[i];
            }
            degrees = newDegrees;
        }
        placeVertexWithOffsets(this.numVertices() - 1, new EdgeOffsetList());

    }


    /**
     * Removes a vertex from the graph. Note that the index of all vertices
     * added after the vertex being deleted will be decreased by one.
     *
     * @param toDelete The index of the vertex to delete.
     */
    public void removeVertex(int toDelete) {
        // Filling in this method would simply be rewriting removeVertices using
        // a single index instead of a list. No reason to rewrite and this seems
        // plenty fast for now.
        TIntArrayList singleton = new TIntArrayList(1);
        singleton.add(toDelete);
        removeVertices(singleton);
    }

    /**
     * Removes the vertices in the list from the graph. Indices are adjusted
     * appropriately. Note that this means that the index of vertices will
     * change after using this function.
     *
     * @param vertices A list of the vertices to remove.
     */
    public void removeVertices(TIntArrayList vertices) {
        int[] translator = new int[numVertices()];
        //ArrayList<EdgeOffsetList> oldOffsetLists = new ArrayList<EdgeOffsetList>(offsetLists);
        ArrayList<EdgeOffsetList> oldVertsToOffsetLists = new ArrayList<EdgeOffsetList>(vertsToOffsetLists);
        boolean[] toRemove = new boolean[numVertices()];
        for (int i = 0; i < vertices.size(); i++) {
            int v = vertices.get(i);
            toRemove[v] = true;
        }
        this.removeAllVertices();
        int w = 0;
        for (int v = 0; v < oldVertsToOffsetLists.size(); v++) {
            if (!toRemove[v]) {
                translator[v] = w;
                w++;
            }
        }
        addVertices(w);
        for (int v = 0; v < oldVertsToOffsetLists.size(); v++) {
            if (toRemove[v]) {
                continue;
            }
            for (Edge e : oldVertsToOffsetLists.get(v).getOutgoingEdges(v)) {
                if (!toRemove[e.dest()]) {
                    addEdge(translator[e.source()], translator[e.dest()], e.wt());
                }
            }
        }
    }

    /**
     * Deletes the whole graph. Should run in constant time.
     */
    public void removeAllVertices() {
        this.offsetLists.clear();
        this.vertsToOffsetLists.clear();
    }

    /**
     * Adds the given number of vertices to the graph. Runs in linear time.
     */
    public void addVertices(int amount) {
        for (int i = 0; i < amount; i++) {
            this.addVertex();
        }
    }

    /**
     * Increases the weight of the edge from sourceVert to destVert by 1. If the
     * edge does not exist yet it will be created.
     * @param sourceVert The index of the source vertex.
     * @param destVert the index of the destination vertex.
     */
    public void addEdge(int sourceVert, int destVert) {
        //this.vertices.get(sourceVert).addOutgoingEdge(this.vertices.get(destVert));
        this.addEdge(sourceVert, destVert, 1);
    }

    public void addEdge(Edge e) {
        addEdge(e.source(), e.dest(), e.wt());
    }

    /**
     * Increases the weight of the edge from sourceVert to destVert by weight.
     * weight can be negative. If the weight of the resulting edge is negative
     * or zero, it is removed. If the edge does not already exist it will be
     * created
     * @param sourceVert The index of the source vertex.
     * @param destVert The index of the destination vertex.
     * @param weight The amount to increase the weight of the edge by.
     */
    public void addEdge(int sourceVert, int destVert, int weight) {
        if (sourceVert == destVert) {
            return;
        }
        EdgeOffsetList offsets = new EdgeOffsetList(this.getOffsetList(sourceVert));
        removeVertexFromOffsetList(sourceVert);

        int offset = destVert - sourceVert;
        int edgeIndex = offsets.find(offset);
        int originalWeight = offsets.wtForOffset(offset);
        int newWeight = Math.max(0, weight + originalWeight);
        if (originalWeight == 0 && newWeight > 0) {
            offsets.addEdge(offset, newWeight);
        } else if (newWeight == 0 && edgeIndex >= 0) {
            offsets.remove(edgeIndex);
        } else if (newWeight > 0) {
            offsets.setWt(edgeIndex, newWeight);
        }
        this.placeVertexWithOffsets(sourceVert, offsets);
    }

    /**
     * Decreases the weight of the edge from sourceVert to destVert by 1. If the
     * weight of the edge becomes zero or less, it is removed.
     * @param sourceVert The index of the source vertex.
     * @param destVert the index of the destination vertex.
     */
    public void removeEdge(int sourceVert, int destVert) {
        this.removeEdge(sourceVert, destVert, 1);
    }

    /**
     * Decreases the weight of the edge from sourceVert to destVert by weight.
     * weight can be negative. If the weight of the resulting edge is negative
     * or zero, it is removed.
     * This is equivalent to addEdge(sourceVert, destVert, -weight)
     * @param sourceVert The index of the source vertex.
     * @param destVert The index of the destination vertex.
     * @param weight The amount to decrease the weight of the edge by.
     */
    public void removeEdge(int sourceVert, int destVert, int weight) {
        this.addEdge(sourceVert, destVert, -weight);
    }

    /**
     * Returns true if the indicated vertex has no outgoing vertices, false otherwise.
     *
     * @param vertIndex The index of a vertex.
     */
    public boolean isSink(int vertIndex) {
        return degree(vertIndex) == 0;
    }

    public boolean isSinkQuick(int vertIndex) {
        return degreeQuick(vertIndex) == 0;
    }

    /**
     * Returns a list containing the indices of all nonsink vertices.
     * @return
     */
    public TIntArrayList getNonSinks() {
        TIntArrayList nonsinks = new TIntArrayList();
        for (int v = 0; v < numVertices(); v++) {
            if (!isSinkQuick(v)) {
                nonsinks.add(v);
            }
        }
        return nonsinks;
    }

    /**
     *
     * @return A list of the indices of each sink vertex.
     */
    public TIntArrayList getSinks() {
        TIntArrayList sinks = new TIntArrayList();
        for (int v = 0; v < numVertices(); v++) {
            if (isSink(v)) {
                sinks.add(v);
            }
        }
        return sinks;
    }

    public SandpileConfiguration fireVertices(SandpileConfiguration config, TIntArrayList verts) {
        SandpileConfiguration newConfig = new SandpileConfiguration(config);
        for (int i = 0; i < verts.size(); i++) {
            int v = verts.get(i);
            for (Edge e : getOutgoingEdges(v)) {
                newConfig.set(e.dest(), newConfig.get(e.dest()) + e.wt());
            }
            newConfig.set(v, newConfig.get(v) - degreeQuick(v));
        }
        return newConfig;
    }

    public SandpileConfiguration fireVerticesInPlace(SandpileConfiguration config, TIntArrayList verts) {
        for (int i = 0; i < verts.size(); i++) {
            int v = verts.get(i);
            for (Edge e : getOutgoingEdges(v)) {
                config.set(e.dest(), config.get(e.dest()) + e.wt());
            }
            config.set(v, config.get(v) - degreeQuick(v));
        }
        return config;
    }

    public void fireVertexInPlace(SandpileConfiguration config, int vert) {
        EdgeList edges = getOutgoingEdges(vert);
        int s = edges.size();
        for (int i = 0; i < s; i++) {
            config.increaseQuick(edges.destQuick(i), edges.wtQuick(i));
        }
        config.increaseQuick(vert, -degreeQuick(vert));
    }

    public TIntArrayList getUnstables(SandpileConfiguration config) {
        TIntArrayList unstables = new TIntArrayList();
        for (int v = 0; v < numVertices(); v++) {
            if (!isSinkQuick(v) && config.get(v) >= degreeQuick(v)) {
                unstables.add(v);
            }
        }
        return unstables;
    }

    public TIntArrayList getUnstables(SandpileConfiguration config, TIntArrayList verts) {
        TIntArrayList unstables = new TIntArrayList();
        //System.err.println("getUnstables");
        for (int i = 0; i < verts.size(); i++) {
            int v = verts.get(i);
            //System.err.println(v);
            if (!isSinkQuick(v) && config.get(v) >= degreeQuick(v)) {
                unstables.add(v);
            }
        }
        return unstables;
    }

//	public Iterator<SandpileConfiguration> updater(final SandpileConfiguration config) {
//		return updaterStartingWith(config, getUnstables(config));
//	}
//
//	public Iterator<SandpileConfiguration> updaterStartingWith(final SandpileConfiguration config, final TIntArrayList startingVertices) {
//
//		final int[] unstables = new int[config.size()];
//		Arrays.fill(unstables, -1);
//		int i = 0;
//		for(int j=0; j < startingVertices.size(); j++){
//			int v = startingVertices.get(j);
//			if(config.get(v)>=degree(v) && !isSink(v)){
//				unstables[i] = v;
//				i++;
//			}
//		}
//		final int[] newUnstables = new int[config.size()];
//		Arrays.fill(newUnstables,-1);
//		return new Iterator<SandpileConfiguration>() {
//
//			SandpileConfiguration curConfig = new SandpileConfiguration(config);
//			//TIntArrayList unstables = getUnstables(config, startingVertices);
//			boolean[] added = new boolean[numVertices()];
//
//			public boolean hasNext() {
//				return unstables.length>0 && unstables[0]!=-1;
//			}
//			public SandpileConfiguration next() {
//				for(int i=0; unstables[i]!=-1; i++){
//					fireVertexInPlace(curConfig, unstables[i]);
//				}
//				int j=0;
//				for(int i=0; unstables[i]!=-1; i++){
//					int v = unstables[i];
//					if(!added[v] && curConfig.get(v)>=degree(v)){
//						newUnstables[j]=v;
//						added[v]=true;
//						j++;
//					}
//					for(int w : getOutgoingVertices(v)){
//						if(!added[w] && curConfig.get(w)>=degree(w) && !isSink(w)){
//							newUnstables[j]=w;
//							added[w]=true;
//							j++;
//						}
//					}
//					unstables[i]=-1;
//				}
//				for(int i=0; newUnstables[i]!=-1; i++){
//					int v = newUnstables[i];
//					added[v]=false;
//					unstables[i] = v;
//					newUnstables[i]=-1;
//				}
//
//				return curConfig;
//			}
//
//			public void remove() {
//				throw new UnsupportedOperationException();
//			}
//		};
//	}
    public Iterator<SandpileConfiguration> inPlaceParallelUpdater(final SandpileConfiguration config) {
        return inPlaceParallelUpdaterStartingWith(config, getUnstables(config));
    }

    public Iterator<SandpileConfiguration> inPlaceParallelUpdaterStartingWith(final SandpileConfiguration config, final TIntArrayList startingVertices) {

        final IntGenerationalQueue unstables = new IntGenerationalQueue(numVertices());
        final boolean[] added = new boolean[numVertices()];
        for (int i = 0; i < startingVertices.size(); i++) {
            int v = startingVertices.getQuick(i);
            if (config.get(v) >= degreeQuick(v)) {
                unstables.addUnsafe(v);
                added[v] = true;
            }
        }
        return new Iterator<SandpileConfiguration>() {

            public boolean hasNext() {
                return unstables.hasNextGeneration();
            }

            public SandpileConfiguration next() {
                int numUnstables = unstables.nextGenerationLength();
                unstables.goToNextGeneration();
                //System.err.println(numUnstables);
                // foreach unstable in the current generation...
                for (int i = 0; i < numUnstables; i++) {

                    // get the next unstable
                    int v = unstables.nextItemUnsafe();
                    // mark it as removed
                    added[v] = false;

		    int timesToFire = config.getQuick(v)/degreeQuick(v);

                    // We get the vertices edge info in the form of offsets.
                    // Going through the offset list gives us more direct access
                    // to the edge info.
                    EdgeOffsetList offsetList = vertsToOffsetLists.get(v);
                    int s = offsetList.size();
                    for (int k = 0; k < s; k++) {
                        // Get the a neighboring vertex.
                        int dest = offsetList.destOffsetQuick(k) + v;
                        // Increase the sand on it.
                        config.increaseQuick(dest, offsetList.wtQuick(k)*timesToFire);
                        // Check to see if we made it unstable.
                        int degree = degreeQuick(dest);
                        if (!added[dest] && config.getQuick(dest) >= degree && degree > 0) {
                            unstables.addUnsafe(dest);
                            added[dest] = true;
                        }
                    }
                    // Remove the sand fired from our source vertex.
                    int degree = offsetList.degree();
                    config.increaseQuick(v, -degree*timesToFire);
                    // if still unstable, include it in next generation
                    if (config.getQuick(v) >= degree) {
                        try {
                            unstables.addUnsafe(v);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            e.printStackTrace();
                        }
                        added[v] = true;
                    }
                }
                return config;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Takes in a configuration and outputs the resulting configuration
     */
    //public SandpileConfiguration updateConfig(SandpileConfiguration config) {
    //	return fireVertices(config, getUnstables(config));
    //}
    public SandpileConfiguration reverseFireVertex(SandpileConfiguration config, int vert) {
        SandpileConfiguration newConfig = new SandpileConfiguration(config);
        newConfig.set(vert, config.get(vert) + degreeQuick(vert));
        for (Edge e : getOutgoingEdges(vert)) {
            newConfig.set(e.dest(), newConfig.get(e.dest()) - e.wt());
        }
        return newConfig;
    }

    public SandpileConfiguration reverseFireVertexInPlace(SandpileConfiguration config, int vert) {
        config.increaseQuick(vert, degreeQuick(vert));
        EdgeList edges = getOutgoingEdges(vert);
        int s = edges.size();
        for (int i = 0; i < s; i++) {
            config.increaseQuick(edges.destQuick(i), -edges.wtQuick(i));
        }
        return config;
    }

    public SandpileConfiguration reverseFireConfig(SandpileConfiguration config) {
        SandpileConfiguration newConfig = new SandpileConfiguration(config);
        for (int sourceVert = 0; sourceVert < config.size(); sourceVert++) {
            newConfig.set(sourceVert, newConfig.get(sourceVert) + degreeQuick(sourceVert));
            for (Edge e : this.getOutgoingEdges(sourceVert)) {
                newConfig.set(e.dest(), newConfig.get(e.dest()) - weight(sourceVert, e.dest()));
            }
        }
        return newConfig;
    }

    /**
     * Updates the graph until all vertices stabilize.
     * WARNING: If the graph does not have a global sink, this function may not end.
     */
    public SandpileConfiguration stabilizeConfig(SandpileConfiguration config) throws InterruptedException {
        return stabilizeConfigStartingWith(config, getUnstables(config));
    }

    public SandpileConfiguration stabilizeConfigStartingWith(SandpileConfiguration config, TIntArrayList starters) throws InterruptedException {
        SandpileConfiguration stableConfig = new SandpileConfiguration(config);
        Iterator<SandpileConfiguration> updater = this.inPlaceParallelUpdaterStartingWith(stableConfig, starters);
        for (; updater.hasNext();) {
            updater.next();
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
        return stableConfig;
    }

    /**
     * Updates the graph until all vertices stabilize.
     * WARNING: If the graph does not have a global sink, this function may not end.
     */
    public SandpileConfiguration stabilizeConfigInPlace(SandpileConfiguration config) throws InterruptedException {
        return stabilizeConfigInPlaceStartingWith(config, getUnstables(config));
    }

    public SandpileConfiguration stabilizeConfigInPlaceStartingWith(SandpileConfiguration config, TIntArrayList starters) throws InterruptedException {
        SandpileConfiguration stableConfig = config;
        Iterator<SandpileConfiguration> updater = this.inPlaceParallelUpdaterStartingWith(config, starters);
        for (; updater.hasNext();) {
            updater.next();
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
        return stableConfig;
    }

    /**
     * Returns the number of edges from the first vertex to the second.
     */
    public int weight(int originVert, int destVert) {
        for (Edge e : getOutgoingEdges(originVert)) {
            if (e.dest() == destVert) {
                return e.wt();
            }
        }
        return 0;
    }

    /**
     * Returns a string representation of the edges in the graph.
     * Purely for command line and diagnostic use.
     *//*
    public String edgesString() {
    String output = new String();
    for( int i = 0; i<edges.size(); i++) {
    output = output.concat(i+ " " + vertices.get(i).degree()+"\n");
    }
    return output;
    }*/

    public SandpileConfiguration getUniformConfig(int amount) {
        SandpileConfiguration config = new SandpileConfiguration(this.numVertices());
        for (int i = 0; i < numVertices(); i++) {
            config.add(amount);
        }
        return config;
    }

    /**
     * Calculates the maximum stable configuration of the graph.
     * Runs in O(V).
     *
     * @return Returns a list of integers, where getMaxConfig()[i] = the amount of sand on vertex i.
     */
    public SandpileConfiguration getMaxConfig() {
        SandpileConfiguration maxConfig = new SandpileConfiguration(this.numVertices());
        for (int i = 0; i < numVertices(); i++) {
            int d = degreeQuick(i);
            maxConfig.add(d - 1);
        }
        return maxConfig;
    }

    /**
     * Calculates the dual of the current configuration. That is, degree[i]-sand[i]-1.
     * Runs in O(V).
     *
     * @return Returns a list representing the configuration.
     */
    public SandpileConfiguration getDualConfig(SandpileConfiguration config) {
        SandpileConfiguration dualConfig = new SandpileConfiguration(this.numVertices());
        for (int i = 0; i < config.size(); i++) {
            dualConfig.add(degree(i) - 1 - config.get(i));
        }
        return dualConfig;
    }

    /**
     * Calculates the minimal burning configuration of the graph.
     *
     * @return Returns a list representing the configuration.
     */
    public SandpileConfiguration getMinimalBurningConfig() throws InterruptedException {
        SandpileConfiguration config = reverseFireConfig(getUniformConfig(0));
        TIntStack inDebts = new TIntStack();
        boolean[] added = new boolean[numVertices()];
        for (int v = 0; v < this.numVertices(); v++) {
            if (config.getQuick(v) < 0 && !isSinkQuick(v)) {
                added[v] = true;
                inDebts.push(v);
            }
        }
        while (inDebts.size() > 0) {
            //System.err.println(w);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            int v = inDebts.pop();
            added[v] = false;
            reverseFireVertexInPlace(config, v);
            if (config.getQuick(v) < 0 && !isSinkQuick(v)) {
                added[v] = true;
                inDebts.push(v);
            }
            EdgeList edges = getOutgoingEdges(v);
            int s = edges.size();
            for (int i = 0; i < s; i++) {
                int w = edges.destQuick(i);
                if (!added[w] && config.getQuick(w) < 0 && !isSinkQuick(w)) {
                    added[w] = true;
                    inDebts.push(w);
                }
            }
        }
        return config;
    }

    /**
     * Calculates the recurrent configuration that is equivalent to config,
     * where equivalent means that stabilize(config+identity) = equivConfig.
     * This method calculates the recurrent by adding the burning config to the
     * config repeatedly, until it no longer changes. The efficiency of this
     * method relies on the fact that stabilizing a stable config plus burning
     * is O(V+E), rather than each update taking O(V+E) as with arbitrary
     * stabilization.
     * WARNING: This method can take a very long time for large graphs.
     * WARNING2: If there is no global sink for the graph, this method may not
     * end.
     * @param config An arbitrary SandpileConfiguration.
     * @return The equivalent sandpile configuration.
     */
    public SandpileConfiguration getEquivalentRecurrent(SandpileConfiguration config) throws InterruptedException {
        SandpileConfiguration burning = getMinimalBurningConfig();
        TIntArrayList burningVertices = new TIntArrayList();
        for (int vert = 0; vert < burning.size(); vert++) {
            if (burning.get(vert) > 0) {
                burningVertices.add(vert);
            }
        }
        SandpileConfiguration result = stabilizeConfig(config.plus(burning));
        SandpileConfiguration nextResult = stabilizeConfigStartingWith(result.plus(burning), burningVertices);
        while (!result.equals(nextResult)) {
            result = nextResult;
            nextResult = stabilizeConfigStartingWith(result.plus(burning), burningVertices);
            if (nextResult == null) {
                return null;
            }
        }
        return result;
    }

    /**
     * Calculates the inverse recurrent config. That is, if config is recurrent,
     * stabilize(config + inverse) = identity.
     * WARNING: This method uses getEquivalentRecurrent(), so the same warnings
     * apply.
     * @param config An arbitrary SandpileConfiguration.
     * @return The recurrent inverse of config;
     */
    public SandpileConfiguration getInverseConfig(SandpileConfiguration config) throws InterruptedException {
        SandpileConfiguration inverse = config.times(-1);
        return getEquivalentRecurrent(inverse);
    }

    /**
     * Gets an arbitrary vertex with negative sand.
     * @return Returns the index of a vertex.
     */
    private int getInDebtVertex(SandpileConfiguration config) {
        int s = config.size();
        for (int i = 0; i < s; i++) {
            if (config.getQuick(i) < 0 && !this.isSinkQuick(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Calculates whether or not the given configuration is stable.
     */
    public boolean isStable(SandpileConfiguration config) {
        for (int vert = 0; vert < config.size(); vert++) {
            if (config.get(vert) >= degreeQuick(vert)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates the identity configuration of the graph.
     * WARNING: For big graphs, this can take a long time.
     * Currently, the function adds the burning configuration
     * to itself until it gets the same thing back. This relies
     * on the fact that stabilizing the burning config is linear.
     * SECOND WARNING: If the graph does not have an identity (there is no global
     * sink or something), this function will never end!
     * @return The identity configuration.
     */
    public SandpileConfiguration getIdentityConfig() throws InterruptedException {
        return getEquivalentRecurrent(getUniformConfig(0));
    }
}
