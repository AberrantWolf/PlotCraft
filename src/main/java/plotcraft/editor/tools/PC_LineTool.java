package plotcraft.editor.tools;

import plotcraft.editor.PC_Editor;

import javax.swing.*;

/**
 * Tool for drawing lines, based on Bresenham's like drawing algorithm
 *
 * Created by harper on 10/11/2015.
 */
public class PC_LineTool extends PC_Tool {
	public PC_LineTool(PC_Editor controller) {
		super(controller);
	}

	// Cheers to Mr. Bresenham ;)
	// Adapted from: http://rosettacode.org/wiki/Bitmap/Bresenham's_line_algorithm#Java
	private void drawLine(int x1, int y1, int x2, int y2) {
		// delta of exact value and rounded value of the dependant variable
		int d = 0;

		int dy = Math.abs(y2 - y1);
		int dx = Math.abs(x2 - x1);

		int dy2 = (dy << 1); // slope scaling factors to avoid floating
		int dx2 = (dx << 1); // point

		int ix = x1 < x2 ? 1 : -1; // increment direction
		int iy = y1 < y2 ? 1 : -1;

		if (dy <= dx) {
			for (;;) {
				_edits.add(new PC_EditedTile(x1, y1, _controller.getSelectedTile()));
				if (x1 == x2)
					break;
				x1 += ix;
				d += dy2;
				if (d > dx) {
					y1 += iy;
					d -= dx2;
				}
			}
		} else {
			for (;;) {
				_edits.add(new PC_EditedTile(x1, y1, _controller.getSelectedTile()));
				if (y1 == y2)
					break;
				y1 += iy;
				d += dx2;
				if (d > dy) {
					x1 += ix;
					d -= dy2;
				}
			}
		}
	}

	private int _lastX, _lastY;

	@Override
	public String getToolName() {
		return "Line";
	}

	@Override
	public void setupToolOptions(JPanel optionsPanel) {
		// TODO: setup tool options for line drawing
	}

	@Override
	protected void handleMouseDown() {
		_edits.clear();

		_lastX = _mouseDownX;
		_lastY = _mouseDownY;

		_edits.add(new PC_EditedTile(_mouseDownX, _mouseDownY, _controller.getSelectedTile()));
	}

	@Override
	protected void handleMouseUp() {

	}

	@Override
	protected void handleMouseDrag() {
		if (_mouseCurrentX != _lastX || _mouseCurrentY != _lastY) {
			_edits.clear();

			drawLine(_mouseDownX, _mouseDownY, _mouseCurrentX, _mouseCurrentY);

			_lastX = _mouseCurrentX;
			_lastY = _mouseCurrentY;
		}
	}
}
