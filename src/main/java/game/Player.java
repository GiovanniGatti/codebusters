package game;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.function.IntSupplier;

final class Player {

    // TODO: Clean up code
    public static void main(String args[]) {
        Random random = new Random();

        Scanner in = new Scanner(System.in);
        int bustersPerPlayer = in.nextInt(); // the amount of busters you control
        int ghostCount = in.nextInt(); // the amount of ghosts on the map
        int myTeamId = in.nextInt(); // if this is 0, your base is on the top left of the map, if it is one, on the
        // bottom right

        State gameState = new State(bustersPerPlayer, ghostCount, myTeamId);

        int visitedPointsCount = 0;

        // game loop
        while (true) {
            gameState.incrementRound();

            Buster[] enemyBusters = new Buster[bustersPerPlayer]; // (x, y) / id / value / state
            Buster[] busters = new Buster[bustersPerPlayer]; // (x, y) / id / idle / ghostId
            Ghost[] ghosts = new Ghost[ghostCount];

            int buster = 0;
            int ghost = 0;
            int enemyBuster = 0;

            int entities = in.nextInt(); // the number of busters and ghosts visible to you
            for (int i = 0; i < entities; i++) {
                int entityId = in.nextInt(); // buster id or ghost id
                int x = in.nextInt();
                int y = in.nextInt(); // position of this buster / ghost
                int entityType = in.nextInt(); // the team id if it is a buster, -1 if it is a ghost.

                // For busters: 0=idle, 1=carrying a ghost, 2=stunned buster, 3=buster is trapping a ghost
                // For ghosts: ghost's stamina
                int state = in.nextInt();

                // For busters: Ghost id being carried/trapped or the number of turns it can move again
                // For ghosts: number of busters attempting to trap this ghost move again.
                int value = in.nextInt();

                if (entityType == myTeamId) {
                    Buster buster1 = new Buster(entityId, x, y, state, value);
                    busters[buster] = buster1;
                    buster++;

                    gameState.visitedPoints[visitedPointsCount][0] = x;
                    gameState.visitedPoints[visitedPointsCount][1] = y;
                    visitedPointsCount++;
                } else if (entityType == -1) {
                    Ghost ghost1 = new Ghost(entityId, x, y, state, value);
                    ghosts[ghost] = ghost1;
                    ghost++;

                    gameState.lastSeen[entityId][0] = x;
                    gameState.lastSeen[entityId][1] = y;
                    gameState.lastSeen[entityId][2] = 1;
                } else {
                    Buster buster1 = new Buster(entityId, x, y, state, value);
                    enemyBusters[buster] = buster1;
                    enemyBuster++;
                }
            }

            System.err.println("----last seen ghosts---");
            for (int i = 0; i < lastSeen.length; i++) {
                System.err.println(lastSeen[i][2] + " @ (" + lastSeen[i][0] + ", " + lastSeen[i][1] + ")");
            }

            System.err.println("----in chase---");
            for (int i = 0; i < inChase.length; i++) {
                System.err.println(i + " @ " + inChase[i]);
            }

            System.err.println("----moving---");
            for (int i = 0; i < moving.length; i++) {
                System.err.println(i + " @ (" + moving[i][0] + ", " + moving[i][1] + ")" + " | " + moving[i][2]);
            }

            System.err.println("----ghosts----");
            for (int i = 0; i < ghost; i++) {
                System.err.println(ghosts[i][2] + " @ (" + ghosts[i][0] + ", " + ghosts[i][1] + ")");
            }

            System.err.println("----enemy busters----");
            for (int i = 0; i < enemyBuster; i++) {
                System.err.println(enemyBusters[i][2] + " @ (" + enemyBusters[i][0] + ", " + enemyBusters[i][1]
                        + ") ->"
                        + enemyBusters[i][3]);
            }

            for (int i = 0; i < bustersPerPlayer; i++) {

                // return ghost to base
                if (busters[i][3] == 1) {
                    moving[i][2] = 0;
                    // carrying a ghost
                    double distToCorner = Math.pow(myTeamId * 16000 - busters[i][0], 2) +
                            Math.pow(myTeamId * 9000 - busters[i][1], 2);
                    if (distToCorner < 2_560_000.0) {
                        System.out.println("RELEASE");
                    } else if (myTeamId == 0) {
                        // compute intersection point between a line and a circle,
                        // in which both pass through the point (0, 0)

                        // y = A*x + B, where B = 0
                        double a = ((double) busters[i][1]) / busters[i][0];

                        // (x-a)^2 + (y-b)^2 = R^2
                        double x = Math.sqrt(2560000.0 / (1.0 + Math.pow(a, 2)));
                        double y = a * x;
                        System.out.println("MOVE " + ((int) (x - 1.0)) + " " + ((int) (y - 1.0)));
                    } else {
                        // playing on mirror mode
                        // FIXME: something is still not perfect
                        double a = ((double) 16000 - busters[i][1]) / (9000 - busters[i][0]);
                        double x = Math.sqrt(2560000.0 / (1.0 + Math.pow(a, 2)));
                        double y = a * x;
                        System.out.println("MOVE " + ((int) (16001 - x)) + " " + ((int) (9001 - y)));
                    }
                    continue;
                }

                // if found an enemy carrying a ghost, attack him
                // TODO: two busters should not stun the same enemy id
                // TODO: stop and move closer if enemy is not in perfect range...
                // TODO: detect when he is in the fog?
                if (lastStun[i] + 20 < roundCount) {
                    boolean stun = false;
                    for (int e = 0; e < enemyBuster && !stun; e++) {
                        if (enemyBusters[e][4] == 1 || enemyBusters[e][4] == 3 ||
                                (enemyBusters[e][4] != 2 && thereIsGhostsInRange(ghosts, ghostCount, busters[i]))) {
                            double dist = Math.pow(enemyBusters[e][0] - busters[i][0], 2) +
                                    Math.pow(enemyBusters[e][1] - busters[i][1], 2);
                            if (dist < 3_097_600.0) {
                                System.out.println("STUN " + enemyBusters[e][2]);
                                stun = true;
                            }
                        }
                    }

                    if (stun) {
                        lastStun[i] = roundCount;
                        continue;
                    }
                }

                // trapping a ghost
                boolean trapped = false;
                for (int g = 0; g < ghost && !trapped; g++) {
                    double dist = Math.pow(ghosts[g][0] - busters[i][0], 2) + Math.pow(ghosts[g][1] - busters[i][1], 2);
                    if (dist > 810_000.0 && dist < 3_097_600.0) {
                        System.out.println("BUST " + ghosts[g][2]);
                        lastSeen[ghosts[g][2]][2] = 2;
                        inChase[i] = -1; // not chasing ghosts anymore
                        trapped = true;
                    }
                }

                if (trapped) {
                    moving[i][2] = 0;
                    continue;
                }

                // if chasing a ghost
                if (inChase[i] != -1) {
                    if (busters[i][0] == lastSeen[inChase[i]][0] && busters[i][1] == lastSeen[inChase[i]][1]
                            && !ghostIsVisible(ghosts, ghost, inChase[i])) {
                        lastSeen[inChase[i]][2] = 0;
                        inChase[i] = -1; // well, someone else captured that ghost...
                        moving[i][2] = 0;
                    } else {
                        // otherwise, keep doing so
                        moving[i][2] = 0;
                        System.out.println("MOVE " + lastSeen[inChase[i]][0] + " " + lastSeen[inChase[i]][1]);
                        continue;
                    }
                }

                // if doing nothing special and found an enemy, attack him
                if (lastStun[i] + 20 < roundCount) {
                    boolean stun = false;
                    for (int e = 0; e < enemyBuster && !stun; e++) {
                        if (enemyBusters[e][4] != 2) {
                            double dist = Math.pow(enemyBusters[e][0] - busters[i][0], 2) +
                                    Math.pow(enemyBusters[e][1] - busters[i][1], 2);
                            if (dist < 3_097_600.0) {
                                System.out.println("STUN " + enemyBusters[e][2]);
                                stun = true;
                            }
                        }
                    }

                    if (stun) {
                        lastStun[i] = roundCount;
                        continue;
                    }
                }

                // searching for ghost

                // Look for ghosts have been last seen...
                double closestGhost = Double.MAX_VALUE;
                int closestGhostId = -1;
                for (int g = 0; g < lastSeen.length; g++) {
                    if (lastSeen[g][2] == 1) {
                        double dist = Math.pow(lastSeen[g][0] - busters[i][0], 2)
                                + Math.pow(lastSeen[g][1] - busters[i][1], 2);
                        if (dist < closestGhost) {
                            closestGhost = dist;
                            closestGhostId = g;
                        }
                    }
                }

                if (closestGhostId != -1) {
                    lastSeen[closestGhostId][2] = 3;
                    inChase[i] = closestGhostId;
                    System.out.println("MOVE " + lastSeen[closestGhostId][0] + " " + lastSeen[closestGhostId][1]);
                    continue;
                }

                // continue movement
                if (moving[i][2] == 1) {
                    double dist = Math.pow(moving[i][0] - busters[i][0], 2)
                            + Math.pow(moving[i][1] - busters[i][1], 2);
                    if (dist > 640_000) {
                        // move hasn't finished, keep going
                        System.out.println("MOVE " + moving[i][0] + " " + moving[i][1]);
                        continue;
                    }
                }

                // finally, find spot to go to
                int x, y;
                do {
                    // Searching for unknown ghosts
                    x = random.nextInt(16000);
                    y = random.nextInt(9000);
                } while (!isClosestAvailableBuster(x, y, i, busters)
                        && isExploredPoint(x, y, visitedPoints, visitedPointsCount));

                moving[i][0] = x;
                moving[i][1] = y;
                moving[i][2] = 1;
                System.out.println("MOVE " + x + " " + y); // MOVE x y | BUST id | RELEASE
            }
        }
    }

    private static boolean ghostIsVisible(int[][] ghosts, int count, int id) {
        for (int i = 0; i < count; i++) {
            if (ghosts[i][2] == id) {
                return true;
            }
        }

        return false;
    }

    private static boolean thereIsGhostsInRange(int[][] ghosts, int ghostCount, int[] buster) {
        for (int i = 0; i < ghostCount; i++) {
            double dist = Math.pow(ghosts[i][0] - buster[0], 2) + Math.pow(ghosts[i][1] - buster[1], 2);
            if (dist < 4_840_000.0) {
                return true;
            }
        }

        return false;
    }

    private static boolean isExploredPoint(int x, int y, int[][] visitedPoints, int visitedPointsCount) {
        for (int i = 0; i < visitedPointsCount; i++) {
            if (Math.pow(x - visitedPoints[i][0], 2) + Math.pow(y - visitedPoints[i][1], 2) < 4_840_000.0) {
                System.err.println("explored");
                return true;
            }
        }

        return false;
    }

    private static boolean isClosestAvailableBuster(int x, int y, int busterId, int[][] busters) {
        double dist = Math.pow(x - busters[busterId][0], 2) + Math.pow(y - busters[busterId][1], 2);
        for (int i = 0; i < busters.length; i++) {
            if (busters[i][2] != busterId && busters[i][3] == 0) {
                if (dist > Math.pow(x - busters[i][0], 2) + Math.pow(y - busters[i][1], 2)) {
                    System.err.println("For point (" + x + ", " + y + ") buster " + i + " is closer than me "
                            + busterId);
                    return false;
                }
            }
        }

        return true;
    }
    
    static class StateMachineAI extends AI {
        int bustersPerPlayer, ghostCount, myTeamId;

        StateMachineAI(IntSupplier inputSupplier) {
            super(Collections.emptyMap(), inputSupplier);
            this.bustersPerPlayer = inputSupplier.getAsInt(); // the amount of busters you control
            this.ghostCount = inputSupplier.getAsInt(); // the amount of ghosts on the map
            this.myTeamId = inputSupplier.getAsInt(); // if this is 0, your base is on the top left of the map, if it is one, on the
            // bottom right
        }

        @Override
        Action play() {
            return null;
        }

        @Override
        void reset() {
            //ILB
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
        abstract Action play();

        Map<String, Object> getConf() {
            return conf;
        }

        int readInput(){
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

        int getX() {
            return x;
        }

        int getY() {
            return y;
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
    }

    final static class Ghost extends Entity {
        private final int stamina;
        private final int trappersCount;

        Ghost(int id, int x, int y, int stamina, int trappersCount) {
            super(id, EntityType.GHOST, x, y);
            this.stamina = stamina;
            this.trappersCount = trappersCount;
        }

        int getStamina() {
            return stamina;
        }

        int getTrappersCount() {
            return trappersCount;
        }
    }
    
    enum BusterState {
        IDLE, CARRYING_GHOST, STUNNED, TRAPPING
    }

    enum Team

    /**
     * Represents the game state
     */
    static final class State implements Cloneable {

        static final int MAX_NUMBER_OF_ROUNDS = 400;

        // Permanent values
        private final int bustersPerPlayer;
        private final int ghostCount;
        private final int myTeamId; // TODO: keep it an int or change it to enum?

        private final int[][] lastSeen;
        private final int[][] visitedPoints;
        private final int[][] moving;
        private final int[] inChase;
        private final int[] lastStun;

        // Turn state
        private int round;
        private int playerScore;
        private Buster[] busters;
        private Buster[] enemyBusters;
        private Ghost[] ghosts;

        State(int bustersPerPlayer, int ghostCount, int myTeamId) {
            this.bustersPerPlayer = bustersPerPlayer;
            this.ghostCount = ghostCount;
            this.myTeamId = myTeamId;

            this.lastSeen = new int[ghostCount][3];
            this.visitedPoints = new int[bustersPerPlayer * MAX_NUMBER_OF_ROUNDS][2];
            this.moving = new int[bustersPerPlayer][3];
            this.inChase = new int[bustersPerPlayer];
            this.lastStun = new int[bustersPerPlayer];

            Arrays.fill(inChase, -1);
            Arrays.fill(lastStun, -20);
        }

        void incrementRound() {
            round++;
        }

        /**
         * Performs an action (which will mutate the game state)
         *
         * @param action to perform
         */
        void perform(Action action) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        protected State clone() {
            throw new UnsupportedOperationException("Not implemented");
        }

        int getPlayerScore() {
            return playerScore;
        }

        public Buster[] getBusters() {
            return busters;
        }

        public void setBusters(Buster[] busters) {
            this.busters = busters;
        }

        public Buster[] getEnemyBusters() {
            return enemyBusters;
        }

        public void setEnemyBusters(Buster[] enemyBusters) {
            this.enemyBusters = enemyBusters;
        }

        public Ghost[] getGhosts() {
            return ghosts;
        }

        public void setGhosts(Ghost[] ghosts) {
            this.ghosts = ghosts;
        }
    }

    /**
     * Represents an action that can be taken
     */
    static final class Action {

        Action() {
            // TODO: implement what action is
        }

        String asString() {
            return "";
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
