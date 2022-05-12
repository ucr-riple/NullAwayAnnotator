package com.uber;

import javax.annotation.Nullable;

public class Super {

    @Nullable
    Object test(Object flag, String name, String lastname) {
        if (flag == null) {
            return new Object();
        } else
            return new Object();
    }

    Object test(Object flag, @Nullable Object name, String lastname) {
        if (flag == null) {
            return new Object();
        } else
            return new Object();
    }
}
