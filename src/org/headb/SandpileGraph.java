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

/**
 * Represents a weighted, directed graph with methods for updating a sandpile
 * configuration on that graph. Contains methods for calculating several
 * important configuration. The most efficient method of updating a config
 * repeatedly is to use an iterator returned by inPlaceUpdater(config). Calling
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
	private ArrayList<EdgeList> adj;
	private TIntArrayList degrees;

	/**
	 * Creates a new, empty graph.
	 */
	public SandpileGraph() {
		this.adj = new ArrayList<EdgeList>();
		this.degrees = new TIntArrayList();
	}

	public SandpileGraph(SandpileGraph graph){
		this.adj = new ArrayList<EdgeList>();
		for (EdgeList v : graph.adj) {
			EdgeList newV = new EdgeList(v);
			this.adj.add(newV);
		}
		this.degrees = new TIntArrayList(graph.degrees.toNativeArray());
	}

	/**
	 * Returns the number of vertices.
	 * @return An int of the number of vertices.
	 */
	public int numVertices(){
		return adj.size();
	}

	/**
	 * Retrieves the edge beginning with source and ending with dest.
	 * Runs in time linear to the number of source' outgoing edges.
	 *
	 * @param source The index of the source vertex
	 * @param dest The index of the destination vertex
	 * @return If the edge exists, the return value will be {source, dest, weight}. Otherwise, returns null.
	 */
	public Edge getEdge(int source, int dest) {
		EdgeList edgeList = adj.get(source);
		for (Edge e : edgeList) {
			if (e.dest() == dest) {
				return e;
			}
		}
		return null;
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
		return this.degrees.get(vert);
	}

	/**
	 * Retrieves the sum of the weights of the outgoing edges of the given
	 * vertex. Does not check to see if the index is within range, thus
	 * creating unpredictable behavior if it is not.
	 *
	 * @param vert The index of the vertex.
	 * @return The sum of the weights of the outgoing edges of the given vertex.
	 */
	public int degreeQuick(int vert){
		return this.degrees.getQuick(vert);
	}

	/**
	 * Retrieves the list of edges which begin with the source vertex.
	 */
	public final EdgeList getOutgoingEdges(int vert) {
		return adj.get(vert);
	}

	public EdgeList getIncomingEdges(int vert) {
		EdgeList edges = new EdgeList();
		for(int v=0; v<this.numVertices(); v++){
			for(Edge e : getOutgoingEdges(v)){
				if(e.dest() == vert)
					edges.add(e);
			}
		}
		return edges;
	}

//	public final Iterable<Integer> getOutgoingVertices(int vert) {
//		final List<int[]> edgeList = getOutgoingEdges(vert);
//		final Iterator<Integer> vertIter = new Iterator<Integer>() {
//
//			Iterator<int[]> edgeIter = edgeList.iterator();
//
//			public boolean hasNext() {
//				return edgeIter.hasNext();
//			}
//
//			public Integer next() {
//				return edgeIter.next()[1];
//			}
//
//			public void remove() {
//				throw new UnsupportedOperationException();
//			}
//		};
//		return new Iterable<Integer>() {
//
//			public Iterator<Integer> iterator() {
//				return vertIter;
//			}
//		};
//	}

	/**
	 * Retrieves the list of edges which begin with any of the edges in the given list.
	 */
//	public final EdgeList getOutgoingEdges(final TIntArrayList verts) {
//		EdgeList edgeList = new EdgeList();
//		boolean[] alreadyAdded = new boolean[numVertices()];
//		for (int i=0; i< verts.size(); i++) {
//			int vert = verts.get(i);
//			for (Edge e : getOutgoingEdges(vert)) {
//				if (!alreadyAdded[e.dest()]) {
//					edgeList.add(e);
//					alreadyAdded[e[1]] = true;
//				}
//			}
//		}
//		return edgeList;
//	}

//	public final Iterable<Integer> getOutgoingVertices(final Iterable<Integer> verts) {
//		final List<int[]> edgeList = getOutgoingEdges(verts);
//		final Iterator<Integer> vertIter = new Iterator<Integer>() {
//
//			Iterator<int[]> edgeIter = edgeList.iterator();
//
//			public boolean hasNext() {
//				return edgeIter.hasNext();
//			}
//
//			public Integer next() {
//				return edgeIter.next()[1];
//			}
//
//			public void remove() {
//				throw new UnsupportedOperationException();
//			}
//		};
//		return new Iterable<Integer>() {
//
//			public Iterator<Integer> iterator() {
//				return vertIter;
//			}
//		};
//	}

	/**
	 * Returns a set of the indices of vertices that have outgoing edges with the indicated vertex as their destination.
	 * Runs in linear time, proportional to the number of vertices in the graph.
	 */
	/*public Set<Integer> getIncomingVertices(int vert) {
	Set<Integer> incVerts = new HashSet<Integer>();
	for (int i = 0; i < this.edges.size(); i++) {
	if (weight(i, vert) > 0) {
	incVerts.add(i);
	}
	}
	return incVerts;
	}*/
	/**
	 * Adds a vertex to the graph.
	 */
	public void addVertex() {
		adj.add(new EdgeList());
		this.degrees.add(0);
	}

	/**
	 * Removes a vertex from the graph. Note that the index of all vertices
	 * added after the vertex being deleted will be decreased by one.
	 *
	 * @param toDelete The index of the vertex to delete.
	 */
	public void removeVertex(int toDelete) {
		for (EdgeList edgeList : adj) {
			for (Iterator<Edge> edgeIter = edgeList.iterator(); edgeIter.hasNext();) {
				Edge e = edgeIter.next();
				if (e.dest() == toDelete) {
					degrees.set(e.source(), degree(e.source()) - e.wt());
					edgeIter.remove();
				} else {
					e.setSource(e.source() - (e.source() > toDelete ? 1 : 0));
					e.setDest(e.dest() - (e.dest() > toDelete ? 1 : 0));
				}
			}
		}
		adj.remove(toDelete);
		degrees.remove(toDelete);
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
		ArrayList<EdgeList> oldAdj = new ArrayList<EdgeList>(adj);
		boolean[] toRemove = new boolean[numVertices()];
		for (int i=0; i<vertices.size(); i++) {
			int v = vertices.get(i);
			toRemove[v] = true;
		}
		this.removeAllVertices();
		int w=0;
		for (int v=0; v<oldAdj.size(); v++){
			if(!toRemove[v]){
				translator[v] = w;
				w++;
			}
		}
		addVertices(w);
		for (int v = 0; v < oldAdj.size(); v++) {
			if (toRemove[v]) {
				continue;
			}
			for (Edge e : oldAdj.get(v)) {
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
		this.adj.clear();
		this.degrees.clear();
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
	 * @return Returns the newly created edge.
	 */
	public Edge addEdge(int sourceVert, int destVert) {
		//this.vertices.get(sourceVert).addOutgoingEdge(this.vertices.get(destVert));
		return this.addEdge(sourceVert, destVert, 1);
	}

	/**
	 * Increases the weight of the edge from sourceVert to destVert by weight.
	 * weight can be negative. If the weight of the resulting edge is negative
	 * or zero, it is removed. If the edge does not already exist it will be
	 * created
	 * @param sourceVert The index of the source vertex.
	 * @param destVert The index of the destination vertex.
	 * @param weight The amount to increase the weight of the edge by.
	 * @return Returns the newly created edge or null if the resuling weight was
	 * zero or less.
	 */
	public Edge addEdge(int sourceVert, int destVert, int weight) {
		Edge e = getEdge(sourceVert, destVert);
		if (e == null && weight > 0) {
			e = new Edge(sourceVert, destVert, weight);
			e.setSource(sourceVert);
			e.setDest(destVert);
			e.setWt(weight);
			adj.get(sourceVert).add(e);
			degrees.set(sourceVert, degree(sourceVert) + weight);
		} else if(e == null){
			return null;
		}else if (weight + e.wt() > 0) {
			e.setWt(e.wt() + weight);
			degrees.set(sourceVert, degree(sourceVert) + weight);
		} else {
			adj.get(sourceVert).remove(e);
			degrees.set(sourceVert, degree(sourceVert) - e.wt());
			return null;
		}
		return e;
	}

	/**
	 * Decreases the weight of the edge from sourceVert to destVert by 1. If the
	 * weight of the edge becomes zero or less, it is removed.
	 * @param sourceVert The index of the source vertex.
	 * @param destVert the index of the destination vertex.
	 * @return Returns the newly created edge.
	 */
	public Edge removeEdge(int sourceVert, int destVert) {
		return this.removeEdge(sourceVert, destVert, 1);
	}

	/**
	 * Decreases the weight of the edge from sourceVert to destVert by weight.
	 * weight can be negative. If the weight of the resulting edge is negative
	 * or zero, it is removed.
	 * This is equivalent to addEdge(sourceVert, destVert, -weight)
	 * @param sourceVert The index of the source vertex.
	 * @param destVert The index of the destination vertex.
	 * @param weight The amount to decrease the weight of the edge by.
	 * @return Returns the newly created edge or null if the resuling weight was
	 * zero or less.
	 */
	public Edge removeEdge(int sourceVert, int destVert, int weight) {
		return this.addEdge(sourceVert, destVert, -weight);
	}

	/**
	 * Returns true if the indicated vertex has no outgoing vertices, false otherwise.
	 *
	 * @param vertIndex The index of a vertex.
	 */
	public boolean isSink(int vertIndex) {
		if (degree(vertIndex) == 0) {
			return true;
		}
		return false;
	}

	/**
	 * Returns a list containing the indices of all nonsink vertices.
	 * @return
	 */
	public TIntArrayList getNonSinks() {
		TIntArrayList nonsinks = new TIntArrayList();
		for(int v=0; v<numVertices(); v++){
			if(!isSink(v))
				nonsinks.add(v);
		}
		return nonsinks;
	}

	/**
	 *
	 * @return A list of the indices of each sink vertex.
	 */
	public TIntArrayList getSinks() {
		TIntArrayList sinks = new TIntArrayList();
		for(int v=0; v<numVertices(); v++){
			if(isSink(v))
				sinks.add(v);
		}
		return sinks;
	}

	public SandpileConfiguration fireVertices(SandpileConfiguration config, TIntArrayList verts) {
		SandpileConfiguration newConfig = new SandpileConfiguration(config);
		for (int i=0; i<verts.size(); i++) {
			int v = verts.get(i);
			for (Edge e : getOutgoingEdges(v)) {
				newConfig.set(e.dest(), newConfig.get(e.dest()) + e.wt());
			}
			newConfig.set(v, newConfig.get(v) - degree(v));
		}
		return newConfig;
	}

	public SandpileConfiguration fireVerticesInPlace(SandpileConfiguration config, TIntArrayList verts) {
		for (int i=0; i<verts.size(); i++) {
			int v = verts.get(i);
			for (Edge e : getOutgoingEdges(v)) {
				config.set(e.dest(), config.get(e.dest()) + e.wt());
			}
			config.set(v, config.get(v) - degree(v));
		}
		return config;
	}

	public void fireVertexInPlace(SandpileConfiguration config, int vert){
		EdgeList edges = getOutgoingEdges(vert);
		int s = edges.size();
		for(int i=0; i<s; i++){
			int dest = edges.destQuick(i);
			config.increaseQuick(dest, edges.wtQuick(i));
			//config.setQuick(dest, config.getQuick(dest)+edges.wtQuick(i));
		}
		//config.setQuick(vert, config.getQuick(vert)-degreeQuick(vert));
		config.increaseQuick(vert, -degreeQuick(vert));
	}

	public TIntArrayList getUnstables(SandpileConfiguration config) {
		TIntArrayList unstables = new TIntArrayList();
		for (int v = 0; v < numVertices(); v++) {
			if (!isSink(v) && config.get(v) >= degree(v)) {
				unstables.add(v);
			}
		}
		return unstables;
	}

	public TIntArrayList getUnstables(SandpileConfiguration config, TIntArrayList verts) {
		TIntArrayList unstables = new TIntArrayList();
		//System.err.println("getUnstables");
		for (int i=0; i< verts.size(); i++) {
			int v = verts.get(i);
			//System.err.println(v);
			if (!isSink(v) && config.get(v) >= degree(v)) {
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
	public Iterator<SandpileConfiguration> inPlaceUpdater(final SandpileConfiguration config) {
		return inPlaceUpdaterStartingWith(config, getUnstables(config));
	}
	public Iterator<SandpileConfiguration> inPlaceUpdaterStartingWith(final SandpileConfiguration config, final TIntArrayList startingVertices) {

		final IntGenerationalQueue unstables = new IntGenerationalQueue(numVertices());
		unstables.addAll(startingVertices);
		final boolean[] added = new boolean[numVertices()];
		for(int i=0; i<startingVertices.size(); i++){
			added[startingVertices.getQuick(i)] = true;
		}
		return new Iterator<SandpileConfiguration>() {
			public boolean hasNext() {
				return unstables.hasNextGeneration();
			}
			public SandpileConfiguration next() {
				int numUnstables = unstables.nextGenerationLength();
				unstables.goToNextGeneration();
				// foreach unstable in the current generation...
				for(int i=0; i<numUnstables; i++){

					// get the next unstable
					int v = unstables.nextItemUnsafe();
					// mark it as removed
					added[v]=false;

					// fire it
					fireVertexInPlace(config, v);

					// if still unstable, include it in next generation
					if(config.getQuick(v)>=degrees.getQuick(v)){
						unstables.addUnsafe(v);
						added[v] = true;
					}

					// include (newly) unstable neighbors, adding them too
					EdgeList edges = getOutgoingEdges(v);
					int s = edges.size();
					for(int k=0; k<s; k++){
						int w = edges.destQuick(k);
						int d = degrees.getQuick(w);
						if(!added[w] && config.getQuick(w)>=d && d>0){
							unstables.addUnsafe(w);
							added[w]=true;
						}
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
		newConfig.set(vert, config.get(vert) + degree(vert));
		for (Edge e : getOutgoingEdges(vert)) {
			newConfig.set(e.dest(), newConfig.get(e.dest()) - e.wt());
		}
		return newConfig;
	}

	public SandpileConfiguration reverseFireConfig(SandpileConfiguration config) {
		SandpileConfiguration newConfig = new SandpileConfiguration(config);
		for (int sourceVert = 0; sourceVert < config.size(); sourceVert++) {
			newConfig.set(sourceVert, newConfig.get(sourceVert) + degree(sourceVert));
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
	public SandpileConfiguration stabilizeConfig(SandpileConfiguration config) throws InterruptedException{
		return stabilizeConfigStartingWith(config, getUnstables(config));
	}

	public SandpileConfiguration stabilizeConfigStartingWith(SandpileConfiguration config, TIntArrayList starters) throws InterruptedException{
		SandpileConfiguration stableConfig = new SandpileConfiguration(config);
		Iterator<SandpileConfiguration> updater = this.inPlaceUpdaterStartingWith(stableConfig, starters);
		for(;updater.hasNext();){
			updater.next();
			if(Thread.interrupted())
				throw new InterruptedException();
		}
		return stableConfig;
	}

	/**
	 * Updates the graph until all vertices stabilize.
	 * WARNING: If the graph does not have a global sink, this function may not end.
	 */
	public SandpileConfiguration stabilizeConfigInPlace(SandpileConfiguration config) throws InterruptedException{
		return stabilizeConfigInPlaceStartingWith(config, getUnstables(config));
	}

	public SandpileConfiguration stabilizeConfigInPlaceStartingWith(SandpileConfiguration config, TIntArrayList starters) throws InterruptedException {
		SandpileConfiguration stableConfig = config;
		Iterator<SandpileConfiguration> updater = this.inPlaceUpdaterStartingWith(config, starters);
		for(;updater.hasNext();){
			updater.next();
			if(Thread.interrupted()){
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
	 * Calculates the maximum stable configuration of the graph, preserving the current configuration.
	 * Runs in O(V).
	 *
	 * @return Returns a list of integers, where getMaxConfig()[i] = the amount of sand on vertex i.
	 */
	public SandpileConfiguration getMaxConfig() {
		SandpileConfiguration maxConfig = new SandpileConfiguration(this.numVertices());
		for (int i=0; i<degrees.size(); i++) {
			int d = degrees.get(i);
			maxConfig.add(d - 1);
		}
		return maxConfig;
	}

	/**
	 * Calculates the dual of the current configuration. That is, degree[i]-sand[i]-1. Preserves the current configuration.
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
	 * Calculates the minimal burning configuration of the graph. Preserves the current configuration.
	 *
	 * @return Returns a list representing the configuration.
	 */
	public SandpileConfiguration getMinimalBurningConfig() {
		SandpileConfiguration config = getUniformConfig(0);
		config = reverseFireConfig(config);
		int w = getInDebtVertex(config);
		while (w > -1) {
			config = reverseFireVertex(config, w);
			w = getInDebtVertex(config);
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
	public SandpileConfiguration getEquivalentRecurrent(SandpileConfiguration config) throws InterruptedException{
		SandpileConfiguration burning = getMinimalBurningConfig();
		TIntArrayList burningVertices = new TIntArrayList();
		for (int vert = 0; vert < burning.size(); vert++) {
			if (burning.get(vert) > 0) {
				burningVertices.add(vert);
			}
		}
		SandpileConfiguration result = stabilizeConfig(config.plus(burning));
		SandpileConfiguration nextResult = stabilizeConfigStartingWith(result.plus(burning), burningVertices);
		while(!result.equals(nextResult)){
			result = nextResult;
			nextResult = stabilizeConfigStartingWith(result.plus(burning), burningVertices);
			if(nextResult==null)
				return null;
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
	public SandpileConfiguration getInverseConfig(SandpileConfiguration config) throws InterruptedException{
		SandpileConfiguration inverse = config.times(-1);
		return getEquivalentRecurrent(inverse);
	}

	/**
	 * Gets an arbitrary vertex with negative sand.
	 * @return Returns the index of a vertex.
	 */
	private int getInDebtVertex(SandpileConfiguration config) {
		for (int i = 0; i < config.size(); i++) {
			if (config.get(i) < 0 && !this.isSink(i)) {
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
			if (config.get(vert) >= degree(vert)) {
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
	public SandpileConfiguration getIdentityConfig() throws InterruptedException{
		return getEquivalentRecurrent(getUniformConfig(0));
	}
}
