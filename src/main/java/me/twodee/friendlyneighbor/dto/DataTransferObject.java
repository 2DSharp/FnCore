package me.twodee.friendlyneighbor.dto;

public abstract class DataTransferObject
{
    protected Notification notification = new Notification();

    public Notification getNotification()
    {
        return notification;
    }

    public void setNotification(Notification notification)
    {
        this.notification = notification;
    }

    public void appendNotification(Notification notification)
    {
        this.notification.getErrors().putAll(notification.getErrors());
    }
}