package com.weather.percussion.extensions.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tester {
    public static void main(String... args) {
        String input = "20120618-fr-test-key-20120618";
        Pattern endPattern = Pattern.compile("-[0-9]{6,8}$");
        Matcher endMatcher = endPattern.matcher(input);
        Pattern startPattern = Pattern.compile("^[0-9]{6,8}-");
        Matcher startMatcher = startPattern.matcher(input);
        
        if (!endMatcher.find()) {
            System.out.println("Does not end with a date");
        }
        else {
            System.out.println("Ends with a date");
        }
        
        if (!startMatcher.find()) {
            System.out.println("Does not start with a date");
        }
        else {
            System.out.println("Starts with a date");
        }
    }
}
