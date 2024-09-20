package invertedIndex;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Math.log10;
import static java.lang.Math.sqrt;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.io.PrintWriter;

public class Index5 {

    int N = 0;  // number of documents
    public Map<Integer, SourceRecord> sources;  // store the doc_id and the file name.
    public HashMap<String, DictEntry> index; // The inverted index
    SortedScore sortedScore;

    public Index5() {
        sources = new HashMap<>();
        index = new HashMap<>();
    }

    public void setN(int n) {
        N = n;
    }

    public void printPostingList(Posting p) {
        System.out.print("[");
        while (p != null) {
            System.out.print(p.docId);
            p = p.next;
            if (p != null) {
                System.out.print(",");
            }
        }
        System.out.println("]");
    }

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

    public int buildIndex(String ln, int fid) {
        int flen = 0;
        String[] words = ln.split("\\W+");
        flen += words.length;
        for (String word : words) {
            word = word.toLowerCase();
            if (stopWord(word)) {
                continue;
            }
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
            index.get(word).term_freq += 1;
        }
        return flen;
    }

    public void buildIndex(String[] files) {
        int fid = 0;
        for (String fileName : files) {
            try (BufferedReader file = new BufferedReader(new FileReader(fileName))) {
                if (!sources.containsKey(fileName)) {
                    sources.put(fid, new SourceRecord(fid, fileName, fileName, "notext"));
                }
                String ln;
                int flen = 0;
                while ((ln = file.readLine()) != null) {
                    flen += indexOneLine(ln, fid);
                }
                sources.get(fid).length = flen;
            } catch (IOException e) {
                System.out.println("File " + fileName + " not found. Skip it");
            }
            fid++;
        }
    }

    public int indexOneLine(String ln, int fid) {
        int flen = 0;
        String[] words = ln.split("\\W+");
        flen += words.length;
        for (String word : words) {
            word = word.toLowerCase();
            if (stopWord(word)) {
                continue;
            }
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
            index.get(word).term_freq += 1;
        }
        return flen;
    }

    boolean stopWord(String word) {
        return word.equals("the") || word.equals("to") || word.equals("be") || word.equals("for") || word.equals("from") || word.equals("in")
                || word.equals("a") || word.equals("into") || word.equals("by") || word.equals("or") || word.equals("and") || word.equals("that")
                || word.length() < 2;
    }

    String stemWord(String word) {
        return word;
    }

    Posting intersect(Posting pL1, Posting pL2) {
        Posting answer = null;
        Posting last = null;
        while (pL1 != null && pL2 != null) {
            if (pL1.docId == pL2.docId) {
                if (answer == null) {
                    answer = new Posting(pL1.docId, pL1.dtf + pL2.dtf);
                    last = answer;
                } else {
                    last.next = new Posting(pL1.docId, pL1.dtf + pL2.dtf);
                    last = last.next;
                }
                pL1 = pL1.next;
                pL2 = pL2.next;
            } else if (pL1.docId < pL2.docId) {
                pL1 = pL1.next;
            } else {
                pL2 = pL2.next;
            }
        }
        return answer;
    }

    public String find_07a(String phrase) {
        System.out.println("-------------------------  find_07a -------------------------");

        String result = "";
        String[] terms = phrase.split("\\W+");
        int len = terms.length;
        sortedScore = new SortedScore();

        double[] scores = new double[N];
        double[] lengths = new double[N];

        // Initialize lengths
        for (int i = 0; i < N; i++) {
            lengths[i] = sources.get(i).norm;
        }

        for (String term : terms) {
            term = term.toLowerCase();
            DictEntry entry = index.get(term);
            if (entry != null) {
                double idf = log10(N / (double) entry.doc_freq);
                Posting p = entry.pList;
                while (p != null) {
                    scores[p.docId] += (1 + log10((double) p.dtf)) * idf;
                    p = p.next;
                }
            }
        }

        // Normalize scores by document length
        for (int i = 0; i < N; i++) {
            if (lengths[i] != 0) {
                scores[i] /= lengths[i];
            }
        }

        // Rank the documents by score
        for (int i = 0; i < N; i++) {
            if (scores[i] > 0) {
                sortedScore.insertScoreRecord(new ScoreRecord(scores[i], sources.get(i).URL, sources.get(i).title, sources.get(i).text));
            }
        }

        // Print and return the top K results
        result = sortedScore.printScores();
        return result;
    }

    public void searchLoop() {
        String phrase;
        do {
            System.out.println("Print search phrase: ");
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            try {
                phrase = in.readLine();
                find_07a(phrase);
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        } while (!phrase.isEmpty());
    }

    public void store(String storageName) {
        try {
            String pathToStorage = "C:\\Users\\zeyad\\Desktop\\is322_HW_3" + storageName;
            Writer wr = new FileWriter(pathToStorage);
            for (Map.Entry<Integer, SourceRecord> entry : sources.entrySet()) {
                wr.write(entry.getKey().toString() + ",");
                wr.write(entry.getValue().URL + ",");
                wr.write(entry.getValue().title.replace(',', '~') + ",");
                wr.write(entry.getValue().length + ",");
                wr.write(String.format("%4.4f", entry.getValue().norm) + ",");
                wr.write(entry.getValue().text.replace(',', '~') + "\n");
            }
            wr.write("section2\n");

            Iterator it = index.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                DictEntry dd = (DictEntry) pair.getValue();
//                System.out.print("** [" + pair.getKey() + "," + dd.doc_freq + "] <" + dd.term_freq + "> =--> ");
                wr.write(pair.getKey().toString() + "," + dd.doc_freq + "," + dd.term_freq + ";");
                Posting p = dd.pList;
                while (p != null) {
//                    System.out.print( p.docId + "," + p.dtf + ":");
                    wr.write(p.docId + "," + p.dtf + ":");
                    p = p.next;
                }
                wr.write("\n");
            }
            wr.write("end\n");
            wr.close();
            System.out.println("=============END STORE=============");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean storageFileExists(String storageName) {
        java.io.File f = new java.io.File("tmp11/rl/" + storageName);
        return f.exists() && !f.isDirectory();
    }

    public void createStore(String storageName) {
        try {
            String pathToStorage = "tmp11/" + storageName;
            Writer wr = new FileWriter(pathToStorage);
            wr.write("end\n");
            wr.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
