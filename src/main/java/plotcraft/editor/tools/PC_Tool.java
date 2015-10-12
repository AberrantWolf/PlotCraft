package plotcraft.editor.tools;

import plotcraft.editor.PC_Editor;
import plotcraft.editor.PC_EditorModel;
import plotcraft.editor.PC_Tile;

import javax.swing.*;
import java.util.ArrayList;

/**
 * Interface for mouse handling and other methods required by drawing tools
 *
 * Created by harper on 10/9/2015.
 */
public abstract class PC_Tool {
	protected ArrayList<PC_EditedTile> _edits;
	protected int _mouseDownX, _mouseDownY;
	protected int _mouseCurrentX, _mouseCurrentY;
	protected boolean _isMousePressed;
	protected PC_Editor _controller;
	protected PC_EditorModel _model;

	public PC_Tool(PC_Editor controller, PC_EditorModel model) {
		_controller = controller;
		_model = model;
	}

	public final ArrayList<PC_EditedTile> getEditedTiles() {
		if (_isMousePressed)
			return _edits;

		return null;
	}

	abstract public String getToolName();
	abstract public void setupToolOptions(JPanel optionsPanel);

	abstract protected void handleMouseDown();
	protected void handleMouseUp() {
		commitEdits();
	}
	abstract protected void handleMouseDrag();

	protected void commitEdits() {
		if (_edits == null || _edits.size() < 1) {
			return;
		}

		ArrayList<PC_EditedTile> undo = new ArrayList<>();

		for (PC_EditedTile tile : _edits) {
			int x = tile.x;
			int y = tile.y;
			undo.add(new PC_EditedTile(x, y, _model.getTile(x, y)));

			_model.setTile(x, y, tile.data);
		}

		// TODO: register undo list with undo manager

		_edits.clear();
	}

	public final void onMouseDown(int x, int y) {
		_edits = new ArrayList<>();
		_mouseDownX = _mouseCurrentX = x;
		_mouseDownY = _mouseCurrentY = y;
		_isMousePressed = true;

		handleMouseDown();
	}

	public final void onMouseUp(int x, int y) {
		if (!_isMousePressed) {
			return;
		}

		_mouseCurrentX = x;
		_mouseCurrentY = y;

		_isMousePressed = false;
		handleMouseUp();
	}

	public final void onMouseDrag(int x, int y) {
		if (_isMousePressed) {
			_mouseCurrentX = x;
			_mouseCurrentY = y;

			handleMouseDrag();
		}
	}
}
