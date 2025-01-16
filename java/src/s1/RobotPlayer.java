package s1;


import java.util.Random;
import java.util.ArrayList;


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


    public static void run(RobotController rc) throws GameActionException {
        RCFunc processor;
        switch (rc.getType()) {
            case SOLDIER:
                processor = RobotPlayer::runSoldier;
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
        SRP_pattern = rc.getResourcePattern();


        while (true) {
            try {
                processor.p(rc);
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


    public static void runTower(RobotController rc) throws GameActionException {

        // Check for surplus and spawn soldier if conditions are met
        //System.out.println("This is the money: " + rc.getMoney());
        //System.out.println("This is the paint: " + rc.getPaint());

        if (rc.getMoney() > 350 && rc.getPaint() > 300) {
            for (Direction dir : directions) {
                MapLocation spawnLoc = rc.getLocation().add(dir);
                if (rc.canBuildRobot(UnitType.SOLDIER, spawnLoc)) {
                    rc.buildRobot(UnitType.SOLDIER, spawnLoc);
                    System.out.println("Surplus soldier spawned at: " + spawnLoc);
                    break; // Spawn only one soldier
                }
            }
        }

        // Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);
        // Pick a random robot type to build.
        int robotType = rtype;
        if (robotType == 0 && rc.canBuildRobot(UnitType.SPLASHER, nextLoc)) {
            rc.buildRobot(UnitType.SPLASHER, nextLoc);
            rtype++;
        } else if (robotType == 1 && rc.canBuildRobot(UnitType.MOPPER, nextLoc)) {
            rc.buildRobot(UnitType.MOPPER, nextLoc);
            rtype++;
        } else if (robotType == 2 && rc.canBuildRobot(UnitType.SOLDIER, nextLoc)) {
            rc.buildRobot(UnitType.SOLDIER, nextLoc);
            rtype++;
        }
        if (rtype == 3) {
            int skip = rng.nextInt(4);
            if (skip == 1) {
                rtype = 0;
            }
        }

        // Attack logic for Tower
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

// Perform Single Target Attack on the lowest HP priority target
        if (nearbyEnemies.length > 0) {
            RobotInfo target = null;

            // Find the lowest HP priority target
            for (RobotInfo enemy : nearbyEnemies) {
                if (target == null || enemy.getHealth() < target.getHealth() ||
                        (enemy.getHealth() == target.getHealth() && isHigherPriority(enemy, target))) {
                    target = enemy;
                }
            }

            if (target != null && rc.canAttack(target.getLocation())) {
                rc.attack(target.getLocation());
            }
        }

// Perform AoE Attack if any enemies are in range
        if (rc.canAttack(null)) {
            rc.attack(null);
        }



        // Read incoming messages
        Message[] messages = rc.readMessages(-1);
        for (Message m : messages) {
            System.out.println("Tower received message: '#" + m.getSenderID() + " " + m.getBytes());
        }


        // TODO: can we attack other bots?
    }
    // Helper method to determine if one robot is a higher priority target than another
    private static boolean isHigherPriority(RobotInfo current, RobotInfo target) {
        UnitType currentType = current.getType();
        UnitType targetType = target.getType();

        // Prioritize splashers first, then soldiers, and lastly moppers
        if (currentType == UnitType.SPLASHER && targetType != UnitType.SPLASHER) {
            return true;
        } else if (currentType == UnitType.SOLDIER && targetType == UnitType.MOPPER) {
            return true;
        }

        return false;
    }


    public static void runSplasher(RobotController rc) throws GameActionException {
        // Sense information about all visible nearby tiles.
        // Move and attack randomly if no objective.
        if (continuation_count == 0) {
            continuation_count = rng.nextInt(120) + 80;
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
    static boolean isBuildingRuin = false;
    static boolean isBuildingSRP = false;
    static boolean SRP_built = false;
    static boolean[][] SRP_pattern;
    public static void runSoldier(RobotController rc) throws GameActionException {
        if (SRP_built == false) {
            boolean early_exit = false;
            if (!rc.canMarkResourcePattern(rc.getLocation())) {
                early_exit = true;
            }
            if (!isBuildingSRP && early_exit == false) {
                MapInfo[] info = rc.senseNearbyMapInfos();
                for (MapInfo mapInfo : info) {
                    if (mapInfo.isResourcePatternCenter()) {
                        early_exit = true;
                        break;
                    }

                    // Locations within SRP range
                    boolean noRobot = rc.senseRobotAtLocation(mapInfo.getMapLocation()) == null;
                    boolean in_range = mapInfo.getMapLocation().isWithinDistanceSquared(rc.getLocation(), 1);
                    if (in_range) {
                        boolean isPaintedbyAlly =mapInfo.getPaint().isAlly(); 
                        boolean hasTower = mapInfo.hasRuin() && !noRobot;
                        if (mapInfo.isWall() || hasTower) {
                            early_exit = true;
                            break;
                        }
                    }

                    boolean noTower = mapInfo.hasRuin() && noRobot;
                    if (noTower) {
                        early_exit = true;
                        break;
                    }
                }
            }
            if (!early_exit && rc.isActionReady()) {
                isBuildingSRP = true;
                MapLocation curr_loc = rc.getLocation();
                MapInfo[] key_squares = rc.senseNearbyMapInfos(8);
                if (rc.senseMapInfo(rc.getLocation()).getPaint() != PaintType.ALLY_SECONDARY) {
                   rc.attack(curr_loc, true); 
                }else{
                    for (MapInfo mapInfo : key_squares) {
                    MapLocation relative_loc = mapInfo.getMapLocation().translate(-curr_loc.x, -curr_loc.y);
                    rc.setIndicatorDot(curr_loc, 0,0,255);
                    // System.out.println(relative_loc);
                    boolean color = SRP_pattern[relative_loc.x+2][-(relative_loc.y-2)];
                    PaintType correct_paint = PaintType.ALLY_PRIMARY;
                    if (color) {
                       correct_paint = PaintType.ALLY_SECONDARY; 
                    }
                    if (mapInfo.getPaint() != correct_paint) {
                       rc.attack(mapInfo.getMapLocation(),color); 
                       break;
                    }
                }
                }
                if (rc.canCompleteResourcePattern(curr_loc)) {
                   rc.completeResourcePattern(curr_loc); 
                   rc.setTimelineMarker("Built SRP", 255,0,0);
                   SRP_built = true;
                   isBuildingSRP = false;
                }
            }
        }
        boolean found = false;
        if (!isBuildingSRP) {
            found = buildRuins(rc);
        }
        // Move and attack randomly if no objective.
        if (!found && !isBuildingSRP) {
            if (continuation_count == 0) {
                continuation_count = rng.nextInt(5) + 1;
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
        if (!currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation()) && SRP_built) {
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
        MapLocation nextLoc = null;
        if (continuation_count == 0) {
            continuation_count = rng.nextInt(5) + 1;
            int i;
            for (i = 0; i < 8; i++) {
                Direction possible = directions[i];
                nextLoc = rc.getLocation().add(possible);
                if (rc.canMove(possible)) {
                    boolean good_position = !rc.senseMapInfo(nextLoc).getPaint().isAlly()
                            && rc.canAttack(rc.getLocation());
                    if (good_position && i != last_dir_index) {
                        cur_dir = possible;
                        last_dir_index = i;
                        break;
                    }
                }
            }


            if (i == 8) {
                cur_dir = directions[rng.nextInt(directions.length)];
            }
        }


        if (rc.canMove(cur_dir)) {
            rc.move(cur_dir);
            continuation_count--;
        } else {
            continuation_count = 0;
        }
        if (rc.canMopSwing(cur_dir)) {
            rc.mopSwing(cur_dir);
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

    static boolean buildPaintTowerNext = false;

    public static boolean buildRuins(RobotController rc) throws GameActionException {
        isBuildingRuin = false;
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
            // Decide tower type based on logic
            UnitType towerToBuild;

            // If chips exceed 3000, always build paint towers
            if (rc.getChips() > 3000) {
                towerToBuild = UnitType.LEVEL_ONE_PAINT_TOWER;
            } else {
                // Alternate between paint and money towers
                if (buildPaintTowerNext) {
                    towerToBuild = UnitType.LEVEL_ONE_PAINT_TOWER;
                } else {
                    towerToBuild = UnitType.LEVEL_ONE_MONEY_TOWER;
                }
                buildPaintTowerNext = !buildPaintTowerNext; // Toggle for the next tower
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
                isBuildingRuin = true;
                return false;
            }
            return true;
        }
        return false;
    }
}
