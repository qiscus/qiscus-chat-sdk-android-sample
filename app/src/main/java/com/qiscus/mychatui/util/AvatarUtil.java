package com.qiscus.mychatui.util;

/**
 * Created on : January 31, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public final class AvatarUtil {
    public static String generateAvatar(String s) {
        s = s.replaceAll(" ", "");
        return "https://robohash.org/" + s + "/bgset_bg2/3.14160?set=set4";
    }
}
