package com.eternalsnow.village;

/** Pure rules shared by registration and regression tests. */
public final class ConfectionerPolicy {
    private ConfectionerPolicy() {}
    public static final int SUGAR_BUY = 12, BEANS_BUY = 8, CARAMEL_BUY = 4, CANDY_BUY = 3;
    public static final int CANDY_SELL_COUNT = 3, CANDY_SELL_PRICE = 2;
    public static boolean eligibleTemplate(String namespace, String path) {
        return namespace.equals("minecraft") && path.startsWith("village/")
                && path.contains("/houses/") && !path.contains("zombie");
    }
}
