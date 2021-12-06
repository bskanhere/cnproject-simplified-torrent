package cn.torrent.tasks;

import cn.torrent.Log;
import cn.torrent.SocketMessageReadWrite;
import cn.torrent.peer.PeerState;

import java.io.IOException;
import java.util.Optional;
import java.util.TimerTask;

public class SelectOptimisticallyUnChokedNeighborTimer extends TimerTask {
    final PeerState state;
    final Log log;

    public SelectOptimisticallyUnChokedNeighborTimer(PeerState state, Log log) {
        this.state = state;
        this.log = log;
    }

    @Override
    public void run() {
        Optional<Integer> optimisticUnchokedPeer = state.updateOptimisticNeighbor();
        if (optimisticUnchokedPeer.isPresent()) {
            SocketMessageReadWrite io = state.getIOHandlerPeer(optimisticUnchokedPeer.get());
            try {
                io.writeUnChoke();
                log.changesOptimisticallyUnChokedNeighbor(state.peerID, optimisticUnchokedPeer.get());
            } catch (IOException ignored) {
            }
        }
    }
}