package invertedIndex;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Math.log10;
import static java.lang.Math.sqrt;

import java.util.*;
import java.io.PrintWriter;

/**
 *
 * @author ehab
 */
public class Index5 {

    //--------------------------------------------
    int N = 0;
    public Map<Integer, SourceRecord> sources;  // store the doc_id and the file name.

    public HashMap<String, DictEntry> index; // THe inverted index
    //--------------------------------------------

    // Constructor
    public Index5() {
        sources = new HashMap<Integer, SourceRecord>();
        index = new HashMap<String, DictEntry>();
    }
    // Setter for N
    public void setN(int n) {
        N = n;
    }


    //---------------------------------------------
    // Print a posting list
    public void printPostingList(Posting p) {
        // Iterator<Integer> it2 = hset.iterator();
        System.out.print("[");
        while (p != null) {
            /// -4- **** complete here ****
            // fix get rid of the last comma
            System.out.print("" + p.docId);
            if(!p.termPositions.isEmpty()){
                System.out.print("--->"+" ("+p.termPositions.get(0));
                for(int i = 1; i < p.termPositions.size(); i++){
                    System.out.print("," + p.termPositions.get(i));
                }
                System.out.println(")");
            }
            p = p.next;
            if (p != null) {
                System.out.print(",");
            }
        }
        System.out.println("]");
    }

    //---------------------------------------------
    // Print the inverted index
    public void printDictionary() {
        Iterator it = index.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            DictEntry dd = (DictEntry) pair.getValue();
            System.out.print("** [" + pair.getKey() + "," + dd.doc_freq + "]       =--> ");
            printPostingList(dd.pList);
        }
        System.out.println("------------------------------------------------------");
        System.out.println("*** Number of terms = " + index.size());
    }

    //-----------------------------------------------
    // Build the inverted index from files
    public void buildIndex(String[] files, String type) {  // from disk not from the internet
        int fid = 0;
        for (String fileName : files) {
            try (BufferedReader file = new BufferedReader(new FileReader(fileName))) {
                if (!sources.containsKey(fileName)) {
                    sources.put(fid, new SourceRecord(fid, fileName, fileName, "notext"));
                }
                String ln;
                int flen = 0;
                while ((ln = file.readLine()) != null) {
                    if (Objects.equals(type, "inverted")){
                        flen += indexOneLine(ln, fid);
                    }
                    else if(Objects.equals(type, "bi")) {
                        flen += biWordIndexOneLine(ln, fid);
                    }else if(Objects.equals(type, "positional")) {
                        flen += positionalIndexOneLine(ln, fid, flen);
                    }
                }
                sources.get(fid).length = flen;

            } catch (IOException e) {
                System.out.println("File " + fileName + " not found. Skip it");
            }
            fid++;
        }
    }

    public int positionalIndexOneLine(String ln,int fid,int currentWordsLength){
        int flen=0;
        String [] words = ln.split("\\W+");
        List<String> wordsList = new ArrayList<>(List.of(words));
        String [] nonStoppedWords = wordsList.toArray(new String[0]);
        for(String word : nonStoppedWords){
            word = word.toLowerCase();
            if(stopWord(word)){
                wordsList.remove(word);
            }
        }
        if(wordsList.isEmpty()){
            return flen;
        }
        words = wordsList.toArray(new String[0]);
        flen += words.length;
        for(int i=0; i<words.length; i++){
            String word = words[i].toLowerCase();
            word = stemWord(word);
            if (!index.containsKey(word)) {
                index.put(word, new DictEntry());
            }
            if (!index.get(word).postingListContains(fid)) {
                index.get(word).doc_freq += 1;
                if (index.get(word).pList == null) {
                    index.get(word).pList = new Posting(fid);
                    index.get(word).last = index.get(word).pList;
                } else {
                    index.get(word).last.next = new Posting(fid);
                    index.get(word).last = index.get(word).last.next;
                }
            } else {
                index.get(word).last.dtf += 1;
            }
            // add the term positions to the posting lists
            index.get(word).getPosting(fid).termPositions.add(currentWordsLength + i);
            index.get(word).term_freq += 1;
        }
        return flen;
    }

    //----------------------------------------------------------------------------
    // Index one line of text
    public int indexOneLine(String ln, int fid) {
        int flen = 0;

        String[] words = ln.split("\\W+");
        //   String[] words = ln.replaceAll("(?:[^a-zA-Z0-9 -]|(?<=\\w)-(?!\\S))", " ").toLowerCase().split("\\s+");
        flen += words.length;
        for (String word : words) {
            word = word.toLowerCase();
            if (stopWord(word)) {
                continue;
            }
            word = stemWord(word);
            // check to see if the word is not in the dictionary
            // if not add it
            if (!index.containsKey(word)) {
                index.put(word, new DictEntry());
            }
            // add document id to the posting list
            if (!index.get(word).postingListContains(fid)) {
                index.get(word).doc_freq += 1; //set doc freq to the number of doc that contain the term 
                if (index.get(word).pList == null) {
                    index.get(word).pList = new Posting(fid);
                    index.get(word).last = index.get(word).pList;
                } else {
                    index.get(word).last.next = new Posting(fid);
                    index.get(word).last = index.get(word).last.next;
                }
            } else {
                index.get(word).last.dtf += 1;
            }
            //set the term_fteq in the collection
            index.get(word).term_freq += 1;
            if (word.equalsIgnoreCase("lattice")) {

                System.out.println("  <<" + index.get(word).getPosting(1) + ">> " + ln);
            }

        }
        return flen;
    }


    public int biWordIndexOneLine(String ln, int fid) {
        int flen = 0;
        String[] words = ln.split("\\W+");
        flen += words.length;

        for (int i = 0; i < words.length; i++) {
            String currentWord = words[i].toLowerCase();

            if (stopWord(currentWord)) {
                continue;
            }

            currentWord = stemWord(currentWord);
            // Add the current word to the index
            addWordToIndex(currentWord, fid);

            // Handle bi-word indexing
            if (i < words.length - 1) {
                String nextWord = words[i + 1].toLowerCase();
                if (!stopWord(nextWord)) {
                    nextWord = stemWord(nextWord);
                    String biWord = currentWord + "_" + nextWord; // Forming the bi-word with underscore
                    addWordToIndex(biWord, fid);
                }
            }

            // Check for specific word logic, if necessary
            if (currentWord.equalsIgnoreCase("lattice")) {
                System.out.println("  <<" + index.get(currentWord).getPosting(1) + ">> " + ln);
            }
        }

        return flen;
    }


    private void addWordToIndex(String word, int fid) {
        if (!index.containsKey(word)) {
            index.put(word, new DictEntry());
        }
        if (!index.get(word).postingListContains(fid)) {
            index.get(word).doc_freq += 1; // Increment doc frequency for new documents
            if (index.get(word).pList == null) {
                index.get(word).pList = new Posting(fid);
                index.get(word).last = index.get(word).pList;
            } else {
                index.get(word).last.next = new Posting(fid);
                index.get(word).last = index.get(word).last.next;
            }
        } else {
            index.get(word).last.dtf += 1; // Increment document term frequency if already exists
        }
        index.get(word).term_freq += 1; // Increment total term frequency
    }

    //----------------------------------------------------------------------------
// Check if a word is a stop word
    boolean stopWord(String word) {
        if (word.equals("the") || word.equals("to") || word.equals("be") || word.equals("for") || word.equals("from") || word.equals("in")
                || word.equals("a") || word.equals("into") || word.equals("by") || word.equals("or") || word.equals("and") || word.equals("that")) {
            return true;
        }
        if (word.length() < 2) {
            return true;
        }
        return false;

    }
    //----------------------------------------------------------------------------
// Stem a word
    String stemWord(String word) { //skip for now
        return word;
//        Stemmer s = new Stemmer();
//        s.addString(word);
//        s.stem();
//        return s.toString();
    }

    //----------------------------------------------------------------------------
    // Intersect two posting lists
    Posting intersect(Posting pL1, Posting pL2) {
        Posting answer = null;
        Posting last = null;
        while(pL1 != null && pL2 != null){
            if(pL1.docId == pL2.docId) {
                if (answer == null) {
                    answer = new Posting(pL1.docId, pL1.dtf + pL2.dtf);
                    last = answer;
                } else {
                    last.next = new Posting(pL1.docId, pL1.dtf + pL2.dtf);
                    last = last.next;
                }
                pL1 = pL1.next;
                pL2 = pL2.next;
            } else if(pL1.docId < pL2.docId) {
                pL1 = pL1.next;
            } else {
                pL2 = pL2.next;
            }
        }
        return answer;
    }
    // Find documents containing a given phrase
    public String find_24_01(String phrase) {
        String result = "";
        String[] words = phrase.split("\\W+");
        for(String word: words){
            System.out.println(word);
        }
        int len = words.length;

        //fix this if word is not in the hash table will crash...
        for(int i = 0; i < len; i++) {
            words[i] = words[i].toLowerCase();
            if(!index.containsKey(words[i])) {
                return "No match found";
            }
        }

        Posting posting = index.get(words[0].toLowerCase()).pList;
        int i = 1;
        while (i < len) {
            posting = intersect(posting, index.get(words[i].toLowerCase()).pList);
            i++;
        }
        while (posting != null) {
            //System.out.println("\t" + sources.get(num));
            result += "\t" + posting.docId + " - " + sources.get(posting.docId).title + " - " + sources.get(posting.docId).length + "\n";
            posting = posting.next;
        }
        if(result.isEmpty()) {
            return "No match found";
        }
        return result;
    }
    public String findPositionalIndex(String query) {
        StringBuilder result = new StringBuilder();

        // Split query into individual words and remove stop words
        String[] words = query.split("\\W+");
        List<String> validWords = new ArrayList<>();
        for (String word : words) {
            word = word.toLowerCase();
            if (!stopWord(word)) {
                validWords.add(word);
            }
        }

        // If no valid words remaining, return "No match found"
        if (validWords.isEmpty()) {
            return "No match found";
        }

        // Check if each word in the query exists in the index
        for (String word : validWords) {
            if (!index.containsKey(word)) {
                return "No match found";
            }
        }

        // Iterate through each document in the index
        for (int fid = 0; fid < N; fid++) {
            boolean found = true;

            // Get the posting list for the first word in the query
            Posting posting = index.get(validWords.get(0)).getPosting(fid);

            // If posting list is null, continue to the next document
            if (posting == null) {
                continue;
            }

            // Get term positions of the first word in the query
            List<Integer> positions = posting.termPositions;

            // Iterate through term positions of the first word
            for (Integer position : positions) {
                int nextPosition = position + 1;
                found = true;

                // Check positions of subsequent words in the query
                for (int i = 1; i < validWords.size(); i++) {
                    String nextWord = validWords.get(i);
                    Posting nextPosting = index.get(nextWord).getPosting(fid);

                    // If posting list is null or next position is not found, set found to false
                    if (nextPosting == null || !nextPosting.termPositions.contains(nextPosition)) {
                        found = false;
                        break;
                    }
                    nextPosition++;
                }

                // If all words are found at consecutive positions, append result
                if (found) {
                    SourceRecord source = sources.get(fid);
                    result.append("\t").append(fid + 1).append(" - ").append(source.title).append(" - ").append(source.length).append("\n");
                    break;
                }
            }
        }

        // If no match found, return appropriate message
        if (result.isEmpty()) {
            return "No match found";
        }

        return result.toString();
    }


    public String findPhraseQuery(String query) {
        String result = "";
        List<Posting> allPostings = new ArrayList<>();

        // Split the query by quotes to handle phrases and individual words distinctly
        String[] parts = query.split("\"");
        for (int i = 0; i < parts.length; i++) {
            // Splitting words inside and outside of quotes
            String[] words = parts[i].split("\\W+");

            if (i % 2 == 1) { // Handle phrases (words inside quotes)
                for (int j = 0; j < words.length; j++) {
                    if (!words[j].isEmpty()) {
                        String word = words[j].toLowerCase();
                        if (!stopWord(word)) {
                            word = stemWord(word);

                            System.out.println(word);

                            if (index.containsKey(word)) {
                                allPostings.add(index.get(word).pList);
                            }

                            if (j + 1 < words.length) {
                                String nextWord = words[j + 1].toLowerCase();
                                if (!stopWord(nextWord)) {

                                    System.out.println(nextWord);

                                    nextWord = stemWord(nextWord);
                                    String biWord = word + "_" + nextWord;

                                    System.out.println(biWord);

                                    if (index.containsKey(biWord)) {
                                        allPostings.add(index.get(biWord).pList);
                                    }
                                }
                            }
                        }
                    }
                }
            } else { // Handle individual words (outside quotes)
                for (String word : words) {
                    System.out.println(word + "\n");
                    if (!word.isEmpty() && !stopWord(word.toLowerCase())) {
                        word = stemWord(word.toLowerCase());

                        if (index.containsKey(word)) {
                            allPostings.add(index.get(word).pList);
                        }
                    }
                }
            }
        }

        if (allPostings.isEmpty()) {
            return "No match found";
        }

        // Intersect all postings
        Posting resultPosting = allPostings.get(0);
        for (int i = 1; i < allPostings.size(); i++) {
            resultPosting = intersect(resultPosting, allPostings.get(i));
            if (resultPosting == null) {
                return "No match found";
            }
        }

        // Append the results from the final posting list
        while (resultPosting != null) {
            result += "\t" + resultPosting.docId + " - " + sources.get(resultPosting.docId).title + " - " + sources.get(resultPosting.docId).length + "\n";
            resultPosting = resultPosting.next;
        }

        return result.isEmpty() ? "No match found" : result;
    }








    //---------------------------------
    // Bubble sort for sorting an array of strings
    String[] sort(String[] words) {  //bubble sort
        boolean sorted = false;
        String sTmp;
        //-------------------------------------------------------
        while (!sorted) {
            sorted = true;
            for (int i = 0; i < words.length - 1; i++) {
                int compare = words[i].compareTo(words[i + 1]);
                if (compare > 0) {
                    sTmp = words[i];
                    words[i] = words[i + 1];
                    words[i + 1] = sTmp;
                    sorted = false;
                }
            }
        }
        return words;
    }

    //---------------------------------
// Store the index to a file
    public void store(String storageName) {
        try {
            String pathToStorage = "tmp11/rl/"+storageName;
            Writer wr = new FileWriter(pathToStorage);
            for (Map.Entry<Integer, SourceRecord> entry : sources.entrySet()) {
                System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue().URL + ", Value = " + entry.getValue().title + ", Value = " + entry.getValue().text);
                wr.write(entry.getKey().toString() + ",");
                wr.write(entry.getValue().URL.toString() + ",");
                wr.write(entry.getValue().title.replace(',', '~') + ",");
                wr.write(entry.getValue().length + ","); //String formattedDouble = String.format("%.2f", fee );
                wr.write(String.format("%4.4f", entry.getValue().norm) + ",");
                wr.write(entry.getValue().text.toString().replace(',', '~') + "\n");
            }
            wr.write("section2" + "\n");

            Iterator it = index.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                DictEntry dd = (DictEntry) pair.getValue();
                //  System.out.print("** [" + pair.getKey() + "," + dd.doc_freq + "] <" + dd.term_freq + "> =--> ");
                wr.write(pair.getKey().toString() + "," + dd.doc_freq + "," + dd.term_freq + ";");
                Posting p = dd.pList;
                while (p != null) {
                    //    System.out.print( p.docId + "," + p.dtf + ":");
                    wr.write(p.docId + "," + p.dtf + ":");
                    p = p.next;
                }
                wr.write("\n");
            }
            wr.write("end" + "\n");
            wr.close();
            System.out.println("=============EBD STORE=============");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //=========================================
// Check if a storage file exists
    public boolean storageFileExists(String storageName){
        java.io.File f = new java.io.File("tmp11/rl/"+storageName);
        if (f.exists() && !f.isDirectory())
            return true;
        return false;

    }
    //----------------------------------------------------
// Create an empty storage file
    public void createStore(String storageName) {
        try {
            String pathToStorage = "tmp11/"+storageName;
            Writer wr = new FileWriter(pathToStorage);
            wr.write("end" + "\n");
            wr.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //----------------------------------------------------
    //load index from hard disk into memory
    public HashMap<String, DictEntry> load(String storageName) {
        try {
            String pathToStorage = "tmp11/rl/"+storageName;
            sources = new HashMap<Integer, SourceRecord>();
            index = new HashMap<String, DictEntry>();
            BufferedReader file = new BufferedReader(new FileReader(pathToStorage));
            String ln = "";
            int flen = 0;
            while ((ln = file.readLine()) != null) {
                if (ln.equalsIgnoreCase("section2")) {
                    break;
                }
                String[] ss = ln.split(",");
                int fid = Integer.parseInt(ss[0]);
                try {
                    System.out.println("**>>" + fid + " " + ss[1] + " " + ss[2].replace('~', ',') + " " + ss[3] + " [" + ss[4] + "]   " + ss[5].replace('~', ','));

                    SourceRecord sr = new SourceRecord(fid, ss[1], ss[2].replace('~', ','), Integer.parseInt(ss[3]), Double.parseDouble(ss[4]), ss[5].replace('~', ','));
                    //   System.out.println("**>>"+fid+" "+ ss[1]+" "+ ss[2]+" "+ ss[3]+" ["+ Double.parseDouble(ss[4])+ "]  \n"+ ss[5]);
                    sources.put(fid, sr);
                } catch (Exception e) {

                    System.out.println(fid + "  ERROR  " + e.getMessage());
                    e.printStackTrace();
                }
            }
            while ((ln = file.readLine()) != null) {
                //     System.out.println(ln);
                if (ln.equalsIgnoreCase("end")) {
                    break;
                }
                String[] ss1 = ln.split(";");
                String[] ss1a = ss1[0].split(",");
                String[] ss1b = ss1[1].split(":");
                index.put(ss1a[0], new DictEntry(Integer.parseInt(ss1a[1]), Integer.parseInt(ss1a[2])));
                String[] ss1bx;   //posting
                for (int i = 0; i < ss1b.length; i++) {
                    ss1bx = ss1b[i].split(",");
                    if (index.get(ss1a[0]).pList == null) {
                        index.get(ss1a[0]).pList = new Posting(Integer.parseInt(ss1bx[0]), Integer.parseInt(ss1bx[1]));
                        index.get(ss1a[0]).last = index.get(ss1a[0]).pList;
                    } else {
                        index.get(ss1a[0]).last.next = new Posting(Integer.parseInt(ss1bx[0]), Integer.parseInt(ss1bx[1]));
                        index.get(ss1a[0]).last = index.get(ss1a[0]).last.next;
                    }
                }
            }
            System.out.println("============= END LOAD =============");
            //    printDictionary();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return index;
    }
}

//=====================================================================