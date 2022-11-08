package io.horizen.evm.interop;

import io.horizen.evm.JsonPointer;

public class LevelDBParams extends JsonPointer {
    public String path;

    public LevelDBParams() {
    }

    public LevelDBParams(String path) {
        this.path = path;
    }
}
