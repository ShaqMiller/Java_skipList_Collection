import java.util.ArrayList;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        Random randomSeed = new Random();
        ArrayList<Integer> ar = new ArrayList<>();

        for(int i=0;i<30;i++){
            Integer num = randomSeed.nextInt(80);
            ar.add(num);
            ar.add(19);
        }
        SkipListSet<Integer> sl = new SkipListSet<>(ar);
        System.err.println("Has "+sl.contains(19));
        System.err.println("Size "+sl.size());

        sl.remove(19);

        System.err.println("Has "+sl.contains(19));
        System.err.println("Size "+sl.size());
        sl.printTree();

    }

   
}


