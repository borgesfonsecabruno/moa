/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package moa.gui.noveltydetectiontab;

import com.github.javacliparser.Option;
import com.github.javacliparser.gui.OptionEditComponent;
import com.github.javacliparser.gui.OptionsConfigurationPanel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import moa.gui.GUIUtils;
import moa.noveltydetection.AbstractNoveltyDetection;
import moa.noveltydetection.NoveltyDetection;
import moa.options.ClassOption;
import moa.streams.clustering.ClusteringStream;

/**
 *
 * @author Bruno
 */
public class NoveltyDetectionAlgoPanelANTIGO extends javax.swing.JPanel implements ActionListener {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected List<OptionEditComponent> editComponents = new LinkedList<OptionEditComponent>();

    private ClassOption streamOption = new ClassOption("Stream", 's',
                "", ClusteringStream.class,
                "FileStream -f C:\\Users\\bf_04\\Documents\\MOA3_fold1_ini.arff");

    private ClassOption algorithmOption0 = new ClassOption("Algorithm", 'n',
            "Algorithm to use.", AbstractNoveltyDetection.class, "minas.Minas");
 
    
   public NoveltyDetectionAlgoPanelANTIGO () {
       InitComponents();
   }
   
   public void renderAlgoPanel() {

       setLayout(new BorderLayout());

       ArrayList<Option> options = new ArrayList<Option>();
       options.add(streamOption);
       options.add(algorithmOption0);

       JPanel optionsPanel = new JPanel();
       GridBagLayout gbLayout = new GridBagLayout();
       optionsPanel.setLayout(gbLayout);

       //Create generic label constraints
       GridBagConstraints gbcLabel = new GridBagConstraints();
       gbcLabel.gridx = 0;
       gbcLabel.fill = GridBagConstraints.NONE;
       gbcLabel.anchor = GridBagConstraints.EAST;
       gbcLabel.weightx = 0;
       gbcLabel.insets = new Insets(5, 5, 5, 5);

       //Create generic editor constraints
       GridBagConstraints gbcOption = new GridBagConstraints();
       gbcOption.gridx = 1;
       gbcOption.fill = GridBagConstraints.HORIZONTAL;
       gbcOption.anchor = GridBagConstraints.CENTER;
       gbcOption.weightx = 1;
       gbcOption.insets = new Insets(5, 5, 5, 0);

       //Stream Option
       JLabel labelStream = new JLabel("Stream");
       labelStream.setToolTipText("Stream to use.");
       optionsPanel.add(labelStream, gbcLabel);
       JComponent editorStream = getEditComponent(streamOption);
       labelStream.setLabelFor(editorStream);
       editComponents.add((OptionEditComponent) editorStream);
       optionsPanel.add(editorStream, gbcOption);

       //Algorithm0 Option
       JLabel labelAlgo0 = new JLabel("Algorithm1");
       labelAlgo0.setToolTipText("Algorithm to use.");
       optionsPanel.add(labelAlgo0, gbcLabel);
       JComponent editorAlgo0 = getEditComponent(algorithmOption0);
       labelAlgo0.setLabelFor(editorAlgo0);
       editComponents.add((OptionEditComponent) editorAlgo0);
       optionsPanel.add(editorAlgo0, gbcOption);

       //use comparison Algorithm Option
       GridBagConstraints gbcClearButton = new GridBagConstraints();
       gbcClearButton.gridx = 2;
       gbcClearButton.gridy = 2;
       gbcClearButton.fill = GridBagConstraints.NONE;
       gbcClearButton.anchor = GridBagConstraints.CENTER;
       gbcClearButton.insets = new Insets(5, 0, 5, 5);

       add(optionsPanel);
   }

    public JComponent getEditComponent(Option option){
        return OptionsConfigurationPanel.getEditComponent(option);
    }
    
    public ClusteringStream getStream(){
        ClusteringStream s = null;
        applyChanges();
        try {
            s = (ClusteringStream) ClassOption.cliStringToObject(streamOption.getValueAsCLIString(), ClusteringStream.class, null);
        } catch (Exception ex) {
            Logger.getLogger(NoveltyDetectionAlgoPanelANTIGO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return s;
    }
    
    public AbstractNoveltyDetection getAlgorithm(){
        AbstractNoveltyDetection nd = null;
        applyChanges();
        try {
            nd = (AbstractNoveltyDetection) ClassOption.cliStringToObject(algorithmOption0.getValueAsCLIString(), NoveltyDetection.class, null);
        } catch (Exception ex) {
            Logger.getLogger(NoveltyDetectionAlgoPanelANTIGO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return nd;
    }
    
    public String getStreamValueAsCLIString(){
        applyChanges();
        return streamOption.getValueAsCLIString();
    }

    public String getAlgorithm0ValueAsCLIString(){
        applyChanges();
        return algorithmOption0.getValueAsCLIString();
    }
    
    /* We need to fetch the right item from editComponents list, index needs to match GUI order */
    public void setStreamValueAsCLIString(String s){
        streamOption.setValueViaCLIString(s);
        editComponents.get(0).setEditState(streamOption.getValueAsCLIString());
    }

    public void setAlgorithm0ValueAsCLIString(String s){
        algorithmOption0.setValueViaCLIString(s);
        editComponents.get(1).setEditState(algorithmOption0.getValueAsCLIString());
    }
    
    public void applyChanges() {
        for (OptionEditComponent editor : this.editComponents) {
            try {
                editor.applyState();
            } catch (Exception ex) {
                GUIUtils.showExceptionDialog(this, "Problem with option "
                        + editor.getEditedOption().getName(), ex);
            }
        }
    }

    public void setPanelTitle(String title){
        setBorder(javax.swing.BorderFactory.createTitledBorder(null,title, javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 11)));
    }                                        

    private void InitComponents() {
        setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Novelty Detection Algorithm Setup", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 11))); // NOI18N
        setLayout(new java.awt.GridBagLayout());
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals("clear")){
            algorithmOption0.setValueViaCLIString("None");
            editComponents.get(2).setEditState("None");
        }
    }

    // Variables declaration - do not modify                     
    // End of variables declaration                   
}
