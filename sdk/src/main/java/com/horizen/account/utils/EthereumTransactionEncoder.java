package com.horizen.account.utils;

import com.horizen.account.transaction.EthereumTransaction;
import org.web3j.crypto.Sign;
import org.web3j.rlp.*;
import org.web3j.utils.Bytes;
import org.web3j.utils.Numeric;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.horizen.account.utils.EthereumTransactionUtils.convertToBytes;
import static org.web3j.crypto.TransactionEncoder.createEip155SignatureData;

public class EthereumTransactionEncoder {

    public EthereumTransactionEncoder() {
    }

    public static byte[] encodeLegacyAsRlpValues(EthereumTransaction tx, boolean accountSignature) {

        List<RlpType> result = new ArrayList<>();

        result.add(RlpString.create(tx.getNonce()));

        result.add(RlpString.create(tx.getGasPrice()));
        result.add(RlpString.create(tx.getGasLimit()));

        // an empty to address (contract creation) should not be encoded as a numeric 0 value
        if (tx.getToAddressString() != null && tx.getToAddressString().length() > 0) {
            // addresses that start with zeros should be encoded with the zeros included, not
            // as numeric values
            result.add(RlpString.create(Numeric.hexStringToByteArray(tx.getToAddressString())));
        } else {
            result.add(RlpString.create(""));
        }

        result.add(RlpString.create(tx.getValue()));

        // value field will already be hex encoded, so we need to convert into binary first
        byte[] dataBytes = tx.getData();
        result.add(RlpString.create(dataBytes));

        if (accountSignature) {
            if (!tx.isSigned())
                throw new IllegalArgumentException("We should take signature into account for encoding, but tx is not signed!");
            Sign.SignatureData signatureData;
            if (tx.isEIP155()) {
                signatureData = createEip155SignatureData(tx.getSignature().getSignatureData(), tx.getChainId());
            } else {
                signatureData = tx.getSignature().getSignatureData();
            }
            result.add(RlpString.create(EthereumTransactionUtils.trimLeadingZeroes(signatureData.getV())));
            result.add(RlpString.create(EthereumTransactionUtils.trimLeadingZeroes(signatureData.getR())));
            result.add(RlpString.create(EthereumTransactionUtils.trimLeadingZeroes(signatureData.getS())));
        } else {
            if (tx.isEIP155()) {
                result.add(RlpString.create(EthereumTransactionUtils.trimLeadingZeroes(convertToBytes(tx.getChainId()))));
                result.add(RlpString.create(EthereumTransactionUtils.trimLeadingZeroes(new byte[] {})));
                result.add(RlpString.create(EthereumTransactionUtils.trimLeadingZeroes(new byte[] {})));
            }
        }

        RlpList rlpList = new RlpList(result);
        return RlpEncoder.encode(rlpList);
    }

    public static byte[] encodeEip1559AsRlpValues(EthereumTransaction tx, boolean accountSignature) {

        List<RlpType> result = new ArrayList<>();

        result.add(RlpString.create(tx.getChainId()));

        result.add(RlpString.create(tx.getNonce()));

        // add maxPriorityFeePerGas and maxFeePerGas if this is an EIP-1559 transaction
        result.add(RlpString.create(tx.getMaxPriorityFeePerGas()));
        result.add(RlpString.create(tx.getMaxFeePerGas()));

        result.add(RlpString.create(tx.getGasLimit()));

        // an empty to address (contract creation) should not be encoded as a numeric 0 value
        if (tx.getToAddressString() != null && tx.getToAddressString().length() > 0) {
            // addresses that start with zeros should be encoded with the zeros included, not
            // as numeric values
            result.add(RlpString.create(Numeric.hexStringToByteArray(tx.getToAddressString())));
        } else {
            result.add(RlpString.create(""));
        }

        result.add(RlpString.create(tx.getValue()));

        // value field will already be hex encoded, so we need to convert into binary first
        byte[] data = Numeric.hexStringToByteArray(tx.getDataString());
        result.add(RlpString.create(data));

        // access list
        result.add(new RlpList());

        if (accountSignature) {
            if (!tx.isSigned())
                throw new IllegalArgumentException("We should take signature into account for encoding, but tx is not signed!");
            Sign.SignatureData signatureData = tx.getSignature().getSignatureData();
            result.add(RlpString.create(Sign.getRecId(signatureData, tx.getChainId())));
            result.add(RlpString.create(EthereumTransactionUtils.trimLeadingZeroes(signatureData.getR())));
            result.add(RlpString.create(EthereumTransactionUtils.trimLeadingZeroes(signatureData.getS())));
        }

        RlpList rlpList = new RlpList(result);
        byte[] encoded = RlpEncoder.encode(rlpList);

        return ByteBuffer.allocate(encoded.length + 1)
                .put(tx.version())
                .put(encoded)
                .array();
    }
}