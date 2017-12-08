from keys import *
# Import the necessary methods from "twitter" library
from twitter import Twitter, OAuth

tweetCountPerHashtag = 100
newTweetFile = open('tweets.csv', 'w')
oauth = OAuth(ACCESS_TOKEN, ACCESS_SECRET, CONSUMER_KEY, CONSUMER_SECRET)

# Initiate the connection to Twitter
twitter = Twitter(auth=oauth)

popularHashtagsFile = 'popularHashtags.txt'
popularHashtags = open(popularHashtagsFile, "r")
# requests = 0
# rateLimit = 15
for hashtag in popularHashtags:
    print(hashtag)
    hashtag = hashtag.strip("\n")
    # print(trend)
    # requests+=1
    # query for hashtag (excluding the retweets)
    query = hashtag + "-filter:retweets"
    # perform tweet search, result will be a json
    json_result = twitter.search.tweets(q=query, result_type='recent', lang='en', count=tweetCountPerHashtag)
    tweets = json_result['statuses'] # all tweet information is contained within statuses

    for tweet in tweets:
        # Take out the tweet's id
        user_name =str(tweet['user']['name'].replace(",", "").encode('ascii', 'ignore')).strip("b").strip("'")

        # Take out user date
        date = tweet["created_at"]

        # Take out the tweet's contents
        # remove commas from string content, encode (get rid of emojis, etc)
        contents = str(tweet['text'].replace("\n", " ").replace(",", "").encode('ascii', 'ignore'))
        contents=contents.strip("b").strip("'")  # encodeing would add 'b<conent>' to content

        # Take out the tweet's hashtags
        hashtagList = tweet['entities']['hashtags']
        allHashtags =""  # concatenate all the hashtags to a string (with comma inbetween each)
        for tag in hashtagList:
            tag = tag["text"].replace(",", "")
            allHashtags+=tag + ","
        # remove commas and encode for good measure
        allHashtags = str(allHashtags.encode("utf-8"))
        allHashtags = allHashtags.strip("b").strip("'")
        hashtag = hashtag.strip("#")
        if hashtag not in allHashtags:
            allHashtags+=hashtag + ","

        newTweetFile.write("no category yet" + "," + user_name+ ',' + date + "," + contents + "," + allHashtags + "\n")

        # if(requests&rateLimit==0):
        #     time.sleep(60*15)

newTweetFile.close()



