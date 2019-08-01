package com.sigilius.android.bookfinder;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class BookAdapter extends ArrayAdapter<Book> {

    // Constructor
    public BookAdapter(Activity context, ArrayList<Book> books) {
        super(context, 0, books);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View listItemView = convertView;

        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }

        // Get the {@link Book} object located at this position in the list
        Book currentBook = getItem(position);

        TextView authorTextView = (TextView) listItemView.findViewById(R.id.author);
        authorTextView.setText(currentBook.getAuthor());

        TextView titleTextView = (TextView) listItemView.findViewById(R.id.title);
        titleTextView.setText(currentBook.getTitle());

        TextView isbn13View = (TextView) listItemView.findViewById(R.id.isbn13);
        isbn13View.setText(currentBook.getIsbn13());

        TextView isbn10View = (TextView) listItemView.findViewById(R.id.isbn10);
        isbn10View.setText(currentBook.getIsbn10());

        // Display the image from the JSON URL
        try {

            ImageView bookImage = (ImageView) listItemView.findViewById(R.id.image_place);
            Bitmap bitmap = BitmapFactory.decodeStream((InputStream) new URL(currentBook.getImageUrI()).getContent());
            bookImage.setImageBitmap(bitmap);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return listItemView;
    }
}
