package org.kie.maven.plugin;

import java.io.File;

import org.junit.Test;

import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;

public class InjectReactiveIntegrationTest extends KieMavenPluginBaseIntegrationTest {

    public InjectReactiveIntegrationTest(MavenRuntime.MavenRuntimeBuilder builder) throws Exception {
        super(builder);
    }

    @Test
    public void testBasicBytecodeInjection() throws Exception {
        File basedir = resources.getBasedir("kjar-4-bytecode-inject");
        MavenExecutionResult result = mavenRuntime
                .forProject(basedir)
                .execute("clean", "install");
        result.assertErrorFreeLog();
    }

}
