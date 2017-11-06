import twitter4j.HashtagEntity;
import twitter4j.Status;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import static java.lang.System.exit;

public class Tweet {

    private String content;
    private String user;
    private String category;
    private long id;
    private String[] hashtags;
    private String csvFileName = "tweets.csv";
    private Date time;

    //Format of CSV file (currently) = category, user, time, content, hashtag[0], hashtag[1], ...

    public Tweet(String content, String user, long id, String[] hashtags, Date time) {
        this.content = content.replace("\n", " ").replace(",", "");
        this.user = user;
        this.id = id;
        this.hashtags = hashtags;
        this.time = time;
        setCategory("no category yet");
        this.printToCSV();
    }

    public Tweet(Status status){
        HashtagEntity[] hashtagEntities = status.getHashtagEntities();
        //ignore if no hashtags
        if(hashtagEntities.length>0) {
            //remove commas and new lines so they don't mess up csv file
            this.content = status.getText().replace("\n", " ").replace(",", "");
            this.user = status.getUser().getScreenName().replace(",", "");
            this.id = status.getId();
            this.time = status.getCreatedAt();

            hashtags = new String[hashtagEntities.length];
            for (int i = 0; i < hashtagEntities.length; i++) {
                this.hashtags[i] = hashtagEntities[i].getText().replace(",", "");
            }
            setCategory("no category yet");
            this.printToCSV();
        }
    }

    public void printToCSV() {
        String line = String.format("%s,%s,%s,%s", this.category, this.user, this.time.toString(), this.content);
        for(String tag: this.hashtags) line =line.concat(","+tag);
        line=line.concat("\n");
        System.out.println(line);
        try {
            FileWriter writer = new FileWriter(csvFileName, true);
            writer.append(line);
            writer.close();
        }catch (IOException ex){
            System.out.println(ex.toString());
            System.out.println("ERROR: must create \'tweets.csv\' in InformationRetrievalProject Folder\n " +
                    "alternatively can change Tweet class to use different csv file");
            exit(0);
        }
    }


    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
