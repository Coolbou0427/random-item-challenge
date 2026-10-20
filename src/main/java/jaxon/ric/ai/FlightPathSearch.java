package jaxon.ric.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/** Incremental A* over flight space. Collision checks must cover the whole carried assembly. */
public final class FlightPathSearch {
    public record Point(double x, double y, double z) {
        public double distanceTo(Point other) {
            double dx = x - other.x, dy = y - other.y, dz = z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    public interface Space {
        boolean canOccupy(Point point);
        boolean canMove(Point from, Point to);
    }

    private record Cell(int x, int y, int z) {}
    private record Node(Cell cell, Point point, double cost, double estimate, Node parent, long order) {}
    private static final int[][] DIRECTIONS = flightDirections();

    private static int[][] flightDirections() {
        List<int[]> directions = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) directions.add(new int[]{x, y, z});
                }
            }
        }
        return directions.toArray(int[][]::new);
    }

    private final Point origin;
    private final Space space;
    private final Predicate<Point> goal;
    private final ToDoubleFunction<Point> heuristic;
    private final double step;
    private final int nodeLimit;
    private final PriorityQueue<Node> open = new PriorityQueue<>(Comparator
            .comparingDouble((Node node) -> node.cost + node.estimate * 1.1)
            .thenComparingDouble(Node::estimate).thenComparingLong(Node::order));
    private final Map<Cell, Double> costs = new HashMap<>();
    private final Map<Cell, Boolean> occupancy = new HashMap<>();
    private Node best;
    private long sequence;
    private int expanded;
    private boolean finished;
    private boolean reachedGoal;
    private List<Point> path = List.of();

    public FlightPathSearch(Point origin, Space space, Predicate<Point> goal,
                            ToDoubleFunction<Point> heuristic, double step, int nodeLimit) {
        if (step <= 0 || nodeLimit < 1) throw new IllegalArgumentException("Invalid flight search limits");
        this.origin = origin;
        this.space = space;
        this.goal = goal;
        this.heuristic = heuristic;
        this.step = step;
        this.nodeLimit = nodeLimit;
        best = new Node(new Cell(0, 0, 0), origin, 0, heuristic.applyAsDouble(origin), null, sequence++);
        costs.put(best.cell, 0.0);
        if (space.canOccupy(origin)) open.add(best);
        else finished = true;
    }

    /** Does at most budget node expansions, so a difficult route cannot stall the server tick. */
    public void advance(int budget) {
        int remaining = Math.max(0, budget);
        while (!finished && remaining > 0) {
            if (open.isEmpty() || expanded >= nodeLimit) {
                finish(best, false);
                return;
            }
            Node current = open.remove();
            if (current.cost > costs.getOrDefault(current.cell, Double.POSITIVE_INFINITY)) continue;
            expanded++;
            remaining--;
            if (goal.test(current.point)) {
                finish(current, true);
                return;
            }
            if (current.estimate < best.estimate - 0.001) best = current;

            for (int[] direction : DIRECTIONS) {
                Cell nextCell = new Cell(current.cell.x + direction[0], current.cell.y + direction[1],
                        current.cell.z + direction[2]);
                double cost = current.cost + step * Math.sqrt(direction[0] * direction[0]
                        + direction[1] * direction[1] + direction[2] * direction[2]);
                if (cost >= costs.getOrDefault(nextCell, Double.POSITIVE_INFINITY)) continue;
                Point next = new Point(origin.x + nextCell.x * step, origin.y + nextCell.y * step,
                        origin.z + nextCell.z * step);
                if (!occupancy.computeIfAbsent(nextCell, ignored -> space.canOccupy(next))) continue;
                // The complete swept-body check also covers diagonal edges:
                // ascend/across/descend together without cutting through corners.
                if (!space.canMove(current.point, next)) continue;
                costs.put(nextCell, cost);
                open.add(new Node(nextCell, next, cost, heuristic.applyAsDouble(next), current, sequence++));
            }
        }
    }

    private void finish(Node end, boolean reachedGoal) {
        finished = true;
        this.reachedGoal = reachedGoal;
        ArrayList<Point> route = new ArrayList<>();
        for (Node node = end; node.parent != null; node = node.parent) route.add(node.point);
        Collections.reverse(route);
        path = List.copyOf(route);
    }

    public boolean isFinished() { return finished; }
    public boolean reachedGoal() { return reachedGoal; }
    public List<Point> path() { return path; }
    public int expandedNodes() { return expanded; }

    public record RouteAim(int index, Point position) {}

    /** Cuts a safe sloping line to a later leg instead of finishing every corner first. */
    public static RouteAim lookAhead(Point from, List<Point> route, int index, Space space) {
        for (int next = Math.min(route.size() - 1, index + 6); next >= index; next--) {
            Point end = route.get(next);
            if (space.canOccupy(end) && space.canMove(from, end)) return new RouteAim(next, end);
            if (next == 0) continue;
            Point start = route.get(next - 1);
            double dx = end.x - start.x, dy = end.y - start.y, dz = end.z - start.z;
            double lengthSquared = dx * dx + dy * dy + dz * dz;
            if (lengthSquared < 0.001) continue;
            double progress = ((from.x - start.x) * dx + (from.y - start.y) * dy
                    + (from.z - start.z) * dz) / lengthSquared;
            for (double fraction = 0.875; fraction >= 0.125; fraction -= 0.125) {
                if (fraction <= progress + 0.02) continue;
                Point candidate = new Point(start.x + dx * fraction, start.y + dy * fraction, start.z + dz * fraction);
                if (candidate.distanceTo(from) < 0.65) continue;
                if (space.canOccupy(candidate) && space.canMove(from, candidate)) return new RouteAim(next, candidate);
            }
        }
        return null;
    }

    /** Compares clear direct approaches with climb/across/descent routes before grid detours. */
    public static List<Point> directOrOverflight(Point start, List<Point> destinations, Space space,
                                                double maximumClimb) {
        List<Point> best = List.of();
        double bestLength = Double.POSITIVE_INFINITY;
        for (Point end : destinations) {
            if (!space.canOccupy(end)) continue;
            double directLength = start.distanceTo(end);
            if (directLength < bestLength && space.canMove(start, end)) {
                best = List.of(end);
                bestLength = directLength;
            }
            double lowestCruise = Math.max(start.y, end.y);
            for (double rise = 0.0; rise <= maximumClimb; rise += 2.0) {
                double cruise = lowestCruise + rise;
                Point aboveStart = new Point(start.x, cruise, start.z);
                Point aboveEnd = new Point(end.x, cruise, end.z);
                double length = start.distanceTo(aboveStart) + aboveStart.distanceTo(aboveEnd) + aboveEnd.distanceTo(end);
                if (length >= bestLength) break;
                // Do not choose a very tall climb when a shorter lateral route may exist.
                if (length > directLength * 1.8 + 20.0) break;
                if (!space.canOccupy(aboveStart) || !space.canOccupy(aboveEnd)) continue;
                if (space.canMove(start, aboveStart) && space.canMove(aboveStart, aboveEnd)
                        && space.canMove(aboveEnd, end)) {
                    best = List.of(aboveStart, aboveEnd, end);
                    bestLength = length;
                    break;
                }
            }
        }
        return best;
    }
}
