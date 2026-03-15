package eliteslayer.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class FileStateStoreTest {

    @TempDir
    File tempDir;

    private FileStateStore store;
    private File stateFile;

    @BeforeEach
    void setUp() {
        stateFile = new File(tempDir, "test_state.cfg");
        store = new FileStateStore(stateFile);
    }

    @Test
    void setAndGetString() {
        store.set("name", "EliteSlayer");
        assertEquals("EliteSlayer", store.get("name", "default"));
    }

    @Test
    void getDefaultWhenKeyMissing() {
        assertEquals("default", store.get("missing", "default"));
    }

    @Test
    void setAndGetInt() {
        store.set("kills", "42");
        assertEquals(42, store.getInt("kills", 0));
    }

    @Test
    void getIntDefaultOnInvalid() {
        store.set("bad", "not_a_number");
        assertEquals(99, store.getInt("bad", 99));
    }

    @Test
    void setAndGetLong() {
        store.set("gp", "1234567890");
        assertEquals(1234567890L, store.getLong("gp", 0L));
    }

    @Test
    void setAndGetBoolean() {
        store.set("enabled", "true");
        assertTrue(store.getBoolean("enabled", false));
        store.set("disabled", "false");
        assertFalse(store.getBoolean("disabled", true));
    }

    @Test
    void saveAndReload() {
        store.set("key1", "value1");
        store.set("key2", "value2");
        store.save();

        // Create new store from same file — should reload
        FileStateStore reloaded = new FileStateStore(stateFile);
        assertEquals("value1", reloaded.get("key1", ""));
        assertEquals("value2", reloaded.get("key2", ""));
    }

    @Test
    void handlesEqualsInValue() {
        store.set("formula", "a=b=c");
        store.save();

        FileStateStore reloaded = new FileStateStore(stateFile);
        assertEquals("a=b=c", reloaded.get("formula", ""));
    }

    @Test
    void handlesNewlineInValue() {
        store.set("multi", "line1\nline2");
        store.save();

        FileStateStore reloaded = new FileStateStore(stateFile);
        assertEquals("line1\nline2", reloaded.get("multi", ""));
    }

    @Test
    void handlesBackslashInValue() {
        store.set("path", "C:\\Users\\test");
        store.save();

        FileStateStore reloaded = new FileStateStore(stateFile);
        assertEquals("C:\\Users\\test", reloaded.get("path", ""));
    }

    @Test
    void nonExistentFileCreatesEmptyStore() {
        File nonExistent = new File(tempDir, "does_not_exist.cfg");
        FileStateStore emptyStore = new FileStateStore(nonExistent);
        assertEquals("default", emptyStore.get("any", "default"));
    }
}
