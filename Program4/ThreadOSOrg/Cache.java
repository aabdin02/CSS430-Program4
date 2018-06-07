/*
Aisha A. Abdinur
*/

import java.util.Vector;

public class Cache{

 CacheBlock[] cache; 
 int cacheSize, blockSize;
  private class CacheBlock{
    private int frameNumber;
    private boolean referenceBit;
    private boolean dirtyBit;
    private byte[] data;

  public CacheBlock(int frameID){
      this.frameNumber = frameID;
      this.referenceBit = false;
      this.dirtyBit = false;
      this.data = new byte[blockSize];
    }
  }
 

  public Cache(int blockSiz, int cacheBlocks){

    this.cache = new CacheBlock[cacheBlocks];   
    this.cacheSize = cacheBlocks;         
    this.blockSize = blockSiz;           

    for(int i = 0; i < cacheSize; i++){
     this.cache[i]  = new CacheBlock(blockSiz);
    } 
  }
private void writeDisk(int target){
    if(cache[target].dirtyBit && cache[target].frameNumber != -1){
      SysLib.rawwrite(cache[target].frameNumber, cache[target].data);
      cache[target].dirtyBit = false;     
    }
}

private void addCache(int target, int blockId, byte[] buffer){
    System.arraycopy(buffer, 0, cache[target].data, 0, blockSize);
    cache[target].dirtyBit = true;     
    updateCache(target, blockId, true);
  }

  //Returns the index of the cacheBlock 
  private int searchID(int ID){
    boolean found = false;int emptyIndex = -1;
    for(int index = 0; index < cacheSize; index++){     
      if(cache[index].frameNumber == ID){ 
        return index;             
      }
      if(!found && (cache[index].frameNumber == -1)){
        found = true;
        emptyIndex = index;
      }
    }

    return emptyIndex;  //Couldn't find ID             
  }
  private void readCache(int target, int blockId, byte[] buffer){
    System.arraycopy(cache[target].data, 0, buffer, 0, blockSize);
    updateCache(target, blockId, true);  
  }
  private void updateCache(int target, int frame, boolean refBit){
      cache[target].frameNumber = frame;       //Save this frame number
      cache[target].referenceBit = refBit;     //update last reference
  }

  public synchronized boolean write(int blockId, byte[] buffer){
     if(blockId < 0)return false;    
     int target = searchID(blockId); 
     if(target < 0){//Cache is Full
      //Use Enhanced Second Chance Algorithm  
      int victim = findVictim();
      writeDisk(victim);         //Find a victim, save first
      addCache(victim, blockId, buffer);    //Add to cache  
      return true;
    }
    if(target >= 0){ 
      addCache(target, blockId, buffer); 
      return true; 
    }   
    if(cache[target].frameNumber == -1){            
      addCache(target, blockId, buffer);
      return true; 
    }   
    
 
    return false;             
  }

  public synchronized boolean read(int blockId, byte[] buffer){
    if(blockId < 0)return false;    
    int target = searchID(blockId);  
    if(target < 0){//Cache is Full
      //Use Enhanced Second Chance Algorithm  
      int victim = findVictim();
      writeDisk(victim);         //Find a victim, save first
      SysLib.rawread(blockId, cache[victim].data);  
      readCache(victim, blockId, buffer);   //read from the cache block 
      return true;  
    }

    if(cache[target].frameNumber == -1){            
      SysLib.rawread(blockId, cache[target].data);
      readCache(target, blockId, buffer); //If found, read from cache
      return true;
    }      
    if(target >= 0){            
      readCache(target, blockId, buffer); 
      return true; 
    }     
    return false;
  }

  //modified(dirty), accessed(referenced)
  private Vector<Vector<Integer>> createCases(){
    Vector<Vector<Integer>> cases = new Vector<>();
    Vector<Integer> case0 = new Vector<>();
    Vector<Integer> case1 = new Vector<>();
    Vector<Integer> case2 = new Vector<>();
    Vector<Integer> case3 = new Vector<>();

    for(int i = 0; i < this.cacheSize; i++){
      if(!cache[i].dirtyBit && !cache[i].referenceBit){
        case0.add(i);
      }
      if(!cache[i].dirtyBit && cache[i].referenceBit){
        case1.add(i);
      }
      if(!cache[i].dirtyBit && cache[i].referenceBit){
        case2.add(i);
      }
      else{
        case3.add(i);
      }
    }
    cases.add(case0);
    cases.add(case1);
    cases.add(case2);
    cases.add(case3);

    return cases;
  }
  private int findVictim(){
    Vector<Vector<Integer>> cases = createCases();
    for(int i = 0; i < cases.size(); i++){
      if(cases.get(i).size() > 0){
        cache[cases.get(i).get(0)].referenceBit = false;
      return cases.get(i).get(0);
      }
    }
    return -1;
  }

  public synchronized void sync(){
    for(int i = 0; i < cacheSize; i++) { writeDisk(i); }
    SysLib.sync();                //Sync to disk
  }

  public synchronized void flush(){
    for(int i = 0; i < cacheSize; i++){      //Loop over all pages
      writeDisk(i);             
      updateCache(i, -1, false);      
    } 
    SysLib.sync();                //Sync to disk
  }
}