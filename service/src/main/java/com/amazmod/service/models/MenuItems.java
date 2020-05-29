package com.amazmod.service.models;


public class MenuItems {

	public static final int ACTION_UNDEFINED = 0;
	public static final int ACTION_ACTIVITY = 1;
	public static final int ACTION_COMMAND = 2;
	public static final int ACTION_COMMAND_DELAY = 3;
	public static final int ACTION_WEAR_ACTIVITY = 4;
	public static final int ACTION_CUSTOM = 5;
	public static final int ACTION_CUSTOM_DELAY = 6;
	public static final int ACTION_TOGGLE = 7;


	private int iconOn;
	private int iconOff;
	private String title;
	private int actionType;
	private String action;
	private boolean state;

	public MenuItems(String title, int icon) {
		this(title,icon,true);
	}

	public MenuItems(String title, int icon, boolean state) {
		this(title,icon,icon,state);
	}

	public MenuItems(String title, int iconOn, int iconOff, boolean state) {
		this.iconOn = iconOn;
		this.iconOff = iconOff;
		this.title = title;
		this.state = state;
		this.actionType = ACTION_UNDEFINED;
	}

	public int getIcon(){
		if (state){
			return iconOn;
		}else{
			return iconOff;
		}
	}


	public int getIconOn() {
		return iconOn;
	}

	public void setIconOn(int iconOn) {
		this.iconOn = iconOn;
	}

	public int getIconOff() {
		return iconOff;
	}

	public void setIconOff(int iconOff) {
		this.iconOff = iconOff;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getActionType() {
		return actionType;
	}

	public void setActionType(int actionType) {
		this.actionType = actionType;
	}

	public String getAction() {
		return action;
	}


	public void setActionWearActivity(char action ){
		this.setActionType(ACTION_WEAR_ACTIVITY);
		this.setAction(Character.toString(action));
	}

	public void setActionActivity(String className ){
		this.setActionType(ACTION_ACTIVITY);
		this.setAction(className);
	}

	public void setActionCommand(String command){
		this.setActionType(ACTION_COMMAND);
		this.setAction(command);
	}

	public void setActionCommandDelay(String command){
		this.setActionType(ACTION_COMMAND_DELAY);
		this.setAction(command);
	}

	public void setActionToggle(String toggle){
		this.setActionType(ACTION_TOGGLE);
		this.setAction(toggle);
	}

	public void setActionCustom(String id){
		this.setActionType(ACTION_CUSTOM);
		this.setAction(id);
	}

	public void setActionCustomDelay(String id){
		this.setActionType(ACTION_CUSTOM_DELAY);
		this.setAction(id);
	}

	public char getActionWearActivity(){
		return this.getAction().toCharArray()[0];
	}

	public void setAction(String action) {
		this.action = action;
	}

	public boolean getState() {
		return state;
	}

	public void setState(boolean state) {
		this.state = state;
	}

	public boolean executeCustomFunction(){
		return this.getActionType() == ACTION_CUSTOM;
	}
}