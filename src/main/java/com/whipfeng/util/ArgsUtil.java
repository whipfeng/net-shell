package com.whipfeng.util;

import java.util.HashMap;

/**
 * Created by user on 2018/11/23.
 */
public class ArgsUtil {

    private HashMap<String, String> argMap = new HashMap<String, String>();

    public ArgsUtil(String args[]) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                argMap.put(arg, args[++i]);
            }
        }
    }

    public int get(String key, int defaultVal) {
        String val = argMap.get(key);
        if (null == val) {
            return defaultVal;
        }
        return Integer.valueOf(val);
    }

    public String get(String key, String defaultVal) {
        String val = argMap.get(key);
        if (null == val) {
            return defaultVal;
        }
        return val;
    }
}
