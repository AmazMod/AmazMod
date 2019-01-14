package com.amazmod.service.models;


public class MenuItems {

	public MenuItems(int iconResOn, int iconResOff, String title, boolean state) {
		this.iconResOn = iconResOn;
		this.iconResOff = iconResOff;
		this.title = title;
		this.state = state;
	}
	public int iconResOn;
	public int iconResOff;
	public String title;
	public boolean state;

}