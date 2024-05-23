package com.sevtinge.hyperceiler.ui.page.utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Xml;

import androidx.annotation.XmlRes;

import com.android.internal.graphics.util.XmlUtils;
import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.settings.adapter.PreferenceHeader;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

public class HeaderUtils {
    public static void loadHeadersFromResource(Context context, @XmlRes int resId, List<PreferenceHeader> list) {
        List<PreferenceHeader> headers;
        XmlResourceParser xmlResourceParser = null;
        try {
            XmlResourceParser xml = context.getResources().getXml(resId);
            try {
                AttributeSet attrs = Xml.asAttributeSet(xml);
                while (true) {
                    int next = xml.next();
                    if (next == 1 || next == 2) {
                        break;
                    }
                }
                String name = xml.getName();
                if ("preference-headers".equals(name)) {
                    int depth = xml.getDepth();
                    Bundle bundle = null;
                    while (true) {
                        int next2 = xml.next();
                        if (next2 == 1 || (next2 == 3 && xml.getDepth() <= depth)) {
                            break;
                        } else if (next2 != 3 && next2 != 4) {
                            if ("header".equals(xml.getName())) {
                                PreferenceHeader header = new PreferenceHeader();
                                TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PreferenceHeader);
                                header.id  = a.getResourceId(R.styleable.PreferenceHeader_android_id, -1);
                                header.iconRes = a.getResourceId(R.styleable.PreferenceHeader_android_icon, 0);
                                header.fragment = a.getString(R.styleable.PreferenceHeader_android_fragment);
                                int titleResId = a.getResourceId(R.styleable.PreferenceHeader_android_title, 0);
                                if (titleResId != 0) {
                                    header.titleRes = titleResId;
                                } else {
                                    header.title = a.getString(R.styleable.PreferenceHeader_android_title);
                                }

                                int summaryResId = a.getResourceId(R.styleable.PreferenceHeader_android_summary, 0);
                                if (summaryResId != 0) {
                                    header.summaryRes = summaryResId;
                                } else {
                                    header.summary = a.getString(R.styleable.PreferenceHeader_android_summary);
                                }

                                int breadCrumbResId = a.getResourceId(R.styleable.PreferenceHeader_android_breadCrumbTitle, 0);
                                if (breadCrumbResId != 0) {
                                    header.breadCrumbTitleRes = breadCrumbResId;
                                } else {
                                    header.breadCrumbTitle = a.getString(R.styleable.PreferenceHeader_android_breadCrumbTitle);
                                }

                                int breadCrumbShortResId = a.getResourceId(R.styleable.PreferenceHeader_android_breadCrumbShortTitle, 0);
                                if (breadCrumbShortResId != 0) {
                                    header.breadCrumbShortTitleRes = breadCrumbShortResId;
                                } else {
                                    header.breadCrumbShortTitle = a.getString(R.styleable.PreferenceHeader_android_breadCrumbShortTitle);
                                }
                                a.recycle();
                                if (bundle == null) {
                                    bundle = new Bundle();
                                }
                                int depth2 = xml.getDepth();
                                while (true) {
                                    int next3 = xml.next();
                                    if (next3 == 1 || (next3 == 3 && xml.getDepth() <= depth2)) {
                                        break;
                                    } else if (next3 != 3 && next3 != 4) {
                                        String name2 = xml.getName();
                                        if (name2.equals("extra")) {
                                            context.getResources().parseBundleExtra("extra", attrs, bundle);
                                            //XmlUtils.skipCurrentTag(xml);
                                        } else if (name2.equals("intent")) {
                                            header.intent = Intent.parseIntent(context.getResources(), xml, attrs);
                                        } else {
                                            XmlUtils.skipCurrentTag(xml);
                                        }
                                    }
                                }
                                if (bundle.size() > 0) {
                                    header.fragmentArguments = bundle;
                                    headers = list;
                                    bundle = null;
                                } else {
                                    headers = list;
                                }
                                headers.add(header);
                            } else {
                                XmlUtils.skipCurrentTag(xml);
                            }
                        }
                    }
                    xml.close();
                } else {
                    throw new RuntimeException("XML document must start with <preference-headers> tag; found" + name + " at " + xml.getPositionDescription());
                }
            } catch (IOException | XmlPullParserException e) {
                throw new RuntimeException("Error parsing headers", e);
            } catch (Throwable th) {
                xmlResourceParser = xml;
                xmlResourceParser.close();
                throw th;
            }
        } catch (Throwable th2) {
            throw new RuntimeException("Error parsing headers", th2);
        }
    }
}