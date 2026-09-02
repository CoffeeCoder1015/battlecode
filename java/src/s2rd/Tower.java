package s2rd;
import java.util.Random;
import battlecode.common.*;
import s2rd.generics.GenericRobotContoller;

public class Tower implements GenericRobotContoller {

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

    final Random rng = new Random(6147);
    
    RobotController rc;

    int rtype = 0;

    int[] spawn_count  = {0,0,0};
    int[] target_count  = {3,1,1};

    public Tower(RobotController handler) throws GameActionException{
        rc = handler;
    }


    public void run() throws GameActionException {
        // Check for surplus and spawn soldier if conditions are met
        // System.out.println("This is the money: " + rc.getMoney());
        // System.out.println("This is the paint: " + rc.getPaint());

        // if (rc.getMoney() > 5000 && rc.getPaint() > 300) {
        // for (Direction dir : directions) {
        // MapLocation spawnLoc = rc.getLocation().add(dir);
        // if (rc.canBuildRobot(UnitType.SOLDIER, spawnLoc)) {
        // rc.buildRobot(UnitType.SOLDIER, spawnLoc);
        // System.out.println("Surplus soldier spawned at: " + spawnLoc);
        // break; // Spawn only one soldier
        // }
        // }
        MapInfo[] infos = rc.senseNearbyMapInfos(-1);
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        int e_count = 0;
        int a_count = 0;
        for (MapInfo mapInfo: infos) {
            if (mapInfo.getPaint().isEnemy()) {
               e_count++; 
            }
            if (mapInfo.getPaint().isAlly()) {
               a_count++; 
            }
        }
        boolean produce = true;
        if (a_count > 60 && rc.getRoundNum() > 250) {
           produce = rc.getChips() > 2000; 
        }
        if(produce) {
            if (e_count > 8 || nearbyEnemies.length > 3) {
                rtype = 2;
            }

            // Pick a direction to build in.
            MapLocation currentLocation = rc.getLocation();
            int y_ref = rc.getMapHeight() - currentLocation.y;
            int x_ref = rc.getMapHeight() - currentLocation.x;
            MapLocation HReflect = new MapLocation(currentLocation.x, y_ref);
            MapLocation VReflect = new MapLocation(x_ref, currentLocation.y);
            MapLocation RReflect = new MapLocation(x_ref, y_ref);
            Direction dir = null;
            switch (rc.getRoundNum() % 6) {
                case 0:
                    dir = currentLocation.directionTo(HReflect);
                    break;
                case 1:
                    dir = currentLocation.directionTo(VReflect);
                    break;
                case 2:
                    dir = currentLocation.directionTo(RReflect);
                    break;
                default:
                    dir = directions[rng.nextInt(directions.length)];
                    break;
            }

            MapLocation nextLoc = rc.getLocation().add(dir);
            // Pick a random robot type to build.
            int robotType = rtype;
            if (robotType == 0 && rc.canBuildRobot(UnitType.SOLDIER, nextLoc)) {
                rc.buildRobot(UnitType.SOLDIER, nextLoc);
                spawn_count[0]++;
            } else if (robotType == 1 && rc.canBuildRobot(UnitType.SPLASHER, nextLoc)) {
                rc.buildRobot(UnitType.SPLASHER, nextLoc);
                spawn_count[1]++;
            } else if (robotType == 2 && rc.canBuildRobot(UnitType.MOPPER, nextLoc)) {
                rc.buildRobot(UnitType.MOPPER, nextLoc);
                spawn_count[2]++;
            }
            // management of rtype relative to chip count;
            if (spawn_count[rtype] >= target_count[rtype]) {
                rtype++;
            }
            if (rtype > 2) {
                rtype = 0;
                spawn_count[0] = 0;
                spawn_count[1] = 0;
                spawn_count[2] = 0;
            }
            int chipCount = rc.getChips();
            if (chipCount > 10_000) {
                target_count[0] = 5;
                target_count[1] = 2;
                target_count[2] = 1; // Add this line for Moppers
            }
            if (chipCount < 650) {
                target_count[0] = 3;
                target_count[1] = 1;
                target_count[2] = 1;
            }
        }

        // Attack logic for Tower

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
    }

    // Helper method to determine if one robot is a higher priority target than another
    private boolean isHigherPriority(RobotInfo current, RobotInfo target) {
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


}
