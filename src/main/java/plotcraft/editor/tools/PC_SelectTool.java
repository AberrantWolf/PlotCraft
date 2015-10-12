package plotcraft.editor.tools;

import plotcraft.editor.PC_Editor;
import plotcraft.editor.PC_EditorModel;

import javax.swing.*;

/**
 * Tool for selecting, dropping, and copying sections
 * Created by harper on 10/12/2015.
 */
public class PC_SelectTool extends PC_Tool {
	private boolean _hasSelection;
	private boolean _isDragging;

	public PC_SelectTool(PC_Editor controller, PC_EditorModel model) {
		super(controller, model);
		_hasSelection = false;
		_isDragging = false;
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
		if (_hasSelection) {
			boolean inSelection = false;
			for (PC_EditedTile edit : _edits) {
				if (_mouseCurrentX == edit.x && _mouseCurrentY == edit.y) {
					inSelection = true;
				}
			}

			if (!inSelection) {
				commitEdits();
			} else {

			}
		}
	}

	@Override
	protected void handleMouseDrag() {

	}
}
