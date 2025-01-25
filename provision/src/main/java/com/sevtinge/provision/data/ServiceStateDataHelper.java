package com.sevtinge.provision.data;

import android.content.Context;

import com.sevtinge.provision.R;

import java.util.ArrayList;
import java.util.HashMap;

public class ServiceStateDataHelper {

    private Context mContext;

    public ArrayList<ServiceItem> mServiceItems = new ArrayList<>();
    public HashMap<String, Integer> mPrivacyTypeMap = new HashMap<>();

    public ServiceStateDataHelper(Context context) {
        mContext = context;
        buildServicePrivacyTypes();
        buildServiceItems();
    }

    public ArrayList<ServiceItem> getServiceItems() {
        return mServiceItems;
    }

    public HashMap<String, Integer> getPrivacyTypeMap() {
        return mPrivacyTypeMap;
    }

    private void buildServiceItems() {
        mServiceItems.add(buildTermsItems());
    }

    private ServiceItem buildTermsItems() {
        ServiceItem serviceItem = new ServiceItem();
        serviceItem.type = TermsAndStatementAdapter.TYPE_TERMS_ITEM;
        serviceItem.termsName = mContext.getResources().getString(R.string.provision_terms_of_use);
        serviceItem.termsTitle = mContext.getString(R.string.provision_terms_of_use_label_use_network_china);
        serviceItem.termsDescription = mContext.getString(R.string.provision_terms_of_label_period);
        return serviceItem;
    }

    private void buildServicePrivacyTypes() {
        mPrivacyTypeMap.put("fast_pairing", 10002);
        mPrivacyTypeMap.put("lock_screen", 11);
        mPrivacyTypeMap.put("location", 12);
        mPrivacyTypeMap.put("content", 13);
        mPrivacyTypeMap.put("security", 14);
        mPrivacyTypeMap.put("game", 15);
        mPrivacyTypeMap.put("download", 10000);
        mPrivacyTypeMap.put("company", 10001);
        mPrivacyTypeMap.put("fast_application", 10003);
    }

}
