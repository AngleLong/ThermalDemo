package com.rrtx.thermaldemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.taobao.sophix.SophixManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 阿里热修复加载补丁的管理类
 */

public class FixManager {
    public static final String FIX_CONFIG = "FIX_CONFIG";
    /**
     * 加载的时间
     */
    public static final String FIX_TIME = "FIX_TIME";
    /**
     * 加载的次数
     */
    public static final String FIX_COUNT = "FIX_COUNT";


    /**
     * 2. 查询并加载补丁
     */
    public static void queryAndLoadNewPatch(Context context) {
        if (context == null) {
            return;
        }
        // 这接口一天不能调用超过20次
        SharedPreferences preferences
                = context.getSharedPreferences(FIX_CONFIG, Context.MODE_PRIVATE);
        String time = preferences.getString(FIX_TIME, "");
        // 如果是没有缓存时间，说明是第一次调用
        if (TextUtils.isEmpty(time)) {
            // 将当前日期存起来
            Calendar calendar = Calendar.getInstance();
            Date date = calendar.getTime();
            // 第一次去加载
            loadFixPatch(preferences, date, 1);
        } else {
            // 就要对比时间了
            Calendar calendar = Calendar.getInstance();
            Date date = calendar.getTime();
            String currentTime = getTime(date);
            // 如果日期相等，就要判断加载的次数
            int count = preferences.getInt(FIX_COUNT, 1);
            // 同一天调用
            if (time.equals(currentTime)) {
                // 限制次数，如果超出范围就不调用了
                if (count < 19) {
                    count++;
                    loadFixPatch(preferences, date, count);
                }
            } else {
                // 去加载补丁，并将次数置为1
                loadFixPatch(preferences, date, 1);
            }
        }

    }

    /**
     * 加载补丁并记录次数
     */
    private static void loadFixPatch(SharedPreferences preferences, Date date, int count) {
        // 这个方法免费一天一个设备调用20次
        SophixManager.getInstance().queryAndLoadNewPatch();
        // 缓存日期和调用的次数
        preferences.edit()
                .putString(FIX_TIME, getTime(date))
                .putInt(FIX_COUNT, count)
                .commit();
    }

    /**
     * 按年月日的时间来缓存日期
     */
    public static String getTime(Date date) {
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            return df.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


}