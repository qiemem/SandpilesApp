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
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;

import java.awt.event.MouseEvent;
import javax.swing.event.MouseInputAdapter;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import java.awt.Graphics2D;

import java.awt.image.BufferedImage;

import java.io.*;

//import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

import java.awt.Canvas;

import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

public class SandpileController extends JPanel implements ActionListener, Serializable, Runnable {

	public static final int ADD_VERT_STATE = 0;
	public static final int DEL_VERT_STATE = 1;
	public static final int ADD_EDGE_STATE = 2;
	public static final int DEL_EDGE_STATE = 3;
	public static final int ADD_UNDI_EDGE_STATE = 4;
	public static final int DEL_UNDI_EDGE_STATE = 5;
	public static final int ADD_SAND_STATE = 6;
	public static final int DEL_SAND_STATE = 7;
	public static final int MAKE_GRID_STATE = 8;
	private boolean repaint = true;
	private boolean repaintAll = false;
	private boolean labels = false;
	private boolean color = true;
	private boolean changingNodeSize = true;
	private boolean drawEdges = true;
	private long delay = 0;
	private SandpileGraph sg;
	static final int VERT_RADIUS = 10;
	static final Color[] SAND_COLOR = {Color.gray, Color.blue, Color.cyan, Color.green, Color.red, Color.orange, Color.yellow};
	static final Color[] SAND_MONOCHROME = {new Color(25, 25, 25), new Color(50, 50, 50), new Color(100, 100, 100), new Color(150, 150, 150), new Color(200, 200, 200), new Color(225, 225, 225), new Color(255, 255, 255)};
	private BufferedImage[] vertexImages = new BufferedImage[SAND_COLOR.length];
	private double scale = 1.0;
	ArrayList<float[]> vertexData;
	//only keeps track of the existence of edges, not of their weights
	ArrayList<HashSet<Integer>> edges;
	private int selectedVertex;
	private long lastUpdate = System.currentTimeMillis();
	public double fps = 0.0;
	private boolean useBufferedImages = true;
	private boolean outputFPS = true;
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

	public SandpileController(SandpileGraph sg) {
		initWithSandpileGraph(sg);
	}

	private void initWithSandpileGraph(SandpileGraph sg) {
		//this.curState = ADD_VERT_STATE;
		this.sg = sg;
		vertexData = new ArrayList<float[]>();
		edges = new ArrayList<HashSet<Integer>>();
		currentConfig = new SandpileConfiguration();

		Canvas canvas = drawer.getCanvas();
		canvas.setPreferredSize(this.getSize());
		this.add(drawer.getCanvas());
		//canvas.setVisible(true);

		selectedVertex = -1;
		addMouseListener(new MouseInputAdapter() {

			@Override
			public void mouseReleased(MouseEvent evt) {

				int x = evt.getX();
				int y = evt.getY();
				int touchVert = touchingVertex(x, y);
				if (touchVert < 0) {
					selectedVertex = -1;
				}
				repaint();
			}

			@Override
			public void mouseDragged(MouseEvent evt) {
				System.out.println("Mouse drag");
				int x = evt.getX();
				int y = evt.getY();

				int touchVert = touchingVertex(x, y);
				if (touchVert >= 0) {
					vertexData.get(touchVert)[0] = x;
					vertexData.get(touchVert)[1] = y;
				}
			}
		});
		this.setIgnoreRepaint(false);
		this.setDoubleBuffered(true);
		this.calcVertexImages();
	}

	public void calcVertexImages() {
		int r = (int) Math.ceil(VERT_RADIUS * this.scale);
		for (int i = 0; i < vertexImages.length; i++) {
			BufferedImage bi = new BufferedImage(r * 2, r * 2, BufferedImage.TYPE_INT_RGB);
			renderVertex(bi.createGraphics(), i, r, r, r);
			vertexImages[i] = toCompatibleImage(bi);
			vertexImages[i]=bi;
		}
	}

	private BufferedImage toCompatibleImage(BufferedImage image) {
		// obtain the current system graphical settings
		GraphicsConfiguration gfx_config = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().
				getDefaultConfiguration();

		/*
		 * if image is already compatible and optimized for current system
		 * settings, simply return it
		 */
		if (image.getColorModel().equals(gfx_config.getColorModel())) {
			return image;
		}

		// image is not optimized, so create a new image that is
		BufferedImage new_image = gfx_config.createCompatibleImage(
				image.getWidth(), image.getHeight(), image.getTransparency());

		// get the graphics context of the new image to draw the old image on
		Graphics2D g2d = (Graphics2D) new_image.getGraphics();

		// actually draw the image and dispose of context no longer needed
		g2d.drawImage(image, 0, 0, null);
		g2d.dispose();

		// return the new optimized image
		return new_image;
	}

	public void renderVertex(Graphics g, int sand, int radius, int x, int y) {
		int colorNum = Math.max(0, Math.min(sand, SAND_COLOR.length - 1));
		if (color) {
			g.setColor(SAND_COLOR[colorNum]);
		} else {
			g.setColor(SAND_MONOCHROME[colorNum]);
		}
		//g.setColor(new Color(+64));
		g.fillOval(x - radius, y - radius, radius * 2, radius * 2);

	}

	public void setRepaint(boolean val) {
		repaint = val;
		repaint();
	}

	public void setRepaintAll(boolean val) {
		repaintAll = val;
	//repaint();
	}

	public void setUseBufferedImages(boolean val) {
		useBufferedImages = val;
	}

	public void setOutputFPS(boolean val) {
		outputFPS = val;
	}

	public void setDelay(long ms) {
		delay = ms;
	}

	public void setColor(boolean val) {
		color = val;
		repaint();
	}

	public void setLabels(boolean val) {
		labels = val;
		repaint();
	}

	public void setDrawEdges(boolean val) {
		this.drawEdges = val;
		repaint();
	}

	public void setChangingNodeSize(boolean val) {
		changingNodeSize = val;
		repaint();
	}

	public void actionPerformed(ActionEvent evt) {
		update();
	}

	/**
	 * Fires all unstable vertices and repaints.
	 */
	public void update() {
		SandpileConfiguration nextConfig = sg.updateConfig(currentConfig);
		repaint();/*
		if (repaintAll) {
			repaint();
		} else {
			for (int i = 0; i < currentConfig.size(); i++) {
				if (nextConfig.get(i) != currentConfig.get(i)) {
					float[] v = vertexData.get(i);
					this.repaint(scaleCoordinate(v[0] - VERT_RADIUS), scaleCoordinate(v[1] - VERT_RADIUS), scaleCoordinate(VERT_RADIUS * 2), scaleCoordinate(VERT_RADIUS * 2));
				}
			}
		//System.out.println(count);
		}*/
		currentConfig = nextConfig;
		if (outputFPS) {
			long curTime = System.currentTimeMillis();
			fps = 1.0 / (curTime - lastUpdate) * 1000.0;
			System.out.println(fps);
			lastUpdate = System.currentTimeMillis();
		}
	}

	public void run() {
		while (!Thread.interrupted()) {
			this.update();
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				return;
			}
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		if (!repaint) {
			return;
		}

		//Graphics2D g2 = (Graphics2D) g;
		Graphics g2 = g;
		super.paintComponent(g2);
		//drawer.paintSandpileGraph(sg, vertexData, currentConfig);
		/*
		if (drawEdges) {
			for (int e1 = 0; e1 < edges.size(); e1++) {
				for (int e2 : edges.get(e1)) {
					int[] pos1 = vertexData.get(e1);
					int[] pos2 = vertexData.get(e2);
					g2.setColor(Color.white);
					g2.drawLine(scaleCoordinate(pos1[0]), scaleCoordinate(pos1[1]), scaleCoordinate(pos2[0]), scaleCoordinate(pos2[1]));
					g2.setColor(Color.pink);
					if (labels) {
						g2.drawString(String.valueOf(sg.weight(e1, e2)), scaleCoordinate((int) (pos1[0] + 0.8 * (pos2[0] - pos1[0]))), scaleCoordinate((int) (pos1[1] + 0.8 * (pos2[1] - pos1[1]))));
					}
				}
			}
		}
		for (int i = 0; i < vertexData.size(); i++) {
			int[] v = vertexData.get(i);
			int sand = currentConfig.get(i);
			int colorNum = Math.max(0, Math.min(sand, SAND_COLOR.length - 1));
			if (useBufferedImages) {
				//int r = VERT_RADIUS;
				g2.drawImage(vertexImages[colorNum], scaleCoordinate(v[0] - VERT_RADIUS), scaleCoordinate(v[1] - VERT_RADIUS), new Color(0,0,0,0),null);
			} else {
				int degree = sg.degree(i);
				int radius = VERT_RADIUS;

				if (changingNodeSize && (degree > 0 && degree > sand)) {
					radius = (int) (((float) sand + 2) / (degree + 2) * VERT_RADIUS);
				}
				if (color) {
					g2.setColor(SAND_COLOR[colorNum]);
				} else {
					g2.setColor(SAND_MONOCHROME[colorNum]);
				}
				//g.setColor(new Color(+64));
				g2.fillOval(scaleCoordinate(v[0] - radius), scaleCoordinate(v[1] - radius), scaleCoordinate(radius * 2), scaleCoordinate(radius * 2));
			}
		}
		if (labels) {
			g2.setColor(Color.black);
			for (int i = 0; i < vertexData.size(); i++) {
				int[] v = vertexData.get(i);
				int sand = currentConfig.get(i);

				g2.drawString(String.valueOf(sand), scaleCoordinate(v[0] - VERT_RADIUS / 2), scaleCoordinate(v[1] + VERT_RADIUS / 2));
			}
		}
		if (selectedVertex >= 0) {
			int[] v = vertexData.get(selectedVertex);
			g2.setColor(Color.cyan);
			g2.drawOval(scaleCoordinate(v[0] - VERT_RADIUS), scaleCoordinate(v[1] - VERT_RADIUS), scaleCoordinate(VERT_RADIUS * 2), scaleCoordinate(VERT_RADIUS * 2));
		}*/
	}

	public void addVertexControl(int x, int y) {
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
	}

	public void delVertexControl(int x, int y) {
		int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			//System.out.println("Del vertex "+x+" "+y);
			delVertex(touchVert);
		} else {
			selectedVertex = -1;
		}

	}

	public void addEdgeControl(int x, int y, int weight) {
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
	}

	public void delEdgeControl(int x, int y, int weight) {
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
	}

	public void addUndiEdgeControl(int x, int y, int weight) {
		int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			if (selectedVertex < 0) {
				selectedVertex = touchVert;
			} else if (touchVert != selectedVertex) {
				addEdge(selectedVertex, touchVert, weight);
				addEdge(touchVert, selectedVertex, weight);
			}
		}
	}

	public void delUndiEdgeControl(int x, int y, int weight) {
		int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			if (selectedVertex < 0) {
				selectedVertex = touchVert;
			} else if (touchVert != selectedVertex) {
				delEdge(selectedVertex, touchVert, weight);
				delEdge(touchVert, selectedVertex, weight);
			}
		}
	}

	public void addSandControl(int x, int y, int amount) {
		int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			selectedVertex = touchVert;
			addSand(touchVert, amount);
		}
	}

	public void setSandControl(int x, int y, int amount) {
		int touchVert = touchingVertex(x, y);
		if (touchVert >= 0) {
			selectedVertex = touchVert;
			setSand(touchVert, amount);
		}

	}

	public void makeGrid(int rows, int cols, int x, int y, int nBorder, int sBorder, int eBorder, int wBorder) {
		x = unscaleCoordinate(x);
		y = unscaleCoordinate(y);
		int gridSpacing = VERT_RADIUS * 2;
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
				addVertexUnscaled(x + j * gridSpacing, y + i * gridSpacing);
			}
		}

		for (int i = 0; i < cols; i++) {
			if (nBorder < 2) {
				nBorderRef[i] = vertexData.size();
				addVertexUnscaled(x + i * gridSpacing, y - gridSpacing);
			}
			if (sBorder < 2) {
				sBorderRef[i] = vertexData.size();
				addVertexUnscaled(x + i * gridSpacing, y + (rows) * gridSpacing);
			}

		}
		for (int i = 0; i < rows; i++) {
			if (wBorder < 2) {
				wBorderRef[i] = vertexData.size();
				addVertexUnscaled(x - gridSpacing, y + i * gridSpacing);
			}
			if (eBorder < 2) {
				eBorderRef[i] = vertexData.size();
				addVertexUnscaled(x + (cols) * gridSpacing, y + i * gridSpacing);
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

	public void makeHoneycomb(int radius, int x, int y, int borders) {
		/*
		 * for borders:
		 * 0 - directed
		 * 1 - undirected
		 **/
		x = unscaleCoordinate(x);
		y = unscaleCoordinate(y);
		int gridSpacing = VERT_RADIUS * 2;
		int curRowLength = radius;
		int[][] gridRef = new int[radius * 2 - 1][radius * 2 - 1];
		for (int i = 0; i < radius * 2 - 1; i++) {
			for (int j = 0; j < curRowLength; j++) {
				gridRef[i][j] = vertexData.size();
				addVertexUnscaled(x + j * gridSpacing + (i + (radius - 1) % 2) % 2 * (gridSpacing / 2) - curRowLength / 2 * (gridSpacing), y + i * (gridSpacing - 4));
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

	public void makeHexGrid(int rows, int cols, int x, int y, int nBorder, int sBorder, int eBorder, int wBorder) {
		x = unscaleCoordinate(x);
		y = unscaleCoordinate(y);
		int gridSpacing = VERT_RADIUS * 2;
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
				addVertexUnscaled(x + j * gridSpacing + i % 2 * (gridSpacing / 2), y + i * gridSpacing);
			}
		}

		for (int i = 0; i < cols + 1; i++) {
			if (nBorder < 2) {
				nBorderRef[i] = vertexData.size();
				addVertexUnscaled(x + i * gridSpacing - (gridSpacing / 2), y - gridSpacing);
			}
			if (sBorder < 2) {
				sBorderRef[i] = vertexData.size();
				addVertexUnscaled(x + i * gridSpacing - (gridSpacing / 2), y + (rows) * gridSpacing);
			}

		}

		for (int i = 0; i < rows; i++) {
			if (wBorder < 2) {
				wBorderRef[i] = vertexData.size();
				addVertexUnscaled(x - gridSpacing + i % 2 * (gridSpacing / 2), y + i * gridSpacing);
			}
			if (eBorder < 2) {
				eBorderRef[i] = vertexData.size();
				addVertexUnscaled(x + (cols) * gridSpacing + i % 2 * (gridSpacing / 2), y + i * gridSpacing);
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
	public void setScale(double scale) {
		this.scale = Math.max(scale, 0.0000001);
		this.calcVertexImages();
		repaint();
	}

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

	public void addVertex(int x, int y) {
		sg.addVertex();
		float[] newPos = {unscaleCoordinate(x), unscaleCoordinate(y)};
		vertexData.add(newPos);
		edges.add(new HashSet<Integer>());
		currentConfig.add(0);
		repaint();
	}

	private void addVertexUnscaled(int x, int y) {
		sg.addVertex();
		float[] newPos = {x, y};
		vertexData.add(newPos);
		edges.add(new HashSet<Integer>());
		currentConfig.add(0);
	//repaint();
	}

	public void delVertex(int v) {
		vertexData.remove(v);
		currentConfig.remove(v);
		sg.removeVertex(v);
		edges.remove(v);
		for (int i = 0; i < edges.size(); i++) {
			edges.get(i).remove(v);
			Set<Integer> updatedOutVerts = new HashSet<Integer>();
			for (Iterator<Integer> iter = edges.get(i).iterator(); iter.hasNext();) {
				int u = iter.next();
				if (u > v) {
					iter.remove();
					updatedOutVerts.add(u - 1);
				}
			}
			edges.get(i).addAll(updatedOutVerts);
		}
	}

	public void delAllVertices() {
		vertexData.clear();
		edges.clear();
		currentConfig.clear();
		sg.removeAllVertices();
		this.selectedVertex = -1;
		repaint();
	}

	public void addEdge(int originVert, int destVert) {
		addEdge(originVert, destVert, 1);
	}

	public void addEdge(int originVert, int destVert, int weight) {
		edges.get(originVert).add(destVert);
		sg.addEdge(originVert, destVert, weight);
	}

	public void delEdge(int originVert, int destVert) {
		this.delEdge(originVert, destVert, 1);
	}

	public void delEdge(int originVert, int destVert, int weight) {
		sg.removeEdge(originVert, destVert, weight);
		if (sg.weight(originVert, destVert) == 0) {
			edges.get(originVert).remove(destVert);
		}
	}

	public void addSand(int vert, int amount) {
		setSand(vert, currentConfig.get(vert) + amount);
	}

	public void setSand(int vert, int amount) {
		currentConfig.set(vert, amount);
	}

	private int touchingVertex(int x, int y) {
		for (int i = 0; i < vertexData.size(); i++) {
			float[] v = vertexData.get(i);
			if (Math.sqrt((x - scaleCoordinate(v[0])) * (x - scaleCoordinate(v[0])) + (y - scaleCoordinate(v[1])) * (y - scaleCoordinate(v[1]))) <= VERT_RADIUS) {
				return i;
			}
		}
		return -1;
	}

	private int scaleCoordinate(float coord) {
		return (int) Math.ceil(coord * scale);
	}

	private int unscaleCoordinate(float coord) {
		return (int) ((double) coord / scale);
	}

	public void editFromString(String s) {
		String[] parts = s.split(" ");
		if (parts[0].toLowerCase().equals("vertex")) {
			int x = Integer.valueOf(parts[1]);
			int y = Integer.valueOf(parts[2]);
			addVertexUnscaled(x, y);
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
				outBuffer.write(v);
				outBuffer.newLine();
			}
			outBuffer.close();
		} catch (IOException e) {
			System.err.println("Caught IOException while trying to save graph: " + e.getMessage());
		}
		repaint();
	}
}
