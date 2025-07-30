package clients.gui;

public class ChatMessage {
    public final String content;
    public final boolean isNew;

    public ChatMessage(String content, boolean isNew) {
        this.content = content;
        this.isNew = isNew;
    }

    @Override
    public String toString() {
        return content;
    }
}
