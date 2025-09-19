import java.util.ArrayList;
import java.util.List;

public class JavaDemo {

    private static final int TIMES = 10;
    private static final int BLOCK_SIZE = 1 * 1024 * 1024; // 1 MiB
    private static final int BLOCKS_PER_ROUND = 5;         // Every round we add 5 MiB
    private static final int ROUNDS = 40;                  // Run 40 rounds ≈ 200 MiB
    private static final int SLEEP_MS = 100;               // Every round we sleep 100ms for observation

    static void printStats(String tag, long heldBytes) {
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        long total = rt.totalMemory();
        long max = rt.maxMemory();
        
        System.out.printf("%-20s | held_by_cache = %10d bytes (~%6.1f MiB) | used=%,d (%.1f MiB)%n",
            tag, heldBytes, heldBytes / 1024.0 / 1024.0, used, used / 1024.0 / 1024.0);
    }

    static void touchPages(byte[] block) {
        // Every 4 KiB we touch the page to force the actual allocation
        for (int i = 0; i < block.length; i += 4096) {
            block[i] = (byte)(block[i] + 1);
        }
    }

    static void growPhase(String label) throws InterruptedException {
        System.out.println("=== " + label + ": START ===");
        
        // Use ArrayList to store multiple memory blocks
        List<byte[]> cache = new ArrayList<>();
        
        for (int j = 1; j <= ROUNDS; j++) {
            for (int k = 0; k < BLOCKS_PER_ROUND; k++) {
                byte[] block = new byte[BLOCK_SIZE];
                touchPages(block); // Force the actual allocation
                cache.add(block);
            }
            
            long held = (long)cache.size() * BLOCK_SIZE;
            String roundLabel = label + " round " + j + " (+" + BLOCKS_PER_ROUND + " MiB)";
            printStats(roundLabel, held);
            Thread.sleep(SLEEP_MS);
        }
        
        // We are still in the scope: not released yet
        long held = (long)cache.size() * BLOCK_SIZE;
        printStats(label + " (before manual release)", held);
        
        // If we clear the cache, memory will be collected by GC
        cache.clear();
        
        // If we really want to release the memory right now, we can call System.gc()
        System.gc();
    }

    public static void main(String[] args) throws Exception {
        printStats("Start", 0);
        
        for (int i = 1; i <= TIMES; i++) {
            // Memory growth
            String label = "GROW #" + i;
            growPhase(label);

            // Out of scope: memory will be collected by GC
            printStats(label + " (after manual release)", 0);
            Thread.sleep(500); // Wait for the curve to fall back
            System.out.println("=== " + label + ": END ===");
        }
        
        // Done
        printStats("Done", 0);
    }
}
