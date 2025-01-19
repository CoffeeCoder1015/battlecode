package s2;

import battlecode.common.*;
import s2.generics.GenericRobotContoller;

public class Mopper implements GenericRobotContoller {

    RobotController rc;
    Pathing pathing_engine;

    public Mopper(RobotController handler) throws GameActionException{
        rc = handler;
        pathing_engine = new Pathing(handler);
    }

    public void run() throws GameActionException{
        System.out.println("Starting mopper logic...");
       
        // Define all possible directions
        Direction[] directions = Direction.values();
   
        // Initialize variables to track the best direction
        Direction bestDirection = null;
        int maxEnemiesInDirection = 0;
   
        // Get the mopper's current location
        MapLocation curLoc = rc.getLocation();
   
        // Scan for all enemies within the circle of radius 2 * sqrt(2)
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(curLoc, 8, rc.getTeam().opponent());
   
        // Count enemies in each direction based on their relative position
        for (Direction dir : directions) {
            int enemyCount = 0;
   
            for (RobotInfo enemy : nearbyEnemies) {
                MapLocation enemyLoc = enemy.getLocation();
   
                // Check if the enemy lies in the swing range for the current direction
                if (isInSwingRange(curLoc, enemyLoc, dir)) {
                    enemyCount++;
                }
            }
   
            System.out.println("Direction: " + dir + ", Enemies: " + enemyCount);
   
            // Update the best direction if this one has more enemies
            if (enemyCount > maxEnemiesInDirection) {
                maxEnemiesInDirection = enemyCount;
                bestDirection = dir;
            }
        }
   
        // Perform the mop swing in the best direction if enemies are found
        if (bestDirection != null && maxEnemiesInDirection > 0) {
           
            rc.mopSwing(bestDirection);
        } else {
            
        }
   
        // If no enemies to mop swing, move randomly
        pathing_engine.Move();

        // Try to paint beneath us as we walk to avoid paint penalties
        MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
        if (!currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation())) {
            rc.attack(rc.getLocation());
        }
    }

    private boolean isInSwingRange(MapLocation mopperLoc, MapLocation targetLoc, Direction swingDir) {
        // Get the relative position of the target
        int dx = targetLoc.x - mopperLoc.x;
        int dy = targetLoc.y - mopperLoc.y;
   
        // Check based on direction and relative positions
        switch (swingDir) {
            case NORTH:
                return dx >= -1 && dx <= 1 && dy < 0 && dy >= -2;
            case SOUTH:
                return dx >= -1 && dx <= 1 && dy > 0 && dy <= 2;
            case EAST:
                return dy >= -1 && dy <= 1 && dx > 0 && dx <= 2;
            case WEST:
                return dy >= -1 && dy <= 1 && dx < 0 && dx >= -2;
            default:
                return false;
        }
    }
}
