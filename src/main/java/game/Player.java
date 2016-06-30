package game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.function.IntSupplier;

final class Player {
    
    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);

        AI player = new StateMachineAI(in::nextInt);

        // game loop
        while (true) {
            Action[] actions = player.play();

            for (Action action : actions) {
                System.out.println(action.asString());
            }
        }
    }

    static class StateMachineAI extends AI {

        static final Random RANDOM = new Random();

        static final int MAX_NUMBER_OF_ROUNDS = 400;

        static final int VISIBILITY_RANGE = 2200;
        static final int SQUARE_VISIBILITY_RANGE = VISIBILITY_RANGE * VISIBILITY_RANGE;

        static final int STUN_RANGE = 1760;
        static final int SQUARE_STUN_RANGE = STUN_RANGE * STUN_RANGE;

        static final int MIN_BUST_RANGE = 900;
        static final int SQUARE_MIN_BUST_RANGE = MIN_BUST_RANGE * MIN_BUST_RANGE;
        static final int MAX_BUST_RANGE = 1760;
        static final int SQUARE_MAX_BUST_RANGE = MAX_BUST_RANGE * MAX_BUST_RANGE;

        static final int BUSTER_MAX_MOVEMENT_RANGE = 800;
        static final int SQUARE_BUSTER_MAX_MOVEMENT_RANGE = BUSTER_MAX_MOVEMENT_RANGE * BUSTER_MAX_MOVEMENT_RANGE;

        int bustersPerPlayer; // the amount of busters you control
        int ghostCount; // the amount of ghosts on the map
        int myTeamId; // if this is 0, your base is on the top left of the map, if it is one, on the bottom right
        int baseX, baseY;

        private final int[][] visitedPoints;
        private int visitedPointsCount;

        private final GhostPos[] foundGhosts;
        private final Target[] movingTo;
        private final int[] chasingGhost;
        private final int[] lastStun;
        private int round;
        private int playerScore;

        StateMachineAI(IntSupplier inputSupplier) {
            super(Collections.emptyMap(), inputSupplier);
            this.bustersPerPlayer = readInput();
            this.ghostCount = readInput();
            this.myTeamId = readInput();
            if (myTeamId == 0) {
                this.baseX = 0;
                this.baseY = 0;
            } else {
                this.baseX = 9_000;
                this.baseY = 16_000;
            }

            this.visitedPoints = new int[bustersPerPlayer * MAX_NUMBER_OF_ROUNDS][2];
            this.visitedPointsCount = 0;

            this.foundGhosts = new GhostPos[ghostCount];
            this.movingTo = new Target[bustersPerPlayer];
            this.chasingGhost = new int[bustersPerPlayer];
            this.lastStun = new int[bustersPerPlayer];

            this.round = 0;
            this.playerScore = 0;

            for (int i = 0; i < foundGhosts.length; i++) {
                foundGhosts[i] = new GhostPos(0, 0, GhostPosState.UNKNOWN);
            }

            for (int i = 0; i < movingTo.length; i++) {
                movingTo[i] = new Target(0, 0, false);
            }

            Arrays.fill(chasingGhost, -1);
            Arrays.fill(lastStun, -20);
        }

        @Override
        Action[] play() {
            this.round++;

            Triple<List<Buster>, List<Buster>, List<Ghost>> triple = load();

            List<Buster> busters = triple.x;
            List<Buster> enemyBusters = triple.y;
            List<Ghost> ghosts = triple.z;

            printState(busters, enemyBusters, ghosts);

            Action[] busterActions = new Action[bustersPerPlayer];

            for (int i = 0; i < bustersPerPlayer; i++) {

                Buster buster = busters.get(i);
                Action action = searchBusterAction(buster, busters, enemyBusters, ghosts);
                busterActions[i] = action;
            }

            return busterActions;
        }

        private Action searchBusterAction(
                Buster buster,
                List<Buster> busters,
                List<Buster> enemyBusters,
                List<Ghost> ghosts) {

            int busterId = buster.getId();

            // return ghost to base
            Target target = movingTo[busterId];

            if (buster.getState() == BusterState.CARRYING_GHOST) {
                target.valid = false;
                // carrying a ghost
                double distToCorner = Math.pow(myTeamId * 16000 - buster.getX(), 2) +
                        Math.pow(myTeamId * 9000 - buster.getY(), 2);
                if (distToCorner < 2_560_000.0) {
                    playerScore++;
                    return new Release();
                } else if (myTeamId == 0) {
                    // compute intersection point between a line and a circle,
                    // in which both pass through the point (0, 0)

                    // y = A*x + B, where B = 0
                    double a = ((double) buster.getY()) / buster.getX();

                    // (x-a)^2 + (y-b)^2 = R^2
                    double x = Math.sqrt(2560000.0 / (1.0 + Math.pow(a, 2)));
                    double y = a * x;
                    return new Move(((int) (x - 1.0)), ((int) (y - 1.0)));
                } else {
                    // playing on mirror mode
                    // FIXME: something is still not perfect
                    double a = ((double) 16000 - buster.getY()) / (9000 - buster.getX());
                    double x = Math.sqrt(2560000.0 / (1.0 + Math.pow(a, 2)));
                    double y = a * x;
                    return new Move(((int) (16001.0 - x)), ((int) (9001.0 - y)));
                }
            }

            // if found an enemy carrying a ghost, attack him
            // TODO: two busters should not stun the same enemy id
            // TODO: stop and move closer if enemy is not in perfect range...
            // TODO: detect when he is in the fog?
            if (lastStun[busterId] + 20 < round) {
                for (Buster enemyBuster : enemyBusters) {
                    BusterState state = enemyBuster.getState();
                    if (state == BusterState.CARRYING_GHOST || state == BusterState.TRAPPING ||
                            (state != BusterState.STUNNED && thereIsGhostsInRange(ghosts, buster))) {
                        long dist = enemyBuster.squareDistTo(buster);
                        if (dist < SQUARE_STUN_RANGE) {
                            lastStun[busterId] = round;
                            return new Stun(enemyBuster.getId());
                        }
                    }
                }
            }

            // trapping a ghost
            for (Ghost ghost : ghosts) {
                double dist = buster.squareDistTo(ghost);
                if (dist > SQUARE_MIN_BUST_RANGE && dist < SQUARE_MAX_BUST_RANGE) {
                    int ghostId = ghost.getId();
                    foundGhosts[ghostId].state = GhostPosState.TRAPPED;
                    chasingGhost[busterId] = -1; // not chasing ghosts anymore
                    target.valid = false;
                    return new Bust(ghostId);
                }
            }

            // if chasing a ghost
            if (chasingGhost[busterId] != -1) {
                target.valid = false;
                int ghostId = chasingGhost[busterId];
                GhostPos foundGhost = foundGhosts[ghostId];
                if (buster.getX() == foundGhost.x && buster.getY() == foundGhost.y
                        && !ghostIsVisible(ghosts, ghostId)) {
                    foundGhost.state = GhostPosState.UNKNOWN;
                    chasingGhost[busterId] = -1; // well, someone else captured that ghost...
                } else {
                    // otherwise, keep doing so
                    return new Move(foundGhost.x, foundGhost.y);
                }
            }

            // if doing nothing special and found an enemy, attack him
            if (lastStun[busterId] + 20 < round) {
                for (Buster enemyBuster : enemyBusters) {
                    if (enemyBuster.getState() != BusterState.STUNNED) {
                        double dist = buster.squareDistTo(enemyBuster);
                        if (dist < SQUARE_STUN_RANGE) {
                            lastStun[busterId] = round;
                            return new Stun(enemyBuster.getId());
                        }
                    }
                }
            }

            // searching for ghost

            // Look for ghosts have been last seen...
            double closestGhost = Double.MAX_VALUE;
            int closestGhostId = -1;
            for (int g = 0; g < foundGhosts.length; g++) {
                GhostPos ghost = foundGhosts[g];
                if (ghost.state != GhostPosState.UNKNOWN && ghost.state != GhostPosState.TRAPPED) {
                    double dist = buster.squareDistTo(ghost.x, ghost.y);
                    if (dist < closestGhost) {
                        closestGhost = dist;
                        closestGhostId = g;
                    }
                }
            }

            if (closestGhostId != -1) {
                GhostPos foundGhost = foundGhosts[closestGhostId];
                foundGhost.state = GhostPosState.IN_CHASE;
                chasingGhost[busterId] = closestGhostId;
                target.valid = false;
                return new Move(foundGhost.x, foundGhost.y);
            }

            // continue movement
            if (target.valid) {
                double dist = buster.squareDistTo(target.x, target.y);
                if (dist > SQUARE_BUSTER_MAX_MOVEMENT_RANGE) {
                    // move hasn't finished, keep going
                    return new Move(target.x, target.y);
                }
            }

            // finally, find spot to go to
            int x, y;
            do {
                // Searching for unknown ghosts
                x = RANDOM.nextInt(16000);
                y = RANDOM.nextInt(9000);
            } while (!isClosestAvailableBuster(x, y, buster, busters)
                    && isExploredPoint(x, y, visitedPoints, visitedPointsCount));

            target.x = x;
            target.y = y;
            target.valid = true;
            return new Move(x, y);
        }

        private Triple<List<Buster>, List<Buster>, List<Ghost>> load() {
            List<Buster> busters = new ArrayList<>(bustersPerPlayer);
            List<Buster> enemyBusters = new ArrayList<>(bustersPerPlayer);
            List<Ghost> ghosts = new ArrayList<>(ghostCount);


            int entities = readInput(); // the number of busters and ghosts visible to you
            for (int i = 0; i < entities; i++) {
                int entityId = readInput();
                int x = readInput();
                int y = readInput();
                int entityType = readInput();
                int state = readInput();
                int value = readInput();

                if (entityType == myTeamId) {
                    busters.add(new Buster(entityId, x, y, state, value));

                    visitedPoints[visitedPointsCount][0] = x;
                    visitedPoints[visitedPointsCount][1] = y;
                    visitedPointsCount++;
                } else if (entityType == -1) {
                    ghosts.add(new Ghost(entityId, x, y, state, value));

                    foundGhosts[entityId] = new GhostPos(x, y, GhostPosState.FOUND);
                } else {
                    enemyBusters.add(new Buster(entityId, x, y, state, value));
                }
            }

            return new Triple<>(busters, enemyBusters, ghosts);
        }

        private void printState(List<Buster> busters, List<Buster> enemyBusters, List<Ghost> ghosts) {
            System.err.println("----last seen ghosts---");
            for (int i = 0; i < foundGhosts.length; i++) {
                System.err.println(i + " - " + foundGhosts[i]);
            }

            System.err.println("----chasing---");
            for (int i = 0; i < chasingGhost.length; i++) {
                System.err.println(i + " @ " + chasingGhost[i]);
            }

            System.err.println("----moving---");
            for (int i = 0; i < movingTo.length; i++) {
                System.err.println(i + " @ (" + movingTo[i]);
            }

            System.err.println("----ghosts----");
            ghosts.forEach(System.err::println);

            System.err.println("----my busters----");
            busters.forEach(System.err::println);

            System.err.println("----enemy busters----");
            enemyBusters.forEach(System.err::println);
        }

        @Override
        void reset() {
            //ILB
        }

        private static boolean ghostIsVisible(List<Ghost> ghosts, int id) {
            return ghosts.stream()
                    .anyMatch(g -> g.getId() == id);
        }

        private static boolean thereIsGhostsInRange(List<Ghost> ghosts, Buster buster) {
            return ghosts.stream()
                    .anyMatch(g -> g.squareDistTo(buster) < SQUARE_VISIBILITY_RANGE);
        }

        private static boolean isExploredPoint(int x, int y, int[][] visitedPoints, int visitedPointsCount) {
            for (int i = 0; i < visitedPointsCount; i++) {
                int px = visitedPoints[i][0];
                int py = visitedPoints[i][1];
                if ((x - px) * (x - px) + (y - py) * (y - py) < SQUARE_VISIBILITY_RANGE) {
                    return true;
                }
            }

            return false;
        }

        private static boolean isClosestAvailableBuster(int x, int y, Buster buster, List<Buster> busters) {
            double dist = buster.squareDistTo(x, y);
            return busters.stream()
                    .filter(b -> b.getId() != buster.getId())
                    .filter(b -> b.getState() == BusterState.IDLE)
                    .map(b -> b.squareDistTo(x, y))
                    .noneMatch(d -> d < dist);
        }

        private static class GhostPos {
            int x, y;
            GhostPosState state;

            private GhostPos(int x, int y, GhostPosState state) {
                this.x = x;
                this.y = y;
                this.state = state;
            }

            @Override
            public String toString() {
                return "(" + x + ", " + y + ") | " + state.name();
            }
        }

        enum GhostPosState {
            UNKNOWN, FOUND, TRAPPED, IN_CHASE
        }

        private static class Target {
            int x, y;
            boolean valid; // true if going to target point, false otherwise

            Target(int x, int y, boolean valid) {
                this.x = x;
                this.y = y;
                this.valid = valid;
            }

            @Override
            public String toString() {
                return "(" + x + ", " + y + ") | " + valid;
            }
        }

        private static class Triple<X, Y, Z> {
            private final X x;
            private final Y y;
            private final Z z;

            Triple(X x, Y y, Z z) {
                this.x = x;
                this.y = y;
                this.z = z;
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
