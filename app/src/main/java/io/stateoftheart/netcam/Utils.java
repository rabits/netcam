package io.stateoftheart.netcam;

import android.app.ActivityManager;
import android.content.Context;
import org.jetbrains.annotations.NotNull;

public final class Utils {
   public static boolean isServiceRunning(@NotNull Context context, @NotNull Class serviceClass) {
      ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
      for(ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
         if(serviceClass.getName().equals(service.service.getClassName())) {
            return true;
         }
      }

      return false;
   }
}
