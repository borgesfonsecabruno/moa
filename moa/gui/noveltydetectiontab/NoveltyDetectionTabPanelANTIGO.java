/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.gui.noveltydetectiontab;

import moa.gui.AbstractTabPanel;
import moa.gui.TaskTextViewerPanel;

/**
 *
 * @author Bruno
 */
public class NoveltyDetectionTabPanelANTIGO extends AbstractTabPanel {

	private static final long serialVersionUID = 1L;
	private NoveltyDetectionSetupTabANTIGO ndSetupTab;
    private javax.swing.JTabbedPane jTabbedPane1;
    private TaskTextViewerPanel previewPanel;
	
	public NoveltyDetectionTabPanelANTIGO() {
        initComponents();
        ndSetupTab.setClusteringTab(this);
    }
    
    @Override
    public String getTabTitle() {
        return "Novelty Detection";
    }

    @Override
    public String getDescription() {
        return "MOA Novelty Detection";
    }
    
    void toggle() {
    	testeANTIGO cTeste = new testeANTIGO();
    	cTeste.start(ndSetupTab.getStream0(), ndSetupTab.getAlgorithm());

    }

    void stop() {

    }
    
    private void jTabbedPane1FocusGained(java.awt.event.FocusEvent evt) {
        
    }

    private void jTabbedPane1MouseClicked(java.awt.event.MouseEvent evt) {
    }
    
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        ndSetupTab = new NoveltyDetectionSetupTabANTIGO();

        setLayout(new java.awt.BorderLayout());

        jTabbedPane1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTabbedPane1MouseClicked(evt);
            }
        });
        jTabbedPane1.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jTabbedPane1FocusGained(evt);
            }
        });
        jTabbedPane1.addTab("Setup", ndSetupTab);
        
        previewPanel = new TaskTextViewerPanel();
        jTabbedPane1.addTab("teste", previewPanel);
        
        add(jTabbedPane1, java.awt.BorderLayout.CENTER);
        

    }

    

    
}
