package cn.torrent.tasks;

import cn.torrent.Logger;
import cn.torrent.SocketMessageReadWrite;
import cn.torrent.PeerState;

import java.io.IOException;
import java.util.Optional;
import java.util.TimerTask;

public class SelectOptimisticallyUnChokedNeighborTimer extends TimerTask {
    final PeerState state;
    final Logger logger;

    public SelectOptimisticallyUnChokedNeighborTimer(PeerState state, Logger logger) {
        this.state = state;
        this.logger = logger;
    }

    @Override
    public void run() {
        Optional<Integer> optimisticUnchokedPeer = state.updateOptimisticNeighbor();
        if (optimisticUnchokedPeer.isPresent()) {
            SocketMessageReadWrite io = state.getIOHandlerPeer(optimisticUnchokedPeer.get());
            try {
                io.writeUnChoke();
                logger.changesOptimisticallyUnChokedNeighbor(state.peerID, optimisticUnchokedPeer.get());
            } catch (IOException ignored) {
            }
        }
    }
}