/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.flow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.Collection;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JViewport;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.actions.AutoWireAction;
import com.rapidminer.gui.flow.processrendering.annotations.AnnotationsVisualizer;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererAnnotationEvent;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererEventListener;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererModelEvent;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererOperatorEvent;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.ViewToolBar;
import com.rapidminer.gui.tools.components.DropDownButton;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;


/**
 * Contains the main {@link ProcessRendererView} and a {@link ProcessButtonBar} to navigate through
 * the process.
 *
 * @author Simon Fischer, Tobias Malbrecht
 */
public class ProcessPanel extends JPanel implements Dockable, ProcessEditor {

	private static final long serialVersionUID = -4419160224916991497L;

	/** the process renderer instance */
	private final ProcessRendererView renderer;

	/** the flow visualizer instance */
	private final FlowVisualizer flowVisualizer;

	/** the workflow annotations handler instance */
	private final AnnotationsVisualizer annotationsHandler;

	private final ProcessButtonBar processButtonBar;

	private OperatorChain operatorChain;

	private final JScrollPane scrollPane;

	public ProcessPanel(final MainFrame mainFrame) {
		processButtonBar = new ProcessButtonBar(mainFrame);

		ProcessRendererModel model = new ProcessRendererModel();
		// listen for display chain changes and update breadcrumbs accordingly
		model.registerEventListener(new ProcessRendererEventListener() {

			@Override
			public void operatorsChanged(ProcessRendererOperatorEvent e, Collection<Operator> operators) {
				// don't care
			}

			@Override
			public void modelChanged(ProcessRendererModelEvent e) {
				switch (e.getEventType()) {
					case DISPLAYED_CHAIN_CHANGED:
					case DISPLAYED_PROCESSES_CHANGED:
						processButtonBar.setSelectedNode(renderer.getModel().getDisplayedChain());
						break;
					case PROCESS_SIZE_CHANGED:
					case MISC_CHANGED:
					default:
						break;
				}
			}

			@Override
			public void annotationsChanged(ProcessRendererAnnotationEvent e, Collection<WorkflowAnnotation> annotations) {
				// don't care
			}
		});
		renderer = new ProcessRendererView(model, this, mainFrame);
		renderer.setBackground(SwingTools.LIGHTEST_BLUE);

		flowVisualizer = new FlowVisualizer(renderer);
		annotationsHandler = new AnnotationsVisualizer(renderer, flowVisualizer);

		ViewToolBar toolBar = new ViewToolBar(ViewToolBar.LEFT);

		// overwrite the default border
		toolBar.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));

		toolBar.add(annotationsHandler.makeAddAnnotationAction(null), ViewToolBar.RIGHT);
		JToggleButton toggleAnnotations = annotationsHandler.getToggleAnnotationsAction().createToggleButton();
		toggleAnnotations.setText(null);
		toggleAnnotations.doClick();
		toolBar.add(toggleAnnotations, ViewToolBar.RIGHT);

		final Action autoWireAction = new AutoWireAction(mainFrame, "wire", CompatibilityLevel.PRE_VERSION_5, false, true);
		DropDownButton autoWireDropDownButton = DropDownButton.makeDropDownButton(autoWireAction);
		autoWireDropDownButton.add(autoWireAction);
		autoWireDropDownButton.add(new AutoWireAction(mainFrame, "wire_recursive", CompatibilityLevel.PRE_VERSION_5, true,
				true));
		autoWireDropDownButton.add(new AutoWireAction(mainFrame, "rewire", CompatibilityLevel.PRE_VERSION_5, false, false));
		autoWireDropDownButton.add(new AutoWireAction(mainFrame, "rewire_recursive", CompatibilityLevel.PRE_VERSION_5, true,
				false));
		autoWireDropDownButton.addToToolBar(toolBar, ViewToolBar.RIGHT);

		toolBar.add(renderer.getArrangeOperatorsAction(), ViewToolBar.RIGHT);
		toolBar.add(flowVisualizer.SHOW_ORDER_TOGGLEBUTTON, ViewToolBar.RIGHT);
		toolBar.add(renderer.getAutoFitAction(), ViewToolBar.RIGHT);
		JToggleButton toggleRealMetadataPropagationButton = new JToggleButton(mainFrame.PROPAGATE_REAL_METADATA_ACTION);
		toggleRealMetadataPropagationButton.setText("");
		toolBar.add(toggleRealMetadataPropagationButton, ViewToolBar.RIGHT);

		setLayout(new BorderLayout());

		JLayeredPane processLayeredPane = new JLayeredPane();
		processLayeredPane.setLayout(new BorderLayout());
		processLayeredPane.add(processButtonBar, BorderLayout.WEST, 1);
		processLayeredPane.add(toolBar, BorderLayout.EAST, 0);
		processLayeredPane.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
		add(processLayeredPane, BorderLayout.NORTH);

		scrollPane = new ExtendedJScrollPane(renderer);
		scrollPane.getVerticalScrollBar().setUnitIncrement(10);
		scrollPane.setBorder(null);

		add(scrollPane, BorderLayout.CENTER);

	}

	/**
	 * Shows the specified {@link OperatorChain} in the {@link ProcessRendererView}.
	 *
	 * @param operatorChain
	 *            the operator chain to show, must not be {@code null}
	 * @deprecated use {@link ProcessRendererModel#setDisplayedChain(OperatorChain)} and
	 *             {@link ProcessRendererModel#fireDisplayedChainChanged()} instead
	 */
	@Deprecated
	public void showOperatorChain(OperatorChain operatorChain) {
		if (operatorChain == null) {
			throw new IllegalArgumentException("operatorChain must not be null!");
		}

		this.operatorChain = operatorChain;

		renderer.getModel().setDisplayedChain(operatorChain);
		renderer.getModel().fireDisplayedChainChanged();
	}

	@Override
	public void setSelection(List<Operator> selection) {
		Operator first = selection.isEmpty() ? null : selection.get(0);
		if (first != null) {
			processButtonBar.addToHistory(first);
		}
	}

	@Override
	public void processChanged(Process process) {
		processButtonBar.clearHistory();
	}

	@Override
	public void processUpdated(Process process) {
		renderer.processUpdated();
		if (operatorChain != null) {
			processButtonBar.setSelectedNode(operatorChain);
		}
	}

	/**
	 * The {@link ProcessRendererView} which is responsible for displaying the current process as
	 * well as interaction with it
	 *
	 * @return the instance, never {@code null}
	 */
	public ProcessRendererView getProcessRenderer() {
		return renderer;
	}

	/**
	 * The {@link FlowVisualizer} instance tied to the process renderer.
	 *
	 * @return the instance, never {@code null}
	 */
	public FlowVisualizer getFlowVisualizer() {
		return flowVisualizer;
	}

	/**
	 * The {@link AnnotationsVisualizer} instance tied to the process renderer.
	 *
	 * @return the instance, never {@code null}
	 */
	public AnnotationsVisualizer getAnnotationsHandler() {
		return annotationsHandler;
	}

	public static final String PROCESS_PANEL_DOCK_KEY = "process_panel";
	private final DockKey DOCK_KEY = new ResourceDockKey(PROCESS_PANEL_DOCK_KEY);
	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}

	public JViewport getViewPort() {
		return scrollPane.getViewport();
	}
}
