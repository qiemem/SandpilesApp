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
	private ArrayList<HashMap<Integer, Integer>> edges;
	private ArrayList<Integer> degrees;
	
	public SandpileGraph() {
		this.edges = new ArrayList<HashMap<Integer, Integer>>();
		this.degrees = new ArrayList<Integer>();
	}
	
	/**
	 * Adds a vertex to the graph.
	 */
	public void addVertex() {
		//this.vertices.add(new SandpileVertex());
		this.edges.add(new HashMap<Integer,Integer>());
		this.degrees.add(0);
	}
	
	/**
	 * Removes a vertex from the graph.
	 */
	public void removeVertex(int i) {
		//SandpileVertex vert = vertices.get(i);
		//for(SandpileVertex v : vertices) {
			//v.removeVertex(vert);
		//}
		//this.vertices.remove(i);
		for(int j=0; j<edges.size(); j++){
			Integer w = edges.get(i).remove(j);
			if(w!=null) degrees.set(i, degrees.get(i)-w);
		}
		this.edges.remove(i);
	}

	public void removeAllVertices() {
		//this.vertices.clear();
		this.edges.clear();
		this.degrees.clear();
	}

	/**
	 * Adds the given number of vertices to the graph.
	 */
	public void addVertices(int amount) {
		for(int i = 0; i<amount; i++)
			this.addVertex();
	}
	
	/**
	 * Adds an edge from the first vertex to the second.
	 */
	public void addEdge(int sourceVert, int destVert) {
		//this.vertices.get(sourceVert).addOutgoingEdge(this.vertices.get(destVert));
		this.addEdge(sourceVert, destVert,1);
	}
	
	/**
	 * Adds an edge of the given weight.
	 */
	public void addEdge(int sourceVert, int destVert, int weight) {
		//this.vertices.get(sourceVert).addOutgoingEdge(this.vertices.get(destVert), weight);
		this.edges.get(sourceVert).put(destVert, weight+this.weight(sourceVert, destVert));
		this.degrees.set(sourceVert,this.degree(sourceVert)+weight);
		if(weight(sourceVert,destVert)<0){
			this.degrees.set(sourceVert,this.degree(sourceVert)-weight(sourceVert,destVert));
			this.edges.get(sourceVert).put(destVert,0);
		}
	}
	
	/**
	 * Removes an edge from the first vertex to the second
	 */
	public void removeEdge(int sourceVert, int destVert) {
		//this.vertices.get(sourceVert).removeEdge(this.vertices.get(destVert));
		this.removeEdge(sourceVert, destVert, 1);
	}
	
	/**
	 * Removes an edge of the given weight.
	 */
	public void removeEdge(int sourceVert, int destVert, int weight) {
		//this.vertices.get(sourceVert).removeEdge(this.vertices.get(destVert), weight);
		this.addEdge(sourceVert, destVert,-weight);
	}
	
	/**
	 * Retrieves the number of outgoing edges of the given vertex.
	 */
	public int degree(int vert) {
		//return this.vertices.get(vert).degree();
		return this.degrees.get(vert);
	}
	
	/**
	 * Retrieves a list of vertices which the given vertex has outgoing edges to.
	 */
	public Set<Integer> getOutgoingVertices( int vert ){/*
		Set<SandpileVertex> outVerts = this.vertices.get(vert).getOutgoingVertices();
		Set<Integer> outVertIndices = new HashSet<Integer>(outVerts.size());
		for(SandpileVertex v : outVerts) {
			outVertIndices.add(vertices.indexOf(v));
		}
		return outVertIndices;*/
		return edges.get(vert).keySet();
	}

	/**
	 * Returns a set of the indices of vertices that have outgoing edges with the indicated vertex as their destination.
	 * Runs in linear time, proportional to the number of vertices in the graph.
	 */
	public Set<Integer> getIncomingVertices(int vert) {/*
		Set<Integer> incomingVertIndecies = new HashSet<Integer>();
		for(int i=0; i<this.vertices.size(); i++){
			if(this.vertices.get(i).weight(this.vertices.get(vert))>0){
				incomingVertIndecies.add(i);
			}
		}
		return incomingVertIndecies;*/
		Set<Integer> incVerts = new HashSet<Integer>();
		for(int i=0; i<this.edges.size();i++){
			if(weight(i,vert)>0) incVerts.add(i);
		}
		return incVerts;
	}

	/**
	 * Returns true if the indicated vertex has no outgoing vertices, false otherwise.
	 *
	 * @param vertIndex The index of a vertex.
	 */
	public boolean isSink(int vertIndex) {
		//return this.vertices.get(vertIndex).isSink();
		if(degree(vertIndex)==0) return true;
		return false;
	}

	/**
	 * Takes in a configuration and outputs the resulting configuration
	 */
	public SandpileConfiguration updateConfig(SandpileConfiguration config) {
		if(config.size()!=edges.size())
			throw new ArrayIndexOutOfBoundsException
					("Tried to update configuration with a different number of vertices than the graph.");
		SandpileConfiguration newConfig = new SandpileConfiguration(config);
		for(int sourceVert = 0; sourceVert<config.size();sourceVert++){
			if(config.get(sourceVert)>=degree(sourceVert)){
				newConfig.set(sourceVert, newConfig.get(sourceVert)-degree(sourceVert));
				for(Integer destVert : this.getOutgoingVertices(sourceVert)){
					newConfig.set(destVert, newConfig.get(destVert)+weight(sourceVert,destVert));
				}
			}
		}
		return newConfig;
	}

	public SandpileConfiguration reverseFireVertex(SandpileConfiguration config, int vert) {
		SandpileConfiguration newConfig = new SandpileConfiguration(config);
		newConfig.set(vert, config.get(vert)+degree(vert));
		for(Integer w : getOutgoingVertices(vert))
			newConfig.set(w,newConfig.get(w)-weight(vert,w));
		return newConfig;
	}

	public SandpileConfiguration reverseFireConfig(SandpileConfiguration config) {
		SandpileConfiguration newConfig = new SandpileConfiguration(config);
		for(int sourceVert = 0; sourceVert<config.size(); sourceVert++){
			newConfig.set(sourceVert, newConfig.get(sourceVert)+degree(sourceVert));
			for(Integer destVert : this.getOutgoingVertices(sourceVert)){
				newConfig.set(destVert, newConfig.get(destVert)-weight(sourceVert,destVert));
			}
		}
		return newConfig;
	}

	/**
	 * Updates the graph until all vertices stabilize.
	 * WARNING: If the graph does not have a global sink, this function may not end.
	 */
	public SandpileConfiguration stabilizeConfig(SandpileConfiguration config) {
		/*SandpileConfiguration nextConfig = updateConfig(config);
		while(!(nextConfig.equals(config))){
			config = nextConfig;
			nextConfig = updateConfig(config);
		}*/
		SandpileConfiguration newConfig = new SandpileConfiguration(config);
		LinkedHashSet<Integer> unstables1 = getUnstableVertices(config);
		while(!unstables1.isEmpty()){
			LinkedHashSet<Integer> unstables2 = new LinkedHashSet<Integer>();
			//System.err.println("Outer loop");
			for(Integer sourceVert : unstables1){
				//System.err.println("Going to next unstable");
				while(newConfig.get(sourceVert)>=degree(sourceVert)){
					//System.err.println("Stabilizing");
					newConfig.set(sourceVert, newConfig.get(sourceVert)-degree(sourceVert));
					for(Integer destVert : this.getOutgoingVertices(sourceVert)){
						newConfig.set(destVert, newConfig.get(destVert)+weight(sourceVert,destVert));
						if(newConfig.get(destVert)>=degree(destVert) && !isSink(destVert)){
							//System.err.println("Adding new unstable");
							unstables2.add(destVert);
						}
					}
				}
			}
			unstables1 = unstables2;
		}
		return newConfig;
	}


	/**
	 * Returns a set of the indices of the unstable vertices.
	 */
	public LinkedHashSet<Integer> getUnstableVertices(SandpileConfiguration config){
		LinkedHashSet<Integer> unstables = new LinkedHashSet<Integer>();
		for(int vert=0; vert<config.size(); vert++){
			if(config.get(vert)>=this.degree(vert) && !isSink(vert))
				unstables.add(vert);
		}
		return unstables;
	}
        
    /**
     * Returns the number of edges from the first vertex to the second.
     */
    public int weight(int originVert, int destVert) {
        //return vertices.get(originVert).weight( vertices.get(destVert) );
		Integer w = this.edges.get(originVert).get(destVert);
		if(w==null) return 0;
		return w;
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
		SandpileConfiguration config = new SandpileConfiguration(this.degrees.size());
		for(int i=0; i<degrees.size(); i++)
			config.add(amount);
		return config;
	}

	/**
	 * Calculates the maximum stable configuration of the graph, preserving the current configuration.
	 * Runs in O(V).
	 *
	 * @return Returns a list of integers, where getMaxConfig()[i] = the amount of sand on vertex i.
	 */
	public SandpileConfiguration getMaxConfig() {
		SandpileConfiguration maxConfig = new SandpileConfiguration(this.edges.size());
		for(Integer d : degrees){
			maxConfig.add(d-1);
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
		SandpileConfiguration dualConfig = new SandpileConfiguration(this.edges.size());
		for(int i = 0; i<config.size(); i++) {
			dualConfig.add(degree(i)- 1 - config.get(i));
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
		while(w>-1){
			config = reverseFireVertex(config, w);
			w = getInDebtVertex(config);
		}
		return config;
	}

	/**
	 * Gets an arbitrary vertex with negative sand.
	 * @return Returns the index of a vertex.
	 */
	private int getInDebtVertex(SandpileConfiguration config){
		for(int i=0; i<config.size(); i++){
			if(config.get(i)<0&&!this.isSink(i)){
				return i;
			}
		}
		return -1;
	}

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
		SandpileConfiguration config = new SandpileConfiguration(this.degrees.size());
		SandpileConfiguration maxConfig = this.getMaxConfig();
		config = maxConfig.plus(maxConfig);
		config = stabilizeConfig(config);
		config = this.getDualConfig(config);
		config = config.plus(maxConfig);
		return stabilizeConfig(config);
	}
	*/
	public SandpileConfiguration getIdentityConfig() {
		SandpileConfiguration burning = getMinimalBurningConfig();
		SandpileConfiguration config = new SandpileConfiguration(burning);
		SandpileConfiguration nextConfig = stabilizeConfig(config.plus(burning));
		while(!config.equals(nextConfig)){
			config = nextConfig;
			nextConfig = stabilizeConfig(config.plus(burning));
		}
		return config;
	}
	
}
