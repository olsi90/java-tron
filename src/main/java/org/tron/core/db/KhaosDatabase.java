package org.tron.core.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.util.Pair;
import org.tron.core.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;

public class KhaosDatabase extends TronDatabase {

  private class KhaosBlock {

    public Sha256Hash getParentHash() {
      return this.blk.getParentHash();
    }

    public KhaosBlock(BlockCapsule blk) {
      this.blk = blk;
      this.hash = blk.getHash();
      this.num = blk.getNum();
    }

    BlockCapsule blk;
    KhaosBlock parent;
    Sha256Hash hash;
    Boolean invalid;
    long num;
  }

  private class KhaosStore {

    private HashMap<Sha256Hash, KhaosBlock> hashKblkMap = new HashMap<>();
    //private HashMap<Sha256Hash, KhaosBlock> parentHashKblkMap = new HashMap<>();
    private int maxCapcity = 1024;

    private LinkedHashMap<Long, ArrayList<KhaosBlock>> numKblkMap =
        new LinkedHashMap<Long, ArrayList<KhaosBlock>>() {

      @Override
      protected boolean removeEldestEntry(Map.Entry<Long, ArrayList<KhaosBlock>> entry) {
        if (size() > maxCapcity) {
          entry.getValue().forEach(b -> hashKblkMap.remove(b.hash));
          return true;
        }
        return false;
      }
    };


    public void setMaxCapcity(int maxCapcity) {
      this.maxCapcity = maxCapcity;
    }

    public void insert(KhaosBlock block) {
      hashKblkMap.put(block.hash, block);
      //parentHashKblkMap.put(block.getParentHash(), block);
      ArrayList<KhaosBlock> listBlk = numKblkMap.get(block.num);
      if (listBlk == null) {
        listBlk = new ArrayList<KhaosBlock>();
      }
      listBlk.add(block);
      numKblkMap.put(block.num, listBlk);
    }

    public boolean remove(Sha256Hash hash) {
      KhaosBlock block = this.hashKblkMap.get(hash);
      //Sha256Hash parentHash = Sha256Hash.ZERO_HASH;
      if (block != null) {
        long num = block.num;
        //parentHash = block.getParentHash();
        ArrayList<KhaosBlock> listBlk = numKblkMap.get(num);
        if (listBlk != null) {
          listBlk.removeIf(b -> b.hash == hash);
        }
        this.hashKblkMap.remove(hash);
        return true;
      }
      return false;
    }

    public List<KhaosBlock> getBlockByNum(Long num) {
      return numKblkMap.get(num);
    }

    public KhaosBlock getByHash(Sha256Hash hash) {
      return hashKblkMap.get(hash);
    }
  }

  private KhaosBlock head;

  private KhaosStore miniStore;

  private KhaosStore miniUnlinkedStore;

  protected KhaosDatabase(String dbName) {
    super(dbName);
  }

  @Override
  void add() {

  }

  @Override
  void del() {

  }

  @Override
  void fetch() {

  }


  void start(BlockCapsule blk) {
    this.head = new KhaosBlock(blk);
    miniStore.insert(this.head);
  }

  void setHead(KhaosBlock blk) {
    this.head = blk;
  }

  void removeBlk(Sha256Hash hash) {
    if (!miniStore.remove(hash)) {
      miniUnlinkedStore.remove(hash);
    }
  }

  /**
   * check if the hash is contained in the KhoasDB.
    */
  public Boolean containBlock(Sha256Hash hash) {
    if (miniStore.getByHash(hash) != null) {
      return true;
    }
    return miniUnlinkedStore.getByHash(hash) != null;
  }

  /**
   * Get the Block form KhoasDB, if it doesn't exist ,return null.
   */
  public BlockCapsule getBlock(Sha256Hash hash) {
    KhaosBlock block = miniStore.getByHash(hash);
    if (block != null) {
      return block.blk;
    } else {
      KhaosBlock blockUnlinked = miniStore.getByHash(hash);
      if (blockUnlinked != null) {
        return blockUnlinked.blk;
      } else {
        return null;
      }
    }
  }

  /**
   * Push the block in the KhoasDB.
   */
  public BlockCapsule push(BlockCapsule blk) {
    KhaosBlock block = new KhaosBlock(blk);
    if (head != null && block.getParentHash() != Sha256Hash.ZERO_HASH) {
      KhaosBlock kblock = miniStore.getByHash(block.getParentHash());
      if (kblock != null) {
        block.parent = kblock;
      } else {
        //unlinked
        miniUnlinkedStore.insert(block);
        return head.blk;
      }
    }

    miniStore.insert(block);

    if (block == null || block.num > head.num) {
      head = block;
    }
    return head.blk;
  }

  public BlockCapsule getHead() {
    return head.blk;
  }

  /**
   * pop the head block then remove it.
   * @return
   */
  public boolean pop() {
    KhaosBlock prev = head.parent;
    miniStore.remove(head.hash);
    if (prev != null) {
      head = prev;
      return true;
    }
    return false;
  }

  public Pair<Sha256Hash, Sha256Hash> getBranch(Sha256Hash block1, Sha256Hash block2) {
    //TODO find two block's first same parent block
    return new Pair<>(Sha256Hash.ZERO_HASH, Sha256Hash.ZERO_HASH);
  }


}
