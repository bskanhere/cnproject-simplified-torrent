package cn.torrent.tasks;

import cn.torrent.Log;
import cn.torrent.SocketMessageReadWrite;
import cn.torrent.peer.PeerState;
import cn.torrent.config.PeerInfo;
import cn.torrent.enums.ChokeStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

public class SelectPreferredNeighborTimer extends TimerTask {
    final PeerState state;
    final Log log;

    public SelectPreferredNeighborTimer(final PeerState state, Log log) {
        this.state = state;
        this.log = log;
    }

    @Override
    public void run() {
        state.updatePreferredNeighbors();
        List<Integer> preferredNeighbors = new ArrayList<>();
        for (PeerInfo peerInfo : state.getPeersConfig()) {
            if (peerInfo.peerID == state.peerID) continue;
            SocketMessageReadWrite io = state.getIOHandlerPeer(peerInfo.peerID);
            try {
                if (state.neighbourChokeStatus.get(peerInfo.peerID) == ChokeStatus.CHOKED) {
                    io.writeChoke();
                    log.sendChoke(state.peerID, peerInfo.peerID);
                } else {
                    io.writeUnChoke();
                    log.sendUnChoke(state.peerID, peerInfo.peerID);
                    preferredNeighbors.add(peerInfo.peerID);
                }
            } catch (IOException e) {
                break;
            }
        }
        if (preferredNeighbors.size() > 0)
            log.changesPreferredNeighbors(state.peerID, preferredNeighbors);
    }
}