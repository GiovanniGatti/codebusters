package game;

import java.util.Random;

/**
 * Created by giovanni on 01/07/16.
 */
public class Cal {

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
    private static double[][] map;

    public static void main(String args[]) throws Exception {
        int myTeamId = 0;

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
            }
        }

        // point (3456, 7845)
//        int[][] points = new int[2][2];
//        points[0][0] = 1300;
//        points[0][1] = 2000;
//        points[1][0] = 4500;
//        points[1][1] = 4000;
//
//        double score = evaluate(map, points);
//        System.out.println("score " + score);
//
//        updateMap(map, points[0][0], points[0][1]);
//        updateMap(map, points[1][0], points[1][1]);


        int[][] plan = planner(map, 1000, 1000, 15000, 8000);

        int x = 1000;
        int y = 1000;

        int x2 = 15000;
        int y2 = 8000;
        int[][] movement = new int[plan.length][];
        for (int i = 0; i < plan.length; i++) {
            if (i % 2 == 0) {
                int[] move = move(x, y, plan[i][0], plan[i][1]);
                movement[i] = move;
                x = move[0];
                y = move[1];
            } else {
                int[] move = move(x2, y2, plan[i][0], plan[i][1]);
                movement[i] = move;
                x2 = move[0];
                y2 = move[1];
            }
        }

        for (int i = 0; i < plan.length; i++) {
            System.out.println(movement[i][0] + " " + movement[i][1]);
            updateMap(map, movement[i][0], movement[i][1]);
        }

        /*

        Random random = new Random();
        int x = 1000;
        int y = 1000;

        int x2 = 15000;
        int y2 = 8000;
        int[][] randomPlan = new int[44][];
        for (int i = 0; i < randomPlan.length; i++) {
            if (i % 2 == 0) {
                int[] move = move(x, y, random.nextInt(9000), random.nextInt(16000));
                randomPlan[i] = move;
                x = move[0];
                y = move[1];
            } else {
                int[] move = move(x2, y2, random.nextInt(9000), random.nextInt(16000));
                randomPlan[i] = move;
                x2 = move[0];
                y2 = move[1];
            }
        }

        for (int i = 0; i < randomPlan.length; i++) {
            System.out.println(randomPlan[i][0] + " " + randomPlan[i][1]);
            updateMap(map, randomPlan[i][0], randomPlan[i][1]);
        }
*/
        for (int i = 0; i < map[0].length; i++) {
            for (int j = 0; j < map.length; j++) {
                System.out.print("|" + map[j][i]);
                if (j == map.length - 1) {
                    System.out.println("");
                }
            }
        }
    }

    // n points for each one of the busters organized in seq
    private static double evaluate(double[][] map, int[][] points) {
        boolean[][] mask = new boolean[map.length][map[0].length];
        double score = 0.0;

        for (int[] point : points) {

            int x = point[0];
            int y = point[1];

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
                        mask[j][i] = true;
                        score += map[j][i];
                    }
                }
            }
        }

        return score;
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

    private static int[][] planner(double[][] map, int x1, int y1, int x2, int y2) {
        int px1 = x1;
        int py1 = y1;
        int px2 = x2;
        int py2 = y2;

        Random random = new Random();

        //break it up in multiple genomes of 6 movements
        int[][][] population = new int[100][88][2];
        for (int i = 0; i < population.length; i++) {
            for (int j = 0; j < population[i].length; j++) {
                population[i][j][0] = random.nextInt(16000);
                population[i][j][1] = random.nextInt(9000);
            }
        }

//        double totalScore = 0.0;
        //double previousTotalScore;
        int generationCount = 0;
        double[] scores;
        do {
            // previousTotalScore = totalScore;
            scores = new double[population.length];

            for (int i = 0; i < population.length; i++) {
                int[][] targets = new int[population[0].length][2];

                for (int j = 0; j < population[i].length; j++) {

                    if (j % 2 == 0) {
                        int[] move = move(px1, py1, population[i][j][0], population[i][j][1]);
                        targets[j] = move;
                        px1 = move[0];
                        py1 = move[1];
                    } else {
                        int[] move = move(px2, py2, population[i][j][0], population[i][j][1]);
                        targets[j] = move;
                        px2 = move[0];
                        py2 = move[1];
                    }
                }

                scores[i] = evaluate(map, targets);
//                for (int[] target : targets) {
//                    System.out.println(target[0] + ", " + target[1]);
//                }
            }

//            totalScore = 0.0;
//            for (double score : scores) {
//                totalScore += score;
//            }

//            for (int j = 0; j < population.length; j++) {
//                System.out.println(j + ": " + scores[j]);
//            }
//            System.out.println("total: " + totalScore);
//            System.out.println("-----");

            int nextGenerationCount = 0;
            int[][][] nextGeneration = new int[population.length][][];
            do {

                int breeding1 = roulleteWheel(scores);
                int breeding2 = roulleteWheel(scores);

                //cross over
                if (random.nextDouble() < .7) {
                    int randomGene = random.nextInt(population[0].length);

                    int[][] child1 = new int[population[0].length][2];
                    int[][] child2 = new int[population[0].length][2];

                    for (int j = 0; j < randomGene; j++) {
                        child1[j] = population[breeding1][j];
                        child2[j] = population[breeding2][j];
                    }

                    for (int j = randomGene; j < population[0].length; j++) {
                        child1[j] = population[breeding2][j];
                        child2[j] = population[breeding1][j];
                    }

                    nextGeneration[nextGenerationCount++] = child1;
                    nextGeneration[nextGenerationCount++] = child2;
                }

                //mutation
                if (random.nextInt(1000) < 1) {
                    int mutate = random.nextInt(population.length);
                    int randomGene = random.nextInt(population[0].length);
                    population[mutate][randomGene][0] = random.nextInt(16000);
                    population[mutate][randomGene][1] = random.nextInt(9000);

                    nextGeneration[nextGenerationCount++] = new int[population[0].length][2];
                    for (int i = 0; i < population[0].length; i++) {
                        if (randomGene != i) {
                            nextGeneration[nextGenerationCount - 1][i][0] = population[mutate][i][0];
                            nextGeneration[nextGenerationCount - 1][i][1] = population[mutate][i][1];
                        } else {
                            nextGeneration[nextGenerationCount - 1][i][0] = random.nextInt(16000);
                            nextGeneration[nextGenerationCount - 1][i][1] = random.nextInt(9000);
                        }
                    }
                }
            } while (nextGenerationCount < population.length - 1);


        } while (generationCount++ < 10);

//        for (int i = 0; i < population.length; i++) {
//            for (int j = 0; j < population[i].length; j++) {
//                System.out.println("(" + population[i][j][0] + ", " + population[i][j][0] + ")");
//            }
//
//            System.out.println("---");
//        }

        double max = Double.NEGATIVE_INFINITY;
        int maxIndex = 0;
        for (int i = 0; i < population.length; i++) {
            if (scores[i] > max) {
                maxIndex = i;
                max = scores[i];
            }
        }

        return population[maxIndex];
    }

    private static int roulleteWheel(double[] scores) {
        int random = new Random().nextInt(101);//0 - 100;

        double totalScore = 0.0;
        for (int j = 0; j < scores.length; j++) {
            totalScore += scores[j];
        }

        double prob = 0;
        for (int j = 0; j < scores.length; j++) {
            prob += 100 * (scores[j] / totalScore);

            if (prob >= random) {
                return j;
            }
        }

        throw new IllegalStateException("Well, fuck this");
    }

    private static int[] move(int x, int y, int targetX, int targetY) {
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
}
