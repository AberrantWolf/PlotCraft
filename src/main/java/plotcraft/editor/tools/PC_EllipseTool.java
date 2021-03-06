package plotcraft.editor.tools;

import plotcraft.editor.PC_Editor;
import plotcraft.editor.PC_EditorModel;

import javax.swing.*;
import java.util.HashMap;

/**
 * Tool for drawing raster ellipses contained within a rectangle
 *
 * Based on the McIlroy algorithm and modified to fix it for various edge cases and odd-length dimensions
 *
 * Created by harper on 10/11/2015.
 */
public class PC_EllipseTool extends PC_Tool {

	private boolean _shouldFill = false;

	public PC_EllipseTool(PC_Editor controller, PC_EditorModel model) {
		super(controller, model);
	}

	private class _MinMaxPair {int min=Integer.MAX_VALUE, max=Integer.MIN_VALUE;}
	private HashMap<Integer, _MinMaxPair> _rows = new HashMap<>();
	private void drawPoint(int x, int y) {
		addEditedTile(new PC_EditedTile(x, y, _controller.getSelectedTile()));

		if (_shouldFill) {
			_MinMaxPair pair = _rows.getOrDefault(y, new _MinMaxPair());
			if (x < pair.min) pair.min = x;
			if (x > pair.max) pair.max = x;
			_rows.putIfAbsent(y, pair);
		}
	}

	private void finishEllipse() {
		// Add in the missing rows, if any were queued to be added
		_rows.forEach((y, pair) -> {
			_edits.add(new PC_EditedTile(pair.min, y, _controller.getSelectedTile()));
			_edits.add(new PC_EditedTile(pair.max, y, _controller.getSelectedTile()));

			for (int x = pair.min; x<=pair.max; x++) {
				addEditedTile(new PC_EditedTile(x, y, _controller.getSelectedTile()));
			}
		});
		_rows.clear();
	}

	// Heavily modified McIlroy's algorithm to account for odd width/height ellipses and the 2-by-n edge case
	// Retrieved from: http://editor.altervista.org/graphical%20effects/shapes%20tracking/draw-ellipse.html
	void drawEllipse(int xc, int yc, int a, int b, boolean exW, boolean exH) {
		/* e(x,y) = b^2*x^2 + a^2*y^2 - a^2*b^2 */

		if (exW) {
			xc++;
			a++;
		}
		if (exH) {
			yc++;
			b++;
		}

		int x = 0, y = b;
		long a2 = (long)a*a, b2 = (long)b*b;
		long crit1 = -(a2/4 + a%2 + b2);
		long crit2 = -(b2/4 + b%2 + a2);
		long crit3 = -(b2/4 + b%2);
		long t = -a2*y; /* e(x+1/2,y-1/2) - (a^2+b^2)/4 */
		long dxt = 2*b2*x, dyt = -2*a2*y;
		long d2xt = 2*b2, d2yt = 2*a2;

		int xp, yp; // x and y when positive
		int xn, yn; // x and y when negative
		while (y>=0 && x<=a) {
			xp = xc + x + (exW ? -1 : 0);
			yp = yc + y + (exH ? -1 : 0);
			xn = xc - x;
			yn = yc - y;

			drawPoint(xp, yp);

			// Edge case for 2-by-n ellipses
			if (x==0 && y!=0 && exW) {
				drawPoint(xc - 1, yn);
				drawPoint(xc, yp);
			}
			// Edge case for n-by-2 ellipses
			if (y==0 && x!=0 && exH) {
				drawPoint(xn, yc - 1);
				drawPoint(xp, yc);
			}

			if (x!=0 || y!=0) {
				drawPoint(xc - x, yc - y);
			}
			if (x!=0 && y!=0) {
				drawPoint(xp, yc - y);
				drawPoint(xc - x, yp);
			}

			// Increment
			if (t + b2*x <= crit1 ||   /* e(x+1,y-1/2) <= 0 */
					    t + a2*y <= crit3) {     /* e(x+1/2,y) <= 0 */
				x++;    //incx
				dxt += d2xt;
				t += dxt;
			}
			else if (t - a2*y > crit2) { /* e(x+1/2,y-1) > 0 */
				y--;    //incy
				dyt += d2yt;
				t += dyt;
			}
			else {
				x++;    //incx
				dxt += d2xt;
				t += dxt;
				y--;    //incy
				dyt += d2yt;
				t += dyt;
			}
		}

		finishEllipse();
	}

	private int _lastX, _lastY;

	@Override
	public String getToolName() {
		return "Ellipse";
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

			int xc, yc, width, height;
			width = right - left;
			height = bottom - top;
			xc = left + width / 2;
			yc = top + height / 2;

			boolean extraWidth = false, extraHeight = false;
			if (width % 2 > 0)
				extraWidth = true;
			if (height % 2 > 0)
				extraHeight = true;

			drawEllipse(xc, yc, width / 2, height / 2, extraWidth, extraHeight);

			_lastX = _mouseCurrentX;
			_lastY = _mouseCurrentY;
		}
	}
}
