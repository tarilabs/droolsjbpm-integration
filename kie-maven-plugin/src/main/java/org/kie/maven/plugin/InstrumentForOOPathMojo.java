package org.kie.maven.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

/**
 * Instrument Pojo and Object Model for OOPath reactivity.
 */
@Mojo(name = "instrumentForOOPath",
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresProject = true,
        defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class InstrumentForOOPathMojo extends AbstractKieMojo {

	@Parameter(required = true, defaultValue = "${project}")
    private MavenProject project;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("CIAO");
		project.getCompileSourceRoots().stream().forEach(sr -> {
			try {
				scan(sr);
			} catch (MojoExecutionException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
		
		
		getLog().info("CIAO");
		try {
			for ( String ce : project.getCompileClasspathElements() ) {
				System.out.println(ce);
			}
		} catch (DependencyResolutionRequiredException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void scan(String root) throws MojoExecutionException {
		final DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setIncludes(new String[] {"**/*.java"});
        directoryScanner.setExcludes(new String[] {});
        directoryScanner.setBasedir(root);
        directoryScanner.scan();
 
        for (String fileName : directoryScanner.getIncludedFiles()) {
            final File file = new File(root, fileName);
            try {
                System.out.println(file);
                javaparse(file);
            } catch (Exception e) {
                throw new MojoExecutionException("io error while writing source list", e);
            }
        }       
	}
	
	private void javaparse(File file) throws Exception {
        FileInputStream in = new FileInputStream(file);

        CompilationUnit cu;
        try {
            cu = JavaParser.parse(in);
        } finally {
            in.close();
        }

        cu.getImports().add(new ImportDeclaration(new NameExpr("org.drools.core.phreak.AbstractReactiveObject"), false, false));
        
        String mainClassName = null;
        for (TypeDeclaration t : cu.getTypes()) {
        	if (t instanceof ClassOrInterfaceDeclaration) {
        		ClassOrInterfaceDeclaration coid = (ClassOrInterfaceDeclaration) t;
        		mainClassName = coid.getName();
        		ClassOrInterfaceType phreakAbsReactObj = new ClassOrInterfaceType("AbstractReactiveObject");
        		List<ClassOrInterfaceType> extendList = new ArrayList<>();
        		extendList.add(phreakAbsReactObj);
        		coid.setExtends(extendList);
        	}
        	
        	for (BodyDeclaration m : t.getMembers()) {
        		if (m instanceof MethodDeclaration) {
        			MethodDeclaration md = (MethodDeclaration) m;
        			if (isASetter(md)) {
        				BlockStmt block = md.getBody();
//        				  NameExpr clazz = new NameExpr("System");
//        				  FieldAccessExpr field = new FieldAccessExpr(clazz, "out");
        				  MethodCallExpr call = new MethodCallExpr(null, "notifyModification");
        				  // ASTHelper.addArgument(call, new StringLiteralExpr("Hello World!"));
        				  ASTHelper.addStmt(block, call);
        				for ( Node n : block.getChildrenNodes() ) {
        					System.err.println(n.getClass()+ " " + n.toString());
        				}
        			}
        		}
        	}
        }
        
        String target = project.getBasedir() + "/target/generated-sources/" + "kie" + "/" + cu.getPackage().getPackageName().replace(".", "/") ;
        File targetDir = new File(target);
        targetDir.mkdirs();
		try(  PrintWriter out = new PrintWriter( new File(targetDir, mainClassName + ".java")  )  ){
            out.println( cu );
        }
    }
	
	public static final boolean isASetter(MethodDeclaration md) {
		return md.getName().startsWith("set");
	}

}
