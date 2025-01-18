package s2;

import battlecode.common.*;

public class Solider implements GenericRobotContoller {
    boolean isBuildingRuin = false;
    boolean isBuildingSRP = false;
    boolean SRP_built = false;
    int cant_find_tower_for = 0;
    boolean[][] SRP_pattern;
    RobotController rc;
    Pathing pathing_engine;
    boolean buildPaintTowerNext = false;

    public Solider(RobotController handler) throws GameActionException {
        rc = handler;
        pathing_engine = new Pathing(handler);
        SRP_pattern = rc.getResourcePattern();
    }
    
    public void run() throws GameActionException {
        if (SRP_built == false && cant_find_tower_for > 40) {
            boolean early_exit = false;
            if (!rc.canMarkResourcePattern(rc.getLocation())) {
                early_exit = true;
            }
            if (rc.getChips() < 700) {
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
                        // boolean isPaintedbyAlly =mapInfo.getPaint().isAlly(); 
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
