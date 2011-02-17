package eu.hatsproject.absplugin.builder;

import static eu.hatsproject.absplugin.util.Constants.JAVA_SOURCE_PATH;
import static eu.hatsproject.absplugin.util.Constants.MARKER_TYPE;
import static eu.hatsproject.absplugin.util.Constants.MAUDE_PATH;
import static eu.hatsproject.absplugin.util.UtilityFunctions.deleteRecursive;

import java.io.File;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import static eu.hatsproject.absplugin.util.CoreControlUnit.notifyBuildListener;
import static eu.hatsproject.absplugin.util.UtilityFunctions.getAbsNature;
import static eu.hatsproject.absplugin.util.UtilityFunctions.isABSFile;

/**
 * Builds the abs files in abs projects. Automated building has to be activated for all abs files to be parsed
 * and typechecked automatically.
 * @author mweber
 *
 */
public class AbsBuilder extends IncrementalProjectBuilder {
	class SampleDeltaVisitor implements IResourceDeltaVisitor {
		private HashSet<String> changedFiles;

		public SampleDeltaVisitor(HashSet<String> changedFiles){
			this.changedFiles = changedFiles;
		}
		
		/**
		 * Visits and parses all files in the ABS project that have changed since last build
		 * 
		 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
		 */
		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			AbsNature nature = getAbsNature(resource.getProject());
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				// handle added resource
				nature.parseABSFile(resource);
				changedFiles.add(resource.getFullPath().toString());
				break;
			case IResourceDelta.REMOVED:
				// handle removed resource
				break;
			case IResourceDelta.CHANGED:
				// handle changed resource
				nature.parseABSFile(resource);
				changedFiles.add(resource.getFullPath().toString());
				break;
			}
			return true;
		}
	}

	/**
	 * Works through the project and parses all abs files. gets called when a full build is needed.
	 * 
	 * @author mweber
	 *
	 */
	class SampleResourceVisitor implements IResourceVisitor {
		private HashSet<String> changedFiles;
		public SampleResourceVisitor(HashSet<String> changedFiles){
			this.changedFiles = changedFiles;
		}
		
		@Override
		public boolean visit(IResource resource) throws CoreException{
			AbsNature nature = getAbsNature(resource.getProject());
			if(isABSFile(resource)){
				nature.parseABSFile(resource);
				changedFiles.add(resource.getFullPath().toString());
			}
			return true;
		}
	}

	/**
	 * gets called when a build (full or partial) is requested
	 * 
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#build(int,
	 *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		AbsNature nature = getAbsNature(getProject());
		synchronized (nature.modelLock) {
			HashSet<String> changedFiles = new HashSet<String>();
			nature.cleanModel();
			fullBuild(monitor, changedFiles);
			
			nature.typeCheckModel();
			notifyBuildListener(changedFiles);
			monitor.done();
			return null;
		}
	}
	
	/**
	 * cleans the project by deleting all marks generated by a previous build, cleaning the model
	 * by removing the current AST and deleting all generated Java and Maude files
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#clean(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {
		AbsNature nature = getAbsNature(getProject());
		synchronized (nature.modelLock) {
			getProject().deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
			getAbsNature(getProject()).cleanModel();
			
			// delete all Java files and classes
			String javaPathString = nature.getProjectPreferenceStore().getString(JAVA_SOURCE_PATH);
			cleanGeneratedFiles(javaPathString);
			
			// delete all Maude files and classes
			String maudePathString = nature.getProjectPreferenceStore().getString(MAUDE_PATH);
			cleanGeneratedFiles(maudePathString);
			
			getProject().refreshLocal(IProject.DEPTH_INFINITE, null);
		}
	}

	private void cleanGeneratedFiles(String pathstring) {
		if(pathstring!=null){
			IPath path = new Path(pathstring);
			File dir;
			if (path.isAbsolute()){
				dir = new File(path.toOSString());
			} else {
				path=getProject().getLocation().append(path);
				dir=path.toFile();
			}
			if(dir.exists()){
				deleteRecursive(dir);
			}
		}
	}
	
	protected void fullBuild(final IProgressMonitor monitor, HashSet<String> changedFiles)
			throws CoreException {
		// the visitor does the work.
		getProject().accept(new SampleResourceVisitor(changedFiles));
	}

	protected void incrementalBuild(IResourceDelta delta,
			IProgressMonitor monitor, HashSet<String> changedFiles) throws CoreException {
		// the visitor does the work.
		delta.accept(new SampleDeltaVisitor(changedFiles));
	}
}