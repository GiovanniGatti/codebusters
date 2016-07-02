package game;

import game.Player.Buster;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static game.Cal.updateMap;

/**
 * Created by giovanni on 02/07/16.
 */
public class Explorer {
    static final int MAP_X_SIZE = 16_000;
    static final int MAP_Y_SIZE = 9_000;
    static final int MAP_RESOLUTION = 200;
    static final int SQUARE_MAP_RESOLUTION = MAP_RESOLUTION * MAP_RESOLUTION;

    static final int MOVEMENT_RANGE = 800;

    static final double BASE_RANGE = 1_600.0;

    // y = a * x + b (map diagonal)
    static final double B = ((double) MAP_Y_SIZE / MAP_RESOLUTION);
    static final double A = -B / (((double) MAP_X_SIZE) / MAP_RESOLUTION);

    static final int MAP_X_CENTRAL_POINT = (MAP_X_SIZE / MAP_RESOLUTION) / 2;
    static final int MAP_Y_CENTRAL_POINT = (MAP_Y_SIZE / MAP_RESOLUTION) / 2;

    static final int FOW_RANGE = 2200;
    static final int SQUARE_FOW_RANGE = FOW_RANGE * FOW_RANGE;

    private double[][] map;
    private final Buster[] busters;
    private final Random random;

    public static void main(String args[]) throws Exception {
        Buster b1 = new Buster(0, 1195, 2020, 0, 0);
        Buster b2 = new Buster(0, 1992, 1148, 0, 0);
        double[][] map = generateMap(0);
        Explorer explorer = new Explorer(map, b1, b2);

        long currentTimeMillis = System.currentTimeMillis();
        Chromosome chromosome = explorer.find(10, 16, 8);
        System.out.println(System.currentTimeMillis() - currentTimeMillis);
        int[][] movement = new int[chromosome.genes.length + chromosome.busters.length][2];

        for (int i = 0; i < chromosome.busters.length; i++) {
            movement[i][0] = chromosome.busters[i].getX();
            movement[i][1] = chromosome.busters[i].getY();
        }

        for (int i = chromosome.busters.length; i < chromosome.genes.length; i++) {
            int[] move = Chromosome.move(
                    movement[i - chromosome.busters.length][0],
                    movement[i - chromosome.busters.length][1],
                    chromosome.genes[i][0],
                    chromosome.genes[i][1]);
            movement[i] = move;
        }

        for (int i = 0; i < chromosome.genes.length; i++) {
//            System.out.println(movement[i][0] + " " + movement[i][1]);
            updateMap(map, movement[i][0], movement[i][1]);
        }

        for (int i = 0; i < map[0].length; i++) {
            for (int j = 0; j < map.length; j++) {
                System.out.print("|" + map[j][i]);
                if (j == map.length - 1) {
                    System.out.println("");
                }
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

                //test
                if (i <= 25 && j >= 25) {
                    map[i][j] = 1.0;
                }
            }
        }

        return map;
    }

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
                if (nextDouble < .15) {
                    gene[0] = random.nextInt(5500);
                    gene[1] = random.nextInt(3500) + 5500;
                } else if (nextDouble < .30) {
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
