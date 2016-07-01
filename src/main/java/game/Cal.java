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
        int myTeamId = 1;

        double[][] map = new double[MAP_X_SIZE / MAP_RESOLUTION][MAP_Y_SIZE / MAP_RESOLUTION];
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                map[i][j] = 0.6;

                //lower triangular area
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
                    //upper base
                    map[i][j] = 0.0;
                } else if (map.length - (BASE_RANGE / MAP_RESOLUTION) <= i &&
                        map[0].length - (BASE_RANGE / MAP_RESOLUTION) <= j) {
                    //lower base
                    map[i][j] = 0.0;
                }
            }
        }

        updateMap(map, 3456, 7845);

        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                System.out.print("|" + map[i][j]);
                if (j == map[i].length - 1) {
                    System.out.println("");
                }
            }
        }
    }

    private static void updateMap(double[][] map, int x, int y) {
        int upperX = x - FOW_RANGE;
        int upperY = y - FOW_RANGE;

        int bottomX = x + FOW_RANGE;
        int bottomY = y + FOW_RANGE;

        System.out.println("i stars at " + (upperX - upperX % MAP_RESOLUTION) / MAP_RESOLUTION);
        System.out.println("i ends at " + (bottomX + MAP_RESOLUTION - bottomX % 200) / MAP_RESOLUTION);
        System.out.println("j stars at " + (upperY - upperY % MAP_RESOLUTION) / MAP_RESOLUTION);
        System.out.println("j ends at " + (bottomY + MAP_RESOLUTION - bottomY % 200) / MAP_RESOLUTION);

        for (int i = (upperX - upperX % MAP_RESOLUTION) / MAP_RESOLUTION; i < (bottomX + MAP_RESOLUTION - bottomX % 200) / MAP_RESOLUTION && i < map.length; i++) {
            int blockX = i * MAP_RESOLUTION;
            for (int j = (upperY - upperY % MAP_RESOLUTION) / MAP_RESOLUTION; j < (bottomY + MAP_RESOLUTION - bottomY % 200) / MAP_RESOLUTION && j < map[i].length; j++) {
                int blockY = j * MAP_RESOLUTION;
                System.out.println(i + ", " + j);
                System.out.println(blockX + ", " + blockY);
                if (blockX < x && blockY < y) {
                    //upper left corner
                    if ((blockX - x) * (blockX - x) +
                            (blockY - y) * (blockY - y) <= SQUARE_FOW_RANGE) {
                        map[i][j] = 0.0;
                    }
                } else if (blockX >= x && j < blockY) {
                    //upper right corner
                    if (((blockX + MAP_RESOLUTION) - x) * ((blockX + MAP_RESOLUTION) - x) +
                            (blockY - y) * (blockY - y) <= SQUARE_FOW_RANGE) {
                        map[i][j] = 0.0;
                    }
                } else if (blockX >= x && j >= blockY) {
                    //lower right corner
                    if (((blockX + MAP_RESOLUTION) - x) * ((blockX + MAP_RESOLUTION) - x) +
                            ((blockY + MAP_RESOLUTION) - y) * ((blockY + MAP_RESOLUTION) - y) <= SQUARE_FOW_RANGE) {
                        map[i][j] = 0.0;
                    }
                } else {
                    //lower left corner
                    if ((blockX - x) * (blockX - x) +
                            (blockY + MAP_RESOLUTION - y) * (blockY + MAP_RESOLUTION - y) <= SQUARE_FOW_RANGE) {
                        map[i][j] = 0.0;
                    }
                }
            }
        }
    }
}
