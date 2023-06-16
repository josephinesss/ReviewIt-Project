package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;

/**
 * @author Josephinesss
 */
public class RegexUtils {
    /**
     * valid phone number
     * @param phone phone number
     * @return true: valid，false: invalid
     */
    public static boolean isPhoneInvalid(String phone){
        return mismatch(phone, RegexPatterns.PHONE_REGEX);
    }
    /**
     * valid email
     * @param email email
     * @return true: valid，false：invalid
     */
    public static boolean isEmailInvalid(String email){
        return mismatch(email, RegexPatterns.EMAIL_REGEX);
    }

    /**
     * valid verification code
     * @param code verification code
     * @return true: valid，false：invalid
     */
    public static boolean isCodeInvalid(String code){
        return mismatch(code, RegexPatterns.VERIFY_CODE_REGEX);
    }

    // valid regex
    private static boolean mismatch(String str, String regex){
        if (StrUtil.isBlank(str)) {
            return true;
        }
        return !str.matches(regex);
    }
}
