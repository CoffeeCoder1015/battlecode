package s2rd;

import battlecode.common.*;
import s2.generics.GenericFunc;

import java.util.Random;

public class Pathing {
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


    static final Random rng = new Random(6147);

    static int robot_dir_idx = -1; //also technically velocity
    
    static RobotController rc;

    static private int modulo(int x, int y) {
        int temp = Math.floorDiv(x,y);
        return x-temp*y;
    }

    public Pathing(RobotController handler) throws GameActionException{
        rc = handler;
    }

    public void Move() throws GameActionException{
    }

}
