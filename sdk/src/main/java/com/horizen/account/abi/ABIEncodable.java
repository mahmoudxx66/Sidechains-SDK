package com.horizen.account.abi;

import org.web3j.abi.DefaultFunctionEncoder;
import org.web3j.abi.datatypes.Type;
import org.web3j.utils.Numeric;

import java.util.Arrays;
import java.util.List;

public interface  ABIEncodable<T extends Type> {

    default byte[] encode() {
        DefaultFunctionEncoder encoder = new DefaultFunctionEncoder();
        List listOfABIObjs = Arrays.asList(asABIType());
        String encodedString = encoder.encodeParameters(listOfABIObjs);
        return Numeric.hexStringToByteArray(encodedString);

    }

    T asABIType();

}

