package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;

/*
 * A fascinating tool our main drone has at its disposal - To plan its precise
 * path to its current destination, before actually flying anywhere, it can
 * command a "shadow drone". Its shadow drone has the same flight properties as
 * the main drone, but e.g. it does not need to obey any "Max Moves"
 * restrictions, and we can change its current position at will.
 */
public class ShadowDrone extends Drone {

    public ShadowDrone(double longitude, double latitude) {
        super(longitude, latitude);
    }

    @Override
    public boolean canGetToDestinationInStraightLine(Point destination, double maxFinalDistance) {

        while (!this.isInRangeOfPoint(destination, maxFinalDistance)) {
            
            //if (this.stepsMade > 40) return false; // TODO
            
            System.out.println();
            System.out.println("Position: " + this.currentPosition);
            System.out.println("Distance: " + EuclideanUtils.computeDistance(this.currentPosition, destination));
            
            var straightPath = new LineSegment(this.currentPosition, destination);
            var exactAngle = straightPath.getAngleInDegrees();
            /*
             * Because our drone can only move at angles that are multiples of 10, we round
             * accordingly.
             */
            double scaledAngle = exactAngle / ANGLE_GRANULARITY; // e.g. 17.4 for 174 degrees and a granularity of 10
            int roundedAngle = (ANGLE_GRANULARITY * (int) Math.rint(scaledAngle) + 360) % 360; // e.g. 170 for the above

            /*
             * Try to approach the destination in the most straight-forward way. If we can, we do it. 
             * If we cannot, this approach evidently does not work and we return false. 
             */
            var nextPos = EuclideanUtils.getNextPosition(this.currentPosition, roundedAngle, MOVE_DISTANCE);
            if (this.canMove(nextPos)) {
                this.makeMove(nextPos);
            } else {
                System.out.println("Failed after " + this.stepsMade + " steps!");
                return false;
            }

        }

        /*
         * If we got here, the shadow drone is now in range of the destination, having
         * gotten there without ever adjusting its course due to obstacles.
         */
        
        System.out.println("Arrived at the destination in " + this.stepsMade + " steps!");
        return true;
    }

    public void setStepsMade(int stepsMade) {
        this.stepsMade = stepsMade;
    }

    public void setPosition(Point position) {
        this.currentPosition = position;
    }

}
