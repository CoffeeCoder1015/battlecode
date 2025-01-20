package s2rd;

import battlecode.common.*;
import s2rd.generics.GenericFunc;

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


    final Random rng = new Random(6147);

    int robot_dir_idx = -1; //also technically velocity
    int energy = 35;
    
    RobotController rc;

    public Pathing(RobotController handler) throws GameActionException{
        rc = handler;
        getInitalDir();
    }

    public void Move() throws GameActionException{
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
        Direction InitalDirection  =closestTower.directionTo(rc.getLocation());
        for (int i = 0; i < 8; i++) {
           if (directions[i] == InitalDirection) {
                robot_dir_idx = i; 
                break;
           } 
        }
    }


}
