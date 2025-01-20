package s2;

import battlecode.common.*;
import s2.generics.GenericRobotContoller;

public class Soldier implements GenericRobotContoller {
    boolean isBuildingRuin = false;
    boolean isBuildingSRP = false;
    boolean SRP_built = false;
    int cant_find_tower_for = 0;
    boolean[][] SRP_pattern;
    boolean[][] MoneyPattern;
    boolean[][] PaintPattern;
    RobotController rc;
    Pathing pathing_engine;
    boolean buildPaintTowerNext = false;

    MapLocation currentLocation;

    public Soldier(RobotController handler) throws GameActionException {
        rc = handler;
        pathing_engine = new Pathing(handler);
        SRP_pattern = rc.getResourcePattern();
        MoneyPattern = rc.getTowerPattern(UnitType.LEVEL_ONE_MONEY_TOWER);
        PaintPattern = rc.getTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER);
    }
    
    public void run() throws GameActionException {
        //get our current location at the start of each run
        currentLocation = rc.getLocation();
        if (shouldBuildSRP()) {
           buildSRP(); 
        }

        boolean found = false;
        if (!isBuildingSRP) {
            found = buildRuins();
            if (!found) {
               cant_find_tower_for++; 
            }
        }
        // Move and attack randomly if no objective.
        if (!found && !isBuildingSRP) {
            pathing_engine.Move();
        }
        //catch all 
        if (rc.isActionReady()) {
            pathing_engine.Move();
        }
        // Try to paint beneath us as we walk to avoid paint penalties.
        // Avoiding wasting paint by re-painting our own tiles.
        if (!isBuildingSRP) {
            MapInfo[] infos = rc.senseNearbyMapInfos(9);
            for (MapInfo mapInfo : infos) {
               MapLocation T = mapInfo.getMapLocation();
               if (rc.canAttack(T)&& !mapInfo.getPaint().isAlly() && mapInfo.isPassable()) {
                   rc.attack(T); 
                   break;
               }
            }
        }
    }


    private boolean buildRuins() throws GameActionException {
        isBuildingRuin = false;
        // Sense information about all visible nearby tiles.
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(-1);
        // Search for a nearby ruin to complete.
        MapInfo curRuin = null;
        int curDist = 100000000;
        for (MapInfo tile : nearbyTiles) {
            // Make sure the ruin is not already complete (has no tower on it)
            if (tile.hasRuin() && rc.senseRobotAtLocation(tile.getMapLocation()) == null) {
                int checkDist = tile.getMapLocation().distanceSquaredTo(currentLocation);
                if (checkDist < curDist) {
                    curDist = checkDist;
                    curRuin = tile;
                }
            }
        }

        if (curRuin != null) {
            MapLocation targetLoc = curRuin.getMapLocation();
            Direction dir = currentLocation.directionTo(targetLoc);
            if (rc.canMove(dir))
                rc.move(dir);
            // Mark the pattern we need to draw to build a tower here if we haven't already.
            // Decide tower type based on logic
            UnitType towerToBuild = UnitType.LEVEL_ONE_MONEY_TOWER;
            boolean[][] PatternToUse = MoneyPattern;

            // If chips exceed 3000, always build paint towers
            if (rc.getChips() > rc.getMapWidth()*rc.getMapHeight()*2.3) {
                towerToBuild = UnitType.LEVEL_ONE_PAINT_TOWER;
                PatternToUse = PaintPattern;
            } 

            for (MapInfo mapInfo : nearbyTiles) {
                MapLocation tileLocation = mapInfo.getMapLocation();
                if (tileLocation == targetLoc) {
                    continue;
                }
                if (!rc.canAttack(tileLocation)) {
                    continue;
                }
                MapLocation relative_loc = tileLocation.translate(-targetLoc.x, -targetLoc.y);
                boolean x_in = -2 <= relative_loc.x && relative_loc.x <= 2;
                boolean y_in = -2 <= relative_loc.y && relative_loc.y <= 2;
                if (x_in && y_in) {
                    rc.setIndicatorDot(tileLocation, 12, 111, 250);
                    boolean color = PatternToUse[relative_loc.x + 2][-(relative_loc.y - 2)];
                    PaintType correct_paint = PaintType.ALLY_PRIMARY;
                    if (color) {
                        correct_paint = PaintType.ALLY_SECONDARY;
                    }
                    if (mapInfo.getPaint() != correct_paint) {
                        rc.attack(mapInfo.getMapLocation(), color);
                        break;
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

    private boolean shouldBuildSRP() throws GameActionException{
        /* early exit conditions */
        if (SRP_built || cant_find_tower_for < 40 || !rc.canMarkResourcePattern(currentLocation) || rc.getChips() < 700 || !rc.isActionReady()) {
           return false; 
        }
        /* bad location exit */
        if (!isBuildingSRP) { // not already doing it
            MapInfo[] info = rc.senseNearbyMapInfos(-1);
            for (MapInfo mapInfo : info) {
                MapLocation tileLocation = mapInfo.getMapLocation();
                if (mapInfo.isResourcePatternCenter()) { // will overlap already built SRP
                    // Resource centers at the very edge of the vision radius 
                    // that are on the same x or y axis as the current are overlappable without issue
                    // relative locations (0,4) (-4,0) (0,-4) (4,0)
                    MapLocation relativeLocation = tileLocation.translate(-currentLocation.x, -currentLocation.y);
                    int x = relativeLocation.x;
                    int y = relativeLocation.y;
                    boolean case1 = x == 0 && (y == 4 || y == -4); 
                    boolean case2 = y == 0 && (x == 4 || x == -4); 
                    if (!(case1 || case2)) {
                        return false;
                    }
                }

                // Has wall or tower blocking
                // Locations within SRP range
                boolean noRobot = rc.senseRobotAtLocation(tileLocation) == null;
                // cant build if there is uncompleted ruin
                // this makes sure SRPs wont overlap onto TowerPatterns
                boolean noTower = mapInfo.hasRuin() && noRobot;
                if (noTower) {
                    return false;
                }
            }
        }
        return true;
    }

    private void buildSRP() throws GameActionException{
        isBuildingSRP = true;
        MapInfo[] key_squares = rc.senseNearbyMapInfos(8);
        if (rc.senseMapInfo(currentLocation).getPaint() != PaintType.ALLY_SECONDARY) {
            rc.attack(currentLocation, true);
        } else {
            for (MapInfo mapInfo : key_squares) {
                MapLocation relative_loc = mapInfo.getMapLocation().translate(-currentLocation.x, -currentLocation.y);
                rc.setIndicatorDot(currentLocation, 0, 0, 255);
                // System.out.println(relative_loc);
                boolean color = SRP_pattern[relative_loc.x + 2][-(relative_loc.y - 2)];
                PaintType correct_paint = PaintType.ALLY_PRIMARY;
                if (color) {
                    correct_paint = PaintType.ALLY_SECONDARY;
                }
                if (mapInfo.getPaint() != correct_paint) {
                    rc.attack(mapInfo.getMapLocation(), color);
                    break;
                }
            }
        }
        if (rc.canCompleteResourcePattern(currentLocation)) {
            rc.completeResourcePattern(currentLocation);
            rc.setTimelineMarker("Built SRP", 255, 0, 0);
            SRP_built = true;
            isBuildingSRP = false;
        }
    }

}
