package s2;

import battlecode.common.*;
import s2.generics.GenericRobotContoller;

public class Splasher implements GenericRobotContoller {
   RobotController  rc;
   Pathing pathing_engine;
   public Splasher(RobotController handler)  throws GameActionException{
        rc = handler;
        pathing_engine = new Pathing(handler);
   }

   public void run() throws GameActionException{
        // Sense information about all visible nearby tiles.
        // Move and attack randomly if no objective.
        pathing_engine.Move();
        // Try to paint beneath us as we walk to avoid paint penalties.
        // Avoiding wasting paint by re-painting our own tiles.
        // MapInfo[] info = rc.senseNearbyMapInfos(3);
        // for (MapInfo mapInfo : info) {
        //    if(mapInfo.getPaint().isAlly()) {
        //        return;
        //    }
        // }
        MapInfo currentTile = rc.senseMapInfo(rc.getLocation());
        if (!currentTile.getPaint().isAlly() && rc.canAttack(rc.getLocation())) {
            rc.attack(rc.getLocation());
        }
   }
}
