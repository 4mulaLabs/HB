package com.example.heartbeat;

public class Destination {
    String ipAddress;
    Boolean isUp;

    public Destination(String ipAddress, Boolean isConnected) {
        this.ipAddress = ipAddress;
        this.isUp = isConnected;
    }

    public Destination(String ipAddress) {
        this.ipAddress = ipAddress;
        this.isUp = false;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Boolean getUp() {
        return isUp;
    }

    public void setIsUp(Boolean up) {
        isUp = up;
    }
}
