package s2;

import battlecode.common.*;
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

    GenericFunc diffuse = Pathing::diffuse1; 

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
        diffuse = Pathing::diffuse1;
        int bias = rng.nextInt(5);
        if (bias == 3) {
            diffuse = Pathing::diffuse2;
        }
        check_dir();
        diffuse.p();
    }

    private void check_dir() {
        if (robot_dir_idx == -1) {
            robot_dir_idx = rng.nextInt(8);
        }
    }

    private static void diffuse2() throws GameActionException{
        MapLocation current_location = rc.getLocation();
        boolean CurrIsAlly = rc.senseMapInfo(current_location).getPaint().isAlly();
        for (int i = 0; i < 6; i++) {
                Direction goal_dir = directions[robot_dir_idx];
                if (rc.canMove(goal_dir) ){
                    boolean NextIsAlly = rc.senseMapInfo(current_location.add(goal_dir)).getPaint().isAlly();
                    if (!(!CurrIsAlly && NextIsAlly)) {
                        rc.move(goal_dir);
                        return;
                    }
                } 
                int adj1 = modulo(robot_dir_idx + 1, 8);
                int adj2 = modulo(robot_dir_idx - 1, 8);
                boolean adj1_canMove = rc.canMove(directions[adj1]);
                boolean adj2_canMove = rc.canMove(directions[adj2]);
                if (robot_dir_idx % 2 == 0 || (!adj1_canMove && !adj2_canMove)) {
                    robot_dir_idx = modulo(robot_dir_idx + 3 + rng.nextInt(3), 8);
                } else {
                    if (adj1_canMove) {
                        robot_dir_idx = adj1;
                    } else { // no need for 2nd check because if both fail then it would have gone into the
                             // previous if statement
                        robot_dir_idx = adj2;
                    }
                }
        }
        // if stuck in hole
        for (int i = 0; i < 10; i++) {
            robot_dir_idx = rng.nextInt(8);
            Direction goal_dir = directions[robot_dir_idx];
            if (rc.canMove(goal_dir)) {
                rc.move(goal_dir);
            }
        }
    }

    private static void diffuse1() throws GameActionException{
        MapInfo[] surrounding = rc.senseNearbyMapInfos(2);
        int surround_count = 0;
        for (MapInfo mapInfo : surrounding) {
           if (mapInfo.getPaint().isAlly()) {
               surround_count++; 
           }
        }
        for (int i = 0; i < 8; i++) {
                Direction goal_dir = directions[robot_dir_idx];
                if (rc.canMove(goal_dir))  {
                    rc.move(goal_dir);
                    break;
                } 
                int adj1 = modulo(robot_dir_idx + 1, 8);
                int adj2 = modulo(robot_dir_idx - 1, 8);
                boolean adj1_canMove = rc.canMove(directions[adj1]);
                boolean adj2_canMove = rc.canMove(directions[adj2]);
                if (robot_dir_idx % 2 == 0 || (!adj1_canMove && !adj2_canMove)) {
                    robot_dir_idx = modulo(robot_dir_idx + 3 + rng.nextInt(3), 8);
                } else {
                    if (adj1_canMove) {
                        robot_dir_idx = adj1;
                    } else { // no need for 2nd check because if both fail then it would have gone into the
                             // previous if statement
                        robot_dir_idx = adj2;
                    }
                }
                boolean next_is_ally = rc.canMove(directions[robot_dir_idx]) && rc.senseMapInfo(rc.getLocation().add(directions[robot_dir_idx])).getPaint().isAlly();
                boolean is_surrounded = surround_count > 3;
                if (next_is_ally && !is_surrounded) {
                    int shift = rng.nextInt(7)+1;
                    int next = modulo( robot_dir_idx + shift ,8);
                    if (next == robot_dir_idx) {
                        next++;
                    }
                    robot_dir_idx = next;
                }
            }
    }


}
