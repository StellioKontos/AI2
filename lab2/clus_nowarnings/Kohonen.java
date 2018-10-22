import java.util.*;
import java.lang.*;

public class Kohonen extends ClusteringAlgorithm
{
	// Size of clustersmap
	private int n;

	// Number of epochs
	private int epochs;
	
	// Dimensionality of the vectors
	private int dim;
	
	// Threshold above which the corresponding html is prefetched
	private double prefetchThreshold;

	private double initialLearningRate; 
	
	// This class represents the clusters, it contains the prototype (the mean of all it's members)
	// and a memberlist with the ID's (Integer objects) of the datapoints that are member of that cluster.  
	private Cluster[][] clusters;

	// Vector which contains the train/test data
	private Vector<float[]> trainData;
	private Vector<float[]> testData;
	
	// Results of test()
	private double hitrate;
	private double accuracy;
	
	static class Cluster
	{
			float[] prototype;

			Set<Integer> currentMembers;

			public Cluster()
			{
				currentMembers = new HashSet<Integer>();
			}
	}
	
	public Kohonen(int n, int epochs, Vector<float[]> trainData, Vector<float[]> testData, int dim)
	{
		this.n = n;
		this.epochs = epochs;
		prefetchThreshold = 0.5;
		initialLearningRate = 0.8;
		this.trainData = trainData;
		this.testData = testData; 
		this.dim = dim;       
		
		Random rnd = new Random();

		// Here n*n new cluster are initialized
		clusters = new Cluster[n][n];
		for (int i = 0; i < n; i++)  {
			for (int i2 = 0; i2 < n; i2++) {
				clusters[i][i2] = new Cluster();
				clusters[i][i2].prototype = new float[dim];
				for(int i3 = 0; i3<dim; i3++) {
					clusters[i][i2].prototype[i3] = rnd.nextFloat();
				}
			}
		}
	}

	///calculate the Euclidian distance
	private double measureEuclidian(float[] a, float[] b) {
		double eucSquared = 0;
		for(int i = 0; i<dim; i++) {
			eucSquared += (a[i] - b[i]) * (a[i] - b[i]);
		}
		return Math.sqrt(eucSquared);
	}

	///move a given unit towards the input vector
	private void moveUnit(int x, int y, double learningRate, float[] input) {
		for(int i = 0; i<dim; i++) {
			clusters[x][y].prototype[i] *= (1-learningRate);
			clusters[x][y].prototype[i] += learningRate*input[i];
		}
	}

	
	public boolean train()
	{
		for(int i = 0; i<epochs; i++) {
			System.out.println("Progress: " + i + "/" + epochs);
			double learningRate = initialLearningRate*(1.0 - (double)i / epochs);
			double r = n * (1.0 - (double)i / epochs) / 2;
			for(int j = 0; j<trainData.size(); j++) {
				int bestX = 0;
				int bestY = 0;
				double minDist = 999999;

				///find the Best Matching Unit
				for(int x = 0; x<n; x++) {
					for(int y = 0; y<n; y++) {
						double dist = measureEuclidian(clusters[x][y].prototype, trainData.get(j));
						if(dist < minDist) {
							bestX = x;
							bestY = y;
							minDist = dist;
						}
					}
				}
				
				///move all nodes within the neighbourhood
				for(int x = 0; x<n; x++) {
					for(int y = 0; y<n; y++) {
						double dist = Math.abs(x - bestX) + Math.abs(y-bestY);
						if(dist <= r) {
							moveUnit(x, y, learningRate, trainData.get(j));
						}
					}
				}
			}
		}
		return true;
	}
	
	public boolean test()
	{

		///iterate over all training data and assign each datapoint to a cluster
		for(int i = 0; i<trainData.size(); i++) {
			int closestX = 0;
			int closestY = 0;
			double minDist = 99999999;
			///for each cluster, calculate the distance, and determine if it is the current minimum
			for(int x = 0; x<n; x++) {
				for(int y = 0; y<n; y++) {
					double dist = measureEuclidian(trainData.get(i), clusters[x][y].prototype);
					if(dist < minDist) {
						minDist = dist;
						closestX = x;
						closestY = y;
					}
				}
			}
			clusters[closestX][closestY].currentMembers.add(i);
		}


		int totalHits = 0;
		int totalRequests = 0;
		int totalPrefetched = 0;

		///for every element count requets, prefetches and hits
		for(int x = 0; x<n; x++) {
			for(int y = 0; y<n; y++) {
				for(int m : clusters[x][y].currentMembers) {
					for(int j = 0; j<dim; j++) {
						boolean pref = false;
						boolean requ = false;
						if(testData.get(m)[j] == 1) requ = true;
						if(clusters[x][y].prototype[j] >= prefetchThreshold) pref = true;
						if(requ) totalRequests++; 
						if(pref) totalPrefetched++;
						if(requ && pref) totalHits++;
					}
				}
			}
		}

		hitrate = (double)(totalHits)/totalRequests;
		accuracy = (double)(totalHits)/totalPrefetched;
		return true;
	}


	public void showTest()
	{
		System.out.println("Initial learning Rate=" + initialLearningRate);
		System.out.println("Prefetch threshold=" + prefetchThreshold);
		System.out.println("Hitrate: " + hitrate);
		System.out.println("Accuracy: " + accuracy);
		System.out.println("Hitrate+Accuracy=" + (hitrate + accuracy));
	}
 
 
	public void showMembers()
	{
		for (int i = 0; i < n; i++)
			for (int i2 = 0; i2 < n; i2++)
				System.out.println("\nMembers cluster["+i+"]["+i2+"] :" + clusters[i][i2].currentMembers);
	}

	public void showPrototypes()
	{
		for (int i = 0; i < n; i++) {
			for (int i2 = 0; i2 < n; i2++) {
				System.out.print("\nPrototype cluster["+i+"]["+i2+"] :");
				
				for (int i3 = 0; i3 < dim; i3++)
					System.out.print(" " + clusters[i][i2].prototype[i3]);
				
				System.out.println();
			}
		}
	}

	public void setPrefetchThreshold(double prefetchThreshold)
	{
		this.prefetchThreshold = prefetchThreshold;
	}
}

