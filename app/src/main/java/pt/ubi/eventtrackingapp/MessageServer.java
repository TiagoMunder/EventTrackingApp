package pt.ubi.eventtrackingapp;

public class MessageServer {

    private String messageBody;
    private String sender;
    private String eventId;
    private String time;

    MessageServer() {
    }
    MessageServer(String messageOwner, String messageBody, String eventId , String date) {
        this.sender = messageOwner;
        this.messageBody  =  messageBody;
        this.eventId = eventId;
        this.time = date;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
