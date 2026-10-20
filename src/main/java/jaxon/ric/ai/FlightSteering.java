package jaxon.ric.ai;

/** Smooth steering at the adult ghast's normal average air cruising speed. */
public final class FlightSteering {
    private FlightSteering() {}

    public static double cruisingSpeed(double flyingAttribute) {
        // Vanilla adds flyingSpeed * 5/3 every 2..6 ticks (mean 4), with 0.91 air drag.
        return flyingAttribute * (5.0 / 3.0) / (4.0 * (1.0 - 0.91));
    }

    public static FlightPathSearch.Point nextVelocity(FlightPathSearch.Point current,
                                                       FlightPathSearch.Point displacement, double flyingAttribute) {
        double distance = displacement.distanceTo(new FlightPathSearch.Point(0, 0, 0));
        if (distance < 0.0001) return new FlightPathSearch.Point(0, 0, 0);
        double cap = cruisingSpeed(flyingAttribute);
        double speed = Math.min(cap, distance * 0.5);
        double x = current.x() * 0.35 + displacement.x() / distance * speed * 0.65;
        double y = current.y() * 0.35 + displacement.y() / distance * speed * 0.65;
        double z = current.z() * 0.35 + displacement.z() / distance * speed * 0.65;
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length > cap && length > 0) {
            x *= cap / length;
            y *= cap / length;
            z *= cap / length;
        }
        return new FlightPathSearch.Point(x, y, z);
    }
}
