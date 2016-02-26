package lekro.rogueoid.map;

import java.awt.Point;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import lekro.rogueoid.RogueMath;
import lekro.rogueoid.entity.Entity;
import lekro.rogueoid.entity.Monster;
import lekro.rogueoid.entity.Player;

public class Level {

	public static final int DEFAULT_WIDTH = 63;
	public static final int DEFAULT_HEIGHT = 30;
	public static final int SECTOR_COUNT_X = 3;
	public static final int SECTOR_COUNT_Y = 3;
	
	public static final char EMPTY_SPACE = ' ';
	public static final char WALL_X = '-';
	public static final char WALL_Y = '|';
	public static final char CORNER_1 = '0';
	public static final char CORNER_2 = '0';
	public static final char FLOOR = '.';
	public static final char PATH_FLOOR = '#';
	public static final char DOOR = '+';
	public static final char MOB = 'M';
	public static final char PLAYER = 'O';
	
	public static final Set<Character> PASSABLE = new HashSet<Character>();
	
	static {
		PASSABLE.add(FLOOR);
		PASSABLE.add(PATH_FLOOR);
		PASSABLE.add(DOOR);
	}
	
	public static final Set<Character> TRANSPARENT = new HashSet<Character>();
	
	static {
		TRANSPARENT.add(FLOOR);
		TRANSPARENT.add(PATH_FLOOR);
		TRANSPARENT.add(DOOR);
	}
	
	public static final int ROOM_SKIP_CHANCE = 10;
	public static final int ROOM_SKIP_MAX = 2;
	
	private int height;
	private int width;
	private Room[][] rooms;
	private Set<Path> paths;
	private char[][] charMap;
	private boolean[][] fogOfWar;
	
	private Set<Entity> entities;
	
	public Level() {
		this(DEFAULT_HEIGHT, DEFAULT_WIDTH);
	}
	
	public Level(int height, int width) {
		this.height = height;
		this.width = width;
		
		Random random = new Random();
		rooms = new Room[SECTOR_COUNT_X][SECTOR_COUNT_Y];
		paths = new HashSet<Path>();
		
		int deleteQuota = ROOM_SKIP_MAX;
		
		// Generate (or not) the rooms
		
		for (int i = 0; i < rooms.length; i++) {
			for (int j = 0; j < rooms[i].length; j++) {
				if (deleteQuota > 0) {
					int deleteChance = random.nextInt(100);
					if (deleteChance < ROOM_SKIP_CHANCE) {
						deleteQuota--;
						rooms[i][j] = null;
						// We will skip some rooms
						continue;
					}
				}
				
				int sectorHeight = height / SECTOR_COUNT_Y;
				int sectorWidth = width / SECTOR_COUNT_X;
				int roomHeight = 0;
				int roomWidth = 0;
				while (true) {
					roomHeight = RogueMath.roll(sectorHeight / 2, sectorHeight);
					roomWidth = RogueMath.roll(sectorWidth / 2, sectorWidth);
					if (!(roomHeight < 5 || roomHeight >= sectorHeight - 2
							|| roomWidth < 5 || roomWidth >= sectorWidth - 2)) {
						break;
					}
				}
				int roomY = random.nextInt(sectorHeight - roomHeight) + j*sectorHeight;
				int roomX = random.nextInt(sectorWidth - roomWidth) + i*sectorWidth;
				rooms[i][j] = new Room(roomX, roomY, roomWidth, roomHeight);
			}
		}

		// Generate paths:
		// Paths in Y-direction:
		
		for (int i = 0; i < rooms.length; i++) {
			for (int j = 0; j < rooms[i].length - 1; j++) {
				if (rooms[i][j] != null && rooms[i][j+1] != null)
					paths.add(new Path(rooms[i][j], rooms[i][j+1]));
			}
		}
		
		// Paths in X-direction:
		
		for (int i = 0; i < rooms.length - 1; i++) {
			for (int j = 0; j < rooms[i].length; j++) {
				if (rooms[i][j] != null && rooms[i+1][j] != null)
					paths.add(new Path(rooms[i][j], rooms[i+1][j]));
			}
		}
		
		entities = new HashSet<Entity>();
		
		// We are placing exactly one monster in a room (for now) :
		
		for (Room[] rms : rooms) {
			for (Room r : rms) {
				if (r == null) continue;
				int x = random.nextInt(r.width-2) + r.x + 1;
				int y = random.nextInt(r.height-2) + r.y + 1;
				Monster m = new Monster(x, y, this);
				entities.add(m);
			}
		}
		
		charMap = new char[width][height];
		
		for (char[] boo : charMap) {
			Arrays.fill(boo, new Character(EMPTY_SPACE));
		}
		
		for (int i = 0; i < rooms.length; i++) {
			for (int j = 0; j < rooms[i].length; j++) {
				Room r = rooms[i][j];
				
				if (r == null) continue;
				
				for (int x = r.x; x < r.x+r.width; x++) {
					charMap[x][r.y] = WALL_X;
					charMap[x][r.y+r.height-1] = WALL_X;
				}
				for (int y = r.y; y < r.y+r.height; y++) {
					charMap[r.x][y] = WALL_Y;
					charMap[r.x+r.width-1][y] = WALL_Y;
				}
				
				charMap[r.x][r.y] = charMap[r.x+r.width-1][r.y+r.height-1] = CORNER_1;
				charMap[r.x][r.y+r.height-1] = charMap[r.x+r.width-1][r.y] = CORNER_2;
				
				for (int x = r.x + 1; x < r.x+r.width - 1; x++) {
					for (int y = r.y + 1; y < r.y+r.height - 1; y++) {
						charMap[x][y] = FLOOR;
					}
				}
			}	
		}
		
		for (Path p : paths) p.displayCharMap(charMap);
		
		fogOfWar = new boolean[charMap.length][charMap[0].length];
		
		
	}
	
	public Point getValidLocation() {
		Random rand = new Random();
		Point p = null;
		while (true) {
			
			Room r = rooms[rand.nextInt(SECTOR_COUNT_X)][rand.nextInt(SECTOR_COUNT_Y)];
			
			if (r == null) continue;
			
			int x = rand.nextInt(r.width-2) + r.x + 1;
			int y = rand.nextInt(r.height-2) + r.y + 1;
			p = new Point(x, y);
			break;
		}
		return p;
	}
	
	public Room getRoom(int x, int y) {
		for (Room[] rms : rooms) {
			for (Room r : rms) {
				if (r == null) continue;
				if (r.contains(x, y)) return r;
			}
		}
		return null;
	}
	
	public boolean isValidLocation(int x, int y) {
		char[][] map = toCharArray();
		if (PASSABLE.contains(map[x][y])) return true;
		return false;
	}
	
	public char[][] toCharArray() {

		char[][] map = new char[charMap.length][charMap[0].length];
		
		for (int i = 0; i < charMap.length; i++) {
			System.arraycopy(charMap[i], 0, map[i], 0, charMap[i].length);
		}
		
		for (Entity e : entities) {
			map[e.getX()][e.getY()] = e.getRepresentation();
		}
		
		return map;
	}
	
	public boolean[][] getFogOfWar() {
		return fogOfWar;
	}
	
	public char[][] applyFogOfWar() {
		char[][] map = toCharArray();
		boolean[][] fow = getFogOfWar();
		for (int i = 0; i < map.length; i++) {
			for (int j = 0; j < map[i].length; j++) {
				map[i][j] = (fow[i][j]) ? map[i][j] : EMPTY_SPACE;
			}
		}
		return map;
	}
	
	public void discoverLand(int x, int y) {
		
		Room room = getRoom(x, y);
		if (room != null && !room.isFound()) {
			room.find();
			for (int i = room.x; i < room.x + room.width; i++) {
				for (int j = room.y; j < room.y + room.height; j++) {
					discoverTile(i, j);
				}
			}
		}
		
		// This code is for seeing only one tile away, except in rooms:
		for (int i = -1; i <= 1; i++) {
			discoverTile(x+i, y);
			discoverTile(x, y+i);
		}
		
		
		// This code is for seeing around in a square:
		/*
		
		int r = 1;
		for (int i = x-r; i <= x+r; i++) {
			for (int j = y-r; j <= y+r; j++) {
				discoverTile(i, j);
			}
		}
		*/
	}
	
	/**
	 * 
	 * "Discover" one tile, i.e., allow the player to see it.
	 * 
	 * @param x - the X coordinate of the tile to be discovered
	 * @param y - the Y coordinate of the tile to be discovered
	 * @return if the tile was existent & changed
	 */
	public boolean discoverTile(int x, int y) {
		boolean[][] fow = getFogOfWar();
		if (x > fow.length || x < 0 || y > fow[x].length || y < 0) return false;
		else {
			boolean old = fow[x][y];
			fow[x][y] = true;
			return old != fow[x][y];
		}
	}
	
	public String toString() {
		char[][] map = applyFogOfWar();
		StringBuilder sb = new StringBuilder((height+1)*width);
		for (int i = 0; i < map[0].length; i++) {
			for (int j = 0; j < map.length; j++) {
				sb.append(map[j][i]);
			}
			sb.append('\n');
		}
		return sb.toString();
	}
	
	public Room[][] getRooms() {
		return rooms;
	}
	
	public int getHeight() {
		return height;
	}
	
	public int getWidth() {
		return width;
	}
	
	public Set<Entity> getEntities() {
		return entities;
	}
	
	public Set<Entity> getEntitiesAtLocation(int x, int y) {
		Set<Entity> foundEntities = new HashSet<Entity>();
		for (Entity e : entities) {
			if (e.getX() == x && e.getY() == y) {
				foundEntities.add(e);
			}
		}
		return foundEntities;
	}
	
	public Player getPlayer() {
		for (Entity e : getEntities()) {
			if (e instanceof Player) return (Player) e;
		}
		return null;
	}
	
}
