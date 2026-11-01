package com.example.astro_mobile.shared.notifications;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.astro_mobile.data.local.NotificationPreferences;
import com.example.astro_mobile.data.local.PushPreferences;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class NotificationPreferenceDeviceTest {
    @Test public void optOutPersistsPerAccountAndDoesNotChangeOtherAccount() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String first="notification-preference-test-first",second="notification-preference-test-second";
        var preferences=context.getSharedPreferences("astro_notification_preferences",Context.MODE_PRIVATE);
        try {
            preferences.edit().remove(first).remove(second).commit();
            assertTrue(NotificationPreferences.isEnabled(context,first));
            NotificationPreferences.save(context,first,false);
            assertFalse(NotificationPreferences.isEnabled(context,first));assertTrue(NotificationPreferences.isEnabled(context,second));
            NotificationPreferences.save(context,second,false);NotificationPreferences.save(context,first,true);
            assertTrue(NotificationPreferences.isEnabled(context,first));assertFalse(NotificationPreferences.isEnabled(context,second));
            assertFalse(NotificationPreferences.isEnabled(context,null));
        }finally{preferences.edit().remove(first).remove(second).commit();}
    }
    @Test public void removingSyncMetadataDoesNotLoseDeviceIdentityOrLogoutState() {
        // Context com preferências próprias: não altera o registro real do aparelho.
        Context original=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context isolated=new android.content.ContextWrapper(original){
            @Override public Context getApplicationContext(){return this;}
            @Override public android.content.SharedPreferences getSharedPreferences(String name,int mode){return original.getSharedPreferences("test-notification-sync-"+name,mode);}
        };
        var data=isolated.getSharedPreferences(PushPreferences.NAME,Context.MODE_PRIVATE);
        try {
            PushPreferences preferences=new PushPreferences(isolated);String device=preferences.deviceId();
            preferences.synchronizedToken("fixture-account","fixture-token",123);preferences.beginLogout("fixture-account");
            preferences.forgetSynchronizedToken();
            assertNull(preferences.uid());assertEquals(device,preferences.deviceId());assertTrue(preferences.logoutPending());
            assertEquals("fixture-account",preferences.logoutUid());assertTrue(preferences.needsSync("fixture-account","fixture-token",124));
        }finally{data.edit().clear().commit();}
    }
}
