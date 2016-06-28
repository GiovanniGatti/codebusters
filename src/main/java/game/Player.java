package game;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

final class Player {

    public static void main(String args[]) {
        Random random = new Random();

        Scanner in = new Scanner(System.in);
        int bustersPerPlayer = in.nextInt(); // the amount of busters you control
        int ghostCount = in.nextInt(); // the amount of ghosts on the map
        int myTeamId = in.nextInt(); // if this is 0, your base is on the top left of the map, if it is one, on the
        // bottom right

        int[][] lastSeen = new int[ghostCount][3]; // (x, y) / 0 -> unknown, 1 -> valid, 2-> trapped, 3 -> in chase
        int[][] visitedPoints = new int[bustersPerPlayer * 400][2]; // (x, y)
        int visitedPointsCount = 0;

        int moving[][] = new int[bustersPerPlayer][3]; // (x, y) / 1 -> going, 0 -> not going

        int[] inChase = new int[bustersPerPlayer]; // busterId => ghostId ; -1 => not chasing anything

        int[][] enemyBusters = new int[bustersPerPlayer][4]; // (x, y) / id / 0 -> idle, 1 -> carrying a ghost

        int[] lastStun = new int[bustersPerPlayer]; // last round where buster stunned someone

        int roundCount = 0;

        Arrays.fill(inChase, -1);
        Arrays.fill(lastStun, -20);

        // game loop
        while (true) {
            roundCount++;
            int[][] busters = new int[bustersPerPlayer][5]; // (x, y) / id / idle / ghostId
            int[][] ghosts = new int[ghostCount][3]; // (x, y) / id /

            int buster = 0;
            int ghost = 0;
            int enemyBuster = 0;

            int entities = in.nextInt(); // the number of busters and ghosts visible to you
            for (int i = 0; i < entities; i++) {
                int entityId = in.nextInt(); // buster id or ghost id
                int x = in.nextInt();
                int y = in.nextInt(); // position of this buster / ghost
                int entityType = in.nextInt(); // the team id if it is a buster, -1 if it is a ghost.
                int state = in.nextInt(); // For busters: 0=idle, 1=carrying a ghost.
                int value = in.nextInt(); // For busters: Ghost id being carried. For ghosts: number of busters
                // attempting to trap this ghost.

                if (entityType == myTeamId) {
                    busters[buster][0] = x;
                    busters[buster][1] = y;
                    busters[buster][2] = entityId;
                    busters[buster][3] = state;
                    busters[buster][4] = value;
                    buster++;

                    visitedPoints[visitedPointsCount][0] = x;
                    visitedPoints[visitedPointsCount][1] = y;
                    visitedPointsCount++;
                } else if (entityType == -1) {
                    ghosts[ghost][0] = x;
                    ghosts[ghost][1] = y;
                    ghosts[ghost][2] = entityId;
                    lastSeen[entityId][0] = x;
                    lastSeen[entityId][1] = y;
                    lastSeen[entityId][2] = 1;
                    ghost++;
                } else {
                    enemyBusters[enemyBuster][0] = x;
                    enemyBusters[enemyBuster][1] = y;
                    enemyBusters[enemyBuster][2] = entityId;
                    enemyBusters[enemyBuster][3] = value;
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

            for (int i = 0; i < bustersPerPlayer; i++) {

                // return ghost to base
                //TODO: ghost being released too late. Why?
                if (busters[i][3] == 1) {
                    moving[i][2] = 0;
                    // carrying a ghost
                    double distToCorner = Math.pow(myTeamId * 16000 - busters[i][0], 2) +
                            Math.pow(myTeamId * 9000 - busters[i][1], 2);
                    if (distToCorner < 2_560_000.0) {
                        System.out.println("RELEASE");
                    } else {
                        System.out.println("MOVE 0 0");
                    }
                    continue;
                }

                // if found an enemy carrying a ghost, attack him
                // TODO: two busters should not stun the same enemy id
                if (lastStun[i] + 20 > roundCount) {
                    boolean stun = false;
                    for (int e = 0; e < enemyBuster && !stun; e++) {
                        if (enemyBusters[e][3] == 1) {
                            double dist = Math.pow(enemyBusters[e][0] - busters[i][0], 2) +
                                    Math.pow(enemyBusters[e][1] - busters[i][1], 2);
                            if (dist < 3_097_600.0) {
                                System.out.println("STUN " + enemyBusters[i][2]);
                                stun = true;
                            }
                        }
                    }

                    if (stun) {
                        lastStun[i] = roundCount;
                        continue;
                    }
                }

                //TODO: but still persists when trapping a ghost
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
                // TODO: two busters should not stun the same enemy id
                if (lastStun[i] + 20 > roundCount) {
                    boolean stun = false;
                    for (int e = 0; e < enemyBuster && !stun; e++) {
                        double dist = Math.pow(enemyBusters[e][0] - busters[i][0], 2) +
                                Math.pow(enemyBusters[e][1] - busters[i][1], 2);
                        if (dist < 3_097_600.0) {
                            System.out.println("STUN " + enemyBusters[i][2]);
                            stun = true;
                        }
                    }

                    if (stun) {
                        lastStun[i] = roundCount;
                        continue;
                    }
                }

                // searching for ghost

                // Look for ghosts have been last seen...
                // TODO: when chasing a ghost, do not chase one that is already being chased
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

    static abstract class AI {

        private final Map<String, Object> conf;

        /**
         * Builds an AI with specified configuration.<br>
         * If the AI does not need a configuration, an empty one may be provided.<br>
         * It is also recommended to create a default configuration.
         */
        AI(Map<String, Object> conf) {
            this.conf = Collections.unmodifiableMap(conf);
        }

        /**
         * Implements the IA algorithm
         *
         * @param current the current state
         * @return the best action found
         */
        abstract Action play(State current);

        Map<String, Object> getConf() {
            return conf;
        }

        /**
         * If eventually the AI is not stateless, i.e. it learns something during a game play, this method may be used
         * to forget anything before another match. Leave it blank if IA doesn't learn anything, or you want the AI to
         * keep its knowledge.
         */
        abstract void reset();
    }

    /**
     * Represents the game state
     */
    static final class State implements Cloneable {

        private int playerScore;
        private int opponentScore;

        State() {
            playerScore = 0;
            opponentScore = 0;
            // TODO: implement what a game state is (all game input variables)
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

        int getOpponentScore() {
            return opponentScore;
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
