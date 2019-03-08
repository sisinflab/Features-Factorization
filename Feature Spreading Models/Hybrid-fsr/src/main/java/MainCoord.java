import BiasedMF.MainBiasedMF;
import GraphSpreadingSpaceModel.App;
import ItemsAttributesManager.ItemsAttributesManager;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class MainCoord {
    private static String filePath = "";
    private static String outPath = "";
    private static String fileAttrPath = "";
    private static String featuresNames = "";
    private static String propertiesPath = "";

    private static int nRecc = 100;
    private static int threshold = 0;


    public static void main(String[] args) throws IOException {
        for (int i = 0; i < args.length; i++) {
            System.out.println(args[i]);
            if (i==0)filePath = args[i];
            if (i==1)fileAttrPath = args[i];
            if (i==2)outPath = args[i];
            if (i==3)nRecc = Integer.valueOf(args[i]);
            if (i==4)threshold = Integer.valueOf(args[i]);
        }
        System.out.println("Content");

        HashMap<Integer, ArrayList<Integer>> map = ItemsAttributesManager.loadMap(filePath,fileAttrPath,featuresNames,threshold,propertiesPath,true);
        HashMap<Integer, HashMap<Integer, Float>> ratingsMap = ItemsAttributesManager.loadRatings(filePath,map);


        HashMap<Integer, HashMap<Integer, Float>> profiles = App.useContent(ratingsMap,map,nRecc);

        HashMap<Integer, HashMap<Integer, Float>> badProfiles = MainBiasedMF.useBiasedMF(profiles);

        HashMap<Integer, ArrayList<AbstractMap.SimpleEntry<Integer, Float>>> recs = App.useContentPostCollaborative(ratingsMap,map,badProfiles,nRecc);

        Map<Integer, ArrayList<AbstractMap.SimpleEntry<Integer, Float>>> results = recs.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        PrintWriter out= new PrintWriter(new FileWriter(outPath));
        results.entrySet().stream().forEachOrdered(e -> {
            e.getValue().stream().forEachOrdered(e2 -> {
                out.println(e.getKey() + "\t" + e2.getKey() + "\t" + e2.getValue());
            });
        });
        out.close();

    }

}
