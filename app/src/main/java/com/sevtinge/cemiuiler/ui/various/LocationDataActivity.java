package com.sevtinge.cemiuiler.ui.various;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.data.LocationData;
import com.sevtinge.cemiuiler.data.SQLiteHelper;
import com.sevtinge.cemiuiler.utils.ToastHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import moralnorm.appcompat.app.AlertDialog;
import moralnorm.appcompat.app.AppCompatActivity;

public class LocationDataActivity extends AppCompatActivity implements View.OnClickListener {

    private Cursor mCursor;
    private ListView mLocationListView;
    private LocationAdapter mAdapter;
    private SQLiteHelper mSQLiteHelper;
    private ArrayList<LocationData> mLocationDataList = new ArrayList<>();

    Button mAddLocation;


    EditText mTitle;
    EditText mOffset;
    EditText mBaseStation;
    EditText mLongitudeAndLatitude;
    EditText mRemarks;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_location);

        mAddLocation = findViewById(R.id.location_add);
        mAddLocation.setOnClickListener(this);

        mSQLiteHelper = new SQLiteHelper(this);
        mAdapter = new LocationAdapter(this, mLocationDataList);
        mLocationListView = findViewById(R.id.list_location);
        mLocationListView.setAdapter(mAdapter);
        mCursor = mSQLiteHelper.a.rawQuery("select * from location order by id desc limit 2000", null);
        if (mCursor.moveToFirst()) {
            do {
                LocationData locationData = new LocationData(String.valueOf(mCursor.getString(0)), mCursor.getDouble(1), mCursor.getDouble(2), mCursor.getInt(3), mCursor.getInt(4), mCursor.getInt(5), mCursor.getString(6), mCursor.getInt(7));
                mLocationDataList.add(locationData);
            } while (mCursor.moveToNext());
        }
        mCursor.close();

        registerForContextMenu(mLocationListView);

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.list_location) {
            menu.setHeaderTitle(String.valueOf(mLocationDataList.get(((AdapterView.AdapterContextMenuInfo) menuInfo).position).getRemarks()));
            getMenuInflater().inflate(R.menu.menu_location, menu);
        } else {
            super.onCreateContextMenu(menu, v, menuInfo);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mAddLocation) {
            View view = LayoutInflater.from(this).inflate(R.layout.location_edit_dialog, null);
            initEditView(view);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("新增");
            builder.setView(view);
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {

                String title = mTitle.getText().toString();
                String offset = mOffset.getText().toString();
                String baseStation = mBaseStation.getText().toString();
                String longitudeAndLatitude = mLongitudeAndLatitude.getText().toString();
                String remarks = mRemarks.getText().toString();

                if (TextUtils.isEmpty(title) ||
                    TextUtils.isEmpty(offset) ||
                    TextUtils.isEmpty(baseStation) ||
                    TextUtils.isEmpty(longitudeAndLatitude) ||
                    TextUtils.isEmpty(remarks)) {

                    ToastHelper.makeText(this, "格式错误");
                } else {

                    String[] split = baseStation.split(",", 2);
                    String[] split2 = longitudeAndLatitude.split(",", 2);

                    LocationData mData = new LocationData(title,
                        Double.parseDouble(split2[0]),
                        Double.parseDouble(split2[1]),
                        Integer.parseInt(offset),
                        Integer.parseInt(split[0]),
                        Integer.parseInt(split[1]),
                        remarks,
                        1);

                    /*mData.setTitle(title);
                    mData.setLongitude(Double.parseDouble(split2[0]));
                    mData.setLatitude(Double.parseDouble(split2[1]));
                    mData.setOffset(Integer.parseInt(offset));
                    mData.setRegionCode(Integer.parseInt(split[0]));
                    mData.setBaseStationCode(Integer.parseInt(split[1]));
                    mData.setRemarks(remarks);
                    mData.setF(1);*/

                    long b2 = mSQLiteHelper.b(mData);
                    if (b2 < 0) {
                        ToastHelper.makeText(this, "Can't insert");
                        return;
                    }
                    mData.setF((int) b2);
                    mLocationDataList.add(0, mData);
                    mAdapter.notifyDataSetChanged();
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null).show();
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        final AdapterView.AdapterContextMenuInfo mMenuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final LocationData mDataPosition = mLocationDataList.get(mMenuInfo.position);

        if (item.getItemId() == R.id.location_add) {
            final EditText editText = new EditText(this);
            View view = LayoutInflater.from(this).inflate(R.layout.location_edit_dialog, null);
            initEditView(view);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("新增");
            builder.setView(view);
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                String n = n(editText.getText().toString());
                if (n.equals("") || !e(n)) {
                    ToastHelper.makeText(this, "格式错误");
                    return;
                }

                String[] split = n.split(",", 6);
                LocationData kVar2 = new LocationData(String.valueOf(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]), Integer.parseInt(split[3]), Integer.parseInt(split[4]), Integer.parseInt(split[5]), split[6], 7);
                long b2 = mSQLiteHelper.b(kVar2);
                if (b2 < 0) {
                    ToastHelper.makeText(this, "Can't insert");
                    return;
                }

                kVar2.setF((int) b2);
                mLocationDataList.add(0, kVar2);
                mAdapter.notifyDataSetChanged();
            });
            builder.setNegativeButton(android.R.string.cancel, null).show();
        } else if (item.getItemId() == R.id.location_delete) {
            mLocationDataList.remove(mMenuInfo.position);
            mAdapter.notifyDataSetChanged();
            if (mSQLiteHelper.a(mDataPosition) != 1) {
                ToastHelper.makeText(this, "Can't delete.");
            }
        } else {
            return super.onContextItemSelected(item);
        }
        return true;
    }

    private void initEditView(View view) {
        mTitle = view.findViewById(R.id.title);
        mOffset = view.findViewById(R.id.offset);
        mBaseStation = view.findViewById(R.id.base_station);
        mLongitudeAndLatitude = view.findViewById(R.id.longitude_latitude);
        mRemarks = view.findViewById(R.id.remarks);

    }

    public static String n(String str) {
        return str.replaceAll("\\s+", "");
    }

    public static boolean e(String str) {
        return Pattern.compile("^((-?\\d+(\\.\\d+)?,){2}(\\d+,){3}.+)$").matcher(str).matches();
    }


    public class LocationAdapter extends ArrayAdapter<LocationData> {

        private List<LocationData> mList;
        private LayoutInflater mInflater;

        public LocationAdapter(@NonNull Context context, List<LocationData> list) {
            super(context, (int) R.layout.item_location, list);
            mList = list;
            mInflater = getLayoutInflater();
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LocationData mData = getItem(position);
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.item_location, parent, false);
                holder = new ViewHolder();
                holder.mTitle = convertView.findViewById(android.R.id.title);
                holder.mRegionCode = convertView.findViewById(R.id.region_code);
                holder.mBaseStation = convertView.findViewById(R.id.base_station);
                holder.mLongitudeAndLatitude = convertView.findViewById(R.id.longitude_latitude);
                holder.mSummary = convertView.findViewById(android.R.id.summary);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.mTitle.setText(mData.getTitle());
            holder.mRegionCode.setText("偏移：" + mData.getOffset());
            holder.mBaseStation.setText("基站：" + mData.getRegionCode() + "," + mData.getBaseStationCode());
            holder.mLongitudeAndLatitude.setText("经纬度：" + mData.getLongitude() + "," + mData.getLatitude());
            holder.mSummary.setText(mData.toString());
            holder.mSummary.setVisibility(View.VISIBLE);
            return convertView;
        }

        class ViewHolder {
            TextView mTitle;
            TextView mRegionCode;
            TextView mBaseStation;
            TextView mLongitudeAndLatitude;
            TextView mSummary;
        }
    }
}
