package com.example.demo.utils;

import java.util.regex.Pattern;

public class isPhoneLegal {
    
    private static final Pattern CN_MOBILE = Pattern.compile("^1[3-9]\\d{9}");

    public static boolean isLegalPhone(String Phone){

        return CN_MOBILE.matcher(Phone).matches();
    }
}
