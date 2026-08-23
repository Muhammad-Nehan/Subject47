package subject47.world;

public class LabGenerator {

    private static final int WALL_HEIGHT = 6;

    public void generateLevel1(Chunk chunk) {
        buildBaseFacility(chunk);
        corridor(chunk, 18, 18, 24, 20);
        airlock(chunk, 10, 5, 12, 8);
        serviceSpine(chunk, 24, 3, 41, 15);
        observationWindow(chunk, 28, 16, 36);
        doorPortal(chunk, 11, 7);
        doorPortal(chunk, 14, 18);
        doorPortal(chunk, 18, 19);
        doorPortal(chunk, 24, 20);
        doorPortal(chunk, 33, 20);
    }

    public void generateLevel2(Chunk chunk) {
        buildBaseFacility(chunk);
        room(chunk, 18, 18, 29, 28, true);
        lightStrip(chunk, 20, 20, 27, 20, false);
        serviceSpine(chunk, 7, 15, 18, 24);
        airlock(chunk, 30, 17, 33, 19);
        doorPortal(chunk, 14, 18);
        doorPortal(chunk, 24, 20);
        doorPortal(chunk, 33, 20);
        doorPortal(chunk, 31, 17);
    }

    public void generateLevel3(Chunk chunk) {
        buildBaseFacility(chunk);
        room(chunk, 36, 10, 46, 18, false);
        corridor(chunk, 34, 12, 38, 14);
        column(chunk, 31, 1, 6);
        column(chunk, 24, 1, 23);
        lightStrip(chunk, 36, 14, 44, 14, true);
        observationWindow(chunk, 36, 11, 16);
        serviceSpine(chunk, 34, 10, 45, 17);
        doorPortal(chunk, 31, 8);
        doorPortal(chunk, 41, 28);
        doorPortal(chunk, 38, 13);
    }

    public void generateLevel4(Chunk chunk) {
        buildBaseFacility(chunk);
        room(chunk, 34, 18, 46, 30, false);
        corridor(chunk, 32, 22, 36, 24);
        lightStrip(chunk, 36, 24, 44, 24, true);
        serviceSpine(chunk, 34, 19, 45, 29);
        airlock(chunk, 35, 22, 37, 24);
        doorPortal(chunk, 35, 24);
        doorPortal(chunk, 43, 24);
    }

    private void buildBaseFacility(Chunk chunk) {
        room(chunk, 2, 2, 11, 11, false);
        corridor(chunk, 11, 5, 24, 8);
        room(chunk, 6, 14, 18, 25, false);
        corridor(chunk, 12, 8, 14, 18);
        room(chunk, 24, 2, 41, 16, false);
        room(chunk, 24, 17, 31, 24, true);
        room(chunk, 33, 17, 40, 24, true);
        corridor(chunk, 31, 19, 33, 21);
        corridor(chunk, 34, 24, 38, 26);
        corridor(chunk, 38, 7, 44, 9);
        corridor(chunk, 40, 9, 43, 29);
        corridor(chunk, 32, 27, 46, 29);
        room(chunk, 32, 24, 46, 31, false);

        lightStrip(chunk, 4, 6, 9, 6, false);
        lightStrip(chunk, 13, 6, 23, 6, false);
        lightStrip(chunk, 8, 19, 16, 19, false);
        lightStrip(chunk, 26, 8, 39, 8, false);
        lightStrip(chunk, 25, 20, 30, 20, false);
        lightStrip(chunk, 34, 20, 39, 20, false);
        lightStrip(chunk, 34, 28, 43, 28, false);

        panelBand(chunk, 2, 2, 11, 11);
        panelBand(chunk, 6, 14, 18, 25);
        panelBand(chunk, 24, 2, 41, 16);
        panelBand(chunk, 26, 16, 38, 24);
        panelBand(chunk, 32, 24, 46, 31);
    }

    private void room(Chunk chunk, int x1, int z1, int x2, int z2, boolean glassWall) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                chunk.setBlock(x, 0, z, BlockType.FLOOR);
                chunk.setBlock(x, WALL_HEIGHT, z, BlockType.CEILING);
            }
        }

        for (int y = 1; y < WALL_HEIGHT; y++) {
            for (int x = x1; x <= x2; x++) {
                chunk.setBlock(x, y, z1, edgeType(x, y, true));
                chunk.setBlock(x, y, z2, edgeType(x, y, true));
            }

            for (int z = z1; z <= z2; z++) {
                chunk.setBlock(x1, y, z, edgeType(z, y, true));
                chunk.setBlock(x2, y, z, edgeType(z, y, true));
            }
        }

        ceilingGrid(chunk, x1, z1, x2, z2);

        if (glassWall) {
            int glassZ = z1;
            for (int y = 2; y < WALL_HEIGHT - 1; y++) {
                for (int x = x1 + 2; x <= x2 - 2; x++) {
                    chunk.setBlock(x, y, glassZ, BlockType.GLASS);
                }
            }
        }
    }

    private void corridor(Chunk chunk, int x1, int z1, int x2, int z2) {
        room(chunk, x1, z1, x2, z2, false);
        trimRun(chunk, x1, z1, x2, z2);
    }

    private void lightStrip(Chunk chunk, int x1, int z1, int x2, int z2, boolean redMood) {
        int colorRow = WALL_HEIGHT - 1;
        for (int x = x1; x <= x2; x += 2) {
            for (int z = z1; z <= z2; z += 2) {
                chunk.setBlock(x, colorRow, z, BlockType.LIGHT);
                if (redMood && x + 1 < Chunk.SIZE) {
                    chunk.setBlock(x + 1, colorRow, z, BlockType.WALL);
                }
            }
        }
    }

    private void column(Chunk chunk, int x, int y1, int z) {
        for (int y = y1; y < WALL_HEIGHT; y++) {
            chunk.setBlock(x, y, z, y == 1 ? BlockType.TRIM : BlockType.PANEL);
        }
    }

    private void airlock(Chunk chunk, int x1, int z1, int x2, int z2) {
        room(chunk, x1, z1, x2, z2, false);
        for (int x = x1 + 1; x < x2; x++) {
            chunk.setBlock(x, 0, z1 + 1, BlockType.TRIM);
            chunk.setBlock(x, WALL_HEIGHT, z1 + 1, BlockType.LIGHT);
        }
    }

    private void serviceSpine(Chunk chunk, int x1, int z1, int x2, int z2) {
        for (int x = x1; x <= x2; x++) {
            if (((x - x1) & 1) == 0) {
                chunk.setBlock(x, 0, z1, BlockType.TRIM);
                chunk.setBlock(x, 0, z2, BlockType.TRIM);
            }
        }
        for (int z = z1; z <= z2; z++) {
            if (((z - z1) & 2) == 0) {
                chunk.setBlock(x1, 2, z, BlockType.PANEL);
                chunk.setBlock(x2, 2, z, BlockType.PANEL);
            }
        }
    }

    private void observationWindow(Chunk chunk, int x1, int x2, int z) {
        for (int x = x1; x <= x2; x++) {
            chunk.setBlock(x, 1, z, BlockType.TRIM);
            chunk.setBlock(x, WALL_HEIGHT - 1, z, BlockType.TRIM);
            for (int y = 2; y < WALL_HEIGHT - 1; y++) {
                chunk.setBlock(x, y, z, BlockType.GLASS);
            }
        }
    }

    private void panelBand(Chunk chunk, int x1, int z1, int x2, int z2) {
        for (int x = x1 + 1; x < x2; x += 3) {
            chunk.setBlock(x, 2, z1, BlockType.PANEL);
            chunk.setBlock(x, 2, z2, BlockType.PANEL);
        }
        for (int z = z1 + 1; z < z2; z += 3) {
            chunk.setBlock(x1, 2, z, BlockType.PANEL);
            chunk.setBlock(x2, 2, z, BlockType.PANEL);
        }
    }

    private void ceilingGrid(Chunk chunk, int x1, int z1, int x2, int z2) {
        for (int x = x1 + 1; x < x2; x++) {
            for (int z = z1 + 1; z < z2; z++) {
                if (((x + z) & 3) == 0) {
                    chunk.setBlock(x, WALL_HEIGHT, z, BlockType.TRIM);
                }
            }
        }
    }

    private void trimRun(Chunk chunk, int x1, int z1, int x2, int z2) {
        for (int x = x1; x <= x2; x++) {
            if (((x - x1) & 1) == 0) {
                chunk.setBlock(x, 0, z1, BlockType.TRIM);
                chunk.setBlock(x, 0, z2, BlockType.TRIM);
            }
        }
        for (int z = z1; z <= z2; z++) {
            if (((z - z1) & 1) == 0) {
                chunk.setBlock(x1, 0, z, BlockType.TRIM);
                chunk.setBlock(x2, 0, z, BlockType.TRIM);
            }
        }
    }

    private BlockType edgeType(int position, int y, boolean horizontalEdge) {
        if (y == 1 || y == WALL_HEIGHT - 1) {
            return BlockType.TRIM;
        }
        if (horizontalEdge && (position % 4 == 0)) {
            return BlockType.PANEL;
        }
        return BlockType.WALL;
    }

    private void doorPortal(Chunk chunk, int centerX, int centerZ) {
        for (int y = 1; y < WALL_HEIGHT - 1; y++) {
            for (int x = centerX - 1; x <= centerX + 1; x++) {
                for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                    chunk.setBlock(x, y, z, BlockType.AIR);
                }
            }
        }
        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                chunk.setBlock(x, 0, z, BlockType.FLOOR);
                chunk.setBlock(x, WALL_HEIGHT, z, BlockType.CEILING);
            }
        }
    }
}
