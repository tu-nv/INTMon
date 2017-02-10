package org.onosproject.intmon.lib;

import java.util.Objects;

public class DevicePair {

    public Integer one;
    public Integer two;

    public DevicePair(Integer one, Integer two) {
        this.one = one;
        this.two = two;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
//        if (!super.equals(obj)) {
//            return false;
//        }c
        if (!(obj instanceof DevicePair)) {
            return false;
        }
        DevicePair other = (DevicePair) obj;

        boolean equalOne = this.one.equals(other.one) && this.two.equals(other.two);
        boolean equalTwo = this.one.equals(other.two) && this.two.equals(other.one);
        if (!(equalOne || equalTwo)) {
            return false;
        }


        return true;
    }

    @Override
    public int hashCode() {
        if (one.compareTo(two) < 0) {
            return Objects.hash(one, two);
        }
        return Objects.hash(two, one);
    }

}
