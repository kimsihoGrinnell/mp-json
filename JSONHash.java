import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * JSON hashes/objects.
 * 
 * @author Samuel A. Rebelsky
 * @author Candice Lu
 * @author Siho Kim
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
    Iterator<KVPair<JSONString, JSONValue>> itr = this.iterator();
    while (itr.hasNext()) {
      str = str + itr.next().toString() + "\n";
    }
    return str;
  } // toString()

  /**
   * Compare to another object.
   */
  public boolean equals(Object other) {
    if (other instanceof JSONValue) {
      JSONHash otherHash = (JSONHash) other;
      int equality = 0;
      Iterator<KVPair<JSONString, JSONValue>> itr = this.iterator();

      while (itr.hasNext()) {

        KVPair<JSONString, JSONValue> tempPair = itr.next();
        if (otherHash.get(tempPair.key()).equals(tempPair.value())) {
          equality++;
          System.out.println(equality);
        }
      }
      return equality == this.size;
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

  // +----------+-------------------------------------------
  // | Iterator |
  // +----------+

  public Iterator<KVPair<JSONString, JSONValue>> iterator() {

    return new Iterator<KVPair<JSONString, JSONValue>>() {

      // +--------+------------------------------------------------------
      // | Fields |
      // +--------+

      /**
       * position inside the bucket array
       */
      int curBucket = 0;

      /**
       * position inside the bucket
       */
      int curValue = -1;

      /**
       * counter to track how many KVPairs we have already iterated through
       */
      int counter = 0;

      /*
       * returns true if there are still KVPairs in remaining buckets
       */
      public boolean hasNext() {
        return counter < JSONHash.this.size();
      } // hasNext()

      /*
       * returns next KVPair in the JSONHash
       */
      public KVPair<JSONString, JSONValue> next() {
        ArrayList<KVPair<JSONString, JSONValue>> bucket = (ArrayList<KVPair<JSONString, JSONValue>>) buckets[curBucket];

        // If bucket is null, skip forward to the next non-null bucket
        while (bucket == null) {
          curBucket++;
          bucket = (ArrayList<KVPair<JSONString, JSONValue>>) buckets[curBucket];
        }

        // if we return something, we increment counter
        counter++;

        curValue++;
        // If there is a value ahead
        if (curValue < bucket.size()) {
          return bucket.get(curValue);
        } else {
          // Next Bucket
          curBucket++;
          bucket = (ArrayList<KVPair<JSONString, JSONValue>>) buckets[curBucket];

          // If bucket is empty, find a new one
          while (bucket == null) {
            curBucket++;
            bucket = (ArrayList<KVPair<JSONString, JSONValue>>) buckets[curBucket];
          }

          curValue = 0;
          return bucket.get(curValue);
        }
      } // next()
    };
  } // Iterator

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
        return;
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
