import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;

public class SkipListSet<T extends Comparable<T>> implements SortedSet<T>{
//public class SkipListSet<T extends Comparable<T>> {
    /**
    *  | 4 | --------------> |___| --------------------------------------> |   |
    *  | 3 | --------------> |___| --------------------------------------> |   |
    *  | 2 | --------------> |___| --------------> |____|----------------> |   |
    *  | 1 | --------------> |___| --------------> |____|----------------> |   |
    *  | 0 | ---> | 3 | ---> | 5 | ---> | 7 | ---> | 12 | ---> | 13 | ---> |   |
    **/

    //ITEM CLASS

    private final int MAX_LEVEL = 10;

    // control the current max height, avoid a sudden boost in level
	private Random randomSeed = new Random();

    private SkipListSetItem head; 
    private SkipListSetItem tail; 
    private int size;
	private int currentTopLevel;

    public SkipListSet(){
        this.head = null;
        this.currentTopLevel = 0;
        this.size = 0;
    }


    //PRIVATE CLASSES
    //Item Class for SkipList
    private class SkipListSetItem {

        public ArrayList<SkipListSetItem> nextList;
        public ArrayList<SkipListSetItem> backList;
        private T value;  // Use the same type T for the value as in the outer class
        private int level;

        public SkipListSetItem(T value, int level){
            this.value = value;
            this.level = level;
            this.nextList = new ArrayList<>(level + 1);  // List of next pointers at different levels
            this.backList = new ArrayList<>(level + 1);  // List of previous pointers at different levels

            // Initialize nextList and backList pointers to null
            for (int i = 0; i < level + 1; i++) {
                nextList.add(null);
                backList.add(null);
            }
        }

        public T getValue() {
            return this.value;
        }

        public int getLevel() {
            return this.level;
        }

        public void changeLevel(int newLevel){
            while (this.level < newLevel){
                addNewLevel();
            }
        }

        public void addNewLevel(){
            this.nextList.add(null);
            this.backList.add(null);
            this.level++;
        }

        @Override
        public String toString() {
            return "[ level " + level + " | value " + value + " ]";
        }
    }


    //Iterator class for SkipList
    private class SkipListSetIterator implements Iterator<T> {
        private SkipListSetItem current; // Points to the current node in iteration

        // Constructor
        public SkipListSetIterator() {
            this.current = head.nextList.get(0); // Start at the first actual element
        }

        // Check if there are more elements
        @Override
        public boolean hasNext() {
            return current != null && current != tail; // Valid element and not the tail
        }

        // Get the next element
        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more elements in the skip list.");
            }
            T value = current.getValue(); // Retrieve the value of the current node
            current = current.nextList.get(0); // Move to the next node
            return value;
        }

        // Remove the current element
        @Override
        public void remove() {
            if (current == null || current == head || current == tail) {
                throw new IllegalStateException("Cannot remove an element here.");
            }

            SkipListSet.this.remove(current.getValue()); // Use the outer class's remove method
        }
    }
    

    @Override
    public Iterator<T> iterator() {
        return new SkipListSetIterator();
    }
    
    /* 
     * Methods from SortedSet<T>: 
    */
    @Override
    public T first(){
        if(head!=null){
            return head.nextList.get(0).getValue();
        }
        return null;
    }
    @Override
    public T last(){
        if(tail!=null){
            return tail.backList.get(0).getValue();
        }
        return null;
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        throw new UnsupportedOperationException("headSet operation is not supported");
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        throw new UnsupportedOperationException("tailSet operation is not supported");
    }
    @Override
    public Comparator<? super T> comparator() {
        return null; 
    }

    
    //SET Methods
    public SkipListSet(Collection<T> collection){
        this(); //  default constructor 
        
        // Add each element from the collection to the skip list
        for (T item : collection) {
            this.add(item); 
        }
    }
    @Override
    public boolean add(T value){
        if(value == null) return false;

        //Get randomized height level
        int level = coinFlipForLevel();
        //Call the actual insert with random level
        return this.add(value,level);
    }

    //Wrapper for inserting with a random level
    public boolean add(T value,int level){
        System.err.println("Adding :"+value+" At level: "+level);

        SkipListSetItem newNode = new SkipListSetItem(value, level);
        //If first element
        if(head == null){
            //Use my head as a node that has no data but has the levels as my highest level(first node)
            head = new SkipListSetItem(null, this.currentTopLevel);
            tail = new SkipListSetItem(null, this.currentTopLevel);

            //Link all levels of head  to this node since first
            for(int i=0;i<(level+1);i++){
                head.nextList.set(i, newNode);
                newNode.backList.set(i, head);

                newNode.nextList.set(i, tail);
                tail.backList.set(i, newNode);
            }
            size++;
            return true;
        }

        //Search for the specific entry spot for this node
        int currentSearchingLevel = currentTopLevel; //Starting from top level
        SkipListSetItem curNode = head;

        while(curNode!=tail){
            
            //Get node to the right of this current level
            SkipListSetItem nextNode = curNode.nextList.get(currentSearchingLevel);
            
            //if nextNode == null || nextNode compared > value we move down
            if(nextNode == tail || greaterThan(nextNode.getValue(), value)){
                //Move down a level if we are not at the bottom
                if(currentSearchingLevel != 0){
                    currentSearchingLevel--;
                    continue; //get next node again
                }else{
                    //insert node here
                    handlePlacingNewNode(curNode,newNode);
                    size++;
                    return true;
                }

            }else if (equalTo(nextNode.getValue(), value)){ 
                //Do nothibg if equal
                break;
            }else{//nextnode is less than newNode
                curNode = nextNode;
            }

        }
        
        return false;
    }
    
    public boolean addAll(Collection<? extends T> collection) {
        // Add each element from the collection to the skip list
        for (T item : collection) {
            this.add(item); 
        }
        return true;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        // If the object is null, it cannot be in the set
        if (o == null) {
            return false;
        }

        // Check if the object is of the correct type
        if (!(o instanceof Comparable<?>)) {
            return false;
        }

        try {
            // Cast to my type
            T value = (T) o;
            
            //Now simply check if value is in my list
            return contains(value);
        } catch (ClassCastException e) {
            // casting failed
            return false;
        }
    }
   
    public boolean contains(T value){

        if(head == null){
            return false;
        }

        //Search for this node
        int currentSearchingLevel = currentTopLevel; //Starting from top level
        SkipListSetItem curNode = head;

        while(curNode!=tail){
            
            //Get node to the right of this current level
            SkipListSetItem nextNode = curNode.nextList.get(currentSearchingLevel);
            
            //if nextNode == null || nextNode compared > value we move down
            if(nextNode == tail || greaterThan(nextNode.getValue(), value)){
                //Move down a level if we are not at the bottom
                if(currentSearchingLevel != 0){
                    currentSearchingLevel--;
                    continue; //get next node again
                }else{
                    return false; //At bottom and still hasnt found
                }

            }else if (equalTo(nextNode.getValue(), value)){ 
                //Found
                return true;
            }else{//nextnode is less than newNode
                curNode = nextNode;
            }
        }
        
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c){
        // Iterate through the collection `c`
        for (Object item : c) {
            // Check if this set contains each item in `c`
            if (!this.contains(item)) {
                return false; // If any item is missing, return false
            }
        }
        return true; 
    }

    @Override
    public boolean equals(Object o) {
        // Check if the given object is the same as this set
        if (this == o) {
            return true;
        }

        // Check if the given object is null or not a Set
        if (!(o instanceof Set<?>)) {
            return false;
        }

        // Cast the object to a Set
        Set<?> otherSet = (Set<?>) o;

        // Check if the sizes are different
        if (this.size() != otherSet.size()) {
            return false;
        }

        // Check if all elements in this set are in the other set
        return this.containsAll(otherSet);
    }

    public boolean retainAll(Collection<?> c){
        if(c == null) return false;
        if(head == null) return false;

        boolean isChangedList = false;

        // Iterate over the elements of the set, do base level so that i hit every element
        SkipListSetItem current = head.nextList.get(0);
        while(current!=tail || current!=null){ //shouldnt rlly have to care about null
            T value = current.getValue();

            // If the value is not in the collection, remove it
            if (!c.contains(value)) {
                SkipListSetItem next = current.nextList.get(0); // Save reference to the next node
                this.remove(value); // Remove the current value
                isChangedList = true;
                current = next; // Move to the next node
            } else {
                current = current.nextList.get(0); // Move to the next node
            }
        }

        return isChangedList;
    }
    
    public boolean remove(T value){
        //If no elements in list
        if(head == null){
            return false;
        }

        int currentSearchingLevel = currentTopLevel; //Starting from top level
        SkipListSetItem curNode = head;

        while(curNode!=tail){
            
            //Get node to the right of this current level
            SkipListSetItem nextNode = curNode.nextList.get(currentSearchingLevel);
            
            //if nextNode == null || nextNode compared > value we move down
            if(nextNode == tail || greaterThan(nextNode.getValue(), value)){
                //Move down a level if we are not at the bottom
                if(currentSearchingLevel != 0){
                    currentSearchingLevel--;
                    continue; //get next node again
                }else{
                    curNode = nextNode;
                }
                    
            }else if (equalTo(nextNode.getValue(), value)){ 
                //Delete if equal
                handleDeletingNode(curNode,nextNode,nextNode.nextList.get(currentSearchingLevel));
                size--;
                return true;
            }else{//nextnode is less than newNode
                curNode = nextNode;
            }

        }
        
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(Object o) {
        if (o == null) {
            return false;
        }

        // Shoudl be comparable
        if (!(o instanceof Comparable<?>)) {
            return false;
        }

        try {
            //Cast to my type
            T value = (T) o;

            // Use your existing remove method for the actual removal
            return this.remove(value);
        } catch (ClassCastException e) {
            //Typecast fail
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public boolean removeAll(Collection<?> c){
        // If the collection is null, throw a NullPointerException
        if (c == null) {
            return false;
        }

        boolean modified = false;
        // Iterate through the collection
        for (Object item : c) {
            // If in my set remove it
            if (this.contains(item)) {
                this.remove((T) item);
                modified = true;
            }
        }

        return modified;
    }

    @Override
    public void clear(){
        head = null;
        tail = null;
        size=0;
    }  

    @Override
    public boolean isEmpty(){
        if(size > 0) return true;
        return false;
    }
    @Override
    public Object[] toArray() {
        Object[] array = new Object[size]; // Create an array of the size of the set
        int index = 0;

        // Iterate through the elements at the bottom level of the skip list so i can reach every element
        SkipListSetItem current = head.nextList.get(0);
        while (current != tail && current != null ) {
            array[index] = current.getValue();
            index++;
            current = current.nextList.get(0);
        }

        return array;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> E[] toArray(E[] a) {
        // If the provided array is too small, create a new array of the same runtime type
        if (a.length < size) {
            a = (E[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
        }

        int index = 0;

        // Iterate through the elements at the bottom level of the skip list
        SkipListSetItem current = head.nextList.get(0);
        while (current != null && current != tail) {
            a[index++] = (E) current.getValue();
            current = current.nextList.get(0);
        }

        // If the provided array is larger than the set, set the next element to null
        if (a.length > size) {
            a[size] = null;
        }

        return a;
    }



    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        if (fromElement == null || toElement == null) {
            throw new NullPointerException("Bounds cannot be null");
        }
        if (fromElement.compareTo(toElement) > 0) {
            throw new IllegalArgumentException("fromElement must be <= toElement");
        }

        SkipListSet<T> subSet = new SkipListSet<>(); // Create a new skip list for the subset

        SkipListSetItem current = head;

        // Traverse from the head to find the starting point (fromElement)
        while (current != null && current != tail) {
            if (greaterThan(current.getValue(), fromElement) || equalTo(current.getValue(), fromElement)) {
                break;
            }
            current = current.nextList.get(0); // Move to the next node at the lowest level
        }

        // Collect all elements in the range [fromElement, toElement)
        while (current != null && current != tail) {
            if (lessThan(current.getValue(), toElement)) {
                subSet.add(current.getValue());
            } else {
                break;
            }
            current = current.nextList.get(0);
        }

        return subSet;
    }


    public int getHeight(){
        return currentTopLevel;
    }

    public int size(){
        return size;
    }

    // Helper functions
    private int coinFlipForLevel(){
        boolean isHead;
        int curLevel = 0;

        //Keep flipping coin and increasing level until tails
        //For me heads = true , tails = false
        for(int i=0;i<MAX_LEVEL;i++){
            isHead = randomSeed.nextBoolean();

            if(isHead){
                //Incriment level
                curLevel++;

                //check if we now have a new Top Level
                if(curLevel > currentTopLevel){
                    currentTopLevel = curLevel;
                    //increase the headerNode list to be this new height
                    if(head!=null)
                        this.changeHeaderAndTailLevel(currentTopLevel);
                }

                //break if at the MAX_LEVEL that was set
                if(curLevel == MAX_LEVEL){
                    break;
                }
            }else{
                //Is tails so finish
                break;
            }
        }
        return curLevel;
    }   
    
    private boolean lessThan(T a, T b) {
		return a.compareTo(b) < 0;
	}

	private boolean equalTo(T a, T b) {
		return a.compareTo(b) == 0;
	}

	private boolean greaterThan(T a, T b) {
		return a.compareTo(b) > 0;
	}

    public void  showTreeLevel(int level){
        SkipListSetItem start = head;
        while(start!=null){
            
            System.err.print(start.getValue() +"-->");
            start = start.nextList.get(level);
        }
    }

    public void printTree(){
        for(int j=this.getHeight();j>=0;j--){
            this.showTreeLevel(j);
            System.err.println("");
        }
    }

    private void handleDeletingNode(SkipListSetItem behind,SkipListSetItem foundNode,SkipListSetItem infront){
        int foundNodeLevel = foundNode.getLevel();
        for(int i=foundNodeLevel;i>=0;i--){
            behind.nextList.set(i, infront);
            infront.backList.set(i, behind);
        }
    }
    private void handlePlacingNewNode(SkipListSetItem behind,SkipListSetItem infront){

        int behindNodeLevel = behind.getLevel();
        //We start from the level of the node infront since we wont adjust anything above that
        //for example if behindNode starts at level5 but infron starts level 3 , we only adjust the pointers starting at level 3
        for(int i=infront.getLevel();i>=0;i--){

            //get level of one behind
            if(i>behindNodeLevel){
                SkipListSetItem curNode = head;
                while(curNode != tail){
                    SkipListSetItem nextNode = curNode.nextList.get(i);
                    if(nextNode == tail || greaterThan(nextNode.getValue(), infront.getValue())){
                        //new node pointers
                        infront.nextList.set(i,nextNode);
                        infront.backList.set(i, curNode);

                        //old node pointers
                        curNode.nextList.set(i,infront);
                        break;
                    }else{
                        curNode = nextNode;
                    }
                }
            }else{
                //new node pointers
                infront.nextList.set(i, behind.nextList.get(i));
                infront.backList.set(i, behind);

                //old node pointers
                behind.nextList.set(i, infront);
            }

            
        }
    }

    private void changeHeaderAndTailLevel(int newHeight){
        int oldHeight = head.getLevel();
        head.changeLevel(newHeight);
        tail.changeLevel(newHeight);
        for(int i=newHeight;i>oldHeight;i--){
            head.nextList.set(i,tail);
            tail.backList.set(i,head);
        }
    }
}