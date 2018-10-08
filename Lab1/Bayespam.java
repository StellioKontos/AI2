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

		double logProbGivenRegular;	//stores the conditional probabilities of the word
		double logProbGivenSpam;

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

	///parameter defines the minimum word length
	private static int alpha = 4;

	///variables store prior probabilities of spam and regular
	private static double logPriorRegular;
	private static double logPriorSpam;

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
		if(finalWord.length() < alpha) {
			return "";
		}
        return finalWord;
    }
    
    // Add a word to the vocabulary
    private static void addWord(String word, MessageType type)
    {
        word = cleanWord(word);
        if(word == "") {
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
			///System.out.println(messages[i]);
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

	/// test the classifier 
    private static void testClassifier()
    throws IOException
    {
        File[] messages = new File[0];
		int correctRegular = 0;
		int incorrectRegular = 0;
		int correctSpam = 0;
		int incorrectSpam = 0;
		boolean testingSpam;

		///seperately test the classifier on regular messages and on spam messages
		for (int i = 0; i<2; i++) {
			if(i == 0) {
				messages = listing_regular;
				testingSpam = false;
			}
			else {
				messages = listing_spam;
				testingSpam = true;
			}
			//Goes through the list of messages and classifies each as spam or regular
			for(int j = 0; j< messages.length; j++) {
				boolean iAmSpam = isSpam(messages[j]);
				if(iAmSpam) {
					if(testingSpam) {
						correctSpam += 1;
					}
					else {
						incorrectRegular += 1;
					}
				}
				else {
					if(testingSpam) {
						incorrectSpam += 1;
					}
					else { 
						correctRegular += 1;
					}
				}
			}
		}
        
        System.out.println("Messages correctly identified as 'regular': " + Integer.toString(correctRegular));
		System.out.println("Messages correctly identified as 'spam' : " + Integer.toString(correctSpam));
		System.out.println("Messages incorrectly identified as 'regular' : " + Integer.toString(incorrectSpam));
		System.out.println("Messages incorrectly identified as 'spam' : " + Integer.toString(incorrectRegular));
		System.out.print("Overal Accuracy: ");
		double accuracy = (double)(correctRegular + correctSpam) / (correctRegular + correctSpam + incorrectRegular + incorrectSpam);
		System.out.println(accuracy);
    }

	///Determine if a message is spam
	private static boolean isSpam(File message) 
	throws IOException
	{
		FileInputStream i_s = new FileInputStream( message );
        BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
        String line;
        String word;

		///create a local vocabulary for words contained in the message
		HashSet<String> messageVocab = new HashSet<String>();
        

		///read message words into local vocabulary
        while ((line = in.readLine()) != null)                      // read a line
        {
            StringTokenizer st = new StringTokenizer(line);         // parse it into words
    
            while (st.hasMoreTokens())                  // while there are stille words left..
            {
                word = cleanWord(st.nextToken());
        		if(word.length() >= 4) {
        			if ( !vocab.contains(word) ){                  // if word doesn't yet exist in the vocab
        		    	messageVocab.add(word);                  // add word to vocab
        			}
        		}
        	}
        }

		in.close();

		///set the initial probabilites to the priors
		double log_regular = logPriorRegular;
		double log_spam = logPriorSpam;

		///for all words in the message, use their conditional probabilities to update the probability of regular/spam
		for(String w : messageVocab) {
        	if ( vocab.containsKey(w) ){                  // if word exists already in the vocabulary..
				Multiple_Counter counter = new Multiple_Counter();
            	counter = vocab.get(w);                  // get the counter from the hashtable				

				///update probabilities with new evidence
				log_regular += counter.logProbGivenRegular;
				log_spam += counter.logProbGivenSpam;
        	}
		}

		if(log_spam >= log_regular) {
			return true;
		}
		return false;
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

			double probGivenRegular = (double)counter.counter_regular / nWordsRegular;
			double probGivenSpam = (double)counter.counter_spam / nWordsSpam;

			///Set zero probabilities to default minimum probability
			if(probGivenRegular == 0) {
				probGivenRegular = epsilon / (nWordsRegular + nWordsSpam);
			}
			if(probGivenSpam == 0) {
				probGivenSpam = epsilon / (nWordsRegular + nWordsSpam);
			}
			
			if(probGivenRegular == 0 || probGivenSpam == 0) {
				System.out.println("Je hebt het verkankered!");
			}

			counter.logProbGivenRegular = Math.log(probGivenRegular);
			counter.logProbGivenSpam = Math.log(probGivenSpam);
        }
	}
   
    public static void main(String[] args)
    throws IOException
    {
        // Location of the traning directory (the path) taken from the cmd line (first arg)
        File dir_location_train = new File( args[0] );
        
        // Check if the cmd line arg is a directory
        if ( !dir_location_train.isDirectory() )
        {
            System.out.println( "- Error: cmd line arg1 not a directory.\n" );
            Runtime.getRuntime().exit(0);
        }

        // Initialize the regular and spam lists
        listDirs(dir_location_train);

        // Read the e-mail messages
        readMessages(MessageType.NORMAL);
        readMessages(MessageType.SPAM);

        // Print out the hash table
        //printVocab();

        ///calculates the prior probabilities
        double nMessagesRegular = listing_regular.length;
        double nMessagesSpam = listing_spam.length;
        double nMessagesTotal = nMessagesRegular + nMessagesSpam;

        logPriorRegular = Math.log(nMessagesRegular / nMessagesTotal);
        logPriorSpam = Math.log(nMessagesSpam / nMessagesTotal);

        ///System.out.println(logPriorRegular);
        ///System.out.println(logPriorSpam);

		///calculate class conditional probabilities
		computeCCProbs();

		/// Location of the testing directory (the path) taken from the cmd line (second arg)
        File dir_location_test = new File( args[1] );
        
        /// Check if the cmd line arg is a directory
        if ( !dir_location_test.isDirectory() )
        {
            System.out.println( "- Error: cmd line arg2 not a directory.\n" );
            Runtime.getRuntime().exit(0);
        }

		/// Initialize the regular and spam lists
        listDirs(dir_location_test);

		///classify the test set messages and print the confusion matrix
		testClassifier();
		
        
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
