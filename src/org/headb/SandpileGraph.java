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

//
//  SandpileGraph.java
//  Sandpiles
//
//  Created by Bryan Head on 9/22/08.
//  Copyright 2008 Reed College. All rights reserved.
//
import java.util.*;

public class SandpileGraph {

	private ArrayList<ArrayList<int[]>> adj;
	private ArrayList<Integer> degrees;

	public SandpileGraph() {
		this.adj = new ArrayList<ArrayList<int[]>>();
		this.degrees = new ArrayList<Integer>();
	}

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
	public int[] getEdge(int source, int dest) {
		ArrayList<int[]> edgeList = adj.get(source);
		for (int[] e : edgeList) {
			if (e[1] == dest) {
				return e;
			}
		}
		return null;
	}

	/**
	 * Retrieves the number of outgoing edges of the given vertex.
	 */
	public int degree(int vert) {
		//return this.vertices.get(vert).degree();
		return this.degrees.get(vert);
	}

	/**
	 * Retrieves the list of edges which begin with the source vertex.
	 */
	public final List<int[]> getOutgoingEdges(int vert) {
		return adj.get(vert);
	}

	public final Iterable<Integer> getOutgoingVertices(int vert) {
		final List<int[]> edgeList = getOutgoingEdges(vert);
		final Iterator<Integer> vertIter = new Iterator<Integer>() {

			Iterator<int[]> edgeIter = edgeList.iterator();

			public boolean hasNext() {
				return edgeIter.hasNext();
			}

			public Integer next() {
				return edgeIter.next()[1];
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
		return new Iterable<Integer>() {

			public Iterator<Integer> iterator() {
				return vertIter;
			}
		};
	}

	/**
	 * Retrieves the list of edges which begin with any of the edges in the given list.
	 */
	public final List<int[]> getOutgoingEdges(final Iterable<Integer> verts) {
		ArrayList<int[]> edgeList = new ArrayList<int[]>();
		boolean[] alreadyAdded = new boolean[numVertices()];
		for (int vert : verts) {
			for (int[] e : getOutgoingEdges(vert)) {
				if (!alreadyAdded[e[1]]) {
					edgeList.add(e);
					alreadyAdded[e[1]] = true;
				}
			}
		}
		return edgeList;
	}

	public final Iterable<Integer> getOutgoingVertices(final Iterable<Integer> verts) {
		final List<int[]> edgeList = getOutgoingEdges(verts);
		final Iterator<Integer> vertIter = new Iterator<Integer>() {

			Iterator<int[]> edgeIter = edgeList.iterator();

			public boolean hasNext() {
				return edgeIter.hasNext();
			}

			public Integer next() {
				return edgeIter.next()[1];
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
		return new Iterable<Integer>() {

			public Iterator<Integer> iterator() {
				return vertIter;
			}
		};
	}

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
		adj.add(new ArrayList<int[]>());
		this.degrees.add(0);
	}

	/**
	 * Removes a vertex from the graph. Runs in time linear to the number of edges plus the number of vertices.
	 */
	public void removeVertex(int toDelete) {
		for (ArrayList<int[]> edgeList : adj) {
			for (Iterator<int[]> edgeIter = edgeList.iterator(); edgeIter.hasNext();) {
				int[] e = edgeIter.next();
				if (e[1] == toDelete) {
					degrees.set(e[0], degree(e[0]) - e[2]);
					edgeIter.remove();
				} else {
					e[0] -= e[0] > toDelete ? 1 : 0;
					e[1] -= e[1] > toDelete ? 1 : 0;
				}
			}
		}
		adj.remove(toDelete);
		degrees.remove(toDelete);
	}

	/**
	 * Removes the vertices in the list from the graph.
	 * Know idea what the running time of this is, but I spent a lot of time trying
	 * to optimize this code.
	 *
	 * @param vertices A list of the vertices to remove.
	 */
	public void removeVertices(List<Integer> vertices) {
		int[] translator = new int[numVertices()];
		ArrayList<ArrayList<int[]>> oldAdj = new ArrayList<ArrayList<int[]>>(adj);
		boolean[] toRemove = new boolean[numVertices()];
		for (int v : vertices) {
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
			for (int[] e : oldAdj.get(v)) {
				if (!toRemove[e[1]]) {
					addEdge(translator[e[0]], translator[e[1]], e[2]);
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
	 * Adds the given number of vertices to the graph. Runs in constant time.
	 */
	public void addVertices(int amount) {
		for (int i = 0; i < amount; i++) {
			this.addVertex();
		}
	}

	/**
	 * Adds an edge from the first vertex to the second.
	 */
	public int[] addEdge(int sourceVert, int destVert) {
		//this.vertices.get(sourceVert).addOutgoingEdge(this.vertices.get(destVert));
		return this.addEdge(sourceVert, destVert, 1);
	}

	/**
	 * Adds an edge of the given weight.
	 */
	public int[] addEdge(int sourceVert, int destVert, int weight) {
		int[] e = getEdge(sourceVert, destVert);
		if (e == null && weight > 0) {
			e = new int[3];
			e[0] = sourceVert;
			e[1] = destVert;
			e[2] = weight;
			adj.get(sourceVert).add(e);
			degrees.set(sourceVert, degree(sourceVert) + weight);
		} else if(e == null){
			return null;
		}else if (weight + e[2] > 0) {
			e[2] += weight;
			degrees.set(sourceVert, degree(sourceVert) + weight);
		} else {
			adj.get(sourceVert).remove(e);
			degrees.set(sourceVert, degree(sourceVert) - e[2]);
			return null;
		}
		return e;
	}

	/**
	 * Removes an edge from the first vertex to the second
	 */
	public void removeEdge(int sourceVert, int destVert) {
		this.removeEdge(sourceVert, destVert, 1);
	}

	/**
	 * Removes an edge of the given weight.
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
		if (degree(vertIndex) == 0) {
			return true;
		}
		return false;
	}

	public List<Integer> getNonSinks() {
		ArrayList<Integer> nonsinks = new ArrayList<Integer>();
		for(int v=0; v<numVertices(); v++){
			if(!isSink(v))
				nonsinks.add(v);
		}
		return nonsinks;
	}

	public List<Integer> getSinks() {
		ArrayList<Integer> sinks = new ArrayList<Integer>();
		for(int v=0; v<numVertices(); v++){
			if(isSink(v))
				sinks.add(v);
		}
		return sinks;
	}

	public SandpileConfiguration fireVertices(SandpileConfiguration config, Iterable<Integer> verts) {
		SandpileConfiguration newConfig = new SandpileConfiguration(config);
		for (int v : verts) {
			for (int[] e : getOutgoingEdges(v)) {
				newConfig.set(e[1], newConfig.get(e[1]) + e[2]);
			}
			newConfig.set(v, newConfig.get(v) - degree(v));
		}
		return newConfig;
	}

	public SandpileConfiguration fireVerticesInPlace(SandpileConfiguration config, Iterable<Integer> verts) {
		for (int v : verts) {
			for (int[] e : getOutgoingEdges(v)) {
				config.set(e[1], config.get(e[1]) + e[2]);
			}
			config.set(v, config.get(v) - degree(v));
		}
		return config;
	}

	public void fireVertexInPlace(SandpileConfiguration config, int vert){
		List<int[]> edges = getOutgoingEdges(vert);
		for(int i=0; i<edges.size(); i++){
			int[] e = edges.get(i);
			config.set(e[1], config.get(e[1])+e[2]);
		}
		config.set(vert, config.get(vert)-degree(vert));
	}

	public List<Integer> getUnstables(SandpileConfiguration config) {
		ArrayList<Integer> unstables = new ArrayList<Integer>();
		for (int v = 0; v < numVertices(); v++) {
			if (!isSink(v) && config.get(v) >= degree(v)) {
				unstables.add(v);
			}
		}
		return unstables;
	}

	public List<Integer> getUnstables(SandpileConfiguration config, Iterable<Integer> verts) {
		ArrayList<Integer> unstables = new ArrayList<Integer>();
		//System.err.println("getUnstables");
		for (int v : verts) {
			//System.err.println(v);
			if (!isSink(v) && config.get(v) >= degree(v)) {
				unstables.add(v);
			}
		}
		return unstables;
	}

	public Iterator<SandpileConfiguration> updater(final SandpileConfiguration config) {
		return updaterStartingWith(config, getUnstables(config));
	}

	public Iterator<SandpileConfiguration> updaterStartingWith(final SandpileConfiguration config, final List<Integer> startingVertices) {

		final int[] unstables = new int[config.size()];
		Arrays.fill(unstables, -1);
		int i = 0;
		for(int v : startingVertices){
			if(config.get(v)>=degree(v) && !isSink(v)){
				unstables[i] = v;
				i++;
			}
		}
		final int[] newUnstables = new int[config.size()];
		Arrays.fill(newUnstables,-1);
		return new Iterator<SandpileConfiguration>() {

			SandpileConfiguration curConfig = new SandpileConfiguration(config);
			//List<Integer> unstables = getUnstables(config, startingVertices);
			boolean[] added = new boolean[numVertices()];

			public boolean hasNext() {
				return unstables.length>0 && unstables[0]!=-1;
			}
			public SandpileConfiguration next() {
				for(int i=0; unstables[i]!=-1; i++){
					fireVertexInPlace(curConfig, unstables[i]);
				}
				int j=0;
				for(int i=0; unstables[i]!=-1; i++){
					int v = unstables[i];
					if(!added[v] && curConfig.get(v)>=degree(v)){
						newUnstables[j]=v;
						added[v]=true;
						j++;
					}
					for(int w : getOutgoingVertices(v)){
						if(!added[w] && curConfig.get(w)>=degree(w) && !isSink(w)){
							newUnstables[j]=w;
							added[w]=true;
							j++;
						}
					}
					unstables[i]=-1;
				}
				for(int i=0; newUnstables[i]!=-1; i++){
					int v = newUnstables[i];
					added[v]=false;
					unstables[i] = v;
					newUnstables[i]=-1;
				}
				
				return curConfig;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	public Iterator<SandpileConfiguration> inPlaceUpdater(final SandpileConfiguration config) {
		return inPlaceUpdaterStartingWith(config, getUnstables(config));
	}
	public Iterator<SandpileConfiguration> inPlaceUpdaterStartingWith(final SandpileConfiguration config, final List<Integer> startingVertices) {

		final int[] unstables = new int[config.size()];
		Arrays.fill(unstables, -1);
		int i = 0;
		for(int v : startingVertices){
			if(config.get(v)>=degree(v) && !isSink(v)){
				unstables[i] = v;
				i++;
			}
		}
		final int[] newUnstables = new int[config.size()];
		Arrays.fill(newUnstables,-1);
		return new Iterator<SandpileConfiguration>() {
			boolean[] added = new boolean[numVertices()];

			public boolean hasNext() {
				return unstables.length!=0 && unstables[0]!=-1;
			}
			public SandpileConfiguration next() {
				for(int i=0; unstables[i]!=-1; i++){
					fireVertexInPlace(config, unstables[i]);
				}
				int j=0;
				for(int i=0; unstables[i]!=-1; i++){
					int v = unstables[i];
					if(!added[v] && config.get(v)>=degree(v)){
						newUnstables[j]=v;
						added[v]=true;
						j++;
					}
					List<int[]> edges = getOutgoingEdges(v);
					for(int k=0; k<edges.size(); k++){
						int w = edges.get(k)[1];
						if(!added[w] && config.get(w)>=degree(w) && !isSink(w)){
							newUnstables[j]=w;
							added[w]=true;
							j++;
						}
					}
					unstables[i]=-1;
				}
				for(int i=0; newUnstables[i]!=-1; i++){
					int v = newUnstables[i];
					added[v]=false;
					unstables[i] = v;
					newUnstables[i]=-1;
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
		for (Integer w : getOutgoingVertices(vert)) {
			newConfig.set(w, newConfig.get(w) - weight(vert, w));
		}
		return newConfig;
	}

	public SandpileConfiguration reverseFireConfig(SandpileConfiguration config) {
		SandpileConfiguration newConfig = new SandpileConfiguration(config);
		for (int sourceVert = 0; sourceVert < config.size(); sourceVert++) {
			newConfig.set(sourceVert, newConfig.get(sourceVert) + degree(sourceVert));
			for (Integer destVert : this.getOutgoingVertices(sourceVert)) {
				newConfig.set(destVert, newConfig.get(destVert) - weight(sourceVert, destVert));
			}
		}
		return newConfig;
	}

	/**
	 * Updates the graph until all vertices stabilize.
	 * WARNING: If the graph does not have a global sink, this function may not end.
	 */
	public SandpileConfiguration stabilizeConfig(SandpileConfiguration config) {
		return stabilizeConfigStartingWith(config, getUnstables(config));
	}

	public SandpileConfiguration stabilizeConfigStartingWith(SandpileConfiguration config, List<Integer> starters) {
		SandpileConfiguration stableConfig = new SandpileConfiguration(config);
		Iterator<SandpileConfiguration> updater = this.inPlaceUpdaterStartingWith(stableConfig, starters);
		for(;updater.hasNext();){
			updater.next();
		}
		return stableConfig;
	}

	/**
	 * Updates the graph until all vertices stabilize.
	 * WARNING: If the graph does not have a global sink, this function may not end.
	 */
	public SandpileConfiguration stabilizeConfigInPlace(SandpileConfiguration config) {
		return stabilizeConfigInPlaceStartingWith(config, getUnstables(config));
	}

	public SandpileConfiguration stabilizeConfigInPlaceStartingWith(SandpileConfiguration config, List<Integer> starters) {
		SandpileConfiguration stableConfig = config;
		Iterator<SandpileConfiguration> updater = this.inPlaceUpdaterStartingWith(config, starters);
		for(;updater.hasNext();){
			updater.next();
		}
		return stableConfig;
	}

	/**
	 * Returns the number of edges from the first vertex to the second.
	 */
	public int weight(int originVert, int destVert) {
		for (int[] e : getOutgoingEdges(originVert)) {
			if (e[1] == destVert) {
				return e[2];
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
		for (Integer d : degrees) {
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
	 * Runs in O(V+E), I think.
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
	/*
	public SandpileConfiguration fireEverything(SandpileConfiguration config, int times){
	SandpileConfiguration newConfig = new SandpileConfiguration(config);
	for(int sourceVert=0; sourceVert<config.size(); sourceVert++){
	newConfig.set(sourceVert, newConfig.get(sourceVert) - times*degree(sourceVert));
	for(Integer destVert : this.getOutgoingVertices(sourceVert))
	newConfig.set(destVert, newConfig.get(destVert)+times*weight(sourceVert,destVert));
	}
	return newConfig;
	}*/

	/**
	 * Calculates the identity configuration of the graph.
	 * WARNING: For big graphs, this can take a long time.
	 * Currently, the function stabilizes 2*max_config,
	 * takes the dual, adds max_config, and stabilizes again.
	 * SECOND WARNING: If the graph does not have an identity (there is no global
	 * sink or something), this function will never end!
	 * @return A list representing the identity configuration.
	 *//*
	public SandpileConfiguration getIdentityConfig() {
	SandpileConfiguration config = new SandpileConfiguration(this.numVertices());
	SandpileConfiguration maxConfig = this.getMaxConfig();
	config = maxConfig.plus(maxConfig);
	config = stabilizeConfig(config);
	config = this.getDualConfig(config);
	config = config.plus(maxConfig);
	return stabilizeConfig(config);
	}
	 */

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
	public SandpileConfiguration getIdentityConfig() {
		SandpileConfiguration burning = getMinimalBurningConfig();
		ArrayList<Integer> burningVertices = new ArrayList<Integer>();
		for (int vert = 0; vert < burning.size(); vert++) {
			if (burning.get(vert) > 0) {
				burningVertices.add(vert);
			}
		}
		SandpileConfiguration config = stabilizeConfig(burning);
		SandpileConfiguration nextConfig = stabilizeConfigStartingWith(config.plus(burning), burningVertices);
		while (!config.equals(nextConfig)) {
			config = nextConfig;
			nextConfig = stabilizeConfigStartingWith(config.plus(burning), burningVertices);
		}
		return config;
	}
}
