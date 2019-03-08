package BiasedMF;

import java.util.*;

public class MainBiasedMF {

    /** Rating Map           */ private static HashMap<Integer, HashMap<Integer, Double>> ratingsMap = new HashMap<>();
    /** Data Model           */ private static MFDataModelArray dataModel;
    /** Learning Rate        */ private static float learningRate = 0.01F;
    /** Bias regularization  */ private static float biasRegularization = 0.01F;
    /** User regularization  */ private static float userRegularization = 0.015F;
    /** Item regularization  */ private static float itemRegularization = 0.015F;
    /** Bias learn rate      */ private static float BiasLearnRate = 1.0F;
    /** Learning rate decay  */ private static float learningRateDecay = 1.0F;
    /** Number of Factors    */ private static int D = 10;
    /** Update Users Fac     */ private static boolean updateUsers = true;
    /** Update Items Fac     */ private static boolean updateItems = true;
    /** Bold driver          */ private static boolean boldDriver = false;
    /** Frequency regularization */ private static boolean frequencyRegularization = false;
    /** Number of iterations */ private static int numIters = 30;
    /** Initial val of Mean  */ private static float initMean = 0;
    /** Initial val of St.Dev*/ private static float initStdDev = 0.1F;
    /** Debug Mode           */ private static int debug = 1;

    public static HashMap<Integer, HashMap<Integer, Float>> useBiasedMF(HashMap<Integer, HashMap<Integer, Float>> ratingsMap) {
        dataModel = new MFDataModelArray(ratingsMap,D,initMean,initStdDev);
        BiasedMF model = new BiasedMF(D, learningRate, biasRegularization, userRegularization, itemRegularization, BiasLearnRate, learningRateDecay, updateUsers, updateItems, boldDriver, frequencyRegularization);
        model.Train(dataModel, numIters);
            return dataModel.getBadProfiles();
    }
}
