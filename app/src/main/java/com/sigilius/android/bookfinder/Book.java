package com.sigilius.android.bookfinder;


import android.os.Parcel;
import android.os.Parcelable;

public class Book implements Parcelable {

    private String mAuthor;
    private String mTitle;
    private String mIsbn13;
    private String mIsbn10;
    private String mImageUrI;

    /**
     * @param title    the book's title
     * @param author   the book's author
     * @param isbn13   ISBN 13
     * @param isbn10   ISBN 10
     * @param imageUrI thumbnail image of the book
     */
    public Book(String title, String author, String isbn13, String isbn10, String imageUrI) {
        mTitle = title;
        mAuthor = author;
        mIsbn13 = isbn13;
        mIsbn10 = isbn10;
        mImageUrI = imageUrI;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getIsbn13() {
        return mIsbn13;
    }

    public String getIsbn10() {
        return mIsbn10;
    }

    public String getImageUrI() {
        return mImageUrI;
    }

    protected Book(Parcel in) {
        mAuthor = in.readString();
        mTitle = in.readString();
        mIsbn13 = in.readString();
        mIsbn10 = in.readString();
        mImageUrI = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mAuthor);
        dest.writeString(mTitle);
        dest.writeString(mIsbn13);
        dest.writeString(mIsbn10);
        dest.writeString(mImageUrI);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Book> CREATOR = new Parcelable.Creator<Book>() {
        @Override
        public Book createFromParcel(Parcel in) {
            return new Book(in);
        }

        @Override
        public Book[] newArray(int size) {
            return new Book[size];
        }
    };

}
