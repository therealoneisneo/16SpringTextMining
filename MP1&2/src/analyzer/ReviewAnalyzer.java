package analyzer;
import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
//import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map.Entry;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
//import org.tartarus.snowball.ext.porterStemmer;

//import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
//import structures.LanguageModel;
import structures.Post;
import structures.Token;

import org.jsoup.*;

/**
 * @author Jinlong
 * BM25F implementation for the Text Mining course project  
 */
public class ReviewAnalyzer 
{
//	int m_N;
	double DocNum;//the total number of App description-review instances

	HashSet<String> m_stopwords;
	HashSet<String> m_CtrlVocabulary;
//	int m_train_num;
//	int m_test_num;
	ArrayList<Post> m_discriptions;// the discription part of the app
	ArrayList<Post> m_reviews; // the review part of the app
	ArrayList<Post> m_queries;// query
	
	HashMap<String, Double> m_count_d; //  global description df counts
	HashMap<String, Double> m_count_r; //  global reviews df counts
	double avl_d = 0;
	double avl_r = 0;// the average length of discription and reviews.
	
//	HashMap<String, Integer> m_ID_Index_map; // hash map mapping review id and the index in m_reviews
//	HashMap<String, Integer> m_classtruth;// the review true pos and neg
	//we have also provided a sample implementation of language model in src.structures.LanguageModel
	Tokenizer m_tokenizer;
	
	//==========================These variables declaired for global use to refine the garbage collection 
	String nameID;
	String parent;
	String title;
//	String sentences = json.getString("sentences");
	String content;
	String[] tokens;
	SnowballStemmer stemmer;
	//==========================
	
	
	//this structure is for language modeling
//	LanguageModel m_langModel;
//	// MP3, the positive and negative language model
//	LanguageModel m_PoslangModel;
//	LanguageModel m_NeglangModel;
	
	public static List<Map.Entry<String, Double>> SortHashMap_double(HashMap<String, Double> wordlist)
	{
		List<Map.Entry<String, Double>> entrylist = new ArrayList<Map.Entry<String, Double>>(wordlist.entrySet());

		Collections.sort(entrylist, new Comparator<Map.Entry<String, Double>>()
			{public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2)
				{
					if (o2.getValue() > o1.getValue())
						return 1;
					else if(o2.getValue() == o1.getValue())
						return 0;
					else
						return -1;
				}
			});
		return entrylist;
	}
	
	public ReviewAnalyzer(String tokenModel, int N) throws InvalidFormatException, FileNotFoundException, IOException {

//		m_N = N;
		m_discriptions = new ArrayList<Post>();
		m_reviews = new ArrayList<Post>();
		m_queries = new ArrayList<Post>();
//		m_stats = new HashMap<String, Token>();
//		m_dfstats = new HashMap<String, Token>();
		m_tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(tokenModel)));
		m_stopwords = new HashSet<String>();
		m_CtrlVocabulary = new HashSet<String>();
		m_count_d = new HashMap<String, Double>();
		m_count_r = new HashMap<String, Double>();
		avl_d = 0;
		avl_r = 0;// the average length of discription and reviews.
		stemmer = new englishStemmer();
	}
	
	public JSONObject LoadJson(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			StringBuffer buffer = new StringBuffer(1024);
			String line;
			
			while((line=reader.readLine())!=null) {
				buffer.append(line);
			}
			reader.close();
			
			return new JSONObject(buffer.toString());
		} catch (IOException e) {
			System.err.format("[Error]Failed to open file %s!", filename);
			e.printStackTrace();
			return null;
		} catch (JSONException e) {
			System.err.format("[Error]Failed to parse json file %s!", filename);
			e.printStackTrace();
			return null;
		}
	}
	
	String[] Tokenize(String text) {
		return m_tokenizer.tokenize(text);
	}
	
	public String Normalization(String token) {
		// remove all non-word characters
		// please change this to removing all English punctuation
//		
		
		token = token.replaceAll("\\p{Punct}+", "");

//		remove punctuation

		// convert to lower case
		token = token.toLowerCase(); 
		
		if (token.matches("[+-]*[0-9]+(\\.[0-9]+)*")) // substitude nums
		{
			token = "NUM";
		}
		
		// add a line to recognize integers and doubles via regular expression
		// and convert the recognized integers and doubles to a special symbol "NUM"
//		token = token.replaceAll("[^A-Za-z0-9]", "");
		token = token.replaceAll("\\W+", "");
		
		return token;
	}
	
	public String SnowballStemming(String token) {
		SnowballStemmer stemmer = new englishStemmer();
		stemmer.setCurrent(token);
		if (stemmer.stem())
			return stemmer.getCurrent();
		else
			return token;
	}
	
	public void LoadQueryInit(String filename) // init read in query 
	{
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;
			System.out.println("Loading query...");

			while ((line = reader.readLine()) != null)
			{
				String[] q = line.split("\t");
//				for (String str : q)
//					System.out.println(str);
				Post query = new Post(q[0]);
				String[] tokens = Tokenize(q[1]);
				for (int i = 0; i < tokens.length; i++)
					tokens[i] = SnowballStemming(Normalization(tokens[i]));
				query.setTokens(tokens);
				query.setContent(q[1]);
				for (String tok : tokens)
				{
//					tok = SnowballStemming(Normalization(tok));
					if (tok.length() != 0)
						query.AddVct(tok);
				}
				m_queries.add(query);
				
			}
			reader.close();
		} catch(IOException e){
			System.err.format("[Error]Failed to open file %s!!", filename);
		}
	}
	
//	public void LoadQueryScores(String filename) // load in query ground truth
//	{
//		try {
//			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
//			String line;
//			HashMap<String, String> content_ID = new HashMap<String, String>();
//			for (Post po : m_queries)
//			{
//				content_ID.put(po.getContent(), po.getID());
//			}
//			
//			while ((line = reader.readLine()) != null)
//			{
//				System.out.println(line);
//				String[] q = line.split("\t");
//				for (String str : q)
//				{
//					System.out.println(str);
//				}
//				int index = Integer.parseInt(content_ID.get(q[0]));
//				
////				if (!m_queries.get(index).getContent().equals(q[0]))
////				{
////					System.out.println("error : query index and query content does not match! ");
////					return;
////				}
//				m_queries.get(index).Addresult(q[1], Double.parseDouble(q[2]), 20);
////				break;
//			}
//			reader.close();
//		} catch(IOException e){
//			System.err.format("[Error]Failed to open file %s!!", filename);
//		}
//	}
	
	public void Dprint(String title, String content)
	{
		System.out.println(title + " : " + content);
	}
	
	public void analyzeDocument(JSONObject json) {		
		try {
//			JSONArray jarray = json.getJSONArray("");
			nameID = json.getString("name");
			parent = json.getString("parent");
			title = json.getString("title");
//			String sentences = json.getString("sentences");
			content = json.getString("content");
//			Dprint("name", nameID);
//			Dprint("parent", parent);
//			Dprint("title", title);
//			Dprint("sentences", sentences);
//			Dprint("content", content);
			Post review = new Post(nameID);
			content = Jsoup.parse(content).text();//get rid of the HTML structures
			tokens = Tokenize(content);
			for (int i = 0; i < tokens.length; i++)
				tokens[i] = SnowballStemming(Normalization(tokens[i]));
//			review.setTokens(tokens);
			review.setTitle(title);
			
			HashSet<String> dfcheck;
			dfcheck = new HashSet<String>();
			
			for (String tok : tokens)
			{
//				if (m_stopwords.contains(tok))
//					continue;
//				tok = SnowballStemming(Normalization(tok));
//				if (tok.length() != 0)
				if (m_CtrlVocabulary.contains(tok))
				{
					dfcheck.add(tok);
					review.AddVct(tok);	
				}	
			}
			double temp = 0;
			
			double len_document = review.getVctCount();
			if (parent.equals(nameID))
			{
				
				for (String tok : dfcheck)
				{
					if(m_count_d.containsKey(tok))
					{
						temp = m_count_d.get(tok);
						temp += 1;
						m_count_d.put(tok, temp);
					}
					else
						m_count_d.put(tok, 1.0);
				}
				
				
//				Dprint("Its a ", "Child");
				m_discriptions.add(review);
				avl_d += len_document;
			}
			else
			{
				
				for (String tok : dfcheck)
				{
					if(m_count_r.containsKey(tok))
					{
						temp = m_count_r.get(tok);
						temp += 1;
						m_count_r.put(tok, temp);
					}
					else
						m_count_r.put(tok, 1.0);
				}
//				Dprint("Its a ", "Parent");
				m_reviews.add(review);
				avl_r += len_document;
			}
//			m_reviews.add(review);
//			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public void LoadDirectory(String folder, String suffix) {
		File dir = new File(folder);
		int count = 0;
		System.out.println("Loading files from " + folder + "...");
		for (File f : dir.listFiles()) 
		{
			count += 1;
			if (f.isFile() && f.getName().endsWith(suffix))
			{
				analyzeDocument(LoadJson(f.getAbsolutePath()));
			}
			else if (f.isDirectory())
				LoadDirectory(f.getAbsolutePath(), suffix);
			if(count % 50 == 0)
				System.out.println(count);
//			if (count == 1000)
//				break;
//			
		}
		
		System.out.println(count + " files loaded  from " + folder + "...");
	}
	
	
	public double CalcScoreAll(Post query, int i)// calculate document scores within consideration of user reviews and discriptions.
	{
		
		double result = 0;
		HashMap<String, Double> q_vec = query.getVct();
		HashMap<String, Double> d_vec = m_discriptions.get(i).getVct();
		HashMap<String, Double> r_vec = m_reviews.get(i).getVct();
		Set<String> tokens = q_vec.keySet();
		Set<String> d_tokens = d_vec.keySet();
		Set<String> r_tokens = r_vec.keySet();
		int N = m_reviews.size() * 2;
		double d_len = m_discriptions.get(i).getVctCount();
		double r_len = m_reviews.get(i).getVctCount();
		double k3 = 1000;
		double k1 = 3.5;
		double bd = 0.4;
		double br = 0.3;
		double boostd = 0.6;
		double boostr = 0.4;
		
		
		for (String tok : tokens)
		{
			if (d_tokens.contains(tok) || r_tokens.contains(tok))
			{
				double cwq = q_vec.get(tok);
				double dfw = 0;
				if(m_count_d.containsKey(tok))
					dfw += m_count_d.get(tok);
				if(m_count_r.containsKey(tok))
					dfw += m_count_r.get(tok);
				double cwd = 0;
				double cwr = 0;
				if (d_tokens.contains(tok))
					cwd = d_vec.get(tok);
				if (r_tokens.contains(tok))
					cwr = r_vec.get(tok);


				double cwa = ((boostd * cwd) / (1 - bd + bd * d_len / avl_d)) +  (( boostr * cwr) / (1- br + br * r_len / avl_r));
				double term1 = (k3 + 1) * cwq / (k3 + cwq);
				double term2 = ((k1 + 1) * cwa) / (k1 + cwa);
				double term3 = Math.log((N + 1) / (dfw + 0.5));
				result += term1 * term2 * term3;
			}
		}
		
		return result;
	}
	
	
	public double CalcScoreD(Post query, int i)// calculate document scores only with discriptions
	{
		
		double result = 0;
		HashMap<String, Double> q_vec = query.getVct();
		HashMap<String, Double> d_vec = m_discriptions.get(i).getVct();
//		HashMap<String, Double> r_vec = m_reviews.get(i).getVct();
		Set<String> tokens = q_vec.keySet();
		Set<String> d_tokens = d_vec.keySet();
//		Set<String> r_tokens = r_vec.keySet();
		double k3 = 1000;
		double k1 = 4;
		double b = 0.4;
		int N = m_discriptions.size();
		double d_len = m_discriptions.get(i).getVctCount();
		
		for (String tok : tokens)
		{
			if (d_tokens.contains(tok))
			{
				double cwq = q_vec.get(tok);
				double dfw = m_count_d.get(tok);
				double cwdraw = 0;
//				double cwd = 0;
				if (d_tokens.contains(tok))
					cwdraw = d_vec.get(tok);
//				if (r_tokens.contains(tok))
//					cwr = r_vec.get(tok);
				
//				int d_len = d_tokens.size();
//				int r_len = r_tokens.size();
				
//				double r_len = m_reviews.get(i).getVctCount();
//				double cwa = ((0.6 * cwd) / (1 - 0.4 + 0.4 * d_len / avl_d)) +  (( 0.4 * cwr) / (1- 0.3 + 0.3 * r_len / avl_r));
				double cwd_prime = cwdraw / (1 - b + b * d_len / avl_d);
				double term1 = (k3 + 1) * cwq / (k3 + cwq);
				double term2 = ((k1 + 1) * cwd_prime) / (k1 + cwd_prime);
				double term3 = Math.log((N + 1) / (dfw + 0.5));
				result += term1 * term2 * term3;
			}
		}
		
		return result;
	}
	
	public void CalcDocumentScore(int mode) // calculate the score of all documents with all queries and the top results stored in each query individually
	{// mode = 0 means the only with discription
		int count1 = 0;
		double score = 0;
		for (Post query : m_queries)
		{
			query.clearResult();
			System.out.println("Calculating Query " + String.valueOf(count1) + "'s score...");
			count1++;
			int count2 = -1;
			for (int i = 0; i < m_discriptions.size(); i++)
			{
				count2++;
				if (count2 % 1000 == 0)
					System.out.println(String.valueOf(count1) + " : " + String.valueOf(count2));
				if (mode == 0)
					score = CalcScoreD(query, i);
				else
					score = CalcScoreAll(query, i);
//				query.Addresult(m_discriptions.get(i).getTitle(), score, 20);
				query.AddresultAll(m_discriptions.get(i).getTitle(), score);
			}
		}
	}
	public void OutputResult(String filename)// output the query result
	{
		try
		{
			File writename = new File(filename);
			writename.createNewFile();
			BufferedWriter out = new BufferedWriter(new FileWriter(writename));  
			
			for (int i = 0; i < m_queries.size(); i++)
			{
				Post query = m_queries.get(i);
				List<Map.Entry<String, Double>> results = SortHashMap_double(query.getResult());
				out.write(String.valueOf(i));
				System.out.print("Exporting Resulf of query " + String.valueOf(i) + "...");
				for (int j = 0; j < results.size(); j++)
				{
					out.write('\t' + results.get(j).getKey() + ':' + String.valueOf(results.get(j).getValue()));
//					System.out.print('\t' + results.get(j).getKey() + ':' + String.valueOf(results.get(j).getValue()));
				}
				out.write("\r\n");
				System.out.print("\r\n");
			}
			
//			for (Entry<String, Double> entry : entrylist)
//			{
//				out.write(entry.getKey() + "," + entry.getValue() + "\r\n");
//			}
				
			out.flush();
			out.close(); 
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public List<String> LoadVocabulary(String filename) {
		List<String> VList = new ArrayList<String>();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;
//			List<String> testlist = new ArrayList<String>();
			
			
			while ((line = reader.readLine()) != null) {
				//it is very important that you perform the same processing operation to the loaded stopwords
				//otherwise it won't be matched in the text content
//				String temp;
//				temp = SnowballStemming(Normalization(line));
				if (!line.isEmpty())
				{
					if (m_CtrlVocabulary.contains(line))
					{
						VList.add(line);
						System.out.println(line);
					}
					m_CtrlVocabulary.add(line);
//					testlist.add(line);
				}
					
//					System.out.println(line);
			}
			reader.close();
			System.out.format("Loading %d controlled vocabulary from %s\n", m_CtrlVocabulary.size(), filename);
			
		} catch(IOException e){
			System.err.format("[Error]Failed to open file %s!!", filename);
		}
		return VList;
	}
	
	
	public static void main(String[] args) throws InvalidFormatException, FileNotFoundException, IOException 
	{	
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
//		String rootdir = "~/Workspace/Sp16/MP1&2/";
		ReviewAnalyzer analyzer = new ReviewAnalyzer("./data/Model/en-token.bin", 1);
		analyzer.LoadQueryInit("./data/AppReview/queryID.txt");
//		analyzer.LoadQueryScores("./data/AppReview/query-app-relevance");
		analyzer.LoadVocabulary("./data/AppReview/CtrlVocabulary.txt");
		
//		HashMap<String, Token> all_TFs, all_DFs;
//		all_TFs = new HashMap<String, Token>();
//		all_DFs = new HashMap<String, Token>();
//		analyzer.LoadStopwords("init_stop_words.txt");
//		analyzer2.LoadStopwords("init_stop_words.txt");
		
		
		analyzer.LoadDirectory("./data/AppReview/childData", ".json");
		analyzer.LoadDirectory("./data/AppReview/parentData", ".json"); // in text categorizaiton, loading all json files
		analyzer.avl_d /= analyzer.m_discriptions.size();
		analyzer.avl_r /= analyzer.m_reviews.size();
		analyzer.CalcDocumentScore(0);
		analyzer.OutputResult("results_donly.txt");
		analyzer.CalcDocumentScore(1);
		analyzer.OutputResult("results_all.txt");
		

//		all_TFs.putAll(analyzer.m_stats);
////		all_TFs.putAll(analyzer2.m_stats);
////		
//		List<Map.Entry<String, Token>> all_tf_sorted = DocAnalyzer.SortHashMap(all_TFs); // Sort the tokens by DF
//		DocAnalyzer.OutputWordCount(all_tf_sorted, "allTF.txt");
//		List<Map.Entry<String, Token>> all_df_sorted = DocAnalyzer.SortHashMap(all_DFs); // Sort the tokens by DF
//		DocAnalyzer.OutputWordCount(all_df_sorted, "allDF.txt"); 
//		
//		analyzer.BuildCVocabulary(all_df_sorted);//Build Controlled vocabulary
//		DocAnalyzer.OutputWordList(analyzer.m_CtrlVocabulary, "CtrlVocabulary.txt");
//		DocAnalyzer.OutputWordList(analyzer.m_stopwords, "Final_stop_words.txt");
//		
//		//***************************************************
//		// Above are the preprocessing
////		DocAnalyzer SimiAnalyzer = new DocAnalyzer("./data/Model/en-token.bin", 1);
////		SimiAnalyzer.LoadVocabulary("N_CtrlVocabulary.txt");
////		SimiAnalyzer.LoadDirectory("./Data/yelp/test", ".json");
////		SimiAnalyzer.LoadQurey("./Data/samples/query.json");
////
////		HashMap<String, Double> DFs = SimiAnalyzer.LoadDFs("N_allDF.txt");
//////		HashMap<String, Double> TFs = SimiAnalyzer.LoadTFs("alltf.txt");
////		SimiAnalyzer.analyzeDocumentVM(SimiAnalyzer.m_reviews);
////		SimiAnalyzer.analyzeDocumentVM(SimiAnalyzer.m_Qreviews);
////		int size = SimiAnalyzer.m_reviews.size();
////		for (int i = 0; i < size; i++)
////		{
////			SimiAnalyzer.TF_IDFCalc(SimiAnalyzer.m_reviews.get(i), DFs, 38688);
////		}
////		size = SimiAnalyzer.m_Qreviews.size();
////		for (int i = 0; i < size; i++)
////		{
////			SimiAnalyzer.TF_IDFCalc(SimiAnalyzer.m_Qreviews.get(i), DFs, 38688);
////		}
////		
////		SimiAnalyzer.CalcSimi();
//		


		
		System.out.println("Done!");
	}
	
}
