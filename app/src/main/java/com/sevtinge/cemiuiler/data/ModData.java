package com.sevtinge.cemiuiler.data;

public class ModData {
    public String title;
    public String breadcrumbs;
    public String key;
    public ModCat cat;
    public String sub;
    public int order;
    public String fragment;
    public int catTitleResId;

    public enum ModCat {
        pref_key_system,
        pref_key_launcher,
        pref_key_controls,
        pref_key_various
    }
}
