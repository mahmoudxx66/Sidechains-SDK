package io.horizen.evm.interop;

public class SnapshotParams extends HandleParams {
    public int revisionId;

    public SnapshotParams() {
    }

    public SnapshotParams(int handle, int revisionId) {
        super(handle);
        this.revisionId = revisionId;
    }
}
