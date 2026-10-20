package jaxon.ric.ai;

/** Uses entity positions, as vanilla ranged attack behavior does, with room to start charging. */
public final class RiderAttackRange {
    private RiderAttackRange() {}

    public static double approachRange(double weaponRange) {
        return Math.max(0.5, weaponRange - 2.0);
    }

    public static boolean contains(FlightPathSearch.Point riderPosition, FlightPathSearch.Point targetPosition,
                                   double weaponRange) {
        return riderPosition.distanceTo(targetPosition) <= approachRange(weaponRange);
    }
}
