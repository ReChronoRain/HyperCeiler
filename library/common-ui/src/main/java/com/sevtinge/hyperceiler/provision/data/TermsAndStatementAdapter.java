package com.sevtinge.hyperceiler.provision.data;

import android.content.Context;
import android.content.res.Resources;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.provision.text.style.ClickSpan;
import com.sevtinge.hyperceiler.provision.text.style.TermsTitleSpan;
import com.sevtinge.hyperceiler.provision.utils.OobeUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class TermsAndStatementAdapter extends BaseAdapter {

    public static int TYPE_SERVICE_ITEM = 1;
    public static int TYPE_TERMS_ITEM = 2;

    private Context mContext;
    private LayoutInflater mInflater;
    private HashMap<String, Integer> mPrivacyTypeMap;
    private ArrayList<ServiceItem> mServiceItems;

    public TermsAndStatementAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        ServiceStateDataHelper serviceStateDataHelper = new ServiceStateDataHelper(context);
        mServiceItems = serviceStateDataHelper.getServiceItems();
        mPrivacyTypeMap = serviceStateDataHelper.getPrivacyTypeMap();
    }


    @Override
    public int getCount() {
        return mServiceItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mServiceItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return mServiceItems.get(position).type;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        ItemViewHolder itemViewHolder;
        int itemViewType = getItemViewType(position);
        if (convertView == null) {
            if (itemViewType == TYPE_SERVICE_ITEM) {
                convertView = mInflater.inflate(R.layout.provision_item_list_statement, null);
                holder = new ViewHolder();
                holder.onBindViewHolder(convertView);
                convertView.setTag(holder);
                itemViewHolder = null;
            } else {
                convertView = mInflater.inflate(R.layout.provision_item_list_terms, null);
                itemViewHolder = new ItemViewHolder();
                itemViewHolder.onBindViewHolder(convertView);
                convertView.setTag(itemViewHolder);
                holder = null;
            }
        } else {
            if (itemViewType == TYPE_SERVICE_ITEM) {
                holder = (ViewHolder) convertView.getTag();
                itemViewHolder = null;
            } else {
                itemViewHolder = (ItemViewHolder) convertView.getTag();
                holder = null;
            }
        }

        ServiceItem serviceItem = mServiceItems.get(position);
        if (itemViewType == TYPE_SERVICE_ITEM) {
            holder.name.setText(serviceItem.name);
            holder.introduction.setText(serviceItem.introduction);
            String protectStatement = serviceItem.protectStatement;
            if (!TextUtils.isEmpty(protectStatement)) {
                holder.protectStatement.setVisibility(View.VISIBLE);
                holder.protectStatement.setText(protectStatement);
            } else {
                holder.protectStatement.setVisibility(View.GONE);
            }
            String provid = serviceItem.provid;
            if (!TextUtils.isEmpty(provid)) {
                holder.provid.setVisibility(View.VISIBLE);
                holder.provid.setText(provid);
            } else {
                holder.provid.setVisibility(View.GONE);
            }
            ArrayList<PermissionItem> permissionItems = serviceItem.permissionItems;
            if (permissionItems != null && permissionItems.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < permissionItems.size(); i++) {
                    PermissionItem permissionItem = permissionItems.get(i);
                    if (permissionItem != null && !TextUtils.isEmpty(permissionItem.name) && !TextUtils.isEmpty(permissionItem.info)) {
                        sb.append(permissionItem.name);
                        sb.append("\n");
                        sb.append(permissionItem.info);
                        if (i != permissionItems.size() - 1) {
                            sb.append("\n");
                        }
                    }
                }
                holder.permissionInfo.setVisibility(View.VISIBLE);
                holder.permissionInfo.setText(sb.toString());
                holder.permissionInfo.setPaddingRelative(mContext.getResources().getDimensionPixelOffset(R.dimen.provision_service_permission_margin_start), 0, 0, 0);
            } else {
                holder.permissionInfo.setVisibility(View.GONE);
            }
            String str3 = serviceItem.disagreement;
            if (!TextUtils.isEmpty(str3)) {
                holder.userDisagreement.setVisibility(View.VISIBLE);
                holder.userDisagreement.setText(str3);
            } else {
                holder.userDisagreement.setVisibility(View.GONE);
            }
            if (serviceItem.policyExist) {
                holder.policy.setVisibility(View.VISIBLE);
                if (serviceItem.name.equals(mContext.getString(R.string.provision_service_name_download))) {
                    holder.policy.setText(Html.fromHtml(mContext.getString(R.string.provision_service_policy_download)));
                    holder.policy.setMovementMethod(LinkMovementMethod.getInstance());
                    CharSequence text = holder.policy.getText();
                    if (text instanceof Spannable) {
                        int length = text.length();
                        Spannable spannable = (Spannable) holder.policy.getText();
                        URLSpan[] uRLSpanArr = spannable.getSpans(0, length, URLSpan.class);
                        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(text);
                        spannableStringBuilder.clearSpans();
                        for (URLSpan uRLSpan : uRLSpanArr) {
                            spannableStringBuilder.setSpan(new ClickSpan(mContext, mPrivacyTypeMap), spannable.getSpanStart(uRLSpan), spannable.getSpanEnd(uRLSpan), 33);
                        }
                        holder.policy.setText(spannableStringBuilder);
                    }
                } else {
                    holder.policy.setText(Html.fromHtml(serviceItem.privacyPolicy, 63));
                    holder.policy.setMovementMethod(LinkMovementMethod.getInstance());
                    holder.policy.setOnClickListener(v -> {
                        OobeUtils.startActivity(mContext, OobeUtils.getLicenseIntent("https://limestart.cn/"));
                    });
                }
            } else {
                holder.policy.setVisibility(View.GONE);
            }
            if (position == getCount() - 1) {
                holder.agree.setVisibility(View.GONE);
            } else {
                holder.agree.setVisibility(View.GONE);
            }
        } else {
            itemViewHolder.termsName.setText(serviceItem.termsName);
            if (enhanceTermsTitle(serviceItem.termsTitle) != null) {
                itemViewHolder.termsTitle.setText(enhanceTermsTitle(serviceItem.termsTitle));
                itemViewHolder.termsTitle.setMovementMethod(fan.androidbase.widget.LinkMovementMethod.getInstance());
            }
            itemViewHolder.termsDescription.setText(serviceItem.termsDescription);
        }
        return convertView;
    }

    public SpannableStringBuilder enhanceTermsTitle(String str) {
        Resources res = mContext.getResources();
        String userAgreement = res.getString(R.string.provision_user_agreement);
        String privacyPolicy = res.getString(R.string.provision_privacy_policy);
        String spanned = Html.fromHtml(str).toString();
        int lastIndexOf = spanned.lastIndexOf(userAgreement);
        int length = userAgreement.length() + lastIndexOf;
        if (lastIndexOf >= 0 && length <= spanned.length()) {
            int indexOf = spanned.indexOf(privacyPolicy);
            int length2 = privacyPolicy.length() + indexOf;
            if (indexOf >= 0 && length2 <= spanned.length()) {
                SpannableStringBuilder builder = new SpannableStringBuilder(spanned);
                int color = res.getColor(R.color.provision_button_text_high_color_light);
                builder.setSpan(new ForegroundColorSpan(color), lastIndexOf, length, 33);
                builder.setSpan(new ForegroundColorSpan(color), indexOf, length2, 33);
                builder.setSpan(new TermsTitleSpan(mContext, 2), lastIndexOf, length, 33);
                builder.setSpan(new TermsTitleSpan(mContext, 1), indexOf, length2, 33);
                return builder;
            }
        }
        return null;
    }


    private class ItemViewHolder {
        TextView termsDescription;
        TextView termsName;
        TextView termsTitle;

        public void onBindViewHolder(View itemView) {
            termsName = itemView.findViewById(R.id.terms_name);
            termsTitle = itemView.findViewById(R.id.terms_title);
            termsDescription = itemView.findViewById(R.id.list_terms_description);
        }
    }

    private class ViewHolder {
        TextView agree;
        TextView introduction;
        TextView name;
        TextView permissionInfo;
        TextView policy;
        TextView protectStatement;
        TextView provid;
        TextView userDisagreement;

        public void onBindViewHolder(View itemView) {
            name = itemView.findViewById(R.id.service_name);
            introduction = itemView.findViewById(R.id.service_introduction);
            protectStatement = itemView.findViewById(R.id.protect_statement);
            provid = itemView.findViewById(R.id.provid);
            permissionInfo = itemView.findViewById(R.id.service_permission_info);
            userDisagreement = itemView.findViewById(R.id.user_disagreement);
            policy = itemView.findViewById(R.id.service_policy);
            agree = itemView.findViewById(R.id.agree_text);
        }
    }
}
