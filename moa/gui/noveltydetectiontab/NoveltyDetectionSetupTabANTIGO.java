
package moa.gui.noveltydetectiontab;

import moa.gui.TaskTextViewerPanel;
import moa.noveltydetection.AbstractNoveltyDetection;
import moa.streams.clustering.ClusteringStream;


public class NoveltyDetectionSetupTabANTIGO extends javax.swing.JPanel {
	private static final long serialVersionUID = 1L;
	
	private NoveltyDetectionTabPanelANTIGO ndTab;
	private javax.swing.JButton buttonStart;
    private javax.swing.JButton buttonStop;
    private moa.gui.noveltydetectiontab.NoveltyDetectionAlgoPanelANTIGO ndAlgoPanel;
    private TaskTextViewerPanel textViewerPanel;


    public NoveltyDetectionSetupTabANTIGO() {
        initComponents();
        ndAlgoPanel.renderAlgoPanel();
    }

    public ClusteringStream getStream0(){
        return ndAlgoPanel.getStream();
    }
    
    public AbstractNoveltyDetection getAlgorithm(){
        return ndAlgoPanel.getAlgorithm();
    }
    
    public TaskTextViewerPanel getTextViewerPanel() {
		return textViewerPanel;
	}
   
    
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        ndAlgoPanel = new moa.gui.noveltydetectiontab.NoveltyDetectionAlgoPanelANTIGO();

        buttonStart = new javax.swing.JButton();
        buttonStop = new javax.swing.JButton();

        textViewerPanel = new TaskTextViewerPanel();

        setLayout(new java.awt.GridBagLayout());

        ndAlgoPanel.setMinimumSize(new java.awt.Dimension(335, 150));
        ndAlgoPanel.setPanelTitle("Novelty Detection Algorithm Setup");
        ndAlgoPanel.setPreferredSize(new java.awt.Dimension(335, 150));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(ndAlgoPanel, gridBagConstraints);

        buttonStart.setText("Start");
        buttonStart.setPreferredSize(new java.awt.Dimension(80, 23));
        buttonStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStartActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(buttonStart, gridBagConstraints);

        buttonStop.setText("Stop");
        buttonStop.setEnabled(false);
        buttonStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonStopActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(buttonStop, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(textViewerPanel, gridBagConstraints);

        
        
    }                   
    
    void setClusteringTab(NoveltyDetectionTabPanelANTIGO ndTab) {
        this.ndTab = ndTab;
    }
    
    private void buttonStartActionPerformed(java.awt.event.ActionEvent evt) {                                            
        toggle(true);
    }                                           

    private void buttonStopActionPerformed(java.awt.event.ActionEvent evt) {                                           
        stop(true);
    }                                          
    
    public void toggleRunMode(){
        toggle(false);
    }

    public void stopRun(){
        stop(false);
    }
    
    private void stop(boolean internal) {
        buttonStart.setEnabled(true);
        buttonStart.setText("Start");
        buttonStop.setEnabled(false);
        setStateConfigButtons(true);

        //push event forward to the cluster tab
        if(internal)
            ndTab.stop();
    }
    
    private void toggle(boolean internal) {
        setStateConfigButtons(false);
        if(buttonStart.getText().equals("Pause")){
            buttonStart.setText("Resume");

        }
        else{
            buttonStart.setText("Pause");
        }

        //push event forward to the cluster tab
        if(internal)
            ndTab.toggle();
    }
    

    private void setStateConfigButtons(boolean state){
        buttonStop.setEnabled(!state);
    }

                                   
}
