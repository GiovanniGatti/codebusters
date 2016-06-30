package game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    static class RoleBasedAI extends AI {

        static final int MAX_NUMBER_OF_ROUNDS = 400;

        private final int bustersPerPlayer;
        private final int ghostCount;

        private final List<ExploredPoint> exploredPoints;

        private final int myTeamId;
        private final TargetPoint[] targetPoints;
        private final GhostStatus[] ghostStatuses;
        private int knownGhosts;

        private int round = 0;

        // round variables
        private List<Buster> busters;
        private List<Buster> enemyBusters;
        private List<Ghost> ghosts;

        RoleBasedAI(IntSupplier inputSupplier) {
            super(Collections.emptyMap(), inputSupplier);

            this.bustersPerPlayer = readInput();
            this.ghostCount = readInput();
            this.myTeamId = readInput();

            this.exploredPoints = new ArrayList<>(bustersPerPlayer * MAX_NUMBER_OF_ROUNDS);

            this.targetPoints = new TargetPoint[bustersPerPlayer];
            for (int i = 0; i < targetPoints.length; i++) {
                this.targetPoints[i] = new TargetPoint(-1, -1, BusterRole.NONE);
            }

            this.knownGhosts = 0;
            this.ghostStatuses = new GhostStatus[ghostCount];
            for (int i = 0; i < ghostStatuses.length; i++) {
                this.ghostStatuses[i] = new GhostStatus(0, 0, GhostPosState.UNKNOWN);
            }
        }

        @Override
        Action[] play() {
            // TODO: implement

            round++;
            return new Action[0];
        }

        @Override
        void reset() {

        }

        /*
         * How to test it?
         */
        private static Action explore(
                TargetPoint[] targetPoints,
                List<ExploredPoint> exploredPoints,
                int knownGhosts) {

            return null;
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
                    exploredPoints.add(new ExploredPoint(x, y));
                } else if (entityType == -1) {
                    ghosts.add(new Player.Ghost(entityId, x, y, state, value));

                    if (ghostStatuses[i].state == GhostPosState.UNKNOWN) {
                        ghostStatuses[i].x = x;
                        ghostStatuses[i].y = y;
                        ghostStatuses[i].state = GhostPosState.FOUND;
                        knownGhosts++;
                    }
                } else {
                    enemyBusters.add(new Player.Buster(entityId, x, y, state, value));
                }
            }
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

            private GhostStatus(int x, int y, GhostPosState state) {
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
