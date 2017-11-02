package org.jboss.ide.eclipse.as.wtp.core.server.publish;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleFolder;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.jboss.ide.eclipse.as.core.util.ModuleResourceUtil;
import org.jboss.ide.eclipse.as.wtp.core.ASWTPToolsPlugin;
import org.jboss.ide.eclipse.as.wtp.core.server.behavior.IFilesystemController;

public class BinaryModulePublishRunner {

	private IModule[] module;
	private IPath archiveDestination;
	private IPath archiveDestinationWithName;
	private IFilesystemController fc;
	private IModuleResource[] all;
	private IModuleResourceDelta[] delta;
	public BinaryModulePublishRunner(IModule[] module, 
			IPath archiveDestination, 
			IPath archiveDestinationWithName, 
			IFilesystemController fc,
			IModuleResource[] all, IModuleResourceDelta[] delta) {
		super();
		this.module = module;
		this.archiveDestination = archiveDestination;
		this.archiveDestinationWithName = archiveDestinationWithName;
		this.fc = fc;
		this.all = all;
		this.delta = delta;
	}
	/*
	 * Use the delta to find all deleted resources. 
	 * Then use the members to delete all those, too. 
	 * @return
	 */
	public MultiStatus removeBinaryModule() throws CoreException {
		MultiStatus ms = removeDeletedBinaryModule();
		if( !isNestedSingleResourceBinaryModule()) {
			removeMembers(all, ms);
		}
		return ms;
	}
	
	private IStatus removeMembers(IModuleResource[] all, MultiStatus ms) throws CoreException {
		for( int i = 0; i < all.length; i++ ) {
			if( all[i] instanceof IModuleFolder) {
				IModuleResource[] children = ((IModuleFolder)all[i]).members();
				removeMembers(children, ms);
			}
			IPath relative = all[i].getModuleRelativePath();
			IPath published = archiveDestination.append(relative).append(all[i].getName());
			IStatus s = fc.deleteResource(published, new NullProgressMonitor());
			if( s != null && !s.isOK()) {
				ms.add(s);
			}
		}
		return ms;
	}
	
	
	private MultiStatus removeNestedSingleResourceBinary(MultiStatus ms) throws CoreException {
		IPath published = archiveDestinationWithName;
		IStatus s = fc.deleteResource(published, new NullProgressMonitor());
		if( s != null && !s.isOK()) {
			ms.add(s);
		}
		return ms;
	}
	/*
	 * Use the delta because the module is gone and has no members
	 */
	public MultiStatus removeDeletedBinaryModule() throws CoreException {
		MultiStatus ms = new MultiStatus(ASWTPToolsPlugin.PLUGIN_ID, 0, "Errors while deleting binary module " + module[module.length-1].getName(), null);
		IModuleResource[] deleted = getAllResources(delta, IModuleResourceDelta.REMOVED, true);
		if( isNestedSingleResourceBinaryModule()) {
			removeNestedSingleResourceBinary(ms);
			return ms;
		}
		
		for( int i = 0; i < deleted.length; i++ ) {
			IPath relative = deleted[i].getModuleRelativePath();
			IPath published = archiveDestination.append(relative).append(deleted[i].getName());
			IStatus s = fc.deleteResource(published, new NullProgressMonitor());
			if( s != null && !s.isOK()) {
				ms.add(s);
			}
		}
		return ms;
	}
	/*
	 * Use the delta to publish only changed resources
	 * @return
	 */
	public MultiStatus publishModuleIncremental() throws CoreException {
		MultiStatus ms = new MultiStatus(ASWTPToolsPlugin.PLUGIN_ID, 0, "Errors while publishing binary module " + module[module.length-1].getName(), null);
		if( isNestedSingleResourceBinaryModule()) {
			copyOneResourceSingleBinary(all[0], ms);
		} else {
			publishModuleIncremental(delta, ms);
		}
		return ms;
	}
	public void publishModuleIncremental(IModuleResourceDelta[] delta, MultiStatus ms) throws CoreException {
		for( int i = 0; i < delta.length; i++ ) {
			int kind = delta[i].getKind();
			IModuleResource mr = delta[i].getModuleResource();
			if( kind == IModuleResourceDelta.REMOVED) {
				removeMembers(new IModuleResource[] {mr}, ms);
			} else if( kind == IModuleResourceDelta.CHANGED || kind == IModuleResourceDelta.ADDED) {
				if( mr instanceof IModuleFolder ) {
					IModuleResourceDelta[] children = delta[i].getAffectedChildren();
					publishModuleIncremental(children, ms);
				} else {
					copyOneResource(mr, ms);
				}
			}
		}
	}
	
	/*
	 * Remove the module. Then do a full publish
	 */
	public MultiStatus publishModuleFull() throws CoreException {
		MultiStatus status = (MultiStatus)removeDeletedBinaryModule();
		if( isNestedSingleResourceBinaryModule()) {
			copyOneResourceSingleBinary(all[0], status);
		} else {
			publishModuleMembers(all, status);
		}
		return status;
	}
	
	
	private boolean isNestedSingleResourceBinaryModule() {
		if( module.length > 1 && all.length == 1 && all[0] instanceof IModuleFile) {
			return true;
		}
		return false;
	}
	private void publishModuleMembers(IModuleResource[] members, MultiStatus ms) throws CoreException {
		for( int i = 0; i < members.length; i++ ) {
			IModuleResource mr = members[i];
			if( mr instanceof IModuleFolder ) {
				IModuleResource[] children = ((IModuleFolder)mr).members();
				publishModuleMembers(children, ms);
			} else {
				copyOneResource(mr, ms);
			}
		}
	}

	private void copyOneResource(IModuleResource r, MultiStatus ms) throws CoreException {
		copyOneResource(r, r.getName(), ms);
	}
	

	private void copyOneResourceSingleBinary(IModuleResource r, MultiStatus ms) throws CoreException {
		copyOneResource(r, archiveDestinationWithName.lastSegment(), ms);
	}
	
	private void copyOneResource(IModuleResource r, String lastSegmentName, MultiStatus ms) throws CoreException {
		File f = ModuleResourceUtil.getFile(r);
		IPath folder = archiveDestination.append(r.getModuleRelativePath());
		IPath dest = folder.append(lastSegmentName);
		IStatus s1 = fc.makeDirectoryIfRequired(folder, new NullProgressMonitor());
		IStatus result = null;
		if( s1 != null && !s1.isOK()) {
			result = s1;
		} else {
			IStatus s2 = fc.copyFile(f, dest, new NullProgressMonitor());
			result = s2;
		}
		if( result != null )
			ms.add(result);
	}

	
	private IModuleResource[] getAllResources(IModuleResourceDelta[] delta, int kind, boolean depthFirst) {
		if( delta == null )
			return new IModuleResource[0];
		
		ArrayList<IModuleResource> list = new ArrayList<IModuleResource>();
		for( int i = 0; i < delta.length; i++ ) {
			IModuleResource r = delta[i].getModuleResource();
			IModuleResourceDelta[] children = delta[i].getAffectedChildren();
			IModuleResource[] childrenRes = null;
			if( children != null ) {
				childrenRes = getAllResources(children, kind, depthFirst);
			} else {
				childrenRes = new IModuleResource[0];
			}
			
			if( depthFirst ) {
				list.addAll(Arrays.asList(childrenRes));
				if( delta[i].getKind() == kind) {
					list.add(r);
				}
			} else {
				if( delta[i].getKind() == kind) {
					list.add(r);
				}
				list.addAll(Arrays.asList(childrenRes));
			}
		}
		return (IModuleResource[]) list.toArray(new IModuleResource[list.size()]);
	}
}