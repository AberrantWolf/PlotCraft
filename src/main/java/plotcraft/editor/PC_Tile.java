package plotcraft.editor;

import java.awt.image.BufferedImage;

/**
 * Simple class to contain information about each tile so we don't end up duplicating all that memory.
 *
 * Created by harper on 10/7/2015.
 */
public class PC_Tile {
	public int id;
	public int dv;
	public String name;
	public BufferedImage image;
	public BufferedImage toolPreview;
	public BufferedImage cachedImage;
	boolean usesBiomeGrass;
}
