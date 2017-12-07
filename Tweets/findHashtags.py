from keys import*
# Import the necessary methods from "twitter" library
from twitter import Twitter, OAuth

oauth = OAuth(ACCESS_TOKEN, ACCESS_SECRET, CONSUMER_KEY, CONSUMER_SECRET)
old_file = 'popularHashtags.txt'
old_file = open(old_file, "r")
hashtags=[]
for tag in old_file:
    if tag not in hashtags:
        hashtags.append(tag)

twitter = Twitter(auth=oauth)
US_code = 23424977
trend_json = twitter.trends.place(_id=US_code)
for trends in trend_json[0]["trends"]:
    trend = trends["name"]
    if trend[0] == '#' and trend not in hashtags:     # only keep it if the trend is a hashtag
        hashtags.append(trend + "\n")   # add new line char so each hashtag is written on its own line in the file
        # how the json result looks (for reference)
        # [
        #     {
        #         "trends": [
        #             {
        #                 "name": "#AYTO",
        #                 "url": "http://twitter.com/search?q=%23AYTO",
        #                 "promoted_content": null,
        #                 "query": "%23AYTO",
        #                 "tweet_volume": 15670
        #             },

newPopularHashtagFile = open('popularHashtags.txt', 'w')
for hashtag in hashtags:
    newPopularHashtagFile.write(hashtag)
    print(hashtag)

