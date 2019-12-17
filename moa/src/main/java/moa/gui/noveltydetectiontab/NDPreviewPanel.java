package moa.gui.noveltydetectiontab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import moa.core.StringUtils;
import moa.evaluation.MeasureCollection;
import moa.evaluation.NDBasic;
import moa.evaluation.preview.Preview;
import moa.gui.PreviewPanel;
import moa.tasks.FailedTaskReport;
import moa.tasks.ResultPreviewListener;
import moa.tasks.meta.ALTaskThread;

public class NDPreviewPanel extends JPanel implements ResultPreviewListener {

	private static final long serialVersionUID = 1L;
	
	protected ALTaskThread previewedThread;

    protected JLabel previewLabel = new JLabel("No preview available");

    protected JButton refreshButton = new JButton("Refresh");

    protected JLabel autoRefreshLabel = new JLabel("Auto refresh: ");

    protected JComboBox<String> autoRefreshComboBox;

    protected NDTaskTextViewerPanel textViewerPanel;

    protected javax.swing.Timer autoRefreshTimer;
	public enum TypePanel {
		ND(new NDBasic());

		private final MeasureCollection measureCollection;
		//Constructor
		TypePanel(MeasureCollection measureCollection){
			this.measureCollection = measureCollection;
		}

		public MeasureCollection getMeasureCollection(){
			return (MeasureCollection) this.measureCollection.copy();
		}
	}
	
	public NDPreviewPanel() {
		this.textViewerPanel = new NDTaskTextViewerPanel(); 
		this.autoRefreshComboBox = new JComboBox<String>(PreviewPanel.autoFreqStrings);
		this.autoRefreshComboBox.setSelectedIndex(1);
		
		JPanel controlPanel = new JPanel();
        controlPanel.add(this.previewLabel);
        controlPanel.add(this.refreshButton);
        controlPanel.add(this.autoRefreshLabel);
        controlPanel.add(this.autoRefreshComboBox);
        setLayout(new BorderLayout());
        add(controlPanel, BorderLayout.NORTH);
        add(this.textViewerPanel, BorderLayout.CENTER);
        
        this.refreshButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                refresh();
            }
        });
        this.autoRefreshTimer = new javax.swing.Timer(1000,
                new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        refresh();
                    }
                });
        this.autoRefreshComboBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                updateAutoRefreshTimer();
            }
        });
        setTaskThreadToPreview(null);
	}
	
	public void refresh() {
        if (this.previewedThread != null) {
            if (this.previewedThread.isComplete()) {
                setLatestPreview();
                disableRefresh();
            } else {
                this.previewedThread.getPreview(NDPreviewPanel.this);
            }
        }
    }
	
	public void setTaskThreadToPreview(ALTaskThread thread) {
        this.previewedThread = thread;
        setLatestPreview();
        if (thread == null) {
            disableRefresh();
        } else if (!thread.isComplete()) {
            enableRefresh();
        }
    }	
	
	public void setLatestPreview() {
		if(this.previewedThread != null && this.previewedThread.isFailed())
    	{
    		FailedTaskReport failedTaskReport = (FailedTaskReport) this.previewedThread.getFinalResult();
    		this.textViewerPanel.setErrorText(failedTaskReport);
    		this.textViewerPanel.setGraph(null, null);
    	}
    	else
    	{
        	Preview preview = null;
    		if ((this.previewedThread != null) && this.previewedThread.isComplete()) {
    			// cancelled, completed or failed task
    			// TODO if the task is failed, the finalResult is a FailedTaskReport, which is not a Preview
    			preview = (Preview) this.previewedThread.getFinalResult();
    			this.previewLabel.setText("Final result");
    			disableRefresh();
    		} else if (this.previewedThread != null){
    			// running task
    			preview = (Preview) this.previewedThread.getLatestResultPreview();
    			double grabTime = this.previewedThread.getLatestPreviewGrabTimeSeconds();
    			String grabString = " (" + StringUtils.secondsToDHMSString(grabTime) + ")";
    			if (preview == null) {
    				this.previewLabel.setText("No preview available" + grabString);
    			} else {
    				this.previewLabel.setText("Preview" + grabString);
    			}
    		} else {
    			// no thread
    			this.previewLabel.setText("No preview available");
    			preview = null;
    		}
        	
    		this.textViewerPanel.setText(preview);
    		this.textViewerPanel.setGraph(preview, getColorCodings(this.previewedThread));
    	}
    }
	
	public void updateAutoRefreshTimer() {
        int autoDelay = PreviewPanel.autoFreqTimeSecs[this.autoRefreshComboBox.getSelectedIndex()];
        if (autoDelay > 0) {
            if (this.autoRefreshTimer.isRunning()) {
                this.autoRefreshTimer.stop();
            }
            this.autoRefreshTimer.setDelay(autoDelay * 1000);
            this.autoRefreshTimer.start();
        } else {
            this.autoRefreshTimer.stop();
        }
    }

    public void disableRefresh() {
        this.refreshButton.setEnabled(false);
        this.autoRefreshLabel.setEnabled(false);
        this.autoRefreshComboBox.setEnabled(false);
        this.autoRefreshTimer.stop();
    }

    public void enableRefresh() {
        this.refreshButton.setEnabled(true);
        this.autoRefreshLabel.setEnabled(true);
        this.autoRefreshComboBox.setEnabled(true);
        updateAutoRefreshTimer();
    }
    
    private Color[] getColorCodings(ALTaskThread thread) {

    	Color[] colors = new Color[1];
    	colors[0] = Color.BLACK;

    	return colors;
    }
    
    @Override
    public void latestPreviewChanged() {
        setTaskThreadToPreview(this.previewedThread);
    }
}
