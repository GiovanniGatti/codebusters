package game;

/**
 * Created by giovanni on 01/07/16.
 */
public class Cal {

    static final int MAP_X_SIZE = 16_000;
    static final int MAP_Y_SIZE = 9_000;
    static final int MAP_RESOLUTION = 200;
    static final int SQUARE_MAP_RESOLUTION = MAP_RESOLUTION * MAP_RESOLUTION;

    static final double BASE_RANGE = 1_600.0;

    // y = a * x + b (map diagonal)
    static final double B = ((double) MAP_Y_SIZE / MAP_RESOLUTION);
    static final double A = -B / (((double) MAP_X_SIZE) / MAP_RESOLUTION);

    static final int MAP_X_CENTRAL_POINT = (MAP_X_SIZE / MAP_RESOLUTION) / 2;
    static final int MAP_Y_CENTRAL_POINT = (MAP_Y_SIZE / MAP_RESOLUTION) / 2;

    static final int FOW_RANGE = 2200;
    static final int SQUARE_FOW_RANGE = FOW_RANGE * FOW_RANGE;

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
        int[][] points = new int[2][2];
        points[0][0] = 1300;
        points[0][1] = 2000;
        points[1][0] = 4500;
        points[1][1] = 4000;

        double score = evaluate(map, points);
        System.out.println("score " + score);

        updateMap(map, points[0][0], points[0][1]);
        updateMap(map, points[1][0], points[1][1]);

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

            for (int i = upperX; i < bottomX && i < map[0].length; i++) {
                int blockX = i * MAP_RESOLUTION;
                for (int j = upperY; j < bottomY && j < map.length; j++) {

                    if (map[j][i] == 0.0 || mask[j][i]) {
                        continue;
                    }

                    int blockY = j * MAP_RESOLUTION;

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

        for (int i = upperX; i < bottomX && i < map[0].length; i++) {
            int blockX = i * MAP_RESOLUTION;
            for (int j = upperY; j < bottomY && j < map.length; j++) {

                if (map[j][i] == 0.0) {
                    continue;
                }

                int blockY = j * MAP_RESOLUTION;

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
}
