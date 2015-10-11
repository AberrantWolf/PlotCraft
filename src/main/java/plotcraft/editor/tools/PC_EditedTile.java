package plotcraft.editor.tools;

import plotcraft.editor.PC_Tile;

/**
 * Created by harper on 10/9/2015.
 */
public class PC_EditedTile {
	public int x;
	public int y;
	public PC_Tile data;

	public PC_EditedTile(int x, int y, PC_Tile data) {
		this.x = x;
		this.y = y;
		this.data = data;
	}
}
