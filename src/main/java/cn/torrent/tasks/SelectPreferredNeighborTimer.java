package cn.torrent.tasks;

import cn.torrent.Logger;
import cn.torrent.MessageIO;
import cn.torrent.PeerState;
import cn.torrent.config.PeerInfo;
import cn.torrent.enums.ChokeStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

public class SelectPreferredNeighborTimer extends TimerTask {
    final PeerState state;
    final Logger logger;

    public SelectPreferredNeighborTimer(final PeerState state, Logger logger) {
        this.state = state;
        this.logger = logger;
    }

    @Override
    public void run() {
        state.updatePreferredNeighbors();
        List<Integer> preferredNeighbors = new ArrayList<>();
        for (PeerInfo peerInfo : state.getPeersConfig()) {
            if (peerInfo.peerID == state.peerID) continue;
            MessageIO io = state.getIOHandlerPeer(peerInfo.peerID);
            try {
                if (state.neighbourChokeStatus.get(peerInfo.peerID) == ChokeStatus.CHOKED) {
                    io.writeChoke();
                    logger.sendChoke(state.peerID, peerInfo.peerID);
                } else {
                    io.writeUnChoke();
                    logger.sendUnChoke(state.peerID, peerInfo.peerID);
                    preferredNeighbors.add(peerInfo.peerID);
                }
            } catch (IOException e) {
                break;
            }
        }
        if (preferredNeighbors.size() > 0)
            logger.changesPreferredNeighbors(state.peerID, preferredNeighbors);
    }
}