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

public class SandpileController implements ActionListener, Serializable{

	public static final int SINKS_BORDER = 0;
	public static final int REFLECTIVE_BORDER = 1;
	public static final int NO_BORDER = 2;
	public static final int LOOP_BORDER = 3;
	public static final int LOOP_REVERSE_BORDER = 4;

	private float VERT_RADIUS = 1.0f;
	private long minUpdateDelay = 100;
	private long lastUpdateTime = 0;
	private long minRepaintDelay = 33;
	private long lastRepaintTime = 0;
	private boolean repaintOnEveryUpdate = false;
	private SandpileGraph sg;
	ArrayList<float[]> vertexData;
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

	private ServerSocket server;
	private Socket incoming;
	private SandpileProtocol protocol;
	private BufferedReader in;
	private PrintWriter out;

	private boolean needsRepaint = false;

	public SandpileController() {
		drawer = new SandpileGLDrawer();
		initWithSandpileGraph(new SandpileGraph());
	}

	public SandpileController(SandpileDrawer d) {
		drawer = d;
		initWithSandpileGraph(new SandpileGraph());
	}

	public SandpileController(SandpileDrawer d, SandpileGraph sg) {
		initWithSandpileGraph(sg);
	}

	private void initWithSandpileGraph(SandpileGraph sg) {
		//this.curState = ADD_VERT_STATE;
		this.sg = sg;
		vertexData = new ArrayList<float[]>();
		firings = new TIntArrayList();
		currentConfig = new SandpileConfiguration();
		selectedVertices = new TIntArrayList();
		configs = new HashMap<String, SandpileConfiguration>();

		Canvas canvas = drawer.getCanvas();
		selectedVertices.clear();
	}

	public void setDrawer(SandpileDrawer sd){
		drawer = sd;
	}

	public void startServer(int port) throws IOException{
		server = new ServerSocket(port, 5);
		//System.err.println("Created server socket");
		protocol = new SandpileProtocol(this);
	}

	public void acceptClient() throws IOException{
		incoming = server.accept();
		//System.err.println("Accepted incoming socket");
		in = new BufferedReader(new InputStreamReader(incoming.getInputStream()));
		out = new PrintWriter(incoming.getOutputStream(), true);
	}

	public String checkForMessage() throws IOException{
		if(in.ready())
			return in.readLine();
		else
			return null;
	}

	public void receiveMessage() throws IOException{
		String msg = null;
		try{
			msg = checkForMessage();
		}catch(Exception e){
			out.println(e.getMessage());
		}
		if(msg!=null)
			out.println(protocol.processInput(msg));
	}

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

	public int configSize(){
		return currentConfig.size();
	}

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
	 * Fires all unstable vertices and repaints.
	 */
	public void update() {
		if(drawer.getColorMode()==SandpileDrawer.ColorMode.FIRINGS)
			updateFirings();
		if (updater == null) {
			updater = sg.inPlaceUpdater(currentConfig);
		}
		if (updater.hasNext()) {
			updater.next();
		}
		//repaint();
	}

	public void resetFirings() {
		firings = new TIntArrayList();
		for (int i=0; i<currentConfig.size(); i++) {
			firings.add(0);
		}
	}

	public void updateFirings() {
		if (firings.size() != configSize()) {
			resetFirings();
		}
		for (int vert = 0; vert < configSize(); vert++) {
			if (currentConfig.get(vert) >= sg.degree(vert)) {
				firings.set(vert, firings.get(vert) + 1);
			}
		}
	}

	public void repaint() {
		drawer.paintSandpileGraph(sg, vertexData, currentConfig, firings, selectedVertices);
	}

	public void onGraphChange(){
		onEdit();
	}

	public void onConfigChange() {
		onEdit();
	}

	public void clearEdgeDependentConfigs() {
		configs.remove("Identity");
		configs.remove("Burning");
	}

	public void onEdit() {
		saved = false;
		updater = null;
	}

	public void setConfig(SandpileConfiguration config){
		if(config.size() == configSize()){
			currentConfig = config;
			onConfigChange();
		}else
			throw new IndexOutOfBoundsException("Tried to set the current sandpile " +
					"configuration to a configuration of an incorrect size. The correct" +
					" size is "+ configSize()+" while the new configuration had size" +
					config.size()+ ".");
	}
	public void addConfig(SandpileConfiguration config){
		if(config.size() == configSize()){
			currentConfig = currentConfig.plus(config);
			onConfigChange();
		}else
			throw new IndexOutOfBoundsException("Tried to add the current sandpile " +
					"configuration to a configuration of an incorrect size. The correct" +
					" size is "+ configSize()+" while the new configuration had size" +
					config.size()+ ".");
	}

	public void addVertexControl(final float x, final float y) {
		int touchVert = touchingVertex(x, y);
		if (touchVert < 0) {
			final int addedVert = addVertex(x, y);
			undoManager.addEdit(new AbstractUndoableEdit() {

				@Override
				public String getPresentationName() {
					return "add vertex";
				}

				@Override
				public void undo() {
					delVertex(addedVert);
					repaint();
				}

				@Override
				public void redo() {
					addVertex(x, y);
					repaint();
				}
			});
		}
		repaint();
	}

	public void delVertexControl(final float x, final float y) {
		final int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			delVertex(touchVert);
			undoManager.addEdit(new AbstractUndoableEdit() {

				private int vertIndex = touchVert;

				@Override
				public String getPresentationName() {
					return "delete vertex";
				}

				@Override
				public void undo() {
					vertIndex = addVertex(x, y);
					repaint();
				}

				@Override
				public void redo() {
					delVertex(vertIndex);
					repaint();
				}
			});
		}
		repaint();
	}

	public void addEdgeControl(float x, float y, final int weight) {
		final int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			for (int i = 0; i<selectedVertices.size(); i++) {
				int v = selectedVertices.get(i);
				addEdge(v, touchVert, weight);
			}
			undoManager.addEdit(new AbstractUndoableEdit() {

				private TIntArrayList sourceVertices = (TIntArrayList) selectedVertices.clone();

				@Override
				public String getPresentationName() {
					return "add edge(s)";
				}

				@Override
				public void undo() {
					for (int i=0; i < sourceVertices.size(); i++) {
						int v = sourceVertices.get(i);
						delEdge(v, touchVert, weight);
					}
					repaint();
				}

				@Override
				public void redo() {
					for (int i=0; i < sourceVertices.size(); i++) {
						int v = sourceVertices.get(i);
						addEdge(v, touchVert, weight);
					}
					repaint();
				}
			});
		}
		repaint();
	}

	public void delEdgeControl(float x, float y, final int weight) {
		final int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			for (int i = 0; i<selectedVertices.size(); i++) {
				int v = selectedVertices.get(i);
				delEdge(v, touchVert, weight);
			}
			undoManager.addEdit(new AbstractUndoableEdit() {

				private TIntArrayList sourceVertices = (TIntArrayList) selectedVertices.clone();

				@Override
				public String getPresentationName() {
					return "delete edge(s)";
				}

				@Override
				public void undo() {
					for (int i=0; i < sourceVertices.size(); i++) {
						int v = sourceVertices.get(i);
						addEdge(v, touchVert, weight);
					}
					repaint();
				}

				@Override
				public void redo() {
					for (int i=0; i < sourceVertices.size(); i++) {
						int v = sourceVertices.get(i);
						delEdge(v, touchVert, weight);
					}
					repaint();
				}
			});
		}
		repaint();
	}

	public void addUndiEdgeControl(float x, float y, final int weight) {
		final int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			for (int i = 0; i<selectedVertices.size(); i++) {
				int v = selectedVertices.get(i);
				addEdge(v, touchVert, weight);
				addEdge(touchVert, v, weight);
			}
			undoManager.addEdit(new AbstractUndoableEdit() {

				private TIntArrayList sourceVertices = (TIntArrayList) selectedVertices.clone();

				@Override
				public String getPresentationName() {
					return "add undirected edge(s)";
				}

				@Override
				public void undo() {
					for (int i=0; i < sourceVertices.size(); i++) {
						int v = sourceVertices.get(i);
						delEdge(v, touchVert, weight);
						delEdge(touchVert, v, weight);
					}
					repaint();
				}

				@Override
				public void redo() {
					for (int i=0; i < sourceVertices.size(); i++) {
						int v = sourceVertices.get(i);
						addEdge(v, touchVert, weight);
						addEdge(touchVert, v, weight);
					}
					repaint();
				}
			});
		}
		repaint();
	}

	public void delUndiEdgeControl(float x, float y, final int weight) {
		final int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			for (int i = 0; i<selectedVertices.size(); i++) {
				int v = selectedVertices.get(i);
				delEdge(v, touchVert, weight);
				delEdge(touchVert, v, weight);
			}
			undoManager.addEdit(new AbstractUndoableEdit() {

				private TIntArrayList sourceVertices = (TIntArrayList) selectedVertices.clone();

				@Override
				public String getPresentationName() {
					return "delete undirected edge(s)";
				}

				@Override
				public void undo() {
					for (int i=0; i < sourceVertices.size(); i++) {
						int v = sourceVertices.get(i);
						addEdge(v, touchVert, weight);
						addEdge(touchVert, v, weight);
					}
					repaint();
				}

				@Override
				public void redo() {
					for (int i=0; i < sourceVertices.size(); i++) {
						int v = sourceVertices.get(i);
						delEdge(v, touchVert, weight);
						delEdge(touchVert, v, weight);
					}
					repaint();
				}
			});
		}
		repaint();
	}

	public void addSandControl(float x, float y, int amount) {
		int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			addSand(touchVert, amount);
		}
		repaint();
	}

	public void setSandControl(float x, float y, int amount) {
		int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			setSand(touchVert, amount);
		}
		repaint();
	}


	public void makeGridControl(final int rows, final int cols,
			final float x, final float y,
			final int nBorder, final int sBorder, final int eBorder, final int wBorder) {
		final int startingIndex = configSize();
		makeGrid(rows, cols, x, y, nBorder, sBorder, eBorder, wBorder);
		final int endingIndex = configSize()-1;
		undoManager.addEdit(new AbstractUndoableEdit() {

			@Override
			public String getPresentationName() {
				return "make gride";
			}

			@Override
			public void undo() {
				TIntArrayList vertices = new TIntArrayList();
				for(int i=startingIndex;i<=endingIndex;i++){
					vertices.add(i);
				}
				delVertices(vertices);
				repaint();
			}

			@Override
			public void redo() {
				makeGrid(rows, cols, x, y, nBorder, sBorder, eBorder, wBorder);
				repaint();
			}
		});
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

		//create vertices
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				gridRef[i][j] = vertexData.size();
				addVertex(x + j * gridSpacing, y - i * gridSpacing);
			}
		}

		for (int i = 0; i < cols; i++) {
			if (nBorder == SINKS_BORDER || nBorder == REFLECTIVE_BORDER) {
				nBorderRef[i] = vertexData.size();
				addVertex(x + i * gridSpacing, y + gridSpacing);
			}
			if (sBorder == SINKS_BORDER || sBorder == REFLECTIVE_BORDER) {
				sBorderRef[i] = vertexData.size();
				addVertex(x + i * gridSpacing, y - (rows) * gridSpacing);
			}

		}
		for (int i = 0; i < rows; i++) {
			if (wBorder == SINKS_BORDER || wBorder == REFLECTIVE_BORDER) {
				wBorderRef[i] = vertexData.size();
				addVertex(x - gridSpacing, y - i * gridSpacing);
			}
			if (eBorder == SINKS_BORDER || eBorder == REFLECTIVE_BORDER) {
				eBorderRef[i] = vertexData.size();
				addVertex(x + (cols) * gridSpacing, y - i * gridSpacing);
			}
		}
		//create edges
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				if (i == 0) {
					switch(nBorder){
						case SINKS_BORDER:
							addEdge(gridRef[i][j], nBorderRef[j], 1);
							break;
						case REFLECTIVE_BORDER:
							addEdge(gridRef[i][j], nBorderRef[j], 1);
							addEdge(nBorderRef[j], gridRef[i][j], 1);
							break;
						case LOOP_BORDER:
							addEdge(gridRef[i][j], gridRef[rows-1][j], 1);
							break;
						case LOOP_REVERSE_BORDER:
							addEdge(gridRef[i][j], gridRef[rows-1][cols-1 - j], 1);
							break;
					}
				} else {
					addEdge(gridRef[i][j], gridRef[i - 1][j]);
				}

				if (i == rows - 1) {
					switch(sBorder){
						case SINKS_BORDER:
							addEdge(gridRef[i][j], sBorderRef[j], 1);
							break;
						case REFLECTIVE_BORDER:
							addEdge(gridRef[i][j], sBorderRef[j], 1);
							addEdge(sBorderRef[j], gridRef[i][j], 1);
							break;
						case LOOP_BORDER:
							addEdge(gridRef[rows-1][j], gridRef[0][j], 1);
							break;
						case LOOP_REVERSE_BORDER:
							addEdge(gridRef[rows-1][j], gridRef[0][cols-1-j], 1);
							break;
					}
				} else {
					addEdge(gridRef[i][j], gridRef[i + 1][j]);
				}
				if (j == cols - 1) {
					switch(eBorder){
						case SINKS_BORDER:
							addEdge(gridRef[i][j], eBorderRef[i], 1);
							break;
						case REFLECTIVE_BORDER:
							addEdge(gridRef[i][j], eBorderRef[i], 1);
							addEdge(eBorderRef[i], gridRef[i][j], 1);
							break;
						case LOOP_BORDER:
							addEdge(gridRef[i][j], gridRef[i][0], 1);
							break;
						case LOOP_REVERSE_BORDER:
							addEdge(gridRef[i][j], gridRef[rows-1-i][0], 1);
							break;
					}
				} else {
					addEdge(gridRef[i][j], gridRef[i][j + 1]);
				}

				if (j == 0) {
					switch(wBorder){
						case SINKS_BORDER:
							addEdge(gridRef[i][j], wBorderRef[i], 1);
							break;
						case REFLECTIVE_BORDER:
							addEdge(gridRef[i][j], wBorderRef[i], 1);
							addEdge(wBorderRef[i], gridRef[i][j], 1);
							break;
						case LOOP_BORDER:
							addEdge(gridRef[i][j], gridRef[i][cols-1], 1);
							break;
						case LOOP_REVERSE_BORDER:
							addEdge(gridRef[i][j], gridRef[rows-1-i][cols-1], 1);
							break;
					}
				} else {
					addEdge(gridRef[i][j], gridRef[i][j - 1]);
				}

			}
		}
	}

	public void makeHoneycombControl(final int radius, final float x, final float y, final int borders) {
		
		final int startingIndex = configSize();
		makeHoneycomb(radius, x, y, borders);
		final int endingIndex = configSize()-1;
		undoManager.addEdit(new AbstractUndoableEdit() {

			@Override
			public String getPresentationName() {
				return "make honeycomb";
			}

			@Override
			public void undo() {
				TIntArrayList vertices = new TIntArrayList();
				for(int i=startingIndex;i<=endingIndex;i++){
					vertices.add(i);
				}
				delVertices(vertices);
				repaint();
			}

			@Override
			public void redo() {
				makeHoneycomb(radius, x, y, borders);
				repaint();
			}
		});
		this.repaint();

	}

	public void makeHoneycomb(final int radius, final float x, final float y, final int borders){
		float gridSpacing = VERT_RADIUS * 2;
		int curRowLength = radius;
		int[][] gridRef = new int[radius * 2 - 1][radius * 2 - 1];
		for (int i = 0; i < radius * 2 - 1; i++) {
			for (int j = 0; j < curRowLength; j++) {
				gridRef[i][j] = vertexData.size();
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
		final int startingIndex = configSize();
		makeHexGrid(rows, cols, x, y, nBorder, sBorder, eBorder, wBorder);
		final int endingIndex = configSize()-1;
		undoManager.addEdit(new AbstractUndoableEdit() {

			@Override
			public String getPresentationName() {
				return "make hex grid";
			}

			@Override
			public void undo() {
				TIntArrayList vertices = new TIntArrayList();
				for(int i=startingIndex;i<=endingIndex;i++){
					vertices.add(i);
				}
				delVertices(vertices);
				repaint();
			}

			@Override
			public void redo() {
				makeHexGrid(rows, cols, x, y, nBorder, sBorder, eBorder, wBorder);
				repaint();
			}
		});
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
				gridRef[i][j] = vertexData.size();
				addVertex(x + j * gridSpacing + i % 2 * (gridSpacing / 2), y - i * gridSpacing);
			}
		}

		for (int i = 0; i < cols + 1; i++) {
			if (nBorder < 2) {
				nBorderRef[i] = vertexData.size();
				addVertex(x + i * gridSpacing - (gridSpacing / 2), y + gridSpacing);
			}
			if (sBorder < 2) {
				sBorderRef[i] = vertexData.size();
				addVertex(x + i * gridSpacing - (gridSpacing / 2), y - (rows) * gridSpacing);
			}

		}

		for (int i = 0; i < rows; i++) {
			if (wBorder < 2) {
				wBorderRef[i] = vertexData.size();
				addVertex(x - gridSpacing + i % 2 * (gridSpacing / 2), y - i * gridSpacing);
			}
			if (eBorder < 2) {
				eBorderRef[i] = vertexData.size();
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
		final int startingIndex = configSize();
		buildLattice(xCoord,yCoord,rows,cols,spacing,vectors,xStartingWith,xFreq,yStartingWith,yFreq,directed,weight,borders);
		final int endingIndex = configSize()-1;
		undoManager.addEdit(new AbstractUndoableEdit() {

			@Override
			public String getPresentationName() {
				return "make hex grid";
			}

			@Override
			public void undo() {
				TIntArrayList vertices = new TIntArrayList();
				for(int i=startingIndex;i<=endingIndex;i++){
					vertices.add(i);
				}
				delVertices(vertices);
				repaint();
			}

			@Override
			public void redo() {
				buildLattice(xCoord,yCoord,rows,cols,spacing,vectors,xStartingWith,xFreq,yStartingWith,yFreq,directed,weight,borders);
				repaint();
			}
		});
		this.repaint();
	}

	public void buildLattice(final float xCoord, final float yCoord, final int rows, final int cols,
			final int spacing, final List<int[]> vectors,
			final TIntArrayList xStartingWith, final TIntArrayList xFreq, final TIntArrayList yStartingWith, final TIntArrayList yFreq,
			final List<Boolean> directed, final TIntArrayList weight, final TIntArrayList borders) {
		float gridSpacing = VERT_RADIUS * (spacing+1);
		int[][] gridRef = new int[rows][cols];

		//create vertices
		for (int x = 0; x < cols; x++) {
			for (int y = 0; y < rows; y++) {
				gridRef[x][y] = vertexData.size();
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
		repaint();
	}

	public void addDualConfig(int times) {
		setConfig(currentConfig.plus(sg.getDualConfig(currentConfig).times(times)));
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
		setConfig(currentConfig.plus(sg.getMaxConfig().times(times)));
		repaint();
	}

	public SandpileConfiguration getIdentity() {
		if (!configs.containsKey("Identity")) {
			configs.put("Identity", sg.getIdentityConfig());
		}
		return configs.get("Identity");
	}

	public void addIdentity(int times) {
		setConfig(currentConfig.plus(getIdentity().times(times)));
		repaint();
	}

	public void setToIdentity(int times) {
		setConfig(getIdentity().times(times));
		repaint();
	}

	public void setSandEverywhere(int amount) {
		setConfig(sg.getUniformConfig(amount));
		repaint();
	}

	public void addSandEverywhere(int amount) {
		setConfig(currentConfig.plus(sg.getUniformConfig(amount)));
		repaint();
	}

	public SandpileConfiguration getBurningConfig(){
		if(!configs.containsKey("Burning")){
			configs.put("Burning", sg.getMinimalBurningConfig());
		}
		return configs.get("Burning");
	}

	public void setToBurningConfig(int times) {
		setConfig(getBurningConfig().times(times));
		repaint();
	}

	public void addBurningConfig(int times) {
		setConfig(currentConfig.plus(getBurningConfig().times(times)));
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

	public void stabilize() {
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
		float[] newPos = {x, y};
		vertexData.add(newPos);
		currentConfig.add(0);
		firings.add(0);
		configs.clear();
		onGraphChange();
		return configSize() - 1;
	}

	public float[] getVertexLocation(int vert) {
		return vertexData.get(vert);
	}

	public int getSand(int vert) {
		return currentConfig.get(vert);
	}

	public void delVertex(int v) {
		vertexData.remove(v);
		currentConfig.remove(v);
		firings.remove(v);
		sg.removeVertex(v);
		configs.clear();
		onGraphChange();
	}

	public void delVertices(TIntArrayList vertices) {
		sg.removeVertices(vertices);
		configs.clear();
		boolean[] toRemove = new boolean[configSize()];
		for (int i = 0; i < vertices.size(); i++) {
			int v = vertices.get(i);
			toRemove[v] = true;
		}
		for (int v = configSize() - 1; v >= 0; v--) {
			if (toRemove[v]) {
				vertexData.remove(v);
				currentConfig.remove(v);
			}
		}
		onGraphChange();
	}

	public void delAllVertices() {
		vertexData.clear();
		currentConfig.clear();
		firings.clear();
		configs.clear();
		sg.removeAllVertices();
		this.selectedVertices.clear();
		onGraphChange();
		repaint();
	}

	public void addEdge(int originVert, int destVert) {
		addEdge(originVert, destVert, 1);
		onGraphChange();
	}

	public void addEdge(int originVert, int destVert, int weight) {
		sg.addEdge(originVert, destVert, weight);
		clearEdgeDependentConfigs();
		onGraphChange();
	}

	public void delEdge(int originVert, int destVert) {
		this.delEdge(originVert, destVert, 1);
		onGraphChange();
	}

	public void delEdge(int originVert, int destVert, int weight) {
		sg.removeEdge(originVert, destVert, weight);
		clearEdgeDependentConfigs();
		onGraphChange();
	}

	public void addSand(int vert, int amount) {
		setSand(vert, currentConfig.get(vert) + amount);
		onConfigChange();
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
		onConfigChange();
	}

	public Iterable<Integer> getOutgoingVertices(int vert) {
		return sg.getOutgoingVertices(vert);
	}

	public TIntArrayList getVerticesInRect(float maxX, float maxY, float minX, float minY) {
		TIntArrayList containedVertices = new TIntArrayList();
		for (int v = 0; v < vertexData.size(); v++) {
			float[] pos = vertexData.get(v);
			if ((pos[0] <= maxX && pos[0] >= minX) && (pos[1] <= maxY && pos[1] >= minY)) {
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
		for (int i=0; i<vertices.size(); i++) {
			int v = vertices.get(i);
			vertexData.get(v)[0] += deltaX;
			vertexData.get(v)[1] += deltaY;
		}
	}

	public int touchingVertex(float x, float y) {
		for (int i = 0; i < vertexData.size(); i++) {
			float[] v = vertexData.get(i);/*
			if (Math.sqrt((x - v[0]) * (x - v[0]) + (y - v[1]) * (y - v[1])) <= VERT_RADIUS) {
			return i;
			}*/
			if ((v[0] - VERT_RADIUS) <= x && v[0] + VERT_RADIUS >= x && (v[1] - VERT_RADIUS) <= y && v[1] + VERT_RADIUS >= y) {
				return i;
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
			this.delAllVertices();
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

	public void saveGraphProject(File file) {
		file.mkdir();
		File graphFile = new File(file, "graph.sg");
		saveGraph(graphFile);
		for (String configName : configs.keySet()) {
			saveConfig(new File(file, configName + ".sc"), configs.get(configName));
		}
		saveConfig(new File(file, "current.sc"), currentConfig);
		projectFile = file;
		saved = true;
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
			for (float[] coords : vertexData) {
				outBuffer.write("vertex " + coords[0] + " " + coords[1]);
				outBuffer.newLine();
			}
			for (int i = 0; i < vertexData.size(); i++) {
				for (int j : sg.getOutgoingVertices(i)) {
					outBuffer.write("edge " + i + " " + j + " " + sg.weight(i, j));
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
			for (int i=0; i< config.size(); i++) {
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
