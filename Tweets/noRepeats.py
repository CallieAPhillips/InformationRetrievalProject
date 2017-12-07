#use if you have 2 sets of hashtags and want to combine them (removing repeats)

old_file = 'trendingTweets.txt'
old_file = open(old_file, "r")
hashtags=[]
for tag in old_file:
    if tag not in hashtags:
        hashtags.append(tag)

file = open('popularHashtags.txt','w')
for tag in hashtags:
    file.write(tag)