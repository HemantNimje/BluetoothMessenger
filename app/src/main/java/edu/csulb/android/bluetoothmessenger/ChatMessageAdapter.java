package edu.csulb.android.bluetoothmessenger;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import static edu.csulb.android.bluetoothmessenger.ProfileActivity.mac_add;

/**
 * Created by nisarg on 5/08/2017
 */

public class ChatMessageAdapter extends ArrayAdapter<MessageInstance> {
    private Context ctx;

    ChatMessageAdapter(Context ctx, int resource){
        super(ctx, resource);
        this.ctx = ctx;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = LayoutInflater.from(ctx).inflate(R.layout.chat_message, null, false);
        MessageInstance msg = (MessageInstance) getItem(position);

        TextView messageView = (TextView) view.findViewById(R.id.singleMessage);
        ImageView imageView = (ImageView) view.findViewById(R.id.chat_image);
        LinearLayout layout = (LinearLayout) view.findViewById(R.id.chat_linear_layout);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = (msg.send ? Gravity.END : Gravity.START);
        layout.setLayoutParams(params);


        if(msg.message != null) {

            messageView.setText(msg.message);
            messageView.setBackgroundResource((msg.send) ? R.drawable.ic_chat_bubble_out : R.drawable.ic_chat_bubble_outline_black_48dp);
//            messageView.setHeight(50);
//            messageView.setBackgroundColor( (Color.LTGRAY));
            messageView.setTextSize(16);
            LinearLayout.LayoutParams params_message = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params_message.width = 0;
            params_message.height = 0;
            imageView.setLayoutParams(params_message);

        } else if(msg.imageBitmap != null) {
            imageView.setImageBitmap(msg.imageBitmap);
            messageView.setBackgroundResource(0);
            imageView.setBackgroundResource((msg.send) ? R.drawable.ic_chat_bubble_out : R.drawable.ic_chat_bubble_outline_black_48dp);
        } else if(msg.audioFile != null){
            String text = (msg.send) ? "File sent: " : "File Received: ";
            messageView.setText(text +  msg.audioFile.getName());
            LinearLayout.LayoutParams params_message = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params_message.width = 0;
            params_message.height = 0;
            imageView.setLayoutParams(params_message);
            messageView.setBackgroundResource((msg.send) ? R.drawable.ic_chat_bubble_out : R.drawable.ic_chat_bubble_outline_black_48dp);
        }
        return view;
    }
}
