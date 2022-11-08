package io.horizen.evm.interop;

public class SetStorageBytesParams extends StorageParams {
    public byte[] value;

    public SetStorageBytesParams() {
    }

    public SetStorageBytesParams(int handle, byte[] address, byte[] key, byte[] value) {
        super(handle, address, key);
        this.value = value;
    }
}
