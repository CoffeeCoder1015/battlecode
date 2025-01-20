package s2;

import battlecode.common.*;
import s2.generics.GenericRobotContoller;

public class Mopper implements GenericRobotContoller {

    RobotController rc;
    Pathing pathing_engine;

    public Mopper(RobotController handler) throws GameActionException {
        rc = handler;
        pathing_engine = new Pathing(handler);
    }

    public void run() throws GameActionException {
        System.out.println("Starting mopper logic...");

        MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
        if (currentTile.getPaint() != null && currentTile.getPaint().isEnemy() && rc.canAttack(rc.getLocation())) {
            System.out.println("Attacking enemy paint under itself.OVERRIDE.########################################");
            rc.attack(rc.getLocation(), false); // Use primary paint color
            return; // Exit this turn after attacking the square under itself
        }

        // Reset variables
        Direction bestDirection = null; // Redeclared here - causing the error
        int maxEnemiesInDirection = 0; // Redeclared here - causing the error

        // Define all possible directions
        Direction[] directions = Direction.values();

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
                if (rc.isActionReady() && isInSwingRange(curLoc, enemyLoc, dir)) {

                    enemyCount++;
                }
            }
            // Update the best direction if this one has more enemies
            if (enemyCount > maxEnemiesInDirection) {
                maxEnemiesInDirection = enemyCount;
                bestDirection = dir;
            }
        }

        if (bestDirection != null && maxEnemiesInDirection > 0) {
            // Check if the robot can perform the mop swing
            if (rc.isActionReady() && rc.canMopSwing(bestDirection)) { 
                rc.mopSwing(bestDirection);
            } else {
                // Handle invalid swing or cooldown
                if (!rc.canMopSwing(bestDirection)) {
                    System.out.println("Mop swing skipped: Can't swing in direction " + bestDirection);
                } else {
                    System.out.println("Mop swing skipped: Action cooldown not expired.");
                }
                // Pass onto the else block or fallback
            }
        } else {
            bestDirection = findEnemyPaintDirection(curLoc, directions);

            if (bestDirection != null) {
                if (rc.isActionReady()) { // Check if the robot is ready to act
                    System.out.println("Clearing enemy paint in direction: " + bestDirection);

                    // Declare and reset targetLoc
                    MapLocation targetLoc = null;

                    // Calculate the target location based on the best direction
                     targetLoc = curLoc.add(bestDirection);

                    // Check if attack is possible and perform the attack
                    if (rc.canAttack(targetLoc)) {
                        rc.attack(targetLoc, false); // Use primary paint color to clear the tile
                    }
                } else {
                    System.out.println("Attack skipped: Action cooldown not expired.");
                }
            } else {
                pathing_engine.Move();
            }
        }

        // Try to paint beneath us as we walk to avoid paint penalties
        currentTile = rc.senseMapInfo(rc.getLocation());
        if (!currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation())) {
            rc.attack(rc.getLocation(), false); // Use primary paint color
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

    private Direction findEnemyPaintDirection(MapLocation curLoc, Direction[] directions) throws GameActionException {
        Direction bestDirection = null;
        int maxEnemyPaintCount = 0;

        // Check each direction for enemy paint
        for (Direction dir : directions) {
            int enemyPaintCount = 0;

            // Scan adjacent tiles in the direction
            MapLocation targetLoc = curLoc.add(dir);

            // Ensure the target location is on the map
            if (rc.onTheMap(targetLoc)) {
                MapInfo targetTile = rc.senseMapInfo(targetLoc);

                // Count tiles with enemy paint
                if (targetTile.getPaint() != null && targetTile.getPaint().isEnemy() && targetTile.isPassable()) {
                    enemyPaintCount++;
                }
                

                // Update the best direction if this one has more enemy paint
                if (enemyPaintCount > maxEnemyPaintCount) {
                    maxEnemyPaintCount = enemyPaintCount;
                    bestDirection = dir;
                }
            }
        }

        return bestDirection;
    }
}

