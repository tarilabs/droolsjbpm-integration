package org.kie.maven.plugin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.drools.core.phreak.ReactiveObject;
import org.drools.core.phreak.ReactiveObjectUtil;
import org.junit.Test;
import org.mockito.Matchers;

import io.takari.maven.testing.executor.MavenExecutionResult;

public class TempTest {
    @Test
    public void testBasicBytecodeInjection() throws Exception {
        File basedir = new File("/home/mmortari/git/droolsjbpm-integration/kie-maven-plugin/target/test-projects/InjectReactiveIntegrationTest_testBasicBytecodeInjection[3.0.5]_kjar-4-bytecode-inject");
        
        File classDir = new File(basedir, "target/classes");
        
        System.out.println(classDir);
        System.out.println(classDir.exists());
        
        ClassLoader cl = new URLClassLoader( new URL[]{ classDir.toURI().toURL(), 
                new File(BytecodeInjectReactive.classpathFromClass(ReactiveObject.class)).toURI().toURL() }, null );
        
        Class<?> personClass = cl.loadClass("org.drools.compiler.xpath.tobeinstrumented.model.Person");
        
        for (Object c : personClass.getConstructors()) {
            System.out.println(c);
        }
        Constructor<?> personConstructor = personClass.getConstructor(String.class, int.class);
        Object personInstance = personConstructor.newInstance("Matteo", 34);
        Method setAgeMethod = personClass.getMethod("setAge", int.class);
        setAgeMethod.invoke(personInstance, 35);
        
        // FIXME well NO, mockito can't mock static method of ReactiveObjectUtil..
        ReactiveObjectUtil mock = mock(ReactiveObjectUtil.class);
        verify(mock).notifyModification(Matchers.any());
    }
}
