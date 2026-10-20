package jaxon.ric.ai;

/** Continuous swept-box collision checks, including sloping approaches past a canopy. */
public final class FlightGeometry {
    private FlightGeometry() {}

    public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {}

    public static boolean sweptIntersects(Box moving, FlightPathSearch.Point delta, Box obstacle) {
        double[] mins = {obstacle.minX - moving.maxX + 0.000001,
                obstacle.minY - moving.maxY + 0.000001, obstacle.minZ - moving.maxZ + 0.000001};
        double[] maxs = {obstacle.maxX - moving.minX - 0.000001,
                obstacle.maxY - moving.minY - 0.000001, obstacle.maxZ - moving.minZ - 0.000001};
        double[] movement = {delta.x(), delta.y(), delta.z()};
        double entry = 0.0, exit = 1.0;
        for (int axis = 0; axis < 3; axis++) {
            double speed = movement[axis];
            if (Math.abs(speed) < 0.00000001) {
                if (mins[axis] > 0 || maxs[axis] < 0) return false;
            } else {
                double first = mins[axis] / speed, second = maxs[axis] / speed;
                entry = Math.max(entry, Math.min(first, second));
                exit = Math.min(exit, Math.max(first, second));
                if (entry > exit) return false;
            }
        }
        return entry <= exit;
    }
}
