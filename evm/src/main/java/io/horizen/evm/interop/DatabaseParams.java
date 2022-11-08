package io.horizen.evm.interop;

import io.horizen.evm.JsonPointer;

public class DatabaseParams extends JsonPointer {
    public int databaseHandle;

    public DatabaseParams() {
    }

    public DatabaseParams(int databaseHandle) {
        this.databaseHandle = databaseHandle;
    }
}
