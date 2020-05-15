package me.twodee.friendlyneighbor.dto;

import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
public class ResultObject
{
    protected Notification notification = new Notification();
    public static String SOMETHING_WENT_WRONG = "Something went wrong internally";

    public ResultObject(Map<String, String> errors)
    {
        notification.setErrors(errors);
    }

    public ResultObject(String errorKey, String errorMessage)
    {
        notification.addError(errorKey, errorMessage);
    }

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