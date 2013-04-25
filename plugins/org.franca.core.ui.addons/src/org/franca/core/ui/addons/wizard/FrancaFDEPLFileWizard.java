package org.franca.core.ui.addons.wizard;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Implementation of the Franca FDEPL file wizard.
 * 
 * @author Tamas Szabo
 *
 */
public class FrancaFDEPLFileWizard extends FrancaFileWizard {

	private static final String EXTENSION = "fdepl";
	private FrancaFileWizardContainerConfigurationPage page1;
	private FrancaFDEPLFileWizardConfigurationPage page2;
	
	@Override
	protected Map<String, String> collectParameters() {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("containerName", page1.getContainerName());
		parameters.put("fileName", page1.getFileName());
        // replace dots with slash in the path
		parameters.put("packageName", page1.getPackageName().replaceAll("\\.", "/"));
		parameters.put("definitionName", page2.getDefinitionName());
		parameters.put("specificationName", page2.getSpecificationName());
		return parameters;
	}
	
	@Override
	protected IPath performFileCreation(IProgressMonitor monitor, Map<String, String> parameters) {        
        return FrancaWizardUtil.createFrancaFDEPLFile(resourceSetProvider, parameters);
	}

	@Override
	public void addPages() {
		page1 = new FrancaFileWizardContainerConfigurationPage(EXTENSION);
        page1.init((IStructuredSelection) selection);
        page1.setDescription(NEW_FRANCA_FDEPL_FILE);
        page2 = new FrancaFDEPLFileWizardConfigurationPage();
        page2.setDescription(NEW_FRANCA_FDEPL_FILE);
        addPage(page1);
        addPage(page2);
        setForcePreviousAndNextButtons(false);
	}
}