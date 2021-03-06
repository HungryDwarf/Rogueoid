package lekro.rogueoid.map;

import java.awt.Point;
import java.util.Random;

public class Path {

	// 0 = X, 1 = Y
	private int direction, middle;
	private Point start, end;
	
	/**
	 * Constructs a path to connect rooms a and b.
	 * @param a
	 * @param b
	 */
	public Path(Room a, Room b) {
		
		// Assuming a and b aren't null.
		// It also helps if the rooms are adjacent, though this is not necessary.
		
		int aX = a.x;
		int aY = a.y;
		int daX = a.width - 1;
		int daY = a.height - 1;
		
		int bX = b.x;
		int bY = b.y;
		int dbX = b.width - 1;
		int dbY = b.height - 1;
		
		// Checking if the rooms overlap, if so, we're not making a path.
		// Let's do this with booleans, so we don't get this super large
		// if statement.
		
		boolean leftA = (aX + daX < bX); // A left of B
		boolean leftB = (bX + dbX < aX); // B left of A
		boolean upA =   (aY + daY < bY); // A above B
		boolean upB =   (bY + dbY < aY); // B above A
		
		if (a.intersects(b)) throw new IllegalArgumentException("Rooms overlap!");
		
		// Okay, now that we got that out of the way,
		// let's figure out what direction we're going.
		
		boolean overlapX = !(leftA || leftB);
		boolean overlapY = !(upA || upB);
		
		boolean overlapNone = !(overlapX || overlapY);
		
		if (overlapNone) {
			
			// Need a bit more information about the space between the rooms...
			// (we don't roll a die just yet)
			
			int spacingX = 0;
			int spacingY = 0;
			
			if (leftA) {
				spacingX = bX - (aX + daX);
			} else {
				spacingX = aX - (bX + dbX);
			}
			
			if (upA) {
				spacingY = bY - (aY + daY);
			} else {
				spacingY = aY - (bY + dbY);
			}
			
			if (spacingX >= spacingY) {
				direction = 0;
			} else {
				direction = 1;
			}
			
		} else if (overlapX) {
			
			// We'll go up/down (Y)
			direction = 1;
			
		} else {
			
			// We'll go left/right (X)
			direction = 0;
			
		}
		
		// Finally, we know which way to go.
		// Now for the fun part - choosing coordinates...
		
		int startX, startY, endX, endY;
		Random rand = new Random();
		
		if (direction == 0) { // going in X-direction
			
			if (leftA) {
				startX = aX + daX;
				endX = bX;
				startY = aY + rand.nextInt(daY-3) + 2;
				endY = bY + rand.nextInt(dbY-3) + 2;
			} else {
				startX = bX + dbX;
				endX = aX;
				startY = bY + rand.nextInt(dbY-3) + 2;
				endY = aY + rand.nextInt(daY-3) + 2;
			}
			
			middle = rand.nextInt(endX - startX - 1) + startX;
			
		} else { // going in Y-direction
			
			if (upA) {
				startY = aY + daY;
				endY = bY;
				startX = aX + rand.nextInt(daX-3) + 2;
				endX = bX + rand.nextInt(dbX-3) + 2;
			} else {
				startY = bY + dbY;
				endY = aY;
				startX = bX + rand.nextInt(dbX-3) +2;
				endX = aX + rand.nextInt(daX-3) + 2;
			}
			
			middle = rand.nextInt(endY - startY - 1) + startY;
			
		}
		
		start = new Point(startX, startY);
		end = new Point(endX, endY);
		
	}
	
	public int getMiddle() {
		return middle;
	}
	
	public Point getStart() {
		return start;
	}
	
	public Point getEnd() {
		return end;
	}
	
	public void displayCharMap(char[][] out) {
		int x = start.x;
		int y = start.y;
		
		boolean dir = direction == 0;
		
		char pathFloor = Level.PATH_FLOOR;
		char door = Level.DOOR;
		
		if (dir) {
			out[x++][y] = door;
			for (; x <= middle; x++) out[x][y] = pathFloor; 
			if (y <= end.y) for (; y <= end.y; y++) out[x][y] = pathFloor; 
			else for (; y >= end.y; y--) out[x][y] = pathFloor; 
			for (; x < end.x; x++) out[x][y] = pathFloor;
			out[x][y] = door;
		} else {
			out[x][y++] = door;
			for (; y <= middle; y++) out[x][y] = pathFloor;
			if (x <= end.x) for (; x <= end.x; x++) out[x][y] = pathFloor;
			else for (; x >= end.x; x--) out[x][y] = pathFloor; 
			for (; y < end.y; y++) out[x][y] = pathFloor;
			out[x][y] = door;
		}
	}
	
}
