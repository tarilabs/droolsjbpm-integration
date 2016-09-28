package org.drools.compiler.xpath.tobeinstrumented;

import static org.kie.maven.plugin.InjectReactiveMojo.*;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.drools.core.phreak.ReactiveObject;
import org.junit.Test;

public class MojoConfigTest {

    @Test
    public void testRegexpForPackagesDefault() {
        String[] inputConfig = new String[]{"*"};
        
        String[] config = convertAllPatternToRegExp(inputConfig);
        
        System.out.println(Arrays.asList(config));
        
        assertTrue(isPackageNameIncluded(Object.class.getPackage().getName(), config));
        assertTrue(isPackageNameIncluded(ReactiveObject.class.getPackage().getName(), config));
        assertTrue(isPackageNameIncluded("xyz.my.MyClass", config));
    }
    
    @Test
    public void testRegexpForPackagesSingle() {
        String[] inputConfig = new String[]{"org.drools"};
        
        String[] config = convertAllPatternToRegExp(inputConfig);
        
        System.out.println(Arrays.asList(config));
        
        assertTrue(isPackageNameIncluded(Object.class.getPackage().getName(), config));
        assertTrue(isPackageNameIncluded(ReactiveObject.class.getPackage().getName(), config));
        assertTrue(isPackageNameIncluded("xyz.my.MyClass", config));
    }
}
