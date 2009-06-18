package org.headb;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author headb
 */
//import java.beans.*;
//import java.io.Serializable;
import javax.swing.event.MouseInputAdapter;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.io.*;

//import java.lang.Math;
import java.util.ArrayList;

import java.awt.Canvas;

public class SandpileController implements ActionListener, Serializable, Runnable {

	private float VERT_RADIUS = 1.0f;
	private long minUpdateDelay = 0;
	private long lastUpdateTime = 0;
	private long minRepaintDelay = 33;
	private long lastRepaintTime = 0;
	private SandpileGraph sg;
	ArrayList<float[]> vertexData;
	private int selectedVertex;
	private long lastUpdate = System.currentTimeMillis();
	public double fps = 0.0;
	private SandpileConfiguration currentConfig;
	private SandpileDrawer drawer;

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
		currentConfig = new SandpileConfiguration();

		Canvas canvas = drawer.getCanvas();

		selectedVertex = -1;
		canvas.addMouseListener(new MouseInputAdapter() {/*
			@Override
			public void mouseClicked(MouseEvent evt) { 
			int x = evt.getX();
			int y = evt.getY();
			int touchVert = touchingVertex(x, y);
			if (touchVert < 0) {
			selectedVertex = -1;
			}
			repaint();
			}*/

		});
	}

	public void actionPerformed(ActionEvent evt) {
		if (System.currentTimeMillis() - lastUpdateTime >= minUpdateDelay) {
			lastUpdateTime = System.currentTimeMillis();
			this.update();
		}
		if (System.currentTimeMillis() - lastRepaintTime >= minRepaintDelay) {
			lastRepaintTime = System.currentTimeMillis();
			this.repaint();
		}
	}

	/**
	 * Fires all unstable vertices and repaints.
	 */
	public void update() {
		SandpileConfiguration nextConfig = sg.updateConfig(currentConfig);
		currentConfig = nextConfig;
	}

	public void run() {
		while (!Thread.interrupted()) {
			if (System.currentTimeMillis() - lastUpdateTime >= minUpdateDelay) {
				lastUpdateTime = System.currentTimeMillis();
				this.update();
			}
			if (System.currentTimeMillis() - lastRepaintTime >= minRepaintDelay) {
				lastRepaintTime = System.currentTimeMillis();
				this.repaint();
			}
		}
	}

	public void repaint() {
		drawer.paintSandpileGraph(sg, vertexData, currentConfig, selectedVertex);
	}

	public void addVertexControl(float x, float y) {
		int touchVert = touchingVertex(x, y);
		if (touchVert < 0) {
			if (selectedVertex >= 0) {
				selectedVertex = -1;
			} else {
				//System.out.println("Adding vertex "+x+" "+y);
				addVertex(x, y);
			}
		} else {
			selectedVertex = touchVert;
		}
		repaint();
	}

	public void delVertexControl(float x, float y) {
		int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			delVertex(touchVert);
		} else {
			selectedVertex = -1;
		}
		repaint();
	}

	public void addEdgeControl(float x, float y, int weight) {
		int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			if (selectedVertex < 0) {
				selectedVertex = touchVert;
			} else if (touchVert != selectedVertex) {
				addEdge(selectedVertex, touchVert, weight);
			}
		} else {
			selectedVertex = -1;
		}
		repaint();
	}

	public void delEdgeControl(float x, float y, int weight) {
		int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			if (selectedVertex < 0) {
				selectedVertex = touchVert;
			} else if (touchVert != selectedVertex) {
				delEdge(selectedVertex, touchVert, weight);
			}
		} else {
			selectedVertex = -1;
		}
		repaint();
	}

	public void addUndiEdgeControl(float x, float y, int weight) {
		int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			if (selectedVertex < 0) {
				selectedVertex = touchVert;
			} else if (touchVert != selectedVertex) {
				addEdge(selectedVertex, touchVert, weight);
				addEdge(touchVert, selectedVertex, weight);
			}
		}
		repaint();
	}

	public void delUndiEdgeControl(float x, float y, int weight) {
		int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			if (selectedVertex < 0) {
				selectedVertex = touchVert;
			} else if (touchVert != selectedVertex) {
				delEdge(selectedVertex, touchVert, weight);
				delEdge(touchVert, selectedVertex, weight);
			}
		}
		repaint();
	}

	public void addSandControl(float x, float y, int amount) {
		int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			selectedVertex = touchVert;
			addSand(touchVert, amount);
		}
		repaint();
	}

	public void setSandControl(float x, float y, int amount) {
		int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			selectedVertex = touchVert;
			setSand(touchVert, amount);
		}
		repaint();
	}

	public void makeGrid(int rows, int cols, float x, float y, int nBorder, int sBorder, int eBorder, int wBorder) {
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
			if (nBorder < 2) {
				nBorderRef[i] = vertexData.size();
				addVertex(x + i * gridSpacing, y + gridSpacing);
			}
			if (sBorder < 2) {
				sBorderRef[i] = vertexData.size();
				addVertex(x + i * gridSpacing, y - (rows) * gridSpacing);
			}

		}
		for (int i = 0; i < rows; i++) {
			if (wBorder < 2) {
				wBorderRef[i] = vertexData.size();
				addVertex(x - gridSpacing, y - i * gridSpacing);
			}
			if (eBorder < 2) {
				eBorderRef[i] = vertexData.size();
				addVertex(x + (cols) * gridSpacing, y - i * gridSpacing);
			}
		}
		//create edges
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				if (i == 0) {
					if (nBorder == 0) {
						addEdge(gridRef[i][j], nBorderRef[j], 1);
					} else if (nBorder == 1) {
						addEdge(gridRef[i][j], nBorderRef[j], 1);
						addEdge(nBorderRef[j], gridRef[i][j], 1);
					}
				} else {
					addEdge(gridRef[i][j], gridRef[i - 1][j]);
				}

				if (i == rows - 1) {
					if (sBorder == 0) {
						addEdge(gridRef[i][j], sBorderRef[j], 1);
					} else if (sBorder == 1) {
						addEdge(gridRef[i][j], sBorderRef[j], 1);
						addEdge(sBorderRef[j], gridRef[i][j], 1);
					}
				} else {
					addEdge(gridRef[i][j], gridRef[i + 1][j]);
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
		repaint();
	}

	public void makeHoneycomb(int radius, float x, float y, int borders) {
		/*
		 * for borders:
		 * 0 - directed
		 * 1 - undirected
		 **/
		float gridSpacing = VERT_RADIUS * 2;
		int curRowLength = radius;
		int[][] gridRef = new int[radius * 2 - 1][radius * 2 - 1];
		for (int i = 0; i < radius * 2 - 1; i++) {
			for (int j = 0; j < curRowLength; j++) {
				gridRef[i][j] = vertexData.size();
				addVertex(x + j * gridSpacing + (i + (radius - 1) % 2) % 2 * (gridSpacing / 2) - curRowLength / 2 * (gridSpacing), y + i * (gridSpacing * 5f/6f));
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
		this.repaint();

	}

	public void makeHexGrid(int rows, int cols, float x, float y, int nBorder, int sBorder, int eBorder, int wBorder) {
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
		this.repaint();
	}

	public void setToDualConfig() {
		currentConfig = sg.getDualConfig(currentConfig);
		repaint();
	}

	public void addDualConfig() {
		currentConfig = currentConfig.plus(sg.getDualConfig(currentConfig));
		repaint();
	}

	//public void setControlState(int controlState) {
	//curState = controlState;
	//}
	public void setToMaxStableConfig() {
		currentConfig = sg.getMaxConfig();
		repaint();
	}

	public void addMaxStableConfig() {
		currentConfig = currentConfig.plus(sg.getMaxConfig());
		repaint();
	}

	public void addIdentity() {
		currentConfig = currentConfig.plus(sg.getIdentityConfig());
		repaint();
	}

	public void setToIdentity() {
		currentConfig = sg.getIdentityConfig();
		repaint();
	}

	public void setSandEverywhere(int amount) {
		currentConfig = sg.getUniformConfig(amount);
		repaint();
	}

	public void addSandEverywhere(int amount) {
		currentConfig = currentConfig.plus(sg.getUniformConfig(amount));
		repaint();
	}

	public void setToBurningConfig() {
		currentConfig = sg.getMinimalBurningConfig();
		repaint();
	}

	public void addBurningConfig() {
		currentConfig = currentConfig.plus(sg.getMinimalBurningConfig());
		repaint();
	}

	public void clearSand() {
		currentConfig = sg.getUniformConfig(0);
		repaint();
	}

	public void addVertex(float x, float y) {
		sg.addVertex();
		float[] newPos = {x, y};
		vertexData.add(newPos);
		currentConfig.add(0);
	}

	public void delVertex(int v) {
		vertexData.remove(v);
		currentConfig.remove(v);
		sg.removeVertex(v);
	}

	public void delAllVertices() {
		vertexData.clear();
		currentConfig.clear();
		sg.removeAllVertices();
		this.selectedVertex = -1;
		repaint();
	}

	public void addEdge(int originVert, int destVert) {
		addEdge(originVert, destVert, 1);
	}

	public void addEdge(int originVert, int destVert, int weight) {
		sg.addEdge(originVert, destVert, weight);
	}

	public void delEdge(int originVert, int destVert) {
		this.delEdge(originVert, destVert, 1);
	}

	public void delEdge(int originVert, int destVert, int weight) {
		sg.removeEdge(originVert, destVert, weight);
	}

	public void addSand(int vert, int amount) {
		setSand(vert, currentConfig.get(vert) + amount);
	}

	public void addSandToRandom(int amount){
		int v = (int)(Math.random()*currentConfig.size());
		addSand(v, amount);
	}

	public void setSand(int vert, int amount) {
		currentConfig.set(vert, amount);
	}

	private int touchingVertex(float x, float y) {
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

	public void saveGraph(File file) {
		try {
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
		} catch (IOException e) {
			System.err.println("Caught IOException while trying to save graph: " + e.getMessage());
		}
	}

	public void loadConfig(File file) {
		try {
			BufferedReader inBuffer = new BufferedReader(new FileReader(file));
			String line = inBuffer.readLine();
			currentConfig.clear();
			while (line != null) {
				currentConfig.add(Integer.valueOf(line));
				line = inBuffer.readLine();
			}
			inBuffer.close();
		} catch (IOException e) {
			System.err.println("Caught IOException while trying to load config: " + e.getMessage());
		}
		repaint();
	}

	public void saveConfig(File file) {
		try {
			BufferedWriter outBuffer = new BufferedWriter(new FileWriter(file));
			for (int v : currentConfig) {
				outBuffer.write(Integer.toString(v));
				outBuffer.newLine();
			}
			outBuffer.close();
		} catch (IOException e) {
			System.err.println("Caught IOException while trying to save graph: " + e.getMessage());
		}
		repaint();
	}

	public void setMinRepaintDelay(long delay) {
		minRepaintDelay = delay;
	}
	public void setMinUpdateDelay(long delay) {
		minUpdateDelay = delay;
	}
}
