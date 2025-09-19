#include <iostream>
#include <vector>
#include <thread>
#include <chrono>
#include <iomanip>

const size_t TIMES = 10;
const size_t BLOCK_SIZE = 1 * 1024 * 1024; // 1 MiB
const size_t BLOCKS_PER_ROUND = 5;         // Every round we add +5 MiB
const size_t ROUNDS = 40;                  // Run 40 rounds ≈ 200 MiB
const size_t SLEEP_MS = 100;               // Every round we sleep 100ms for observation

void print_stats(const std::string& tag, size_t held_bytes) {
    std::cout << std::setw(20) << std::right << tag 
              << " | held_by_cache = " << std::setw(10) << held_bytes 
              << " bytes (~" << std::setw(6) << std::fixed << std::setprecision(1)
              << static_cast<double>(held_bytes) / 1024.0 / 1024.0 << " MiB)" << std::endl;
}

void touch_pages(char* block, size_t size) {
    // Every 4 KiB we touch the page to force the actual allocation
    for (size_t i = 0; i < size; i += 4096) {
        block[i] = block[i] + 1;
    }
}

void grow_phase(const std::string& label) {
    std::cout << "=== " << label << ": START ===" << std::endl;
    
    // Use vector to store multiple memory blocks
    std::vector<char*> cache;
    
    for (size_t j = 1; j <= ROUNDS; ++j) {
        for (size_t k = 0; k < BLOCKS_PER_ROUND; ++k) {
            char* block = new char[BLOCK_SIZE];
            touch_pages(block, BLOCK_SIZE); // Force the actual allocation
            cache.push_back(block);
        }
        
        size_t held = cache.size() * BLOCK_SIZE;
        std::string round_label = label + " round " + std::to_string(j) + 
                                " (+" + std::to_string(BLOCKS_PER_ROUND) + " MiB)";
        print_stats(round_label, held);
        std::this_thread::sleep_for(std::chrono::milliseconds(SLEEP_MS));
    }
    
    // We are still holding the memory
    size_t held = cache.size() * BLOCK_SIZE;
    print_stats(label + " (before manual release)", held);
    
    // Manually release all memory
    for (char* block : cache) {
        delete[] block;
    }
    cache.clear();
    
    // We have manually released all memory
    print_stats(label + " (after manual release)", 0);
    std::this_thread::sleep_for(std::chrono::milliseconds(500)); // Sleep for 500ms to observe the curve
    std::cout << "=== " << label << ": END ===" << std::endl;
}

int main() {
    print_stats("Start", 0);
    
    for (size_t i = 1; i <= TIMES; ++i) {
        // Memory growth
        grow_phase("GROW #" + std::to_string(i));
    }
    
    // Done
    print_stats("Done", 0);
    return 0;
}
