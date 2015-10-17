package plotcraft.editor.tools;

import plotcraft.editor.PC_Config;
import plotcraft.editor.PC_Editor;
import plotcraft.editor.PC_EditorModel;
import plotcraft.editor.PC_Tile;

import javax.swing.*;
import java.util.ArrayList;

/**
 * Tool for selecting, dropping, and copying sections
 * Created by harper on 10/12/2015.
 */
public class PC_SelectTool extends PC_Tool {
	private boolean _hasSelection;
	private boolean _isDraggingSelection;
	private int _mousePrevX, _mousePrevY;

	public PC_SelectTool(PC_Editor controller, PC_EditorModel model) {
		super(controller, model);
		_hasSelection = false;
		_isDraggingSelection = false;
	}

	public void injectSelection(ArrayList<PC_EditedTile> tiles) {
		if (_hasSelection) {
			commitEdits();
		}

		tiles.forEach((tile) -> addEditedTile(new PC_EditedTile(tile)));

		if (_edits.size() > 0) {
			_hasSelection = true;
		}
	}

	@Override
	public String getToolName() {
		return null;
	}

	@Override
	public void setupToolOptions(JPanel optionsPanel) {

	}

	@Override
	protected void handleMouseDown() {
		// TODO: look for shift pressed to toggle tiles selected
		if (_hasSelection) {
			boolean inSelection = false;
			for (PC_EditedTile edit : _edits) {
				if (_mouseCurrentX == edit.x && _mouseCurrentY == edit.y) {
					inSelection = true;
				}
			}

			if (inSelection) {
				_isDraggingSelection = true;
				_mousePrevX = _mouseCurrentX;
				_mousePrevY = _mouseCurrentY;
			} else {
				commitEdits();
				_hasSelection = false;
				_controller.setCanCopy(false);
			}
		}
	}

	@Override
	protected void handleMouseDrag() {
		if (_isDraggingSelection) {
			int dMouseX, dMouseY;
			dMouseX = _mouseCurrentX - _mousePrevX;
			dMouseY = _mouseCurrentY - _mousePrevY;

			if (dMouseX != 0 || dMouseY != 0) {
				_edits.forEach(tile -> {
					tile.x += dMouseX;
					tile.y += dMouseY;
				});
			}

			_mousePrevX = _mouseCurrentX;
			_mousePrevY = _mouseCurrentY;
			return;
		}

		// Add to selection
		_edits.clear();
		int minX = Math.min(_mouseDownX, _mouseCurrentX);
		int minY = Math.min(_mouseDownY, _mouseCurrentY);
		int maxX = Math.max(_mouseDownX, _mouseCurrentX);
		int maxY = Math.max(_mouseDownY, _mouseCurrentY);
		PC_Tile defTile = PC_Config.getDefaultTile();
		for (int y=minY; y<=maxY; y++) {
			for (int x=minX; x<=maxX; x++) {
				PC_Tile tile = _model.getTile(x, y);
				if (tile != defTile) {
					addEditedTile(new PC_EditedTile(x, y, tile));
				}
			}
		}
		_hasSelection = true;
	}

	@Override
	protected void handleMouseUp() {
		if (_isDraggingSelection) {
			_isDraggingSelection = false;
		} else {
			PC_Tile defTile = PC_Config.getDefaultTile();
			PC_Tile currentTile = _model.getTile(_mouseCurrentX, _mouseCurrentY);
			if (_edits.isEmpty() && currentTile != defTile) {
				addEditedTile(new PC_EditedTile(_mouseCurrentX, _mouseCurrentY, currentTile));
				_hasSelection = true;
			}

			_edits.forEach(tile -> {
				_model.setTile(tile.x, tile.y, defTile);
			});

			if (_edits.size() > 0) {
				_controller.setCanCopy(true);
			}
		}
	}
}
