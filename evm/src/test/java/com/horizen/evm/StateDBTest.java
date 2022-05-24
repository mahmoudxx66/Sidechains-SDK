package com.horizen.evm;

import com.horizen.evm.library.Evm;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public class StateDBTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void TestStateDB() throws Exception {
        var databaseFolder = tempFolder.newFolder("evm-db");
        System.out.println("temporary database folder: " + databaseFolder.getAbsolutePath());

        String hashNull = "0x0000000000000000000000000000000000000000000000000000000000000000";
        String hashEmpty = "0x56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421";

        System.out.println("Initialize");
        var initResult = Evm.Instance.Initialize(databaseFolder.getAbsolutePath());
        System.out.println("Initialize result " + initResult);

        System.out.println("OpenState");
        var statedb = new StateDB(hashNull);
        System.out.println("OpenState result " + statedb);

        System.out.println("GetIntermediateStateRoot");
        var root = statedb.GetIntermediateStateRoot();
        System.out.println("GetIntermediateStateRoot result " + root);

        assertEquals("empty state should give the hash of an empty string as the root hash", root, hashEmpty);

        System.out.println("CloseState");
        statedb.close();
    }
}
