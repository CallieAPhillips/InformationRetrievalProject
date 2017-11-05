import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;
import java.util.List;

public class Stream {
    public static void main(String[] args) {
        startStreaming();

        //call with query term to filter by query term
//        searchQuery("term here");
    }
    public static void startStreaming(){
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true);
        cb.setOAuthConsumerKey(Keys.ConsumerKey);
        cb.setOAuthConsumerSecret(Keys.ConsumerSecret);
        cb.setOAuthAccessToken(Keys.AccessToken);
        cb.setOAuthAccessTokenSecret(Keys.AccessTokenSecret);

        TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();

        StatusListener listener = new StatusListener() {

            @Override
            public void onException(Exception arg0) {}

            @Override
            public void onDeletionNotice(StatusDeletionNotice arg0) {}

            @Override
            public void onScrubGeo(long arg0, long arg1) {}

            @Override
            public void onStallWarning(StallWarning stallWarning) {}

            @Override
            public void onStatus(Status status) {
                //tweet class creates tweet object and adds it to a csv file
                new Tweet(status);
                User user = status.getUser();

                // gets Username
                String username = status.getUser().getScreenName();
                System.out.println(username);
                String profileLocation = user.getLocation();
                System.out.println(profileLocation);
                long tweetId = status.getId();
                System.out.println(tweetId);
                String content = status.getText();
                System.out.println(content +"\n");

            }

            @Override
            public void onTrackLimitationNotice(int arg0) {}

        };
        FilterQuery filterQuery = new FilterQuery();

//        String keywords[] = {};
//
//        filterQuery.track(keywords);
        filterQuery.locations(new double[][] {{-180, -90}, {180, 90}}); //entire world
        filterQuery.language("en");

        twitterStream.addListener(listener);
        twitterStream.filter(filterQuery);

    }

    public static void searchQuery(String[] args){
        if (args.length < 1) {
            System.out.println("SearchQuery method error: no query given");
            System.exit(-1);
        }
        Twitter twitter = new TwitterFactory().getInstance();
            try {
            Query query = new Query(args[0]);
            QueryResult result;
            do {
                result = twitter.search(query);
                List<Status> tweets = result.getTweets();
                for (Status tweet : tweets) {
                    System.out.println("@" + tweet.getUser().getScreenName() + " - " + tweet.getText());
                    new Tweet(tweet);
                }
            } while ((query = result.nextQuery()) != null);
            System.exit(0);
        } catch (TwitterException te) {
            te.printStackTrace();
            System.out.println("Failed to search tweets: " + te.getMessage());
            System.exit(-1);
        }
    }
}