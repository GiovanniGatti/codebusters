package game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Scanner;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

final class Player {

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);

        AI player = new RoleBasedAI(in::nextInt);

        // game loop
        while (true) {
            Action[] actions = player.play();

            for (Action action : actions) {
                System.out.println(action.asString());
            }
        }
    }

    static class RoleBasedAI extends AI {
        static final int FOW_RANGE = 2200;
        static final int SQUARE_FOW_RANGE = FOW_RANGE * FOW_RANGE;

        static final int MAX_NUMBER_OF_ROUNDS = 400;

        private final int bustersPerPlayer;
        private final int ghostCount;

        private final int myTeamId;
        private final TargetPoint[] targetPoints;

        private static final int MOVEMENT_RANGE = 800;
        private static final int SQUARE_MOVEMENT_RANGE = MOVEMENT_RANGE * MOVEMENT_RANGE;

        static final int MIN_BUST_RANGE = 900;
        static final int SQUARE_MIN_BUST_RANGE = MIN_BUST_RANGE * MIN_BUST_RANGE;
        static final int MAX_BUST_RANGE = 1760;
        static final int SQUARE_MAX_BUST_RANGE = MAX_BUST_RANGE * MAX_BUST_RANGE;

        private int round = 0;

        Base base;
        private final Explorer explorer;
        private final Trapper trapper;

        // round variables
        private List<Buster> busters;
        private List<Buster> enemyBusters;
        private List<Ghost> ghosts;

        RoleBasedAI(IntSupplier inputSupplier) {
            super(Collections.emptyMap(), inputSupplier);

            this.bustersPerPlayer = readInput();
            this.ghostCount = readInput();
            this.myTeamId = readInput();

            this.targetPoints = new TargetPoint[bustersPerPlayer];
            for (int i = 0; i < targetPoints.length; i++) {
                this.targetPoints[i] = new TargetPoint(-1, -1, BusterRole.NONE);
            }


            if (myTeamId == 0) {
                base = new Base(0, 0, 0);
            } else {
                base = new Base(16000, 9000, 1);
            }

            this.explorer = new Explorer(bustersPerPlayer);
            this.trapper = new Trapper(ghostCount, base);
        }

        @Override
        Action[] play() {
            // TODO: implement
            loadInputState();

            long start = System.currentTimeMillis();
            for (GhostStatus ghostStatus : trapper.ghostStatuses) {
                System.err.println(ghostStatus);
            }

            List<Buster> explorers = new ArrayList<>();
            List<Buster> trappers = new ArrayList<>();

            List<Buster> available = getAvailableBusters();
            PairBusterAction[] returnToBase = returnToBase();

            if (ghosts.isEmpty()) {
                explorers.addAll(available);
            } else {
                for (Buster buster : available) {
                    if (buster.getId() == 0) {
                        explorers.add(buster);
                    } else {
                        trappers.add(buster);
                    }
                }
            }

            long beforeAction = System.currentTimeMillis();
            PairBusterAction[] pairBusterActions = explorer.find(round, explorers.toArray(new Buster[explorers.size()]));

            System.err.println(System.currentTimeMillis() - beforeAction);

            long beforeTrapping = System.currentTimeMillis();
            PairBusterAction[] trapperActions = trapper.find(trappers.toArray(new Buster[trappers.size()]));

            System.err.println(System.currentTimeMillis() - beforeTrapping);

            List<PairBusterAction> sorted = new ArrayList<>();
            sorted.addAll(Arrays.asList(returnToBase));
            sorted.addAll(Arrays.asList(pairBusterActions));
            sorted.addAll(Arrays.asList(trapperActions));
            Collections.sort(sorted);

            Action[] actions = new Action[sorted.size()];

            for (int i = 0; i < sorted.size(); i++) {
                actions[i] = sorted.get(i).a;
            }

            round++;

            System.err.println(System.currentTimeMillis() - start);
            System.err.println(actions.length);
            return actions;
        }

        @Override
        void reset() {

        }

        private void loadInputState() {
            this.busters = new ArrayList<>(bustersPerPlayer);
            this.enemyBusters = new ArrayList<>(bustersPerPlayer);
            this.ghosts = new ArrayList<>(ghostCount);

            int entities = readInput(); // the number of busters and ghosts visible to you
            for (int i = 0; i < entities; i++) {
                int entityId = readInput();
                int x = readInput();
                int y = readInput();
                int entityType = readInput();
                int state = readInput();
                int value = readInput();

                if (entityType == myTeamId) {
                    busters.add(new Player.Buster(entityId, x, y, state, value));
                    explorer.update(new ExploredPoint(x, y));
                } else if (entityType == -1) {
                    Ghost ghost = new Ghost(entityId, x, y, state, value);
                    ghosts.add(ghost);
                    trapper.update(ghost, round);
                } else {
                    enemyBusters.add(new Player.Buster(entityId, x, y, state, value));
                }
            }
        }

        private List<Buster> getAvailableBusters() {
            return busters.stream()
                    .filter(b -> b.getState() != BusterState.CARRYING_GHOST)
                    .collect(Collectors.toList());
        }

        //TODO: optimize it
        private PairBusterAction[] returnToBase() {
            List<Buster> nonAvailableBusters = getNonAvailableBusters();
            PairBusterAction[] pair = new PairBusterAction[nonAvailableBusters.size()];
            for (int i = 0; i < pair.length; i++) {
                Buster buster = nonAvailableBusters.get(i);
                if (buster.squareDistTo(base.x, base.y) < 1600 * 1600) {
                    pair[i] = new PairBusterAction(buster, new Release("Releasing"));
                } else {
                    pair[i] = new PairBusterAction(buster, new Move(base.x, base.y, "Delivering"));
                }
            }
            return pair;
        }

        /**
         * TODO: optimize it
         */
        private List<Buster> getNonAvailableBusters() {
            return busters.stream()
                    .filter(b -> b.getState() == BusterState.CARRYING_GHOST)
                    .collect(Collectors.toList());
        }

        private static class TargetPoint {
            int x;
            int y;
            BusterRole role;

            TargetPoint(int x, int y, BusterRole role) {
                this.x = x;
                this.y = y;
                this.role = role;
            }
        }

        private enum BusterRole {
            NONE, EXPLORER, TRAPPER, STEALER, BODYGUARD
        }

        private static class GhostStatus {
            int x, y;
            GhostPosState state;
            int stamina;
            int round;
            int id;

            private GhostStatus(int x, int y, GhostPosState state, int stamina, int id, int round) {
                this.x = x;
                this.y = y;
                this.state = state;
                this.stamina = stamina;
                this.id = id;
                this.round = round;
            }

            @Override
            public String toString() {
                return "(" + x + ", " + y + ") | " + state.name();
            }
        }

        enum GhostPosState {
            UNKNOWN, FOUND, TRAPPED, HUTING, LOST
        }

        private static class ExploredPoint {
            final int x, y;

            ExploredPoint(int x, int y) {
                this.x = x;
                this.y = y;
            }

            @Override
            public String toString() {
                return "(" + x + ", " + y + ")";
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                ExploredPoint that = (ExploredPoint) o;
                return x == that.x &&
                        y == that.y;
            }

            @Override
            public int hashCode() {
                return Objects.hash(x, y);
            }
        }

        private static class PairBusterAction implements Comparable<PairBusterAction> {
            final Buster b;
            final Action a;

            PairBusterAction(Buster b, Action a) {
                this.b = b;
                this.a = a;
            }

            Buster getBuster() {
                return b;
            }

            @Override
            public int compareTo(PairBusterAction o) {
                return b.getId() - o.getBuster().getId();
            }
        }

        static class Explorer {
            private final Random random;
            private final ToExplorePoint[] toExplorePoints;
            private final List<ExploredPoint> exploredPoints;

            Explorer(int numberOfBusters) {
                this.random = new Random();
                this.exploredPoints = new ArrayList<>(MAX_NUMBER_OF_ROUNDS * numberOfBusters);
                this.toExplorePoints = new ToExplorePoint[numberOfBusters];
            }

            void update(ExploredPoint point) {
                exploredPoints.add(point);
            }

            PairBusterAction[] find(int round, Buster... busters) {

                PairBusterAction[] pair = new PairBusterAction[busters.length];

                for (int i = 0; i < busters.length; i++) {
                    Buster buster = busters[i];
                    ToExplorePoint toExplorePoint = toExplorePoints[buster.getId()];
                    if (toExplorePoint == null || !toExplorePoint.going ||
                            buster.squareDistTo(toExplorePoint.x, toExplorePoint.y) < SQUARE_FOW_RANGE) {
                        toExplorePoint = generate(round, buster);
                        toExplorePoints[buster.getId()] = toExplorePoint;
                    }

                    pair[i] = new PairBusterAction(buster, new Move(toExplorePoint.x, toExplorePoint.y, "Exploring"));
                }

                return pair;
            }

            private ToExplorePoint generate(int round, Buster buster) {
                int x, y;

                do {
                    x = random.nextInt(16000);
                    y = random.nextInt(9000);
                } while (!isExplored(exploredPoints, buster.getX(), buster.getY(), x, y));

                return new ToExplorePoint(x, y);
            }

            private static boolean isExplored(List<ExploredPoint> exploredPoints, int x, int y, int tx, int ty) {
                for (ExploredPoint p1 : exploredPoints) {
                    for (ExploredPoint p2 : exploredPoints) {
                        if (crosses(p1, p2, x, y, tx, ty)) {
                            return true;
                        }
                    }

                    if ((p1.x - x) * (p1.x - x) + (p1.y - y) * (p1.y - y) < SQUARE_FOW_RANGE) {
                        return true;
                    }
                }

                return false;
            }

            /**
             * Return true if segment AB crosses segment CD
             * Source: http://jsfiddle.net/ytr9314a/4/
             */
            private static boolean crosses(ExploredPoint a, ExploredPoint b, int cx, int cy, int dx, int dy) {
                boolean aSide = (dx - cx) * (a.y - cy) - (dy - cy) * (a.x - cx) > 0;
                boolean bSide = (dx - cx) * (b.y - cy) - (dy - cy) * (b.x - cx) > 0;
                boolean cSide = (b.x - a.x) * (cy - a.y) - (b.y - a.y) * (cx - a.x) > 0;
                boolean dSide = (b.x - a.x) * (dy - a.y) - (b.y - a.y) * (dx - a.x) > 0;
                return aSide != bSide && cSide != dSide;
            }

            private static class ToExplorePoint {
                int x;
                int y;
                boolean going;

                ToExplorePoint(int x, int y) {
                    this.x = x;
                    this.y = y;
                    this.going = true;
                }
            }
        }

        static class Trapper {

            private int knownGhosts;
            private GhostStatus[] ghostStatuses;
            private Base base;

            Trapper(int numberOfGhosts, Base base) {
                ghostStatuses = new GhostStatus[numberOfGhosts];
                this.ghostStatuses = new GhostStatus[numberOfGhosts];
                for (int i = 0; i < ghostStatuses.length; i++) {
                    this.ghostStatuses[i] = new GhostStatus(0, 0, GhostPosState.UNKNOWN, 0, i, 0);
                }
                this.base = base;
                knownGhosts = 0;
            }

            void update(Ghost ghost, int round) {
                ghostStatuses[ghost.getId()].x = ghost.getX();
                ghostStatuses[ghost.getId()].y = ghost.getY();
                ghostStatuses[ghost.getId()].state = GhostPosState.FOUND;
                ghostStatuses[ghost.getId()].round = round;
                ghostStatuses[ghost.getId()].stamina = ghost.getStamina();
                knownGhosts++;
            }

            PairBusterAction[] find(Buster... busters) {
                PairBusterAction[] pair = new PairBusterAction[busters.length];

                for (int i = 0; i < busters.length; i++) {
                    Buster buster = busters[i];
                    GhostStatus targetGhost = null;
                    for (GhostStatus status : ghostStatuses) {
                        long minWeight = Long.MAX_VALUE;
                        if (status.state == GhostPosState.FOUND) {
                            long weight = buster.squareDistTo(status.x, status.y) / SQUARE_MOVEMENT_RANGE +
                                    status.stamina
                                    + base.squareDistTo(status.x, status.y) / SQUARE_MOVEMENT_RANGE;
                            if (weight < minWeight) {
                                targetGhost = status;
                            }
                        }
                    }

                    double dist = (targetGhost.x - buster.getX()) * (targetGhost.x - buster.getX())
                            + (targetGhost.y - buster.getY()) * (targetGhost.y - buster.getY());
                    if (dist < SQUARE_MAX_BUST_RANGE && dist > SQUARE_MIN_BUST_RANGE) {
                        pair[i] = new PairBusterAction(buster, new Bust(targetGhost.id, "Trapping"));
                    } else {
                        pair[i] = new PairBusterAction(buster, new Move(targetGhost.x, targetGhost.y, "Trapping"));
                    }
                }

                return pair;
            }
        }
    }

    static abstract class AI {

        private final Map<String, Object> conf;
        private final IntSupplier inputSupplier;

        /**
         * Builds an AI with specified configuration.<br>
         * If the AI does not need a configuration, an empty one may be provided.<br>
         * It is also recommended to create a default configuration.
         */
        AI(Map<String, Object> conf, IntSupplier inputSupplier) {
            this.conf = Collections.unmodifiableMap(conf);
            this.inputSupplier = inputSupplier;
        }

        /**
         * Implements the IA algorithm
         *
         * @return the best action found
         */
        abstract Action[] play();

        Map<String, Object> getConf() {
            return conf;
        }

        int readInput() {
            return inputSupplier.getAsInt();
        }

        /**
         * If eventually the AI is not stateless, i.e. it learns something during a game play, this method may be used
         * to forget anything before another match. Leave it blank if IA doesn't learn anything, or you want the AI to
         * keep its knowledge.
         */
        abstract void reset();
    }

    static class Entity {
        private final int id;
        private final int x;
        private final int y;

        Entity(int id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }

        int getId() {
            return id;
        }

        int getX() {
            return x;
        }

        int getY() {
            return y;
        }

        long squareDistTo(Entity other) {
            return squareDistTo(other.x, other.y);
        }

        long squareDistTo(int x, int y) {
            return (this.x - x) * (this.x - x) + (this.y - y) * (this.y - y);
        }

        double distTo(Entity other) {
            return Math.sqrt(squareDistTo(other.x, other.y));
        }

        double distTo(int x, int y) {
            return Math.sqrt(squareDistTo(x, y));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entity entity = (Entity) o;
            return getId() == entity.getId();
        }

        @Override
        public int hashCode() {
            return Objects.hash(getId());
        }
    }

    final static class Buster extends Entity {
        private final BusterState state;
        private final int value;

        Buster(int id, int x, int y, int state, int value) {
            super(id, x, y);
            this.value = value;
            switch (state) {
                case 0:
                    this.state = BusterState.IDLE;
                    break;
                case 1:
                    this.state = BusterState.CARRYING_GHOST;
                    break;
                case 2:
                    this.state = BusterState.STUNNED;
                    break;
                case 3:
                    this.state = BusterState.TRAPPING;
                    break;
                default:
                    throw new IllegalStateException("Unknown buster state " + state);
            }
        }

        public BusterState getState() {
            return state;
        }

        /**
         * Ghost id being carried/trapped or the number of turns it can move again
         */
        public int getValue() {
            return value;
        }

        @Override
        public String toString() {
            return super.id + " @ (" + super.x + ", " + super.y + ") | " +
                    state.name() + " | " +
                    "value=" + value;
        }
    }

    final static class Ghost extends Entity {
        private final int stamina;
        private final int trappersCount;

        Ghost(int id, int x, int y, int stamina, int trappersCount) {
            super(id, x, y);
            this.stamina = stamina;
            this.trappersCount = trappersCount;
        }

        int getStamina() {
            return stamina;
        }

        int getTrappersCount() {
            return trappersCount;
        }

        @Override
        public String toString() {
            return super.id + " @ (" + super.x + ", " + super.y + ") | " +
                    "stamina=" + stamina + " | " +
                    "trappers=" + trappersCount;
        }
    }

    enum BusterState {
        IDLE, CARRYING_GHOST, STUNNED, TRAPPING
    }

    static class Base {
        int x;
        int y;
        int myTeamId;

        Base(int x, int y, int myTeamId) {
            this.x = x;
            this.y = y;
            this.myTeamId = myTeamId;
        }

        public long squareDistTo(int x, int y) {
            return (this.x - x) * (this.x - x) + (this.y - y) * (this.y - y);
        }
    }

    /**
     * Represents an action that can be taken
     */
    interface Action {
        String asString();
    }

    static class Move implements Action {
        private final int x, y;
        private final String message;

        Move(int x, int y) {
            this(x, y, null);
        }

        Move(int x, int y, String message) {
            this.x = x;
            this.y = y;
            this.message = message;
        }

        @Override
        public String asString() {
            String action = "MOVE " + x + " " + y;
            if (message != null) {
                return action.concat(" " + message);
            }
            return action;
        }
    }

    static class Bust implements Action {
        private final int ghostId;
        private final String message;

        Bust(int ghostId) {
            this(ghostId, null);
        }

        Bust(int ghostId, String message) {
            this.ghostId = ghostId;
            this.message = message;
        }

        @Override
        public String asString() {
            String action = "BUST " + ghostId;
            if (message != null) {
                return action.concat(" " + message);
            }
            return action;
        }
    }

    static class Release implements Action {
        private final String message;

        Release() {
            this(null);
        }

        Release(String message) {
            this.message = message;
        }

        @Override
        public String asString() {
            String action = "RELEASE";
            if (message != null) {
                return action.concat(" " + message);
            }
            return action;
        }
    }

    static class Stun implements Action {
        private final int enemyBusterId;
        private final String message;

        Stun(int enemyBusterId) {
            this(enemyBusterId, null);
        }

        Stun(int enemyBusterId, String message) {
            this.enemyBusterId = enemyBusterId;
            this.message = message;
        }

        @Override
        public String asString() {
            String action = "STUN " + enemyBusterId;
            if (message != null) {
                return action.concat(" " + message);
            }
            return action;
        }
    }

    // Change it to sample standard deviation instead of population standard deviation
    /*
     * See more https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Online_algorithm
     * http://www.alcula.com/calculators/statistics/variance/
     * https://en.wikipedia.org/wiki/68%E2%80%9395%E2%80%9399.7_rule
     */
    static final class Timer {

        private final long endTime;
        private final long startTime;
        private final boolean strict;

        private int laps;
        private long elapsed;
        private long previous;
        private double mean;
        private double M2;
        private double variance;

        private double security;

        private Timer(long expectedMillis) {
            startTime = System.nanoTime();
            endTime = startTime + expectedMillis * 1_000_000L;
            laps = 0;
            previous = startTime;
            mean = 0L;
            M2 = 0L;
            variance = 0L;
            this.strict = false;
        }

        private Timer(long expectedMillis, double security) {
            startTime = System.nanoTime();
            endTime = startTime + expectedMillis * 1_000_000L;
            laps = 0;
            previous = startTime;
            mean = 0L;
            M2 = 0L;
            variance = 0L;
            this.strict = true;
            this.security = security;
        }

        static Timer start(long expectedMillis) {
            return new Timer(expectedMillis);
        }

        static Timer start(long expectedMillis, double security) {
            return new Timer(expectedMillis, security);
        }

        boolean finished() {
            if (strict) {
                double deviation = Math.sqrt(variance);
                return System.nanoTime() + (mean + security * deviation) > endTime;
            }
            return System.nanoTime() + mean > endTime;
        }

        void lap() {
            long current = System.nanoTime();
            elapsed = current - previous;
            previous = current;
            laps++;
            double delta = elapsed - mean;
            mean += delta / laps;
            if (strict) {
                M2 += delta * (elapsed - mean);
                if (laps > 1) {
                    variance = M2 / (laps - 1);
                }
            }
        }

        void print() {
            System.out.println("lap=" + laps + ", elapsed=" + elapsed + "," + "mean=" + (long) mean + ", sigma^2="
                    + (long) variance + ", sigma=" + Math.sqrt(variance));
        }
    }
}
