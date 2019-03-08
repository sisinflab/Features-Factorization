package GraphSpreadingSpaceModel;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class User {
	
	//user to be processed
	 int activeUser = 0;
	 
	 //features map
 	 //o -- co sum(avgsingle) orate weight sumWeightedFeature sumWeightedMovies
	 Map<Integer, ArrayList<Float>> o = new HashMap<>();
	 
	 //profile length
	 int total = 0;
	 
	 //overall relevances of items in user's profile
	 HashMap<Integer, Float> moviesWeights = new HashMap();
	 
	 //overall relevances of items in the collection
	 HashMap<Integer, Float> allmoviesWeights = new HashMap();

	 //stats
	 double userStats[] = new double[3];
	 double voteStats[] = new double[3];
	 double userCosineDen;

	 
	 //items' map with related values
	 //m -- mrate sumWeightedFeatures sumWeights
	 //m -- predicted rating | weighted sum of features | weights' sum
     Map<Integer, Float> m2 = new HashMap<>();
 	 
 	 //recs to be returned
 	 int numberRecs;

 	 //it has to be define while creating the object
 	 //item features associations map
 	 Map<Integer, ArrayList<Integer>> map;
 	 
 	 //ratings map
	 Map<Integer, Float> votes;

     List<AbstractMap.SimpleEntry<Integer, Float>> recs = new ArrayList<>();


	public User(HashMap<Integer, ArrayList<Integer>>  map, int user, Map<Integer, Float> votes, int numberRecs) {
		this.map = map;
		this.votes = votes;
		this.activeUser = user;
		this.numberRecs = numberRecs;
	}
	public ArrayList<AbstractMap.SimpleEntry<Integer, Float>> getRecs(){
		return (ArrayList<AbstractMap.SimpleEntry<Integer, Float>>) recs;
		
	}
	
	public void process(){
    	o = new HashMap<Integer, ArrayList<Float>>();
    	total = votes.size();
    	moviesWeights = new HashMap();
	    userStats = new double[3];
	    voteStats = new double[3];
	    allmoviesWeights = new HashMap();

		computeFeaturesRelevance();
		normalizeFeaturesRelevance();
        
	    /////////////////
		computeItemsRelevanceUsers();
		sum_RelevanceItemDotRatingItem_perFeature();
		normalizeFeaturesRating();
	    //////////////////


//
//	    userStats();
//	    voteStats();
//	    popStats();

		computeItemsRelevanceCollection();

		userCosineDen = computeCosineDen();

	    generalRankings();

	    try {

			sortRankings();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    private double computeCosineDen(){
	    double exitVal = o.values().stream().mapToDouble(al -> al.get(2)*al.get(3)*al.get(2)*al.get(3)).sum();
	    return Math.sqrt(exitVal);
    }
	public HashMap<Integer, Double> getProfile(){
        HashMap<Integer, Double> profile = new HashMap<>();
        for (Map.Entry<Integer, ArrayList<Float>> entry : o.entrySet()){
            profile.put(entry.getKey(), Double.valueOf(entry.getValue().get(2)));
        }
        return profile;
    }
	

	
	public  void sortRankings() throws IOException{
	    recs = m2.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).limit(numberRecs)
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(),e.getValue()))
                .collect(Collectors.toList());
	}
	
	public  void generalRankings(){
		//it predicts ratings for each item in allmovieweights

        allmoviesWeights.entrySet().stream().forEach(item -> {
            m2.put(item.getKey(),score(item.getKey()));
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
        float den = (float)(userCosineDen*Math.sqrt(itemFeatures.size()));
        return (den!=0)?sumWeightedFeatures.get()/den:0f;
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

	public  double[] stats(ArrayList<Double> vec){
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
	
    public  void normalizeFeaturesRating(){
    	//it computes the feature rating as the mean of items ratings weighted through item relevance

        o.entrySet().stream().forEach(feature -> {
            ArrayList<Float> featureStats = o.get(feature.getKey());
            featureStats.set(2, (featureStats.get(4) / featureStats.get(5)));
            o.put(feature.getKey(),featureStats);
        });
    }
    
	public  void sum_RelevanceItemDotRatingItem_perFeature(){
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
    
	public  void computeItemsRelevanceUsers(){
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

	public  void computeItemsRelevanceCollection(){
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
	
	
    public  void normalizeFeaturesRelevance(){
    	//compute feature relevance divinding the number of times the feature appears in items (max 1 per item) to the number of items
        o.entrySet().stream().forEach(feature -> {
            ArrayList<Float> featureStats = feature.getValue();
            featureStats.set(3, (featureStats.get(0) / (float) total ));
            o.put(feature.getKey(), featureStats);
        });
    }
    public  void computeFeaturesRelevance(){
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
