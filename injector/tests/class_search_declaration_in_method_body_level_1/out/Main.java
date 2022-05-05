package com.test;

import javax.annotation.Nullable;

public class Main implements Runnable {

    @Override
    public Object foo() {
        class Bar {

            int i = 5 * 6;

            @Nullable
            public Object get() {
                return null;
            }
        }
    }
}
