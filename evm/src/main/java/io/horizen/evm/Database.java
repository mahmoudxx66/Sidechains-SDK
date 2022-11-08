package io.horizen.evm;

public class Database extends ResourceHandle {
    public Database(int handle) {
        super(handle);
    }

    @Override
    public void close() throws Exception {
        LibEvm.closeDatabase(handle);
    }
}
