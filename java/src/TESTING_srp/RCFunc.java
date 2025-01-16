package TESTING_srp;

import battlecode.common.*;

@FunctionalInterface
public interface RCFunc {
    void p(RobotController r) throws GameActionException;
}