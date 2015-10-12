package plotcraft.editor.tools;

import plotcraft.editor.PC_Editor;
import plotcraft.editor.PC_EditorModel;
import plotcraft.editor.PC_Tile;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Basic drawing brush
 *
 * Created by harper on 10/9/2015.
 */
public class PC_BrushTool extends PC_Tool {
	private static int s_brushSize = 1;

	public PC_BrushTool(PC_Editor controller, PC_EditorModel model) {
		super(controller, model);
	}

	@Override
	public String getToolName() {
		return "Brush";
	}

	@Override
	public void setupToolOptions(JPanel optionsPanel) {
		JPanel sizePanel = new JPanel();

		JSpinner sizeSpinner = new JSpinner();
		sizeSpinner.setModel(new SpinnerNumberModel(s_brushSize, 1, null, 1));
		sizeSpinner.addChangeListener(changeEvent -> s_brushSize = (Integer)sizeSpinner.getValue());

		sizePanel.setLayout(new GridLayout(0, 2));
		sizePanel.add(new JLabel("Size"));
		sizePanel.add(sizeSpinner);

		optionsPanel.add(sizePanel);
		optionsPanel.revalidate();
	}

	@Override
	protected void handleMouseDown() {
		_edits.add(new PC_EditedTile(_mouseDownX, _mouseDownY, _controller.getSelectedTile()));
	}

	@Override
	protected void handleMouseDrag() {
		boolean exists = false;
		for(PC_EditedTile tile : _edits) {
			if (tile.x == _mouseCurrentX && tile.y == _mouseCurrentY) {
				exists = true;
				break;
			}
		}

		if (!exists) {
			_edits.add(new PC_EditedTile(_mouseCurrentX, _mouseCurrentY, _controller.getSelectedTile()));
		}
	}
}
