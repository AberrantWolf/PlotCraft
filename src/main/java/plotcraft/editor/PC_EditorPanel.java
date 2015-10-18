package plotcraft.editor;

import plotcraft.editor.tools.PC_EditedTile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

/**
 * PC_Editor.PC_EditorPanel
 *
 * Panel for drawing the editor view and handling mouse input, etc...
 *
 * Created by harper on 10/5/2015.
 */
public class PC_EditorPanel extends JPanel implements Scrollable, MouseListener, MouseMotionListener {

	private PC_EditorModel _model;
	private PC_Editor _controller;

	private boolean _isMouseInside;
	private Point _mousePosition;

	public void SetDataModel(PC_EditorModel m) {
		_model = m;
	}

	public void SetController(PC_Editor ed) {
		_controller = ed;
	}

	public void updateSize() {
		int tileSize = _controller.getTileSize();
		int width = _model.getWidth();
		int height = _model.getHeight();

		Dimension preferred = new Dimension(width * tileSize + 1, height * tileSize + 1);
		setPreferredSize(preferred);
		revalidate();
	}

	public PC_EditorPanel() {
		super();

		_isMouseInside = false;
		_mousePosition = new Point();

		addMouseListener(this);
		addMouseMotionListener(this);

		System.out.println("Focus enbaled: " + isFocusable());
	}

	public Point getCurrentGridPoint() {
		int tileSize = _controller.getTileSize();
		int maxWidth = _model.getWidth() - 1;
		int maxHeight = _model.getHeight() - 1;

		return new Point(Math.min(_mousePosition.x / tileSize, maxWidth), Math.min(_mousePosition.y / tileSize, maxHeight));
	}

	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(Color.WHITE);
		g2.fill(g2.getClip());

		int tileSize = _controller.getTileSize();

		// Draw the tiles
		Rectangle clipBounds = g2.getClipBounds();
		_model.forEachTileInCurrentLayer((tile, x, y) -> {
			int xpos = x * tileSize;
			int ypos = y * tileSize;
			if (xpos > clipBounds.x + clipBounds.width
					|| ypos > clipBounds.y + clipBounds.height
					|| xpos + tileSize < clipBounds.x
					|| ypos + tileSize < clipBounds.y)
				return;

			PC_Tile t = tile.data;
			int depth = 0;
			if (t == PC_Config.getDefaultTile()) {
				int workingLayer = _model.getCurrentLayer();

				while (workingLayer > -1 && t == PC_Config.getDefaultTile()) {
					workingLayer--;
					t = _model.getTile(workingLayer, x, y);
				}

				if (t != PC_Config.getDefaultTile()) {
					depth = _model.getCurrentLayer() - workingLayer;
				}
			}

			if (t != null && t.image != null) {
				if (t.cachedImage == null) {
					PC_Config.getInstance().cacheImages(tileSize, getGraphicsConfiguration());
				}
				g2.drawImage(t.cachedImage, xpos, ypos, null);

				depth = Math.min(depth, 8);
				depth = Math.max(depth, 0);
				if (depth > 0) {
					float alpha = (float) Math.sin(depth * (Math.PI/16.0f)) * 0.8f + 0.1f;
					g2.setColor(new Color(1.0f, 1.0f, 1.0f, alpha));
					g2.fillRect(xpos, ypos, tileSize, tileSize);
				}
			}
		});

		// Draw tool edits
		ArrayList<PC_EditedTile> edits = _controller.getSelectedTool().getEditedTiles();
		if (edits != null) {
			g2.setStroke(new BasicStroke(2));
			// TODO: is there a more efficient way to do this layering without multiple duplicate checks?
			// TODO: is it safe enough to assume that the majority of tiles will be onscreen and so the checks for onscreen here are irrelevant?
			g2.setColor(Color.YELLOW);
			edits.forEach(eTile -> {
				int xpos = eTile.x * tileSize;
				int ypos = eTile.y * tileSize;
				if (xpos > clipBounds.x + clipBounds.width
						    || ypos > clipBounds.y + clipBounds.height
						    || xpos + tileSize < clipBounds.x
						    || ypos + tileSize < clipBounds.y) {
					return;
				}

				g2.drawRect(xpos-1, ypos-1, tileSize+2, tileSize+2);
			});

			g2.setColor(Color.WHITE);
			edits.forEach(eTile -> {
				int xpos = eTile.x * tileSize;
				int ypos = eTile.y * tileSize;
				if (xpos > clipBounds.x + clipBounds.width
						    || ypos > clipBounds.y + clipBounds.height
						    || xpos + tileSize < clipBounds.x
						    || ypos + tileSize < clipBounds.y) {
					return;
				}

				PC_Tile tile = eTile.data;
				if (tile.cachedImage != null) {
					g2.drawImage(tile.cachedImage, xpos, ypos, null);
				} else {
					g2.fillRect(xpos, ypos, tileSize, tileSize);
				}
			});
		}

		// Draw a grid for each point
		g2.setColor(Color.BLACK);
		g2.setStroke(new BasicStroke(1));
		int width = _model.getWidth();
		int height = _model.getHeight();
		for (int y = 0; y<height; y++) {
			//ArrayList<PC_Editor.PC_EditorModel.Tile> row = tiles.get(y);

			int pix_y = y * tileSize;
			g2.drawLine(0, pix_y, width * tileSize, pix_y);

			for (int x=0; x<width; x++) {
				int pix_x = x * tileSize;
				g2.drawLine(pix_x, 0, pix_x, height * tileSize);
			}
		}
		g2.drawLine(0, height * tileSize, width * tileSize, height * tileSize);
		g2.drawLine(width * tileSize, 0, width * tileSize, height * tileSize);

		// Highlight the tile under the mouse cursor
		if (_isMouseInside) {
			g2.setStroke(new BasicStroke(2));
			Point gridPt = getCurrentGridPoint();
			g2.setColor(new Color(95, 190, 255));
			g2.drawRect(gridPt.x * tileSize, gridPt.y * tileSize, tileSize+1, tileSize+1);
		}
	}

	// SCROLLABLE Interface
	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle rectangle, int i, int i1) {
		return 1;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return false;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle rectangle, int i, int i1) {
		return 32;
	}

	// MOUSE LISTENER Interface
	@Override
	public void mouseClicked(MouseEvent mouseEvent) {
		// ignore mouse click
	}

	@Override
	public void mousePressed(MouseEvent mouseEvent) {
		if (mouseEvent.getButton() == 1) {
			Point p = getCurrentGridPoint();
			_controller.getSelectedTool().onMouseDown(p.x, p.y);
		}

		grabFocus();

		repaint();
	}

	@Override
	public void mouseReleased(MouseEvent mouseEvent) {
		Point p = getCurrentGridPoint();

		_controller.getSelectedTool().onMouseUp(p.x, p.y);

		repaint();
	}

	// MOUSE MOTION LISTENER Interface
	@Override
	public void mouseEntered(MouseEvent mouseEvent) {
		_isMouseInside = true;
	}

	@Override
	public void mouseExited(MouseEvent mouseEvent) {
		_isMouseInside = false;
		_controller.updateStatusText();
		repaint();
	}

	@Override
	public void mouseDragged(MouseEvent mouseEvent) {
		if (_isMouseInside) {
			_mousePosition.setLocation(mouseEvent.getPoint());
			_controller.updateStatusText();

			Point p = getCurrentGridPoint();
			_controller.getSelectedTool().onMouseDrag(p.x, p.y);

			repaint();
		}
	}

	@Override
	public void mouseMoved(MouseEvent mouseEvent) {
		if (_isMouseInside) {
			_mousePosition.setLocation(mouseEvent.getPoint());
			_controller.updateStatusText();
			repaint();
		}
	}
}
