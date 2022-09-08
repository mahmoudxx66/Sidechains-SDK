package com.horizen.account.api.rpc.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

public class Quantity {
    private final String value;

    @JsonCreator
    public Quantity(String value) {
        this.value = value;
    }

    public Quantity(BigInteger number) {
        this(Numeric.encodeQuantity(number));
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}