package plotcraft.editor;

import plotcraft.editor.tools.PC_Brush;
import plotcraft.editor.tools.PC_Tool;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;


/**
 * PC_Editor
 *
 * Manages the UI for the editor.
 *
 * Created by harper on 10/5/2015.
 */
public class PC_Editor {
	private JFrame _window;

	private JLabel _statsField;
	private JToggleButton _brushToggleButton;
	private JToggleButton _lineToggleButton;
	private JToggleButton _rectToggleButton;
	private JToggleButton _circleToggleButton;
	private JToggleButton _selectToggleButton;
	private JScrollPane _editorScrollPane;
	private JPanel _mainPanel;
	private JSpinner _widthSpinner;
	private JSpinner _heightSpinner;
	private JSpinner _depthSpinner;
	private JSpinner _currentLayerSpinner;
	private JSpinner _tileSizeSpinner;
	private JScrollPane _tileSelectorScrollPane;
	private JPanel _tileSelectorPanel;

	private PC_EditorPanel _editorPanel;
	private PC_EditorModel _model;

	private PC_Tool _selectedTool;
	private PC_Tile _selectedTile;

	private final JFileChooser _fileChooser = new JFileChooser();

	private PC_Editor() {
		_model = new PC_EditorModel();
		_selectedTool = new PC_Brush(this);

		_brushToggleButton.addActionListener(actionEvent -> {
			System.out.println("Pencil button pressed.");
			_model.setCurrentTool(PC_EditorModel.EditorTool.ET_Pencil);
			_selectedTool = new PC_Brush(this);
			updateStatusText();
		});
		_lineToggleButton.addActionListener(actionEvent -> {
			System.out.println("Line button pressed.");
			_model.setCurrentTool(PC_EditorModel.EditorTool.ET_Line);
		});
		_rectToggleButton.addActionListener(actionEvent -> {
			System.out.println("Rect button pressed.");
			_model.setCurrentTool(PC_EditorModel.EditorTool.ET_Rect);
		});
		_circleToggleButton.addActionListener(actionEvent -> {
			System.out.println("Circle button pressed.");
			_model.setCurrentTool(PC_EditorModel.EditorTool.ET_Circle);
		});
		_selectToggleButton.addActionListener(actionEvent -> {
			System.out.println("Select button pressed.");
			_model.setCurrentTool(PC_EditorModel.EditorTool.ET_Select);
		});
		_currentLayerSpinner.addChangeListener(changeEvent -> {
			int val = (Integer)_currentLayerSpinner.getValue();
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
			_model.setWidth((Integer)_widthSpinner.getValue());
			_editorPanel.updateSize();
			_editorPanel.revalidate();
		});
		_heightSpinner.addChangeListener(changeEvent -> {
			_model.setHeight((Integer)_heightSpinner.getValue());
			_editorPanel.updateSize();
			_editorPanel.revalidate();
		});
		_depthSpinner.addChangeListener(changeEvent -> {
			int newDepth = (Integer)_depthSpinner.getValue();
			_model.setDepth(newDepth);
			if ((Integer)_currentLayerSpinner.getValue() > newDepth - 1) {
				_currentLayerSpinner.setValue(newDepth - 1);
				_model.setCurrentLayer(newDepth - 1);
			}
			_editorPanel.updateSize();
			_editorPanel.revalidate();
		});
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
		}
		catch (UnsupportedLookAndFeelException | ClassNotFoundException | IllegalAccessException | InstantiationException e) {
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

	private void makeMenu(JFrame frame) {
		JMenuBar menuBar = new JMenuBar();

		int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		// File menu
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);

		JMenuItem fileNewItem = new JMenuItem("New", KeyEvent.VK_N);
		fileNewItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, menuMask));
		fileNewItem.setIcon(null);
		fileNewItem.addActionListener(actionEvent -> doNew());

		JMenuItem fileOpenItem = new JMenuItem("Open", KeyEvent.VK_O);
		fileOpenItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, menuMask));
		fileOpenItem.setIcon(null);
		fileOpenItem.addActionListener(actionEvent -> doOpen());

		JMenuItem fileSaveItem = new JMenuItem("Save", KeyEvent.VK_S);
		fileSaveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, menuMask));
		fileSaveItem.setIcon(null);
		fileSaveItem.addActionListener(actionEvent -> doSave());

		JMenuItem fileExitItem = new JMenuItem("Exit", KeyEvent.VK_X);
		fileExitItem.addActionListener(actionEvent -> {
			System.exit(0);
		});

		fileMenu.add(fileNewItem);
		fileMenu.add(fileOpenItem);
		fileMenu.addSeparator();
		fileMenu.add(fileSaveItem);
		fileMenu.addSeparator();
		fileMenu.add(fileExitItem);

		menuBar.add(fileMenu);

		// Edit menu

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
		_statsField.setText(String.format("[%s]: %s @(%d, %d)", _selectedTool.getToolName(), _selectedTile.name, mp.x, mp.y));
	}

	public PC_Tool getSelectedTool() { return _selectedTool; }
	public PC_Tile getSelectedTile() { return _selectedTile; }
	public int getTileSize() {
		return (Integer) _tileSizeSpinner.getValue();
	}
}
