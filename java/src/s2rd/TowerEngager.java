//Includes attacking Logic and Sequence.

package s2rd;

import java.util.HashSet;

import battlecode.common.*;
//import s2.generics.GenericFunc;

//import java.util.Random;


public class TowerEngager {
    

    HashSet<MapLocation> enemyLocations = new HashSet<MapLocation>();
    

    RobotController rc;

    public TowerEngager(RobotController handler) {
        this.rc = handler;
    }

    public boolean engageEnemyTower() throws GameActionException {
        //System.out.println("Running tower engagement logic...");

        // Sense nearby map info in a certain radius
        MapInfo[] nearbyMapInfo = rc.senseNearbyMapInfos(-1);

        MapLocation enemyTowerLocation = null;

        // Identify the location of an enemy tower
        int minDistance = Integer.MAX_VALUE; // Initialize with a large value
        for (MapInfo mapInfo : nearbyMapInfo) {
            if (mapInfo.hasRuin()) {
                RobotInfo robot = rc.senseRobotAtLocation(mapInfo.getMapLocation());
                if (robot != null && robot.getTeam() != rc.getTeam()) {
                    int currentDistance = rc.getLocation().distanceSquaredTo(mapInfo.getMapLocation());
                    if (currentDistance < minDistance) {
                        // System.out.println("We found something Boss! A tower?");
                        minDistance = currentDistance; // Update the minimum distance
                        enemyTowerLocation = mapInfo.getMapLocation(); // Update the closest tower
                    }
                }
            }
            
        }

        // Add the closest tower to the HashSet if one was found
        if (enemyTowerLocation != null) {
            enemyLocations.add(enemyTowerLocation);
        }


        if (enemyTowerLocation == null) {
            //System.out.println("No enemy tower detected in range.");
            return false; // Exit the function if no enemy tower is found
        }

        // System.out.println("Enemy tower detected at: " + enemyTowerLocation);

        // Check if the enemy tower is already a ruin
        if (enemyLocations.contains(enemyTowerLocation) && rc.senseRobotAtLocation(enemyTowerLocation) == null) {
            // System.out.println("Enemy tower has been destroyed (turned into a ruin).");
            enemyLocations.remove(enemyTowerLocation); // Remove the tower's location from the HashSet
            return false; // Exit as there is no active enemy tower at this location
        }
        

        // Get the current robot's location
        MapLocation currentLocation = rc.getLocation();

        // Calculate a direction towards the enemy tower
        Direction towardsTower = currentLocation.directionTo(enemyTowerLocation);

        // Move one step towards the enemy tower if possible
        // Check if the robot is outside the tower's attack range but can still see it
        // System.out.println("Debug Before Cooldown Check: Movement=" + rc.getMovementCooldownTurns() + ", Action=" + rc.getActionCooldownTurns());
        // if (!rc.isActionReady() || !rc.isMovementReady()) { //IMPORTANT RUNNING WITH 0 IS OPTIMAL BUT SHOUDL TWEAK LATER
        //     // System.out.println("Movement cooldown: " + rc.getMovementCooldownTurns());
        //     // System.out.println("Cooldowns too high, skipping engagement. Action cooldown is:" + rc.getActionCooldownTurns());
        //     return true;
        // }
        
        
        if (currentLocation.distanceSquaredTo(enemyTowerLocation) > rc.getType().actionRadiusSquared) {
            if (rc.canMove(towardsTower) && rc.isActionReady()) {
                rc.move(towardsTower);
                // System.out.println("Moved towards enemy tower at: " + enemyTowerLocation);

                //Attacking sequence added here:
                // Attack the tower after moving closer
                if (rc.canAttack(enemyTowerLocation)) {
                    rc.attack(enemyTowerLocation); // Attack the tower
                    // System.out.println("Attacked enemy tower after moving closer: " + enemyTowerLocation);
                }

                return true; // Exit after taking one step
            }
        }

        // If the robot is within the tower's attack range, move one step away
        if (currentLocation.distanceSquaredTo(enemyTowerLocation) <= rc.getType().actionRadiusSquared) {
            Direction awayFromTower = enemyTowerLocation.directionTo(currentLocation);
            if (rc.canMove(awayFromTower)) {

                //Attack before leaving sequence here:
               
                if (rc.canAttack(enemyTowerLocation)) {
                    rc.attack(enemyTowerLocation); // Attack the tower
                    // System.out.println("Attacked enemy tower before retreating: " + enemyTowerLocation);
                }

                rc.move(awayFromTower);
                // System.out.println("Moved away from enemy tower to avoid attack.");
                return true; // Move away from the tower successfully
            }
        }

        return false;

    }
}

