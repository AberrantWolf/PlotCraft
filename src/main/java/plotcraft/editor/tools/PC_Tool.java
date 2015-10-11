package plotcraft.editor.tools;

import plotcraft.editor.PC_Editor;
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

	public PC_Tool(PC_Editor controller) {
		_controller = controller;
	}

	public final ArrayList<PC_EditedTile> getEditedTiles() {
		if (_isMousePressed)
			return _edits;

		return null;
	}

	abstract public String getToolName();
	abstract public void setupToolOptions(JPanel optionsPanel);

	abstract protected void handleMouseDown();
	abstract protected void handleMouseUp();
	abstract protected void handleMouseDrag();

	public final void onMouseDown(int x, int y) {
		_edits = new ArrayList<>();
		_mouseDownX = _mouseCurrentX = x;
		_mouseDownY = _mouseCurrentY = y;
		_isMousePressed = true;

		handleMouseDown();
	}

	public final ArrayList<PC_EditedTile> onMouseUp(int x, int y) {
		if (!_isMousePressed) {
			return null;
		}

		_isMousePressed = false;
		handleMouseUp();

		return _edits;
	}

	public final void onMouseDrag(int x, int y) {
		if (_isMousePressed) {
			_mouseCurrentX = x;
			_mouseCurrentY = y;

			handleMouseDrag();
		}
	}
}
