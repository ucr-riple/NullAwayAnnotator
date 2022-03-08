package com.uber;

import javax.annotation.Nullable;

public class Super {

    @Nullable
    Object test(@Nullable Object flag) {
        if (flag == null) {
            return new Object();
        } else
            return new Object();
    }
}
