package io.horizen.evm.interop;

import io.horizen.evm.utils.Address;

import java.math.BigInteger;

public class EvmContext {
    public BigInteger difficulty;
    public Address coinbase;
    public BigInteger blockNumber;
    public BigInteger time;
    public BigInteger baseFee;
    public BigInteger gasLimit;
}
