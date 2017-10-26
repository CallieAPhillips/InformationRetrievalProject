public class Tweet {

    private String content;
    private String user;
    private String category;
    private int id;
    private String[] hashtags;

    public Tweet(String content, String user, int id, String[] hashtags) {
        this.content = content;
        this.user = user;
        this.id = id;
        this.hashtags = hashtags;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
