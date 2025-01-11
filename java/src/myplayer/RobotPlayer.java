package myplayer;

import battlecode.common.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class RobotPlayer {
    static int turnCount = 0;

    static final Random rng = new Random(6147);

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    // robot movement
    static int continuation_count = 0;
    static int last_dir_index = -1;
    static Direction cur_dir;

    // tower
    static int rtype = 0;

    public static void run(RobotController rc) throws GameActionException {
        rc.setIndicatorString("Hello world!");
        while (true) {
            turnCount += 1;
            try {
                switch (rc.getType()) {
                    case SOLDIER:
                        runSoldier(rc);
                        break;
                    case MOPPER:
                        runMopper(rc);
                        break;
                    case SPLASHER:
                        runSplasher(rc);
                        break; // Consider upgrading examplefuncsplayer to use splashers!
                    default:
                        runTower(rc);
                        break;
                }
            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop
                // again.
                Clock.yield();
            }
        }
    }

    /**
     * Run a single turn for towers.
     * This code is wrapped inside the infinite loop in run(), so it is called once
     * per turn.
     */
    public static void runTower(RobotController rc) throws GameActionException {
        // Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);
        // Pick a random robot type to build.
        int robotType = rtype;
        if (robotType == 0 && rc.canBuildRobot(UnitType.SOLDIER, nextLoc)) {
            rc.buildRobot(UnitType.SOLDIER, nextLoc);
            rtype++;
        } else if (robotType == 1 && rc.canBuildRobot(UnitType.MOPPER, nextLoc)) {
            rc.buildRobot(UnitType.MOPPER, nextLoc);
            rtype++;
        } else if (robotType == 2 && rc.canBuildRobot(UnitType.SPLASHER, nextLoc)) {
            rc.buildRobot(UnitType.SPLASHER, nextLoc);
            rtype++;
        }
        if (rtype == 3) {
            int skip = rng.nextInt(2);
            if (skip == 1) {
                rtype = 0;
            }
        }
        

        // Read incoming messages
        Message[] messages = rc.readMessages(-1);
        for (Message m : messages) {
            System.out.println("Tower received message: '#" + m.getSenderID() + " " + m.getBytes());
        }

        // TODO: can we attack other bots?
    }

    public static void runSplasher(RobotController rc) throws GameActionException {
        // Sense information about all visible nearby tiles.
        // Move and attack randomly if no objective.
        if (continuation_count == 0) {
            continuation_count = rng.nextInt(60) + 10;
            int i;
            ArrayList<Integer> moveable = new ArrayList<Integer>();
            for (i = 0; i < 8; i++) {
                Direction possible = directions[i];
                MapLocation nextLoc = rc.getLocation().add(possible);
                if (rc.canMove(possible)) {
                    moveable.add(i);
                    if (!rc.senseMapInfo(nextLoc).getPaint().isAlly() && i != last_dir_index) {
                        cur_dir = possible;
                        last_dir_index = i;
                        break;
                    }
                }
            }

            if (i == 8 && moveable.size() > 0) {
                int index = rng.nextInt(moveable.size());
                cur_dir = directions[moveable.get(index)];
            }
        }

        if (rc.canMove(cur_dir)) {
            rc.move(cur_dir);
            continuation_count--;
        } else {
            continuation_count = 0;
        }
        // Try to paint beneath us as we walk to avoid paint penalties.
        // Avoiding wasting paint by re-painting our own tiles.
        MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
        if (!currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation())) {
            rc.attack(rc.getLocation());
        }

    }

    /**
     * Run a single turn for a Soldier.
     * This code is wrapped inside the infinite loop in run(), so it is called once
     * per turn.
     */
    public static void runSoldier(RobotController rc) throws GameActionException {
        boolean found = buildRuins(rc);
        // Move and attack randomly if no objective.
        if (!found) {
            if (continuation_count == 0) {
                continuation_count = rng.nextInt(60) + 10;
                int i;
                for (i = 0; i < 8; i++) {
                    Direction possible = directions[i];
                    MapLocation nextLoc = rc.getLocation().add(possible);
                    if (rc.canMove(possible)) {
                        boolean good_position = !rc.senseMapInfo(nextLoc).getPaint().isAlly() && rc.canAttack(rc.getLocation());
                        if (good_position && i != last_dir_index) {
                            cur_dir = possible;
                            last_dir_index = i;
                            break;
                        }
                    }
                }

                if (i == 8 ) {
                    cur_dir = directions[rng.nextInt(directions.length)];
                }
            }

            if (rc.canMove(cur_dir)) {
                rc.move(cur_dir);
                continuation_count--;
            } else {
                continuation_count = 0;
            }
        }else{
            continuation_count = 0;
        }
        // Try to paint beneath us as we walk to avoid paint penalties.
        // Avoiding wasting paint by re-painting our own tiles.
        MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
        if (!currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation())) {
            rc.attack(rc.getLocation());
        }
    }

    /**
     * Run a single turn for a Mopper.
     * This code is wrapped inside the infinite loop in run(), so it is called once
     * per turn.
     */
    public static void runMopper(RobotController rc) throws GameActionException {
        // Move and attack randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
        if (rc.canMopSwing(dir)) {
            rc.mopSwing(dir);
        } else if (rc.canAttack(nextLoc)) {
            rc.attack(nextLoc);
        }
        // We can also move our code into different methods or classes to better
        // organize it!
        updateEnemyRobots(rc);
    }

    public static void updateEnemyRobots(RobotController rc) throws GameActionException {
        // Sensing methods can be passed in a radius of -1 to automatically
        // use the largest possible value.
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemyRobots.length != 0) {
            // Save an array of locations with enemy robots in them for possible future use.
            MapLocation[] enemyLocations = new MapLocation[enemyRobots.length];
            for (int i = 0; i < enemyRobots.length; i++) {
                enemyLocations[i] = enemyRobots[i].getLocation();
            }
            RobotInfo[] allyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
            // Occasionally try to tell nearby allies how many enemy robots we see.
            if (rc.getRoundNum() % 20 == 0) {
                for (RobotInfo ally : allyRobots) {
                    if (rc.canSendMessage(ally.location, enemyRobots.length)) {
                        rc.sendMessage(ally.location, enemyRobots.length);
                    }
                }
            }
        }
    }

    public static boolean buildRuins(RobotController rc) throws GameActionException {
        // Sense information about all visible nearby tiles.
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        // Search for a nearby ruin to complete.
        MapInfo curRuin = null;
        for (MapInfo tile : nearbyTiles) {
            if (tile.hasRuin()) {
                curRuin = tile;
            }
        }

        if (curRuin != null) {
            MapLocation targetLoc = curRuin.getMapLocation();
            Direction dir = rc.getLocation().directionTo(targetLoc);
            boolean isActuallyTower = rc.canTransferPaint(targetLoc, -1);
            if (isActuallyTower) {
               return false; 
            }
            if (rc.canMove(dir))
                rc.move(dir);
            // Mark the pattern we need to draw to build a tower here if we haven't already.
            UnitType towerToBuild = UnitType.LEVEL_ONE_MONEY_TOWER;
            int t_id = rng.nextInt(4);
            switch (t_id) {
                case 3:
                    towerToBuild = UnitType.LEVEL_ONE_PAINT_TOWER;
                    break;
                default:
                    break;
            }
            MapLocation shouldBeMarked = curRuin.getMapLocation().subtract(dir);
            if (rc.senseMapInfo(shouldBeMarked).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(towerToBuild, targetLoc)) {
                rc.markTowerPattern(towerToBuild, targetLoc);
                System.out.println("Trying to build a tower at " + targetLoc);
            }
            // Fill in any spots in the pattern with the appropriate paint.
            for (MapInfo patternTile : rc.senseNearbyMapInfos(targetLoc, 8)) {
                if (patternTile.getMark() != patternTile.getPaint()) {
                    if (patternTile.getMark() != PaintType.EMPTY) {
                        boolean useSecondaryColor = patternTile.getMark() == PaintType.ALLY_SECONDARY;
                        if (rc.canAttack(patternTile.getMapLocation()))
                            rc.attack(patternTile.getMapLocation(), useSecondaryColor);
                    }
                }
            }
            // Complete the ruin if we can.
            if (rc.canCompleteTowerPattern(towerToBuild, targetLoc)) {
                rc.completeTowerPattern(towerToBuild, targetLoc);
                rc.setTimelineMarker("Tower built", 0, 255, 0);
                System.out.println("Built a tower at " + targetLoc + "!");
            }else{
                return false;
            }
            return true;
        }
        return false;
    }
}
