package io.horizen.evm.interop;

import io.horizen.evm.JsonPointer;

public class HashParams extends JsonPointer {
    public byte[][] values;

    public HashParams() {
    }

    public HashParams(byte[][] values) {
        this.values = values;
    }
}
