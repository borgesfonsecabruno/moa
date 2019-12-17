package moa.gui.noveltydetectiontab;

import java.awt.BorderLayout;

import moa.gui.AbstractTabPanel;

public class NDTabPanel extends AbstractTabPanel {

	private static final long serialVersionUID = 1L;

	protected NDTaskManagerPanel taskManagerPanel;

	protected NDPreviewPanel previewPanel;

	public NDTabPanel() {
		this.taskManagerPanel = new NDTaskManagerPanel();
		this.previewPanel = new NDPreviewPanel();
		this.taskManagerPanel.setPreviewPanel(this.previewPanel);
		setLayout(new BorderLayout());
		add(this.taskManagerPanel, BorderLayout.NORTH);
		add(this.previewPanel, BorderLayout.CENTER);
	}

	//returns the string to display as title of the tab
    @Override
	public String getTabTitle() {
		return "Novelty";
	}

	//a short description (can be used as tool tip) of the tab, or contributor, etc.
    @Override
	public String getDescription(){
		return "MOA Novelty Detection";
	}
}
