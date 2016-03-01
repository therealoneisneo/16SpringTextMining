/**
 * 
 */
package structures;

import java.util.*;

/**
 * @author hongning
 * Suggested structure for constructing N-gram language model
 */
public class LanguageModel {

	int m_N; // N-gram
	int m_V; // the vocabular size
	HashMap<String, Token> m_model; // sparse structure for storing the maximum likelihood estimation of LM with the seen N-grams
	LanguageModel m_reference; // pointer to the reference language model for smoothing purpose
	HashMap<String, HashMap<String, Double>> m_bigrams;// the haspMap for the bigram, with the first word and all tokens with the second word.
	double m_lambda; // parameter for linear interpolation smoothing
	double m_delta; // parameter for absolute discount smoothing
	double m_totalcount;// parameter to count the total word count in this corpus.
	Set<String> m_uniSet;// the set holds all unigram keys for the bigram generation purpose.
	
	
	
	public LanguageModel(int N) {
		m_N = N;
//		m_V = V;
		m_totalcount = 0;
		m_model = new HashMap<String, Token>();
		m_bigrams = new HashMap<String, HashMap<String, Double>>();
		m_lambda = 0.9;
		m_delta = 0.1;
	}
	
	public int getGramNum()
	{
		return m_N;
	}
//	
	
	
	public void setUniKeySet(Set<String> uni)
	{
		m_uniSet = uni;
	}
//	public void testbigrams()
//	{
//		for (Map.Entry<String, Token> entry : m_model.entrySet())
//		{
//			String[] tokStr = entry.getKey().split("_");
//			if (tokStr[0].equals("good"))
//			{
//				System.out.println(entry.getValue().m_token);
//				System.out.println(entry.getValue().m_value);
//			}
//		}
//	}
//	
	public void bigramTokenProcess()
	{
//		int countgood = 0;
		for (Map.Entry<String, Token> entry : m_model.entrySet())
		{
//			if (entry.getKey().equals("good_meal"))
//				countgood += 1;
			String[] tokStr = entry.getKey().split("_");
			
				
			
			if (m_bigrams.containsKey(tokStr[0]))
			{
				HashMap<String, Double> temp = m_bigrams.get(tokStr[0]);
				temp.put(tokStr[1], entry.getValue().getValue());
			}
			else
			{	
				HashMap<String, Double> temp = new HashMap<String, Double>();
				temp.put(tokStr[1], entry.getValue().getValue());
				m_bigrams.put(tokStr[0], temp);
			}
		}
//		System.out.println("good :" + countgood);
	}

	public HashMap<String, Token> getModel()
	{
		return m_model;
	}
	
	public void increCount()// increase the word count of m_totalcount
	{
		m_totalcount += 1;
	}
	
	public void setDelta(double value)
	{
		m_delta = value;
	}
	
	public void setLambda(double value)
	{
		m_lambda = value;
	}
	
	public void addToken(String tok)// add token count or new token to m_model
	{
		if (m_model.containsKey(tok))
		{
			Token temp = m_model.get(tok);
			temp.setValue(temp.getValue() + 1); // increase count by 1
		}
		else
		{
			Token newt = new Token(m_model.size(), tok);
			newt.setValue(1); 
			m_model.put(tok, newt);
		}
	}
	
	public void setRefModel(LanguageModel Ref)
	{
		m_reference = Ref;
	}
	
	
	
	public static List<Map.Entry<String, Double>> SortHashMap(HashMap<String, Double> tokens)
	{
		List<Map.Entry<String, Double>> entrylist = new ArrayList<Map.Entry<String, Double>>(tokens.entrySet());

		Collections.sort(entrylist, new Comparator<Map.Entry<String, Double>>()
			{public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2)
				{
					if (o2.getValue() - o1.getValue() > 0)
						return 1;
					else if (o2.getValue() - o1.getValue() == 0)
						return 0;
					else 
						return -1;
				}
			});
		return entrylist;
	}
	
	
	
	public List<Map.Entry<String, Double>> ProbQuery(String preTok, String smtype)// to query the top "num" of bigram probs with prefix "preTok"
	{
		Set<String> postToks = m_bigrams.get(preTok).keySet();
		HashMap<String, Double> tks = new HashMap<String, Double>();
		for (String tok : postToks)
		{
			double prob = 0.0;
			String combo = preTok + "_" + tok;
			if (smtype.equals("lin"))
			{
				prob = calcLinearSmoothedProb(combo);
			}
			else
			{
				prob = calcAbsSmoothedProb(combo);
			}
			tks.put(combo, prob);
		}
		List<Map.Entry<String, Double>> result = SortHashMap(tks);
		return result;
	}
	
	
	
	public double calcMLProb(String token) {
		// return m_model.get(token).getValue(); // should be something like this
		if (m_N > 1)// for the bigram case, the prob is the conditional prob under the first word of the bigram
		{
			String[] toks = token.split("_");
			if (m_bigrams.containsKey(toks[0]))
			{
				double count = 0;
				HashMap<String, Double> temp = m_bigrams.get(toks[0]);
				for (Map.Entry<String, Double> entry : temp.entrySet())
				{
					count += entry.getValue();
				}
				if (temp.containsKey(toks[1]))
				{
					return temp.get(toks[1]) / count;
				}
				else
					return 0;
				
			}
			else
				return 0;
		}
		else
		{
			if (m_model.containsKey(token))
			{
				return m_model.get(token).getValue();// / m_totalcount;
			}
			else
				return 0;
		}
	}
	
	public double calcAbsCount(String token) // when doing the absolute discounting smoothing, need to calculate the max(c[i,i-1] - delta, 0)/c[i -1]
	{
		String[] toks = token.split("_");
		if (m_bigrams.containsKey(toks[0]))
		{
			HashMap<String, Double> temp = m_bigrams.get(toks[0]);
			double S = temp.size();// the number of uniq word following toks[0]
			double count = 0;
			for (Map.Entry<String, Double> entry : temp.entrySet())
			{
				count += entry.getValue();
			}
			if (temp.containsKey(toks[1]))
				return (Math.max(temp.get(toks[1]) - m_delta, 0) + m_delta * S * m_reference.calcLinearSmoothedProb(toks[1])) / count ;
			else
				return m_delta * S * m_reference.calcLinearSmoothedProb(toks[1]) / count;
		}
		else
			return m_reference.calcLinearSmoothedProb(toks[1]);
	}

	public double calcLinearSmoothedProb(String token) 
	{
		if (m_N>1) 
		{
			return m_lambda * calcMLProb(token) + (1.0-m_lambda) * m_reference.calcLinearSmoothedProb(token);
		}
		else
		{
			double temp = (calcMLProb(token) + m_delta) / (m_totalcount + m_delta * m_model.size());
			return m_lambda * temp + (1.0 - m_lambda) / m_model.size();
		}
		// please use additive smoothing to smooth a unigram language model
	}
	
	public double calcAbsSmoothedProb(String token) 
	{
		if (m_N>1) 
		{
			
			return m_lambda * calcAbsCount(token) + (1.0-m_lambda) * m_reference.calcLinearSmoothedProb(token);
		}
		else
		{
			double temp = (calcMLProb(token) + m_delta) / (m_totalcount + m_delta * m_model.size());
			return m_lambda * temp + (1.0 - m_lambda) / m_model.size();
		}
		// please use additive smoothing to smooth a unigram language model
	}
	
	
	public String genSentance(int len, int ngram, String SmType)
	{
		String result = "";
		if (ngram == 1)
		{
			result = sampling(SmType);
			for (int i = 0; i < len - 1; i++)
			{
				result += " " + sampling(SmType);
			}
			result += ".";
			
		}
		else
		{
			result = m_reference.sampling(SmType);
			String preTok = result;
			for (int i = 0; i < len - 1; i++)
			{
				String temptest = sampling(SmType, preTok);
				String[] toks = temptest.split("_");
				System.out.println(toks);
				preTok = toks[1];
				result += " " + preTok;
			}
			result += ".";
		}
		result = result.substring(0, 1).toUpperCase() + result.substring(1);
		return result;
	}
	
//	public void testProbSum()
//	{
//		for (String preTok : m_uniSet)
//		{
//			double testsum1 = 0.0;
//			double testsum2 = 0.0;
//			for(String postTok : m_uniSet)
//			{
//				String tok = preTok + "_" + postTok;
//				testsum1 += calcLinearSmoothedProb(tok);
//				testsum2 += calcAbsSmoothedProb(tok);
//			}
//			
//			int a = 0;
//		}
//		
//	}
	
	//We have provided you a simple implementation based on unigram language model, please extend it to bigram (i.e., controlled by m_N)
	public String sampling(String SmType, String preTok) 
	{
		double prob = Math.random(); // prepare to perform uniform sampling

		while (prob > 0)
		{
			for(String postTok : m_uniSet)
			{
				String tok = preTok + "_" + postTok;
				if(SmType.equals("lin"))
					prob -= calcLinearSmoothedProb(tok);
				else
					prob -= calcAbsSmoothedProb(tok);
				
				if (prob<=0)
					return tok;
			}
		}
		return null; //How to deal with this special case?
	}
	
	public String sampling(String SmType) // mainly for unigram sampling
	{
		double prob = Math.random(); // prepare to perform uniform sampling
		while (prob > 0)
		{
			if (m_N == 1)
			{
				for(String token:m_model.keySet()) 
				{
					prob -= calcLinearSmoothedProb(token);
					if (prob<=0)
						return token;
				}
			}
		}
		return null; //How to deal with this special case?
	}
	
	//We have provided you a simple implementation based on unigram language model, please extend it to bigram (i.e., controlled by m_N)
	public double logLikelihood(Post review) {
		double likelihood = 0;
		for(String token:review.getTokens()) {
			likelihood += Math.log(calcLinearSmoothedProb(token));
		}
		return likelihood;
	}
}
