package org.kie.maven.plugin;

import org.junit.Test;

public class RemoveBeforeFlightTest {

    @Test
    public void testFail() {
        org.junit.Assert.fail("restore -X version for base integration test");
        org.junit.Assert.fail("remove it-plugin.version from inner project");
    }
}
