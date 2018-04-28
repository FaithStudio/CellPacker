package dev.lb.cellpacker.structure.view;

import java.awt.Component;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;
import dev.lb.cellpacker.structure.resource.Resource;

public class SingleResourceView extends ResourceView{

	private Resource currentResource;
	private Resource originalResource;
	private boolean changesMade;
	private String viewName;
	private boolean showOriginal;
	
	private Component display;
	private Component displayOriginal;
	private Component options;
	private JMenuItem[] menu;
	
	public SingleResourceView(String name, Resource resource) {
		currentResource = resource;
		originalResource = null;
		changesMade = false;
		showOriginal = false;
		viewName = name;
	}
	
	@Override
	public String getName() {
		return viewName;
	}

	@Override
	public Component getControls() {
		init();
		return options;
	}

	@Override
	public Component getDisplay() {
		init();
		return showOriginal ? displayOriginal : display;
	}

	@Override
	public boolean setShowOriginals(boolean value) {
		showOriginal = value;
		if(!changesMade) showOriginal = false;
		return showOriginal;
	}

	@Override
	public void replaceCurrentResource(Component dialogParent) {
		JFileChooser jfc = new JFileChooser();
		jfc.setFileFilter(currentResource.getFileFilter());
		Resource newRes = ResourceView.selectReplaceResource(dialogParent, currentResource); 
		if(newRes != null){
			if(changesMade){
				currentResource = newRes;
			}else{
				originalResource = currentResource;
				currentResource = newRes;
				changesMade = true;
			}
		}
		init();
	}

	@Override
	public void restoreCurrentResource(Component dialogParent) {
		restoreAllResources(dialogParent);//The same since there is only one
	}

	@Override
	public void restoreAllResources(Component dialogParent) {
		if(changesMade && JOptionPane.showConfirmDialog(dialogParent, "<html>Changes made to this resource will be lost.<br>Are you sure you want to restore this resource view?", "Confirm restoring", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
			currentResource = originalResource;
			originalResource = null;
			changesMade = false;
			init();
		}
	}

	@Override
	public JMenuItem[] getContextMenu() {
		init();
		return menu;
	}

	@Override
	public void init() {
		if(menu == null){
			menu = new JMenuItem[5];
			menu[0] = new JMenuItem("Export this resource");
			menu[0].setToolTipText("Export the currently visible resource to a file");
			menu[0].addActionListener((e) -> {
				exportResource(menu[0]);
			});
			menu[1] = new JMenuItem("Replace this resource");
			menu[1].setToolTipText("Replace the currently visible resource with a file");
			menu[1].addActionListener((e) -> {
				replaceCurrentResource(menu[1]);
			});
			menu[2] = new JMenuItem("Restore this resource");
			menu[2].setToolTipText("Restore the currently visible resource to its original state");
			menu[2].addActionListener((e) -> {
				restoreCurrentResource(menu[2]);
			});
			menu[3] = new JMenuItem("$Sep$");
			menu[4] = new JRadioButtonMenuItem("Show Original");
			menu[4].setToolTipText("Replace the currently visible resource with a file");
			((JRadioButtonMenuItem) menu[2]).addChangeListener((e) -> {
				((JRadioButtonMenuItem) menu[2]).setSelected(setShowOriginals(((JRadioButtonMenuItem) menu[2]).isSelected()));
			});
		}
		if(currentResource != null){
			currentResource.init();
			JTabbedPane tabs = new JTabbedPane();
			tabs.add("Resource", currentResource.getComponent());
			display = tabs;
			tabs.setComponentPopupMenu(ResourceView.createPopup(menu));
		}
		if(originalResource != null){
			originalResource.init();
			JTabbedPane tabs = new JTabbedPane();
			tabs.add("Resource", originalResource.getComponent());
			displayOriginal = tabs;
			tabs.setComponentPopupMenu(ResourceView.createPopup(menu));
		}
		if(options == null){
			options = new JPanel();
		}
	}

	@Override
	public void exportResource(Component dialogParent) {
		ResourceView.exportResourceToFile(dialogParent, currentResource);
	}

	@Override
	public void exportResourceView(Component dialogParent) {
		exportResource(dialogParent);
	}
	
	
}
