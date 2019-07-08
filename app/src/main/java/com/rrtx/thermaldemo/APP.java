package com.rrtx.thermaldemo;

import android.app.Application;

import com.taobao.sophix.SophixManager;

/**
 * @author : Angle
 * 创建时间 : 2019/7/5 10:26
 * 描述 : 项目的Application
 */
public class APP extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        SophixManager.getInstance().queryAndLoadNewPatch();//查询是否有新的补丁
    }
}
