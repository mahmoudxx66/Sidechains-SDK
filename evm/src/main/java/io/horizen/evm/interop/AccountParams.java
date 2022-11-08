package io.horizen.evm.interop;

import io.horizen.evm.utils.Address;

public class AccountParams extends HandleParams {
    public Address address;

    public AccountParams() {
    }

    public AccountParams(int handle, byte[] address) {
        super(handle);
        this.address = Address.FromBytes(address);
    }
}
