package org.onosproject.intmon.lib;

import java.util.Objects;

public class DevicePair {

    Integer srcD;
    Integer dstD;

    public DevicePair(Integer srcD, Integer dstD) {
        this.srcD = srcD;
        this.dstD = dstD;
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
//        }
        if (!(obj instanceof DevicePair)) {
            return false;
        }
        DevicePair other = (DevicePair) obj;

        if (!this.srcD.equals(other.srcD)) {
            return false;
        }

        if (!this.dstD.equals(other.dstD)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(srcD, dstD);
    }

}
