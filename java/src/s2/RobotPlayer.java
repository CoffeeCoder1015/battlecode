package s2;

import battlecode.common.*;
public class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        GenericRobotContoller processor;
        switch (rc.getType()) {
            case SOLDIER:
                processor = new Soldier(rc);
                break;
            case MOPPER:
                processor = new Mopper(rc);
                break;
            case SPLASHER:
                processor = new Splasher(rc) ;
                break; // Consider upgrading examplefuncsplayer to use splashers!
            default:
                processor = new Tower(rc);
                break;
        }

        while (true) {
            try {
                processor.run();
            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();


            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop
                // again.
                Clock.yield();
            }
        }
    }
}
