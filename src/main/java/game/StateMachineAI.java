package game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.IntSupplier;

class StateMachineAI extends Player.AI {

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
    Player.Action[] play() {
        this.round++;

        Triple<List<Player.Buster>, List<Player.Buster>, List<Player.Ghost>> triple = load();

        List<Player.Buster> busters = triple.x;
        List<Player.Buster> enemyBusters = triple.y;
        List<Player.Ghost> ghosts = triple.z;

        printState(busters, enemyBusters, ghosts);

        Player.Action[] busterActions = new Player.Action[bustersPerPlayer];

        for (int i = 0; i < bustersPerPlayer; i++) {

            Player.Buster buster = busters.get(i);
            Player.Action action = searchBusterAction(buster, busters, enemyBusters, ghosts);
            busterActions[i] = action;
        }

        return busterActions;
    }

    private Player.Action searchBusterAction(
            Player.Buster buster,
            List<Player.Buster> busters,
            List<Player.Buster> enemyBusters,
            List<Player.Ghost> ghosts) {

        int busterId = buster.getId();

        // return ghost to base
        Target target = movingTo[busterId];

        if (buster.getState() == Player.BusterState.CARRYING_GHOST) {
            target.valid = false;
            // carrying a ghost
            double distToCorner = Math.pow(myTeamId * 16000 - buster.getX(), 2) +
                    Math.pow(myTeamId * 9000 - buster.getY(), 2);
            if (distToCorner < 2_560_000.0) {
                playerScore++;
                return new Player.Release();
            } else if (myTeamId == 0) {
                // compute intersection point between a line and a circle,
                // in which both pass through the point (0, 0)

                // y = A*x + B, where B = 0
                double a = ((double) buster.getY()) / buster.getX();

                // (x-a)^2 + (y-b)^2 = R^2
                double x = Math.sqrt(2560000.0 / (1.0 + Math.pow(a, 2)));
                double y = a * x;
                return new Player.Move(((int) (x - 1.0)), ((int) (y - 1.0)));
            } else {
                // playing on mirror mode
                // FIXME: something is still not perfect
                double a = ((double) 16000 - buster.getY()) / (9000 - buster.getX());
                double x = Math.sqrt(2560000.0 / (1.0 + Math.pow(a, 2)));
                double y = a * x;
                return new Player.Move(((int) (16001.0 - x)), ((int) (9001.0 - y)));
            }
        }

        // if found an enemy carrying a ghost, attack him
        // TODO: two busters should not stun the same enemy id
        // TODO: stop and move closer if enemy is not in perfect range...
        // TODO: detect when he is in the fog?
        if (lastStun[busterId] + 20 < round) {
            for (Player.Buster enemyBuster : enemyBusters) {
                Player.BusterState state = enemyBuster.getState();
                if (state == Player.BusterState.CARRYING_GHOST || state == Player.BusterState.TRAPPING ||
                        (state != Player.BusterState.STUNNED && thereIsGhostsInRange(ghosts, buster))) {
                    long dist = enemyBuster.squareDistTo(buster);
                    if (dist < SQUARE_STUN_RANGE) {
                        lastStun[busterId] = round;
                        return new Player.Stun(enemyBuster.getId());
                    }
                }
            }
        }

        // trapping a ghost
        for (Player.Ghost ghost : ghosts) {
            double dist = buster.squareDistTo(ghost);
            if (dist > SQUARE_MIN_BUST_RANGE && dist < SQUARE_MAX_BUST_RANGE) {
                int ghostId = ghost.getId();
                foundGhosts[ghostId].state = GhostPosState.TRAPPED;
                chasingGhost[busterId] = -1; // not chasing ghosts anymore
                target.valid = false;
                return new Player.Bust(ghostId);
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
                return new Player.Move(foundGhost.x, foundGhost.y);
            }
        }

        // if doing nothing special and found an enemy, attack him
        if (lastStun[busterId] + 20 < round) {
            for (Player.Buster enemyBuster : enemyBusters) {
                if (enemyBuster.getState() != Player.BusterState.STUNNED) {
                    double dist = buster.squareDistTo(enemyBuster);
                    if (dist < SQUARE_STUN_RANGE) {
                        lastStun[busterId] = round;
                        return new Player.Stun(enemyBuster.getId());
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
            return new Player.Move(foundGhost.x, foundGhost.y);
        }

        // continue movement
        if (target.valid) {
            double dist = buster.squareDistTo(target.x, target.y);
            if (dist > SQUARE_BUSTER_MAX_MOVEMENT_RANGE) {
                // move hasn't finished, keep going
                return new Player.Move(target.x, target.y);
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
        return new Player.Move(x, y);
    }

    private Triple<List<Player.Buster>, List<Player.Buster>, List<Player.Ghost>> load() {
        List<Player.Buster> busters = new ArrayList<>(bustersPerPlayer);
        List<Player.Buster> enemyBusters = new ArrayList<>(bustersPerPlayer);
        List<Player.Ghost> ghosts = new ArrayList<>(ghostCount);


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

                visitedPoints[visitedPointsCount][0] = x;
                visitedPoints[visitedPointsCount][1] = y;
                visitedPointsCount++;
            } else if (entityType == -1) {
                ghosts.add(new Player.Ghost(entityId, x, y, state, value));

                foundGhosts[entityId] = new GhostPos(x, y, GhostPosState.FOUND);
            } else {
                enemyBusters.add(new Player.Buster(entityId, x, y, state, value));
            }
        }

        return new Triple<>(busters, enemyBusters, ghosts);
    }

    private void printState(List<Player.Buster> busters, List<Player.Buster> enemyBusters, List<Player.Ghost> ghosts) {
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

    private static boolean ghostIsVisible(List<Player.Ghost> ghosts, int id) {
        return ghosts.stream()
                .anyMatch(g -> g.getId() == id);
    }

    private static boolean thereIsGhostsInRange(List<Player.Ghost> ghosts, Player.Buster buster) {
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

    private static boolean isClosestAvailableBuster(int x, int y, Player.Buster buster, List<Player.Buster> busters) {
        double dist = buster.squareDistTo(x, y);
        return busters.stream()
                .filter(b -> b.getId() != buster.getId())
                .filter(b -> b.getState() == Player.BusterState.IDLE)
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
