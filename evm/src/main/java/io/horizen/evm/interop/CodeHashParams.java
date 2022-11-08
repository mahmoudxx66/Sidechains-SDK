package io.horizen.evm.interop;

import io.horizen.evm.utils.Hash;

public class CodeHashParams extends AccountParams {
    public Hash codeHash;

    public CodeHashParams() {
    }

    public CodeHashParams(int handle, byte[] address, byte[] codeHash) {
        super(handle, address);
        this.codeHash = Hash.FromBytes(codeHash);
    }
}
