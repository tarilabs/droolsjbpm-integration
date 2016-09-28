package org.kie.maven.plugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

import org.drools.core.phreak.ReactiveObject;
import org.drools.core.phreak.ReactiveObjectUtil;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.MockingDetails;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;

import static org.mockito.Mockito.*;

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
        
       
        Class<?> personClass = cl.loadClass("org.drools.compiler.xpath.tobeinstrumented.model.Adult");
        Constructor<?> personConstructor = personClass.getConstructor(String.class, int.class);
        Object personInstance = personConstructor.newInstance("Matteo", 34);
        System.out.println(personInstance.getClass());
        Object mock = mock(personInstance.getClass());
        Method getAgeMethod = personClass.getMethod("getAge");
        System.out.println( getAgeMethod.invoke(mock) );
        Method setAgeMethod = personClass.getMethod("setAge", int.class);
        setAgeMethod.invoke(mock, 35);
        System.out.println( getAgeMethod.invoke(mock) );
        
        MockingDetails a = mockingDetails(mock);
        Collection<Invocation> c = a.getInvocations();
        c.forEach(System.out::println);
    }

}
