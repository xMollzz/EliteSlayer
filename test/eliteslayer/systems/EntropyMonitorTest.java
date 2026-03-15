package eliteslayer.systems;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntropyMonitorTest {

    private EntropyMonitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new EntropyMonitor();
    }

    @Test
    void emptyWindowReturnsNormalizedOne() {
        // No actions recorded — normalised entropy should default to 1.0
        assertEquals(1.0, monitor.getNormalized(), 1e-9);
    }

    @Test
    void singleActionReturnsNormalizedOne() {
        monitor.record(42);
        // Only one unique action in a window of size 1 → normalised = 1.0
        assertEquals(1.0, monitor.getNormalized(), 1e-9);
    }

    @Test
    void identicalActionsGiveZeroEntropy() {
        for (int i = 0; i < 50; i++) {
            monitor.record(1);
        }
        // All actions identical → Shannon entropy = 0
        assertEquals(0.0, monitor.getEntropy(), 1e-9);
        assertEquals(0.0, monitor.getNormalized(), 1e-9);
    }

    @Test
    void uniformDistributionGivesHighEntropy() {
        // Record 10 distinct actions multiple times each
        for (int round = 0; round < 10; round++) {
            for (int actionId = 0; actionId < 10; actionId++) {
                monitor.record(actionId);
            }
        }
        // Uniform distribution → normalised entropy should be close to 1.0
        assertTrue(monitor.getNormalized() > 0.9,
            "Uniform distribution should have normalised entropy > 0.9, got: " + monitor.getNormalized());
    }

    @Test
    void windowSlidingEvictsOldEntries() {
        // Fill window with action 0
        for (int i = 0; i < 100; i++) {
            monitor.record(0);
        }
        assertEquals(0.0, monitor.getEntropy(), 1e-9);

        // Now overwrite entire window with action 1
        for (int i = 0; i < 100; i++) {
            monitor.record(1);
        }
        // Still zero entropy (all action 1 now)
        assertEquals(0.0, monitor.getEntropy(), 1e-9);
    }

    @Test
    void normalizedIsClamped() {
        for (int i = 0; i < 50; i++) {
            monitor.record(i);
        }
        double norm = monitor.getNormalized();
        assertTrue(norm >= 0.0 && norm <= 1.0,
            "Normalised entropy should be in [0, 1], got: " + norm);
    }
}
