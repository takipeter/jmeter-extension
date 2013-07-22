package com.tpeter.loadtest.jmeter.vizualizers;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import kg.apc.jmeter.vizualizers.JSettingsPanel;

/**
 * Add custom fields to the options panel
 * 
 * @author Peter
 *
 */
public class MySettingsJPanel extends JSettingsPanel
{
	private static final String MY_OPTIONS_PANEL_LABEL = "My Graph Settings";
	private static final String SCALE_BY_LABEL = "Thread num scale by: ";
	private final static int THR_SCALE_BY = 10;

    private JTextField jTextFieldThrScale = new JTextField();
	private JLabel jLabelMaxPoints = new JLabel();
	private int thrScaleBy = THR_SCALE_BY;

	private final ThreadAndResponseTimeVSTimeGUI parent;
    
    public int getThrScaleBy() {
		return thrScaleBy;
	}

    public MySettingsJPanel(ThreadAndResponseTimeVSTimeGUI parent, int options)
    {
    	super(parent, options);
    	this.parent = parent;

    	// init option fields
		jLabelMaxPoints.setText(SCALE_BY_LABEL);		
		jTextFieldThrScale.setText(Integer.toString(THR_SCALE_BY));
		jTextFieldThrScale.setColumns(4);
		jTextFieldThrScale.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				update();
			}
		});
		
		// init main panel
		JPanel myjPanelAllSettingsContainer =  new JPanel();
		myjPanelAllSettingsContainer.setBorder(javax.swing.BorderFactory
				.createTitledBorder(MY_OPTIONS_PANEL_LABEL));
		myjPanelAllSettingsContainer.setLayout(new FlowLayout(FlowLayout.LEFT));

		// add options fields
		myjPanelAllSettingsContainer.add(jLabelMaxPoints);
		myjPanelAllSettingsContainer.add(jTextFieldThrScale);

		// add main panel
		add(myjPanelAllSettingsContainer, BorderLayout.BEFORE_FIRST_LINE);
    }

	private void update() {
		String text = jTextFieldThrScale.getText();
		thrScaleBy = Integer.parseInt(text);
		
		parent.updateData();
	}

    @Override
    public boolean isPreview() {
        return true;
    }
    
}