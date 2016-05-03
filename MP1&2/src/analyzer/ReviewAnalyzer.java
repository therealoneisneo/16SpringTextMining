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
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import org.tartarus.snowball.ext.porterStemmer;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.InvalidFormatException;
import structures.LanguageModel;
import structures.Post;
import structures.Token;

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
	ArrayList<Post> m_discriptions;
	ArrayList<Post> m_reviews;
	ArrayList<Post> m_queries;
	
	HashMap<String, Token> m_count_d;
	HashMap<String, Token> m_count_r; // the global count of terms in discription and reviews
	
//	HashMap<String, Integer> m_ID_Index_map; // hash map mapping review id and the index in m_reviews
//	HashMap<String, Integer> m_classtruth;// the review true pos and neg
	//we have also provided a sample implementation of language model in src.structures.LanguageModel
	Tokenizer m_tokenizer;
	
	//this structure is for language modeling
//	LanguageModel m_langModel;
//	// MP3, the positive and negative language model
//	LanguageModel m_PoslangModel;
//	LanguageModel m_NeglangModel;
	
	
	
	public ReviewAnalyzer(String tokenModel, int N) throws InvalidFormatException, FileNotFoundException, IOException {

//		m_N = N;
		m_reviews = new ArrayList<Post>();
		m_Qreviews = new ArrayList<Post>();
		m_stats = new HashMap<String, Token>();
		m_dfstats = new HashMap<String, Token>();
		m_tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(tokenModel)));
		m_stopwords = new HashSet<String>();
		m_CtrlVocabulary = new HashSet<String>();
		m_train_num = -1;
		m_test_num = -1;
	}
	
	
	
}
