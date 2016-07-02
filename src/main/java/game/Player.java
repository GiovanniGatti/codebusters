package game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    static class RoleBasedAI extends AI {

        static final int MAP_X_SIZE = 16_000;
        static final int MAP_Y_SIZE = 9_000;
        static final int MAP_RESOLUTION = 200;

        static final int MOVEMENT_RANGE = 800;

        static final double BASE_RANGE = 1_600.0;

        // y = a * x + b (map diagonal)
        static final double B = ((double) MAP_Y_SIZE / MAP_RESOLUTION);
        static final double A = -B / (((double) MAP_X_SIZE) / MAP_RESOLUTION);

        static final int MAP_X_CENTRAL_POINT = (MAP_X_SIZE / MAP_RESOLUTION) / 2;
        static final int MAP_Y_CENTRAL_POINT = (MAP_Y_SIZE / MAP_RESOLUTION) / 2;

        static final int FOW_RANGE = 2200;
        static final int SQUARE_FOW_RANGE = FOW_RANGE * FOW_RANGE;

        static final int MAX_NUMBER_OF_ROUNDS = 400;

        private final int bustersPerPlayer;
        private final int ghostCount;

        private final List<ExploredPoint> exploredPoints;

        private final int myTeamId;
        private final TargetPoint[] targetPoints;
        private final GhostStatus[] ghostStatuses;
        private int knownGhosts;

        private int round = 0;

        private double[][] map;

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

            this.map = generateMap(myTeamId);

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
        private PairBusterAction[] explore(Buster... busters) {


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

        private static double[][] generateMap(int myTeamId) {
            double[][] map = new double[MAP_X_SIZE / MAP_RESOLUTION][MAP_Y_SIZE / MAP_RESOLUTION];
            for (int i = 0; i < map.length; i++) {
                for (int j = 0; j < map[i].length; j++) {
                    map[i][j] = 0.6;

                    // lower triangular area
                    double y = A * i + B;
                    if ((y < j && myTeamId == 0) || (y > j && myTeamId == 1)) {
                        map[i][j] = 0.3;
                    }

                    if (((i > MAP_X_CENTRAL_POINT && j > MAP_Y_CENTRAL_POINT) && myTeamId == 0)
                            || ((i < MAP_X_CENTRAL_POINT && j < MAP_Y_CENTRAL_POINT) && myTeamId == 1)) {
                        map[i][j] = 0.1;
                    }

                    if (0 <= i && i <= (BASE_RANGE / MAP_RESOLUTION) - 1
                            && 0 <= j && j <= (BASE_RANGE / MAP_RESOLUTION) - 1) {
                        // upper base
                        map[i][j] = 0.0;
                    } else if (map.length - (BASE_RANGE / MAP_RESOLUTION) <= i &&
                            map[0].length - (BASE_RANGE / MAP_RESOLUTION) <= j) {
                        // lower base
                        map[i][j] = 0.0;
                    }

                    // closest base corner
                    if (i <= 15 && j >= 30) {
                        if (myTeamId == 0) {
                            map[i][j] = 1.0;
                        } else {
                            map[i][j] = 0.8;
                        }
                    }

                    if (i >= 65 && j <= 15) {
                        if (myTeamId == 0) {
                            map[i][j] = 0.8;
                        } else {
                            map[i][j] = 1.0;
                        }
                    }
                }
            }

            return map;
        }

        private static void updateMap(double[][] map, int x, int y) {
            int upperX = (x - FOW_RANGE) / MAP_RESOLUTION;
            int upperY = (y - FOW_RANGE) / MAP_RESOLUTION;

            int bottomX = (x + FOW_RANGE) / MAP_RESOLUTION;
            int bottomY = (y + FOW_RANGE) / MAP_RESOLUTION;

            // map bord constraints
            if (upperX < 0) {
                upperX = 0;
            }

            if (upperY < 0) {
                upperY = 0;
            }

            if (bottomX > map.length) {
                bottomX = map.length;
            }

            if (bottomY > map[0].length) {
                bottomY = map[0].length;
            }

            for (int j = upperX; j < bottomX; j++) {
                int blockX = j * MAP_RESOLUTION;
                for (int i = upperY; i < bottomY; i++) {

                    if (map[j][i] == 0.0) {
                        continue;
                    }

                    int blockY = i * MAP_RESOLUTION;

                    int dx = blockX - x;
                    int dy = blockY - y;

                    if (blockX >= x && j < blockY) {
                        // upper right corner
                        dx = (dx + MAP_RESOLUTION);
                    } else if (blockX >= x && j >= blockY) {
                        // lower right corner
                        dx = (dx + MAP_RESOLUTION);
                        dy = (dy + MAP_RESOLUTION);
                    } else if (blockX <= x && j >= blockY) {
                        // lower left corner
                        dy = (dy + MAP_RESOLUTION);
                    }

                    if (dx * dx + dy * dy <= SQUARE_FOW_RANGE) {
                        map[j][i] = 0.0;
                    }
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

        private static class PairBusterAction {
            final Buster b;
            final Action a;

            PairBusterAction(Buster b, Action a) {
                this.b = b;
                this.a = a;
            }
        }

        static class Explorer {
            private double[][] map;
            private final Buster[] busters;
            private final Random random;

            Explorer(double[][] map, Buster... busters) {
                this.map = map;
                this.busters = busters;
                this.random = new Random();
            }

            Chromosome find(int movements, int popSize, int generations) {

                // Create the pool
                List<Chromosome> pool = new ArrayList<>(popSize);
                List<Chromosome> newPool = new ArrayList<>(popSize);

                // Generate unique chromosomes in the pool
                for (int i = 0; i < popSize; i++) {
                    Chromosome chromosome = new Chromosome(movements, busters);
                    chromosome.score(map);
                    pool.add(chromosome);
                }

                // Loop until solution is found
                for (int generation = 0; generation < generations; generation++) {
                    // Clear the new pool
                    newPool.clear();

                    // Loop until the pool has been processed
                    for (int x = pool.size() - 1; x >= 0; x -= 2) {
                        // Select two members
                        Chromosome n1 = selectMember(pool);
                        Chromosome n2 = selectMember(pool);

                        // Cross over and mutate
                        n1.crossOver(n2);
                        n1.mutate();
                        n2.mutate();

                        // score new nodes
                        n1.score(map);
                        n2.score(map);

                        // Add to the new pool
                        newPool.add(n1);
                        newPool.add(n2);
                    }

                    // Add the newPool back to the old pool
                    pool.addAll(newPool);

//            double tot = 0.0;
//            for (int x = newPool.size() - 1; x >= 0; x--) {
//                double score = (newPool.get(x)).score;
//                tot += score;
//            }
//            System.out.println("generation " + generation + ": " + tot);
                }

                double maxScore = Double.NEGATIVE_INFINITY;
                Chromosome best = null;
                for (Chromosome chromosome : newPool) {
                    if (chromosome.score > maxScore) {
                        maxScore = chromosome.score;
                        best = chromosome;
                    }
                }

                return best;
            }

            private Chromosome selectMember(List<Chromosome> l) {

                // Get the total fitness
                double tot = 0.0;
                for (int x = l.size() - 1; x >= 0; x--) {
                    double score = (l.get(x)).score;
                    tot += score;
                }
                double slice = tot * random.nextDouble();

                // Loop to find the node
                double ttot = 0.0;
                for (int x = l.size() - 1; x >= 0; x--) {
                    Chromosome node = l.get(x);
                    ttot += node.score;
                    if (ttot >= slice) {
                        l.remove(x);
                        return node;
                    }
                }

                return l.remove(l.size() - 1);
            }

            private static class Chromosome {
                private final double crossoverRate;
                private final double mutationRate;
                private final Buster[] busters;
                private final Random random;

                private int[][] genes;
                private double score;

                Chromosome(int numberOfMovements, double crossoverRate, double mutationRate, Buster... busters) {
                    this.crossoverRate = crossoverRate;
                    this.mutationRate = mutationRate;
                    this.busters = busters;
                    random = new Random();
                    genes = new int[numberOfMovements * busters.length][2];
                    for (int[] gene : genes) {
                        double nextDouble = random.nextDouble();
                        //TODO: take in consideration base pos
                        if (nextDouble < .15) {
                            gene[0] = random.nextInt(5500);
                            gene[1] = random.nextInt(3500) + 5500;
                        } else if (nextDouble < .25) {
                            gene[0] = 10500 + random.nextInt(5500);
                            gene[1] = random.nextInt(5500);
                        } else {
                            gene[0] = random.nextInt(16000);
                            gene[1] = random.nextInt(9000);
                        }
                    }

                    score = 0.0;
                }

                Chromosome(int numberOfMovements, Buster... busters) {
                    this(numberOfMovements, .7, .001, busters);
                }

                void score(double[][] map) {
                    boolean[][] mask = new boolean[map.length][map[0].length];
                    double score = 0.0;

                    int[][] bustersPositions = new int[busters.length][2];
                    for (int i = 0; i < busters.length; i++) {
                        bustersPositions[i][0] = busters[i].getX();
                        bustersPositions[i][1] = busters[i].getY();
                    }

                    for (int g = 0; g < genes.length; g++) {

                        int xi = bustersPositions[g % busters.length][0];
                        int yi = bustersPositions[g % busters.length][1];

                        int xt = genes[g][0];
                        int yt = genes[g][1];

                        int[] move = move(xi, yi, xt, yt);
                        int x = move[0];
                        int y = move[1];
                        bustersPositions[g % busters.length][0] = x;
                        bustersPositions[g % busters.length][1] = y;

                        int upperX = (x - FOW_RANGE) / MAP_RESOLUTION;
                        int upperY = (y - FOW_RANGE) / MAP_RESOLUTION;

                        int bottomX = (x + FOW_RANGE) / MAP_RESOLUTION;
                        int bottomY = (y + FOW_RANGE) / MAP_RESOLUTION;

                        // map bord constraints
                        if (upperX < 0) {
                            upperX = 0;
                        }

                        if (upperY < 0) {
                            upperY = 0;
                        }

                        if (bottomX > map.length) {
                            bottomX = map.length;
                        }

                        if (bottomY > map[0].length) {
                            bottomY = map[0].length;
                        }

                        for (int j = upperX; j < bottomX; j++) {
                            int blockX = j * MAP_RESOLUTION;
                            for (int i = upperY; i < bottomY; i++) {

                                if (map[j][i] == 0.0 || mask[j][i]) {
                                    continue;
                                }

                                int blockY = i * MAP_RESOLUTION;

                                int dx = blockX - x;
                                int dy = blockY - y;

                                if (blockX >= x && j < blockY) {
                                    // upper right corner
                                    dx = (dx + MAP_RESOLUTION);
                                } else if (blockX >= x && j >= blockY) {
                                    // lower right corner
                                    dx = (dx + MAP_RESOLUTION);
                                    dy = (dy + MAP_RESOLUTION);
                                } else if (blockX <= x && j >= blockY) {
                                    // lower left corner
                                    dy = (dy + MAP_RESOLUTION);
                                }

                                if (dx * dx + dy * dy <= SQUARE_FOW_RANGE) {
                                    mask[j][i] = true;
                                    score += map[j][i];
                                }
                            }
                        }
                    }

                    this.score = score;
                }

                static int[] move(int x, int y, int targetX, int targetY) {
                    int[] point = new int[2];

                    double dist = Math.hypot(targetX - x, targetY - y);

                    if (dist < MOVEMENT_RANGE) {
                        point[0] = targetX;
                        point[1] = targetY;
                        return point;
                    }

                    double cosa = (targetX - x) / dist;
                    double sina = (targetY - y) / dist;

                    point[0] = (int) (MOVEMENT_RANGE * cosa) + x;
                    point[1] = (int) (MOVEMENT_RANGE * sina) + y;

                    return point;
                }

                void crossOver(Chromosome another) {
                    if (random.nextDouble() < crossoverRate) {
                        int randomGene = random.nextInt(genes.length);

                        int[][] child1 = new int[genes.length][];
                        int[][] child2 = new int[genes.length][];

                        for (int j = 0; j < randomGene; j++) {
                            child1[j] = genes[j];
                            child2[j] = another.genes[j];
                        }

                        for (int j = randomGene; j < genes.length; j++) {
                            child1[j] = another.genes[j];
                            child2[j] = genes[j];
                        }

                        this.genes = child1;
                        another.genes = child2;
                    }
                }

                void mutate() {
                    for (int[] gene : genes) {
                        if (random.nextDouble() <= mutationRate) {
                            gene[0] = random.nextInt(16000);
                            gene[1] = random.nextInt(9000);
                        }
                    }
                }
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
