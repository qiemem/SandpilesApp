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
 * This panel contains swing elements for both controlling graphs,
 * configurations, etc. and for displaying the sandpiles themselve. It uses
 * a SandpileController to do all internal manipulations of graphs and
 * configurations. It has GLCanvases to display the sandpiles and
 * SandpileDrawers to dictate how that is done. Note that the fact that this
 * class uses GLCanvases, which are heavyweight, AWT components may create
 * problems on some computers. However, they have a significant performance gain
 * over GLPanels, which are the swing equivalent.
 * 
 * @author Bryan Head
 */
import java.awt.AWTException;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.datatransfer.*;
import javax.swing.Timer;
import java.util.Vector;
import java.util.HashSet;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.Robot;
import java.awt.Rectangle;
import java.util.Arrays;
import java.awt.Dimension;
import gnu.trove.TIntArrayList;

public class SandpilesInteractionPanel extends javax.swing.JPanel implements ClipboardOwner, SandpileChangeListener {
	private static final String MAKE_GRID_STATE = "Make Grid";
	private static final String MAKE_HEX_GRID_STATE = "Make Hex Grid";
	private static final String MAKE_HONEYCOMB_STATE = "Make Honeycomb";
	private static final String CONFIG_MANAGER_STATE = "Config Manager";
	private static final String VISUAL_OPTIONS_STATE = "Visual Options";
	private static final String EDIT_GRAPH_STATE = "Edit Graph";
	private static final String BUILD_LATTICE_STATE = "Build Lattice";

	//built-in configs
	private static final String MAX_CONFIG = "Max Stable";
	private static final String IDENTITY_CONFIG = "Identity";
	private static final String BURNING_CONFIG = "Burning";
	private static final String EQUIVALENT_RECURRENT = "Equivalent Recurrent";
	private static final String INVERSE_CONFIG = "Inverse";
	private static final String CURRENT_CONFIG = "Current";
	private static final String DUAL_CONFIG = "Dual of Current";
	private static final String ONES_CONFIG = "Ones Everywhere";
	private static final String RANDOM_CONFIG = "Random Grain";
	private final String[] defaultConfigs = { MAX_CONFIG, IDENTITY_CONFIG, BURNING_CONFIG, EQUIVALENT_RECURRENT, INVERSE_CONFIG, CURRENT_CONFIG, DUAL_CONFIG, ONES_CONFIG, RANDOM_CONFIG };

	private final int PORT = 7236;

	SandpileDrawer currentDrawer;



	public static enum MouseMode {
		SELECT(false), MOVE(true), EDIT(true);

		public final boolean scrollOnDrag;

		MouseMode(boolean scroll){
			scrollOnDrag = scroll;
		}
	}
	private float mouseX=0f, mouseY=0f;
	private float boxX, boxY;
	private boolean selecting = false;
	private boolean movingVertices = false;

	private SandpileController sandpileController;
	private SandpileGLDrawer drawer;
	private Sandpile3dDrawer drawer3d;

	private Clipboard localClipboard = new Clipboard("Sandpile Clipboard");

	private boolean runningThread=false;
	private Thread calculationThread;



    private boolean running = false;
	//private Thread spThread;
    private Timer runTimer;
	private Timer serverMsgChecker;


    /** Creates new form SandpilesInteractionPanel */
    public SandpilesInteractionPanel() {
        initComponents();

		drawer = new SandpileGLDrawer(canvas);
		sandpileController = new SandpileController(drawer);
		sandpileController.addSandpileChangeListener(this);
		canvas.addMouseListener(drawer);
		canvas.addMouseMotionListener(drawer);
		//drawer.addReshapeListener(this);

		drawer3d = new Sandpile3dDrawer(canvas3d);

		currentDrawer = drawer;

		runTimer = new Timer(0,sandpileController);
		runTimer.setDelay(0);
		updateDelayTextField();
		updateSelectedInfo();

		canvas.addMouseListener(new MouseAdapter(){
			@Override public void mousePressed(MouseEvent e){
				if(e.getButton()!=e.BUTTON1)
					return;
				float[] coords = drawer.transformCanvasCoords(e.getX(), e.getY());
				mouseX = coords[0];
				mouseY = coords[1];
				int vert = sandpileController.touchingVertex(mouseX, mouseY);
				
				if(sandpileController.getSelectedVertices().contains(vert)&&getMouseMode(e)!=MouseMode.MOVE){
					movingVertices = true;
				}else if(getMouseMode(e) == MouseMode.SELECT){
					boxX = mouseX;
					boxY = mouseY;
					selecting = true;
				}
			}
			@Override public void mouseClicked(MouseEvent e){
				if(e.getButton()!=e.BUTTON1)
					return;
				float[] coords = drawer.transformCanvasCoords(e.getX(), e.getY());
				mouseX = coords[0];
				mouseY = coords[1];
				int vert = sandpileController.touchingVertex(mouseX, mouseY);
				if(vert>=0&&getMouseMode(e) == MouseMode.SELECT){
					if(sandpileController.isSelected(vert)){
						sandpileController.unselectVertex(vert);
						updateSelectedInfo();
					}else{
						sandpileController.selectVertex(vert);
						updateSelectedInfo();
					}
				}else if(getMouseMode(e)==MouseMode.SELECT){
					sandpileController.unselectVertices();
					updateSelectedInfo();
				}
				sandpileController.repaint();
			}
			@Override public void mouseReleased(MouseEvent e){
				if(e.getButton()!=e.BUTTON1)
					return;
				if(getMouseMode(e) == MouseMode.SELECT)
					drawer.clearSelectionBox();
				selecting = false;
				movingVertices = false;
				sandpileController.repaint();
			}
		});
		canvas.addMouseMotionListener(new MouseMotionAdapter(){
			@Override public void mouseDragged(MouseEvent e){
				if(e.getButton()!=e.BUTTON1)
					return;
				float[] coords = drawer.transformCanvasCoords(e.getX(), e.getY());
				int vert = sandpileController.touchingVertex(mouseX, mouseY);
				if(movingVertices){
					sandpileController.moveVertices(sandpileController.getSelectedVertices(), coords[0]-mouseX, coords[1]-mouseY);
					sandpileController.repaint();
				}else if(selecting){
					float maxX = Math.max(boxX, coords[0]);
					float maxY = Math.max(boxY, coords[1]);
					float minX = Math.min(boxX, coords[0]);
					float minY = Math.min(boxY, coords[1]);
					drawer.setSelectionBox(maxX, maxY, minX, minY);
					sandpileController.setSelectedVertices(sandpileController.getVerticesInRect(maxX, maxY, minX, minY));
					updateSelectedInfo();
					sandpileController.repaint();
				}
				mouseX = coords[0];
				mouseY = coords[1];
				updateCenterCoordLabel();
			}
		});
		MouseWheelListener zoomListener = new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent evt){
				updateZoomTextField();
			}
		};
		canvas.addMouseWheelListener(zoomListener);
		canvas3d.addMouseWheelListener(zoomListener);

		final SandpilesInteractionPanel me = this;
		serverMsgChecker = new Timer(0, new ActionListener(){
			public void actionPerformed(ActionEvent e){
				try{
					sandpileController.receiveMessage();
				}catch(IOException error){
					JOptionPane.showMessageDialog(me, "Error while checking for messages from client: " + error.getMessage(), "Socket Erro", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		serverMsgChecker.stop();
    }

//	public void onReshape() {
//		updateZoomTextField();
//		this.centerCoordLabel.setText(String.format("%.2f, %.2f", drawer.getOriginX(), drawer.getOriginY()));
//	}

	public void setColors(Float2dArrayList colors, Float2dArrayList inDebtColors, float[] backgroundColor){
		drawer.setColors(colors, inDebtColors, backgroundColor);
		drawer3d.setColors(colors, inDebtColors, backgroundColor);
	}

	public void copyVertexDataToClipboard(Float2dArrayList locationData, TIntArrayList sandData, EdgeList edgeData){
		localClipboard.setContents(new SandpileTransferable(locationData, sandData, edgeData), this);
	}
	
	public void copySelectedToClipboard(){
		TIntArrayList vertices = sandpileController.getSelectedVertices();
		Float2dArrayList locationData = new Float2dArrayList(0,2);
		TIntArrayList configData = new TIntArrayList();
		EdgeList edgeData = new EdgeList();
		int vert = 0;
		for(int i=0; i< vertices.size(); i++){
			int v = vertices.get(i);
			float x = sandpileController.getVertexX(v)-drawer.getOriginX();
			float y = sandpileController.getVertexY(v)-drawer.getOriginY();
			locationData.addRow(x,y);
			configData.add(sandpileController.getSand(v));
			for(Edge e : sandpileController.getGraph().getOutgoingEdges(v)){
				int w = e.dest();
				int destVert = vertices.indexOf(w);
				if(destVert>=0){
					edgeData.add(vert,destVert,sandpileController.getGraph().weight(v, w));
				}
			}
			vert++;
		}
		copyVertexDataToClipboard(locationData,configData, edgeData);
	}

	public void cutSelectedToClipBoard(){
		copySelectedToClipboard();
		sandpileController.delVerticesControl(sandpileController.getSelectedVertices());
		//sandpileController.unselectVertices();
		sandpileController.repaint();
	}

	public void pasteVertexDataFromClipboard(){
		if(!localClipboard.isDataFlavorAvailable(DataFlavor.getTextPlainUnicodeFlavor())) return;
		SandpileTransferable data = (SandpileTransferable) localClipboard.getContents(this);
		Float2dArrayList locationData = data.getLocationData();
		TIntArrayList configData = data.getConfigData();
		int startingIndex = sandpileController.getConfig().size();
		for(int i=0; i<locationData.rows(); i++){
			sandpileController.addVertex(locationData.get(i,0)+drawer.getOriginX(), locationData.get(i,1)+drawer.getOriginY());
			sandpileController.setSand(i, configData.get(i));
		}
		for(Edge e : data.getEdgeData()){
			sandpileController.addEdge(e.source()+startingIndex,e.dest()+startingIndex,e.wt());
		}
		sandpileController.repaint();
	}

	public void lostOwnership(Clipboard clipboard, Transferable contents) {
		System.err.println("Lost clipboard ownership");
	}

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        canvas = new javax.media.opengl.GLCanvas();
        controlStateComboBox = new javax.swing.JComboBox();
        optionsContainerPanel = new javax.swing.JPanel();
        blankOptionsPanel = new javax.swing.JPanel();
        makeHexGridOptionsPanel = new javax.swing.JPanel();
        gridSizeLabel1 = new javax.swing.JLabel();
        hexGridRowsField = new javax.swing.JTextField();
        gridSizeCrossLabel1 = new javax.swing.JLabel();
        hexGridColsField = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        hexNBorderComboBox = new javax.swing.JComboBox();
        jLabel15 = new javax.swing.JLabel();
        hexSBorderComboBox = new javax.swing.JComboBox();
        jLabel16 = new javax.swing.JLabel();
        hexEBorderComboBox = new javax.swing.JComboBox();
        jLabel17 = new javax.swing.JLabel();
        hexWBorderComboBox = new javax.swing.JComboBox();
        editConfigButtonGroup = new javax.swing.ButtonGroup();
        editGraphButtonGroup = new javax.swing.ButtonGroup();
        sandpileViewScrollPane = new javax.swing.JScrollPane();
        mouseButtonGroup = new javax.swing.ButtonGroup();
        repaintOptionsButtonGroup = new javax.swing.ButtonGroup();
        canvas3d = new javax.media.opengl.GLCanvas();
        jSplitPane1 = new javax.swing.JSplitPane();
        controlPanel = new javax.swing.JPanel();
        quitButton = new javax.swing.JButton();
        optionsTabbedPane = new javax.swing.JTabbedPane();
        editGraphPanel = new javax.swing.JPanel();
        addVertexRadioButton = new javax.swing.JRadioButton();
        removeVertexRadioButton = new javax.swing.JRadioButton();
        addEdgeRadioButton = new javax.swing.JRadioButton();
        removeEdgeRadioButton = new javax.swing.JRadioButton();
        addUndirectedEdgeRadioButton = new javax.swing.JRadioButton();
        removeUndirectedEdgeRadioButton = new javax.swing.JRadioButton();
        edgeWeightField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        deleteSelectedVerticesButton = new javax.swing.JButton();
        makeSinkButton = new javax.swing.JButton();
        configManagerOptionsPanel = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        configSelectList = new javax.swing.JList();
        addConfigButton = new javax.swing.JButton();
        setConfigButton = new javax.swing.JButton();
        addSandRadioButton = new javax.swing.JRadioButton();
        jLabel7 = new javax.swing.JLabel();
        removeSandRadioButton = new javax.swing.JRadioButton();
        setSandRadioButton = new javax.swing.JRadioButton();
        jLabel8 = new javax.swing.JLabel();
        storeConfigButton = new javax.swing.JButton();
        removeConfigButton = new javax.swing.JButton();
        jLabel25 = new javax.swing.JLabel();
        subtractConfigButton = new javax.swing.JButton();
        configTimesSpinner = new javax.swing.JSpinner();
        amountOfSandSpinner = new javax.swing.JSpinner();
        makeGridOptionsPanel = new javax.swing.JPanel();
        gridSizeLabel = new javax.swing.JLabel();
        gridRowsField = new javax.swing.JTextField();
        gridSizeCrossLabel = new javax.swing.JLabel();
        gridColsField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        nBorderComboBox = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        sBorderComboBox = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        eBorderComboBox = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        wBorderComboBox = new javax.swing.JComboBox();
        makeHoneycombOptionsPanel = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        makeHoneycombBorderComboBox = new javax.swing.JComboBox();
        jLabel10 = new javax.swing.JLabel();
        makeHoneycombRadiusField = new javax.swing.JTextField();
        buildLaticePanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        buildLatticeTable = new javax.swing.JTable();
        rowSpinner = new javax.swing.JSpinner();
        colSpinner = new javax.swing.JSpinner();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        latticeSpacingSpinner = new javax.swing.JSpinner();
        jLabel24 = new javax.swing.JLabel();
        visualOptionsPanel = new javax.swing.JPanel();
        repaintCheckBox = new javax.swing.JCheckBox();
        edgeLabelsCheckBox = new javax.swing.JCheckBox();
        changingNodeSizeCheckBox = new javax.swing.JCheckBox();
        drawEdgesCheckBox = new javax.swing.JCheckBox();
        printFPSCheckBox = new javax.swing.JCheckBox();
        jLabel18 = new javax.swing.JLabel();
        colorModeComboBox = new javax.swing.JComboBox();
        repaintDelayRadioButton = new javax.swing.JRadioButton();
        repaintOnUpdateRadioButton = new javax.swing.JRadioButton();
        repaintDelayTextField = new javax.swing.JTextField();
        vertexLabelsCheckBox = new javax.swing.JCheckBox();
        dimensionToggleButton = new javax.swing.JToggleButton();
        heightSmoothingSpinner = new javax.swing.JSpinner();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        colorSmoothingSpinner = new javax.swing.JSpinner();
        heightScalarSpinner = new javax.swing.JSpinner();
        drawShapeCheckBox = new javax.swing.JCheckBox();
        drawWireCheckBox = new javax.swing.JCheckBox();
        jLabel13 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        canvasHolderPanel = new javax.swing.JPanel();
        infoToolBar = new javax.swing.JToolBar();
        centerLabel = new javax.swing.JLabel();
        centerCoordLabel = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JToolBar.Separator();
        currentActionLabel = new javax.swing.JLabel();
        cancelButton = new javax.swing.JButton();
        jSeparator6 = new javax.swing.JToolBar.Separator();
        selecteCenterButton = new javax.swing.JButton();
        selectedSandLabel = new javax.swing.JLabel();
        selectedDegreeLabel = new javax.swing.JLabel();
        selectedFiringsLabel = new javax.swing.JLabel();
        controlToolBar = new javax.swing.JToolBar();
        runButton = new javax.swing.JToggleButton();
        stepButton = new javax.swing.JButton();
        stabilizeButton = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        clearSandButton = new javax.swing.JButton();
        deleteGraphButton = new javax.swing.JButton();
        resetFiringsButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        delayLabel = new javax.swing.JLabel();
        delayTextField = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        bigDecDelayButton = new javax.swing.JButton();
        smallDevDelayButton = new javax.swing.JButton();
        smallIncDelayButton = new javax.swing.JButton();
        bigIncDelayButton = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        jLabel2 = new javax.swing.JLabel();
        zoomTextField = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        bigZoomOutButton = new javax.swing.JButton();
        smallZoomOutButton = new javax.swing.JButton();
        smallZoomInButton = new javax.swing.JButton();
        bigZoomInButton = new javax.swing.JButton();
        serverToggleButton = new javax.swing.JToggleButton();
        jSeparator5 = new javax.swing.JToolBar.Separator();
        mouseToolBar = new javax.swing.JToolBar();
        selectToggleButton = new javax.swing.JToggleButton();
        editToggleButton = new javax.swing.JToggleButton();

        canvas.setMinimumSize(new java.awt.Dimension(0,0));
        canvas.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                canvasMouseReleased(evt);
            }
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                canvasMouseClicked(evt);
            }
        });

        controlStateComboBox.setMaximumRowCount(16);
        controlStateComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { EDIT_GRAPH_STATE, CONFIG_MANAGER_STATE, MAKE_GRID_STATE, MAKE_HEX_GRID_STATE, MAKE_HONEYCOMB_STATE, VISUAL_OPTIONS_STATE}));
        controlStateComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                controlStateComboBoxItemStateChanged(evt);
            }
        });
        controlStateComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                controlStateComboBoxActionPerformed(evt);
            }
        });

        optionsContainerPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Options"));
        optionsContainerPanel.setLayout(new java.awt.CardLayout());
        //optionsContainerPanel.add(blankOptionsPanel, ADD_VERTEX_STATE);
        //optionsContainerPanel.add(blankOptionsPanel, DEL_VERTEX_STATE);
        optionsContainerPanel.add(makeGridOptionsPanel, MAKE_GRID_STATE);
        //optionsContainerPanel.add(addSandOptionsPanel, ADD_SAND_STATE);
        //optionsContainerPanel.add(addSandOptionsPanel, DEL_SAND_STATE);
        //optionsContainerPanel.add(addEdgeOptionsPanel, ADD_EDGE_STATE);
        optionsContainerPanel.add(makeHoneycombOptionsPanel, MAKE_HONEYCOMB_STATE);
        optionsContainerPanel.add(configManagerOptionsPanel, CONFIG_MANAGER_STATE);
        optionsContainerPanel.add(visualOptionsPanel, VISUAL_OPTIONS_STATE);
        optionsContainerPanel.add(editGraphPanel, EDIT_GRAPH_STATE);
        //optionsContainerPanel.add(addEdgeOptionsPanel, DEL_EDGE_STATE);
        //optionsContainerPanel.add(addEdgeOptionsPanel, ADD_UNDI_EDGE_STATE);
        //optionsContainerPanel.add(addEdgeOptionsPanel, DEL_UNDI_EDGE_STATE);

        org.jdesktop.layout.GroupLayout blankOptionsPanelLayout = new org.jdesktop.layout.GroupLayout(blankOptionsPanel);
        blankOptionsPanel.setLayout(blankOptionsPanelLayout);
        blankOptionsPanelLayout.setHorizontalGroup(
            blankOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 55, Short.MAX_VALUE)
        );
        blankOptionsPanelLayout.setVerticalGroup(
            blankOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 17, Short.MAX_VALUE)
        );

        gridSizeLabel1.setText("Grid Size:"); // NOI18N

        hexGridRowsField.setText("5"); // NOI18N
        hexGridRowsField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hexGridRowsFieldActionPerformed(evt);
            }
        });
        hexGridRowsField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                hexGridRowsFieldPropertyChange(evt);
            }
        });

        gridSizeCrossLabel1.setText("X"); // NOI18N

        hexGridColsField.setText("5"); // NOI18N
        hexGridColsField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hexGridColsFieldActionPerformed(evt);
            }
        });

        jLabel14.setText("N Border:"); // NOI18N

        hexNBorderComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Directed", "Undirected", "None" }));
        hexNBorderComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hexNBorderComboBoxActionPerformed(evt);
            }
        });

        jLabel15.setText("S Border:"); // NOI18N

        hexSBorderComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Directed", "Undirected", "None" }));
        hexSBorderComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hexSBorderComboBoxActionPerformed(evt);
            }
        });

        jLabel16.setText("E Border:"); // NOI18N

        hexEBorderComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Directed", "Undirected", "None" }));
        hexEBorderComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hexEBorderComboBoxActionPerformed(evt);
            }
        });

        jLabel17.setText("W Border:"); // NOI18N

        hexWBorderComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Directed", "Undirected", "None" }));
        hexWBorderComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hexWBorderComboBoxActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout makeHexGridOptionsPanelLayout = new org.jdesktop.layout.GroupLayout(makeHexGridOptionsPanel);
        makeHexGridOptionsPanel.setLayout(makeHexGridOptionsPanelLayout);
        makeHexGridOptionsPanelLayout.setHorizontalGroup(
            makeHexGridOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(makeHexGridOptionsPanelLayout.createSequentialGroup()
                .add(makeHexGridOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(makeHexGridOptionsPanelLayout.createSequentialGroup()
                        .add(gridSizeLabel1)
                        .add(2, 2, 2)
                        .add(hexGridRowsField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 40, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(gridSizeCrossLabel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(hexGridColsField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 39, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(makeHexGridOptionsPanelLayout.createSequentialGroup()
                        .add(jLabel15)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(hexSBorderComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(makeHexGridOptionsPanelLayout.createSequentialGroup()
                        .add(jLabel14)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(hexNBorderComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(makeHexGridOptionsPanelLayout.createSequentialGroup()
                        .add(makeHexGridOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel16)
                            .add(jLabel17))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(makeHexGridOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(hexWBorderComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(hexEBorderComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .add(167, 167, 167))
        );
        makeHexGridOptionsPanelLayout.setVerticalGroup(
            makeHexGridOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(makeHexGridOptionsPanelLayout.createSequentialGroup()
                .add(makeHexGridOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(gridSizeLabel1)
                    .add(hexGridRowsField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(gridSizeCrossLabel1)
                    .add(hexGridColsField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(makeHexGridOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel14)
                    .add(hexNBorderComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(makeHexGridOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel15)
                    .add(hexSBorderComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(makeHexGridOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel16)
                    .add(hexEBorderComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(makeHexGridOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel17)
                    .add(hexWBorderComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(511, Short.MAX_VALUE))
        );

        sandpileViewScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        sandpileViewScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        canvas3d.setMinimumSize(new Dimension(0,0));

        setPreferredSize(new java.awt.Dimension(1024, 768));
        setRequestFocusEnabled(false);
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                formMouseEntered(evt);
            }
        });

        jSplitPane1.setDividerLocation(350);
        jSplitPane1.setDividerSize(8);
        jSplitPane1.setOneTouchExpandable(true);
        jSplitPane1.setPreferredSize(new java.awt.Dimension(996, 800));

        quitButton.setText("Quit"); // NOI18N
        quitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quitButtonActionPerformed(evt);
            }
        });

        optionsTabbedPane.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        optionsTabbedPane.setTabPlacement(javax.swing.JTabbedPane.LEFT);
        optionsTabbedPane.setMinimumSize(new java.awt.Dimension(200, 44));
        optionsTabbedPane.setPreferredSize(new java.awt.Dimension(200, 621));

        editGraphPanel.setToolTipText("Add/remove vertices and edges.");

        editGraphButtonGroup.add(addVertexRadioButton);
        addVertexRadioButton.setSelected(true);
        addVertexRadioButton.setText("Add Vertex"); // NOI18N
        addVertexRadioButton.setToolTipText("Clicking on the canvas will create a new vertex.");

        editGraphButtonGroup.add(removeVertexRadioButton);
        removeVertexRadioButton.setText("Remove Vertex"); // NOI18N
        removeVertexRadioButton.setToolTipText("Click on a vertex to remove it.");

        editGraphButtonGroup.add(addEdgeRadioButton);
        addEdgeRadioButton.setText("Add Edge"); // NOI18N
        addEdgeRadioButton.setToolTipText("Clicking on a vertex will create an edge from any selected vertices to the clicked on vertex.");

        editGraphButtonGroup.add(removeEdgeRadioButton);
        removeEdgeRadioButton.setText("Remove Edge"); // NOI18N
        removeEdgeRadioButton.setToolTipText("Clicking on a vertex will decrease the weight between any selected vertices and this vertex.");

        editGraphButtonGroup.add(addUndirectedEdgeRadioButton);
        addUndirectedEdgeRadioButton.setText("Add Undirected Edge"); // NOI18N
        addUndirectedEdgeRadioButton.setToolTipText("Clicking on a vertex will create an undirected edge between it and any selected vertices.");

        editGraphButtonGroup.add(removeUndirectedEdgeRadioButton);
        removeUndirectedEdgeRadioButton.setText("Remove Undirected Edge"); // NOI18N
        removeUndirectedEdgeRadioButton.setToolTipText("Clicking on a vertex will decrease the weight of any edge between it and any selected vertices (no matter which way the edges go).");

        edgeWeightField.setText("1"); // NOI18N
        edgeWeightField.setToolTipText("The amount to increase or decrease the weight of edges modified with the above tools.");

        jLabel1.setText("Edge Weight"); // NOI18N

        deleteSelectedVerticesButton.setText("Delete Selected Vertices");
        deleteSelectedVerticesButton.setToolTipText("Deletes the selected vertices; this can be slow if deleting hundreds of vertices.");
        deleteSelectedVerticesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteSelectedVerticesButtonActionPerformed(evt);
            }
        });

        makeSinkButton.setText("Turn into Sink");
        makeSinkButton.setToolTipText("Removes all outgoing edges from the selected vertices.");
        makeSinkButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                makeSinkButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout editGraphPanelLayout = new org.jdesktop.layout.GroupLayout(editGraphPanel);
        editGraphPanel.setLayout(editGraphPanelLayout);
        editGraphPanelLayout.setHorizontalGroup(
            editGraphPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(editGraphPanelLayout.createSequentialGroup()
                .add(editGraphPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(addVertexRadioButton)
                    .add(removeVertexRadioButton)
                    .add(addUndirectedEdgeRadioButton)
                    .add(removeUndirectedEdgeRadioButton)
                    .add(editGraphPanelLayout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(edgeWeightField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 36, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(addEdgeRadioButton)
                    .add(removeEdgeRadioButton)
                    .add(makeSinkButton)
                    .add(deleteSelectedVerticesButton))
                .addContainerGap(108, Short.MAX_VALUE))
        );
        editGraphPanelLayout.setVerticalGroup(
            editGraphPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(editGraphPanelLayout.createSequentialGroup()
                .add(addVertexRadioButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(removeVertexRadioButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(addEdgeRadioButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(removeEdgeRadioButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(addUndirectedEdgeRadioButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(removeUndirectedEdgeRadioButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(editGraphPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(edgeWeightField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(deleteSelectedVerticesButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(makeSinkButton)
                .addContainerGap(413, Short.MAX_VALUE))
        );

        optionsTabbedPane.addTab(EDIT_GRAPH_STATE, editGraphPanel);

        configManagerOptionsPanel.setPreferredSize(new java.awt.Dimension(150, 600));
        configManagerOptionsPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                configManagerOptionsPanelComponentShown(evt);
            }
        });

        //DefaultListModel configSelectListModel = new DefaultListModel();
        //configSelectListModel.addElement(MAX_CONFIG);
        //configSelectListModel.addElement(IDENTITY_CONFIG);
        //configSelectListModel.addElement(BURNING_CONFIG);
        //configSelectListModel.addElement(ONES_CONFIG);
        jScrollPane3.setViewportView(configSelectList);

        addConfigButton.setText("Add"); // NOI18N
        addConfigButton.setToolTipText("Adds the selected configuration to the current configuration the indicated number of times.");
        addConfigButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addConfigButtonActionPerformed(evt);
            }
        });

        setConfigButton.setText("Set"); // NOI18N
        setConfigButton.setToolTipText("Sets the current configuration to the selected configuration multiplied by the times field.");
        setConfigButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setConfigButtonActionPerformed(evt);
            }
        });

        editConfigButtonGroup.add(addSandRadioButton);
        addSandRadioButton.setSelected(true);
        addSandRadioButton.setText("Add Sand"); // NOI18N

        jLabel7.setText("On click:"); // NOI18N

        editConfigButtonGroup.add(removeSandRadioButton);
        removeSandRadioButton.setText("Remove Sand"); // NOI18N

        editConfigButtonGroup.add(setSandRadioButton);
        setSandRadioButton.setText("Set Sand"); // NOI18N
        setSandRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setSandRadioButtonActionPerformed(evt);
            }
        });

        jLabel8.setText("Amount:"); // NOI18N

        storeConfigButton.setText("Store");
        storeConfigButton.setToolTipText("Stores the current configuration in the config manager so that it may be used later. Editing the graph in any way will remove custom configurations.");
        storeConfigButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                storeConfigButtonActionPerformed(evt);
            }
        });

        removeConfigButton.setText("Remove");
        removeConfigButton.setToolTipText("Removes the currently selected custom configuration.");
        removeConfigButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeConfigButtonActionPerformed(evt);
            }
        });

        jLabel25.setText("Times:");

        subtractConfigButton.setText("Subtract");
        subtractConfigButton.setToolTipText("Subtracts the selected configuration to the current configuration the indicated number of times.");
        subtractConfigButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subtractConfigButtonActionPerformed(evt);
            }
        });

        configTimesSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(1), null, Integer.valueOf(1)));
        configTimesSpinner.setToolTipText("The number of times to apply the above operations.");

        amountOfSandSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(1), null, null, Integer.valueOf(1)));
        amountOfSandSpinner.setToolTipText("The number of grains the above operations will use.");

        org.jdesktop.layout.GroupLayout configManagerOptionsPanelLayout = new org.jdesktop.layout.GroupLayout(configManagerOptionsPanel);
        configManagerOptionsPanel.setLayout(configManagerOptionsPanelLayout);
        configManagerOptionsPanelLayout.setHorizontalGroup(
            configManagerOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(configManagerOptionsPanelLayout.createSequentialGroup()
                .add(addConfigButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(subtractConfigButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 56, Short.MAX_VALUE)
                .add(setConfigButton))
            .add(configManagerOptionsPanelLayout.createSequentialGroup()
                .add(storeConfigButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 133, Short.MAX_VALUE)
                .add(removeConfigButton))
            .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 302, Short.MAX_VALUE)
            .add(configManagerOptionsPanelLayout.createSequentialGroup()
                .add(2, 2, 2)
                .add(configManagerOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel7)
                    .add(addSandRadioButton)
                    .add(removeSandRadioButton)
                    .add(configManagerOptionsPanelLayout.createSequentialGroup()
                        .add(jLabel25)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(configTimesSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 72, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(setSandRadioButton)
                    .add(configManagerOptionsPanelLayout.createSequentialGroup()
                        .add(jLabel8)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(amountOfSandSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 83, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(153, Short.MAX_VALUE))
        );
        configManagerOptionsPanelLayout.setVerticalGroup(
            configManagerOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(configManagerOptionsPanelLayout.createSequentialGroup()
                .add(configManagerOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(storeConfigButton)
                    .add(removeConfigButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 167, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(configManagerOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(addConfigButton)
                    .add(setConfigButton)
                    .add(subtractConfigButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(configManagerOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel25)
                    .add(configTimesSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(jLabel7)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(addSandRadioButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(removeSandRadioButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 23, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(setSandRadioButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(configManagerOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel8)
                    .add(amountOfSandSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(249, Short.MAX_VALUE))
        );

        optionsTabbedPane.addTab(CONFIG_MANAGER_STATE, configManagerOptionsPanel);

        gridSizeLabel.setText("Grid Size:"); // NOI18N

        gridRowsField.setText("50"); // NOI18N
        gridRowsField.setToolTipText("Number of rows.");
        gridRowsField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gridRowsFieldActionPerformed(evt);
            }
        });
        gridRowsField.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                gridRowsFieldPropertyChange(evt);
            }
        });

        gridSizeCrossLabel.setText("X"); // NOI18N

        gridColsField.setText("50"); // NOI18N
        gridColsField.setToolTipText("Number of columns.");
        gridColsField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gridColsFieldActionPerformed(evt);
            }
        });

        jLabel3.setText("N Border:"); // NOI18N

        nBorderComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Directed", "Undirected", "None", "Looped to S", "Looped to S Rev." }));
        nBorderComboBox.setToolTipText("");
        nBorderComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nBorderComboBoxActionPerformed(evt);
            }
        });

        jLabel4.setText("S Border:"); // NOI18N

        sBorderComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Directed", "Undirected", "None", "Looped to N", "Looped to N Rev." }));
        sBorderComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sBorderComboBoxActionPerformed(evt);
            }
        });

        jLabel5.setText("E Border:"); // NOI18N

        eBorderComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Directed", "Undirected", "None", "Looped to W", "Looped to W Rev." }));
        eBorderComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                eBorderComboBoxActionPerformed(evt);
            }
        });

        jLabel6.setText("W Border:"); // NOI18N

        wBorderComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Directed", "Undirected", "None", "Looped to E", "Looped to E Rev." }));
        wBorderComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wBorderComboBoxActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout makeGridOptionsPanelLayout = new org.jdesktop.layout.GroupLayout(makeGridOptionsPanel);
        makeGridOptionsPanel.setLayout(makeGridOptionsPanelLayout);
        makeGridOptionsPanelLayout.setHorizontalGroup(
            makeGridOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(makeGridOptionsPanelLayout.createSequentialGroup()
                .add(gridSizeLabel)
                .add(2, 2, 2)
                .add(gridRowsField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 40, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(gridSizeCrossLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(gridColsField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 39, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
            .add(makeGridOptionsPanelLayout.createSequentialGroup()
                .add(makeGridOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel3)
                    .add(jLabel4)
                    .add(jLabel6)
                    .add(jLabel5))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(makeGridOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, sBorderComboBox, 0, 238, Short.MAX_VALUE)
                    .add(nBorderComboBox, 0, 238, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, eBorderComboBox, 0, 238, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, wBorderComboBox, 0, 238, Short.MAX_VALUE)))
        );
        makeGridOptionsPanelLayout.setVerticalGroup(
            makeGridOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(makeGridOptionsPanelLayout.createSequentialGroup()
                .add(makeGridOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(gridSizeLabel)
                    .add(gridRowsField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(gridSizeCrossLabel)
                    .add(gridColsField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(makeGridOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(nBorderComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(makeGridOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(sBorderComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(makeGridOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel5)
                    .add(eBorderComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(makeGridOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel6)
                    .add(wBorderComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(511, Short.MAX_VALUE))
        );

        optionsTabbedPane.addTab(MAKE_GRID_STATE, makeGridOptionsPanel);

        jLabel9.setText("Border:"); // NOI18N

        makeHoneycombBorderComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Directed", "Undirected" }));
        makeHoneycombBorderComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                makeHoneycombBorderComboBoxActionPerformed(evt);
            }
        });

        jLabel10.setText("Radius:"); // NOI18N

        makeHoneycombRadiusField.setText("50"); // NOI18N
        makeHoneycombRadiusField.setToolTipText("Number of layers of vertices.");
        makeHoneycombRadiusField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                makeHoneycombRadiusFieldActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout makeHoneycombOptionsPanelLayout = new org.jdesktop.layout.GroupLayout(makeHoneycombOptionsPanel);
        makeHoneycombOptionsPanel.setLayout(makeHoneycombOptionsPanelLayout);
        makeHoneycombOptionsPanelLayout.setHorizontalGroup(
            makeHoneycombOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(makeHoneycombOptionsPanelLayout.createSequentialGroup()
                .add(makeHoneycombOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(makeHoneycombOptionsPanelLayout.createSequentialGroup()
                        .add(jLabel9)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(makeHoneycombBorderComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(makeHoneycombOptionsPanelLayout.createSequentialGroup()
                        .add(jLabel10)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(makeHoneycombRadiusField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 41, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .add(184, 184, 184))
        );
        makeHoneycombOptionsPanelLayout.setVerticalGroup(
            makeHoneycombOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(makeHoneycombOptionsPanelLayout.createSequentialGroup()
                .add(makeHoneycombOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel10)
                    .add(makeHoneycombRadiusField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(makeHoneycombOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel9)
                    .add(makeHoneycombBorderComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(604, Short.MAX_VALUE))
        );

        optionsTabbedPane.addTab(MAKE_HONEYCOMB_STATE, makeHoneycombOptionsPanel);

        buildLatticeTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {new Integer(1), new Integer(0), new Integer(0), new Integer(1), new Integer(0), new Integer(2), new Boolean(true), new Integer(1)},
                {new Integer(-1), new Integer(0), new Integer(0), new Integer(1), new Integer(1), new Integer(2), new Boolean(true), new Integer(1)},
                {new Integer(0), new Integer(1), new Integer(0), new Integer(2), new Integer(0), new Integer(1), new Boolean(true), new Integer(1)},
                {new Integer(0), new Integer(-1), new Integer(1), new Integer(2), new Integer(0), new Integer(1), new Boolean(true), new Integer(1)},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null}
            },
            new String [] {
                "x", "y", "x start", "x freq", "y start", "y freq", "Dir", "Weight"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Boolean.class, java.lang.Integer.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        buildLatticeTable.setToolTipText("Creates a lattice based on the integer vectors. The default is the manhattan lattice, made up of one way streets switching directions every other row/column.");
        buildLatticeTable.setCellSelectionEnabled(true);
        jScrollPane1.setViewportView(buildLatticeTable);
        buildLatticeTable.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        buildLatticeTable.getColumnModel().getColumn(0).setHeaderValue("x");
        buildLatticeTable.getColumnModel().getColumn(1).setHeaderValue("y");
        buildLatticeTable.getColumnModel().getColumn(2).setHeaderValue("x start");
        buildLatticeTable.getColumnModel().getColumn(3).setHeaderValue("x freq");
        buildLatticeTable.getColumnModel().getColumn(4).setHeaderValue("y start");
        buildLatticeTable.getColumnModel().getColumn(5).setHeaderValue("y freq");
        buildLatticeTable.getColumnModel().getColumn(6).setHeaderValue("Dir");
        buildLatticeTable.getColumnModel().getColumn(7).setHeaderValue("Weight");

        rowSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(50), Integer.valueOf(1), null, Integer.valueOf(1)));
        rowSpinner.setToolTipText("Number of rows of the lattice.");

        colSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(50), Integer.valueOf(1), null, Integer.valueOf(1)));
        colSpinner.setToolTipText("Number of columns in the lattice.");

        jLabel22.setText("Rows:");

        jLabel23.setText("Cols:");

        latticeSpacingSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(1), null, Integer.valueOf(1)));
        latticeSpacingSpinner.setToolTipText("Distance between each row/column in the lattice.");

        jLabel24.setText("Spacing:");

        org.jdesktop.layout.GroupLayout buildLaticePanelLayout = new org.jdesktop.layout.GroupLayout(buildLaticePanel);
        buildLaticePanel.setLayout(buildLaticePanelLayout);
        buildLaticePanelLayout.setHorizontalGroup(
            buildLaticePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(buildLaticePanelLayout.createSequentialGroup()
                .add(buildLaticePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel22)
                    .add(jLabel23))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(buildLaticePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(colSpinner)
                    .add(rowSpinner, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 49, Short.MAX_VALUE)
                .add(jLabel24)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(latticeSpacingSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 61, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
            .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 302, Short.MAX_VALUE)
        );
        buildLaticePanelLayout.setVerticalGroup(
            buildLaticePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(buildLaticePanelLayout.createSequentialGroup()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 594, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(buildLaticePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel22)
                    .add(rowSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(latticeSpacingSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel24))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(buildLaticePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel23)
                    .add(colSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        optionsTabbedPane.addTab(BUILD_LATTICE_STATE, buildLaticePanel);

        repaintCheckBox.setSelected(true);
        repaintCheckBox.setText("Repaint"); // NOI18N
        repaintCheckBox.setToolTipText("Toggles repainting in 2d mode. If delay is 0, this will speed up updates.");
        repaintCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                repaintCheckBoxActionPerformed(evt);
            }
        });

        edgeLabelsCheckBox.setText("Draw Edge Labels"); // NOI18N
        edgeLabelsCheckBox.setToolTipText("Toggles the display of edge degrees.");
        edgeLabelsCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                edgeLabelsCheckBoxActionPerformed(evt);
            }
        });

        changingNodeSizeCheckBox.setSelected(true);
        changingNodeSizeCheckBox.setText("Changing Node Size"); // NOI18N
        changingNodeSizeCheckBox.setToolTipText("With this on, the size of each vertex is dependent on number of grains/degree.");
        changingNodeSizeCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changingNodeSizeCheckBoxActionPerformed(evt);
            }
        });

        drawEdgesCheckBox.setSelected(true);
        drawEdgesCheckBox.setText("Draw Edges"); // NOI18N
        drawEdgesCheckBox.setToolTipText("Toggles the display of edges.");
        drawEdgesCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                drawEdgesCheckBoxActionPerformed(evt);
            }
        });

        printFPSCheckBox.setText("Print FPS");
        printFPSCheckBox.setToolTipText("Prints the FPS to standard error each update.");
        printFPSCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                printFPSCheckBoxActionPerformed(evt);
            }
        });

        jLabel18.setText("Color Mode: ");

        colorModeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Number of grains", "Stability", "Total firings", "Difference from Base" }));
        colorModeComboBox.setToolTipText("Changes what each vertex is colored and labelled based on.");
        colorModeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                colorModeComboBoxActionPerformed(evt);
            }
        });

        repaintOptionsButtonGroup.add(repaintDelayRadioButton);
        repaintDelayRadioButton.setSelected(true);
        repaintDelayRadioButton.setText("Repaint Every:");
        repaintDelayRadioButton.setToolTipText("Ensures that a certain amount of time passes between each repaint. Setting this higher increases the speed of updates.");
        repaintDelayRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                repaintDelayRadioButtonActionPerformed(evt);
            }
        });

        repaintOptionsButtonGroup.add(repaintOnUpdateRadioButton);
        repaintOnUpdateRadioButton.setText("Repaint On Update");
        repaintOnUpdateRadioButton.setToolTipText("A repaint will occur after each update.");
        repaintOnUpdateRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                repaintOnUpdateRadioButtonActionPerformed(evt);
            }
        });

        repaintDelayTextField.setText("30");
        repaintDelayTextField.setToolTipText("Minimum time between each repaint.");
        repaintDelayTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                repaintDelayTextFieldActionPerformed(evt);
            }
        });

        vertexLabelsCheckBox.setText("Draw Vertex Labels");
        vertexLabelsCheckBox.setToolTipText("Toggles the display of the number of grains/times fired of each vertex.");
        vertexLabelsCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                vertexLabelsCheckBoxActionPerformed(evt);
            }
        });

        dimensionToggleButton.setText("3d");
        dimensionToggleButton.setToolTipText("Toggles between 2d and 3d. Note that this may not work for large graphs (bigger than 20,000 vertices). 3d mode does not display edges.");
        dimensionToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dimensionToggleButtonActionPerformed(evt);
            }
        });

        heightSmoothingSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(3), null, null, Integer.valueOf(1)));
        heightSmoothingSpinner.setToolTipText("Averages the height of each vertex with the height of its neighbors the given number of times.");
        heightSmoothingSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                heightSmoothingSpinnerStateChanged(evt);
            }
        });

        jLabel19.setText("Height Smoothing:");

        jLabel20.setText("Color Smoothing:");

        jLabel21.setText("Height Scalar:");

        colorSmoothingSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(3), null, null, Integer.valueOf(1)));
        colorSmoothingSpinner.setToolTipText("Averages the color of each vertex with the height of its neighbors the given number of times.");
        colorSmoothingSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                colorSmoothingSpinnerStateChanged(evt);
            }
        });

        heightScalarSpinner.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(3.0f), null, null, Float.valueOf(0.5f)));
        heightScalarSpinner.setToolTipText("Multiplies the height of each vertex by the given number. Can be negative.");
        heightScalarSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                heightScalarSpinnerStateChanged(evt);
            }
        });

        drawShapeCheckBox.setSelected(true);
        drawShapeCheckBox.setText("Draw Shape");
        drawShapeCheckBox.setToolTipText("Display the colored model of the graph.");
        drawShapeCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                drawShapeCheckBoxActionPerformed(evt);
            }
        });

        drawWireCheckBox.setText("Draw Wire");
        drawWireCheckBox.setToolTipText("Display the outlines of the triangles that make up the shape. Shows how exactly the graph was triangulated and looks badass.");
        drawWireCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                drawWireCheckBoxActionPerformed(evt);
            }
        });

        jLabel13.setText("ms");

        jButton1.setText("Set Base Config");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout visualOptionsPanelLayout = new org.jdesktop.layout.GroupLayout(visualOptionsPanel);
        visualOptionsPanel.setLayout(visualOptionsPanelLayout);
        visualOptionsPanelLayout.setHorizontalGroup(
            visualOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(visualOptionsPanelLayout.createSequentialGroup()
                .add(visualOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(repaintCheckBox)
                    .add(edgeLabelsCheckBox)
                    .add(vertexLabelsCheckBox)
                    .add(changingNodeSizeCheckBox)
                    .add(drawEdgesCheckBox)
                    .add(printFPSCheckBox)
                    .add(colorModeComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel18)
                    .add(visualOptionsPanelLayout.createSequentialGroup()
                        .add(repaintDelayRadioButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(repaintDelayTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 47, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel13))
                    .add(repaintOnUpdateRadioButton)
                    .add(visualOptionsPanelLayout.createSequentialGroup()
                        .add(visualOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(dimensionToggleButton)
                            .add(jLabel19)
                            .add(jLabel20)
                            .add(jLabel21))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(visualOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                            .add(heightSmoothingSpinner, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 62, Short.MAX_VALUE)
                            .add(colorSmoothingSpinner)
                            .add(heightScalarSpinner)))
                    .add(drawShapeCheckBox)
                    .add(drawWireCheckBox)
                    .add(jButton1))
                .addContainerGap(99, Short.MAX_VALUE))
        );
        visualOptionsPanelLayout.setVerticalGroup(
            visualOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(visualOptionsPanelLayout.createSequentialGroup()
                .add(repaintCheckBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(edgeLabelsCheckBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(vertexLabelsCheckBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(changingNodeSizeCheckBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(drawEdgesCheckBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(printFPSCheckBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(visualOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(repaintDelayRadioButton)
                    .add(repaintDelayTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel13))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(repaintOnUpdateRadioButton)
                .add(25, 25, 25)
                .add(jLabel18)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(colorModeComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jButton1)
                .add(6, 6, 6)
                .add(dimensionToggleButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(visualOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel19)
                    .add(heightSmoothingSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(visualOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel20)
                    .add(colorSmoothingSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(visualOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel21)
                    .add(heightScalarSpinner, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(drawShapeCheckBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(drawWireCheckBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 23, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(165, Short.MAX_VALUE))
        );

        optionsTabbedPane.addTab(VISUAL_OPTIONS_STATE, visualOptionsPanel);

        org.jdesktop.layout.GroupLayout controlPanelLayout = new org.jdesktop.layout.GroupLayout(controlPanel);
        controlPanel.setLayout(controlPanelLayout);
        controlPanelLayout.setHorizontalGroup(
            controlPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(controlPanelLayout.createSequentialGroup()
                .add(17, 17, 17)
                .add(quitButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 331, Short.MAX_VALUE))
            .add(optionsTabbedPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 348, Short.MAX_VALUE)
        );
        controlPanelLayout.setVerticalGroup(
            controlPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, controlPanelLayout.createSequentialGroup()
                .add(optionsTabbedPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 688, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(quitButton)
                .addContainerGap())
        );

        jSplitPane1.setLeftComponent(controlPanel);

        canvasHolderPanel.setBackground(new java.awt.Color(51, 51, 51));
        canvasHolderPanel.setLayout(new java.awt.CardLayout());
        canvasHolderPanel.add(canvas3d,"3d");
        canvasHolderPanel.add(canvas,"2d");
        CardLayout cl = (CardLayout) canvasHolderPanel.getLayout();
        cl.show(canvasHolderPanel, "2d");

        infoToolBar.setFloatable(false);
        infoToolBar.setRollover(true);

        centerLabel.setText("Center: ");
        centerLabel.setToolTipText("");
        infoToolBar.add(centerLabel);

        centerCoordLabel.setText("0.0, 0.0");
        infoToolBar.add(centerCoordLabel);
        infoToolBar.add(jSeparator4);

        currentActionLabel.setText("None: ");
        currentActionLabel.setToolTipText("Displays operations that may take a long time.");
        currentActionLabel.setEnabled(false);
        infoToolBar.add(currentActionLabel);

        cancelButton.setText("Cancel");
        cancelButton.setToolTipText("Cancel the current operation.");
        cancelButton.setEnabled(false);
        cancelButton.setFocusable(false);
        cancelButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        cancelButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        infoToolBar.add(cancelButton);
        infoToolBar.add(jSeparator6);

        selecteCenterButton.setText("Sel. Center: -, -; ");
        selecteCenterButton.setFocusable(false);
        selecteCenterButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        selecteCenterButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        selecteCenterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selecteCenterButtonActionPerformed(evt);
            }
        });
        infoToolBar.add(selecteCenterButton);

        selectedSandLabel.setText("Sand: -; ");
        infoToolBar.add(selectedSandLabel);

        selectedDegreeLabel.setText("Degree: -; ");
        infoToolBar.add(selectedDegreeLabel);

        selectedFiringsLabel.setText("Firings: -; ");
        infoToolBar.add(selectedFiringsLabel);

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(canvasHolderPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 664, Short.MAX_VALUE)
            .add(infoToolBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 664, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                .add(canvasHolderPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 709, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(infoToolBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        jSplitPane1.setRightComponent(jPanel1);

        controlToolBar.setFloatable(false);
        controlToolBar.setRollover(true);
        controlToolBar.setMinimumSize(new java.awt.Dimension(0, 32));

        runButton.setText("Run"); // NOI18N
        runButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runButtonActionPerformed(evt);
            }
        });
        controlToolBar.add(runButton);

        stepButton.setText("Step"); // NOI18N
        stepButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stepButtonActionPerformed(evt);
            }
        });
        stepButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                stepButtonMouseClicked(evt);
            }
        });
        controlToolBar.add(stepButton);

        stabilizeButton.setText("Stabilize");
        stabilizeButton.setFocusable(false);
        stabilizeButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        stabilizeButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        stabilizeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stabilizeButtonActionPerformed(evt);
            }
        });
        controlToolBar.add(stabilizeButton);
        controlToolBar.add(jSeparator3);

        clearSandButton.setText("Clear Sand"); // NOI18N
        clearSandButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearSandButtonActionPerformed(evt);
            }
        });
        controlToolBar.add(clearSandButton);

        deleteGraphButton.setText("Del. Graph"); // NOI18N
        deleteGraphButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteGraphButtonActionPerformed(evt);
            }
        });
        controlToolBar.add(deleteGraphButton);

        resetFiringsButton.setText("Reset Firings Count");
        resetFiringsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetFiringsButtonActionPerformed(evt);
            }
        });
        controlToolBar.add(resetFiringsButton);
        controlToolBar.add(jSeparator1);

        delayLabel.setText("Delay:"); // NOI18N
        controlToolBar.add(delayLabel);

        delayTextField.setToolTipText("The minimum amount of time between updates.");
        delayTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                delayTextFieldActionPerformed(evt);
            }
        });
        delayTextField.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                delayTextFieldCaretUpdate(evt);
            }
        });
        controlToolBar.add(delayTextField);

        jLabel12.setText("ms "); // NOI18N
        controlToolBar.add(jLabel12);

        bigDecDelayButton.setText("--");
        bigDecDelayButton.setToolTipText("Decrease delay by 25 ms.");
        bigDecDelayButton.setFocusable(false);
        bigDecDelayButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bigDecDelayButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bigDecDelayButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bigDecDelayButtonActionPerformed(evt);
            }
        });
        controlToolBar.add(bigDecDelayButton);

        smallDevDelayButton.setText("-");
        smallDevDelayButton.setToolTipText("Decrease delay by 5 ms.");
        smallDevDelayButton.setFocusable(false);
        smallDevDelayButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        smallDevDelayButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        smallDevDelayButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                smallDevDelayButtonActionPerformed(evt);
            }
        });
        controlToolBar.add(smallDevDelayButton);

        smallIncDelayButton.setText("+");
        smallIncDelayButton.setToolTipText("Increase delay by 5 ms.");
        smallIncDelayButton.setFocusable(false);
        smallIncDelayButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        smallIncDelayButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        smallIncDelayButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                smallIncDelayButtonActionPerformed(evt);
            }
        });
        controlToolBar.add(smallIncDelayButton);

        bigIncDelayButton.setText("++");
        bigIncDelayButton.setToolTipText("Increases the delay by 25 ms.");
        bigIncDelayButton.setFocusable(false);
        bigIncDelayButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bigIncDelayButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bigIncDelayButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bigIncDelayButtonActionPerformed(evt);
            }
        });
        controlToolBar.add(bigIncDelayButton);
        controlToolBar.add(jSeparator2);

        jLabel2.setText("Zoom:"); // NOI18N
        controlToolBar.add(jLabel2);

        zoomTextField.setText("100.0");
        zoomTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomTextFieldActionPerformed(evt);
            }
        });
        zoomTextField.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                zoomTextFieldCaretUpdate(evt);
            }
        });
        controlToolBar.add(zoomTextField);

        jLabel11.setText("%  "); // NOI18N
        controlToolBar.add(jLabel11);

        bigZoomOutButton.setText("--");
        bigZoomOutButton.setToolTipText("Decrease the zoom by 25% of its current value.");
        bigZoomOutButton.setFocusable(false);
        bigZoomOutButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bigZoomOutButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bigZoomOutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bigZoomOutButtonActionPerformed(evt);
            }
        });
        controlToolBar.add(bigZoomOutButton);

        smallZoomOutButton.setText("-");
        smallZoomOutButton.setToolTipText("Decrease the zoom by 5% of its current value.");
        smallZoomOutButton.setFocusable(false);
        smallZoomOutButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        smallZoomOutButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        smallZoomOutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                smallZoomOutButtonActionPerformed(evt);
            }
        });
        controlToolBar.add(smallZoomOutButton);

        smallZoomInButton.setText("+");
        smallZoomInButton.setToolTipText("Increase the zoom by 5% of its current value.");
        smallZoomInButton.setFocusable(false);
        smallZoomInButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        smallZoomInButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        smallZoomInButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                smallZoomInButtonActionPerformed(evt);
            }
        });
        controlToolBar.add(smallZoomInButton);

        bigZoomInButton.setText("++");
        bigZoomInButton.setToolTipText("Increase the zoom by 25% of its current value.");
        bigZoomInButton.setFocusable(false);
        bigZoomInButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        bigZoomInButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        bigZoomInButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bigZoomInButtonActionPerformed(evt);
            }
        });
        controlToolBar.add(bigZoomInButton);

        serverToggleButton.setText("Server");
        serverToggleButton.setToolTipText("Listen for a TCP connection. Used to control this program from some other program. For instance, one can write a python script to collect statistics on graphs being created and updated with this program.");
        serverToggleButton.setFocusable(false);
        serverToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        serverToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        serverToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serverToggleButtonActionPerformed(evt);
            }
        });
        controlToolBar.add(serverToggleButton);
        controlToolBar.add(jSeparator5);

        mouseToolBar.setFloatable(false);
        mouseToolBar.setRollover(true);

        mouseButtonGroup.add(selectToggleButton);
        selectToggleButton.setText("Select");
        selectToggleButton.setToolTipText("Clicking on a vertex selects/unselects it. Dragging the mouse creates a selection box. Holding down shift has the same effect (and is usually more convenient).");
        selectToggleButton.setFocusable(false);
        selectToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        selectToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        selectToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectToggleButtonActionPerformed(evt);
            }
        });
        mouseToolBar.add(selectToggleButton);

        mouseButtonGroup.add(editToggleButton);
        editToggleButton.setSelected(true);
        editToggleButton.setText("Edit");
        editToggleButton.setToolTipText("The current selection in the sidepanel determines what happens when you click. Dragging moves you around the canvas. Hold down shift to enter selection mode and let go to re-enter edit mode.");
        editToggleButton.setFocusable(false);
        editToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        editToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        editToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editToggleButtonActionPerformed(evt);
            }
        });
        mouseToolBar.add(editToggleButton);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(controlToolBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 935, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(mouseToolBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(2, 2, 2))
            .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1024, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(controlToolBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 30, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(mouseToolBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 30, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 738, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

	public SandpileController getSandpileController() {
		return this.sandpileController;
	}
	private void runButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runButtonActionPerformed
		//runTimer.setDelay( delaySlider.getValue());
		updateControllerDelay();
		if(runTimer.isRunning()){
			runTimer.stop();
			runButton.setText("Run");
		}else{
			runButton.setText("Pause");
			runTimer.start();
		}
		/*if(spThread.isAlive()) {
			spThread.interrupt();
			try{
				spThread.join();
				spThread = new Thread(sandpileController);
			}catch(InterruptedException e){
				return;
			}
		}else {
			spThread.start();
		}*/
}//GEN-LAST:event_runButtonActionPerformed

	private void delayTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_delayTextFieldActionPerformed
		updateControllerDelay();
}//GEN-LAST:event_delayTextFieldActionPerformed

	private void controlStateComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_controlStateComboBoxItemStateChanged
		String currentState = (String)evt.getItem();
		CardLayout cl = (CardLayout)(optionsContainerPanel.getLayout());
		//cl.show(optionsContainerPanel, (String)evt.getItem());
		if(currentState.equals(MAKE_GRID_STATE) || currentState.equals(MAKE_HEX_GRID_STATE)){
			cl.show(optionsContainerPanel, MAKE_GRID_STATE);
		}else if(currentState.equals(MAKE_HONEYCOMB_STATE)){
			cl.show(optionsContainerPanel,MAKE_HONEYCOMB_STATE);
		}else if(currentState.equals(CONFIG_MANAGER_STATE)){
			cl.show(optionsContainerPanel, CONFIG_MANAGER_STATE);
		}else if(currentState.equals(VISUAL_OPTIONS_STATE)){
			cl.show(optionsContainerPanel, VISUAL_OPTIONS_STATE);
		}else if(currentState.equals(EDIT_GRAPH_STATE)){
			cl.show(optionsContainerPanel, EDIT_GRAPH_STATE);
		}
}//GEN-LAST:event_controlStateComboBoxItemStateChanged

	private void controlStateComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_controlStateComboBoxActionPerformed
		//sandpileController.setControlState(controlStateComboBox.getSelectedIndex());
}//GEN-LAST:event_controlStateComboBoxActionPerformed

	private void clearSandButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearSandButtonActionPerformed
		sandpileController.clearSand();
}//GEN-LAST:event_clearSandButtonActionPerformed

	private void quitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quitButtonActionPerformed


		System.exit(0);
}//GEN-LAST:event_quitButtonActionPerformed

	private void stepButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stepButtonActionPerformed
		sandpileController.update();
		sandpileController.repaint();
}//GEN-LAST:event_stepButtonActionPerformed

	private void stepButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_stepButtonMouseClicked

}//GEN-LAST:event_stepButtonMouseClicked

	private void deleteGraphButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteGraphButtonActionPerformed
		if(runningThread)
			return;
		sandpileController.delAllVerticesControl();
		try{
			drawer3d.triangulate(sandpileController.vertexData);
		}catch(InterruptedException e){
			
		}
		this.updateConfigSelectList();
}//GEN-LAST:event_deleteGraphButtonActionPerformed

	private void makeHoneycombBorderComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_makeHoneycombBorderComboBoxActionPerformed
		// TODO add your handling code here:
}//GEN-LAST:event_makeHoneycombBorderComboBoxActionPerformed

	private void makeHoneycombRadiusFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_makeHoneycombRadiusFieldActionPerformed
		// TODO add your handling code here:
}//GEN-LAST:event_makeHoneycombRadiusFieldActionPerformed

	private void gridRowsFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gridRowsFieldActionPerformed
		//sandpileController.setGridRows( Integer.parseInt(gridRowsField.getText() ) );
}//GEN-LAST:event_gridRowsFieldActionPerformed

	private void gridRowsFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_gridRowsFieldPropertyChange
		// TODO add your handling code here:
}//GEN-LAST:event_gridRowsFieldPropertyChange

	private void gridColsFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gridColsFieldActionPerformed
		//sandpileController.setGridCols( Integer.parseInt( gridColsField.getText() ) );
}//GEN-LAST:event_gridColsFieldActionPerformed

	private void nBorderComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nBorderComboBoxActionPerformed
		// TODO add your handling code here:
}//GEN-LAST:event_nBorderComboBoxActionPerformed

	private void sBorderComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sBorderComboBoxActionPerformed
		// TODO add your handling code here:
}//GEN-LAST:event_sBorderComboBoxActionPerformed

	private void eBorderComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_eBorderComboBoxActionPerformed
		// TODO add your handling code here:
}//GEN-LAST:event_eBorderComboBoxActionPerformed

	private void wBorderComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wBorderComboBoxActionPerformed
		// TODO add your handling code here:
}//GEN-LAST:event_wBorderComboBoxActionPerformed

	private void addConfigButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addConfigButtonActionPerformed
		if(runningThread)
			return;
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		String selection = (String)configSelectList.getSelectedValue();
		final int times = (Integer)configTimesSpinner.getValue();
		if (selection != null) {
			if (selection.equals(MAX_CONFIG)) {
				sandpileController.addMaxStableConfig(times);
			} else if (selection.equals(CURRENT_CONFIG)) {
				sandpileController.addCurrentConfig(times);
			} else if (selection.equals(DUAL_CONFIG)) {
				sandpileController.addDualConfig(times);
			} else if (selection.equals(ONES_CONFIG)) {
				sandpileController.addSandEverywhere(times);
			} else if (selection.equals(IDENTITY_CONFIG)) {
				calculationThread = new Thread(){
					@Override public void run(){
						if(!calculationThreadInit("Calculating identity"))
							return;
						try{
							sandpileController.addIdentity(times);
						}catch(InterruptedException e){

						}
						calculationThreadEnd();
					}
				};
				calculationThread.start();
			} else if (selection.equals(BURNING_CONFIG)) {
				calculationThread = new Thread(){
					@Override public void run(){
						if(!calculationThreadInit("Calculating burning"))
							return;
						try{
							sandpileController.addBurningConfig(times);
						}catch(InterruptedException e){

						}
						calculationThreadEnd();
					}
				};
				calculationThread.start();
			} else if (selection.equals(EQUIVALENT_RECURRENT)) {
				calculationThread = new Thread(){
					@Override public void run(){
						if(!calculationThreadInit("Calculating equivalent recurrent"))
							return;
						try{
							sandpileController.addEquivalentRecurrent(times);
						}catch(InterruptedException e){

						}
						calculationThreadEnd();
					}
				};
				calculationThread.start();
			} else if (selection.equals(INVERSE_CONFIG)) {
				calculationThread = new Thread(){
					@Override public void run(){
						if(!calculationThreadInit("Calculating inverse"))
							return;
						try{
							sandpileController.addInverseConfig(times);
						}catch(InterruptedException e){

						}
						calculationThreadEnd();
					}
				};
				calculationThread.start();
			} else if (selection.equals(RANDOM_CONFIG)) {
				sandpileController.addRandomConfig(times);
			} else {
				sandpileController.addConfigNamed(selection, times);
			}
		}
		this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_addConfigButtonActionPerformed

	private void setConfigButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setConfigButtonActionPerformed
		if(runningThread)
			return;
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		String selection = (String)configSelectList.getSelectedValue();
		final int times = (Integer)configTimesSpinner.getValue();
		if(selection!=null){
			if (selection.equals(MAX_CONFIG)) {
				sandpileController.setToMaxStableConfig(times);
			} else if (selection.equals(DUAL_CONFIG)) {
				sandpileController.setToDualConfig(times);
			} else if (selection.equals(ONES_CONFIG)) {
				sandpileController.setSandEverywhere(times);
			}  else if (selection.equals(IDENTITY_CONFIG)) {
				calculationThread = new Thread(){
					@Override public void run(){
						if(!calculationThreadInit("Calculating identity"))
							return;
						try{
							sandpileController.setToIdentity(times);
						}catch(InterruptedException e){

						}
						calculationThreadEnd();
					}
				};
				calculationThread.start();
			} else if (selection.equals(BURNING_CONFIG)) {
				calculationThread = new Thread(){
					@Override public void run(){
						if(!calculationThreadInit("Calculating burning"))
							return;
						try{
							sandpileController.setToBurningConfig(times);
						}catch(InterruptedException e){

						}
						calculationThreadEnd();
					}
				};
				calculationThread.start();
			} else if (selection.equals(EQUIVALENT_RECURRENT)) {
				calculationThread = new Thread(){
					@Override public void run(){
						if(!calculationThreadInit("Calculating equivalent recurrent"))
							return;
						try{
							sandpileController.setToEquivalentRecurrent(times);
						}catch(InterruptedException e){

						}
						calculationThreadEnd();
					}
				};
				calculationThread.start();
			} else if (selection.equals(INVERSE_CONFIG)) {
				calculationThread = new Thread(){
					@Override public void run(){
						if(!calculationThreadInit("Calculating inverse"))
							return;
						try{
							sandpileController.setToInverseConfig(times);
						}catch(InterruptedException e){

						}
						calculationThreadEnd();
					}
				};
				calculationThread.start();
			} else if (selection.equals(CURRENT_CONFIG)) {
				sandpileController.setToCurrentConfig(times);
			} else if (selection.equals(RANDOM_CONFIG)) {
				sandpileController.setToRandomConfig(times);
			} else {
				sandpileController.setConfigNamed(selection,times);
			}
		}
		this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_setConfigButtonActionPerformed

	private void repaintCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_repaintCheckBoxActionPerformed
		drawer.repaint = repaintCheckBox.isSelected();
}//GEN-LAST:event_repaintCheckBoxActionPerformed

	private void edgeLabelsCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_edgeLabelsCheckBoxActionPerformed
		drawer.drawEdgeLabels=edgeLabelsCheckBox.isSelected();
		sandpileController.repaint();
}//GEN-LAST:event_edgeLabelsCheckBoxActionPerformed

	private void changingNodeSizeCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changingNodeSizeCheckBoxActionPerformed
		drawer.changingVertexSize = changingNodeSizeCheckBox.isSelected();
		sandpileController.repaint();
}//GEN-LAST:event_changingNodeSizeCheckBoxActionPerformed

	private void drawEdgesCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drawEdgesCheckBoxActionPerformed
		drawer.drawEdges=drawEdgesCheckBox.isSelected();
		sandpileController.repaint();
}//GEN-LAST:event_drawEdgesCheckBoxActionPerformed

	private void sandpileViewPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sandpileViewPanelMouseClicked

}//GEN-LAST:event_sandpileViewPanelMouseClicked

	private void sandpileViewPanelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sandpileViewPanelMouseReleased

}//GEN-LAST:event_sandpileViewPanelMouseReleased

	private void printFPSCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_printFPSCheckBoxActionPerformed
		drawer.printFPS = printFPSCheckBox.isSelected();
	}//GEN-LAST:event_printFPSCheckBoxActionPerformed

	private void canvasMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_canvasMouseClicked
		if(runningThread)
			return;
		if(getMouseMode(evt)!=MouseMode.EDIT || evt.getButton()!=evt.BUTTON1) return;
		setWaitCursor();
		String currentState = optionsTabbedPane.getTitleAt(optionsTabbedPane.getSelectedIndex());
		float[] coords = drawer.transformCanvasCoords(evt.getX(), evt.getY());
		float x = coords[0];
		float y = coords[1];
		try{
			if (currentState.equals(MAKE_GRID_STATE)) {
				sandpileController.makeGridControl(Integer.valueOf(gridRowsField.getText()), Integer.valueOf(gridColsField.getText()), x, y,
						nBorderComboBox.getSelectedIndex(),
						sBorderComboBox.getSelectedIndex(),
						eBorderComboBox.getSelectedIndex(),
						wBorderComboBox.getSelectedIndex());
			} else if (currentState.equals(MAKE_HEX_GRID_STATE)) {
				sandpileController.makeHexGridControl(Integer.valueOf(hexGridRowsField.getText()), Integer.valueOf(hexGridColsField.getText()), x, y,
						hexNBorderComboBox.getSelectedIndex(),
						hexSBorderComboBox.getSelectedIndex(),
						hexEBorderComboBox.getSelectedIndex(),
						hexWBorderComboBox.getSelectedIndex());
			} else if (currentState.equals(EDIT_GRAPH_STATE)) {
				if (editGraphButtonGroup.isSelected(addVertexRadioButton.getModel())) {
					sandpileController.addVertexControl(x, y);
				} else if (editGraphButtonGroup.isSelected(removeVertexRadioButton.getModel())) {
					sandpileController.delVertexControl(x, y);
				} else if (editGraphButtonGroup.isSelected(addEdgeRadioButton.getModel())) {
					sandpileController.addEdgeControl(x, y, Integer.valueOf(edgeWeightField.getText()));
				} else if (editGraphButtonGroup.isSelected(removeEdgeRadioButton.getModel())) {
					sandpileController.delEdgeControl(x, y, Integer.valueOf(edgeWeightField.getText()));
				} else if (editGraphButtonGroup.isSelected(addUndirectedEdgeRadioButton.getModel())) {
					sandpileController.addUndiEdgeControl(x, y, Integer.valueOf(edgeWeightField.getText()));
				} else if (editGraphButtonGroup.isSelected(removeUndirectedEdgeRadioButton.getModel())) {
					sandpileController.delUndiEdgeControl(x, y, Integer.valueOf(edgeWeightField.getText()));
				}

			} else if (currentState.equals(MAKE_HONEYCOMB_STATE)) {
				sandpileController.makeHoneycombControl(Integer.valueOf(makeHoneycombRadiusField.getText()), x, y, makeHoneycombBorderComboBox.getSelectedIndex());
			} else if (currentState.equals(CONFIG_MANAGER_STATE)) {
				if (editConfigButtonGroup.isSelected(addSandRadioButton.getModel())) {
					sandpileController.addSandControl(x, y, (Integer)amountOfSandSpinner.getValue());
				} else if (editConfigButtonGroup.isSelected(removeSandRadioButton.getModel())) {
					sandpileController.addSandControl(x, y, -(Integer)amountOfSandSpinner.getValue());
				} else if (editConfigButtonGroup.isSelected(setSandRadioButton.getModel())) {
					sandpileController.setSandControl(x, y, (Integer)amountOfSandSpinner.getValue());
				}
			} else if (currentState.equals(BUILD_LATTICE_STATE)) {
				ArrayList<int[]> vectors = new ArrayList<int[]>();
				TIntArrayList xStarts = new TIntArrayList();
				TIntArrayList xFreqs = new TIntArrayList();
				TIntArrayList yStarts = new TIntArrayList();
				TIntArrayList yFreqs = new TIntArrayList();
				ArrayList<Boolean> dirs = new ArrayList<Boolean>();
				TIntArrayList weights = new TIntArrayList();
				TIntArrayList borders = new TIntArrayList();

				for (int r = 0; r < buildLatticeTable.getRowCount(); r++) {
					Integer xCoord = (Integer) buildLatticeTable.getValueAt(r, 0);
					Integer yCoord = (Integer) buildLatticeTable.getValueAt(r, 1);
					Integer xStart = (Integer) buildLatticeTable.getValueAt(r, 2);
					Integer xFreq = (Integer) buildLatticeTable.getValueAt(r, 3);
					Integer yStart = (Integer) buildLatticeTable.getValueAt(r, 4);
					Integer yFreq = (Integer) buildLatticeTable.getValueAt(r, 5);
					Boolean dir = (Boolean) buildLatticeTable.getValueAt(r, 6);
					Integer weight = (Integer) buildLatticeTable.getValueAt(r, 7);
					if (xCoord != null && yCoord != null) {
						int[] vec = {xCoord, yCoord};
						vectors.add(vec);
						if (xStart == null) {
							xStarts.add(0);
						} else {
							xStarts.add(xStart);
						}
						if (xFreq == null) {
							xFreqs.add(1);
						} else {
							xFreqs.add(xFreq);
						}
						if (yStart == null) {
							yStarts.add(0);
						} else {
							yStarts.add(yStart);
						}
						if (yFreq == null) {
							yFreqs.add(1);
						} else {
							yFreqs.add(yFreq);
						}
						if (dir == null) {
							dirs.add(false);
						} else {
							dirs.add(dir);
						}
						if (weight == null) {
							weights.add(1);
						} else {
							weights.add(weight);
						}
						borders.add(SandpileController.NO_BORDER);
					}
				}
				if (!vectors.isEmpty()) {
					int rows = (Integer) rowSpinner.getValue();
					int cols = (Integer) colSpinner.getValue();
					int spacing = (Integer) latticeSpacingSpinner.getValue();
					float newY = y-(rows-1)*spacing*sandpileController.VERT_RADIUS*2f;
					sandpileController.buildLatticeControl(x, newY, rows, cols,
							spacing, vectors,
							xStarts, xFreqs, yStarts, yFreqs, dirs, weights, borders);
				}
			}
		} catch (NumberFormatException e) {
		} catch (NullPointerException e) {
		}
		this.updateConfigSelectList();
		setDefaultCursor();
	}//GEN-LAST:event_canvasMouseClicked

	private void canvasMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_canvasMouseReleased
		// TODO add your handling code here:
	}//GEN-LAST:event_canvasMouseReleased

	private void hexGridRowsFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hexGridRowsFieldActionPerformed
		// TODO add your handling code here:
}//GEN-LAST:event_hexGridRowsFieldActionPerformed

	private void hexGridRowsFieldPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_hexGridRowsFieldPropertyChange
		// TODO add your handling code here:
}//GEN-LAST:event_hexGridRowsFieldPropertyChange

	private void hexGridColsFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hexGridColsFieldActionPerformed
		// TODO add your handling code here:
}//GEN-LAST:event_hexGridColsFieldActionPerformed

	private void hexNBorderComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hexNBorderComboBoxActionPerformed
		// TODO add your handling code here:
}//GEN-LAST:event_hexNBorderComboBoxActionPerformed

	private void hexSBorderComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hexSBorderComboBoxActionPerformed
		// TODO add your handling code here:
}//GEN-LAST:event_hexSBorderComboBoxActionPerformed

	private void hexEBorderComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hexEBorderComboBoxActionPerformed
		// TODO add your handling code here:
}//GEN-LAST:event_hexEBorderComboBoxActionPerformed

	private void hexWBorderComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hexWBorderComboBoxActionPerformed
		// TODO add your handling code here:
}//GEN-LAST:event_hexWBorderComboBoxActionPerformed

	private void colorModeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_colorModeComboBoxActionPerformed
		
		switch(colorModeComboBox.getSelectedIndex()){
			case 0: 
				drawer.setColorMode(SandpileDrawer.ColorMode.NUM_OF_GRAINS);
				drawer3d.setColorMode(SandpileDrawer.ColorMode.NUM_OF_GRAINS);
				break;
			case 1:
				drawer.setColorMode(SandpileDrawer.ColorMode.STABILITY);
				drawer3d.setColorMode(SandpileDrawer.ColorMode.STABILITY);
				break;
			case 2:
				drawer.setColorMode(SandpileDrawer.ColorMode.FIRINGS);
				drawer3d.setColorMode(SandpileDrawer.ColorMode.FIRINGS);
				break;
			case 3:
				drawer.setColorMode(SandpileDrawer.ColorMode.DIFFERENCE);
				drawer3d.setColorMode(SandpileDrawer.ColorMode.DIFFERENCE);
				break;
		}
		sandpileController.repaint();
}//GEN-LAST:event_colorModeComboBoxActionPerformed

	private void resetFiringsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetFiringsButtonActionPerformed
		sandpileController.resetFirings();
		sandpileController.repaint();
	}//GEN-LAST:event_resetFiringsButtonActionPerformed

	private void stabilizeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stabilizeButtonActionPerformed
		if(runningThread)
			return;
		calculationThread = new Thread() {

			@Override
			public void run() {
				calculationThreadInit("Stabilizing");
				try {
					sandpileController.stabilize();
				} catch (InterruptedException e) {
				}
				calculationThreadEnd();
			}
		};
		calculationThread.start();
		sandpileController.repaint();
	}//GEN-LAST:event_stabilizeButtonActionPerformed

	private void smallIncDelayButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_smallIncDelayButtonActionPerformed
		sandpileController.setMinUpdateDelay(sandpileController.getMinUpdateDelay()+5);
		updateDelayTextField();
	}//GEN-LAST:event_smallIncDelayButtonActionPerformed

	private void bigIncDelayButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bigIncDelayButtonActionPerformed
		sandpileController.setMinUpdateDelay(sandpileController.getMinUpdateDelay()+25);
		updateDelayTextField();
	}//GEN-LAST:event_bigIncDelayButtonActionPerformed

	private void smallDevDelayButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_smallDevDelayButtonActionPerformed
		sandpileController.setMinUpdateDelay(sandpileController.getMinUpdateDelay()-5);
		updateDelayTextField();
	}//GEN-LAST:event_smallDevDelayButtonActionPerformed

	private void bigDecDelayButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bigDecDelayButtonActionPerformed
		sandpileController.setMinUpdateDelay(sandpileController.getMinUpdateDelay()-25);
		updateDelayTextField();
	}//GEN-LAST:event_bigDecDelayButtonActionPerformed

	private void delayTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_delayTextFieldCaretUpdate
		//updateControllerDelay();
	}//GEN-LAST:event_delayTextFieldCaretUpdate

	private void bigZoomInButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bigZoomInButtonActionPerformed
		float amount = currentDrawer.getZoom()*1.25f;
		drawer.setZoom(amount);
		drawer3d.setZoom(amount);
		updateZoomTextField();
		sandpileController.repaint();
	}//GEN-LAST:event_bigZoomInButtonActionPerformed

	private void smallZoomInButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_smallZoomInButtonActionPerformed
		float amount = currentDrawer.getZoom()*1.05f;
		drawer.setZoom(amount);
		drawer3d.setZoom(amount);
		updateZoomTextField();
		sandpileController.repaint();
	}//GEN-LAST:event_smallZoomInButtonActionPerformed

	private void smallZoomOutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_smallZoomOutButtonActionPerformed
		float amount = currentDrawer.getZoom()*0.95f;
		drawer.setZoom(amount);
		drawer3d.setZoom(amount);
		updateZoomTextField();
		sandpileController.repaint();
	}//GEN-LAST:event_smallZoomOutButtonActionPerformed

	private void bigZoomOutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bigZoomOutButtonActionPerformed
		float amount = currentDrawer.getZoom()*0.75f;
		drawer.setZoom(amount);
		drawer3d.setZoom(amount);
		updateZoomTextField();
		sandpileController.repaint();
	}//GEN-LAST:event_bigZoomOutButtonActionPerformed

	private void zoomTextFieldCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_zoomTextFieldCaretUpdate
		//updateDrawerZoom();
	}//GEN-LAST:event_zoomTextFieldCaretUpdate

	private void zoomTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomTextFieldActionPerformed
		updateDrawerZoom();
	}//GEN-LAST:event_zoomTextFieldActionPerformed

	private void storeConfigButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_storeConfigButtonActionPerformed
		String name = javax.swing.JOptionPane.showInputDialog("Enter a name for the configuration:");
		if(name!=null){
			sandpileController.storeCurrentConfig(name);
			updateConfigSelectList();
		}
	}//GEN-LAST:event_storeConfigButtonActionPerformed

	private void selectToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectToggleButtonActionPerformed

	}//GEN-LAST:event_selectToggleButtonActionPerformed

	private void editToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editToggleButtonActionPerformed

	}//GEN-LAST:event_editToggleButtonActionPerformed

	private MouseMode getSelectedMouseMode(){
		if(selectToggleButton.isSelected())
			return MouseMode.SELECT;
		else if(editToggleButton.isSelected())
			return MouseMode.EDIT;
		return MouseMode.MOVE;
	}

	public MouseMode getMouseMode(MouseEvent evt){
		MouseMode mm;
		if(evt.isShiftDown())
			mm=MouseMode.SELECT;
		else
			mm=getSelectedMouseMode();
		drawer.scrollOnDrag = mm.scrollOnDrag;
		return mm;
	}

	private void deleteSelectedVerticesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteSelectedVerticesButtonActionPerformed
		if(runningThread)
			return;
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		TIntArrayList verts = sandpileController.getSelectedVertices();
		sandpileController.delVerticesControl(verts);
		sandpileController.unselectVertices();
		sandpileController.repaint();
		this.setCursor(Cursor.getDefaultCursor());
	}//GEN-LAST:event_deleteSelectedVerticesButtonActionPerformed

	private void serverToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverToggleButtonActionPerformed
		if(serverToggleButton.isSelected()){
			try{
				sandpileController.startServer(7236);
				int answer = JOptionPane.showConfirmDialog(this, "Server created on port 7236. Sandpiles will now wait for a client to connect.\nThe program will not respond until a client connects.\nWould you like to continue?");
				if(answer == JOptionPane.YES_OPTION){
					sandpileController.acceptClient();
					JOptionPane.showMessageDialog(this, "Client accepted! Celebration!");
					serverMsgChecker.start();
				}else{
					serverToggleButton.setSelected(false);
					try{
						sandpileController.stopServer();
					}catch(IOException e){
						JOptionPane.showMessageDialog(this, "Error stopping server on port 7236: "+e.getMessage(), "Socket Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}catch(IOException e){
				JOptionPane.showMessageDialog(this, "Could not start server on port 7236: "+e.getMessage(), "Socket Error", JOptionPane.ERROR_MESSAGE);
				serverToggleButton.setSelected(false);
			}
		}else{
			try{
				sandpileController.stopServer();
			}catch(IOException e){
				System.err.println("Error while stopping server: "+e.getMessage());
			}
			serverMsgChecker.stop();
		}
	}//GEN-LAST:event_serverToggleButtonActionPerformed

	private void repaintDelayRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_repaintDelayRadioButtonActionPerformed
		sandpileController.setMinRepaintDelay(Integer.valueOf(repaintDelayTextField.getText()));
		sandpileController.setRepaintOnEveryUpdate(!repaintDelayRadioButton.isSelected());
	}//GEN-LAST:event_repaintDelayRadioButtonActionPerformed

	private void repaintOnUpdateRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_repaintOnUpdateRadioButtonActionPerformed
		sandpileController.setRepaintOnEveryUpdate(repaintOnUpdateRadioButton.isSelected());
	}//GEN-LAST:event_repaintOnUpdateRadioButtonActionPerformed

	private void repaintDelayTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_repaintDelayTextFieldActionPerformed
		try{
			sandpileController.setMinRepaintDelay(Integer.valueOf(repaintDelayTextField.getText()));
		}catch(Exception e){
			
		}
	}//GEN-LAST:event_repaintDelayTextFieldActionPerformed

	private void setSandRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setSandRadioButtonActionPerformed
		// TODO add your handling code here:
	}//GEN-LAST:event_setSandRadioButtonActionPerformed

	private void removeConfigButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeConfigButtonActionPerformed
		String selection = (String)configSelectList.getSelectedValue();
		sandpileController.removeConfigNamed(selection);
		updateConfigSelectList();
	}//GEN-LAST:event_removeConfigButtonActionPerformed

	private void vertexLabelsCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_vertexLabelsCheckBoxActionPerformed
		drawer.drawVertexLabels=vertexLabelsCheckBox.isSelected();
		sandpileController.repaint();
	}//GEN-LAST:event_vertexLabelsCheckBoxActionPerformed

	private void dimensionToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dimensionToggleButtonActionPerformed
		if(dimensionToggleButton.isSelected()){
			calculationThread = new Thread(){
				@Override public void run(){
					if(!calculationThreadInit("Calculating triangulation"))
							return;
					try{
						drawer3d.triangulate(sandpileController.vertexData);

						dimensionToggleButton.setText("2d");
						drawer3d.setCamera(drawer.getOriginX(), drawer.getOriginY());
						drawer3d.setZoom(drawer.getZoom());
						CardLayout cl = (CardLayout) canvasHolderPanel.getLayout();
						cl.show(canvasHolderPanel, "3d");
						sandpileController.setDrawer(drawer3d);
						currentDrawer = drawer3d;
					}catch(InterruptedException e){
						dimensionToggleButton.setText("3d");
						CardLayout cl = (CardLayout) canvasHolderPanel.getLayout();
						cl.show(canvasHolderPanel, "2d");
						sandpileController.setDrawer(drawer);
						dimensionToggleButton.setSelected(false);
						drawer.setZoom(drawer3d.getZoom());
						currentDrawer = drawer;
					}
					calculationThreadEnd();
				}
			};
			calculationThread.start();
		}else{
			dimensionToggleButton.setText("3d");
			CardLayout cl = (CardLayout) canvasHolderPanel.getLayout();
			cl.show(canvasHolderPanel,"2d");
			sandpileController.setDrawer(drawer);
			drawer.setZoom(drawer3d.getZoom());
			currentDrawer = drawer;
		}
		sandpileController.repaint();
	}//GEN-LAST:event_dimensionToggleButtonActionPerformed

	private void heightSmoothingSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_heightSmoothingSpinnerStateChanged
		drawer3d.setHeightSmoothing((Integer)heightSmoothingSpinner.getValue());
		sandpileController.repaint();
	}//GEN-LAST:event_heightSmoothingSpinnerStateChanged

	private void colorSmoothingSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_colorSmoothingSpinnerStateChanged
		drawer3d.setColorSmoothing((Integer)colorSmoothingSpinner.getValue());
		sandpileController.repaint();
	}//GEN-LAST:event_colorSmoothingSpinnerStateChanged

	private void heightScalarSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_heightScalarSpinnerStateChanged
		drawer3d.setHeightScalar((Float)heightScalarSpinner.getValue());
		sandpileController.repaint();
	}//GEN-LAST:event_heightScalarSpinnerStateChanged

	private void drawShapeCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drawShapeCheckBoxActionPerformed
		drawer3d.setDrawShape(drawShapeCheckBox.isSelected());
		sandpileController.repaint();
	}//GEN-LAST:event_drawShapeCheckBoxActionPerformed

	private void drawWireCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drawWireCheckBoxActionPerformed
		drawer3d.setDrawWire(drawWireCheckBox.isSelected());
		sandpileController.repaint();
	}//GEN-LAST:event_drawWireCheckBoxActionPerformed

	private void subtractConfigButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subtractConfigButtonActionPerformed
		if(runningThread)
			return;
		String selection = (String)configSelectList.getSelectedValue();
		final int times = -(Integer)configTimesSpinner.getValue();
		if(selection!=null){
			if(selection.equals(MAX_CONFIG)){
				sandpileController.addMaxStableConfig(times);
			}else if(selection.equals(CURRENT_CONFIG)){
				sandpileController.addCurrentConfig(times);
			}if(selection.equals(DUAL_CONFIG)){
				sandpileController.addDualConfig(times);
			}else if(selection.equals(ONES_CONFIG)){
				sandpileController.addSandEverywhere(times);
			}  else if (selection.equals(IDENTITY_CONFIG)) {
				calculationThread = new Thread(){
					@Override public void run(){
						if(!calculationThreadInit("Calculating identity"))
							return;
						try{
							sandpileController.addIdentity(times);
						}catch(InterruptedException e){

						}
						calculationThreadEnd();
					}
				};
				calculationThread.start();
			} else if (selection.equals(BURNING_CONFIG)) {
				calculationThread = new Thread(){
					@Override public void run(){
						if(!calculationThreadInit("Calculating burning"))
							return;
						try{
							sandpileController.addBurningConfig(times);
						}catch(InterruptedException e){

						}
						calculationThreadEnd();
					}
				};
				calculationThread.start();
			} else if (selection.equals(EQUIVALENT_RECURRENT)) {
				calculationThread = new Thread(){
					@Override public void run(){
						if(!calculationThreadInit("Calculating equivalent recurrent"))
							return;
						try{
							sandpileController.addEquivalentRecurrent(times);
						}catch(InterruptedException e){

						}
						calculationThreadEnd();
					}
				};
				calculationThread.start();
			} else if (selection.equals(INVERSE_CONFIG)) {
				calculationThread = new Thread(){
					@Override public void run(){
						if(!calculationThreadInit("Calculating inverse"))
							return;
						try{
							sandpileController.addInverseConfig(times);
						}catch(InterruptedException e){

						}
						calculationThreadEnd();
					}
				};
				calculationThread.start();
			}else if(selection.equals(RANDOM_CONFIG)){
				sandpileController.addRandomConfig(times);
			}else {
				sandpileController.addConfigNamed(selection,times);
			}
		}
	}//GEN-LAST:event_subtractConfigButtonActionPerformed

	private void configManagerOptionsPanelComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_configManagerOptionsPanelComponentShown
		updateConfigSelectList();
	}//GEN-LAST:event_configManagerOptionsPanelComponentShown

	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
		if(runningThread)
			calculationThread.interrupt();
	}//GEN-LAST:event_cancelButtonActionPerformed

	private void makeSinkButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_makeSinkButtonActionPerformed
		sandpileController.makeSink(sandpileController.getSelectedVertices());
	}//GEN-LAST:event_makeSinkButtonActionPerformed

	private void formMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseEntered
		if(runningThread){
			setWaitCursor();
		}
	}//GEN-LAST:event_formMouseEntered

	private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
		drawer.setBaseConfig(sandpileController.getConfig());
		drawer3d.setBaseConfig(sandpileController.getConfig());
	}//GEN-LAST:event_jButton1ActionPerformed

	private void selecteCenterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selecteCenterButtonActionPerformed
		drawer.setGLDimensions(getSelectedX(), getSelectedY(), drawer.getWidth(), drawer.getHeight());
		updateCenterCoordLabel();
	}//GEN-LAST:event_selecteCenterButtonActionPerformed

	public void updateConfigSelectList() {
		Vector<String> newList = new Vector<String>(java.util.Arrays.asList(defaultConfigs));
		for(String s : sandpileController.getStoredConfigNames()){
			if(!(s.equals(IDENTITY_CONFIG) || s.equals(BURNING_CONFIG)))
				newList.add(s);
		}
		configSelectList.setListData(newList);
	}

	public void updateZoomTextField(){
		zoomTextField.setText(String.format("%.2f",(currentDrawer.getZoom()*100.0f)));
	}

	public boolean updateDrawerZoom(){
		try{
			drawer.setZoom(Float.valueOf(zoomTextField.getText())/100.0f);
			drawer3d.setZoom(Float.valueOf(zoomTextField.getText())/100.0f);
			return true;
		}catch(NumberFormatException e){
			return false;
		}catch(NullPointerException e){
			return false;
		}
	}

	public void updateDelayTextField() {
		delayTextField.setText(String.valueOf(sandpileController.getMinUpdateDelay()));
	}

	public boolean updateControllerDelay(){
		try{
			sandpileController.setMinUpdateDelay(Long.valueOf(delayTextField.getText()));
			return true;
		}catch(NumberFormatException e){
			return false;
		}catch(NullPointerException e){
			return false;
		}
	}

	public float getSelectedX() {
		TIntArrayList selVerts = sandpileController.getSelectedVertices();
		float x = 0f;
		if(!selVerts.isEmpty()){
			int n = selVerts.size();
			for(int i=0; i<n; i++){
				int v=selVerts.get(i);
				x+=sandpileController.getVertexX(v);
			}
			x/=n;
		}
		return x;
	}

	public float getSelectedY() {
		TIntArrayList selVerts = sandpileController.getSelectedVertices();
		float y = 0f;
		if(!selVerts.isEmpty()){
			int n = selVerts.size();
			for(int i=0; i<n; i++){
				int v=selVerts.get(i);
				y+=sandpileController.getVertexY(v);
			}
			y/=n;
		}
		return y;
	}

	public int getSelectedDegree() {
		TIntArrayList selVerts = sandpileController.getSelectedVertices();
		int degree = 0;
		if(!selVerts.isEmpty()){
			int n = selVerts.size();
			for(int i=0; i<n; i++){
				int v=selVerts.get(i);
				degree+=sandpileController.getGraph().degree(v);
			}
		}
		return degree;
	}

	public int getSelectedSand() {
		TIntArrayList selVerts = sandpileController.getSelectedVertices();
		int sand = 0;
		if(!selVerts.isEmpty()){
			int n = selVerts.size();
			for(int i=0; i<n; i++){
				int v=selVerts.get(i);
				sand+=sandpileController.getSand(v);
			}
		}
		return sand;
	}

	public int getSelectedFirings() {
		TIntArrayList selVerts = sandpileController.getSelectedVertices();
		int firings = 0;
		if(!selVerts.isEmpty()){
			int n = selVerts.size();
			for(int i=0; i<n; i++){
				int v=selVerts.get(i);
				firings+=sandpileController.getFirings(v);
			}
		}
		return firings;
	}

	public void updateSelectedInfo() {
		TIntArrayList selVerts = sandpileController.getSelectedVertices();
		if(!selVerts.isEmpty()){
			int n = selVerts.size();
			float x=getSelectedX();
			float y=getSelectedY();
			int degree=getSelectedDegree();
			int sand=getSelectedSand();
			int firings=getSelectedFirings();
			this.selecteCenterButton.setText(String.format("Sel. Center: %.2f, %.2f; ", x,y));
			this.selectedDegreeLabel.setText(String.format("Degree: %d; ",degree));
			this.selectedSandLabel.setText(String.format("Sand: %d; ",sand));
			this.selectedFiringsLabel.setText(String.format("Firings: %d; ",firings));
		}else{
			this.selecteCenterButton.setText("Sel. Center: -, -; ");
			this.selectedDegreeLabel.setText("Degree: -; ");
			this.selectedSandLabel.setText("Sand: -; ");
			this.selectedFiringsLabel.setText("Firings: -;");
		}
	}

	public void updateCenterCoordLabel() {
		centerCoordLabel.setText(String.format("%.2f, %.2f", drawer.getOriginX(), drawer.getOriginY()));
	}

	public void onGraphChange(SandpileGraph sg) {
		updateSelectedInfo();
		drawer.setBaseConfig(sandpileController.getGraph().getUniformConfig(0));
		drawer3d.setBaseConfig(sandpileController.getGraph().getUniformConfig(0));
	}

	public void onConfigChange(SandpileConfiguration config) {
		updateSelectedInfo();
	}

	public BufferedImage getCanvasShot(){
		int height = canvasHolderPanel.getHeight();
		int width = canvasHolderPanel.getWidth();
		int x = canvasHolderPanel.getLocationOnScreen().x;
		int y = canvasHolderPanel.getLocationOnScreen().y;
		Rectangle rect = new Rectangle(x,y,width,height);
		try{
			Robot robot = new Robot();
			return robot.createScreenCapture(rect);
		}catch(java.awt.AWTException e){
			javax.swing.JOptionPane.showInternalMessageDialog(this, "Error creating robot: "+e.getMessage());
			return null;
		}
	}

	public void setWaitCursor(){
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}

	public void setDefaultCursor(){
		this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	public boolean calculationThreadInit(String description){
		if(runningThread)
			return false;
		runningThread = true;
		setWaitCursor();
		currentActionLabel.setText(description+": ");
		currentActionLabel.setEnabled(true);
		cancelButton.setEnabled(true);
		return true;
	}

	public void calculationThreadEnd(){
		setDefaultCursor();
		currentActionLabel.setText("None: ");
		currentActionLabel.setEnabled(false);
		cancelButton.setEnabled(false);
		sandpileController.repaint();
		runningThread = false;
	}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addConfigButton;
    private javax.swing.JRadioButton addEdgeRadioButton;
    private javax.swing.JRadioButton addSandRadioButton;
    private javax.swing.JRadioButton addUndirectedEdgeRadioButton;
    private javax.swing.JRadioButton addVertexRadioButton;
    private javax.swing.JSpinner amountOfSandSpinner;
    private javax.swing.JButton bigDecDelayButton;
    private javax.swing.JButton bigIncDelayButton;
    private javax.swing.JButton bigZoomInButton;
    private javax.swing.JButton bigZoomOutButton;
    private javax.swing.JPanel blankOptionsPanel;
    private javax.swing.JPanel buildLaticePanel;
    private javax.swing.JTable buildLatticeTable;
    private javax.swing.JButton cancelButton;
    private javax.media.opengl.GLCanvas canvas;
    private javax.media.opengl.GLCanvas canvas3d;
    private javax.swing.JPanel canvasHolderPanel;
    private javax.swing.JLabel centerCoordLabel;
    private javax.swing.JLabel centerLabel;
    private javax.swing.JCheckBox changingNodeSizeCheckBox;
    private javax.swing.JButton clearSandButton;
    private javax.swing.JSpinner colSpinner;
    private javax.swing.JComboBox colorModeComboBox;
    private javax.swing.JSpinner colorSmoothingSpinner;
    private javax.swing.JPanel configManagerOptionsPanel;
    private javax.swing.JList configSelectList;
    private javax.swing.JSpinner configTimesSpinner;
    private javax.swing.JPanel controlPanel;
    private javax.swing.JComboBox controlStateComboBox;
    private javax.swing.JToolBar controlToolBar;
    private javax.swing.JLabel currentActionLabel;
    private javax.swing.JLabel delayLabel;
    private javax.swing.JTextField delayTextField;
    private javax.swing.JButton deleteGraphButton;
    private javax.swing.JButton deleteSelectedVerticesButton;
    private javax.swing.JToggleButton dimensionToggleButton;
    private javax.swing.JCheckBox drawEdgesCheckBox;
    private javax.swing.JCheckBox drawShapeCheckBox;
    private javax.swing.JCheckBox drawWireCheckBox;
    private javax.swing.JComboBox eBorderComboBox;
    private javax.swing.JCheckBox edgeLabelsCheckBox;
    private javax.swing.JTextField edgeWeightField;
    private javax.swing.ButtonGroup editConfigButtonGroup;
    private javax.swing.ButtonGroup editGraphButtonGroup;
    private javax.swing.JPanel editGraphPanel;
    private javax.swing.JToggleButton editToggleButton;
    private javax.swing.JTextField gridColsField;
    private javax.swing.JTextField gridRowsField;
    private javax.swing.JLabel gridSizeCrossLabel;
    private javax.swing.JLabel gridSizeCrossLabel1;
    private javax.swing.JLabel gridSizeLabel;
    private javax.swing.JLabel gridSizeLabel1;
    private javax.swing.JSpinner heightScalarSpinner;
    private javax.swing.JSpinner heightSmoothingSpinner;
    private javax.swing.JComboBox hexEBorderComboBox;
    private javax.swing.JTextField hexGridColsField;
    private javax.swing.JTextField hexGridRowsField;
    private javax.swing.JComboBox hexNBorderComboBox;
    private javax.swing.JComboBox hexSBorderComboBox;
    private javax.swing.JComboBox hexWBorderComboBox;
    private javax.swing.JToolBar infoToolBar;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JToolBar.Separator jSeparator4;
    private javax.swing.JToolBar.Separator jSeparator5;
    private javax.swing.JToolBar.Separator jSeparator6;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSpinner latticeSpacingSpinner;
    private javax.swing.JPanel makeGridOptionsPanel;
    private javax.swing.JPanel makeHexGridOptionsPanel;
    private javax.swing.JComboBox makeHoneycombBorderComboBox;
    private javax.swing.JPanel makeHoneycombOptionsPanel;
    private javax.swing.JTextField makeHoneycombRadiusField;
    private javax.swing.JButton makeSinkButton;
    private javax.swing.ButtonGroup mouseButtonGroup;
    private javax.swing.JToolBar mouseToolBar;
    private javax.swing.JComboBox nBorderComboBox;
    private javax.swing.JPanel optionsContainerPanel;
    private javax.swing.JTabbedPane optionsTabbedPane;
    private javax.swing.JCheckBox printFPSCheckBox;
    private javax.swing.JButton quitButton;
    private javax.swing.JButton removeConfigButton;
    private javax.swing.JRadioButton removeEdgeRadioButton;
    private javax.swing.JRadioButton removeSandRadioButton;
    private javax.swing.JRadioButton removeUndirectedEdgeRadioButton;
    private javax.swing.JRadioButton removeVertexRadioButton;
    private javax.swing.JCheckBox repaintCheckBox;
    private javax.swing.JRadioButton repaintDelayRadioButton;
    private javax.swing.JTextField repaintDelayTextField;
    private javax.swing.JRadioButton repaintOnUpdateRadioButton;
    private javax.swing.ButtonGroup repaintOptionsButtonGroup;
    private javax.swing.JButton resetFiringsButton;
    private javax.swing.JSpinner rowSpinner;
    private javax.swing.JToggleButton runButton;
    private javax.swing.JComboBox sBorderComboBox;
    private javax.swing.JScrollPane sandpileViewScrollPane;
    private javax.swing.JToggleButton selectToggleButton;
    private javax.swing.JButton selecteCenterButton;
    private javax.swing.JLabel selectedDegreeLabel;
    private javax.swing.JLabel selectedFiringsLabel;
    private javax.swing.JLabel selectedSandLabel;
    private javax.swing.JToggleButton serverToggleButton;
    private javax.swing.JButton setConfigButton;
    private javax.swing.JRadioButton setSandRadioButton;
    private javax.swing.JButton smallDevDelayButton;
    private javax.swing.JButton smallIncDelayButton;
    private javax.swing.JButton smallZoomInButton;
    private javax.swing.JButton smallZoomOutButton;
    private javax.swing.JButton stabilizeButton;
    private javax.swing.JButton stepButton;
    private javax.swing.JButton storeConfigButton;
    private javax.swing.JButton subtractConfigButton;
    private javax.swing.JCheckBox vertexLabelsCheckBox;
    private javax.swing.JPanel visualOptionsPanel;
    private javax.swing.JComboBox wBorderComboBox;
    private javax.swing.JTextField zoomTextField;
    // End of variables declaration//GEN-END:variables

}
