/*
PreferencesPanel.java this class is part of Galileo Firmware Update tool 
Copyright (C) 2015 Intel Corporation

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.intel.galileo.flash.tool;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.event.ActionEvent;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.event.ActionListener;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.io.FilenameFilter;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.SwingConstants;

/**
 * A JPanel to select the preferences for driving the firmware update.
 */
public class PreferencesPanel extends javax.swing.JPanel {

    /**
     * Creates new form PreferencesPanel
     * @param galileo flashing functionality
     */
    public PreferencesPanel(GalileoFirmwareUpdater galileo) {
        
        this.galileo = galileo;
        this.updateAction = null;
        this.galileo.addPropertyChangeListener(changes);
        initComponents();
        initFirmware();
        List<CommunicationService> services = galileo.getCommunicationServices();
        if (! services.isEmpty()) {
            servicesComboBox.removeAllItems();
            for (CommunicationService s : services) {
                servicesComboBox.addItem(s);                        
            }
            populateConnections(galileo);
            String currentConnection = galileo.getCommunicationConnection();
            if (currentConnection != null) {
                connectionComboBox.setSelectedItem(currentConnection);
            }
            
        }

        updateFirmwareVersion();
    }
    
    /*
     * Checks the current directory for possible *.cap file
     * if there is more than one, will take the latest
     */
    public URL isThereAnyCap()
    {

    	try {

    		SortedSet<File> modificationOrder = new TreeSet<File>(new Comparator<File>() {
    			public int compare(File a, File b) {
    				return (int) (a.lastModified() - b.lastModified());
    			}
    		});

    		File currentDir =  new File(System.getProperty("user.dir"));

    		// searching only "*.cap" files
    		FilenameFilter filter = new FilenameFilter () {
    			public boolean accept(File dir, String name) {
    				return name.toLowerCase().endsWith(".cap");
    			}
    		};

    		for (File file :currentDir.listFiles(filter)) {
    			modificationOrder.add(file);
    		}

    		File last = modificationOrder.last();
    		return last.toURI().toURL();
    	} catch (Exception e) {
    		return null;
    	}
    }
    
    /*
     *  Update Screen based in url choosen 
     */
    private void updateCanvasBasedInURL(URL _url) {
    	
    	
    	if (_url == null) return;
    	    	
		galileo.setLocalCapFile(_url);
		
        // ToDo - Remove the cache mechanism
	    String home = System.getProperty("user.home");
	    File f = new File(home, ".galileo");
	    f.mkdir();
        FirmwareCapsule cap = new FirmwareCapsule(galileo.getLocalCapFile(), 
					                                  f);
        // updating the cap file instance.
		galileo.setUpdate(cap);

		updateBoardVersion();	
    	
    }   
    
    /**
     * @wbp.parser.constructor
	 * Keep this for WindowBuilder
     */
    public PreferencesPanel(GalileoFirmwareUpdater galileo, FirmwareUpdateAction action) {
        
        this.galileo = galileo;
        this.galileo.addPropertyChangeListener(changes);
        this.updateAction = action;
        this.updateAction.setPreferencesPanel(this);
        initComponents();
        initFirmware();
        List<CommunicationService> services = galileo.getCommunicationServices();
        if (! services.isEmpty()) {
            servicesComboBox.removeAllItems();
            for (CommunicationService s : services) {
                servicesComboBox.addItem(s);                        
            }
            populateConnections(galileo);
            String currentConnection = galileo.getCommunicationConnection();
            if (currentConnection != null) {
                connectionComboBox.setSelectedItem(currentConnection);
            }
            
        }
        jLabel1.setVisible(false);
        servicesComboBox.setVisible(false);
        
        // if it is necessary to enable the feature to read automatically
        // local capsule file located in the same directory of this tool
        // then you need to add updateCanvasBasedInURL(isThereAnyCap())
        // before the updateFirmwareVersion()
        
        uploadFirmwareButton.setEnabled(false);
        updateFirmwareVersion();
    }
    
    private void populateConnections(GalileoFirmwareUpdater galileo) {
        CommunicationService current = galileo.getCommunicationService();
        uploadFirmwareButton.setEnabled(false);
        if (current != null) {
            servicesComboBox.setSelectedItem(current);
            jLabel2.setText(current.getConnectionLabel());
            List<String> connections = current.getAvailableConnections();
            connectionComboBox.removeAllItems();
            for (String connection : connections) {
                connectionComboBox.addItem(connection);
            }
            
        }
    }

    private void initFirmware() {
        List<FirmwareCapsule> available = galileo.getAvailableFirmware();

    }
    
    private SwingWorker boardVersionUpdater;
    private void updateBoardVersion() {
        boardVersion.setText("Unknown");
        if (galileo.getCommunicationConnection() != null) {
            boardVersionUpdater = new SwingWorker<GalileoVersion, Void>() {

                @Override
                protected GalileoVersion doInBackground() throws Exception {
                    return galileo.getCurrentBoardVersion();

                }

                @Override
                protected void done() {
                    try {
                        GalileoVersion vers = get();
                        if (vers != null) {
                            boardVersion.setText(vers.toPresentationString());
                            boardVersion.repaint();
                            uploadFirmwareButton.setEnabled(true);
                            msgJlabel.setText("");
                            msgJlabel.setVisible(true);
                        }
                        else {
                        	String port = connectionComboBox.getSelectedItem().toString();
                        	uploadFirmwareButton.setEnabled(false);
                        	msgJlabel.setText("<html><font color='red'>Galileo not found on " + port + "! Please make sure that you select the correct serial port and that you have permission to access.</font></html>");
                        	msgJlabel.setVisible(true);
                        }
                    } catch (InterruptedException unused) {
                    } catch (ExecutionException unused) {
                    }
                }

            };
            boardVersionUpdater.execute();
        }
    }
    
    
    private SwingWorker capsuleVersionUpdater;
    private void updateFirmwareVersion() {
    	
        capsuleVersion.setText("Unknown");
        if (galileo.getUpdate() != null) {
            capsuleVersionUpdater = new SwingWorker<GalileoVersion,Void>() {
                
                @Override
                protected GalileoVersion doInBackground() throws Exception {
                    return galileo.getUpdateVersion();

                }

                @Override
                protected void done() {
                    try {
                        GalileoVersion vers = get();
                        if (vers != null) {
                        	
                            capsuleVersion.setText(vers.toPresentationString());
                            FirmwareUpdateTool.capVersion = vers.toPresentationString();

                            // getting the same of the resource and setting the ratio text
                            resourceNameRatio.setText(vers.toPresentationString());
                            
                            capsuleVersion.repaint();

                            frame.repaint();
                            
                        }
                    } catch (InterruptedException unused) {
                    } catch (ExecutionException unused) {
                    }
                }
                
            };
           
           capsuleVersionUpdater.execute();
           repaint();
           
       }
    }
    
    private PropertyChangeListener changes = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String name = evt.getPropertyName();
            final Object o = evt.getNewValue();
            if ("updateVersion".equals(name)) {
                GalileoVersion v = (o != null) ? (GalileoVersion) o : null;
                final String updateVersion = (v != null) ? v.toPresentationString() : "Unknown";
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        capsuleVersion.setText(updateVersion);
                        capsuleVersion.repaint();
                                            
                    }
                });
            } else if ("currentBoardVersion".equals(name)) {
                GalileoVersion v = (o != null) ? (GalileoVersion) o : null;
                final String version = (v != null) ? v.toPresentationString() : "Unknown";
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        boardVersion.setText(version);
                        boardVersion.repaint();
                                            
                    }
                });
            }
        }
        
    };
    
    /* Necessary to fool the SWT and repaint the main frame */
    public void setFrame(JFrame f) {
    	if (f==null) {
    		throw new InternalError("NULL frame set ");
    		
    	} else {
        	frame = f;
    	}
     }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        servicesComboBox = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        connectionComboBox = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        capsuleVersion = new javax.swing.JTextField();
        capsuleVersion.setBackground(Color.LIGHT_GRAY);
        boardVersion = new javax.swing.JTextField();
        boardVersion.setBackground(Color.LIGHT_GRAY);
        jLabel5 = new javax.swing.JLabel();
        uploadFirmwareButton = new javax.swing.JButton("Upload Firmware");
        uploadFirmwareButton.setEnabled(false);
        msgJlabel = new javax.swing.JLabel();
        msgJlabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        uploadFirmwareButton.setAction(updateAction);
                
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel1.setText("Connection:");
        servicesComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "None Available" }));	

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText("Port:");

        connectionComboBox.setEditable(true);
        connectionComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "None" }));
        connectionComboBox.setName("connection"); // NOI18N
        connectionComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectionComboBoxActionPerformed(evt);
            }
        });

	connectionComboBox.addPopupMenuListener(new  PopupMenuListener() {

	    @Override
	    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		populateConnections(galileo);
	    }

	    @Override
	    public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {

	    }

	    @Override
	    public void popupMenuCanceled(PopupMenuEvent event) {

      	    }
	});
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setText("Firmware:");



        jLabel4.setHorizontalAlignment(SwingConstants.LEFT);
        jLabel4.setLabelFor(capsuleVersion);
        jLabel4.setText("Update Firmware Version:");

        capsuleVersion.setEditable(false);
        capsuleVersion.setText("Unknown");
        capsuleVersion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                capsuleVersionActionPerformed(evt);
            }
        });

        boardVersion.setEditable(false);
        boardVersion.setText("Unknown");

        jLabel5.setHorizontalAlignment(SwingConstants.LEFT);
        jLabel5.setText("Current Board Firmware:");
        
        resourceNameRatio = new JRadioButton("Browse for .cap file");
        resourceNameRatio.setSelected(true);
        buttonGroup.add(resourceNameRatio);
        
        JRadioButton browserRatio = new JRadioButton("Browse for .cap file");
        browserRatio.setSelected(true);
        buttonGroup.add(browserRatio);
         
        browserRatio.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent arg0) {

        		// allow the user to select the cap file in the file system
        		JFileChooser chooser = new JFileChooser();
        		FileNameExtensionFilter filter = new FileNameExtensionFilter("Galileo cap file", "cap");
        		chooser.setFileFilter(filter);
        		chooser.setCurrentDirectory(new File("."));
        		int returnVal = chooser.showOpenDialog(getParent());
        		if(returnVal == JFileChooser.APPROVE_OPTION) {

        	            try {
							updateCanvasBasedInURL(chooser.getSelectedFile().toURI().toURL());
						} catch (MalformedURLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
        		} else {
        			   // the user cancelled the operation
        			resourceNameRatio.setSelected(true);
        		}
        		
        	}
        });
        
        

        resourceNameRatio.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent arg0) {

        		// allow the user to select the cap file in the resource folder
                galileo.setUpdate(galileo.getAvailableFirmware().get(0));
        		
        	}
        });

        
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        layout.setHorizontalGroup(
        	layout.createParallelGroup(Alignment.LEADING)
        		.addGroup(layout.createSequentialGroup()
        			.addContainerGap()
        			.addGroup(layout.createParallelGroup(Alignment.LEADING)
        				.addGroup(layout.createSequentialGroup()
        					.addComponent(msgJlabel, GroupLayout.DEFAULT_SIZE, 499, Short.MAX_VALUE)
        					.addContainerGap())
        				.addGroup(layout.createSequentialGroup()
        					.addGroup(layout.createParallelGroup(Alignment.LEADING, false)
        						.addComponent(jLabel3, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        						.addComponent(jLabel2, GroupLayout.DEFAULT_SIZE, 84, Short.MAX_VALUE)
        						.addComponent(jLabel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        					.addGroup(layout.createParallelGroup(Alignment.LEADING)
        						.addGroup(layout.createSequentialGroup()
        							.addPreferredGap(ComponentPlacement.RELATED)
        							.addGroup(layout.createParallelGroup(Alignment.LEADING)
        								.addComponent(servicesComboBox, 0, 289, Short.MAX_VALUE)
        								.addGroup(layout.createSequentialGroup()
        									.addComponent(connectionComboBox, 0, 233, Short.MAX_VALUE)
        									.addGap(56))
        								.addGroup(layout.createSequentialGroup()
        									.addGroup(layout.createParallelGroup(Alignment.LEADING)
        										.addComponent(jLabel5, GroupLayout.PREFERRED_SIZE, 168, GroupLayout.PREFERRED_SIZE)
        										.addComponent(jLabel4, GroupLayout.PREFERRED_SIZE, 146, GroupLayout.PREFERRED_SIZE))
        									.addPreferredGap(ComponentPlacement.RELATED)
        									.addGroup(layout.createParallelGroup(Alignment.LEADING)
        										.addComponent(capsuleVersion, GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)
        										.addComponent(uploadFirmwareButton, GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)
        										.addComponent(boardVersion, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)))))
        						.addGroup(layout.createSequentialGroup()
        							.addGap(24)
        							.addGroup(layout.createParallelGroup(Alignment.LEADING)
        								.addComponent(resourceNameRatio)
        								.addComponent(browserRatio))))
        					.addGap(130))))
        );
        layout.setVerticalGroup(
        	layout.createParallelGroup(Alignment.LEADING)
        		.addGroup(layout.createSequentialGroup()
        			.addGap(13)
        			.addGroup(layout.createParallelGroup(Alignment.BASELINE)
        				.addComponent(jLabel1)
        				.addComponent(servicesComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        			.addPreferredGap(ComponentPlacement.RELATED)
        			.addGroup(layout.createParallelGroup(Alignment.BASELINE)
        				.addComponent(jLabel2)
        				.addComponent(connectionComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        			.addGap(11)
        			.addGroup(layout.createParallelGroup(Alignment.BASELINE)
        				.addComponent(jLabel3)
        				.addComponent(resourceNameRatio))
        			.addPreferredGap(ComponentPlacement.UNRELATED)
        			.addComponent(browserRatio)
        			.addGap(18)
        			.addGroup(layout.createParallelGroup(Alignment.BASELINE)
        				.addComponent(jLabel4)
        				.addComponent(capsuleVersion, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        			.addGap(18)
        			.addGroup(layout.createParallelGroup(Alignment.BASELINE)
        				.addComponent(jLabel5)
        				.addComponent(boardVersion, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        			.addGap(18)
        			.addComponent(uploadFirmwareButton, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE)
        			.addPreferredGap(ComponentPlacement.UNRELATED)
        			.addComponent(msgJlabel, GroupLayout.PREFERRED_SIZE, 42, GroupLayout.PREFERRED_SIZE)
        			.addContainerGap(36, Short.MAX_VALUE))
        );
        this.setLayout(layout);

        connectionComboBox.getAccessibleContext().setAccessibleName("");
    }// </editor-fold>//GEN-END:initComponents

    private void capsuleVersionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_capsuleVersionActionPerformed
        
        
    }//GEN-LAST:event_capsuleVersionActionPerformed

    private void connectionComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectionComboBoxActionPerformed
    	try {
    		String connection = (String)connectionComboBox.getSelectedItem();
	    	if(connection != lastPort)
	    	{
	    		lastPort = connection;
	    		galileo.invalidateBoardVersion();
	    	}
	    	if(!galileo.getQueryState())
            {	
            	galileo.setQueryState(true);
            	boardVersion.setText("");
            	connection = (String)connectionComboBox.getSelectedItem();
            	galileo.setCommunicationConnection(connection);
            	updateBoardVersion();
            	galileo.setQueryState(false);
            }
        
    	} catch (Exception e) {
			e.printStackTrace();
		}
    }//GEN-LAST:event_connectionComboBoxActionPerformed
    
    
    public void disableUI(){
    	parentTool.setEnabled(false);
    }
    
    public void enableUI(){
    	parentTool.setEnabled(true);
    }
    
    public void setFirmwareUpdateTool(FirmwareUpdateTool tool)
    {
    	this.parentTool = tool;
    }
    
    public boolean isUpdateRunning(){
    	return updateAction.isRunning();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField boardVersion;
    private javax.swing.JTextField capsuleVersion;
    private JRadioButton resourceNameRatio;
    private javax.swing.JComboBox connectionComboBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel msgJlabel;
    private javax.swing.JButton uploadFirmwareButton;
    private javax.swing.JComboBox servicesComboBox;
    // End of variables declaration//GEN-END:variables

    private final GalileoFirmwareUpdater galileo;
    private final FirmwareUpdateAction updateAction;
    private String lastPort ="";
    private final ButtonGroup buttonGroup = new ButtonGroup();  
    
    private JFrame frame;
    private FirmwareUpdateTool parentTool;
}
