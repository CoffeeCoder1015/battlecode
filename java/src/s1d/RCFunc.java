package s1d;

import battlecode.common.*;

@FunctionalInterface
public interface RCFunc {
    void p(RobotController r) throws GameActionException;
}