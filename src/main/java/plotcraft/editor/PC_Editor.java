package plotcraft.editor;

import plotcraft.editor.tools.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;


/**
 * PC_Editor
 * <p>
 * Manages the UI for the editor.
 * <p>
 * Created by harper on 10/5/2015.
 */
public class PC_Editor {
	private JFrame _window;

	private JLabel _statsField;
	private JToggleButton _brushToggleButton;
	private JToggleButton _lineToggleButton;
	private JToggleButton _rectToggleButton;
	private JToggleButton _ellipseToggleButton;
	private JToggleButton _selectToggleButton;
	private JScrollPane _editorScrollPane;
	private JPanel _mainPanel;
	private JSpinner _widthSpinner;
	private JSpinner _heightSpinner;
	private JSpinner _depthSpinner;
	private JSpinner _currentLayerSpinner;
	private JSpinner _tileSizeSpinner;
	private JScrollPane _tileSelectorScrollPane;
	private JPanel _optionsPanel;
	private JPanel _tileSelectorPanel;

	private PC_EditorPanel _editorPanel;
	private PC_EditorModel _model;

	private PC_Tool _currentTool;
	private PC_Tile _selectedTile;

	private final JFileChooser _fileChooser = new JFileChooser();

	private PC_Editor() {
		_model = new PC_EditorModel();
		_currentTool = new PC_BrushTool(this, _model);
		_copyBuffer = new ArrayList<>();

		$$$setupUI$$$();
		_brushToggleButton.addActionListener(actionEvent -> {
			System.out.println("Pencil button pressed.");
			_model.setCurrentTool(PC_EditorModel.EditorTool.ET_Pencil);
			changeTool(new PC_BrushTool(this, _model));
		});
		_lineToggleButton.addActionListener(actionEvent -> {
			System.out.println("Line button pressed.");
			_model.setCurrentTool(PC_EditorModel.EditorTool.ET_Line);
			changeTool(new PC_LineTool(this, _model));
		});
		_rectToggleButton.addActionListener(actionEvent -> {
			System.out.println("Rect button pressed.");
			_model.setCurrentTool(PC_EditorModel.EditorTool.ET_Rect);
			changeTool(new PC_RectTool(this, _model));
		});
		_ellipseToggleButton.addActionListener(actionEvent -> {
			System.out.println("Ellipse button pressed.");
			_model.setCurrentTool(PC_EditorModel.EditorTool.ET_Ellipse);
			changeTool(new PC_EllipseTool(this, _model));
		});
		_selectToggleButton.addActionListener(actionEvent -> {
			System.out.println("Select button pressed.");
			_model.setCurrentTool(PC_EditorModel.EditorTool.ET_Select);
			changeTool(new PC_SelectTool(this, _model));
		});
		_currentLayerSpinner.addChangeListener(changeEvent -> {
			int val = (Integer) _currentLayerSpinner.getValue();
			if (val < 0)
				val = 0;
			if (val > _model.getDepth() - 1)
				val = _model.getDepth() - 1;

			_currentLayerSpinner.setValue(val);

			_model.setCurrentLayer(val);
			_editorPanel.repaint();
		});
		_tileSizeSpinner.addChangeListener(changeEvent -> {
			_editorPanel.updateSize();

			PC_Config.getInstance().cacheImages(getTileSize(), _editorPanel.getGraphicsConfiguration());

			_editorPanel.revalidate();
		});
		_widthSpinner.addChangeListener(changeEvent -> {
			_model.setWidth((Integer) _widthSpinner.getValue());
			_editorPanel.updateSize();
			_editorPanel.revalidate();
		});
		_heightSpinner.addChangeListener(changeEvent -> {
			_model.setHeight((Integer) _heightSpinner.getValue());
			_editorPanel.updateSize();
			_editorPanel.revalidate();
		});
		_depthSpinner.addChangeListener(changeEvent -> {
			int newDepth = (Integer) _depthSpinner.getValue();
			_model.setDepth(newDepth);
			if ((Integer) _currentLayerSpinner.getValue() > newDepth - 1) {
				_currentLayerSpinner.setValue(newDepth - 1);
				_model.setCurrentLayer(newDepth - 1);
			}
			_editorPanel.updateSize();
			_editorPanel.revalidate();
		});
	}

	private void changeTool(PC_Tool tool) {
		_currentTool.commitEdits();
		_editorPanel.repaint();

		_currentTool = tool;
		_optionsPanel.removeAll();
		_currentTool.setupToolOptions(_optionsPanel);
		updateStatusText();
	}

	public static PC_Editor makeEditor() {
		PC_Editor editor = new PC_Editor();
		JFrame frame = new JFrame("PCEditor");
		frame.setMinimumSize(new Dimension(640, 480));

		frame.setContentPane(editor._mainPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// TODO: call a method to add a menu bar here
		//...

		// Add the editor panel to the premade setup
		editor._editorPanel = new PC_EditorPanel();
		editor._editorPanel.SetDataModel(editor._model);
		editor._editorPanel.SetController(editor);
		editor._editorScrollPane.setViewportView(editor._editorPanel);

		editor._editorPanel.updateSize();
		editor._editorScrollPane.getViewport().revalidate();

		PC_Config config = PC_Config.getInstance();
		config.cacheImages(editor.getTileSize(), editor._editorPanel.getGraphicsConfiguration());

		// create the buttons for selecting what tile to use
		WrapLayout wrap = new WrapLayout();
		editor._tileSelectorPanel = new JPanel();
		editor._tileSelectorPanel.setLayout(wrap);

		editor._tileSelectorScrollPane.setViewportView(editor._tileSelectorPanel);
		editor._tileSelectorScrollPane.getVerticalScrollBar().setUnitIncrement(8);

		int btn_size = config.getPreviewSize() + 10;
		ButtonGroup tileGroup = new ButtonGroup();
		for (PC_Tile tile : config.getAllTiles()) {
			JToggleButton tbtn;
			if (tile.image != null) {
				tbtn = new JToggleButton(new ImageIcon(tile.toolPreview));
			} else {
				tbtn = new JToggleButton();
			}
			Dimension d = new Dimension(btn_size, btn_size);
			tbtn.setMinimumSize(d);
			tbtn.setMaximumSize(d);
			tbtn.setPreferredSize(d);
			tbtn.setFocusable(false);
			tbtn.setToolTipText(tile.name);
			editor._tileSelectorPanel.add(tbtn);

			if (editor._selectedTile == null) {
				editor._selectedTile = tile;
				tbtn.setSelected(true);
			}

			tbtn.addActionListener(actionEvent -> {
				editor._selectedTile = tile;
				editor.updateStatusText();
			});

			tileGroup.add(tbtn);
		}

		// Switch to system LnF for a more native-feeling experience
		try {
			// Set System L&F
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException | ClassNotFoundException | IllegalAccessException | InstantiationException e) {
			// handle exception
			System.out.println("ERROR: Could not change to system look and feel");
			e.printStackTrace();
		}
		SwingUtilities.updateComponentTreeUI(frame);
		SwingUtilities.updateComponentTreeUI(editor._fileChooser);

		editor.makeMenu(frame);

		frame.pack();
		frame.setVisible(true);

		editor._window = frame;

		editor.updateStatusText();
		editor.updateWindowTitle();

		return editor;
	}

	JMenuItem _editCopyItem;
	JMenuItem _editPasteItem;
	private void makeMenu(JFrame frame) {
		JMenuBar menuBar = new JMenuBar();

		int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		// File menu
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);

		JMenuItem fileNewItem = new JMenuItem("New", KeyEvent.VK_N);
		fileNewItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcutMask));
		fileNewItem.addActionListener(actionEvent -> doNew());

		JMenuItem fileOpenItem = new JMenuItem("Open", KeyEvent.VK_O);
		fileOpenItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcutMask));
		fileOpenItem.addActionListener(actionEvent -> doOpen());

		JMenuItem fileSaveItem = new JMenuItem("Save", KeyEvent.VK_S);
		fileSaveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutMask));
		fileSaveItem.addActionListener(actionEvent -> doSave());

		JMenuItem fileExitItem = new JMenuItem("Exit", KeyEvent.VK_X);
		fileExitItem.addActionListener(actionEvent -> {
			// TODO: Check for saaving, etc...
			System.exit(0);
		});

		fileMenu.add(fileNewItem);
		fileMenu.add(fileOpenItem);
		fileMenu.addSeparator();
		fileMenu.add(fileSaveItem);
		fileMenu.addSeparator();
		fileMenu.add(fileExitItem);

		// Edit menu
		JMenu editMenu = new JMenu("Edit");
		fileMenu.setMnemonic(KeyEvent.VK_E);

		_editCopyItem = new JMenuItem("Copy Tiles", KeyEvent.VK_C);
		_editCopyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutMask));
		_editCopyItem.addActionListener(actionEvent -> doCopy());
		setCanCopy(false);

		_editPasteItem = new JMenuItem("Paste Tiles", KeyEvent.VK_P);
		_editPasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcutMask));
		_editPasteItem.addActionListener(actionEvent -> doPaste());
		setCanPaste(false);

		editMenu.add(_editCopyItem);
		editMenu.add(_editPasteItem);

		// Add menus to bar
		menuBar.add(fileMenu);
		menuBar.add(editMenu);

		frame.setJMenuBar(menuBar);
	}

	private void updateUIFromModel() {
		_widthSpinner.setValue(_model.getWidth());
		_heightSpinner.setValue(_model.getHeight());
		_depthSpinner.setValue(_model.getDepth());
		_currentLayerSpinner.setValue(_model.getCurrentLayer());
	}

	private void doNew() {
		_model.reset();
		updateUIFromModel();
		updateStatusText();
		updateWindowTitle();

		_editorPanel.updateSize();
		_editorPanel.repaint();
	}

	private void doOpen() {
		_fileChooser.setFileFilter(new FileNameExtensionFilter("JSON", "json"));
		int result = _fileChooser.showOpenDialog(_mainPanel);

		if (result == JFileChooser.APPROVE_OPTION) {
			File file = _fileChooser.getSelectedFile();
			try {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				StringBuilder sb = new StringBuilder();
				String line = reader.readLine();

				while (line != null) {
					sb.append(line).append("\n");
					line = reader.readLine();
				}

				if (_model.deserializeFromJson(sb.toString())) {
					System.out.println("File loaded successfully");
					_model.setFilePath(file.getPath());

					updateUIFromModel();
					updateStatusText();
					updateWindowTitle();

					_editorPanel.updateSize();
					_editorPanel.revalidate();
					_editorPanel.repaint();
				} else {
					System.out.println("ERROR: Could not load selected file: " + file.getName());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void doSave() {
		File file = null;

		String path = _model.getFilePath();
		if (path != null) {
			file = new File(path);
		} else {
			int result = _fileChooser.showSaveDialog(_mainPanel);

			if (result == JFileChooser.APPROVE_OPTION) {
				file = _fileChooser.getSelectedFile();
			}
		}

		if (file != null) {
			try {
				PrintWriter writer = new PrintWriter(new FileWriter(file));
				String json = _model.serializeToJson();
				writer.print(json);
				writer.close();

				_model.setFilePath(file.getPath());

				updateWindowTitle();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private boolean _canCopy;
	public void setCanCopy(boolean can) {
		_canCopy = can;

		_editCopyItem.setEnabled(_canCopy);
	}
	private boolean _canPaste;
	public void setCanPaste(boolean can) {
		_canPaste = can;
		_editPasteItem.setEnabled(_canPaste);
	}

	ArrayList<PC_EditedTile> _copyBuffer;
	private void doCopy() {
		if (!_canCopy) {
			return;
		}

		_copyBuffer.clear();
		_currentTool.getEditedTiles().forEach((tile) -> _copyBuffer.add(new PC_EditedTile(tile)));

		if (_copyBuffer.size() > 0) {
			setCanPaste(true);
		}
	}

	private void doPaste() {
		if (!_canPaste || _copyBuffer.size() < 1) {
			return;
		}

		if (_currentTool.getClass() != PC_SelectTool.class) {
			_selectToggleButton.doClick();
			//changeTool(new PC_SelectTool(this, _model));
		}

		PC_SelectTool selTool = (PC_SelectTool) _currentTool;
		selTool.injectSelection(_copyBuffer);
	}

	private void createUIComponents() {
		// TODO: place custom component creation code here
		_tileSizeSpinner = new JSpinner();
		_tileSizeSpinner.setModel(new SpinnerNumberModel(32, 8, 256, 8));

		_widthSpinner = new JSpinner();
		_widthSpinner.setModel(new SpinnerNumberModel(8, 1, null, 1));

		_heightSpinner = new JSpinner();
		_heightSpinner.setModel(new SpinnerNumberModel(8, 1, null, 1));

		_depthSpinner = new JSpinner();
		_depthSpinner.setModel(new SpinnerNumberModel(1, 1, null, 1));
	}

	public void updateWindowTitle() {
		_window.setTitle("PlotCraft - " + _model.getFileName());
	}

	public void updateStatusText() {
		// TODO: display the position of the mouse cursor, but ONLY if the cursor is within the edit panel
		Point mp = _editorPanel.getCurrentGridPoint();
		_statsField.setText(String.format("[%s]: %s @(%d, %d)", _currentTool.getToolName(), _selectedTile.name, mp.x, mp.y));
	}

	public PC_Tool getSelectedTool() {
		return _currentTool;
	}

	public PC_Tile getSelectedTile() {
		return _selectedTile;
	}

	public int getTileSize() {
		return (Integer) _tileSizeSpinner.getValue();
	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer
	 * >>> IMPORTANT!! <<<
	 * DO NOT edit this method OR call it in your code!
	 *
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		createUIComponents();
		_mainPanel = new JPanel();
		_mainPanel.setLayout(new GridBagLayout());
		_mainPanel.setEnabled(true);
		final JSplitPane splitPane1 = new JSplitPane();
		GridBagConstraints gbc;
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		_mainPanel.add(splitPane1, gbc);
		final JSplitPane splitPane2 = new JSplitPane();
		splitPane2.setOrientation(0);
		splitPane1.setLeftComponent(splitPane2);
		final JPanel panel1 = new JPanel();
		panel1.setLayout(new GridBagLayout());
		splitPane2.setRightComponent(panel1);
		final JPanel panel2 = new JPanel();
		panel2.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(3, 3, 3, 3);
		panel1.add(panel2, gbc);
		final JPanel panel3 = new JPanel();
		panel3.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		panel2.add(panel3, gbc);
		final JLabel label1 = new JLabel();
		label1.setText("Width");
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.WEST;
		panel3.add(label1, gbc);
		final JLabel label2 = new JLabel();
		label2.setText("Height");
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.WEST;
		panel3.add(label2, gbc);
		final JLabel label3 = new JLabel();
		label3.setText("Layers");
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.WEST;
		panel3.add(label3, gbc);
		final JLabel label4 = new JLabel();
		label4.setText("Tile Size");
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.WEST;
		panel3.add(label4, gbc);
		final JPanel panel4 = new JPanel();
		panel4.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(1, 1, 1, 1);
		panel2.add(panel4, gbc);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel4.add(_widthSpinner, gbc);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel4.add(_heightSpinner, gbc);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel4.add(_depthSpinner, gbc);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel4.add(_tileSizeSpinner, gbc);
		final JPanel panel5 = new JPanel();
		panel5.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(3, 3, 3, 3);
		panel1.add(panel5, gbc);
		final JLabel label5 = new JLabel();
		label5.setText("Current Layer");
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.WEST;
		panel5.add(label5, gbc);
		_currentLayerSpinner = new JSpinner();
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel5.add(_currentLayerSpinner, gbc);
		final JLabel label6 = new JLabel();
		label6.setText("Inspector");
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weighty = 1.0;
		gbc.anchor = GridBagConstraints.WEST;
		panel5.add(label6, gbc);
		_optionsPanel = new JPanel();
		_optionsPanel.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.BOTH;
		panel5.add(_optionsPanel, gbc);
		_tileSelectorScrollPane = new JScrollPane();
		_tileSelectorScrollPane.setPreferredSize(new Dimension(100, 0));
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		panel1.add(_tileSelectorScrollPane, gbc);
		final JScrollPane scrollPane1 = new JScrollPane();
		scrollPane1.setMinimumSize(new Dimension(80, 100));
		splitPane2.setLeftComponent(scrollPane1);
		final JPanel panel6 = new JPanel();
		panel6.setLayout(new GridBagLayout());
		scrollPane1.setViewportView(panel6);
		_brushToggleButton = new JToggleButton();
		_brushToggleButton.setFocusable(false);
		_brushToggleButton.setSelected(true);
		_brushToggleButton.setText("Brush");
		_brushToggleButton.setMnemonic('B');
		_brushToggleButton.setDisplayedMnemonicIndex(0);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		panel6.add(_brushToggleButton, gbc);
		_lineToggleButton = new JToggleButton();
		_lineToggleButton.setEnabled(true);
		_lineToggleButton.setFocusable(false);
		_lineToggleButton.setText("Line");
		_lineToggleButton.setMnemonic('L');
		_lineToggleButton.setDisplayedMnemonicIndex(0);
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		panel6.add(_lineToggleButton, gbc);
		_rectToggleButton = new JToggleButton();
		_rectToggleButton.setEnabled(true);
		_rectToggleButton.setFocusable(false);
		_rectToggleButton.setText("Rect");
		_rectToggleButton.setMnemonic('R');
		_rectToggleButton.setDisplayedMnemonicIndex(0);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		panel6.add(_rectToggleButton, gbc);
		_ellipseToggleButton = new JToggleButton();
		_ellipseToggleButton.setEnabled(true);
		_ellipseToggleButton.setFocusable(false);
		_ellipseToggleButton.setText("Ellipse");
		_ellipseToggleButton.setMnemonic('L');
		_ellipseToggleButton.setDisplayedMnemonicIndex(1);
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		panel6.add(_ellipseToggleButton, gbc);
		_selectToggleButton = new JToggleButton();
		_selectToggleButton.setEnabled(true);
		_selectToggleButton.setFocusable(false);
		_selectToggleButton.setText("Select");
		_selectToggleButton.setMnemonic('S');
		_selectToggleButton.setDisplayedMnemonicIndex(0);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 1.0;
		panel6.add(_selectToggleButton, gbc);
		_editorScrollPane = new JScrollPane();
		_editorScrollPane.setMinimumSize(new Dimension(200, 200));
		splitPane1.setRightComponent(_editorScrollPane);
		_statsField = new JLabel();
		_statsField.setText("Stats Display");
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		gbc.anchor = GridBagConstraints.WEST;
		_mainPanel.add(_statsField, gbc);
		ButtonGroup buttonGroup;
		buttonGroup = new ButtonGroup();
		buttonGroup.add(_brushToggleButton);
		buttonGroup.add(_lineToggleButton);
		buttonGroup.add(_rectToggleButton);
		buttonGroup.add(_ellipseToggleButton);
		buttonGroup.add(_selectToggleButton);
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return _mainPanel;
	}
}
