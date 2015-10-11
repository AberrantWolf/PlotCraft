package plotcraft.editor;

import javax.imageio.ImageIO;
import javax.json.*;
import java.awt.*;
import java.awt.image.BufferedImage;
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
	public static PC_Tile getTile(int id) {
		for (PC_Tile t : getInstance()._tileTemplates) {
			if (t.id == id) {
				return t;
			}
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
				BufferedImage cache = gc.createCompatibleImage(size, size);
				Graphics2D g2 = cache.createGraphics();
				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
				g2.drawImage(tile.image, 0, 0, size, size, 0, 0, tile.image.getWidth(), tile.image.getHeight(), null);
				g2.dispose();

				if (tile.usesBiomeGrass) {
					applyBiomeColor(cache);
				}

				tile.cachedImage = cache;
			}
		}
	}

	private BufferedImage resizeImage(BufferedImage src, int w, int h) {
		if (src == null) {
			return null;
		}

		BufferedImage result = new BufferedImage(w, h, src.getType());
		Graphics2D g2 = result.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g2.drawImage(src, 0, 0, w, h, 0, 0, src.getWidth(), src.getHeight(), null);
		g2.dispose();

		return result;
	}

	private void applyBiomeColor(BufferedImage cache) {
		Color b = getBiomeGrassColor();
		for (int y = 0; y < cache.getHeight(); y++) {
			for (int x = 0; x < cache.getWidth(); x++) {
				Color c = new Color(cache.getRGB(x, y));

				float fr = (c.getRed() / 255f) * (b.getRed() / 255f);
				float fg = (c.getGreen() / 255f) * (b.getGreen() / 255f);
				float fb = (c.getBlue() / 255f) * (b.getBlue() / 255f);

				int f = (int) (fr * 255) << 16
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

			String path = null;
			if (!entry.isNull("texture")) {
				path = entry.getString("texture");
			}

			if (path != null) {
				BufferedImage img = null;
				try {
					img = ImageIO.read(new File("textures/blocks/" + path));

					int tile_idx = entry.getInt("tile_idx", -1);
					if (tile_idx > -1) {
						int tile_size = entry.getInt("tile_size", 16);
						BufferedImage tile_img = new BufferedImage(tile_size, tile_size, img.getType());
						Graphics2D g2 = tile_img.createGraphics();
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
				tile.toolPreview = resizeImage(tile.image, getPreviewSize(), getPreviewSize());
			}

			tile.name = entry.getString("name", "<default>");
			tile.usesBiomeGrass = entry.getBoolean("use_biome_grass", false);

			_tileTemplates.add(tile);
		}
	}
}
