package s2;

import battlecode.common.*;

@FunctionalInterface
public interface RCFunc {
    void p(RobotController r) throws GameActionException;
}