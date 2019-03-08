package GraphSpreadingSpaceModel;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class User {

    /** active user */
    private int activeUser;
    /**features map
    //o -- co sum(avgsingle) orate weight sumWeightedFeature sumWeightedMovies*/
    private Map<Integer, ArrayList<Float>> o = new HashMap<>();
    /** items in user profile */
    private int total = 0;
    /** overall relevances of items in user's profile */
    private HashMap<Integer, Float> moviesWeights = new HashMap();
    /** overall relevances of items in the collection */
    private HashMap<Integer, Float> allmoviesWeights = new HashMap();
    /** User and Votes stats */
    private double userStats[] = new double[3];
    private double voteStats[] = new double[3];
    /** Estimated ratings map */
    private Map<Integer, Float> m2 = new HashMap<>();
    private int numberRecs;
    private Map<Integer, ArrayList<Integer>> map;
    /** User rating map */
    private Map<Integer, Float> votes;
    private List<AbstractMap.SimpleEntry<Integer, Float>> recs = new ArrayList<>();
    private HashMap<Integer, Float> userProfile = new HashMap<>();

    public User(HashMap<Integer, ArrayList<Integer>>  map, int user, Map<Integer, Float> votes, int numberRecs) {
        this.map = map;
        this.votes = votes;
        this.activeUser = user;
        this.numberRecs = numberRecs;
    }

    public void process(){
        total = votes.size();

        computeFeaturesRelevance();
        normalizeFeaturesRelevance();
  	    /////////////////
        computeItemsRelevanceUsers();
        sum_RelevanceItemDotRatingItem_perFeature();
        normalizeFeaturesRating();
  	    //////////////////
        computeItemsRelevanceCollection();
        userProfile = getProfile();
        generalRankings();
        sortRankings();
    }

    public HashMap<Integer, Float> getProfile(){
        ArrayList<Double> weightArray = o.values().stream().map(feature -> (double)feature.get(2)*feature.get(3)).collect(Collectors.toCollection(ArrayList::new));
        double mean = stats(weightArray)[0];

        HashMap<Integer, Float> profile = new HashMap<>();
        for (Map.Entry<Integer, ArrayList<Float>> entry : o.entrySet()){
            double featureVal = entry.getValue().get(2)*entry.getValue().get(2);
            if(featureVal > mean){
                profile.put(entry.getKey(), entry.getValue().get(2)*entry.getValue().get(2));
            }
        }
        return profile;
    }

    public ArrayList<AbstractMap.SimpleEntry<Integer, Float>> getRecs(){
        return (ArrayList<AbstractMap.SimpleEntry<Integer, Float>>) recs;

    }

    private float computeJaccard(List<Integer> userFeatures, ArrayList<Integer> itemFeatures){
        int unionSize = Stream.concat(userFeatures.stream(), itemFeatures.stream()).distinct().collect(Collectors.toList()).size();
        int intersectionSize = userFeatures.stream().filter(itemFeatures::contains).collect(Collectors.toList()).size();

        float den = unionSize;
        return (den!=0)?intersectionSize/den:0f;
    }

    private  void sortRankings(){
        recs = m2.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).limit(numberRecs)
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(),e.getValue()))
                .collect(Collectors.toList());
    }

    private  void generalRankings(){
        //it predicts ratings for each item in allmovieweights
        List<Integer> profile = new ArrayList<>(userProfile.keySet());
        allmoviesWeights.entrySet().stream().forEach(item -> {
            m2.put(item.getKey(),computeJaccard(profile,map.get(item.getKey())));
        });
    }

    @SuppressWarnings("Duplicates")
    private float score(int item){
        ArrayList<Integer> itemFeatures = map.get(item);
        AtomicReference<Float> sumWeightedFeatures = new AtomicReference<>(0f);
        AtomicReference<Float> sumWeigts = new AtomicReference<>(0f);
        HashSet<Integer> bag = new HashSet<Integer>();
        itemFeatures.stream().forEach(feature -> {
            if (!bag.contains(feature)){
                ArrayList<Float> featureStats = o.get(feature);
                if(featureStats!=null){
                    float featImportance = (float)applyRelevanceThreshold(featureStats);
                    sumWeightedFeatures.set(sumWeightedFeatures.get() + featureStats.get(2) * featImportance);
                    sumWeigts.set(sumWeigts.get() + featImportance);
                    bag.add(feature);
                }
            }
        });
        return sumWeightedFeatures.get();
    }

    @SuppressWarnings("Duplicates")
    private double applyRelevanceThreshold(ArrayList<Float> featureStats){
        double featImportance = (float)0.0;
        if ((featureStats.get(0)==(float)0.0)){
            //collaborative features
            //it could be used to apply a threshold to collaborative features
            if ((featureStats.get(3)>(float)0.1)&&(featureStats.get(2)>(voteStats[0]+voteStats[1]*1.0))){
                featImportance = featureStats.get(3);
            }
        }else{
            //original features
            //it could be used to apply a threshold to original features
            if ((featureStats.get(2)>(voteStats[0]+voteStats[1]*0.0))){
                featImportance = featureStats.get(3);
            }
        }
        return featImportance;
    }

    private  double[] stats(ArrayList<Double> vec){
        int n = vec.size();
        double mean = 0;
        double sum = 0;
        double stdDev = 0;
        double min = 0;
        for(Double d : vec)
        { sum += d;}
        mean = sum/n;
        sum = 0;
        for(Double d : vec)
        { sum += Math.pow((d-mean), 2);}
        stdDev = Math.sqrt(sum/n);
        if (stdDev!=0){min = (Collections.min(vec)-mean)/stdDev;}else{min=mean;}

        return new double[]{mean,stdDev,min};
    }

    private  void normalizeFeaturesRating(){
    	//it computes the feature rating as the mean of items ratings weighted through item relevance

        o.entrySet().stream().forEach(feature -> {
            ArrayList<Float> featureStats = o.get(feature.getKey());
            featureStats.set(2, (featureStats.get(4) / featureStats.get(5)));
            o.put(feature.getKey(),featureStats);
        });
    }

    private  void sum_RelevanceItemDotRatingItem_perFeature(){
		//For each feature it computes the weighted sum of different items (relevance*rating) and it computes the sum of relevances per item
        votes.entrySet().stream().forEach(item -> {
            float itemWeight = moviesWeights.get(item.getKey());
            HashSet<Integer> bag = new HashSet<Integer>();
            map.get(item.getKey()).stream().forEach(feature -> {
                if (!bag.contains(feature)){
                    ArrayList<Float> featureStats = o.get(feature);
                    featureStats.set(4, featureStats.get(4) + (itemWeight * item.getValue()));
                    featureStats.set(5, featureStats.get(5) + (itemWeight));
                    o.put(feature, featureStats);
                    bag.add(feature);
                }
            });
        });
    }

    private  void computeItemsRelevanceUsers(){
		//it computes the item relevance summing up the relevances of the involved features
		//!!! referred only to items in user profile

        votes.keySet().stream().forEach(item -> {
            ArrayList<Integer> itemFeatures = map.get(item);
            HashSet<Integer> bag = new HashSet<>();
            float sumWeightsItem = (float)itemFeatures.stream().map(feature -> {
                if(!bag.contains(feature)){
                    bag.add(feature);
                    return o.get(feature).get(3);
                }else{
                    return null;
                }
            }).filter(Objects::nonNull).mapToDouble(e -> e).sum();
            moviesWeights.put(item, sumWeightsItem);
        });
    }

    private  void computeItemsRelevanceCollection(){
		//For each item it sums the involved features relevances
		//!!! referred to all items

        map.entrySet().stream().forEach(item -> {
            //All unrated items
            if(!votes.containsKey(item.getKey())){
                float sumWeightsItem = (float)item.getValue().stream().map(feature -> (o.get(feature)!=null)?o.get(feature).get(3):null).filter(Objects::nonNull).mapToDouble(e -> e).sum();
                allmoviesWeights.put(item.getKey(), sumWeightsItem);
            }
        });
    }


    private  void normalizeFeaturesRelevance(){
    	//compute feature relevance divinding the number of times the feature appears in items (max 1 per item) to the number of items
        o.entrySet().stream().forEach(feature -> {
            ArrayList<Float> featureStats = feature.getValue();
            featureStats.set(3, (featureStats.get(0) / (float) total ));
            o.put(feature.getKey(), featureStats);
        });
    }

    private  void computeFeaturesRelevance(){
    	//initialization of features arraylists - here it is only incremented the number of times the user consumes each feature
        votes.entrySet().stream().forEach(rate -> {
            //Single Item
            ArrayList<Integer> itemFeatures = map.get(rate.getKey());
            //HashSet to ensure single usage of features per item
            HashSet<Integer> bag = new HashSet<>();
            if (itemFeatures!=null)itemFeatures.stream().forEach(feature -> {
                //single feature
                if (!bag.contains(feature)){
                    ArrayList<Float> featureStats = o.get(feature);
                    //o -- occurrences | sum(avgsingle) | estimated_rating | feature_weight | sumWeightedFeature | sumWeightedMovies |
                    if(featureStats==null) {
                        featureStats = new ArrayList<>(Arrays.asList(1f,0f,0f,0f,0f,0f));
                    }
                    else {
                        featureStats.set(0, featureStats.get(0) + 1);
                    }
                    o.put(feature, featureStats);
                    bag.add(feature);
                }
            });
        });
    }
}
