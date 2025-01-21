package s2rd;

import battlecode.common.*;

import java.util.Random;

public class Pathing {
    final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };
    // boids
    // rule
    // 1. Steer away from nearby robots
    // 2. align direction with nearby robots
    // 3. Steer towards the center of mass * (I dont think this is good for our case)


    // diffusion core
    // 1. Each particle must have a energy
    // 2. Particles glide away from nearby robots
    // 3. Particles lose energy from collsion 
    
    // Robots receieve an initial energy and direction from their spawn tower 
    // This ensures the robots are in a field pushing them away from the tower
    // When this energy reaches 0 as it collides with objects we hand back the control to the robot itself 

    final Random rng = new Random(234922929);

    int robot_dir_idx = -1; //also technically velocity
    int energy = 40;
    
    RobotController rc;
    
    MapLocation currentLocation;
    
    RobotInfo[] rbInfo;
    MapInfo[] infos;

    public Pathing(RobotController handler) throws GameActionException{
        rc = handler;
        getInitalDir();
    }

    public void Move() throws GameActionException{
        rbInfo = rc.senseNearbyRobots(-1);
        infos = rc.senseNearbyMapInfos(8);
        currentLocation = rc.getLocation();
        Diffuse();
    }

    private int modulo(int x, int y) {
        int temp = Math.floorDiv(x,y);
        return x-temp*y;
    }

    private void getInitalDir() throws GameActionException{
        MapInfo[] infos = rc.senseNearbyMapInfos(2);
        MapLocation closestTower = null;
        for (MapInfo mapInfo : infos) {
            if (mapInfo.hasRuin()) {
               closestTower = mapInfo.getMapLocation(); 
            }
        }
        if (closestTower == null) {
           return; 
        }
        Direction InitialDirection  =closestTower.directionTo(rc.getLocation());
        robot_dir_idx = DirToIndex(InitialDirection);
    }
    
    private int DirToIndex(Direction dir) {
        switch (dir) {
            case NORTH:
               return  0;
            case NORTHEAST:
                return 1;
            case EAST:
                return 2;
            case SOUTHEAST:
                return 3;
            case SOUTH:
                return 4;
            case SOUTHWEST:
                return 5;
            case WEST:
                return 6;
            case NORTHWEST:
                return 7;
            default:
                break;
        }
        return 0;
    } 

    private void Diffuse() throws GameActionException{
        Direction goalDirection = directions[robot_dir_idx];
        MapLocation currentLocation = rc.getLocation();
        MapLocation targetLocation = currentLocation.add(goalDirection);
        boolean canMove =rc.canMove(goalDirection);

        if (canMove) {
            rc.move(goalDirection); 
            return;
        }
    
        calculateNextIdx(targetLocation);        
    }
    private void calculateNextIdx(MapLocation targetLocation) throws GameActionException{
        // collision has occurred
        // if wall or ruin then energy decrease to 0
        // else decrease by 1
        boolean isWallLike = false;
        if (rc.canSenseLocation(targetLocation)) {
            MapInfo infos = rc.senseMapInfo(targetLocation);
            isWallLike = infos.isWall();
        }
        if (getDirRobotCount() > 14) {
            isWallLike = true;
        }
        if (treatAllyTileAsWall()) {
            isWallLike = true;
        }
        if (isWallLike) {
            energy = 0;
        } else {
            energy--;
        }
        switch (energy) {
            case 0: // wall or ruin collision
                // bounce back
                energy = rng.nextInt(15) + 20;
                robot_dir_idx = modulo(robot_dir_idx + 2*rng.nextInt(2), 8);
                break;
            default: // other robot collision
                // dodge
                int decide = rng.nextInt(5);
                int amount = rng.nextInt(1, 3);
                if (decide < 3) {
                    if (decide <= 1) {
                        robot_dir_idx = modulo(robot_dir_idx + amount, 8);
                    } else {
                        robot_dir_idx = modulo(robot_dir_idx - amount, 8);
                        break;
                    }
                }
                break;
        }       
    }
    
    private int getDirRobotCount() throws GameActionException{
        int rbCount = 0;
        for (RobotInfo rb: rbInfo) {
            if (rb.getTeam() != rc.getTeam() || rc.getType().isTowerType()) {
               continue; 
            }
            MapLocation loc = rb.getLocation();
            Direction dirTo = rc.getLocation().directionTo(loc);
            int idx  = DirToIndex(dirTo);
            int idx_dist_to_current = modulo(robot_dir_idx - idx, 8);
            if(0<=idx_dist_to_current && idx_dist_to_current<=2){
                // rc.setIndicatorLine( loc,currentLocation, 23,177,23);
                rbCount++;
            }
        }
        return rbCount;
    }
    
    private boolean treatAllyTileAsWall() throws GameActionException{
        int infront = 0;
        int behind = 0;
        for (MapInfo mapInfo: infos) {
            if (!mapInfo.getPaint().isAlly()) {
               continue; 
            }
            MapLocation loc = mapInfo.getMapLocation();
            Direction dirTo = rc.getLocation().directionTo(loc);
            int idx  = DirToIndex(dirTo);
            int idx_dist_to_current = modulo(robot_dir_idx - idx, 8);
            if(0<=idx_dist_to_current && idx_dist_to_current<=2){
                // rc.setIndicatorLine( loc,currentLocation, 120,188,233);
                infront++;
            }else{
                behind++;
            }
        }
        return infront>behind;
    }
}
