import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * JSON hashes/objects.
 */
public class JSONHash implements JSONValue {

  // +--------+------------------------------------------------------
  // | Fields |
  // +--------+

  /**
   * The load factor for expanding the table.
   */
  static final double LOAD_FACTOR = 0.5;

  /**
   * The number of values currently stored in the hash table. We use this to
   * determine when to expand the hash table.
   */
  int size = 0;

  /**
   * The array that we use to store the ArrayList of key/value pairs. (We use an
   * array, rather than an ArrayList, because we want to control expansion and
   * ArrayLists of ArrayLists are just weird.)
   */
  Object[] buckets;

  /**
   * Our helpful random number generator, used primarily when expanding the size
   * of the table..
   */
  Random rand;

  static int INITIAL_SIZE = 10;

  // +--------------+------------------------------------------------
  // | Constructors |
  // +--------------+

  public JSONHash() {
    this.rand = new Random();
    this.size = 0;
    this.buckets = new Object[INITIAL_SIZE];
  }

  // +-------------------------+-------------------------------------
  // | Standard object methods |
  // +-------------------------+

  /**
   * Convert to a string (e.g., for printing).
   */
  public String toString() {
    String str = "";
    while (this.iterator().hasNext()) {
      str = str + this.iterator().next().toString();
    }
    return str;
  } // toString()

  /**
   * Compare to another object.
   */
  public boolean equals(Object other) {
    if (toString().equals(other.toString())) {
      return true;
    } else {
      return false;
    }
  } // equals(Object)

  /**
   * Compute the hash code. // credit Osera
   */
  public int hashCode() {
    int result = 920;
    while (this.iterator().hasNext()) {
      int c = this.iterator().next().hashCode();
      result = 31 * result + c;
    }
    return result;
  } // hashCode()

  // +--------------------+------------------------------------------
  // | Additional methods |
  // +--------------------+

  /**
   * Write the value as JSON.
   */
  public void writeJSON(PrintWriter pen) {
    while (this.iterator().hasNext()) {
      pen.print(this.iterator().next());
    }
  } // writeJSON(PrintWriter)

  /**
   * Get the underlying value.
   */
  public Iterator<KVPair<JSONString, JSONValue>> getValue() {
    return this.iterator();
  } // getValue()

  // +-------------------+-------------------------------------------
  // | Hashtable methods |
  // +-------------------+

  /**
   * Get the value associated with a key.
   */
  public JSONValue get(JSONString key) {
    int index = find(key);
    @SuppressWarnings("unchecked")
    ArrayList<KVPair<JSONString, JSONValue>> alist = (ArrayList<KVPair<JSONString, JSONValue>>) buckets[index];
    if (alist == null) {
      throw new IndexOutOfBoundsException("Invalid key: " + key);
    } else {
      KVPair<JSONString, JSONValue> pair;
      for (int i = 0; i < alist.size(); i++) {
        pair = alist.get(i);
        if (pair.key().equals(key)) {
          return pair.value();
        }
      }
      throw new IndexOutOfBoundsException("Invalid key: " + key);
    } // get
  } // get(JSONString)

  /**
   * Get all of the key/value pairs.
   */
  public Iterator<KVPair<JSONString, JSONValue>> iterator() {

    return new Iterator<KVPair<JSONString, JSONValue>>() {
      
      //Fields

      // position inside the bucket
      int curBucket = 0;

      int curValue = 0;
      

      public boolean hasNext() {
        if (this.curBucket < JSONHash.this.buckets.length) {
          return true;
        } else {
          ArrayList<KVPair<JSONString, JSONValue>> buck = (ArrayList<KVPair<JSONString, JSONValue>>)JSONHash.this.buckets[curBucket];
          if (this.curValue < buck.size() - 1) {
            return true;
          } else {
            return false;
          }
        }
      }

      public KVPair<JSONString, JSONValue> next() {
        // if we are at the end of bucket, go to next bucket
        if (hasNext()) {
          ArrayList<KVPair<JSONString, JSONValue>> buck = (ArrayList<KVPair<JSONString, JSONValue>>)JSONHash.this.buckets[curBucket];
          if (this.curValue == buck.size() - 1) {
            curBucket++;
            this.curValue = 0;
            ArrayList<KVPair<JSONString, JSONValue>> nextBucket = (ArrayList<KVPair<JSONString, JSONValue>>)JSONHash.this.buckets[curBucket];
            return nextBucket.get(curValue);
          } else {
            // else we give the next value and move cursor to next value in the bucket
            this.curValue++;
            return buck.get(curValue);
          }            
        } else {
          return null;
        }            
      }
    };
  } //Iterator
 
  /**
   * Set the value associated with a key.
   */
  public void set(JSONString key, JSONValue value) {
    // If there are too many entries, expand the table.
    if (this.size > (this.buckets.length * LOAD_FACTOR)) {
      expand();
    } // if there are too many entries

    // Find out where the key belongs and put the pair there.
    int index = find(key);
    ArrayList<KVPair<JSONString, JSONValue>> alist = (ArrayList<KVPair<JSONString, JSONValue>>) this.buckets[index];
    // Special case: Nothing there yet
    if (alist == null) {
      alist = new ArrayList<KVPair<JSONString, JSONValue>>();
      this.buckets[index] = alist;
    }

    KVPair<JSONString, JSONValue> pair;
    for (int i = 0; i < alist.size(); i++) {
      pair = alist.get(i);
      if (pair.key().equals(key)) {
        alist.set(i, new KVPair<JSONString, JSONValue>(key, value));
      }
    }
    alist.add(new KVPair<JSONString, JSONValue>(key, value));
    ++this.size;
  } // set(JSONString, JSONValue)

  /**
   * Find out how many key/value pairs are in the hash table.
   */
  public int size() {
    return this.size;
  } // size()

  /**
   * Find the index of the entry with a given key. If there is no such entry,
   * return the index of an entry we can use to store that key.
   */
  int find(JSONValue key) {
    return Math.abs(key.hashCode()) % this.buckets.length;
  } // find(K)

  /**
   * Expand the size of the table.
   */
  void expand() {
    // Figure out the size of the new table
    int newSize = 2 * this.buckets.length + rand.nextInt(10);

    // Remember the old table
    Object[] oldBuckets = this.buckets;
    // Create a new table of that size.
    this.buckets = new Object[newSize];
    // Move all buckets from the old table to their appropriate
    // location in the new table.
    ArrayList<KVPair<JSONString, JSONValue>> alist;
    KVPair<JSONString, JSONValue> pair;
    for (int i = 0; i < oldBuckets.length; i++) {
      alist = (ArrayList<KVPair<JSONString, JSONValue>>) oldBuckets[i];
      if (alist == null) {
        continue;
      }
      for (int j = 0; j < alist.size(); j++) {
        pair = alist.get(j);
        set(pair.key(), pair.value());
        this.size--;
      }
    }
  } // expand()

  /**
   * Clear the whole table.
   */
  public void clear() {
    this.buckets = new Object[INITIAL_SIZE];
    this.size = 0;
  } // clear()

} // class JSONHash
