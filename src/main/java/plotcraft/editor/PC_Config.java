package plotcraft.editor;

import javax.imageio.ImageIO;
import javax.json.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageFilter;
import java.awt.image.RGBImageFilter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Static configuration class loads up settings and image templates
 *
 * Created by harper on 10/7/2015.
 */
public class PC_Config {
	private static PC_Config ourInstance = new PC_Config();
	
	public static PC_Config getInstance() {
		return ourInstance;
	}

	private boolean _hasValidData;
	private JsonObject _settings;

	private ArrayList<PC_Tile> _tileTemplates;
	private int _tileCacheSize = -1;

	public static PC_Tile getRandomTile() {
		int sel = (int)(Math.random() * (ourInstance._tileTemplates.size() - 1) + 1);
		return getInstance()._tileTemplates.get(sel);
	}
	public static PC_Tile getDefaultTile() {
		return getInstance()._tileTemplates.get(0);
	}
	public static PC_Tile getTile(int id, int dv) {
		PC_Tile result = null;
		for (PC_Tile t : getInstance()._tileTemplates) {
			if (t.id == id) {
				if (t.dv == dv) {
					return t;
				}
				if (result == null) {
					result = t;
				}
			}
		}

		if (result != null) {
			return result;
		}

		PC_Tile mismatch = new PC_Tile();
		mismatch.id = id;
		mismatch.image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2 = mismatch.image.createGraphics();
		g2.setColor(Color.RED);
		g2.fillRect(0, 0, 16, 16);
		g2.setColor(new Color(127, 31, 0));
		g2.setFont(new Font(g2.getFont().getFontName(), Font.PLAIN, 8));
		g2.drawString("" + id, 1, 15);
		g2.dispose();

		mismatch.cachedImage = null;

		getInstance()._tileTemplates.add(mismatch);
		return mismatch;
	}

	public int getPreviewSize() {
		return 32;
	}

	public ArrayList<PC_Tile> getAllTiles() {
		return _tileTemplates;
	}

	public void cacheImages(int size, GraphicsConfiguration gc) {
		boolean needsResize = false;
		if (size != _tileCacheSize) {
			needsResize = true;
		}

		_tileCacheSize = size;

		for (PC_Tile tile : _tileTemplates) {
			if (tile.image == null) {
				continue;
			}

			if (tile.cachedImage == null || needsResize) {
				BufferedImage cache = resizeImage(tile.image, size, size, gc);

				applyBiomeColor(cache, tile.biomeColor);

				tile.cachedImage = cache;
			}
		}
	}

	private static GraphicsConfiguration _gc;
	private BufferedImage resizeImage(BufferedImage src, int w, int h, GraphicsConfiguration gcIn) {
		if (src == null) {
			return null;
		}

		if (gcIn != null && _gc != gcIn) {
			_gc = gcIn;
		}

		if (_gc == null) {
			// just grab the default graphics configuration
			_gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
		}

		BufferedImage result = _gc.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
		Graphics2D g2 = result.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

		g2.setComposite(AlphaComposite.Src);

		g2.drawImage(src, 0, 0, w, h, 0, 0, src.getWidth(), src.getHeight(), null);
		g2.dispose();

		return result;
	}

	private void applyBiomeColor(BufferedImage cache, BiomeColor bc) {
		Color b;

		switch (bc) {
			case Grass:
				b = getBiomeGrassColor();
				break;
			case Leaf:
				b = getBiomeLeafColor();
				break;
			default:
				return;
		}

		for (int y = 0; y < cache.getHeight(); y++) {
			for (int x = 0; x < cache.getWidth(); x++) {
				int argb = cache.getRGB(x, y);
				Color c = new Color(argb, true);

				float fa = (c.getAlpha() / 255f) * (b.getAlpha() / 255f);
				float fr = (c.getRed() / 255f) * (b.getRed() / 255f);
				float fg = (c.getGreen() / 255f) * (b.getGreen() / 255f);
				float fb = (c.getBlue() / 255f) * (b.getBlue() / 255f);

				int f = (int) (fa * 255) << 24
						        | (int)(fr * 255) << 16
						        | (int) (fg * 255) << 8
						        | (int) (fb * 255);

				cache.setRGB(x, y, f);
			}
		}
	}

	private PC_Config() {
		try {
			System.out.println("Working Directory = " +
					                   System.getProperty("user.dir"));
			FileReader fileReader = new FileReader("settings.json");
			JsonReader reader = Json.createReader(fileReader);
			JsonStructure jsonStructure = reader.read();

			if (jsonStructure.getValueType() == JsonValue.ValueType.OBJECT) {
				_settings = (JsonObject) jsonStructure;
			}

			if (_settings.containsKey("palette"))
				_hasValidData = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		if (_hasValidData) {
			CreateTiles();
		}
	}

	private Color getBiomeGrassColor() {
		return Color.GREEN;
	}
	private Color getBiomeLeafColor() {
		return Color.GREEN;
	}


	private boolean checkForTransparent(BufferedImage img) {
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				int argb = img.getRGB(x, y);
				if ((argb & 0xFF000000) == 0) {
					System.out.println("FOUND TRANSPARENT!");
					return true;
				}
			}
		}

		return false;
	}


	private void CreateTiles() {
		_tileTemplates = new ArrayList<>();

		JsonArray palette = _settings.getJsonArray("palette");
		for (int idx=0; idx<palette.size(); idx++) {
			JsonObject entry = palette.getJsonObject(idx);

			if (entry.getBoolean("skip", false)) {
				continue;
			}

			PC_Tile tile = new PC_Tile();
			tile.id = entry.getInt("id");
			tile.dv = entry.getInt("data_value", 0);

			tile.name = entry.getString("name", "<default>");
			tile.isTransparent = entry.getBoolean("transparent", false);

			// Biome color tinting
			if (entry.getBoolean("use_biome_grass", false)) {
				tile.biomeColor = BiomeColor.Grass;
			} else if (entry.getBoolean("use_biome_leaf", false)) {
				tile.biomeColor = BiomeColor.Leaf;
			} else {
				tile.biomeColor = BiomeColor.None;
			}

			// Check for valid data value range
			if (tile.dv > 15) {
				System.out.println(String.format("WARNING: tile (%s: %d) has invalid data_value %d", tile.name, tile.id, tile.dv));
			}

			String path = null;
			if (!entry.isNull("texture")) {
				path = entry.getString("texture");
			}

			// Load the tile images
			if (path != null) {
				BufferedImage img = null;
				try {
					img = ImageIO.read(new File("textures/blocks/" + path));

					// load tiled images (needed for the animated textures like water and lava)
					int tile_idx = entry.getInt("tile_idx", -1);
					if (tile_idx > -1) {
						int tile_size = entry.getInt("tile_size", 16);
						BufferedImage tile_img = new BufferedImage(tile_size, tile_size, img.getType());
						Graphics2D g2 = tile_img.createGraphics();
						g2.setComposite(AlphaComposite.Src);
						g2.drawImage(img, 0, 0, null);
						g2.dispose();

						img = tile_img;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (img == null) {
					System.out.println("Couldn't load image: " + path);
				}

				tile.image = img;
				tile.toolPreview = resizeImage(tile.image, getPreviewSize(), getPreviewSize(), null);

				if (checkForTransparent(tile.image) != checkForTransparent(tile.toolPreview)) {
					System.out.println("ERROR: lost transparency");
				}

				applyBiomeColor(tile.toolPreview, tile.biomeColor);
			}

			_tileTemplates.add(tile);
		}
	}
}
