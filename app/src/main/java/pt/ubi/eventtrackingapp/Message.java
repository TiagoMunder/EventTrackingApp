package pt.ubi.eventtrackingapp;

public class Message {

    private String messageBody;
    private String messageOwner;
    private Boolean sendByUs;
    private String eventId;
    private String date;

    public Message() {}

    public Message(String messageOwner, String messageBody, Boolean sendByUs, String eventId, String date) {
        this.messageOwner = messageOwner;
        this.messageBody = messageBody;
        this.sendByUs = sendByUs;
        this.eventId = eventId;
        this.date =  date;
    }



    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    public String getMessageOwner() {
        return messageOwner;
    }

    public void setMessageOwner(String messageOwner) {
        this.messageOwner = messageOwner;
    }

    public Boolean getSendByUs() {
        return sendByUs;
    }

    public void setSendByUs(Boolean sendByUs) {
        this.sendByUs = sendByUs;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
