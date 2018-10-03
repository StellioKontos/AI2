import java.io.*;
import java.util.*;

public class Bayespam
{
    // This defines the two types of messages we have.
    static enum MessageType
    {
        NORMAL, SPAM
    }

    // This a class with two counters (for regular and for spam)
    static class Multiple_Counter
    {
        int counter_spam    = 0;
        int counter_regular = 0;

		double probGivenRegular;	//stores the conditional probabilities of the word
		double probGivenSpam;

        // Increase one of the counters by one
        public void incrementCounter(MessageType type)
        {
            if ( type == MessageType.NORMAL ){
                ++counter_regular;
            } else {
                ++counter_spam;
            }
        }
        // Get counter value
        public int getCount(MessageType type) {
            if ( type == MessageType.NORMAL ) {
                return counter_regular;
            }
            else {
                return counter_spam;
            }
        }
    }

	///parameter defines the default minimum probability
	private static double epsilon = 1;

    // Listings of the two subdirectories (regular/ and spam/)
    private static File[] listing_regular = new File[0];
    private static File[] listing_spam = new File[0];

    // A hash table for the vocabulary (word searching is very fast in a hash table)
    private static Hashtable <String, Multiple_Counter> vocab = new Hashtable <String, Multiple_Counter> ();

    //check if word passes filter
    private static String cleanWord(String word) {
        String finalWord = "";
        for(int i = 0; i<word.length(); i++) {
            char c = word.charAt(i);
            if(Character.isUpperCase(c)) {
                c = Character.toLowerCase(c);
            }
            if(Character.isLetter(c)) {
                finalWord += c;
            }
        }
        return finalWord;
    }
    
    // Add a word to the vocabulary
    private static void addWord(String word, MessageType type)
    {
        word = cleanWord(word);
        if(word.length() < 4) {
            return;
        }
        Multiple_Counter counter = new Multiple_Counter();

        if ( vocab.containsKey(word) ){                  // if word exists already in the vocabulary..
            counter = vocab.get(word);                  // get the counter from the hashtable
        }
        counter.incrementCounter(type);                 // increase the counter appropriately

        vocab.put(word, counter);                       // put the word with its counter into the hashtable
    }


    // List the regular and spam messages
    private static void listDirs(File dir_location)
    {
 	   // List all files in the directory passed
    	File[] dir_listing = dir_location.listFiles();
    	String folder_name; 
    	Boolean spam_found = false, regular_found = false;

    	// Check that there are exactly 2 subdirectories
    	if ( dir_listing.length != 2 )
    	{
    	    System.out.println( "- Error: the directory should contain exactly 2 subdirectories (named spam and regular).\n" );
    	    Runtime.getRuntime().exit(0);
    	}
    
    	// Loop through all subdirectories
    	for (File f : dir_listing) {
        	folder_name = f.toString();
        	// If the folder_name ends in the word spam, store it as the spam folder
        	if (folder_name.length() > 3 && folder_name.substring(folder_name.length() - 4).equals("spam")) {
            	listing_spam = f.listFiles();
            	spam_found = true;
        	// If the folder_name ends in the word regular, store it as the regular folder
        	} else if (folder_name.length() > 6 && folder_name.substring(folder_name.length() - 7).equals("regular")) {
            	listing_regular = f.listFiles();
            	regular_found = true;
        	}
        
    	}
    
    	if (!spam_found) {
        	System.out.println( "- Error: directory with spam messages not found. Make sure your input directory contains a folder named spam\n" );
        	Runtime.getRuntime().exit(0);
    	}
    	if (!regular_found) {
        	System.out.println( "- Error: directory with regular messages not found. Make sure your input directory contains a folder named regular\n" );
        	Runtime.getRuntime().exit(0);
    	}
	}


    
    // Print the current content of the vocabulary
    private static void printVocab()
    {
        Multiple_Counter counter = new Multiple_Counter();

        for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
        {   
            String word;
            
            word = e.nextElement();
            counter  = vocab.get(word);
            
            System.out.println( word + " | in regular: " + counter.counter_regular + 
                                " in spam: "    + counter.counter_spam);
        }
    }


    // Read the words from messages and add them to your vocabulary. The boolean type determines whether the messages are regular or not  
    private static void readMessages(MessageType type)
    throws IOException
    {
        File[] messages = new File[0];

        if (type == MessageType.NORMAL){
            messages = listing_regular;
        } else {
            messages = listing_spam;
        }
        
        
        for (int i = 0; i < messages.length; ++i)
        {
            FileInputStream i_s = new FileInputStream( messages[i] );
            BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
            String line;
            String word;
            
            while ((line = in.readLine()) != null)                      // read a line
            {
                StringTokenizer st = new StringTokenizer(line);         // parse it into words
        
                while (st.hasMoreTokens())                  // while there are stille words left..
                {
                    addWord(st.nextToken(), type);                  // add them to the vocabulary
                }
            }

            in.close();
        }
    }

	///calculate class conditional probabilities for all words in vocabulary
	private static void computeCCProbs() {
		Multiple_Counter counter = new Multiple_Counter();
		int nWordsRegular = 0;
		int nWordsSpam = 0;

		///count up the total words in Regular and Spam
        for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
        {   
            String word;
            
            word = e.nextElement();
            counter  = vocab.get(word);

			nWordsRegular += counter.counter_regular;
			nWordsSpam += counter.counter_spam;
        }

		///give each word its conditional probabilities
		for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
        {   
            String word;
            
            word = e.nextElement();
            counter  = vocab.get(word);

			counter.probGivenRegular = (double)counter.counter_regular / nWordsRegular;
			counter.probGivenSpam = (double)counter.counter_spam / nWordsSpam;

			///Set zero probabilities to default minimum probability
			if(counter.probGivenRegular == 0) {
				counter.probGivenRegular = epsilon / (nWordsRegular + nWordsSpam);
			}
			if(counter.probGivenSpam == 0) {
				counter.probGivenRegular = epsilon = (nWordsRegular + nWordsSpam);
			}
        }
	}
   
    public static void main(String[] args)
    throws IOException
    {
        // Location of the directory (the path) taken from the cmd line (first arg)
        File dir_location = new File( args[0] );
        
        // Check if the cmd line arg is a directory
        if ( !dir_location.isDirectory() )
        {
            System.out.println( "- Error: cmd line arg not a directory.\n" );
            Runtime.getRuntime().exit(0);
        }

        // Initialize the regular and spam lists
        listDirs(dir_location);

        // Read the e-mail messages
        readMessages(MessageType.NORMAL);
        readMessages(MessageType.SPAM);

        // Print out the hash table
        printVocab();

        ///calculates the prior probabilities
        double nMessagesRegular = listing_regular.length;
        double nMessagesSpam = listing_spam.length;
        double nMessagesTotal = nMessagesRegular + nMessagesSpam;

        double priorRegular = nMessagesRegular / nMessagesTotal;
        double priorSpam = nMessagesSpam / nMessagesTotal;

        System.out.println(priorRegular);
        System.out.println(priorSpam);

		///calculate class conditional probabilities
		computeCCProbs();
        
        // Now all students must continue from here:
        //
        // 1) A priori class probabilities must be computed from the number of regular and spam messages
        // 2) The vocabulary must be clean: punctuation and digits must be removed, case insensitive
        // 3) Conditional probabilities must be computed for every word
        // 4) A priori probabilities must be computed for every word
        // 5) Zero probabilities must be replaced by a small estimated value
        // 6) Bayes rule must be applied on new messages, followed by argmax classification
        // 7) Errors must be computed on the test set (FAR = false accept rate (misses), FRR = false reject rate (false alarms))
        // 8) Improve the code and the performance (speed, accuracy)
        //
        // Use the same steps to create a class BigramBayespam which implements a classifier using a vocabulary consisting of bigrams
    }
}
