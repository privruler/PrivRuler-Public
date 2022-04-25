package com.privruler.azure;

import android.util.Log;
import com.microsoft.windowsazure.messaging.NotificationHub;
import com.privruler.MainActivity;
import com.privruler.tools.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NotificationHubTest {
    public boolean accessible;
    private String pkg;
    private NotificationHub notificationHub;
    private boolean register;
    private boolean unregister;
    private String randomUUID;

    public NotificationHubTest(String pkg, NotificationHub notificationHub) {
        this.pkg = pkg;
        this.notificationHub = notificationHub;
        this.register = false;
        this.unregister = false;
        this.accessible = false;
        this.randomUUID = UUID.nameUUIDFromBytes("test-email-anonymous@gmail.com".getBytes()).toString();
    }

    public void doTest() {
        Log.v(MainActivity.LOG_TAG, "Testing NotificationHub App:" + this.pkg);

        try {
            this.notificationHub.register(this.randomUUID);
            this.register = true;
            this.accessible = true;
        } catch (Exception e) {
            StringUtils.logException(e);
            // Exception that represents a resource not found in the Notification Hub server
            if (e.toString().contains("NotificationHubUnauthorizedException")) {
                this.accessible = true;
            }
        }

        try {
            this.notificationHub.unregister();
            this.unregister = true;
        } catch (Exception e) {
            StringUtils.logException(e);
        }
    }

    public List<String> toCapabilityList() {
        List<String> ret = new ArrayList<String>();
        if (this.register) {
            ret.add(" register");
        }

        if (this.unregister) {
            ret.add(" unregister");
        }
        return ret;
    }
}