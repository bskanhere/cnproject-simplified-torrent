package cn.torrent;

import cn.torrent.exceptions.AlreadyHavePieceException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;

public class BitField {
  public int haveCounter = 0;
  private final ArrayList<PieceStatus> bitset;

  public static BitField from(final int size, final boolean isFull) {
    return new BitField(size, isFull);
  }

  private BitField(final int size, final boolean isFull) {
    bitset = new ArrayList<>(Arrays.asList(new PieceStatus[size]));
    if (isFull) {
      haveCounter = size;
    }
    if (isFull) {
      Collections.fill(bitset, PieceStatus.HAVE);
    } else {
      Collections.fill(bitset, PieceStatus.MISSING);
    }
  }

  public PieceStatus getStatus(final int index) {
    return bitset.get(index);
  }

  public byte[] toByteArray() {
    BitSet set = new BitSet(bitset.size());
    for (int i = 0; i < bitset.size(); i++) {
      if (bitset.get(i) == PieceStatus.HAVE) {
        set.set(i);
      }
    }
    return set.toByteArray();
  }

  public void setBitField(final byte[] bytes) {
    BitSet set = BitSet.valueOf(bytes);
    haveCounter = 0;
    for (int i = 0; i < bitset.size(); i++) {
      if (set.get(i)) {
        bitset.set(i, PieceStatus.HAVE);
        haveCounter++;
      } else {
        bitset.set(i, PieceStatus.MISSING);
      }
    }
  }

  public void setMissing(final int index) throws AlreadyHavePieceException {
    if (bitset.get(index) != PieceStatus.HAVE) {
      bitset.set(index, PieceStatus.MISSING);
    } else {
      throw new AlreadyHavePieceException();
    }
  }

  public void setRequested(final int index) throws AlreadyHavePieceException {
    if (bitset.get(index) != PieceStatus.HAVE) {
      bitset.set(index, PieceStatus.REQUESTED);
    } else {
      throw new AlreadyHavePieceException();
    }
  }

  public void setHave(final int index) throws AlreadyHavePieceException {
    if (bitset.get(index) != PieceStatus.HAVE) {
      bitset.set(index, PieceStatus.HAVE);
      haveCounter++;
    } else {
      throw new AlreadyHavePieceException();
    }
  }

  public boolean isFull() {
    return haveCounter == bitset.size();
  }
}

enum PieceStatus {
  MISSING,
  REQUESTED,
  HAVE
}
