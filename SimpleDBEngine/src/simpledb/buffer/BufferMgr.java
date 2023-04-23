package simpledb.buffer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import simpledb.file.*;
import simpledb.log.LogMgr;

/**
 * Manages the pinning and unpinning of buffers to blocks.
 * 
 * @author Edward Sciore
 *
 */
public class BufferMgr {
   /*
    * The class was using the first unpinned buffer it finds, instead of doing
    * something intelligent like LRU.
    * We have implemented LinkedList to use "Least Recently Used" Buffer.
    */

   private LinkedList<Buffer> unpinnedBuffers;
   private Map<BlockId, Buffer> allocatedBuffers;
   private int numBuffers;
   private static final long MAX_TIME = 10000; // 10 seconds

   /**
    * Creates a buffer manager having the specified number
    * of buffer slots.
    * This constructor depends on a {@link FileMgr} and
    * {@link simpledb.log.LogMgr LogMgr} object.
    * 
    * @param numbuffs the number of buffer slots to allocate
    */
   public BufferMgr(FileMgr fm, LogMgr lm, int numbuffs) {
      numBuffers = numbuffs;
      unpinnedBuffers = new LinkedList<Buffer>();
      allocatedBuffers = new HashMap<BlockId, Buffer>();
      for (int i = 0; i < numbuffs; i++) {
         Buffer buffer = new Buffer(fm, lm, i);
         unpinnedBuffers.add(buffer);
      }
   }

   /**
    * Returns the number of available (i.e. unpinned) buffers.
    * 
    * @return the number of available buffers
    */
   public synchronized int available() {
      return unpinnedBuffers.size();
   }

   /**
    * Flushes the dirty buffers modified by the specified transaction.
    * 
    * @param txnum the transaction's id number
    */
   public synchronized void flushAll(int txnum) {
      for (Buffer buff : allocatedBuffers.values())
         if (buff.modifyingTx() == txnum)
            buff.flush();
   }

   /**
    * Unpins the specified data buffer. If its pin count
    * goes to zero, then notify any waiting threads.
    * 
    * @param buff the buffer to be unpinned
    */
   public synchronized void unpin(Buffer buff) {
      buff.unpin();
      if (!buff.isPinned()) {
         unpinnedBuffers.addLast(buff);
         notifyAll();
      }
   }

   /**
    * Pins a buffer to the specified block, potentially
    * waiting until a buffer becomes available.
    * If no buffer becomes available within a fixed
    * time period, then a {@link BufferAbortException} is thrown.
    * 
    * @param blk a reference to a disk block
    * @return the buffer pinned to that block
    */
   public synchronized Buffer pin(BlockId blk) {
      try {
         long timestamp = System.currentTimeMillis();
         Buffer buff = tryToPin(blk);
         while (buff == null && !waitingTooLong(timestamp)) {
            wait(MAX_TIME);
            buff = tryToPin(blk);
         }
         if (buff == null)
            throw new BufferAbortException();
         return buff;
      } catch (InterruptedException e) {
         throw new BufferAbortException();
      }
   }

   public synchronized void printStatus() {
      System.out.println("Allocated Buffers:");
      for (Map.Entry<BlockId, Buffer> entry : allocatedBuffers.entrySet()) {
         Buffer buffer = entry.getValue();
         BlockId blockId = entry.getKey();
         System.out.println("Buffer " + buffer.getId() + ": " + blockId.toString() + " " +
               (buffer.isPinned() ? "pinned" : "unpinned"));
      }

      System.out.println("Unpinned Buffers in LRU order:");
      for (Buffer buffer : unpinnedBuffers) {
         if (!buffer.isPinned()) {
            System.out.print(buffer.getId() + " " + buffer.block() + ", ");
         }
      }
      System.out.println();
   }

   private boolean waitingTooLong(long starttime) {
      return System.currentTimeMillis() - starttime > MAX_TIME;
   }

   /**
    * Tries to pin a buffer to the specified block.
    * If there is already a buffer assigned to that block
    * then that buffer is used;
    * otherwise, an unpinned buffer from the pool is chosen.
    * Returns a null value if there are no available buffers.
    * 
    * @param blk a reference to a disk block
    * @return the pinned buffer
    */
   private Buffer tryToPin(BlockId blk) {
      Buffer buff = allocatedBuffers.get(blk);
      // Block already has a buffer in memory.
      if (buff != null) {
         if (!buff.isPinned()) { // Unpinned
            unpinnedBuffers.remove(buff);
            buff.pin();
         } else {
            System.out
                  .println(buff.getId() + " numbered buffer is already pinned so we can not pin buffer with the block"
                        + blk.number() + ". You should wait until it becomes unpinned");
         }
         return buff;
      } else {
         buff = chooseUnpinnedBuffer();
         if (buff == null) {
            return null;
         }
         allocatedBuffers.remove(buff.block());
         buff.assignToBlock(blk);
         allocatedBuffers.put(blk, buff);
         buff.pin();
         return buff;
      }

   }

   private Buffer chooseUnpinnedBuffer() {
      if (!unpinnedBuffers.isEmpty()) {
         Buffer buff = unpinnedBuffers.remove(0);
         return buff;
      } else {
         return null;
      }
   }

}
