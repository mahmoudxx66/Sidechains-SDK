package io.horizen.evm.interop;

import io.horizen.evm.utils.Address;
import io.horizen.evm.utils.Hash;

import java.math.BigInteger;

public class ProofAccountResult {
    public Address address;
    public String[] accountProof;
    public BigInteger balance;
    public Hash codeHash;
    public BigInteger nonce;
    public Hash storageHash;
    public ProofStorageResult[] storageProof;

    public static class ProofStorageResult {
        public String key;
        public BigInteger value;
        public String[] proof;
    }
}
