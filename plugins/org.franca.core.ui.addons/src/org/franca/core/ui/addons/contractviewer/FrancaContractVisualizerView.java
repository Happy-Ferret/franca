/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.de).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.franca.core.ui.addons.contractviewer;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;
import org.franca.core.contracts.ContractDotGenerator;
import org.franca.core.franca.FInterface;
import org.franca.core.franca.FModel;
import org.franca.core.franca.FState;
import org.franca.core.franca.FTransition;
import org.franca.core.ui.addons.contractviewer.graph.Graph;
import org.franca.core.ui.addons.contractviewer.graph.GraphConnection;
import org.franca.core.ui.addons.contractviewer.graph.GraphNode;
import org.franca.core.ui.addons.contractviewer.graph.ZestStyles;
import org.franca.core.ui.addons.contractviewer.util.GraphSelectionListener;
import org.franca.core.utils.FrancaRecursiveValidator;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * A {@link ViewPart} implementation used to display the Franca contracts in a directed graph form. 
 * 
 * @author Tamas Szabo
 *
 */
public class FrancaContractVisualizerView extends ViewPart {

	private static String viewId = "org.franca.core.ui.addons.contractviewer";
	private XtextEditor activeEditor;
	private IFile activeFile;
	private static WeakReference<FrancaContractVisualizerView> instance = new WeakReference<FrancaContractVisualizerView>(null);
	private FModel activeModel;
	private FModel previousModel;
	private Map<FState, Set<FTransition>> backwardIndex;
	private Graph graph;
	private ContractDotGenerator generator;
	private SelectionListener selectionListener;
	
	@Inject
	Injector injector;
	
	@Inject
	private FrancaRecursiveValidator validator;
	
	public FrancaContractVisualizerView() {
		generator = new ContractDotGenerator();
		previousModel = null;
	}
	
    public static FrancaContractVisualizerView getInstance() {
    	if (instance.get() == null) {
	        IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	        if (activeWorkbenchWindow != null && activeWorkbenchWindow.getActivePage() != null) {
	            instance = new WeakReference<FrancaContractVisualizerView>((FrancaContractVisualizerView) activeWorkbenchWindow.getActivePage().findView(viewId));
	        }
    	}
        return instance.get();
    }
	
	@Override
	public void createPartControl(Composite parent) {
		selectionListener = new GraphSelectionListener();
		injector.injectMembers(selectionListener);
		graph = new Graph(parent, SWT.NONE);
		graph.setConnectionStyle(ZestStyles.CONNECTIONS_DIRECTED);
		graph.addSelectionListener(selectionListener);
	}

	@Override
	public void setFocus() {
		graph.setFocus();
	}
	
	public IFile getActiveFile() {
		return activeFile;
	}
	
	public void setActiveFile(IFile activeFile) {
		this.activeFile = activeFile;
	}
	
	public void setActiveEditor(XtextEditor activeEditor) {
		this.activeEditor = activeEditor;
	}
	
	public XtextEditor getActiveEditor() {
		return activeEditor;
	}
	
	public FModel getActiveModel() {
		return activeModel;
	}
	
	public Map<FState, Set<FTransition>> getBackwardIndex() {
		return Collections.unmodifiableMap(backwardIndex);
	}
	
	public void updateModel() {
		if (activeEditor != null) {
			activeEditor.getDocument().readOnly(new IUnitOfWork.Void<XtextResource>() {
				@Override
				public void process(XtextResource resource) throws Exception {
					if (resource != null) {
						for (EObject obj : resource.getContents()) {
							if (obj instanceof FModel) {
								activeModel = (FModel) obj;
								break;
							}
						}
					}
				}
			});
		}
	
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (activeModel != null /*previousModel == null || (!previousModel.equals(activeModel))*/) {
					graph.clear();
					if (!validator.hasErrors(activeModel.eResource())) {
						constructGraph();
						previousModel = activeModel;	
					}					
				}
			}
		});
	}
	
	public void clear() {
		this.activeEditor = null;
		this.activeFile = null;
		this.activeModel = null;
		this.previousModel = null;
	}
	
	private void constructGraph() {
		if (activeModel != null) {
			Map<FState, GraphNode> nodeMap = new HashMap<FState, GraphNode>();
			
			for (FInterface _interface : activeModel.getInterfaces()) {
				if (_interface != null && _interface.getContract() != null) {
					for (FState state : _interface.getContract().getStateGraph().getStates()) {
						GraphNode node = new GraphNode(graph, SWT.NONE, state.getName());
						node.setData(state.getName());
						nodeMap.put(state, node);
					}
				}
			}
			
			for (FState state : nodeMap.keySet()) {
				for (FTransition transition : state.getTransitions()) {
					GraphNode toState = nodeMap.get(transition.getTo());
					if (toState != null) {
						GraphConnection connection = new GraphConnection(graph, SWT.NONE, nodeMap.get(state), nodeMap.get(transition.getTo()));
						String label = generator.genLabel(transition);
						connection.setText(label);
						connection.setData(label);
					}
				}
			}
			
			graph.applyLayout();
		}
	}
	
}
