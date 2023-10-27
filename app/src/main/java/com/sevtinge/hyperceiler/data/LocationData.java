package com.sevtinge.hyperceiler.data;

public class LocationData {

    private String Title; // 备注
    private Double Longitude; // 经度
    private Double Latitude; // 纬度
    private int Offset;
    private int RegionCode; // 区域代码
    private int BaseStationCode; // 基站代码
    private int f;
    private String Remarks; // 备注

    public LocationData() {
    }

    // 标题,经度,纬度,偏移
    public LocationData(String title, Double longitude, Double latitude, int offset, int regionCode, int baseStationCode, String remarks, int i4) {
        Title = title;
        Longitude = longitude;
        Latitude = latitude;
        Offset = offset;
        RegionCode = regionCode;
        BaseStationCode = baseStationCode;
        Remarks = remarks;
        f = i4;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public Double getLongitude() {
        return Longitude;
    }

    public void setLongitude(Double longitude) {
        Longitude = longitude;
    }

    public Double getLatitude() {
        return Latitude;
    }

    public void setLatitude(Double latitude) {
        Latitude = latitude;
    }

    public int getOffset() {
        return Offset;
    }

    public void setOffset(int offset) {
        Offset = offset;
    }

    public int getRegionCode() {
        return RegionCode;
    }

    public void setRegionCode(int regionCode) {
        RegionCode = regionCode;
    }

    public int getBaseStationCode() {
        return BaseStationCode;
    }

    public void setBaseStationCode(int baseStationCode) {
        BaseStationCode = baseStationCode;
    }

    public int getF() {
        return f;
    }

    public void setF(int f) {
        this.f = f;
    }

    public String getRemarks() {
        return Remarks;
    }

    public void setRemarks(String remarks) {
        Remarks = remarks;
    }

    public String toString() {
        return String.valueOf(Latitude) + "," + String.valueOf(Longitude) + "," + String.valueOf(Offset) + "," + String.valueOf(RegionCode) + "," + String.valueOf(BaseStationCode) + "," + Remarks;
    }
}
