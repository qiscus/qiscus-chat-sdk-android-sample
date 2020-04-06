package com.qiscus.mychatui.util;

import com.qiscus.sdk.chat.core.QiscusCore;

public class Const {

    private static QiscusCore qiscusCore;

    private static QiscusCore qiscusCore1;
    private static QiscusCore qiscusCore2;

    public static void setQiscusCore(QiscusCore qiscusCore) {
        Const.qiscusCore = qiscusCore;
    }

    public static void setQiscusCore1(QiscusCore qiscusCore1) {
        Const.qiscusCore1 = qiscusCore1;
    }

    public static void setQiscusCore2(QiscusCore qiscusCore2) {
        Const.qiscusCore2 = qiscusCore2;
    }

    public static QiscusCore qiscusCore1() {
        if (qiscusCore1 != null) {
            return qiscusCore1;
        } else {
            try {
                throw new Exception("QiscusCore 1 null");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public static QiscusCore qiscusCore2() {
        if (qiscusCore2 != null) {
            return qiscusCore2;
        } else {
            try {
                throw new Exception("QiscusCore 2 null");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public static QiscusCore qiscusCore() {
        if (qiscusCore != null) {
            return qiscusCore;
        } else {
            try {
                throw new Exception("QiscusCore null");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
