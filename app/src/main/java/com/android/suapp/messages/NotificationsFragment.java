package com.android.suapp.messages;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.Fragment;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.suapp.R;
import com.android.suapp.suapp.sdk.SUAppServer;
import com.android.suapp.suapp.server.database.objects.Message;
import com.android.suapp.suapp.server.database.objects.Student;
import com.android.suapp.suapp.server.database.objects.StudyGroup;
import com.android.suapp.suapp.server.responses.ServerResponse;
import com.android.suapp.suapp.server.utility.MessagesListWrapper;
import com.google.gson.Gson;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.android.suapp.LoginActivity.APP_PREFERENCES;
import static com.android.suapp.LoginActivity.APP_PREFERENCES_STUDENT_DATA;
import static com.android.suapp.LoginActivity.APP_PROFESSION;

/**
 * Created by fokin on 10.04.2018.
 */

public class NotificationsFragment extends Fragment {

    private ImageButton newMessage;
    private SharedPreferences proffesion;
    private int select;
    private Student student;
    private String sourceData;
    public static int messageCount;
    public static List<Message> list;
    private SharedPreferences sp;
    final Handler h = new Handler();
    public final static String APP_MESSAGES_PREFERENCES = "Data_of_messages";
    public final static String APP_MESSAGE_PREFERENCES = "Message_data";

    @SuppressLint("StaticFieldLeak")
    private static NotificationsFragment instance;
    public static NotificationsFragment newInstance() {
        if (instance == null){
            instance = new NotificationsFragment();
        }
        return instance;
    }

    private void getMessages(){
        try{
            sp = getActivity().getSharedPreferences(APP_MESSAGES_PREFERENCES, Context.MODE_PRIVATE);
            String buffer = SUAppServer.getMessages(student.getToken());
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(APP_MESSAGE_PREFERENCES, buffer);
            editor.apply();
            MessagesListWrapper listWrapper = new Gson().fromJson(buffer, MessagesListWrapper.class);
            messageCount = listWrapper.getList().size();
            list = listWrapper.getList();
            System.out.println(messageCount);
        } catch (ConnectException e){
            String buffer = sp.getString(APP_MESSAGE_PREFERENCES, null);
            MessagesListWrapper listWrapper = new Gson().fromJson(buffer, MessagesListWrapper.class);
            messageCount = listWrapper.getList().size();
            list = listWrapper.getList();
        }
        catch (final Exception e) {
            e.printStackTrace();
            h.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

                }
            });
        }
    }

    public NotificationsFragment() {
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notificate, container, false);
        List<String> nameList = new ArrayList<>();
        proffesion = getActivity().getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        final SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setRefreshing(true);
        try {
            sourceData = proffesion.getString(APP_PREFERENCES_STUDENT_DATA, null);
            student =new Gson().fromJson(sourceData, Student.class);
        }
                catch (Exception e){
            Toast.makeText(getContext(), "Не удалось загрузить данные о пользователе", Toast.LENGTH_SHORT).show();
        }

        final RecyclerView recyclerView = view.findViewById(R.id.recycler_view_all_messages);

        new Thread(new Runnable() {
            @Override
            public void run() {
                getMessages();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                            recyclerView.setAdapter(new RecyclerViewAdapter(NotificationsFragment.this.getContext(), CardModel.getObjectList(list)));
                            swipeRefreshLayout.setRefreshing(false);
                        }catch (Exception ignored){}
                    }
                }, 100);
            }
        }).start();




        newMessage = view.findViewById(R.id.new_message);
        final SwipeRefreshLayout.OnRefreshListener listener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                final Handler h = new Handler();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        getMessages();
                        h.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                                recyclerView.setAdapter(new RecyclerViewAdapter(NotificationsFragment.this.getContext(), CardModel.getObjectList(list)));
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        }, 100);
                    }
                }).start();
            }
        };
        newMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                proffesion = getActivity().getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
                select = proffesion.getInt(APP_PROFESSION, -1);
                switch (select){
                    case -1:
                        Toast.makeText(getContext(), "Не удалось загрузить данные о пользователе", Toast.LENGTH_SHORT).show();
                        break;
                    case 0:
                        Toast.makeText(getContext(), "Вашему типу пользователя недоступна отправка сообщений", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                    case 2:
                    case 3:
                        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                        LayoutInflater inflater = getActivity().getLayoutInflater();
                        final View dialogView = inflater.inflate(R.layout.dialog_send_message, null);
                        builder.setView(dialogView)
                                .setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        final EditText messageBody = dialogView.findViewById(R.id.edittext_chat);
                                        final Handler h = new Handler();
                                        swipeRefreshLayout.setRefreshing(true);
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    final String message = SUAppServer.sendMessage(
                                                            new Message().setSenderId(student.getId()).setBody(messageBody.getText().toString()),
                                                            new StudyGroup().setNumber(student.getGroupNumber()),
                                                            student.getToken());
                                                    final ServerResponse response = new Gson().fromJson(message, ServerResponse.class);
                                                    h.post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if(response != null){
                                                                Toast.makeText(getContext(), response.getResponse(), Toast.LENGTH_SHORT).show();
                                                            }else{
                                                                Toast.makeText(getContext(), "Сервер недоступен", Toast.LENGTH_SHORT).show();
                                                            }
                                                            listener.onRefresh();
                                                        }
                                                    });
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }).start();
                                    }
                                })
                                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                    }
                                });

                        AlertDialog dialog = builder.create();
                        dialog.show();
                        break;

                }
            }
        });

        swipeRefreshLayout.setOnRefreshListener(listener);

        return view;
    }

    public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder>{

        private List<CardModel> objectList;
        private LayoutInflater inflater;

        public RecyclerViewAdapter(Context context, List<CardModel> objectList) {
            try {
                inflater = LayoutInflater.from(context);
                this.objectList = objectList;
            }catch(Exception ignored){}
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = inflater.inflate(R.layout.activity_cardview, parent, false);
            MyViewHolder holder = new MyViewHolder(view);
            return holder;
        }

        @Override
        public int getItemCount() {
            return objectList.size();
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            CardModel current = objectList.get(position);
            holder.setData(current, position);
        }

        public class MyViewHolder extends RecyclerView.ViewHolder {
            private CardView mCardView;
            private TextView textIcon;
            private TextView textUserName;
            private TextView textMessage;
            private int position;
            private CardModel currentObject;

            public MyViewHolder(View itemView) {
                super(itemView);
                mCardView = itemView.findViewById(R.id.card_view);
                textIcon = itemView.findViewById(R.id.text_view_user_alphabet);
                textUserName = itemView.findViewById(R.id.text_view_username);
                textMessage = itemView.findViewById(R.id.text_message);
            }

            public void setData(CardModel currentObject, int position) {
                this.textIcon.setText(currentObject.getIcon());
                this.textMessage.setText(currentObject.getMessageBody());
                this.textUserName.setText(currentObject.getUserName());
                this.position = position;
                this.currentObject = currentObject;
            }
        }

        public void removeItem(int position) {
            objectList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, objectList.size());
//		notifyDataSetChanged();
        }

        public void addItem(int position, CardModel currentObject) {
            objectList.add(position, currentObject);
            notifyItemInserted(position);
            notifyItemRangeChanged(position, objectList.size());
//		notifyDataSetChanged();
        }
    }


}
