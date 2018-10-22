import java.util.*;
import java.util.Random;
import java.lang.*;

public class KMeans extends ClusteringAlgorithm
{
	// Number of clusters
	private int k;

	// Dimensionality of the vectors
	private int dim;
	
	// Threshold above which the corresponding html is prefetched
	private double prefetchThreshold;
	
	// Array of k clusters, class cluster is used for easy bookkeeping
	private Cluster[] clusters;
	
	// This class represents the clusters, it contains the prototype (the mean of all it's members)
	// and memberlists with the ID's (which are Integer objects) of the datapoints that are member of that cluster.
	// You also want to remember the previous members so you can check if the clusters are stable.
	static class Cluster
	{
		float[] prototype;

		Set<Integer> currentMembers;
		Set<Integer> previousMembers;
		  
		public Cluster()
		{
			currentMembers = new HashSet<Integer>();
			previousMembers = new HashSet<Integer>();
		}
	}
	// These vectors contains the feature vectors you need; the feature vectors are float arrays.
	// Remember that you have to cast them first, since vectors return objects.
	private Vector<float[]> trainData;
	private Vector<float[]> testData;

	// Results of test()
	private double hitrate;
	private double accuracy;
	
	public KMeans(int k, Vector<float[]> trainData, Vector<float[]> testData, int dim)
	{
		this.k = k;
		this.trainData = trainData;
		this.testData = testData; 
		this.dim = dim;
		prefetchThreshold = 0.5;
		
		// Here k new cluster are initialized
		clusters = new Cluster[k];
		for (int ic = 0; ic < k; ic++)
			clusters[ic] = new Cluster();
	}

	///Initialize the prototypes with random values
	private void initPrototypes() {
		Random r = new Random();
		for(int i = 0; i < k; i++) {
			float[] p = new float[dim];
			for(int j = 0; j < dim; j++) {
				p[j] = r.nextFloat();
			}
			clusters[i].prototype = p;
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

	///Assign each datapoint to the closest prototype
	private void generatePartition(boolean test) {

		///the boolean 'test' allows us to generate a partition from either the training set or the testing set
		Vector<float[]> data;
		if(test) {
			data = testData;
		}
		else {
			data = trainData;
		}		

		for(int i = 0; i<k; i++) {
			///move members from current set to previous set
			clusters[i].previousMembers = new HashSet<Integer>(clusters[i].currentMembers);

			///make a new empty current members set
			clusters[i].currentMembers = new HashSet<Integer>();
		}
		///iterate over all training data and assign each datapoint to a cluster
		for(int i = 0; i<data.size(); i++) {
			int closestCluster = 0;
			double minDist = 99999999;
			///for each cluster, calculate the distance, and determine if it is the current minimum
			for(int j = 0; j<k; j++) {
				double dist = measureEuclidian(data.get(i), clusters[j].prototype);
				if(dist < minDist) {
					minDist = dist;
					closestCluster = j;
				}
			}
			clusters[closestCluster].currentMembers.add(i);
		}
	}

	///Calculate the cluster center based on its members
	private void calculatePrototypes() {
		for(int i = 0; i<k; i++) {
			float[] newPrototype = new float[dim]; 						///generate new empty prototype
			for(int j : clusters[i].currentMembers) {
				///add the member's vector to the prototype
				for(int m = 0; m<dim; m++) {
					newPrototype[m] += trainData.get(j)[m];
				}
			}
			///divide the sum to achieve the average
			for(int j = 0; j<dim; j++) {
				newPrototype[j] /= clusters[i].currentMembers.size();
			}
			clusters[i].prototype = newPrototype;
		}
	}


	///check if training finished
	private boolean amIDoneYet() {;
		for(int i = 0; i<k; i++) {
			for(int j : clusters[i].currentMembers) {
				if(!clusters[i].previousMembers.contains(j)) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean train()
	{
		///Select an initial random partitioning
		initPrototypes();
		boolean done = false;
		int iter = 1;
		while(!done) {
			System.out.println("Iteration: " + iter);

			generatePartition(false);	///put each datapoint in a cluster
			calculatePrototypes();	///calculate centers of each cluster

			for(int i = 0; i<k; i++) {
				System.out.println("Cluster "+ i +" has " + clusters[i].currentMembers.size() + " many members");
			}

			///check if the clusters have stabilized
			done = amIDoneYet();
			iter++;
		}
		return false;
	}

	public boolean test()
	{

		int totalHits = 0;
		int totalRequests = 0;
		int totalPrefetched = 0;

		generatePartition(true);		///partition the testing data

		///for every element count requets, prefetches and hits
		for(int i = 0; i<k; i++) {
			for(int m : clusters[i].currentMembers) {
				for(int j = 0; j<dim; j++) {
					boolean pref = false;
					boolean requ = false;
					if(testData.get(m)[j] == 1) requ = true;
					if(clusters[i].prototype[j] >= prefetchThreshold) pref = true;
					if(requ) totalRequests++; 
					if(pref) totalPrefetched++;
					if(requ && pref) totalHits++;
				}
			}
		}

		hitrate = (double)(totalHits)/totalRequests;
		accuracy = (double)(totalHits)/totalPrefetched;	

		return true;
	}


	// The following members are called by RunClustering, in order to present information to the user
	public void showTest()
	{
		System.out.println("Prefetch threshold=" + this.prefetchThreshold);
		System.out.println("Hitrate: " + this.hitrate);
		System.out.println("Accuracy: " + this.accuracy);
		System.out.println("Hitrate+Accuracy=" + (this.hitrate + this.accuracy));
	}
	
	public void showMembers()
	{
		for (int i = 0; i < k; i++)
			System.out.println("\nMembers cluster["+i+"] :" + clusters[i].currentMembers);
	}
	
	public void showPrototypes()
	{
		for (int ic = 0; ic < k; ic++) {
			System.out.print("\nPrototype cluster["+ic+"] :");
			
			for (int ip = 0; ip < dim; ip++)
				System.out.print(clusters[ic].prototype[ip] + " ");
			
			System.out.println();
		 }
	}

	// With this function you can set the prefetch threshold.
	public void setPrefetchThreshold(double prefetchThreshold)
	{
		this.prefetchThreshold = prefetchThreshold;
	}
}
