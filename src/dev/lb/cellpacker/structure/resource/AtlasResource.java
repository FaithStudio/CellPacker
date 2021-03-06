package dev.lb.cellpacker.structure.resource;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import dev.lb.cellpacker.Utils;
import dev.lb.cellpacker.annotation.Async;
import dev.lb.cellpacker.annotation.Unmodifiable;
import dev.lb.cellpacker.controls.JSpriteViewer;
import dev.lb.cellpacker.controls.SpriteSavingList;

public class AtlasResource extends Resource{

	private String hexString;
	private JTextArea textDisplay;
	private AtlasData atlasData;
	
	public AtlasResource(String name, String path, int magic, byte[] data) {
		super(name, path, magic, data);
	}
	
	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToString(byte[] bytes){
		char[] chars = new char[3*bytes.length];
		for(int i = 0; i < bytes.length; i++){
			int data = bytes[i] & 0xFF;
			chars[i * 3] = hexArray[data >>> 4];
			chars[i * 3 + 1] = hexArray[data & 0x0F];
			chars[i * 3 + 2] = 0x20;
		}
		return new String(chars);
	}
	
	@Async
	private void init(){
		if(isInitialized)
			return;
		hexString = bytesToString(data);
		atlasData = new AtlasData(data);
		isInitialized = true;
	}
	
	@Override
	public Component getComponent() {
		if(!isInitialized){
			return Utils.asyncFill(() -> {
				init();
				textDisplay = new JTextArea(hexString);
				textDisplay.setLineWrap(true);
				textDisplay.setWrapStyleWord(true);
				textDisplay.setEditable(false);
				return new JScrollPane(textDisplay);
			}, 300);
		}else{
			textDisplay = new JTextArea(hexString);
			textDisplay.setLineWrap(true);
			textDisplay.setWrapStyleWord(true);
			textDisplay.setEditable(false);
			return new JScrollPane(textDisplay);
		}
	}

	@Async
	@Override
	public Object getContent() {
		if(!isInitialized)
			init();
		return hexString;
	}

	@Override
	public Resource clone() {
		return new AtlasResource(getName(), getPath(), getMagicNumber(), getData());
	}

	@Override
	public FileFilter getFileFilter() {
		return new FileNameExtensionFilter("Atlas File", "*.atlas", ".atlas", "atlas");
	}

	public Component createSpriteView(ImageResource main, ImageResource filter, JTabbedPane tabs) {
		if(!isInitialized){
			return Utils.asyncFill(() -> {
				init();
				return atlasData.createView(main.getImage(), filter == null ? null : filter.getImage(), tabs);
			}, 300);
		}else{
			return atlasData.createView(main.getImage(), filter == null ? null : filter.getImage(), tabs);
		}
	}
	
	@Async
	public AtlasData getAtlasData(){
		if(!isInitialized)
			init();
		return atlasData;
	}
	
	public static class AtlasData{
		private List<Sprite> sprites;
		//private JSpriteViewer currentSprite;
		//private SpriteSavingList currentList;
		
		public Component createView(BufferedImage main, BufferedImage filter, JTabbedPane tabs){
			JPanel con = new JPanel(new BorderLayout());
			SpriteSavingList currentList = new SpriteSavingList(new DefaultListModel<>());
			sprites.forEach((s) -> ((DefaultListModel<Sprite>) currentList.getModel()).addElement(s));
			currentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			JScrollPane listScroll = new JScrollPane(currentList);
			listScroll.setBorder(new CompoundBorder(new EmptyBorder(10, 10, 10, 10), listScroll.getBorder()));
			JPanel westCon = new JPanel(new BorderLayout());
			westCon.add(Utils.call(new JLabel("Select sprite:"), (c) -> c.setBorder(new EmptyBorder(10, 10, 0, 10))), BorderLayout.NORTH); //Overly complicated, but fun
			westCon.add(listScroll, BorderLayout.CENTER);
			con.add(westCon, BorderLayout.WEST);
			JPanel centerCon = new JPanel(new BorderLayout());
			JSpriteViewer currentSprite = new JSpriteViewer(main, filter);
			JTextArea details = new JTextArea();
			details.setBorder(new EmptyBorder(10, 20, 10, 20));
			currentList.setDetailsArea(details);
			JPanel southCon = new JPanel(new BorderLayout());
			southCon.add(details, BorderLayout.CENTER);
			JPanel options = new JPanel(new GridLayout(2, 2));
			// OPTIONS
			JRadioButton jrbMain = new JRadioButton("Show main image", true);
			JRadioButton jrbFilt = new JRadioButton("Show Filter image", false);
			if(filter == null) jrbFilt.setEnabled(false);
			ButtonGroup group = new ButtonGroup();
			group.add(jrbMain);
			group.add(jrbFilt);
			currentList.setRadioButtons(jrbMain, jrbFilt);
			
			JCheckBox highlight = new JCheckBox("Highlight in image", false);
			JButton animation = new JButton("Animations");
			currentList.setHighlightButton(highlight, animation);
			options.add(jrbMain);
			options.add(jrbFilt);
			options.add(highlight);
			options.add(animation);
			options.setBorder(new EmptyBorder(10, 10, 10, 10));
			// /OPTIONS
			southCon.add(options, BorderLayout.EAST);
			centerCon.add(southCon, BorderLayout.SOUTH);
			JPanel jspBorder = new JPanel(new BorderLayout());
			jspBorder.setBorder(new EmptyBorder(20, 20, 20, 20));
			jspBorder.add(currentSprite, BorderLayout.CENTER);
			centerCon.add(jspBorder, BorderLayout.CENTER);
			con.add(centerCon, BorderLayout.CENTER);
			
			currentList.setJSP(currentSprite);
			if(currentList.getModel().getSize() > 0) currentList.setSelectedIndex(0);
			
			return con;
		}
		
		public AtlasData(byte[] data){
			sprites = new ArrayList<>();
			int pointer = 4;
			int filenamelen = data[4] & 0xFF;
			String filename = new String(Arrays.copyOfRange(data, pointer + 1, pointer + filenamelen + 1));
			pointer = pointer + filenamelen + 1;
			
			do{
				//Beginning sprite
				int strlen = data[pointer] & 0xFF;
				String name = new String(Arrays.copyOfRange(data, pointer + 1, pointer + strlen + 1));
				pointer = pointer + strlen + 3; //First data byte
				byte[] spriteData = Arrays.copyOfRange(data, pointer, pointer + 16);
				Sprite current = new Sprite(name, decodeByte2(spriteData, 0), decodeByte2(spriteData, 2), decodeByte2(spriteData, 4),
						decodeByte2(spriteData, 6), decodeByte2(spriteData, 8), decodeByte2(spriteData, 10),
						decodeByte2(spriteData, 12), decodeByte2(spriteData, 14));
				sprites.add(current);
				pointer += 16;
			}while(pointer < data.length - 2); //The -2 is important
			System.out.println("Read " + sprites.size() + " Sprites for file " + filename);
		}
		
		public static int decodeByte2(byte[] data, int off1){
			return (data[off1] & 0xFF) + ((data[off1 + 1] & 0xFF) << 8); 
		}
		
		@Unmodifiable
		public List<Sprite> getSprites(){
			return Collections.unmodifiableList(sprites);
		}
	}
	
	
	public static class Sprite{
		private String name;
		private int x;
		private int y;
		private int width;
		private int height;
		private int offsetX;
		private int offsetY;
		private int origX;
		private int origY;
		
		private Sprite(String name, int x, int y, int width, int height, int offsetX, int offsetY, int origX, int origY) {
			this.name = name;
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.origX = origX;
			this.origY = origY;
		}
		
		public Point getPosition(){
			return new Point(x, y);
		}
		
		public Dimension getSize(){
			return new Dimension(width, height);
		}
		
		public Rectangle getArea(){
			return new Rectangle(x, y, width, height);
		}
		
		public BufferedImage getImageSection(BufferedImage main){
			return main.getSubimage(x, y, width, height);
		}

		public String getName(){
			return name;
		}
		
		public int getX() {
			return x;
		}

		public int getY() {
			return y;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public int getOffsetX() {
			return offsetX;
		}

		public int getOffsetY() {
			return offsetY;
		}

		public int getOrigX() {
			return origX;
		}

		public int getOrigY() {
			return origY;
		}

		@Override
		public String toString() {
			return name;
		}
		
	}

}
