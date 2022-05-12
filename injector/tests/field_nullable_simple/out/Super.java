package com.uber;

import javax.annotation.Nullable;

public class Super {

    @Nullable
    Object h = new Object();

    public void test(@Nullable Object f) {
        h = f;
    }
}
