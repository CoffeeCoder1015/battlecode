package TESTING_srp;

import java.util.Random;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashSet;

import battlecode.common.*;


public class RobotPlayer {
    static final Random rng = new Random(6147);

    // robot movement
    static int continuation_count = 0;
    static int last_dir_index = -1;
    static Direction cur_dir;

    // tower
    static int rtype = 0;
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

    static byte[][] SRP_map = null;

    public static void run(RobotController rc) throws GameActionException {
        int a = 0;
        a += 1;
        RCFunc processor;
        switch (rc.getType()) {
            case SOLDIER:
                SRP_map = new byte[rc.getMapWidth()][rc.getMapHeight()];
                processor = RobotPlayer::runSoldier;
                if (rc.getTeam() == Team.B) {
                    return;
                }
                break;
            case MOPPER:
                processor = RobotPlayer::runMopper;
                break;
            case SPLASHER:
                processor = RobotPlayer::runSplasher;
                break; // Consider upgrading examplefuncsplayer to use splashers!
            default:
                processor = RobotPlayer::runTower;
                break;
        }

        while (true) {
           try {
                processor.p(rc); 
            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
                System.exit(0);
            } catch (Exception e) {
                System.out.println("Exception");
                System.exit(0);
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop
                // again.
                Clock.yield();
            }
        }
    }


    static int bcount = 0;
    public static void runTower(RobotController rc) throws GameActionException {
        // Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);
        // Pick a random robot type to build.
        if (rc.canBuildRobot(UnitType.SOLDIER, nextLoc) && bcount == 0) {
            rc.buildRobot(UnitType.SOLDIER, nextLoc);
            bcount++;
        }

        // Read incoming messages
        Message[] messages = rc.readMessages(-1);
        for (Message m : messages) {
            System.out.println("Tower received message: '#" + m.getSenderID() + " " + m.getBytes());
        }

        // TODO: can we attack other bots?
    }

    public static void runSplasher(RobotController rc) throws GameActionException {
    }

    static byte[][] KeyLocations = {
        {4,20},
        {4,14},
        {4,10},
        {6,6}

    };
    static int locations_completed = 0;
    public static void runSoldier(RobotController rc) throws GameActionException {
        byte[] target_cord = KeyLocations[locations_completed];
        MapLocation Target = new MapLocation(target_cord[0],target_cord[1]);
        Direction tdir = rc.getLocation().directionTo(Target);
        if (rc.canMove(tdir)) {
            rc.move(tdir); 
        }
        // if (rc.getRoundNum() > 100) {
        //    rc.attack(rc.getLocation()); 
        // }
        // if (rc.canMarkResourcePattern(Target)) {
        //     rc.markResourcePattern(Target);
        // }
        System.out.println(locations_completed);
        //// NOTE: if center found within visual range, nothing can be built in this location
        if (locations_completed > 1 && rc.getRoundNum() > 100) {
           rc.attack(rc.getLocation(),false);
        }
        if (rc.canSenseLocation(Target)) {
            MapInfo info = rc.senseMapInfo(Target);
            boolean c_alrdy = rc.canCompleteResourcePattern(Target)==false &&info.isResourcePatternCenter() == true;
            if (c_alrdy && rc.getRoundNum() > 100) {
                rc.attack(Target,false);
            }
        }
        if (rc.canCompleteResourcePattern(Target)) {
            rc.setTimelineMarker("SRP comp", 255,0,0);
            System.out.println("here");
            rc.completeResourcePattern(Target);
            locations_completed++;
        }
    }

    /**
     * Run a single turn for a Mopper.
     * This code is wrapped inside the infinite loop in run(), so it is called once
     * per turn.
     */
    public static void runMopper(RobotController rc) throws GameActionException {
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

    static boolean temp_false = false;
    public static boolean buildRuins(RobotController rc) throws GameActionException {
        temp_false = false;
        // Sense information about all visible nearby tiles.
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        // Search for a nearby ruin to complete.
        MapInfo curRuin = null;
        int curDist = 100000000;
        for (MapInfo tile : nearbyTiles) {
            // Make sure the ruin is not already complete (has no tower on it)
            if (tile.hasRuin() && rc.senseRobotAtLocation(tile.getMapLocation()) == null) {
                int checkDist = tile.getMapLocation().distanceSquaredTo(rc.getLocation());
                if (checkDist < curDist) {
                    curDist = checkDist;
                    curRuin = tile;
                }
            }
        }

        if (curRuin != null) {
            MapLocation targetLoc = curRuin.getMapLocation();
            Direction dir = rc.getLocation().directionTo(targetLoc);
            if (rc.canMove(dir))
                rc.move(dir);
            // Mark the pattern we need to draw to build a tower here if we haven't already.
            UnitType towerToBuild = UnitType.LEVEL_ONE_MONEY_TOWER;
            if (rc.getChips() > 2000) {
                towerToBuild = UnitType.LEVEL_ONE_PAINT_TOWER;
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
                temp_false = true;
                return false;
            }
            return true;
        }
        return false;
    }
}
