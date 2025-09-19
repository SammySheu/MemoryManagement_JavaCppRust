// src/main.rs
use std::{thread, time::Duration};

const TIMES: usize = 10;
const BLOCK_SIZE: usize = 1 * 1024 * 1024; // 1 MiB
const BLOCKS_PER_ROUND: usize = 5;         // Every round we add 5 MiB
const ROUNDS: usize = 40;                  // Run 40 rounds ≈ 200 MiB
const SLEEP_MS: u64 = 100;                 // Every round we sleep for 100ms for observation

fn print_stats(tag: &str, held_bytes: usize) {
    println!(
        "{tag:>20} | held_by_cache = {:>10} bytes (~{:>6.1} MiB)",
        held_bytes,
        held_bytes as f64 / 1024.0 / 1024.0
    );
}

#[inline]
fn touch_pages(block: &mut [u8]) {
    // Every 4 KiB we touch the page to force the actual allocation
    for i in (0..block.len()).step_by(4096) {
        block[i] = block[i].wrapping_add(1);
    }
}

fn grow_phase(label: &str) {
    println!("=== {label}: START ===");
    {
        // This is served as Scope. 
        // When it ends, the cache and all variables in it will be released
        let mut cache: Vec<Vec<u8>> = Vec::new();

        for j in 1..=ROUNDS {
            for _ in 0..BLOCKS_PER_ROUND {
                let mut block = vec![0u8; BLOCK_SIZE];
                touch_pages(&mut block); // Force the actual allocation
                cache.push(block);
            }
            let held = cache.iter().map(|v| v.len()).sum::<usize>();
            print_stats(&format!("{label} round {j:02} (+{} MiB)", BLOCKS_PER_ROUND), held);
            thread::sleep(Duration::from_millis(SLEEP_MS));
        }

        // Still in the scope: not released yet
        let held = cache.iter().map(|v| v.len()).sum::<usize>();
        print_stats(&format!("{label} (before scope end)"), held);
    }
    // Scope ends: automatically released
    print_stats(&format!("{label} (after scope end)"), 0);
    thread::sleep(Duration::from_millis(500)); // Sleep for 500ms for observation
    println!("=== {label}: END ===");
}

fn main() {
    print_stats("Start", 0);
    for i in 1..=TIMES {
        // Memory growth
        grow_phase(&format!("GROW #{}", i));
    }


    // Done
    print_stats("Done", 0);
}
