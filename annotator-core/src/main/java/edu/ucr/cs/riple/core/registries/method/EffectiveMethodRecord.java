/*
 * MIT License
 *
 * Copyright (c) 2025 Nima Karimipour
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */


package edu.ucr.cs.riple.core.registries.method;

import edu.ucr.cs.riple.injector.location.OnMethod;

import java.util.Objects;

public class EffectiveMethodRecord {
    /**
     * Method that uses the parameter in an effective way.
     */
    public final OnMethod method;
    /**
     * The parameter name that is used in an effective way.
     */
    public final String parameter;
    /**
     * The index of the parameter in the method.
     */
    public final int index;

    public EffectiveMethodRecord(OnMethod method, String parameter, int index) {
        this.method = method;
        this.parameter = parameter;
        this.index = index;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EffectiveMethodRecord)) {
            return false;
        }
        EffectiveMethodRecord that = (EffectiveMethodRecord) o;
        return index == that.index && Objects.equals(method, that.method) && Objects.equals(parameter, that.parameter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, parameter, index);
    }
}
