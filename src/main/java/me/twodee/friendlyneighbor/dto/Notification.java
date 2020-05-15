package me.twodee.friendlyneighbor.dto;

import java.util.HashMap;
import java.util.Map;

public class Notification
{
    Map<String, String> errors = new HashMap<>();

    public void addError(String key, String message)
    {
        errors.put(key, message);
    }

    public Map<String, String> getErrors()
    {
        return errors;
    }

    public void setErrors(Map<String, String> errors)
    {
        this.errors = errors;
    }

    public boolean hasErrors()
    {
        return !errors.isEmpty();
    }
}
