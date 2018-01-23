package com.sleepycookie.stillstanding;

/**
 * Created by geotsam on 23/01/2018.
 * This class is used to create a Person object with some contact data (eg. name, mail etc.)
 */

public class Person {
    private String mName;
    private String mMail;
    private String mWebsite;
    private int mImageID;

    public Person(String name, String mail, String website, int imageID){
        mName = name;
        mMail = mail;
        mWebsite = website;
        mImageID = imageID;
    }

    public Person(String name, String mail, String website){
        mName = name;
        mMail = mail;
        mWebsite = website;
    }

    public String getName() {
        return mName;
    }

    public String getMail() {
        return mMail;
    }

    public String getWebsite() {
        return mWebsite;
    }

    public int getImageID() {
        return mImageID;
    }
}
