package com.sevtinge.cemiuiler.utils;

public class ModData {

	public enum ModCat {
		prefs_key_home,
		prefs_key_security_center,
		prefs_key_various
	}

	public String title;
	public String breadcrumbs;
	public String key;
	public ModCat cat;
	public String sub;
	public int order;
}
