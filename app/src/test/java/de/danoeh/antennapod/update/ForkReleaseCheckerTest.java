package de.danoeh.antennapod.update;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ForkReleaseCheckerTest {
    @Test
    public void normalizeVersionStripsLeadingVAndSuffixes() {
        assertEquals("3.11.2", ForkReleaseChecker.normalizeVersion("v3.11.2-build.4"));
    }

    @Test
    public void isNewerVersionReturnsTrueForHigherPatch() {
        assertTrue(ForkReleaseChecker.isNewerVersion("3.11.2", "3.11.1"));
    }

    @Test
    public void isNewerVersionReturnsTrueForHigherMinor() {
        assertTrue(ForkReleaseChecker.isNewerVersion("3.12.0", "3.11.9"));
    }

    @Test
    public void isNewerVersionReturnsFalseForEqualVersion() {
        assertFalse(ForkReleaseChecker.isNewerVersion("3.11.2", "3.11.2"));
    }

    @Test
    public void isNewerVersionReturnsFalseForOlderVersion() {
        assertFalse(ForkReleaseChecker.isNewerVersion("3.11.0", "3.11.2"));
    }

    @Test
    public void isNewerVersionReturnsFalseForInvalidVersionStrings() {
        assertFalse(ForkReleaseChecker.isNewerVersion("latest", "3.11.2"));
    }
}