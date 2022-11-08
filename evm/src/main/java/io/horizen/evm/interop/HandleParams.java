package io.horizen.evm.interop;

import io.horizen.evm.JsonPointer;

public class HandleParams extends JsonPointer {
    public int handle;

    public HandleParams() {
    }

    public HandleParams(int handle) {
        this.handle = handle;
    }
}
