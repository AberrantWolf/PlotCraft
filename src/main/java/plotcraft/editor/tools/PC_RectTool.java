package plotcraft.editor.tools;

import plotcraft.editor.PC_Editor;
import plotcraft.editor.PC_EditorModel;

import javax.swing.*;

/**
 * Tool for dragging out rectangles of whatever block
 *
 * Created by harper on 10/11/2015.
 */
public class PC_RectTool extends PC_Tool {

	private int _lastX, _lastY;
	private boolean _shouldFill;

	public PC_RectTool(PC_Editor controller, PC_EditorModel model) {
		super(controller, model);
	}

	@Override
	public String getToolName() {
		return "Rectangle";
	}

	@Override
	public void setupToolOptions(JPanel optionsPanel) {
		// Filled or not filled check box
		JCheckBox fillBox = new JCheckBox("Draw filled", false);
		fillBox.addActionListener(actionEvent -> _shouldFill = fillBox.isSelected());

		optionsPanel.add(fillBox);
		optionsPanel.revalidate();
	}

	@Override
	protected void handleMouseDown() {
		_edits.clear();

		_lastX = _mouseDownX;
		_lastY = _mouseDownY;

		_edits.add(new PC_EditedTile(_mouseDownX, _mouseDownY, _controller.getSelectedTile()));
	}

	@Override
	protected void handleMouseDrag() {
		if (_mouseCurrentX != _lastX || _mouseCurrentY != _lastY) {
			_edits.clear();

			int left = Math.min(_mouseDownX, _mouseCurrentX);
			int right = Math.max(_mouseDownX, _mouseCurrentX);
			int top = Math.min(_mouseDownY, _mouseCurrentY);
			int bottom = Math.max(_mouseDownY, _mouseCurrentY);

			for (int y=top; y <= bottom; y++) {
				for (int x=left; x<=right; x++) {
					if (_shouldFill || y == top || y == bottom || x == left || x == right) {
						_edits.add(new PC_EditedTile(x, y, _controller.getSelectedTile()));
					}
				}
			}

			_lastX = _mouseCurrentX;
			_lastY = _mouseCurrentY;
		}
	}
}
