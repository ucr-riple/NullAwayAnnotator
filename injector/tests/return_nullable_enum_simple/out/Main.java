package com.uber;

import javax.annotation.Nullable;

public class Main {

    public enum Test {

        CLASSIC;

        @Nullable
        public Object run() {
            return null;
        }
    }
}
