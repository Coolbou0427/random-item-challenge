package jaxon.ric.ai;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/** Dependency-free geometry regressions; run with FlightPathSearch on the javac/java classpath. */
public final class FlightPathSearchTest {
    private static final double RIG_BOTTOM = 5.55;
    private static final double RIG_TOP = 5.05;

    public static void main(String[] args) {
        descendsEvenWhenHorizontallyClose();
        routesAroundCanopyToLowShot();
        largerGhastRoutesAroundCanopy();
        canInitiallyMoveAwayFromTarget();
        rejectsDoorwayTooNarrowForAssembly();
        checksSpaceBetweenGridPoints();
        boundsUnreachableSearch();
        respectsPerTickBudget();
        reservesRoomInsideActualCrossbowRange();
        choosesDirectApproachInOpenSpace();
        climbsOverCanopyBeforeDescending();
        retainsLateralFallbackWhenClimbIsBlocked();
        diagonalSweepDoesNotFillEmptySpace();
        separateBodiesLeaveRopeSpaceOpen();
        beginsDescentBeforeFinishingCrossing();
        steeringRespondsWithoutIncreasingCruisingSpeed();
        smallerClearanceFitsSafeGap();
        System.out.println("PASS: 17 flight routing/range/steering scenarios");
    }

    private static void descendsEvenWhenHorizontallyClose() {
        Geometry world = new Geometry(List.of(), 3.05, false);
        FlightPathSearch.Point start = p(6, 24, 0), player = p(0, 1.6, 0);
        Predicate<FlightPathSearch.Point> firing = point -> canShoot(world, point, player);
        check(!firing.test(start), "Being horizontally close must not count as firing from too high up");
        FlightPathSearch search = solve(start, world, firing, point -> point.distanceTo(p(6, 7, 0)), 2, 6000);
        check(search.reachedGoal(), "Open-air descent must reach a firing position");
        FlightPathSearch.Point end = last(search);
        check(end.y() < 10 && end.y() >= RIG_BOTTOM, "Descent must get low without grounding the boat");
        validate(start, search, world);
    }

    private static void routesAroundCanopyToLowShot() {
        Geometry world = new Geometry(List.of(
                new Box(-0.5, 0, -0.5, 0.5, 14, 0.5),
                new Box(-5, 9, -5, 5, 15, 5)), 3.05, false);
        FlightPathSearch.Point start = p(-10, 16, 0), player = p(5, 1.6, 0);
        FlightPathSearch search = solve(start, world, point -> canShoot(world, point, player),
                point -> Math.max(0, point.distanceTo(p(4, 7, 0)) - 8), 2, 6000);
        check(search.reachedGoal(), "A tree with open space around it must not stop the hunt");
        check(last(search).y() < 10, "The route must descend rather than stop above the canopy");
        check(world.canOccupy(last(search)), "The complete rig must fit at the firing position");
        validate(start, search, world);
    }

    private static void canInitiallyMoveAwayFromTarget() {
        Geometry world = new Geometry(List.of(
                new Box(-2, 0, -10, 0, 20, 10),
                new Box(-14, 0, -10, 0, 20, -8),
                new Box(-14, 0, 8, 0, 20, 10)), 3.05, true);
        FlightPathSearch.Point start = p(-8, 6, 0), goal = p(12, 6, 0);
        FlightPathSearch search = solve(start, world, point -> point.distanceTo(goal) < 0.1,
                point -> point.distanceTo(goal), 2, 6000);
        check(search.reachedGoal(), "A U-shaped obstacle requires a route through its rear opening");
        check(search.path().stream().anyMatch(point -> point.x() <= -18),
                "The route must be able to move away from the player before going around an obstacle");
        validate(start, search, world);
    }

    private static void largerGhastRoutesAroundCanopy() {
        Geometry world = new Geometry(List.of(
                new Box(-0.5, 0, -0.5, 0.5, 14, 0.5),
                new Box(-5, 9, -5, 5, 15, 5)), 5.05, false);
        FlightPathSearch.Point start = p(-12, 18, 0), player = p(8, 1.6, 0);
        FlightPathSearch search = solve(start, world, point -> canShoot(world, point, player),
                point -> Math.max(0, point.distanceTo(p(4, 7, 0)) - 8), 2, 6000);
        check(search.reachedGoal(), "An enlarged ghast must use a route that fits its wider and taller body");
        check(last(search).y() < 10, "The enlarged variant must still descend to firing height");
        validate(start, search, world);
    }

    private static void rejectsDoorwayTooNarrowForAssembly() {
        List<Box> wall = List.of(new Box(-1, 0, -40, 1, 20, -2), new Box(-1, 0, 2, 1, 20, 40));
        FlightPathSearch.Point start = p(-12, 6, 0), goal = p(12, 6, 0);
        Geometry small = new Geometry(wall, 0.5, true);
        Geometry large = new Geometry(wall, 3.05, true);
        FlightPathSearch smallSearch = solve(start, small, point -> point.distanceTo(goal) < 0.1,
                point -> point.distanceTo(goal), 2, 3000);
        FlightPathSearch largeSearch = solve(start, large, point -> point.distanceTo(goal) < 0.1,
                point -> point.distanceTo(goal), 2, 3000);
        check(smallSearch.reachedGoal(), "The test doorway must be open to a smaller entity");
        check(!largeSearch.reachedGoal(), "The same doorway must reject the large hanging assembly");
        validate(start, largeSearch, large);
    }

    private static void checksSpaceBetweenGridPoints() {
        Geometry world = new Geometry(List.of(new Box(-0.1, 0, -3, 0.1, 20, 3)), 1, true);
        FlightPathSearch.Point start = p(-6, 6, 0), goal = p(6, 6, 0);
        FlightPathSearch search = solve(start, world, point -> point.distanceTo(goal) < 0.1,
                point -> point.distanceTo(goal), 4, 3000);
        check(search.reachedGoal(), "A thin wall must be routed around, even between grid points");
        check(search.path().stream().anyMatch(point -> Math.abs(point.z()) >= 4), "No tunnelling through a thin wall");
        validate(start, search, world);
    }

    private static void boundsUnreachableSearch() {
        Geometry world = new Geometry(List.of(new Box(-1, 0, -40, 1, 40, 40)), 3.05, true);
        FlightPathSearch.Point goal = p(12, 6, 0);
        FlightPathSearch search = solve(p(-12, 6, 0), world, point -> point.distanceTo(goal) < 0.1,
                point -> point.distanceTo(goal), 2, 120);
        check(!search.reachedGoal() && search.expandedNodes() <= 120,
                "An unreachable target must terminate within its total planning budget");
    }

    private static void respectsPerTickBudget() {
        Geometry world = new Geometry(List.of(), 3.05, false);
        FlightPathSearch.Point goal = p(20, 8, 0);
        FlightPathSearch search = new FlightPathSearch(p(-20, 24, 0), world,
                point -> point.distanceTo(goal) < 0.1, point -> point.distanceTo(goal), 2, 6000);
        int turns = 0;
        while (!search.isFinished() && turns++ < 3000) {
            int before = search.expandedNodes();
            search.advance(3);
            check(search.expandedNodes() - before <= 3, "A planner step exceeded its server tick budget");
        }
        check(search.reachedGoal(), "Incremental planning must resume until it reaches the goal");
    }

    private static FlightPathSearch solve(FlightPathSearch.Point start, Geometry space,
                                          Predicate<FlightPathSearch.Point> goal,
                                          ToDoubleFunction<FlightPathSearch.Point> estimate, double step, int limit) {
        FlightPathSearch search = new FlightPathSearch(start, space, goal, estimate, step, limit);
        for (int turn = 0; !search.isFinished() && turn < 1000; turn++) search.advance(64);
        check(search.isFinished(), "Search did not finish");
        return search;
    }

    private static boolean canShoot(Geometry space, FlightPathSearch.Point point, FlightPathSearch.Point player) {
        FlightPathSearch.Point feet = p(point.x(), point.y() - 3.0, point.z());
        FlightPathSearch.Point playerFeet = p(player.x(), player.y() - 1.6, player.z());
        FlightPathSearch.Point eye = p(point.x(), point.y() - 1.2, point.z());
        return RiderAttackRange.contains(feet, playerFeet, 8) && eye.y() <= player.y() + 4
                && Math.hypot(eye.x() - player.x(), eye.z() - player.z()) >= 3
                && space.obstacles.stream().noneMatch(box -> box.blocksRay(eye, player));
    }

    private static void reservesRoomInsideActualCrossbowRange() {
        FlightPathSearch.Point player = p(0, 0, 0);
        // Minecraft 26.3 CrossbowItem.getDefaultProjectileRange() returns 8.
        check(RiderAttackRange.approachRange(8) == 6, "Approach must leave room inside the real firing range");
        check(!RiderAttackRange.contains(p(14, 0, 0), player, 8), "The old 14-block hover range cannot start an attack");
        check(!RiderAttackRange.contains(p(7.9, 0, 0), player, 8), "Do not hover at the very edge of weapon range");
        check(RiderAttackRange.contains(p(5.9, 0, 0), player, 8), "A comfortable open-field approach must be accepted");
        check(!RiderAttackRange.contains(p(5, 4, 0), player, 8), "Include vertical separation in actual rider range");
        check(RiderAttackRange.contains(p(4, 3, 0), player, 8), "A close elevated rider should be in range");
    }

    private static void choosesDirectApproachInOpenSpace() {
        Geometry world = new Geometry(List.of(), 3.05, false);
        FlightPathSearch.Point start = p(-12, 14, 0), goal = p(8, 6, 0);
        List<FlightPathSearch.Point> route = FlightPathSearch.directOrOverflight(start, List.of(goal), world, 24);
        check(route.equals(List.of(goal)), "Open ground should use a direct closing approach");
        validateRoute(start, route, world);
    }

    private static void climbsOverCanopyBeforeDescending() {
        Geometry world = new Geometry(List.of(new Box(-4, 9, -14, 4, 15, 14)), 3.05, false);
        FlightPathSearch.Point start = p(-10, 12, 0), goal = p(10, 6, 0);
        List<FlightPathSearch.Point> route = FlightPathSearch.directOrOverflight(start, List.of(goal), world, 24);
        check(route.size() == 3, "An obstructing canopy should produce climb/across/descent waypoints");
        check(route.get(0).x() == start.x() && route.get(0).z() == start.z(), "Climb before crossing the canopy");
        check(route.get(0).y() - RIG_BOTTOM >= 15, "The hanging boat must also clear the top of the leaves");
        check(route.get(1).x() == goal.x() && route.get(1).y() == route.get(0).y(), "Cross above the tree to the target side");
        check(route.get(2).equals(goal), "Descend at the far side to the close firing position");
        double length = start.distanceTo(route.get(0)) + route.get(0).distanceTo(route.get(1)) + route.get(1).distanceTo(goal);
        check(length < 56, "This overflight should be shorter than circling the broad canopy");
        validateRoute(start, route, world);
    }

    private static void retainsLateralFallbackWhenClimbIsBlocked() {
        Geometry world = new Geometry(List.of(new Box(-16, 12, -4, -8, 14, 4),
                new Box(-1, 0, -5, 1, 20, 5)), 3.05, false);
        FlightPathSearch.Point start = p(-12, 6, 0), goal = p(12, 6, 0);
        check(FlightPathSearch.directOrOverflight(start, List.of(goal), world, 24).isEmpty(),
                "Do not climb through an overhead obstacle");
        FlightPathSearch fallback = solve(start, world, point -> point.distanceTo(goal) < 0.1,
                point -> point.distanceTo(goal), 2, 6000);
        check(fallback.reachedGoal(), "A blocked climb must retain the safe lateral path search");
        validate(start, fallback, world);
    }

    private static void validateRoute(FlightPathSearch.Point start, List<FlightPathSearch.Point> route, Geometry space) {
        FlightPathSearch.Point previous = start;
        for (FlightPathSearch.Point point : route) {
            check(space.canOccupy(point), "A direct/overflight waypoint intersects terrain");
            check(space.canMove(previous, point), "A direct/overflight leg intersects terrain");
            previous = point;
        }
    }

    private static void diagonalSweepDoesNotFillEmptySpace() {
        FlightGeometry.Box body = new FlightGeometry.Box(0, 10, 0, 2, 12, 2);
        FlightGeometry.Box belowStart = new FlightGeometry.Box(0, 0, 0, 2, 2, 2);
        FlightGeometry.Box onSlope = new FlightGeometry.Box(5, 5, 0, 7, 7, 2);
        check(!FlightGeometry.sweptIntersects(body, p(10, -10, 0), belowStart),
                "A diagonal descent must not collide with the empty corner of its broad bounding box");
        check(FlightGeometry.sweptIntersects(body, p(10, -10, 0), onSlope),
                "An actual obstacle along that same sloping path must be rejected");
    }

    private static void separateBodiesLeaveRopeSpaceOpen() {
        FlightGeometry.Box ghast = new FlightGeometry.Box(-2, 8, -2, 2, 12, 2);
        FlightGeometry.Box boat = new FlightGeometry.Box(-1, 4, -1, 1, 5, 1);
        FlightGeometry.Box emptyRopeCorner = new FlightGeometry.Box(1.5, 5, 0, 2, 7, 1);
        check(!FlightGeometry.sweptIntersects(ghast, p(1, 0, 0), emptyRopeCorner)
                        && !FlightGeometry.sweptIntersects(boat, p(1, 0, 0), emptyRopeCorner),
                "An obstacle beside the rope must not be treated as touching either real body");
    }

    private static void beginsDescentBeforeFinishingCrossing() {
        Geometry world = new Geometry(List.of(new Box(-4, 9, -14, 4, 15, 14)), 3.05, false);
        FlightPathSearch.Point start = p(0, 22, 0);
        List<FlightPathSearch.Point> route = List.of(start, p(10, 22, 0), p(10, 6, 0));
        FlightPathSearch.RouteAim aim = FlightPathSearch.lookAhead(start, route, 1, world);
        check(aim != null && aim.index() == 2 && aim.position().x() > start.x() && aim.position().y() < start.y(),
                "Start descending across the canopy as soon as a safe sloping segment opens");
        check(world.canMove(start, aim.position()), "The early diagonal descent must still clear the whole body");
    }

    private static void steeringRespondsWithoutIncreasingCruisingSpeed() {
        double cap = FlightSteering.cruisingSpeed(0.05);
        FlightPathSearch.Point velocity = FlightSteering.nextVelocity(p(0, 0, 0), p(10, -10, 0), 0.05);
        check(velocity.x() > 0 && velocity.y() < 0, "Respond in both horizontal and vertical directions on the first tick");
        for (int tick = 0; tick < 100; tick++) {
            velocity = FlightSteering.nextVelocity(velocity, p(10, -10, 0), 0.05);
            check(velocity.distanceTo(p(0, 0, 0)) <= cap + 0.000001, "Never exceed the vanilla mean air cruise speed");
        }
        for (int tick = 0; tick < 3; tick++) velocity = FlightSteering.nextVelocity(velocity, p(-10, 10, 0), 0.05);
        check(velocity.x() < 0 && velocity.y() > 0, "Course reversal must respond within three ticks");
        FlightPathSearch.Point stopped = FlightSteering.nextVelocity(velocity, p(0, 0, 0), 0.05);
        check(stopped.distanceTo(p(0, 0, 0)) == 0, "Arrival must not retain velocity toward an obstacle");
    }

    private static void smallerClearanceFitsSafeGap() {
        List<Box> wall = List.of(new Box(-1, 0, -40, 1, 20, -2.5), new Box(-1, 0, 2.5, 1, 20, 40));
        Geometry world = new Geometry(wall, 2.25, true);
        FlightPathSearch.Point start = p(-12, 6, 0), goal = p(12, 6, 0);
        FlightPathSearch search = solve(start, world, point -> point.distanceTo(goal) < 0.1,
                point -> point.distanceTo(goal), 2, 3000);
        check(search.reachedGoal(), "A four-block body with quarter-block clearance must fit a five-block opening");
        validate(start, search, world);
    }

    private static void validate(FlightPathSearch.Point start, FlightPathSearch search, Geometry space) {
        FlightPathSearch.Point previous = start;
        for (FlightPathSearch.Point point : search.path()) {
            check(space.canOccupy(point), "Route intersects a block with the rig's full dimensions");
            check(space.canMove(previous, point), "A route edge cuts through terrain");
            previous = point;
        }
    }

    private static FlightPathSearch.Point last(FlightPathSearch search) {
        check(!search.path().isEmpty(), "Expected a nonempty route");
        return search.path().getLast();
    }

    private static FlightPathSearch.Point p(double x, double y, double z) { return new FlightPathSearch.Point(x, y, z); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private record Geometry(List<Box> obstacles, double halfWidth, boolean fixedHeight) implements FlightPathSearch.Space {
        private Box rig(FlightPathSearch.Point point) {
            return new Box(point.x() - halfWidth, point.y() - RIG_BOTTOM, point.z() - halfWidth,
                    point.x() + halfWidth, point.y() + Math.max(RIG_TOP, (halfWidth - 1.05) * 2 + 1.05),
                    point.z() + halfWidth);
        }

        public boolean canOccupy(FlightPathSearch.Point point) {
            Box box = rig(point);
            return point.x() >= -32 && point.x() <= 32 && point.z() >= -30 && point.z() <= 30
                    && box.minY >= 0 && box.maxY < 60 && (!fixedHeight || point.y() == 6)
                    && obstacles.stream().noneMatch(box::intersects);
        }

        public boolean canMove(FlightPathSearch.Point from, FlightPathSearch.Point to) {
            Box body = rig(from);
            FlightGeometry.Box moving = new FlightGeometry.Box(body.minX, body.minY, body.minZ,
                    body.maxX, body.maxY, body.maxZ);
            FlightPathSearch.Point delta = p(to.x() - from.x(), to.y() - from.y(), to.z() - from.z());
            return obstacles.stream().noneMatch(obstacle -> FlightGeometry.sweptIntersects(moving, delta,
                    new FlightGeometry.Box(obstacle.minX, obstacle.minY, obstacle.minZ,
                            obstacle.maxX, obstacle.maxY, obstacle.maxZ)));
        }
    }

    private record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        boolean intersects(Box other) {
            return maxX > other.minX && minX < other.maxX && maxY > other.minY
                    && minY < other.maxY && maxZ > other.minZ && minZ < other.maxZ;
        }

        Box union(Box other) {
            return new Box(Math.min(minX, other.minX), Math.min(minY, other.minY), Math.min(minZ, other.minZ),
                    Math.max(maxX, other.maxX), Math.max(maxY, other.maxY), Math.max(maxZ, other.maxZ));
        }

        boolean blocksRay(FlightPathSearch.Point from, FlightPathSearch.Point to) {
            double low = 0, high = 1;
            double[] starts = {from.x(), from.y(), from.z()}, ends = {to.x(), to.y(), to.z()};
            double[] mins = {minX, minY, minZ}, maxs = {maxX, maxY, maxZ};
            for (int axis = 0; axis < 3; axis++) {
                double delta = ends[axis] - starts[axis];
                if (Math.abs(delta) < 0.000001) {
                    if (starts[axis] < mins[axis] || starts[axis] > maxs[axis]) return false;
                } else {
                    double a = (mins[axis] - starts[axis]) / delta, b = (maxs[axis] - starts[axis]) / delta;
                    low = Math.max(low, Math.min(a, b));
                    high = Math.min(high, Math.max(a, b));
                    if (low > high) return false;
                }
            }
            return true;
        }
    }
}
