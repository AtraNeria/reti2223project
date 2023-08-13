import java.io.Serializable;

public class RankingEntry implements Comparable<RankingEntry>, Serializable {
	
    public String username;
	public float score;

	public RankingEntry (String user, float sc) {
			username = user;
			score = sc;
	}

	@Override
	public int compareTo(RankingEntry re) {
		if (score <= re.score) return 1;
		else return -1;
	}
	
}
