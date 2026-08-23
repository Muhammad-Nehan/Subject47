package subject47.world;

public class Chunk {

    public static final int SIZE = 48;

    private final BlockType[][][] blocks = new BlockType[SIZE][SIZE][SIZE];

    public Chunk() {
        fillAir();
    }

    public void fillAir() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    blocks[x][y][z] = BlockType.AIR;
                }
            }
        }
    }

    public void setBlock(int x, int y, int z, BlockType type) {
        if (inBounds(x, y, z)) {
            blocks[x][y][z] = type;
        }
    }

    public BlockType getBlock(int x, int y, int z) {
        return inBounds(x, y, z) ? blocks[x][y][z] : BlockType.AIR;
    }

    private boolean inBounds(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0
                && x < SIZE && y < SIZE && z < SIZE;
    }
}
