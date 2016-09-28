package org.kie.maven.plugin;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.drools.core.phreak.ReactiveObject;

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
        
        File classDir = new File(basedir, "target/classes");
        
        System.out.println(classDir);
        System.out.println(classDir.exists());
        
        ClassLoader cl = new URLClassLoader( new URL[]{ classDir.toURI().toURL(), 
                new File(BytecodeInjectReactive.classpathFromClass(ReactiveObject.class)).toURI().toURL() }, null );
        
        assertTrue( looksLikeInstrumentedClass( cl.loadClass("org.drools.compiler.xpath.tobeinstrumented.model.Adult") ) );
        
    }

    private boolean looksLikeInstrumentedClass(Class<?> personClass) {
        boolean foundReactiveObjectInterface = false;
        for ( Class<?> i : personClass.getInterfaces() ){
            if ( i.getName().equals(ReactiveObject.class.getName()) ) {
                foundReactiveObjectInterface = true;
            }
        }
        // the ReactiveObject interface method are injected by the bytecode instrumenter, better check they are indeed available..
        boolean containsGetLeftTuple = checkContainsMethod(personClass, "getLeftTuples");
        boolean containsAddLeftTuple = checkContainsMethod(personClass, "addLeftTuple");
        boolean containsRemoveLeftTuple = checkContainsMethod(personClass, "removeLeftTuple");
        
        boolean foundReactiveInjectedMethods = false;
        for ( Method m : personClass.getMethods() ){
            if ( m.getName().startsWith(BytecodeInjectReactive.DROOLS_PREFIX) ) {
                foundReactiveInjectedMethods = true;
            }
        }
        return foundReactiveObjectInterface
                && containsGetLeftTuple && containsAddLeftTuple && containsRemoveLeftTuple
                && foundReactiveInjectedMethods ;
    }

    private boolean checkContainsMethod(Class<?> personClass, Object methodName) {
        for ( Method m : personClass.getMethods() ){
            if (m.getName().equals(methodName)) {
                return true;
            }
        }
        return false;
    }

}
