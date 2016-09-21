package org.kie.maven.plugin;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import javassist.CtClass;

@Mojo(name = "injectreactive",
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresProject = true,
        defaultPhase = LifecyclePhase.COMPILE)
@Execute(goal = "injectreactive",
        phase = LifecyclePhase.COMPILE)
public class InjectReactiveMojo extends AbstractKieMojo {

    private List<File> sourceSet = new ArrayList<File>();
    
    @Parameter(required = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;
    
    @Parameter(property = "failOnError", defaultValue = "true")
    private boolean failOnError = true;
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Perform a depth first search for sourceSet
        File root = outputDirectory;
        if ( !root.exists() ) {
            getLog().info( "Skipping Hibernate enhancement plugin execution since there is no classes dir " + outputDirectory );
            return;
        }
        walkDir( root );
        if ( sourceSet.isEmpty() ) {
            getLog().info( "Skipping Hibernate enhancement plugin execution since there are no classes to enhance on " + outputDirectory );
            return;
        }

        getLog().info( "Starting Hibernate enhancement for classes on " + outputDirectory );
        final ClassLoader classLoader = toClassLoader( Collections.singletonList( root ) );
    }
    
    private ClassLoader toClassLoader(List<File> runtimeClasspath) throws MojoExecutionException {
        List<URL> urls = new ArrayList<URL>();
        for ( File file : runtimeClasspath ) {
            try {
                urls.add( file.toURI().toURL() );
                getLog().debug( "Adding classpath entry for classes root " + file.getAbsolutePath() );
            }
            catch (MalformedURLException e) {
                String msg = "Unable to resolve classpath entry to URL: " + file.getAbsolutePath();
                if ( failOnError ) {
                    throw new MojoExecutionException( msg, e );
                }
                getLog().warn( msg );
            }
        }

        // HHH-10145 Add dependencies to classpath as well - all but the ones used for testing purposes
        Set<Artifact> artifacts = null;
        MavenProject project = ( (MavenProject) getPluginContext().get( "project" ) );
        if ( project != null ) {
            // Prefer execution project when available (it includes transient dependencies)
            MavenProject executionProject = project.getExecutionProject();
            artifacts = ( executionProject != null ? executionProject.getArtifacts() : project.getArtifacts() );
        }
        if ( artifacts != null) {
            for ( Artifact a : artifacts ) {
                if ( !Artifact.SCOPE_TEST.equals( a.getScope() ) ) {
                    try {
                        urls.add( a.getFile().toURI().toURL() );
                        getLog().debug( "Adding classpath entry for dependency " + a.getId() );
                    }
                    catch (MalformedURLException e) {
                        String msg = "Unable to resolve URL for dependency " + a.getId() + " at " + a.getFile().getAbsolutePath();
                        if ( failOnError ) {
                            throw new MojoExecutionException( msg, e );
                        }
                        getLog().warn( msg );
                    }
                }
            }
        }

        return new URLClassLoader( urls.toArray( new URL[urls.size()] ), null );
    }
    
    /**
     * Expects a directory.
     */
    private void walkDir(File dir) {
        walkDir(
                dir,
                new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return ( pathname.isFile() && pathname.getName().endsWith( ".class" ) );
                    }
                },
                new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return ( pathname.isDirectory() );
                    }
                }
        );
    }

    private void walkDir(File dir, FileFilter classesFilter, FileFilter dirFilter) {
        File[] dirs = dir.listFiles( dirFilter );
        for ( File dir1 : dirs ) {
            walkDir( dir1, classesFilter, dirFilter );
        }
        File[] files = dir.listFiles( classesFilter );
        Collections.addAll( this.sourceSet, files );
    }

    private void writeOutEnhancedClass(byte[] enhancedBytecode, CtClass ctClass, File file) throws MojoExecutionException{
        if ( enhancedBytecode == null ) {
            return;
        }
        try {
            if ( file.delete() ) {
                if ( !file.createNewFile() ) {
                    getLog().error( "Unable to recreate class file [" + ctClass.getName() + "]" );
                }
            }
            else {
                getLog().error( "Unable to delete class file [" + ctClass.getName() + "]" );
            }
        }
        catch (IOException e) {
            getLog().warn( "Problem preparing class file for writing out enhancements [" + ctClass.getName() + "]" );
        }

        try {
            FileOutputStream outputStream = new FileOutputStream( file, false );
            try {
                outputStream.write( enhancedBytecode );
                outputStream.flush();
            }
            catch (IOException e) {
                String msg = String.format( "Error writing to enhanced class [%s] to file [%s]", ctClass.getName(), file.getAbsolutePath() );
                if ( failOnError ) {
                    throw new MojoExecutionException( msg, e );
                }
                getLog().warn( msg );
            }
            finally {
                try {
                    outputStream.close();
                    ctClass.detach();
                }
                catch (IOException ignore) {
                }
            }
        }
        catch (FileNotFoundException e) {
            String msg = "Error opening class file for writing: " + file.getAbsolutePath();
            if ( failOnError ) {
                throw new MojoExecutionException( msg, e );
            }
            getLog().warn( msg );
        }
    }

}
