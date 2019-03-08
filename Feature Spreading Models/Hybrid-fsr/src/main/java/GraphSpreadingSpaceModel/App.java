package GraphSpreadingSpaceModel;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;


public class App 
{

	public static HashMap<Integer, HashMap<Integer, Float>> useContent(HashMap<Integer, HashMap<Integer, Float>> allVotes, HashMap<Integer, ArrayList<Integer>> featMap, int numberRecs) throws IOException
	{

		HashMap<Integer, HashMap<Integer, Float>> profiles = new HashMap<>();

		allVotes.entrySet().parallelStream().map(map2 ->{
			User a = new User(featMap,map2.getKey(),map2.getValue());
			a.process();
			return new AbstractMap.SimpleEntry<>(map2.getKey(),a.getProfile());
		}).forEachOrdered(entry ->{
			profiles.put(entry.getKey(),entry.getValue());
		});
		return profiles;
	}

	public static HashMap<Integer, ArrayList<AbstractMap.SimpleEntry<Integer, Float>>> useContentPostCollaborative(HashMap<Integer, HashMap<Integer, Float>> allVotes, HashMap<Integer, ArrayList<Integer>> featMap, HashMap<Integer, HashMap<Integer, Float>> badProfiles, int numberRecs) throws IOException
	{

        HashMap<Integer, ArrayList<AbstractMap.SimpleEntry<Integer, Float>>> recs = new HashMap<>();

		allVotes.entrySet().parallelStream().map(map2 ->{
            UserPostCollaborative a = new UserPostCollaborative(featMap,map2.getKey(),map2.getValue(),badProfiles.get(map2.getKey()),numberRecs);
			a.process();
            return new AbstractMap.SimpleEntry<>(map2.getKey(),a.getRecs());
        }).forEachOrdered(entry ->{
            recs.put(entry.getKey(),entry.getValue());
        });
        return recs;
	}
}
