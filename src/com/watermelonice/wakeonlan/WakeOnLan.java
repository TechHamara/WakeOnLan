package com.watermelonice.wakeonlan;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;

public class WakeOnLan extends AndroidNonvisibleComponent {

    private enum HexType {
        MAC, SECUREON
    }

    public WakeOnLan(ComponentContainer container) {
        super(container.$form());
    }

    @SimpleEvent(description = "Raised when magic packet is sent with WakeOnLanAsync method.")
    public void OnSent(String macAddress, String hostname, int port, String secureOnPassword) {
        EventDispatcher.dispatchEvent(this, "OnSent", macAddress, hostname, port, secureOnPassword);
    }

    @SimpleEvent(description = "Raised when error occurred during WakeOnLanAsync.")
    public void OnWoLError(String error, String macAddress, String hostname, int port, String secureOnPassword) {
        EventDispatcher.dispatchEvent(this, "OnWoLError", error, macAddress, hostname, port, secureOnPassword);
    }

    @SimpleEvent(description = "Raised when online state is found.")
    public void GotOnlineState(boolean online, String hostname) {
        EventDispatcher.dispatchEvent(this, "GotOnlineState", online, hostname);
    }

    @SimpleEvent(description = "Raised when error occurred during GetOnlineState.")
    public void OnGetOnlineStateError(String error, String hostname) {
        EventDispatcher.dispatchEvent(this, "OnGetOnlineStateError", error, hostname);
    }

    @SimpleFunction(description = "Wake On Lan asynchronously. The format of mac address and SecureOn password is XX:XX:XX:XX:XX:XX or XX-XX-XX-XX-XX-XX.")
    public void WakeOnLanAsync(final String macAddress, final String hostname, final int port,
            final String secureOnPassword) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                wol(macAddress, hostname, port, secureOnPassword);
            }
        }).start();
    }

    @SimpleFunction(description = "Check if device is online (no guarantee). Timeout is in milliseconds (ms).")
    public void GetOnlineState(final String hostname, final int timeout) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                onlineState(hostname, timeout);
            }
        }).start();
    }

    private void wol(String mac, String hostname, int port, String secureOn) {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] macBytes = getMacOrSecureOnBytes(mac, HexType.MAC);
            byte[] bytes;
            if (!secureOn.isEmpty()) {
                byte[] secureOnBytes = getMacOrSecureOnBytes(secureOn, HexType.SECUREON);
                bytes = new byte[6 + 16 * macBytes.length + secureOnBytes.length];
                System.arraycopy(secureOnBytes, 0, bytes, 102, secureOnBytes.length);
            } else {
                bytes = new byte[6 + 16 * macBytes.length];
            }

            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) 0xff;
            }
            for (int i = 6; i < bytes.length; i += macBytes.length) {
                System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
            }

            InetAddress address = InetAddress.getByName(hostname);
            hostname = address.getHostAddress();
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
            socket.send(packet);

            OnSent(mac, hostname, port, secureOn);
        } catch (Exception e) {
            OnWoLError(e.toString(), mac, hostname, port, secureOn);
        }
    }

    private static byte[] getMacOrSecureOnBytes(String str, HexType t) throws IllegalArgumentException {
        byte[] bytes = new byte[6];
        String[] hex = str.split("[\\:\\-]");
        if (hex.length != 6) {
            throw new IllegalArgumentException(t == HexType.MAC ? "Invalid MAC address." : "Invalid SecureOn password");
        }
        try {
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) Integer.parseInt(hex[i], 16);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    t == HexType.MAC ? "Invalid hex digit in MAC address." : "Invalid hex digit in SecureOn password.");
        }
        return bytes;
    }

    private void onlineState(String hostname, int timeout) {
        boolean online = false;

        try {
            InetAddress addr = InetAddress.getByName(hostname);
            hostname = addr.getHostAddress();
            online = addr.isReachable(timeout);
        } catch (UnknownHostException e) { /* Do Nothing */ }
            catch (Exception e) {
            OnGetOnlineStateError(e.toString(), hostname);
        }

        GotOnlineState(online, hostname);
    }

}