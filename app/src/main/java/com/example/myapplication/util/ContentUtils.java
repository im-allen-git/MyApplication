package com.example.myapplication.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ContentUtils {


    private static ThreadLocal<Activity> currentActivity = new ThreadLocal<>();

    private static File currentDir = null;

    public static void setCurrentDir(File file) {
        ContentUtils.currentDir = file;
    }




}