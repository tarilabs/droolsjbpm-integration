package org.drools.compiler.xpath.tobeinstrumented;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.drools.compiler.xpath.tobeinstrumented.model.*;
import org.drools.core.base.ClassObjectType;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.impl.InternalKnowledgeBase;
import org.drools.core.phreak.ReactiveObject;
import org.drools.core.reteoo.BetaMemory;
import org.drools.core.reteoo.EntryPointNode;
import org.drools.core.reteoo.LeftInputAdapterNode;
import org.drools.core.reteoo.LeftTuple;
import org.drools.core.reteoo.ObjectTypeNode;
import org.drools.core.reteoo.ReactiveFromNode;
import org.drools.core.reteoo.ReteDumper;
import org.drools.core.reteoo.TupleMemory;
import org.drools.core.util.Iterator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;
import org.kie.maven.plugin.BytecodeInjectReactive;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

public class XPathTest {
    @BeforeClass
    public static void init() throws Exception {
        String aname = ReactiveObject.class.getPackage().getName().replaceAll("\\.", "/") + "/" +  ReactiveObject.class.getSimpleName()+".class";
        System.out.println(aname);
        String apath = ClassLoader.getSystemClassLoader().getResource( aname).getPath();
        System.out.println( apath );
        String path = null;
        if (apath.contains("!")) {
            path = apath.substring(0, apath.indexOf("!"));
        } else {
            path = "file:"+apath.substring(0, apath.indexOf(aname));
        }
        System.out.println( path );
        
        File f = new File(new URI(path));
        
        ClassPool cp = new ClassPool(null);
        cp.appendSystemPath();
        cp.appendClassPath(f.getAbsolutePath());
        BytecodeInjectReactive enhancer = BytecodeInjectReactive.newInstance(cp);
        
        byte[] personBytecode = enhancer.test2("org.drools.compiler.xpath.tobeinstrumented.model.Person");
        byte[] schoolBytecode = enhancer.test2("org.drools.compiler.xpath.tobeinstrumented.model.School");
        
        ClassPool cp2 = new ClassPool(null);
        cp2.appendSystemPath();
        cp2.appendClassPath(f.getAbsolutePath());
        
        CtClass personCtClass = cp2.makeClass(new ByteArrayInputStream(personBytecode));
        Class<?> class1 = personCtClass.toClass();
        Arrays.stream(class1.getMethods()).forEach(System.out::println);
        
        CtClass schooltClass = cp2.makeClass(new ByteArrayInputStream(schoolBytecode));
        schooltClass.toClass();
        File schoolClassFile = new File("./target/JAVASSIST/School.class");
        schoolClassFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(schoolClassFile);
        fos.write(schoolBytecode);
        fos.close();
    }
    
    /**
     * Copied from drools-compiler.
     */
    @Test
    public void testReactiveOnLia() {
        String drl =
                "import org.drools.compiler.xpath.tobeinstrumented.model.*;\n" +
                "global java.util.List list\n" +
                "\n" +
                "rule R when\n" +
                "  Man( $toy: /wife/children{age > 10}/toys )\n" +
                "then\n" +
                "  list.add( $toy.getName() );\n" +
                "end\n";

        KieSession ksession = new KieHelper().addContent( drl, ResourceType.DRL )
                                             .build()
                                             .newKieSession();

        List<String> list = new ArrayList<String>();
        ksession.setGlobal( "list", list );

        Woman alice = new Woman( "Alice", 38 );
        Man bob = new Man( "Bob", 40 );
        bob.setWife( alice );

        Child charlie = new Child( "Charles", 12 );
        Child debbie = new Child( "Debbie", 10 );
        alice.addChild( charlie );
        alice.addChild( debbie );

        charlie.addToy( new Toy( "car" ) );
        charlie.addToy( new Toy( "ball" ) );
        debbie.addToy( new Toy( "doll" ) );

        ksession.insert( bob );
        ksession.fireAllRules();

        assertEquals( 2, list.size() );
        assertTrue( list.contains( "car" ) );
        assertTrue( list.contains( "ball" ) );

        list.clear();
        debbie.setAge( 11 );
        ksession.fireAllRules();

        assertEquals( 1, list.size() );
        assertTrue( list.contains( "doll" ) );
    }

    /**
     * Copied from drools-compiler.
     */
    @Test
    public void testReactiveDeleteOnLia() {
        String drl =
                "import org.drools.compiler.xpath.tobeinstrumented.model.*;\n" +
                "global java.util.List list\n" +
                "\n" +
                "rule R when\n" +
                "  Man( $toy: /wife/children{age > 10}/toys )\n" +
                "then\n" +
                "  list.add( $toy.getName() );\n" +
                "end\n";

        KieBase kbase = new KieHelper().addContent( drl, ResourceType.DRL ).build();
        KieSession ksession = kbase.newKieSession();
        
        EntryPointNode epn = ( (InternalKnowledgeBase) ksession.getKieBase() ).getRete().getEntryPointNodes().values().iterator().next();
        ObjectTypeNode otn = epn.getObjectTypeNodes().values().stream()
                .filter(ot-> ot.getObjectType() instanceof ClassObjectType && !((ClassObjectType)ot.getObjectType()).getClassName().contains("InitialFact"))
                .findFirst().get();
        LeftInputAdapterNode lian = (LeftInputAdapterNode)otn.getObjectSinkPropagator().getSinks()[0];
        ReactiveFromNode from1 = (ReactiveFromNode)lian.getSinkPropagator().getSinks()[0];
        ReactiveFromNode from2 = (ReactiveFromNode)from1.getSinkPropagator().getSinks()[0];
        ReactiveFromNode from3 = (ReactiveFromNode)from2.getSinkPropagator().getSinks()[0];

        BetaMemory betaMemory = ( (InternalWorkingMemory) ksession ).getNodeMemory(from3).getBetaMemory();

        List<String> list = new ArrayList<String>();
        ksession.setGlobal( "list", list );

        Woman alice = new Woman( "Alice", 38 );
        Man bob = new Man( "Bob", 40 );
        bob.setWife( alice );

        Child charlie = new Child( "Charles", 12 );
        Child debbie = new Child( "Debbie", 11 );
        alice.addChild( charlie );
        alice.addChild( debbie );

        charlie.addToy( new Toy( "car" ) );
        charlie.addToy( new Toy( "ball" ) );
        debbie.addToy( new Toy( "doll" ) );

        ksession.insert( bob );
        ksession.fireAllRules();

        assertEquals( 3, list.size() );
        assertTrue( list.contains( "car" ) );
        assertTrue( list.contains( "ball" ) );
        assertTrue( list.contains( "doll" ) );

        TupleMemory tupleMemory = betaMemory.getLeftTupleMemory();
        assertEquals( 2, betaMemory.getLeftTupleMemory().size() );
        Iterator<LeftTuple> it = tupleMemory.iterator();
        for ( LeftTuple next = it.next(); next != null; next = it.next() ) {
            Object obj = next.getFactHandle().getObject();
            assertTrue( obj == charlie || obj == debbie );
        }

        list.clear();
        debbie.setAge( 10 );
        ksession.fireAllRules();

        assertEquals( 0, list.size() );

        assertEquals( 1, betaMemory.getLeftTupleMemory().size() );
        it = tupleMemory.iterator();
        for ( LeftTuple next = it.next(); next != null; next = it.next() ) {
            Object obj = next.getFactHandle().getObject();
            assertTrue( obj == charlie );
        }
    }
    
    /**
     * Copied from drools-compiler.
     */
    @Test
    public void testReactiveOnBeta() {
        String drl =
                "import org.drools.compiler.xpath.tobeinstrumented.model.*;\n" +
                "global java.util.List list\n" +
                "\n" +
                "rule R when\n" +
                "  $i : Integer()\n" +
                "  Man( $toy: /wife/children{age > $i}?/toys )\n" +
                "then\n" +
                "  list.add( $toy.getName() );\n" +
                "end\n";

        KieSession ksession = new KieHelper().addContent( drl, ResourceType.DRL )
                                             .build()
                                             .newKieSession();

        List<String> list = new ArrayList<String>();
        ksession.setGlobal( "list", list );

        Woman alice = new Woman( "Alice", 38 );
        Man bob = new Man( "Bob", 40 );
        bob.setWife( alice );

        Child charlie = new Child( "Charles", 12 );
        Child debbie = new Child( "Debbie", 10 );
        alice.addChild( charlie );
        alice.addChild( debbie );

        charlie.addToy( new Toy( "car" ) );
        charlie.addToy( new Toy( "ball" ) );
        debbie.addToy( new Toy( "doll" ) );

        ksession.insert( 10 );
        ksession.insert( bob );
        ksession.fireAllRules();

        assertEquals( 2, list.size() );
        assertTrue( list.contains( "car" ) );
        assertTrue( list.contains( "ball" ) );

        list.clear();
        debbie.setAge( 11 );
        ksession.fireAllRules();

        assertEquals( 1, list.size() );
        assertTrue( list.contains( "doll" ) );
    }
    
    /**
     * Copied from drools-compiler.
     */
    @Test
    public void testReactive2Rules() {
        String drl =
                "import org.drools.compiler.xpath.tobeinstrumented.model.*;\n" +
                "global java.util.List toyList\n" +
                "global java.util.List teenagers\n" +
                "\n" +
                "rule R1 when\n" +
                "  $i : Integer()\n" +
                "  Man( $toy: /wife/children{age >= $i}/toys )\n" +
                "then\n" +
                "  toyList.add( $toy.getName() );\n" +
                "end\n" +
                "rule R2 when\n" +
                "  School( $child: /children{age >= 13} )\n" +
                "then\n" +
                "  teenagers.add( $child.getName() );\n" +
                "end\n";

        KieSession ksession = new KieHelper().addContent( drl, ResourceType.DRL )
                                             .build()
                                             .newKieSession();

        List<String> toyList = new ArrayList<String>();
        ksession.setGlobal( "toyList", toyList );
        List<String> teenagers = new ArrayList<String>();
        ksession.setGlobal( "teenagers", teenagers );

        Woman alice = new Woman( "Alice", 38 );
        Man bob = new Man( "Bob", 40 );
        bob.setWife( alice );

        Child charlie = new Child( "Charles", 15 );
        Child debbie = new Child( "Debbie", 12 );
        alice.addChild( charlie );
        alice.addChild( debbie );

        charlie.addToy( new Toy( "car" ) );
        charlie.addToy( new Toy( "ball" ) );
        debbie.addToy( new Toy( "doll" ) );

        School school = new School( "Da Vinci" );
        school.addChild( charlie );
        school.addChild( debbie );

        ksession.insert( 13 );
        ksession.insert( bob );
        ksession.insert( school );
        ksession.fireAllRules();

        assertEquals( 2, toyList.size() );
        assertTrue( toyList.contains( "car" ) );
        assertTrue( toyList.contains( "ball" ) );

        assertEquals( 1, teenagers.size() );
        assertTrue( teenagers.contains( "Charles" ) );

        toyList.clear();
        debbie.setAge( 13 );
        ksession.fireAllRules();

        assertEquals( 1, toyList.size() );
        assertTrue( toyList.contains( "doll" ) );

        assertEquals( 2, teenagers.size() );
        assertTrue( teenagers.contains( "Charles" ) );
        assertTrue( teenagers.contains( "Debbie" ) );
    }
    
    @Test
    public void testListReactive() {
        String drl =
                "import org.drools.compiler.xpath.tobeinstrumented.model.*;\n" +
                "\n" +
                "rule R2 when\n" +
                "  School( $child: /children{age >= 13 && age < 20} )\n" +
                "then\n" +
                "  System.out.println( $child );\n" +
                "  insertLogical( $child );\n" +
                "end\n";

        KieSession ksession = new KieHelper().addContent( drl, ResourceType.DRL )
                                             .build()
                                             .newKieSession();
        
        ReteDumper.dumpRete(ksession);
        
        Child charlie = new Child( "Charles", 15 );
        Child debbie = new Child( "Debbie", 19 );
        School school = new School( "Da Vinci" );
        school.addChild( charlie );
        ksession.insert( school );
        ksession.fireAllRules();
        assertTrue(ksession.getObjects().contains(charlie));
        assertFalse(ksession.getObjects().contains(debbie));
        
        school.addChild( debbie );
        ksession.fireAllRules();
        assertTrue(ksession.getObjects().contains(charlie));
        assertTrue(ksession.getObjects().contains(debbie));
        
        debbie.setAge( 20 );
        school.removeChild(debbie);
        ksession.fireAllRules();
        assertTrue(ksession.getObjects().contains(charlie));
        assertFalse(ksession.getObjects().contains(debbie));
        
        school.addChild( debbie );
        ksession.fireAllRules();
        assertTrue(ksession.getObjects().contains(charlie));
        assertFalse(ksession.getObjects().contains(debbie));
        
        debbie.setAge( 20 );
        ksession.fireAllRules();
        assertTrue(ksession.getObjects().contains(charlie));
        assertFalse(ksession.getObjects().contains(debbie));
    }
}
