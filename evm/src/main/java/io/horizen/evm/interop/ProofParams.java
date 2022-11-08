package io.horizen.evm.interop;

import io.horizen.evm.utils.Address;
import io.horizen.evm.utils.Hash;

import java.util.Arrays;

public class ProofParams extends AccountParams {
    public Hash[] keys;

    public ProofParams() {
    }

    public ProofParams(int handle, byte[] address, byte[][] keys) {
        super(handle, address);
        this.address = Address.FromBytes(address);
        this.keys = Arrays.stream(keys).map(Hash::FromBytes).toArray(Hash[]::new);
    }
}


