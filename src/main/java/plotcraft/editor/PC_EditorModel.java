package plotcraft.editor;

import javax.json.*;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * PC_Editor.PC_EditorModel
 *
 *
 *
 * Created by harper on 10/5/2015.
 */
public class PC_EditorModel {
	//==========================================================//
	// Interfaces for handling lambda expressions for each tile //
	public interface EachTileHandler {
		void HandleTile(Tile tile, int x, int y, int depth);
	}
	public interface EachTileInLayerHandler {
		void HandleTile(Tile tile, int x, int y);
	}

	public void forEachTile(EachTileHandler ref) {
		for (int depth = 0; depth < _depth; depth++) {
			ArrayList<ArrayList<Tile>> layer = _tiles.get(depth);
			for (int y = 0; y < _height; y++) {
				ArrayList<Tile> row = layer.get(y);
				for (int x = 0; x < _width; x++) {
					Tile tile = row.get(x);
					ref.HandleTile(tile, x, y, depth);
				}
			}
		}
	}

	public void forEachTileInCurrentLayer(EachTileInLayerHandler ref) {
		ArrayList<ArrayList<Tile>> layer = _tiles.get(_currentLayer);
		for (int y = 0; y < _height; y++) {
			ArrayList<Tile> row = layer.get(y);
			for (int x = 0; x < _width; x++) {
				Tile tile = row.get(x);
				ref.HandleTile(tile, x, y);
			}
		}
	}
	//==========================================================//

	public EditorTool get_currentTool() {
		return _currentTool;
	}

	public void setCurrentTool(EditorTool _currentTool) {
		this._currentTool = _currentTool;
	}

	public int getWidth() {
		return _width;
	}
	public void setWidth(int w) {
		if (w < 1) {
			return;
		}

		if (w == _width) {
			return;
		}

		for (ArrayList<ArrayList<Tile>> depth_slice : _tiles) {
			for (ArrayList<Tile> row : depth_slice) {
				while (w > row.size()) {
					row.add(new Tile());
				}
				while (w < row.size()) {
					row.remove(row.size() - 1);
				}
			}
		}

		_width = w;

		assert TilesIsNotEmpty();
	}

	public int getHeight() {
		return _height;
	}
	public void setHeight(int h) {
		if (h < 1) {
			return;
		}

		if (h == _height) {
			return;
		}

		for (ArrayList<ArrayList<Tile>> depth_slice : _tiles) {
			while (h > depth_slice.size()) {
				ArrayList<Tile> row = new ArrayList<>();
				while (_width > row.size()) {
					row.add(new Tile());
				}
				depth_slice.add(row);
			}
			while (h < depth_slice.size()) {
				depth_slice.remove(depth_slice.size()-1);
			}
		}

		_height = h;

		assert TilesIsNotEmpty();
	}

	public int getDepth() {
		return _depth;
	}
	public void setDepth(int d) {
		if (d < 1) {
			return;
		}

		if (d == _depth) {
			return;
		}

		while (d > _tiles.size()) {
			ArrayList<ArrayList<Tile>> depth_slice = new ArrayList<>();
			while (_height > depth_slice.size()) {
				ArrayList<Tile> row = new ArrayList<>();
				while (_width > row.size()) {
					row.add(new Tile());
				}
				depth_slice.add(row);
			}
			_tiles.add(depth_slice);
		}

		while (d < _tiles.size()) {
			_tiles.remove(_tiles.size() - 1);
		}

		_depth = d;

		assert TilesIsNotEmpty();
	}

	private boolean TilesIsNotEmpty() {
		forEachTile((Tile tile, int x, int y, int depth) -> {
			assert tile != null;
		});

		return true;
	}

	public int getCurrentLayer() {
		return _currentLayer;
	}

	public void setCurrentLayer(int l) {
		if (l >= _depth || l < 0) {
			System.out.println(String.format("ERROR: Trying to set an invalid layer: %d", l));
			return;
		}
		_currentLayer = l;
	}

	public ArrayList<ArrayList<ArrayList<Tile>>> getTiles() {
		return _tiles;
	}
	public ArrayList<ArrayList<Tile>> getTiles(int layer) {
		return _tiles.get(layer);
	}

	public PC_Tile getTile(int x, int y) { return getTile(_currentLayer, x, y);	}
	public PC_Tile getTile(int layer, int x, int y) {
		if (layer < 0 || x < 0 || y < 0 ||
				    layer > _depth || x > _width || y > _height) {
			return null;
		}

		ArrayList<ArrayList<Tile>> t_layer = _tiles.get(layer);
		ArrayList<Tile> row = t_layer.get(y);
		return row.get(x).data;
	}

	public void setTile(int x, int y, PC_Tile tile) { setTile(_currentLayer, x, y, tile); }
	public void setTile(int layer, int x, int y, PC_Tile tile) {
		if (layer < 0 || x < 0 || y < 0 ||
				    layer > _depth || x > _width || y > _height) {
			System.out.println("ERROR: Trying to set a tile outside the bounds of the data set");
			return;
		}

		ArrayList<ArrayList<Tile>> t_layer = _tiles.get(layer);
		ArrayList<Tile> row = t_layer.get(y);
		row.get(x).data = tile;
	}

	public String getFilePath() {
		return _filePath;
	}
	public void setFilePath(String path) {
		_filePath = path;
	}

	public String getFileName() {
		if (_filePath == null) {
			return "[new file]";
		}

		Path p = Paths.get(_filePath);
		return p.getFileName().toString();
	}

	public String serializeToJson() {
		JsonObjectBuilder rootBuilder = Json.createObjectBuilder();

		JsonArrayBuilder dataBuilder = Json.createArrayBuilder();
		for (ArrayList<ArrayList<Tile>> layer : _tiles) {
			JsonArrayBuilder layerBuilder = Json.createArrayBuilder();

			for (ArrayList<Tile> row : layer) {
				JsonArrayBuilder rowBuilder = Json.createArrayBuilder();

				for (Tile tile : row) {
					//rowBuilder.add(tile.data.id);
					rowBuilder.add(String.format("%d:%d", tile.data.id, tile.data.dv));
				}
				layerBuilder.add(rowBuilder);
			}
			dataBuilder.add(layerBuilder);
		}
		rootBuilder.add("data", dataBuilder);

		JsonObject root = rootBuilder.build();
		StringWriter stringWriter = new StringWriter();
		JsonWriter writer = Json.createWriter(stringWriter);
		writer.write(root);

		return stringWriter.toString();
	}

	public boolean deserializeFromJson(String json) {
		JsonReader reader = Json.createReader(new StringReader(json));

		JsonStructure structure = reader.read();
		if (structure.getValueType() != JsonValue.ValueType.OBJECT) {
			System.out.println("ERROR: JSON data does not contain a JSON object structure as its root");
			return false;
		}
		JsonObject root = (JsonObject)structure;
		reader.close();

		// load other data it
		// author?
		// creation date?
		// editing state (current layer?)

		// Load tile data
		if (root.containsKey("data")) {
			if (root.get("data").getValueType() != JsonValue.ValueType.ARRAY) {
				System.out.println("ERROR: 'data' entry in file was not a JSON array");
				return false;
			}

			// reset
			_width = _height = _depth = 0;
			_tiles = new ArrayList<>();

			JsonArray data = root.getJsonArray("data");
			for (JsonValue layer_value : data) {
				if (layer_value.getValueType() != JsonValue.ValueType.ARRAY) {
					System.out.println("ERROR: layer array entry was not another JSON array");
					return false;
				}

				ArrayList<ArrayList<Tile>> t_layer = new ArrayList<>();
				JsonArray layer = (JsonArray)layer_value;
				for (JsonValue row_value : layer) {
					if (row_value.getValueType() != JsonValue.ValueType.ARRAY) {
						System.out.println("ERROR: row array entry was not another JSON array");
						return false;
					}

					ArrayList<Tile> t_row = new ArrayList<>();
					JsonArray row = (JsonArray)row_value;
					for (JsonValue tile_value : row) {
						int tile_id = 0, tile_dv = 0;
						if (tile_value.getValueType() == JsonValue.ValueType.STRING) {
							JsonString tile_info = (JsonString) tile_value;
							String[] parts = tile_info.getString().split(":");
							if (parts.length < 1) {
								System.out.println("ERROR: tile entry string has no data");
								return false;
							}

							try {
								tile_id = Integer.parseInt(parts[0]);

								if (parts.length > 1) {
									tile_dv = Integer.parseInt(parts[1]);
								}
							} catch (NumberFormatException e) {
								System.out.println(String.format("ERROR: \"%s\" could not be parsed as an integer", tile_info.getString()));
								e.printStackTrace();
							}
						} else if (tile_value.getValueType() == JsonValue.ValueType.NUMBER) {
							tile_id = ((JsonNumber) tile_value).intValue();

						} else {
							System.out.println("ERROR: tile array entry was not a JSON number or string");
						}

						Tile t = new Tile();
						t.data = PC_Config.getTile(tile_id, tile_dv);
						t_row.add(t);
					}
					// CHECK
					if (_width == 0) {
						_width = t_row.size();
					}
					if (_width != t_row.size()) {
						System.out.println("ERROR: JSON row does not have the same number of entries as first row");
						return false;
					}
					// ADD
					t_layer.add(t_row);
				}
				// CHECK
				if (_height == 0) {
					_height = t_layer.size();
				}
				if (_height != t_layer.size()) {
					System.out.println("ERROR: JSON layer does not have the same number of rows as the first layer");
					return false;
				}
				// ADD
				_tiles.add(t_layer);
			}
			// set depth to whatever it is
			_depth = _tiles.size();

			if (_depth < 1) {
				System.out.println("ERROR: no layers loaded from JSON");
				return false;
			}
		} else {
			System.out.println("ERROR: no 'data' entry found in JSON data");
			return false;
		}

		return true;
	}

	// MEMBERS
	public enum EditorTool {
		ET_Pencil,
		ET_Line,
		ET_Rect,
		ET_Ellipse,
		ET_Select,
		ET_Erase
	}
	private EditorTool _currentTool;

	// Lightweight container class -- all we should have in memory is a ton of pointers, which should be pretty small
	class Tile {
		PC_Tile data;

		Tile() {
			data = PC_Config.getDefaultTile();
		}
	}
	private ArrayList<ArrayList<ArrayList<Tile>>> _tiles;   //3D array [shudder]

	private int _width, _height, _depth;
	private int _currentLayer;

	private String _filePath;

	// CONSTRUCTOR
	public PC_EditorModel() {
		_currentTool = EditorTool.ET_Pencil;

		reset();
	}

	public void reset() {
		_depth = 1;
		_width = 8;
		_height = 8;

		_filePath = null;

		// build out the initial array of elements
		_tiles = new ArrayList<>(_depth);
		for (int z=0; z<_depth; z++)  {
			ArrayList<ArrayList<Tile>> layer = new ArrayList<>(_height);
			_tiles.add(layer);
			for (int y=0; y<_height; y++) {
				ArrayList<Tile> row = new ArrayList<>(_width);
				layer.add(row);
				for (int x=0; x<_width; x++) {
					row.add(new Tile());
				}
			}
		}
		// END FOR EACH TILE
	}
}
