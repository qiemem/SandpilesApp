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

/**
 *
 * @author headb
 */
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.Iterator;

import java.io.*;
import java.util.ArrayList;

import java.awt.Canvas;
import javax.swing.undo.*;

import java.net.*;

import gnu.trove.TIntArrayList;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Contains all methods used to manipulate graphs and configurations independent
 * of a GUI. Furthermore, this class tracks the positions of all vertices,
 * which vertices are selected, and how many times each vertex has fired.
 * Furthermore, this class contains methods for saving and loading projects, and
 * openning up TCP connections for remote access. Finally, undo maintenance is
 * also contained in this class.
 * 
 * @author Bryan Head
 */
public class SandpileController implements ActionListener, Serializable{

	public static final int SINKS_BORDER = 0;
	public static final int REFLECTIVE_BORDER = 1;
	public static final int NO_BORDER = 2;
	public static final int LOOP_BORDER = 3;
	public static final int LOOP_REVERSE_BORDER = 4;

	public float VERT_RADIUS = 1.0f;
	private long minUpdateDelay = 100;
	private long lastUpdateTime = 0;
	private long minRepaintDelay = 33;
	private long lastRepaintTime = 0;
	private boolean repaintOnEveryUpdate = false;
	private SandpileGraph sg;
	Float2dArrayList vertexData;
	TIntArrayList firings;
	private TIntArrayList selectedVertices;
	private long lastUpdate = System.currentTimeMillis();
	public double fps = 0.0;
	private SandpileConfiguration currentConfig;
	private SandpileDrawer drawer;
	private File projectFile = null;
	private boolean saved = false;
	private HashMap<String, SandpileConfiguration> configs;
	private Iterator<SandpileConfiguration> updater = null;
	public UndoManager undoManager = new UndoManager();

	private ArrayList<SandpileChangeListener> listeners = new ArrayList<SandpileChangeListener>();

	private ServerSocket server;
	private Socket incoming;
	private SandpileProtocol protocol;
	private BufferedReader in;
	private PrintWriter out;

	private boolean needsRepaint = false;
	private ReentrantLock configLock = new ReentrantLock();

	private abstract class SGEdit extends AbstractUndoableEdit {
		//private SandpileGraph oldSG = new SandpileGraph(sg);
		private Float2dArrayList oldLocations = new Float2dArrayList(vertexData);
		private SandpileConfiguration oldCurConfig = new SandpileConfiguration(currentConfig);
		//private HashMap<String, SandpileConfiguration> oldConfigs = new HashMap<String, SandpileConfiguration>(configs);
		//private TIntArrayList oldSelected = new TIntArrayList(selectedVertices.toNativeArray());

		//private SandpileGraph newSG;
		private Float2dArrayList newLocations;
		private SandpileConfiguration newCurConfig;
		//private HashMap<String, SandpileConfiguration> newConfigs;
		//private TIntArrayList newSelected;
		
		private String presentationName;

		public SGEdit(String name){
			presentationName = name;
		}

		@Override public String getPresentationName(){
			return presentationName;
		}

		@Override public void undo(){
			//System.err.println("undo " + getPresentationName());
			//newSG = new SandpileGraph(sg);
			newLocations = new Float2dArrayList(vertexData);
			newCurConfig = new SandpileConfiguration(currentConfig);
//			newConfigs = new HashMap<String, SandpileConfiguration>(configs);
//			newSelected = new TIntArrayList(selectedVertices.toNativeArray());

			undoAction();
			//sg = new SandpileGraph(oldSG);
			vertexData = new Float2dArrayList(oldLocations);
			currentConfig = new SandpileConfiguration(oldCurConfig);
//			configs = new HashMap<String, SandpileConfiguration>(oldConfigs);
//			selectedVertices = new TIntArrayList(oldSelected.toNativeArray());
			onGraphChange();
			onConfigEdit();
			repaint();
		}

		@Override public void redo(){
			//System.err.println("redo " + getPresentationName());
			//sg = newSG;
			redoAction();
			vertexData = newLocations;
//			currentConfig = newCurConfig;
//			configs = newConfigs;
			onGraphChange();
			onConfigEdit();
			repaint();
		}

		public abstract void undoAction();

		public abstract void redoAction();
	}


	/**
	 * Assigns the SandpileGraph and SandpileDrawer to new instances. The SandpileDrawer
	 * is a SandpileGLDrawer and the graph is empty.
	 */
	public SandpileController() {
		drawer = new SandpileGLDrawer();
		initWithSandpileGraph(new SandpileGraph());
	}

	/**
	 * Allows you to set the drawer. Useful if you've already added the canvas.
	 *
	 * via a GUI maker and assigned it a SandpileDrawer.
	 * @param d The SandpileDrawer that will be sent repaint commands from this Controller.
	 */
	public SandpileController(SandpileDrawer d) {
		drawer = d;
		initWithSandpileGraph(new SandpileGraph());
	}

	/**
	 * Allows you to set both the graph and drawer used by this controller.
	 *
	 * @param d The SandpileDrawer that will be sent repaint commands from this controller.
	 * @param sg The SandpileGraph that this controller will edit, use to update configs, and tell the drawer to draw.
	 */
	public SandpileController(SandpileDrawer d, SandpileGraph sg) {
		initWithSandpileGraph(sg);
	}

	/**
	 * Initializes all members. Should be called by any SandpileController constructor.
	 *
	 * @param sg The SandpileGraph that this controller will edit, use to update configs, and tell the drawer to draw.
	 */
	private void initWithSandpileGraph(SandpileGraph sg) {
		//this.curState = ADD_VERT_STATE;
		this.sg = sg;
		vertexData = new Float2dArrayList(0,2);
		firings = new TIntArrayList();
		currentConfig = new SandpileConfiguration();
		selectedVertices = new TIntArrayList();
		configs = new HashMap<String, SandpileConfiguration>();

		Canvas canvas = drawer.getCanvas();
		selectedVertices.clear();
	}

	/**
	 * Changes the drawer that this controller sends repaint commands to.
	 * Useful if you want to display the graph in a different way; e.g.
	 * switch from 2d to 3d. A corresponding switch in canvases will have to happen
	 * elsewhere, if the two different drawers use different canvases.
	 *
	 * @param sd The new drawer that will be sent repaint commands by this
	 * controller.
	 */
	public void setDrawer(SandpileDrawer sd){
		drawer = sd;
	}

	public void addSandpileChangeListener(SandpileChangeListener listener){
		listeners.add(listener);
	}

	/**
	 * Creates a new server socket on the specified port.
	 * @param port The port to listen on.
	 * @throws IOException Throws this exception if the connection is
	 * if the method fails to create a new ServerSocket.
	 */
	public void startServer(int port) throws IOException{
		server = new ServerSocket(port, 5);
		//System.err.println("Created server socket");
		protocol = new SandpileProtocol(this);
	}
	/**
	 * Tells the controller to wait and listen for a TCP connection request on
	 * the specified port. This method will return only when a request is made.
	 * You must call startServer() before calling this method.
	 * @throws IOException Throws this exception if the connection fails or
	 * if it fails to create an input or output stream.
	 */
	public void acceptClient() throws IOException{
		incoming = server.accept();
		//System.err.println("Accepted incoming socket");
		in = new BufferedReader(new InputStreamReader(incoming.getInputStream()));
		out = new PrintWriter(incoming.getOutputStream(), true);
	}

	/**
	 * Looks to see if there are any messages have been sent to the server
	 * created by startServer(). The messages must end with a newline character
	 * or else they will not be recognized as complete messages. If there is not
	 * a message that ends in a newline character, this method will simply
	 * return. You must call startServer() and acceptClient() successfully in
	 * order to use this method.
	 * @return Returns the next message waiting if there is one or null.
	 * @throws IOException Throws this exception if there is a problem reading
	 * from the input stream.
	 */
	public String checkForMessage() throws IOException{
		if(in.ready())
			return in.readLine();
		else
			return null;
	}

	/**
	 * Checks to see if any messages have been sent to the server and, if so,
	 * acts appropriately by interpretting the message with an instance of
	 * SandpileProtocol. You must call startServer() and acceptClient()
	 * succesfully before using this method. This method should be called at
	 * regular intervals for correct server behavior. If there are no
	 * messages waiting, the method will simply return.
	 * @throws IOException Throws this exception if there is a problem reading
	 * from the input stream or writing to the output stream.
	 */
	public void receiveMessage() throws IOException{
		String msg = null;
		try{
			msg = checkForMessage();
		}catch(Exception e){
			out.println(e.getMessage());
		}
		if(msg!=null){
			String response = "";
			try{
				response = protocol.processInput(msg);
			}catch(Exception e){
				e.printStackTrace();
				response = e.getMessage();
			}
			out.println(response);

		}
	}

	/**
	 * Closes the connection created by startServer() and closes the
	 * corresponding input and output streams. I don't think a connection
	 * actually has to be open to call this method however.
	 * @throws IOException Throw this exception if there is a problem closing
	 * the connection or the input/output streams.
	 */
	public void stopServer() throws IOException{
		if(incoming!=null){
			incoming.close();
			incoming=null;
		}
		in = null;
		out = null;
		protocol = null;
		server.close();
	}

	/**
	 * Looks to see if there is a project file associated with the current graph
	 * and returns it. Project files are just folders containing a graph.sg
	 * file.
	 * @return Returns the project name if there is one or "Untitled".
	 */
	public String getProjectTitle() {
		String title;
		if (projectFile != null) {
			title = projectFile.getName();
		} else {
			title = "Untitled";
		}
		if (!saved) {
			title += " *";
		}
		return title;
	}

	/**
	 * Returns the size of the current config, which should always be number of
	 * vertices.
	 * @return The size of the current config.
	 */
	public int configSize(){
		return currentConfig.size();
	}

	/**
	 * Performs a repaint/update cycle if the appropriate amount of time has
	 * passed since the last rapint/update. This method is useful for hooking
	 * this controller up to a javax.swing.Timer or something similar.
	 * @param evt The ActionEvent that triggers this method.
	 * @see setMinUpdateDelay(), setMinRepaintDelay(), setRepaintOnEveryUpdate()
	 */
	public void actionPerformed(ActionEvent evt) {
		if(repaintOnEveryUpdate){
			if (System.currentTimeMillis() - lastUpdateTime >= minUpdateDelay) {
				lastUpdateTime = System.currentTimeMillis();
				this.update();
				this.repaint();
			}
		}else{
			if (System.currentTimeMillis() - lastUpdateTime >= minUpdateDelay) {
				lastUpdateTime = System.currentTimeMillis();
				this.update();
				needsRepaint = true;
			}
			if (System.currentTimeMillis() - lastRepaintTime >= minRepaintDelay && needsRepaint) {
				lastRepaintTime = System.currentTimeMillis();
				this.repaint();
				needsRepaint = false;
			}
		}
	}

	/**
	 * Updated firing counts (by checking for unstables) and fires all unstable
	 * vertices. This method uses the iterator returned by
	 * SandpileGraph.inPlaceParallelUpdater() to update the current config in place. It
	 * will only remake the iterator if it is null. Thus, it is the
	 * responsibility of the methods that change the graph or config to set the
	 * iterator to null; the onEdit() method does this. This method will only
	 * update if the iterator's hasNext() returns true. Hence, no unnecessary
	 * update cycles will be performed.
	 */
	public void update() {
		configLock.lock();
		updateFirings();
		if (updater == null) {
			updater = sg.inPlaceParallelUpdater(currentConfig);
		}
		if (updater.hasNext()) {
			updater.next();
			onConfigChange();
		}
		configLock.unlock();
	}

	/**
	 * Resets all firing counts to 0.
	 */
	public void resetFirings() {
		firings = new TIntArrayList();
		int size = configSize();
		for (int i=0; i<size; i++) {
			firings.add(0);
		}
	}

	/**
	 * Updates the firing counts by looking up the unstable vertices.
	 */
	public void updateFirings() {
		int s = configSize();
		if (firings.size() != s) {
			resetFirings();
		}
		for (int vert = 0; vert < s; vert++) {
			int d = sg.degreeQuick(vert);
			if (currentConfig.getQuick(vert) >= d && d!=0) {
				firings.setQuick(vert, firings.getQuick(vert) + 1);
			}
		}
	}

	public int getFirings(int v){
		return firings.get(v);
	}

	public TIntArrayList getFirings() {
		return firings;
	}

	/**
	 * Calls this object's SandpileDrawer's paintSandpileGraph() method with
	 * this object's current data.
	 */
	public void repaint() {
		drawer.paintSandpileGraph(sg, vertexData, currentConfig, firings, selectedVertices);
	}

	/**
	 * Should be called by any public method that changes the graph. Calls
	 * onEdit() and clearVertexDependentConfigs();
	 */
	public void onGraphChange(){
		onEdit();
		clearVertexDependentConfigs();

		for(SandpileChangeListener listener: listeners){
			listener.onGraphChange(sg);
		}
	}

	/**
	 * Should be called by any public method that changes the current config.
	 * Calls onEdit() and onConfigChange().
	 */
	public void onConfigEdit() {
		onConfigChange();
		onEdit();
	}

	/**
	 * Indicates the current configuration has been changed. Does NOT call
	 * onEdit(). Hence, unless the change is made by update(), onEdit() should
	 * probably also be called (to reset the updater iterator).
	 */
	public void onConfigChange() {
		for(SandpileChangeListener listener: listeners){
			listener.onConfigChange(this.getConfig());
		}
	}

	/**
	 * Removes any stored configs that depend on the edges of the graph. This
	 * includes the identity and minimum burning configs by default.
	 */
	public void clearEdgeDependentConfigs() {
		configs.remove("Identity");
		configs.remove("Burning");
	}

	/**
	 * Removes any stored config that depend on the vertices of the graph. By
	 * default, this is all stored configs.
	 */
	public void clearVertexDependentConfigs() {
		configs.clear();
	}

	/**
	 * Ensures the the current project no longer registers as saved and sets the
	 * updater iterator is set to null.
	 */
	public void onEdit() {
		saved = false;
		updater = null;
	}

	/**
	 * Set the current config to the given config.
	 * @param config The new current config.
	 * @throws IndexOutOfBoundsException Throws this exception if the given
	 * config is a different size than the current config.
	 */
	public void setConfig(SandpileConfiguration config){
		configLock.lock();
		if(config.size() == configSize()){
			currentConfig = config;
			onConfigEdit();
		}else
			throw new IndexOutOfBoundsException("Tried to set the current sandpile " +
					"configuration to a configuration of an incorrect size. The correct" +
					" size is "+ configSize()+" while the new configuration had size" +
					config.size()+ ".");
		configLock.unlock();
	}
	/**
	 * Sets the current config to the given config plus the current config.
	 * @param config The config to add to the current config.
	 * @throws IndexOutOfBoundsException Throws this exception if the given
	 * config is a different size than the current config.
	 */
	public void addConfig(SandpileConfiguration config){
		configLock.lock();
		if(config.size() == configSize()){
			currentConfig.plusEquals(config);
			onConfigEdit();
		}else
			throw new IndexOutOfBoundsException("Tried to add the current sandpile " +
					"configuration to a configuration of an incorrect size. The correct" +
					" size is "+ configSize()+" while the new configuration had size" +
					config.size()+ ".");
		configLock.unlock();
	}

	/**
	 * Adds a vertex at the given coordinates, as long as the given coordinates
	 * don't touch any other vertex. Triggers onGraphChange().
	 * @param x The x coordinate of the new vertex.
	 * @param y The y coordinate of the new vertex.
	 */
	public void addVertexControl(final float x, final float y) {
		int touchVert = touchingVertex(x, y);
		if (touchVert < 0) {
			final int vertIndex = configSize();
			undoManager.addEdit(new SGEdit("add vertex"){
				@Override public void undoAction() {
					delVertex(vertIndex);
				}
				@Override public void redoAction() {
					addVertex(x, y);
				}
			});
			addVertex(x,y);
			onGraphChange();
			repaint();
		}
	}

	/**
	 * Deletes the most recently placed vertex that touchs the points (x,y). If
	 * no vertex is touching (x,y), this method does nothing. Triggers
	 * onGraphChange().
	 * @param x The x coordinate.
	 * @param y The y coordinate.
	 */
	public void delVertexControl(final float x, final float y) {
		final SandpileGraph oldSG = new SandpileGraph(sg);
		final int touchVert = touchingVertex(x, y);
		if (touchVert >= 0){
			undoManager.addEdit(new SGEdit("delete vertex"){
				@Override public void undoAction() {
					unselectVertices();
					sg = new SandpileGraph(oldSG);
				}
				@Override public void redoAction() {
					delVertex(touchVert);
				}
			});
			delVertex(touchVert);
			onGraphChange();
			unselectVertices();
		}
		repaint();
	}

	/**
	 * Adds an starting from each selected vertex to the most recently placed
	 * vertex that touches the point (x,y) of the specified weight. If an edge
	 * already exists this will increase the weight of the edge by the given
	 * weight. If no vertex touches (x,y), this method does nothing. Triggers
	 * onGraphChange().
	 * @param x The x coordinate.
	 * @param y The y coordinate.
	 * @param weight The weight of the new edges.
	 */
	public void addEdgeControl(float x, float y, final int weight) {

		final int touchVert = touchingVertex(x, y);
		if (touchVert >= 0  && !selectedVertices.isEmpty()) {
			final TIntArrayList sourceVerts = new TIntArrayList(selectedVertices.toNativeArray());
			undoManager.addEdit(new SGEdit("add edge(s)"){
				@Override
				public void undoAction() {
					for (int i = 0; i<sourceVerts.size(); i++) {
						int v = sourceVerts.get(i);
						delEdge(v, touchVert, weight);
					}
				}
				@Override
				public void redoAction() {
					for (int i = 0; i < sourceVerts.size(); i++) {
						int v = sourceVerts.get(i);
						addEdge(v, touchVert, weight);
					}
				}
			});
			for (int i = 0; i<selectedVertices.size(); i++) {
				int v = selectedVertices.get(i);
				addEdge(v, touchVert, weight);
			}
			onGraphChange();
		}
		repaint();
	}

	final private ArrayList<SingleSourceEdgeList> storeOutgoingEdgeData(TIntArrayList verts){
		final ArrayList<SingleSourceEdgeList> edgeLists = new ArrayList<SingleSourceEdgeList>(selectedVertices.size());
		for (int i = 0; i < selectedVertices.size(); i++) {
			int v = selectedVertices.getQuick(i);
			edgeLists.add(new SingleSourceEdgeList(getGraph().getOutgoingEdges(v),v));
		}
		return edgeLists;
	}

	final private void restoreOutgoingEdgeData(ArrayList<SingleSourceEdgeList> edgeLists){
		for (int i = 0; i < edgeLists.size(); i++) {
			getGraph().setOutgoingEdges(edgeLists.get(i).source(), new SingleSourceEdgeList(edgeLists.get(i)));
		}
	}

	/**
	 * Calling delEdgeControl(x,y,weight) is the same as
	 * addEdgeControl(x,y,-weight). If an edge weight goes to zero or less,
	 * it will simply be removed.
	 * @param x The x coordinate.
	 * @param y The y coordinate
	 * @param weight The amount of weight to take away from the edges.
	 */
	public void delEdgeControl(float x, float y, final int weight) {
		final int touchVert = touchingVertex(x, y);
		if (touchVert >= 0 && !selectedVertices.isEmpty()) {

			//Store the current outgoing edges of all selected vertices
			//should they be restored later.
			final ArrayList<SingleSourceEdgeList> edgeLists = storeOutgoingEdgeData(selectedVertices);
			undoManager.addEdit(new SGEdit("delete edge(s)"){
				@Override
				public void undoAction() {
					restoreOutgoingEdgeData(edgeLists);
				}
				@Override
				public void redoAction() {
					for (int i = 0; i < edgeLists.size(); i++) {
						int v = edgeLists.get(i).source();
						delEdge(v, touchVert, weight);
					}
				}
			});
			for (int i = 0; i<selectedVertices.size(); i++) {
				int v = selectedVertices.get(i);
				delEdge(v, touchVert, weight);
			}
			onGraphChange();
		}
		repaint();
	}

	public void makeSink(TIntArrayList verts){
		if(verts.isEmpty())
			return;
		
		final ArrayList<SingleSourceEdgeList> edgeLists = storeOutgoingEdgeData(selectedVertices);
		SGEdit theEdit = new SGEdit("make sink(s)"){
			@Override
			public void undoAction() {
				restoreOutgoingEdgeData(edgeLists);
			}
			@Override
			public void redoAction() {
				for(int i=0; i<edgeLists.size(); i++){
					int v = edgeLists.get(i).source();
					getGraph().setOutgoingEdges(v, new SingleSourceEdgeList(v));
				}
			}
		};

		undoManager.addEdit(theEdit);

		theEdit.redoAction();
		onGraphChange();
		repaint();
	}

	public void addUndiEdgeControl(float x, float y, final int weight) {
		final int touchVert = touchingVertex(x, y);
		if (touchVert >= 0 && !selectedVertices.isEmpty()) {
			final TIntArrayList otherVerts = new TIntArrayList(selectedVertices.toNativeArray());
			SGEdit theEdit = new SGEdit("add undirected edges"){
				@Override
				public void undoAction() {
					for(int i = 0; i<otherVerts.size(); i++){
						int v = otherVerts.get(i);
						delEdge(v, touchVert, weight);
						delEdge(touchVert, v, weight);
					}
				}
				@Override
				public void redoAction() {
					for(int i = 0; i<otherVerts.size(); i++){
						int v = otherVerts.get(i);
						addEdge(v, touchVert, weight);
						addEdge(touchVert, v, weight);
					}
				}
			};
			undoManager.addEdit(theEdit);
			theEdit.redoAction();
			onGraphChange();
		}
		repaint();
	}

	public void delUndiEdgeControl(float x, float y, final int weight) {
		final int touchVert = touchingVertex(x, y);
		if (touchVert >= 0 && !selectedVertices.isEmpty()) {
			final TIntArrayList otherVerts = new TIntArrayList(selectedVertices.toNativeArray());
			final ArrayList<SingleSourceEdgeList> edgeLists = storeOutgoingEdgeData(otherVerts);
			final SingleSourceEdgeList touchVertEdges = new SingleSourceEdgeList(getGraph().getOutgoingEdges(touchVert),touchVert);
			SGEdit theEdit = new SGEdit("delete undirected edge(s)"){
				@Override
				public void undoAction() {
					restoreOutgoingEdgeData(edgeLists);
					getGraph().setOutgoingEdges(touchVert, new SingleSourceEdgeList(touchVertEdges));
				}

				@Override
				public void redoAction() {
					for (int i = 0; i < selectedVertices.size(); i++) {
						int v = selectedVertices.get(i);
						delEdge(v, touchVert, weight);
						delEdge(touchVert, v, weight);
					}
				}
			};
			undoManager.addEdit(theEdit);
			theEdit.redoAction();
			onGraphChange();
		}
		repaint();
	}

	public void addSandControl(float x, float y, int amount) {
		int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			addSand(touchVert, amount);
			onConfigEdit();
		}
		repaint();
	}

	public void setSandControl(float x, float y, int amount) {
		int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			setSand(touchVert, amount);
			onConfigEdit();
		}
		repaint();
	}


	public void makeGridControl(final int rows, final int cols,
			final float x, final float y,
			final int nBorder, final int sBorder, final int eBorder, final int wBorder) {

		SGEdit theEdit = new SGEdit("make grid"){
			private int startIndex, endSize;
			@Override
			public void undoAction(){
				TIntArrayList verts = new TIntArrayList();
				for(int i=startIndex; i<endSize; i++)
					verts.add(i);
				delVertices(verts);
			}

			@Override
			public void redoAction() {
				startIndex = configSize();
				makeGrid(rows, cols, x, y, nBorder, sBorder, eBorder, wBorder);
				endSize = configSize();
			}
		};
		undoManager.addEdit(theEdit);
		theEdit.redoAction();
		onGraphChange();
		repaint();
	}

	public void makeGrid(int rows, int cols, float x, float y, int nBorder, int sBorder, int eBorder, int wBorder){
		float gridSpacing = VERT_RADIUS * 2;
		//int curVertDataSize = vertexData.size();
		int[][] gridRef = new int[rows][cols];
		int[] nBorderRef = new int[cols];
		int[] sBorderRef = new int[cols];
		int[] eBorderRef = new int[rows];
		int[] wBorderRef = new int[rows];

		// create vertices
		// Note that we try to create the vertices in a row-by-row order as this
		// helps the SandileGraph identify common structures. It is not
		// necessary by any means, just more efficient.
		for (int i = 0; i < cols; i++) {
			if (nBorder == SINKS_BORDER || nBorder == REFLECTIVE_BORDER) {
				nBorderRef[i] = configSize();
				addVertex(x + i * gridSpacing, y + gridSpacing);
			}
		}
		for (int i = 0; i < rows; i++) {
			if (wBorder == SINKS_BORDER || wBorder == REFLECTIVE_BORDER) {
				wBorderRef[i] = configSize();
				addVertex(x - gridSpacing, y - i * gridSpacing);
			}
			for (int j = 0; j < cols; j++) {
				gridRef[i][j] = configSize();
				addVertex(x + j * gridSpacing, y - i * gridSpacing);
			}
			if (eBorder == SINKS_BORDER || eBorder == REFLECTIVE_BORDER) {
				eBorderRef[i] = configSize();
				addVertex(x + (cols) * gridSpacing, y - i * gridSpacing);
			}
		}

		for (int i = 0; i < cols; i++) {
			if (sBorder == SINKS_BORDER || sBorder == REFLECTIVE_BORDER) {
				sBorderRef[i] = configSize();
				addVertex(x + i * gridSpacing, y - (rows) * gridSpacing);
			}

		}
		//create edges
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				SingleSourceEdgeList edges = new SingleSourceEdgeList(gridRef[i][j]);
				if (i == 0) {
					switch(nBorder){
						case SINKS_BORDER:
							edges.add(nBorderRef[j],1);
							//addEdge(gridRef[i][j], nBorderRef[j], 1);
							break;
						case REFLECTIVE_BORDER:
							edges.add(nBorderRef[j],1);
							//addEdge(gridRef[i][j], nBorderRef[j], 1);
							addEdge(nBorderRef[j], gridRef[i][j], 1);
							break;
						case LOOP_BORDER:
							//addEdge(gridRef[i][j], gridRef[rows-1][j], 1);
							edges.add(gridRef[rows-1][j], 1);
							break;
						case LOOP_REVERSE_BORDER:
							edges.add(gridRef[rows-1][cols-1 - j], 1);
							break;
					}
				} else {
					edges.add(gridRef[i - 1][j],1);
				}

				if (i == rows - 1) {
					switch(sBorder){
						case SINKS_BORDER:
							edges.add(sBorderRef[j], 1);
							break;
						case REFLECTIVE_BORDER:
							edges.add(sBorderRef[j], 1);
							addEdge(sBorderRef[j], gridRef[i][j], 1);
							break;
						case LOOP_BORDER:
							edges.add(gridRef[0][j], 1);
							break;
						case LOOP_REVERSE_BORDER:
							edges.add(gridRef[0][cols-1-j], 1);
							break;
					}
				} else {
					edges.add(gridRef[i + 1][j],1);
				}
				if (j == cols - 1) {
					switch(eBorder){
						case SINKS_BORDER:
							edges.add(eBorderRef[i], 1);
							break;
						case REFLECTIVE_BORDER:
							edges.add(eBorderRef[i], 1);
							addEdge(eBorderRef[i], gridRef[i][j], 1);
							break;
						case LOOP_BORDER:
							edges.add(gridRef[i][0], 1);
							break;
						case LOOP_REVERSE_BORDER:
							edges.add(gridRef[rows-1-i][0], 1);
							break;
					}
				} else {
					edges.add(gridRef[i][j + 1],1);
				}

				if (j == 0) {
					switch(wBorder){
						case SINKS_BORDER:
							edges.add(wBorderRef[i], 1);
							break;
						case REFLECTIVE_BORDER:
							edges.add(wBorderRef[i], 1);
							addEdge(wBorderRef[i], gridRef[i][j], 1);
							break;
						case LOOP_BORDER:
							edges.add(gridRef[i][cols-1], 1);
							break;
						case LOOP_REVERSE_BORDER:
							edges.add(gridRef[rows-1-i][cols-1], 1);
							break;
					}
				} else {
					edges.add(gridRef[i][j - 1],1);
				}
				getGraph().setOutgoingEdges(gridRef[i][j], edges);
			}
		}
	}

	public void makeHoneycombControl(final int radius, final float x, final float y, final int borders) {
		SGEdit theEdit = new SGEdit("make grid"){
			private int startIndex, endSize;
			@Override
			public void undoAction(){
				TIntArrayList verts = new TIntArrayList();
				for(int i=startIndex; i<endSize; i++)
					verts.add(i);
				delVertices(verts);
			}

			@Override
			public void redoAction() {
				startIndex = configSize();
				makeHoneycomb(radius, x, y, borders);;
				endSize = configSize();
			}
		};
		undoManager.addEdit(theEdit);
		theEdit.redoAction();
		
		onGraphChange();
		this.repaint();

	}

	public void makeHoneycomb(final int radius, final float x, final float y, final int borders){
		float gridSpacing = VERT_RADIUS * 2;
		int curRowLength = radius;
		int[][] gridRef = new int[radius * 2 - 1][radius * 2 - 1];
		for (int i = 0; i < radius * 2 - 1; i++) {
			for (int j = 0; j < curRowLength; j++) {
				gridRef[i][j] = configSize();
				addVertex(x + j * gridSpacing + (i + (radius - 1) % 2) % 2 * (gridSpacing / 2) - curRowLength / 2 * (gridSpacing), y - i * (gridSpacing * 5f / 6f));
			}
			if (i < radius - 1) {
				curRowLength++;
			} else {
				curRowLength--;
			}
		}
		curRowLength = radius;
		for (int i = 0; i < radius * 2 - 1; i++) {
			if (i == 0 || i == radius * 2 - 2) {
				continue;
			}
			for (int j = 0; j < curRowLength; j++) {
				if (j == 0) {
					continue;
				}
				addEdge(gridRef[i][j], gridRef[i][j - 1]);
				addEdge(gridRef[i][j], gridRef[i][j + 1]);
				if (i < radius - 1) {
					addEdge(gridRef[i][j], gridRef[i - 1][j - 1]);
					addEdge(gridRef[i][j], gridRef[i - 1][j]);
					addEdge(gridRef[i][j], gridRef[i + 1][j + 1]);
					addEdge(gridRef[i][j], gridRef[i + 1][j]);
				} else if (i == radius - 1) {
					addEdge(gridRef[i][j], gridRef[i - 1][j - 1]);
					addEdge(gridRef[i][j], gridRef[i - 1][j]);
					addEdge(gridRef[i][j], gridRef[i + 1][j - 1]);
					addEdge(gridRef[i][j], gridRef[i + 1][j]);
				} else {
					addEdge(gridRef[i][j], gridRef[i - 1][j + 1]);
					addEdge(gridRef[i][j], gridRef[i - 1][j]);
					addEdge(gridRef[i][j], gridRef[i + 1][j - 1]);
					addEdge(gridRef[i][j], gridRef[i + 1][j]);
				}

			}
			if (i < radius - 1) {
				curRowLength++;
			} else {
				curRowLength--;
			}
		}
		if (borders == 1) {
			for (int i = 0; i < radius; i++) {
				addEdge(gridRef[0][i], gridRef[1][i]);
				addEdge(gridRef[0][i], gridRef[1][i + 1]);
				addEdge(gridRef[2 * radius - 2][i], gridRef[2 * radius - 3][i]);
				addEdge(gridRef[2 * radius - 2][i], gridRef[2 * radius - 3][i + 1]);
				if (i > 0) {
					addEdge(gridRef[0][i], gridRef[0][i - 1]);
					addEdge(gridRef[2 * radius - 2][i], gridRef[2 * radius - 2][i - 1]);
				}
				if (i < radius - 1) {
					addEdge(gridRef[0][i], gridRef[0][i + 1]);
					addEdge(gridRef[2 * radius - 2][i], gridRef[2 * radius - 2][i + 1]);
				}
			}
			for (int i = 1; i < radius - 1; i++) {
				addEdge(gridRef[i][0], gridRef[i - 1][0]);
				addEdge(gridRef[i][0], gridRef[i + 1][0]);
				addEdge(gridRef[i][0], gridRef[i][1]);
				addEdge(gridRef[i][0], gridRef[i + 1][1]);

				addEdge(gridRef[radius + i - 1][0], gridRef[radius + i][0]);
				addEdge(gridRef[radius + i - 1][0], gridRef[radius + i - 2][0]);
				addEdge(gridRef[radius + i - 1][0], gridRef[radius + i - 1][1]);
				addEdge(gridRef[radius + i - 1][0], gridRef[radius + i - 2][1]);

				addEdge(gridRef[i][radius + i - 1], gridRef[i - 1][radius + i - 2]);
				addEdge(gridRef[i][radius + i - 1], gridRef[i + 1][radius + i]);
				addEdge(gridRef[i][radius + i - 1], gridRef[i][radius + i - 2]);
				addEdge(gridRef[i][radius + i - 1], gridRef[i + 1][radius + i - 1]);

				addEdge(gridRef[radius + i - 1][2 * radius - i - 2], gridRef[radius + i - 2][2 * radius - i - 1]);
				addEdge(gridRef[radius + i - 1][2 * radius - i - 2], gridRef[radius + i][2 * radius - i - 3]);
				addEdge(gridRef[radius + i - 1][2 * radius - i - 2], gridRef[radius + i - 1][2 * radius - i - 3]);
				addEdge(gridRef[radius + i - 1][2 * radius - i - 2], gridRef[radius + i - 2][2 * radius - i - 2]);
			}
			addEdge(gridRef[radius - 1][0], gridRef[radius - 2][0]);
			addEdge(gridRef[radius - 1][0], gridRef[radius][0]);
			addEdge(gridRef[radius - 1][0], gridRef[radius - 1][1]);

			addEdge(gridRef[radius - 1][2 * radius - 2], gridRef[radius - 2][2 * radius - 3]);
			addEdge(gridRef[radius - 1][2 * radius - 2], gridRef[radius][2 * radius - 3]);
			addEdge(gridRef[radius - 1][2 * radius - 2], gridRef[radius - 1][2 * radius - 3]);
		}
	}

	public void makeHexGridControl(final int rows, final int cols,
			final float x, final float y,
			final int nBorder, final int sBorder, final int eBorder, final int wBorder) {
		SGEdit theEdit = new SGEdit("make grid"){
			private int startIndex, endSize;
			@Override
			public void undoAction(){
				TIntArrayList verts = new TIntArrayList();
				for(int i=startIndex; i<endSize; i++)
					verts.add(i);
				delVertices(verts);
			}

			@Override
			public void redoAction() {
				startIndex = configSize();
				makeHexGrid(rows, cols, x, y, nBorder, sBorder, eBorder, wBorder);
				endSize = configSize();
			}
		};
		undoManager.addEdit(theEdit);
		theEdit.redoAction();
		onGraphChange();
		this.repaint();
	}

	public void makeHexGrid(final int rows, final int cols,
			final float x, final float y,
			final int nBorder, final int sBorder, final int eBorder, final int wBorder){
		float gridSpacing = VERT_RADIUS * 2;
		//int curVertDataSize = vertexData.size();

		int[][] gridRef = new int[rows][cols];
		int[] nBorderRef = new int[cols + 1];
		int[] sBorderRef = new int[cols + 1];
		int[] eBorderRef = new int[rows];
		int[] wBorderRef = new int[rows];

		//create vertices
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				gridRef[i][j] = configSize();
				addVertex(x + j * gridSpacing + i % 2 * (gridSpacing / 2), y - i * gridSpacing);
			}
		}

		for (int i = 0; i < cols + 1; i++) {
			if (nBorder < 2) {
				nBorderRef[i] = configSize();
				addVertex(x + i * gridSpacing - (gridSpacing / 2), y + gridSpacing);
			}
			if (sBorder < 2) {
				sBorderRef[i] = configSize();
				addVertex(x + i * gridSpacing - (gridSpacing / 2), y - (rows) * gridSpacing);
			}

		}

		for (int i = 0; i < rows; i++) {
			if (wBorder < 2) {
				wBorderRef[i] = configSize();
				addVertex(x - gridSpacing + i % 2 * (gridSpacing / 2), y - i * gridSpacing);
			}
			if (eBorder < 2) {
				eBorderRef[i] = configSize();
				addVertex(x + (cols) * gridSpacing + i % 2 * (gridSpacing / 2), y - i * gridSpacing);
			}
		}

		//create edges
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				if (i % 2 == 0) {
					if (i == 0) {
						if (nBorder == 0) {
							addEdge(gridRef[i][j], nBorderRef[j], 1);
							addEdge(gridRef[i][j], nBorderRef[j + 1], 1);
						} else if (nBorder == 1) {
							addEdge(gridRef[i][j], nBorderRef[j], 1);
							addEdge(nBorderRef[j], gridRef[i][j], 1);
							addEdge(gridRef[i][j], nBorderRef[j + 1], 1);
							addEdge(nBorderRef[j + 1], gridRef[i][j], 1);
						}
					} else {
						addEdge(gridRef[i][j], gridRef[i - 1][j]);
						if (j == 0) {
							if (wBorder == 0) {
								addEdge(gridRef[i][j], wBorderRef[i - 1]);
							} else if (wBorder == 2) {
								addEdge(gridRef[i][j], wBorderRef[i - 1]);
								addEdge(wBorderRef[i - 1], gridRef[i][j]);
							}
						} else {

							addEdge(gridRef[i][j], gridRef[i - 1][j - 1]);
						}
					}
					if (i == rows - 1) {
						if (sBorder == 0) {
							addEdge(gridRef[i][j], sBorderRef[j], 1);
							addEdge(gridRef[i][j], sBorderRef[j + 1], 1);
						} else if (sBorder == 1) {
							addEdge(gridRef[i][j], sBorderRef[j], 1);
							addEdge(sBorderRef[j], gridRef[i][j], 1);
							addEdge(gridRef[i][j], sBorderRef[j + 1], 1);
							addEdge(sBorderRef[j + 1], gridRef[i][j], 1);
						}
					} else {
						addEdge(gridRef[i][j], gridRef[i + 1][j]);
						if (j == 0) {

							if (wBorder == 0) {
								addEdge(gridRef[i][j], wBorderRef[i + 1]);
							} else if (wBorder == 2) {
								addEdge(gridRef[i][j], wBorderRef[i + 1]);
								addEdge(wBorderRef[i + 1], gridRef[i][j]);
							}
						} else {
							addEdge(gridRef[i][j], gridRef[i + 1][j - 1]);
						}
					}
				} else {
					if (i == rows - 1) {
						if (sBorder == 0) {
							addEdge(gridRef[i][j], sBorderRef[j], 1);
							addEdge(gridRef[i][j], sBorderRef[j + 1], 1);
						} else if (sBorder == 1) {
							addEdge(gridRef[i][j], sBorderRef[j], 1);
							addEdge(sBorderRef[j], gridRef[i][j], 1);
							addEdge(gridRef[i][j], sBorderRef[j + 1], 1);
							addEdge(sBorderRef[j + 1], gridRef[i][j], 1);
						}

					} else {
						if (j == cols - 1) {

							if (eBorder == 0) {
								addEdge(gridRef[i][j], eBorderRef[i + 1]);
							} else if (eBorder == 2) {
								addEdge(gridRef[i][j], eBorderRef[i + 1]);
								addEdge(eBorderRef[i + 1], gridRef[i][j]);

							}
						} else {
							addEdge(gridRef[i][j], gridRef[i + 1][j + 1]);
						}
						addEdge(gridRef[i][j], gridRef[i + 1][j]);
					}
					if (j == cols - 1) {

						if (eBorder == 0) {
							addEdge(gridRef[i][j], eBorderRef[i - 1]);
						} else if (eBorder == 2) {
							addEdge(gridRef[i][j], eBorderRef[i - 1]);
							addEdge(eBorderRef[i - 1], gridRef[i][j]);

						}
					} else {
						addEdge(gridRef[i][j], gridRef[i - 1][j + 1]);
					}
					addEdge(gridRef[i][j], gridRef[i - 1][j]);
				}
				if (j == cols - 1) {
					if (eBorder == 0) {
						addEdge(gridRef[i][j], eBorderRef[i], 1);
					} else if (eBorder == 1) {
						addEdge(gridRef[i][j], eBorderRef[i], 1);
						addEdge(eBorderRef[i], gridRef[i][j], 1);
					}
				} else {
					addEdge(gridRef[i][j], gridRef[i][j + 1]);
				}

				if (j == 0) {
					if (wBorder == 0) {
						addEdge(gridRef[i][j], wBorderRef[i], 1);
					} else if (wBorder == 1) {
						addEdge(gridRef[i][j], wBorderRef[i], 1);
						addEdge(wBorderRef[i], gridRef[i][j], 1);
					}
				} else {
					addEdge(gridRef[i][j], gridRef[i][j - 1]);
				}

			}
		}
	}

	public void buildLatticeControl(final float xCoord, final float yCoord, final int rows, final int cols,
			final int spacing, final List<int[]> vectors,
			final TIntArrayList xStartingWith, final TIntArrayList xFreq, final TIntArrayList yStartingWith, final TIntArrayList yFreq,
			final List<Boolean> directed, final TIntArrayList weight, final TIntArrayList borders){
		SGEdit theEdit = new SGEdit("make grid"){
			private int startIndex, endSize;
			@Override
			public void undoAction(){
				TIntArrayList verts = new TIntArrayList();
				for(int i=startIndex; i<endSize; i++)
					verts.add(i);
				delVertices(verts);
			}

			@Override
			public void redoAction() {
				startIndex = configSize();
				buildLattice(xCoord, yCoord, rows, cols, spacing, vectors, xStartingWith, xFreq, yStartingWith, yFreq, directed, weight, borders);
				endSize = configSize();
			}
		};
		undoManager.addEdit(theEdit);
		theEdit.redoAction();
		onGraphChange();
		this.repaint();
	}

	protected void buildLattice(final float xCoord, final float yCoord, final int rows, final int cols,
			final int spacing, final List<int[]> vectors,
			final TIntArrayList xStartingWith, final TIntArrayList xFreq, final TIntArrayList yStartingWith, final TIntArrayList yFreq,
			final List<Boolean> directed, final TIntArrayList weight, final TIntArrayList borders) {
		float gridSpacing = VERT_RADIUS * (spacing) * 2f;
		int[][] gridRef = new int[rows][cols];

		//create vertices
		for (int x = 0; x < cols; x++) {
			for (int y = 0; y < rows; y++) {
				gridRef[x][y] = configSize();
				addVertex(xCoord + x * gridSpacing, yCoord + y * gridSpacing);
			}
		}

				for(int i=0; i<vectors.size(); i++){
		for(int x=0; x<cols; x++){
			for(int y=0; y<rows; y++){
					int[] vec = vectors.get(i);
					int w = weight.get(i);
					boolean d = directed.get(i);
					int xs = xStartingWith.get(i);
					int xf = xFreq.get(i);
					int ys = yStartingWith.get(i);
					int yf = yFreq.get(i);
					int b = borders.get(i);
					if(xf==0 && x == xs && yf == 0 && y == ys && x+vec[0]<cols && y+vec[1]<rows && x+vec[0]>=0 && y+vec[1]>=0){
						if (!d)
							addEdge(gridRef[x + vec[0]][y + vec[1]], gridRef[x][y], w);
						addEdge(gridRef[x][y], gridRef[x+vec[0]][y+vec[1]],w);
					}else if (xf == 0 && x == xs && (y >= ys && (y - ys) % yf == 0) && x + vec[0] < cols && y + vec[1] < rows && x + vec[0] >= 0 && y + vec[1] >= 0) {
						if (!d)
							addEdge(gridRef[x + vec[0]][y + vec[1]], gridRef[x][y], w);
						addEdge(gridRef[x][y], gridRef[x + vec[0]][y + vec[1]], w);
					}  else if (xf==0){
						continue;
					} else if (yf == 0 && y == ys && (x >= xs && (x - xs) % xf == 0) && x + vec[0] < cols && y + vec[1] < rows && x + vec[0] >= 0 && y + vec[1] >= 0) {
						if (!d)
							addEdge(gridRef[x + vec[0]][y + vec[1]], gridRef[x][y], w);
						addEdge(gridRef[x][y], gridRef[x+vec[0]][y+vec[1]],w);
					} else if (yf == 0){
						continue;
					} else if ((x >= xs && (x - xs) % xf == 0) && (y >= ys && (y - ys) % yf == 0) && x + vec[0] < cols && y + vec[1] < rows && x + vec[0] >= 0 && y + vec[1] >= 0) {
						if(!d)
							addEdge(gridRef[x+vec[0]][y+vec[1]],gridRef[x][y],w);
						addEdge(gridRef[x][y], gridRef[x+vec[0]][y+vec[1]],w);
					}
//					if(x%xf==xs && y%yf==ys && (x+vec[0]>=cols || y+vec[1]>=rows || x+vec[0]<0 || y+vec[1]<0) && (b == SINKS_BORDER || b == REFLECTIVE_BORDER)){
//						int destVert = this.configSize();
//						addVertex(xCoord + (x+vec[0]) * gridSpacing, yCoord + (y+vec[0]) * gridSpacing);
//						if(b==REFLECTIVE_BORDER)
//							addEdge(destVert,gridRef[x][y],w);
//						addEdge(gridRef[x][y], destVert,w);
//					}
//					if(x%xf==xs && y%yf==ys && (x-vec[0]>=cols || y-vec[1]>=rows || x-vec[0]<0 || y-vec[1]<0) && (b == SINKS_BORDER || b == REFLECTIVE_BORDER)){
//						int destVert = this.configSize();
//						addVertex(xCoord + (x-vec[0]) * gridSpacing, yCoord + (y-vec[0]) * gridSpacing);
//						if(b==REFLECTIVE_BORDER)
//							addEdge(destVert,gridRef[x][y],w);
//						addEdge(gridRef[x][y], destVert,w);
//					}

				}
			}
		}
	}

	public void setToDualConfig(int times) {
		setConfig(sg.getDualConfig(currentConfig).times(times));
		onConfigEdit();
		repaint();
	}

	public void addDualConfig(int times) {
		addConfig(sg.getDualConfig(currentConfig).times(times));
		repaint();
	}

	//public void setControlState(int controlState) {
	//curState = controlState;
	//}
	public void setToMaxStableConfig(int times) {
		setConfig(sg.getMaxConfig().times(times));
		repaint();
	}

	public void addMaxStableConfig(int times) {
		addConfig(sg.getMaxConfig().times(times));
		repaint();
	}

	public SandpileConfiguration getIdentity() throws InterruptedException{
		if (!configs.containsKey("Identity")) {
			configs.put("Identity", sg.getIdentityConfig());
		}
		return configs.get("Identity");
	}

	public void addIdentity(int times) throws InterruptedException{
		addConfig(getIdentity().times(times));
		repaint();
	}

	public void setToIdentity(int times) throws InterruptedException{
		setConfig(getIdentity().times(times));
		repaint();
	}

	public void setSandEverywhere(int amount) {
		setConfig(sg.getUniformConfig(amount));
		repaint();
	}

	public void addSandEverywhere(int amount) {
		addConfig(sg.getUniformConfig(amount));
		repaint();
	}

	public SandpileConfiguration getBurningConfig() throws InterruptedException{
		if(!configs.containsKey("Burning")){
			configs.put("Burning", sg.getMinimalBurningConfig());
		}
		return configs.get("Burning");
	}

	public void setToBurningConfig(int times) throws InterruptedException{
		setConfig(getBurningConfig().times(times));
		repaint();
	}

	public void addBurningConfig(int times) throws InterruptedException{
		addConfig(getBurningConfig().times(times));
		repaint();
	}

	public void setToEquivalentRecurrent(int times) throws InterruptedException{
		setConfig(sg.getEquivalentRecurrent(currentConfig).times(times));
		repaint();
	}

	public void addEquivalentRecurrent(int times) throws InterruptedException{
		addConfig(sg.getEquivalentRecurrent(currentConfig).times(times));
		repaint();
	}

	public void setToInverseConfig(int times) throws InterruptedException{
		setConfig(sg.getInverseConfig(currentConfig).times(times));
		repaint();
	}

	public void addInverseConfig(int times) throws InterruptedException{
		addConfig(sg.getInverseConfig(currentConfig).times(times));
		repaint();
	}

	public void setToRandomConfig(int times) {
		this.clearSand();
		this.addSandToRandom(sg.getNonSinks(), times);
		repaint();
	}

	public void addRandomConfig(int times) {
		addSandToRandom(sg.getNonSinks(), times);
		repaint();
	}

	public void setToCurrentConfig(int times) {
		setConfig(currentConfig.times(times));
		repaint();
	}

	public void addCurrentConfig(int times) {
		addConfig(currentConfig.times(times));
		repaint();
	}

	public SandpileConfiguration getConfigByName(String name) {
		return configs.get(name);
	}

	public SandpileConfiguration removeConfigNamed(String name) {
		return configs.remove(name);
	}

	public void addConfigNamed(String name, int times) {
		SandpileConfiguration config = getConfigByName(name);
		if(config==null)
			return;
		setConfig(currentConfig.plus(config.times(times)));
		repaint();
	}

	public void setConfigNamed(String name, int times) {
		SandpileConfiguration config = getConfigByName(name);
		if(config==null)
			return;
		setConfig(config.times(times));
		repaint();
	}

	public void clearSand() {
		setConfig(sg.getUniformConfig(0));
		repaint();
	}

	public void stabilize() throws InterruptedException{
		setConfig(sg.stabilizeConfig(currentConfig));
	}

	public final SandpileGraph getGraph() {
		return sg;
	}

	public SandpileConfiguration getConfig() {
		return currentConfig;
	}

	public int addVertex(float x, float y) {
		sg.addVertex();
		vertexData.addRow(x,y);
		currentConfig.add(0);
		firings.add(0);
		return configSize() - 1;
	}

	public int insertVertex(int index, float x, float y){
		sg.insertVertex(index);
		vertexData.insertRow(index, x,y);
		currentConfig.insert(index, 0);
		firings.insert(index, 0);
		return configSize() - 1;
	}

	public float getVertexX(int vert) {
		return vertexData.get(vert, 0);
	}

	public float getVertexY(int vert) {
		return vertexData.get(vert, 1);
	}

	public void setVertexPos(int vert, float x, float y) {
		vertexData.set(vert, 0, x);
		vertexData.set(vert, 1, y);
	}

	public int getSand(int vert) {
		return currentConfig.get(vert);
	}

	private void delVertex(int v) {
		vertexData.removeRow(v);
		currentConfig.remove(v);
		int index = selectedVertices.indexOf(v);
		if(index>=0){
			selectedVertices.remove(index);
			for (int i = 0; i < selectedVertices.size(); i++) {
				if (selectedVertices.get(i) > v) {
					selectedVertices.set(i, selectedVertices.get(i) - 1);
				}
			}
		}
		firings.remove(v);
		sg.removeVertex(v);
		configs.clear();
	}

	public void delVerticesControl(TIntArrayList vertices) {
		final SandpileGraph oldSG = new SandpileGraph(sg);
		final TIntArrayList theVerts = new TIntArrayList(vertices.toNativeArray());
		SGEdit theEdit = new SGEdit("delete vertices") {
			@Override
			public void undoAction() {
				sg = new SandpileGraph(oldSG);
				unselectVertices();
			}

			@Override
			public void redoAction() {
				delVertices(theVerts);
				unselectVertices();
			}
		};
		undoManager.addEdit(theEdit);
		theEdit.redoAction();
		onGraphChange();
		repaint();
	}

	protected void delVertices(TIntArrayList vertices) {
		boolean[] toRemove = new boolean[configSize()];
		for (int i = 0; i < vertices.size(); i++) {
			int v = vertices.get(i);
			toRemove[v] = true;
//			int index = selectedVertices.indexOf(v);
//			if(index>=0){
//				selectedVertices.remove(index);
//				for (int j = 0; j < selectedVertices.size(); j++) {
//					if (selectedVertices.get(j) > v) {
//						selectedVertices.set(j, selectedVertices.get(j) - 1);
//					}
//				}
//			}
		}
		for (int v = configSize() - 1; v >= 0; v--) {
			if (toRemove[v]) {
				vertexData.removeRow(v);
				currentConfig.remove(v);
			}
		}
		sg.removeVertices(vertices);
		selectedVertices.clear();
		configs.clear();
	}

	private SandpileGraph tryStoreGraph() {
		SandpileGraph oldSG;
		try{
			oldSG = new SandpileGraph(sg);
		} catch (OutOfMemoryError e){
			System.err.println("Not enough memory to copy graph.");
			oldSG = new SandpileGraph();
		}
		return oldSG;
	}

	public void delAllVerticesControl() {
		final SandpileGraph oldSG = tryStoreGraph();
		SGEdit theEdit = new SGEdit("delete graph") {
			@Override
			public void undoAction() {
				sg = new SandpileGraph(oldSG);
			}

			@Override
			public void redoAction() {
				delAllVertices();
			}
		};
		undoManager.addEdit(theEdit);
		theEdit.redoAction();
		onGraphChange();
		repaint();
	}

	protected void delAllVertices() {
		vertexData.clear();
		currentConfig.clear();
		firings.clear();
		configs.clear();
		sg.removeAllVertices();
		selectedVertices.clear();
	}

	protected void addEdge(int originVert, int destVert) {
		addEdge(originVert, destVert, 1);
	}

	protected void addEdge(int originVert, int destVert, int weight) {
		sg.addEdge(originVert, destVert, weight);
		clearEdgeDependentConfigs();
	}

	protected void delEdge(int originVert, int destVert) {
		this.delEdge(originVert, destVert, 1);
	}

	protected void delEdge(int originVert, int destVert, int weight) {
		sg.removeEdge(originVert, destVert, weight);
		clearEdgeDependentConfigs();
	}

	public void addSand(int vert, int amount) {
		setSand(vert, currentConfig.get(vert) + amount);
		onConfigEdit();
	}

	public void addSandToRandom(TIntArrayList vertices, int amount){
		int sign = 1;
		if(amount<0){
			sign = -1;
			amount = -amount;
		}

		if(vertices.isEmpty())
			return;
		for(int i=0; i<amount; i++){
			int v = (int) (Math.random() * vertices.size());
			addSand(vertices.get(v),sign);
		}
	}

	public void setSand(int vert, int amount) {
		currentConfig.set(vert, amount);
		onConfigEdit();
	}

//	public Iterable<Integer> getOutgoingVertices(int vert) {
//		return sg.getOutgoingVertices(vert);
//	}

	public TIntArrayList getVerticesInRect(float maxX, float maxY, float minX, float minY) {
		TIntArrayList containedVertices = new TIntArrayList();
		for (int v = 0; v < configSize(); v++) {
			float x = getVertexX(v);
			float y = getVertexY(v);
			if ((x <= maxX && x >= minX) && (y <= maxY && y >= minY)) {
				containedVertices.add(v);
			}
		}
		return containedVertices;
	}

	public void unselectVertices() {
		selectedVertices.clear();
	}

	public void unselectVertex(int vert) {
		int i = selectedVertices.binarySearch(vert);
		if(i>-1)
			selectedVertices.remove(i);
	}

	public void selectVertices(TIntArrayList vertices) {
		selectedVertices.add(vertices.toNativeArray());
	}

	public void setSelectedVertices(TIntArrayList vertices) {
		selectedVertices = vertices;
	}

	public void selectVertex(int vert) {
		selectedVertices.add(vert);
	}

	public boolean isSelected(int vert) {
		return selectedVertices.contains(vert);
	}

	public TIntArrayList getSelectedVertices() {
		return selectedVertices;
	}

	public void moveVertices(TIntArrayList vertices, float deltaX, float deltaY) {
		for (int v=0; v<configSize(); v++) {
			setVertexPos(v, getVertexX(v)+deltaX, getVertexY(v)+deltaY);
		}
	}

	public int touchingVertex(float x, float y) {
		for (int v = 0; v < configSize(); v++) {
			float vx = getVertexX(v);
			float vy = getVertexY(v);
			if ((vx - VERT_RADIUS) <= x && vx + VERT_RADIUS >= x && (vy - VERT_RADIUS) <= y && vy + VERT_RADIUS >= y) {
				return v;
			}
		}
		return -1;
	}

	public void editFromString(String s) {
		String[] parts = s.split(" ");
		if (parts[0].toLowerCase().equals("vertex")) {
			float x = Float.valueOf(parts[1]);
			float y = Float.valueOf(parts[2]);
			addVertex(x, y);
		} else if (parts[0].toLowerCase().equals("edge")) {
			int v1 = Integer.valueOf(parts[1]);
			int v2 = Integer.valueOf(parts[2]);
			int w = Integer.valueOf(parts[3]);
			this.addEdge(v1, v2, w);
		}
	}

	public void loadGraph(File file) {
		try {
			BufferedReader inBuffer = new BufferedReader(new FileReader(file));
			String line = inBuffer.readLine();
			this.delAllVerticesControl();
			while (line != null) {
				this.editFromString(line);
				line = inBuffer.readLine();
			}
			inBuffer.close();
		} catch (IOException e) {
			System.err.println("Caught IOException while trying to load graph: " + e.getMessage());
		}
		repaint();
	}

	public boolean hasProjectFile() {
		return projectFile != null;
	}

	public void storeCurrentConfig(String name) {
		configs.put(name, currentConfig);
		onEdit();
	}

	public Set<String> getStoredConfigNames() {
		return configs.keySet();
	}

	public void saveGraphProject() {
		saveGraphProject(projectFile);
	}

	public boolean saveGraphProject(File file) {
		file.mkdir();
		File graphFile = new File(file, "graph.sg");
		saveGraph(graphFile);
		for(File f : file.listFiles()){
			if(f.getName().endsWith(".sc")){
				if(!f.delete())
					System.err.println("Unable to clear file: "+f.getName()+".");
			}
		}
		for (String configName : configs.keySet()) {
			saveConfig(new File(file, configName + ".sc"), configs.get(configName));
		}
		saveConfig(new File(file, "current.sc"), currentConfig);
		projectFile = file;
		saved = true;
		return true;
	}

	public boolean loadGraphProject(File file) {
		File graphFile = new File(file, "graph.sg");
		if (graphFile.exists()) {
			loadGraph(graphFile);
			String[] projectFileNames = file.list();
			for (String s : projectFileNames) {
				if (s.equals("current.sc")) {
					System.err.println("Loading current");
					loadCurrentConfig(new File(file, "current.sc"));
				} else if (s.endsWith(".sc")) {
					System.err.println("Loading " + s.substring(0, s.length() - 3));
					configs.put(s.substring(0, s.length() - 3), loadConfig(new File(file, s)));
				}
			}
			projectFile = file;
		} else {
			return false;
		}
		repaint();
		saved = true;
		return true;
	}

	public void saveGraph(File file) {
		try {
			file.createNewFile();
			BufferedWriter outBuffer = new BufferedWriter(new FileWriter(file));
			for (int v=0; v<configSize(); v++) {
				outBuffer.write("vertex " + getVertexX(v) + " " + getVertexY(v));
				outBuffer.newLine();
			}
			for (int v = 0; v < configSize(); v++) {
				for (Edge e : sg.getOutgoingEdges(v)) {
					outBuffer.write("edge " + e.source() + " " + e.dest() + " " + e.wt());
					outBuffer.newLine();
				}
			}
			outBuffer.close();
			saved = true;
		} catch (IOException e) {
			System.err.println("Caught IOException while trying to save graph: " + e.getMessage());
		}
	}

	public void loadCurrentConfig(File file) {
		setConfig(loadConfig(file));
		saved = true;
	}

	public SandpileConfiguration loadConfig(File file) {
		SandpileConfiguration config = new SandpileConfiguration();
		try {
			BufferedReader inBuffer = new BufferedReader(new FileReader(file));
			String line = inBuffer.readLine();
			while (line != null) {
				config.add(Integer.valueOf(line));
				line = inBuffer.readLine();
			}
			inBuffer.close();
		} catch (IOException e) {
			System.err.println("Caught IOException while trying to load config: " + e.getMessage());
		}
		return config;
	}

	public void saveConfig(File file) {
		saveConfig(file, currentConfig);
	}

	public void saveConfig(File file, SandpileConfiguration config) {
		try {
			BufferedWriter outBuffer = new BufferedWriter(new FileWriter(file));
			for (int i=0; i< configSize(); i++) {
				int v = config.get(i);
				outBuffer.write(Integer.toString(v));
				outBuffer.newLine();
			}
			outBuffer.close();
		} catch (IOException e) {
			System.err.println("Caught IOException while trying to save graph: " + e.getMessage());
		}
	}

	public void setMinRepaintDelay(long delay) {
		minRepaintDelay = delay;
	}

	public void setMinUpdateDelay(long delay) {
		minUpdateDelay = delay;
	}

	public void setRepaintOnEveryUpdate(boolean value){
		repaintOnEveryUpdate = value;
	}

	public long getMinUpdateDelay() {
		return minUpdateDelay;
	}
}
