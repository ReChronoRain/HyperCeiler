/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.hooker.home;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.HomeRecentsHapticOS4;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import fan.appcompat.app.AlertDialog;

/** Browse without changing preferences; only the explicit Use action saves. */
final class HomeHapticEffectsDialog {
    private HomeHapticEffectsDialog() { }

    static String name(Context context,int id) {
        var firmware=HomeHapticEffectCatalog.find(context,id);if(firmware!=null)return firmware.name();
        // Android HAL Effect IDs, not UI feedback constants or composition primitives:
        // https://android.googlesource.com/platform/hardware/interfaces/+/refs/heads/main/vibrator/aidl/android/hardware/vibrator/Effect.aidl
        int resource=switch(id) {
            case 0 -> R.string.home_recent_haptic_effect_click;
            case 1 -> R.string.home_recent_haptic_effect_double_click;
            case 2 -> R.string.home_recent_haptic_effect_tick;
            case 3 -> R.string.home_recent_haptic_effect_thud;
            case 4 -> R.string.home_recent_haptic_effect_pop;
            case 5 -> R.string.home_recent_haptic_effect_heavy_click;
            case 21 -> R.string.home_recent_haptic_effect_texture_tick;
            default -> 0;
        };
        if(resource!=0)return context.getString(resource);
        if(id>=6&&id<=20)return context.getString(R.string.home_recent_haptic_effect_ringtone,id-5);
        String systemName=HomeRecentsHapticOS4.effectName(id);
        return systemName!=null&&!systemName.toUpperCase(Locale.ROOT).contains("UNKNOWN")?systemName
            :context.getString(R.string.home_recent_haptic_effect_unnamed);
    }

    record Selection(boolean custom,int value) { }

    static String summary(Context context,Selection selected) {
        if(!selected.custom())return context.getString(R.string.home_recent_haptic_selected_preset,
            context.getResources().getStringArray(R.array.home_recent_card_haptic_types)[selected.value()]);
        return context.getString(R.string.home_recent_haptic_selected_effect,name(context,selected.value()),selected.value());
    }

    private record Effect(Selection selection,String name,int group) {
        String detail(Context context){return selection.custom()?HomeHapticEffectCatalog.groupName(context,group)+"  ID "+selection.value():context.getString(R.string.home_recent_haptic_preset);}
        boolean matches(Context context,String query){return (name+" "+detail(context)).toLowerCase(Locale.ROOT).contains(query);}
    }

    static void show(Context context,Selection current,Consumer<Selection> preview,Consumer<Selection> apply) {
        // Use the dialog theme for every child, rather than the activity theme.
        var builder=new AlertDialog.Builder(context);
        Context themed=builder.getContext();
        var header=new LinearLayout(themed);header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(0,0,0,dp(themed,8));
        var title=new TextView(themed);title.setText(R.string.home_recent_haptic_picker_title);
        title.setTextSize(20);title.setGravity(Gravity.CENTER);textColor(title,fan.appcompat.R.attr.dialogTitleTextColor);
        title.setPadding(0,0,0,dp(themed,16));header.addView(title,new LinearLayout.LayoutParams(-1,-2));
        var search=new EditText(themed);search.setSingleLine(true);search.setTextSize(15);
        search.setMinHeight(dp(themed,48));search.setMinimumHeight(dp(themed,48));
        search.setPaddingRelative(dp(themed,16),dp(themed,10),dp(themed,16),dp(themed,10));
        search.setHint(R.string.home_recent_haptic_search);
        textColor(search,fan.appcompat.R.attr.editTextColor);
        var searchColors=themed.obtainStyledAttributes(new int[]{fan.appcompat.R.attr.editTextColorHint});
        try {var color=searchColors.getColorStateList(0);if(color!=null)search.setHintTextColor(color);}
        finally {searchColors.recycle();}
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        search.setOnEditorActionListener((view,action,event)->{
            if(action!=android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH)return false;
            var keyboard=themed.getSystemService(android.view.inputmethod.InputMethodManager.class);
            if(keyboard!=null)keyboard.hideSoftInputFromWindow(search.getWindowToken(),0);
            return true;
        });
        header.addView(search,new LinearLayout.LayoutParams(-1,-2));
        var empty=new TextView(themed);empty.setGravity(Gravity.CENTER);
        textColor(empty,fan.appcompat.R.attr.dialogCommentTextColor);
        empty.setText(R.string.home_recent_haptic_loading);empty.setPadding(0,dp(themed,12),0,dp(themed,12));
        header.addView(empty,new LinearLayout.LayoutParams(-1,-2));
        var adapter=new EffectsAdapter(themed,current);
        var presets=themed.getResources().getStringArray(R.array.home_recent_card_haptic_types);
        for(int i=0;i<presets.length;++i)adapter.all.add(new Effect(new Selection(false,i),presets[i],-1));
        adapter.filter("");
        boolean[] loaded={false};
        Runnable updateEmpty=()->{
            empty.setText(loaded[0]?R.string.home_recent_haptic_empty:R.string.home_recent_haptic_loading);
            empty.setVisibility(!loaded[0]||adapter.getCount()==0?View.VISIBLE:View.GONE);
        };
        // The native dialog list owns scrolling. A ListView nested inside setView
        // gets wrapped by the dialog's scrolling container and loses drag gestures.
        var dialog=builder.setCustomTitle(header).setAdapter(adapter,null)
            .setPositiveButton(R.string.home_recent_haptic_use,null)
            .setNegativeButton(android.R.string.cancel,null).create();
        // This searchable picker needs a stable top edge. The immersive bottom
        // dialog translates its whole panel above the IME and moves it again
        // whenever filtering changes the list height. Let the window resize
        // the native list instead, keeping the title and editor in place.
        dialog.setEnableImmersive(false);
        dialog.setOnShowListener(ignored->{
            View body=dialog.findViewById(fan.appcompat.R.id.parentPanel);
            if(body!=null&&body.getLayoutParams() instanceof android.widget.FrameLayout.LayoutParams params){
                params.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;
                body.setLayoutParams(params);
            }
            // Custom titles inherit the library's large title-panel side padding.
            // Use ordinary field gutters so the search fills the dialog body.
            if(header.getParent() instanceof View panel){
                panel.setPaddingRelative(dp(themed,16),panel.getPaddingTop(),dp(themed,16),dp(themed,8));
            }
            ListView list=dialog.getListView();list.setDivider(null);
            var use=dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);use.setEnabled(adapter.selectionVisible());
            use.setOnClickListener(view->{if(!adapter.selectionVisible())return;apply.accept(adapter.selected);dialog.dismiss();});
            list.setOnItemClickListener((parent,view,position,id)->{
                Effect chosen=adapter.getItem(position).effect();if(chosen==null)return;
                adapter.selected=chosen.selection();adapter.notifyDataSetChanged();
                use.setEnabled(true);preview.accept(adapter.selected);
            });
            new Thread(()->{
                HomeHapticEffectCatalog.load();
                var inventory=HomeRecentsHapticOS4.effectIds(context.getApplicationContext());
                var effects=new ArrayList<Effect>();
                for(int id:inventory.ids().keySet())effects.add(deviceEffect(themed,new Selection(true,id)));
                if(current.custom()&&!inventory.ids().containsKey(current.value()))
                    effects.add(deviceEffect(themed,current));
                effects.sort(java.util.Comparator.comparingInt(Effect::group).thenComparingInt(effect->effect.selection().value()));
                header.post(()->{
                    if(!dialog.isShowing()||(context instanceof android.app.Activity activity&&activity.isDestroyed()))return;
                    loaded[0]=true;adapter.all.addAll(effects);adapter.filter(search.getText().toString());updateEmpty.run();
                    use.setEnabled(adapter.selectionVisible());
                    if(search.getText().length()==0)for(int i=0;i<adapter.visible.size();++i)
                        if(adapter.visible.get(i).effect()!=null&&adapter.visible.get(i).effect().selection().equals(current)){
                            if(current.custom())list.setSelection(Math.max(0,i-1));break;
                        }
                });
            },"HomeHapticEffects").start();
        });
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count){
                adapter.filter(s.toString());updateEmpty.run();
                var use=dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
                if(use!=null)use.setEnabled(adapter.selectionVisible());
                var list=dialog.getListView();if(list!=null)list.setSelection(0);
            }
            public void afterTextChanged(Editable s) { }
        });
        header.setFocusableInTouchMode(true);header.requestFocus();
        dialog.show();
        dialog.getWindow().setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        // The library's bottom-dialog margin is a fraction of window height;
        // retaining it on a top-anchored window makes the editor drift on IME
        // resize and on each filtered content-height change.
        var windowParams=dialog.getWindow().getAttributes();
        windowParams.verticalMargin=0;windowParams.y=0;
        dialog.getWindow().setAttributes(windowParams);
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            |android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        // AlertController only checks setView for editors; ours is in the title.
        dialog.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    }

    private static void textColor(TextView view,int attribute){
        var colors=view.getContext().obtainStyledAttributes(new int[]{attribute});
        try {var color=colors.getColorStateList(0);if(color!=null)view.setTextColor(color);}
        finally {colors.recycle();}
    }

    private static Effect deviceEffect(Context context,Selection selected){
        var label=HomeHapticEffectCatalog.find(context,selected.value());
        int group=label!=null?label.group():selected.value()<=5||selected.value()==21?1:selected.value()<=20?2:4;
        return new Effect(selected,name(context,selected.value()),group);
    }

    private static int dp(Context context,int value){return Math.round(value*context.getResources().getDisplayMetrics().density);}

    private record Row(String heading,Effect effect,int group) { }

    private static final class EffectsAdapter extends BaseAdapter {
        final Context context;
        final List<Effect> all=new ArrayList<>();
        final List<Row> visible=new ArrayList<>();
        Selection selected;
        EffectsAdapter(Context context,Selection selected){this.context=context;this.selected=selected;}
        void filter(String text){
            String query=text.trim().toLowerCase(Locale.ROOT);visible.clear();int group=Integer.MIN_VALUE;
            for(var effect:all){
                if(!effect.matches(context,query))continue;
                if(group!=effect.group()){
                    group=effect.group();
                    String label=group<0?context.getString(R.string.home_recent_haptic_presets_group)
                        :HomeHapticEffectCatalog.groupName(context,group);
                    visible.add(new Row(label,null,group));
                }
                visible.add(new Row(null,effect,group));
            }
            notifyDataSetChanged();
        }
        public int getCount(){return visible.size();}
        boolean selectionVisible(){return visible.stream().anyMatch(row->row.effect()!=null&&row.effect().selection().equals(selected));}
        public Row getItem(int position){return visible.get(position);}
        public long getItemId(int position){
            Row row=getItem(position);if(row.effect()==null)return Long.MIN_VALUE+100+row.group();
            var effect=row.effect().selection();return effect.custom()?effect.value():-1L-effect.value();
        }
        public boolean hasStableIds(){return true;}
        public boolean areAllItemsEnabled(){return false;}
        public boolean isEnabled(int position){return getItem(position).effect()!=null;}
        public int getViewTypeCount(){return 2;}
        public int getItemViewType(int position){return getItem(position).effect()==null?0:1;}
        public View getView(int position,View recycled,ViewGroup parent){
            Row item=getItem(position);
            if(item.effect()==null){
                TextView heading=recycled instanceof TextView prior?prior:new TextView(context);
                heading.setText(item.heading());heading.setTextSize(13);
                heading.setTypeface(null,Typeface.BOLD);textColor(heading,fan.appcompat.R.attr.textColorListSecondary);
                heading.setTextColor(heading.getTextColors().getDefaultColor());
                heading.setPadding(dp(context,16),dp(context,position==0?8:16),dp(context,16),dp(context,6));
                heading.setAccessibilityHeading(true);
                return heading;
            }
            LinearLayout row;
            if(recycled instanceof LinearLayout existing)row=existing;
            else {
                row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);
                var text=new LinearLayout(context);text.setOrientation(LinearLayout.VERTICAL);
                var title=new TextView(context);title.setTextSize(16);title.setMaxLines(2);
                title.setTypeface(null,Typeface.NORMAL);textColor(title,fan.appcompat.R.attr.textColorDialogListItem);
                var detail=new TextView(context);detail.setTextSize(12);
                textColor(detail,fan.appcompat.R.attr.textColorListSecondary);detail.setPadding(0,dp(context,3),0,0);
                text.addView(title);text.addView(detail);row.addView(text,new LinearLayout.LayoutParams(0,-2,1));
                var radio=new RadioButton(context);radio.setClickable(false);radio.setFocusable(false);
                radio.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);row.addView(radio);
            }
            Effect effect=item.effect();boolean custom=effect.selection().custom();
            row.setPadding(dp(context,16),dp(context,10),dp(context,16),dp(context,10));
            row.setMinimumHeight(dp(context,custom?60:48));
            var text=(LinearLayout)row.getChildAt(0);
            ((TextView)text.getChildAt(0)).setText(effect.name());
            TextView detail=(TextView)text.getChildAt(1);detail.setVisibility(custom?View.VISIBLE:View.GONE);
            detail.setText(custom?"ID "+effect.selection().value():"");
            boolean checked=effect.selection().equals(selected);
            ((RadioButton)row.getChildAt(1)).setChecked(checked);row.setSelected(checked);
            row.setAccessibilityDelegate(new View.AccessibilityDelegate(){
                @Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){
                    super.onInitializeAccessibilityNodeInfo(host,info);info.setCheckable(true);info.setChecked(checked);
                }
            });
            return row;
        }
    }
}
